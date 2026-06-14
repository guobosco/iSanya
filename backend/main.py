from fastapi import FastAPI, Depends, HTTPException, Query, File, UploadFile, Form, WebSocket, WebSocketDisconnect
from fastapi.staticfiles import StaticFiles
from sqlalchemy.orm import Session
from sqlalchemy import or_, desc, func
from typing import List, Optional
import time
import uuid
import math
import json
import bcrypt # 替换 passlib
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from jose import JWTError, jwt
from datetime import datetime, timedelta, date
import os
try:
    from . import models, schemas
except ImportError:
    import models, schemas
try:
    from .database import SessionLocal, engine
except ImportError:
    from database import SessionLocal, engine
try:
    from .schema_sync import sync_sqlite_schema
except ImportError:
    from schema_sync import sync_sqlite_schema
try:
    from .chat_realtime import ChatRealtimeHub
except ImportError:
    from chat_realtime import ChatRealtimeHub

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.join(BASE_DIR, "static")
AVATAR_DIR = os.path.join(STATIC_DIR, "avatars")
CHAT_MEDIA_DIR = os.path.join(STATIC_DIR, "chat")
SERVICE_MEDIA_DIR = os.path.join(STATIC_DIR, "services")
EXPERIENCE_MEDIA_DIR = os.path.join(STATIC_DIR, "experiences")
MAX_UPLOAD_SIZE_BYTES = int(os.getenv("LULU_MAX_UPLOAD_SIZE_MB", "10")) * 1024 * 1024
MEDIA_BIZ_TO_DIR = {
    "avatar": AVATAR_DIR,
    "service": SERVICE_MEDIA_DIR,
    "experience": EXPERIENCE_MEDIA_DIR,
    "chat": CHAT_MEDIA_DIR,
}
APP_ENV = os.getenv("LULU_ENV", "development").lower()
chat_hub = ChatRealtimeHub()

def verify_password(plain_password, hashed_password):
    try:
        return bcrypt.checkpw(plain_password.encode('utf-8'), hashed_password.encode('utf-8'))
    except ValueError:
        return False

def get_password_hash(password):
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed.decode('utf-8')

# JWT 配置
SECRET_KEY = os.getenv("LULU_SECRET_KEY", "dev-secret")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES_ENV = os.getenv("LULU_ACCESS_TOKEN_EXPIRE_MINUTES", "").strip()
ACCESS_TOKEN_EXPIRE_MINUTES = int(ACCESS_TOKEN_EXPIRE_MINUTES_ENV) if ACCESS_TOKEN_EXPIRE_MINUTES_ENV else 0
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/token")
# 与广场发现流一致：无 token 时不报错，供公开读接口（如他人资料）使用
oauth2_scheme_optional = OAuth2PasswordBearer(tokenUrl="/auth/token", auto_error=False)

if APP_ENV == "production" and SECRET_KEY == "dev-secret":
    raise RuntimeError("LULU_SECRET_KEY must be set in production")
if ACCESS_TOKEN_EXPIRE_MINUTES < 0:
    raise RuntimeError("LULU_ACCESS_TOKEN_EXPIRE_MINUTES must be >= 0")

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    to_encode = data.copy()
    expire_delta = expires_delta
    if expire_delta is None and ACCESS_TOKEN_EXPIRE_MINUTES > 0:
        expire_delta = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    if expire_delta is not None:
        expire = datetime.utcnow() + expire_delta
        to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

# 数据库依赖项 (Dependency)
# 用于在请求处理期间获取数据库会话，并在请求结束时关闭会话
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid authentication credentials")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid authentication credentials")
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return user


def get_optional_current_user(
    token: Optional[str] = Depends(oauth2_scheme_optional),
    db: Session = Depends(get_db),
) -> Optional[models.User]:
    """已登录则返回用户；无 token 或无效 token 时返回 None（用于公开只读接口）。"""
    if not token:
        return None
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            return None
    except JWTError:
        return None
    return db.query(models.User).filter(models.User.id == user_id).first()


def ensure_self_access(current_user: models.User, user_id: str):
    if current_user.id != user_id:
        raise HTTPException(status_code=403, detail="Not authorized to access this user")

def ensure_optional_self_access(current_user: models.User, user_id: Optional[str]) -> str:
    if user_id is None:
        return current_user.id
    ensure_self_access(current_user, user_id)
    return user_id

def ensure_field_matches_current_user(current_user: models.User, field_value: Optional[str], field_name: str = "user_id"):
    if field_value is None or field_value != current_user.id:
        raise HTTPException(status_code=403, detail=f"{field_name} does not match current user")

def ensure_record_owner(record, current_user: models.User, owner_field: str = "user_id"):
    owner_id = getattr(record, owner_field, None)
    ensure_field_matches_current_user(current_user, owner_id, owner_field)


DEFAULT_WISHLIST_GROUP_NAME = "默认分组"


def _dedupe_non_empty_ids(values: Optional[List[str]]) -> List[str]:
    return list(dict.fromkeys([value for value in (values or []) if value]))


def _normalize_wishlist_group_name(name: Optional[str]) -> str:
    trimmed = (name or "").strip()
    return trimmed or DEFAULT_WISHLIST_GROUP_NAME


def _filter_existing_service_ids(db: Session, service_ids: List[str]) -> List[str]:
    normalized = _dedupe_non_empty_ids(service_ids)
    if not normalized:
        return []
    existing_ids = {
        item.id for item in db.query(models.Service.id).filter(
            models.Service.id.in_(normalized),
            models.Service.is_deleted == False
        ).all()
    }
    return [sid for sid in normalized if sid in existing_ids]


def _build_wishlist_profile_response(
    service_ids: List[str],
    groups: List[tuple[str, List[str]]]
) -> schemas.WishlistProfileResponse:
    return schemas.WishlistProfileResponse(
        service_ids=service_ids,
        groups=[
            schemas.WishlistGroupPayload(name=name, service_ids=group_service_ids)
            for name, group_service_ids in groups
        ]
    )


def _load_wishlist_profile(db: Session, current_user: models.User) -> schemas.WishlistProfileResponse:
    service_ids = _dedupe_non_empty_ids(current_user.favorite_service_ids or [])
    favorite_id_set = set(service_ids)
    group_rows = db.query(models.WishlistGroup).filter(
        models.WishlistGroup.user_id == current_user.id
    ).order_by(
        models.WishlistGroup.sort_order.asc(),
        models.WishlistGroup.created_at.asc(),
        models.WishlistGroup.id.asc()
    ).all()

    group_ids = [row.id for row in group_rows]
    items_by_group: dict[str, List[str]] = {group_id: [] for group_id in group_ids}
    if group_ids:
        item_rows = db.query(models.WishlistGroupItem).filter(
            models.WishlistGroupItem.user_id == current_user.id,
            models.WishlistGroupItem.group_id.in_(group_ids)
        ).order_by(
            models.WishlistGroupItem.created_at.asc(),
            models.WishlistGroupItem.id.asc()
        ).all()
        for item in item_rows:
            items_by_group.setdefault(item.group_id, []).append(item.service_id)

    assigned_ids = set()
    groups: List[tuple[str, List[str]]] = []
    default_group_index: Optional[int] = None
    for row in group_rows:
        name = _normalize_wishlist_group_name(row.name)
        if name == DEFAULT_WISHLIST_GROUP_NAME and default_group_index is None:
            default_group_index = len(groups)
        group_service_ids: List[str] = []
        for service_id in items_by_group.get(row.id, []):
            if service_id in favorite_id_set and service_id not in assigned_ids:
                group_service_ids.append(service_id)
                assigned_ids.add(service_id)
        groups.append((name, group_service_ids))

    unassigned_ids = [service_id for service_id in service_ids if service_id not in assigned_ids]
    if default_group_index is None:
        default_group_index = 0
        groups.insert(0, (DEFAULT_WISHLIST_GROUP_NAME, []))

    if unassigned_ids:
        default_name, default_service_ids = groups[default_group_index]
        groups[default_group_index] = (default_name, default_service_ids + unassigned_ids)

    deduped_groups: List[tuple[str, List[str]]] = []
    seen_group_names = set()
    for name, group_service_ids in groups:
        if name in seen_group_names:
            continue
        seen_group_names.add(name)
        deduped_groups.append((name, group_service_ids))

    return _build_wishlist_profile_response(service_ids, deduped_groups)


def _persist_wishlist_profile(
    db: Session,
    current_user: models.User,
    service_ids: List[str],
    groups: Optional[List[schemas.WishlistGroupPayload]] = None
) -> schemas.WishlistProfileResponse:
    normalized_service_ids = _filter_existing_service_ids(db, service_ids)
    normalized_service_id_set = set(normalized_service_ids)
    source_groups = groups if groups is not None else _load_wishlist_profile(db, current_user).groups

    normalized_groups: List[tuple[str, List[str]]] = []
    seen_group_names = set()
    assigned_ids = set()
    for group in source_groups:
        name = _normalize_wishlist_group_name(group.name)
        if name in seen_group_names:
            continue
        seen_group_names.add(name)
        group_service_ids: List[str] = []
        for service_id in _dedupe_non_empty_ids(group.service_ids):
            if service_id not in normalized_service_id_set or service_id in assigned_ids:
                continue
            group_service_ids.append(service_id)
            assigned_ids.add(service_id)
        normalized_groups.append((name, group_service_ids))

    if DEFAULT_WISHLIST_GROUP_NAME not in seen_group_names:
        normalized_groups.insert(0, (DEFAULT_WISHLIST_GROUP_NAME, []))

    default_index = next(
        (index for index, (name, _) in enumerate(normalized_groups) if name == DEFAULT_WISHLIST_GROUP_NAME),
        0
    )
    unassigned_ids = [service_id for service_id in normalized_service_ids if service_id not in assigned_ids]
    if unassigned_ids:
        default_name, default_service_ids = normalized_groups[default_index]
        normalized_groups[default_index] = (default_name, default_service_ids + unassigned_ids)

    timestamp = int(time.time() * 1000)
    current_user.favorite_service_ids = normalized_service_ids
    current_user.updated_at = timestamp

    db.query(models.WishlistGroupItem).filter(
        models.WishlistGroupItem.user_id == current_user.id
    ).delete(synchronize_session=False)
    db.query(models.WishlistGroup).filter(
        models.WishlistGroup.user_id == current_user.id
    ).delete(synchronize_session=False)

    for index, (name, group_service_ids) in enumerate(normalized_groups):
        group_id = uuid.uuid4().hex
        db.add(models.WishlistGroup(
            id=group_id,
            user_id=current_user.id,
            name=name,
            sort_order=index,
            created_at=timestamp,
            updated_at=timestamp
        ))
        for service_id in group_service_ids:
            db.add(models.WishlistGroupItem(
                id=uuid.uuid4().hex,
                user_id=current_user.id,
                group_id=group_id,
                service_id=service_id,
                created_at=timestamp,
                updated_at=timestamp
            ))

    return _build_wishlist_profile_response(normalized_service_ids, normalized_groups)


def _build_upload_filename(user_id: str, upload_file: UploadFile, force_ext: Optional[str] = None) -> str:
    if force_ext:
        ext = force_ext
    else:
        ext = upload_file.filename.split(".")[-1] if upload_file.filename and "." in upload_file.filename else "bin"
    return f"{user_id}_{uuid.uuid4().hex}.{ext}"


def _build_media_object_key(biz: str, user_id: str, filename: str) -> str:
    return f"{biz}s/{user_id}/{filename}"


def _build_media_storage_path(biz: str, user_id: str, filename: str) -> str:
    user_dir = os.path.join(MEDIA_BIZ_TO_DIR[biz], user_id)
    os.makedirs(user_dir, exist_ok=True)
    return os.path.join(user_dir, filename)


def _save_upload_file_with_limit(upload_file: UploadFile, file_path: str, max_bytes: int) -> int:
    total_size = 0
    chunk_size = 1024 * 1024
    with open(file_path, "wb") as buffer:
        while True:
            chunk = upload_file.file.read(chunk_size)
            if not chunk:
                break
            total_size += len(chunk)
            if total_size > max_bytes:
                buffer.close()
                try:
                    os.remove(file_path)
                except OSError:
                    pass
                raise HTTPException(
                    status_code=413,
                    detail=f"File too large, max size is {max_bytes // (1024 * 1024)}MB"
                )
            buffer.write(chunk)
    return total_size


def _upload_media(
    file: UploadFile,
    current_user: models.User,
    biz: str,
    image_only: bool = False
) -> schemas.MediaUploadResponse:
    content_type = file.content_type or ""
    if not content_type:
        raise HTTPException(status_code=400, detail="Missing content type")
    if image_only and not content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File provided is not an image.")
    if biz not in MEDIA_BIZ_TO_DIR:
        raise HTTPException(status_code=400, detail="Unsupported media biz type")

    force_ext = None
    if biz == "avatar":
        force_ext = file.filename.split(".")[-1] if file.filename and "." in file.filename else "jpg"
    new_filename = _build_upload_filename(current_user.id, file, force_ext=force_ext)
    file_path = _build_media_storage_path(biz, current_user.id, new_filename)
    size = _save_upload_file_with_limit(file, file_path, MAX_UPLOAD_SIZE_BYTES)

    if biz == "avatar":
        object_key = _build_media_object_key("avatar", current_user.id, new_filename)
        timestamp = int(time.time())
        url = f"static/{object_key}?t={timestamp}"
    elif biz == "service":
        object_key = _build_media_object_key("service", current_user.id, new_filename)
        url = f"static/{object_key}"
    elif biz == "experience":
        object_key = _build_media_object_key("experience", current_user.id, new_filename)
        url = f"static/{object_key}"
    else:
        object_key = f"chat/{new_filename}"
        url = f"static/{object_key}"

    return schemas.MediaUploadResponse(
        url=url,
        object_key=object_key,
        mime_type=content_type,
        size=size
    )

def get_ws_user(token: str, db: Session) -> models.User:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid authentication credentials")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid authentication credentials")
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return user


def get_direct_peer(conversation: models.Conversation, current_user_id: str, db: Session):
    peer_ids = [item for item in (conversation.participant_ids or []) if item != current_user_id]
    if not peer_ids:
        return None
    return db.query(models.User).filter(models.User.id == peer_ids[0]).first()


def calculate_service_years_from_first_service(db: Session, user_id: str) -> int:
    first_service_created_at = db.query(func.min(models.Service.created_at)).filter(
        models.Service.creator_id == user_id,
        models.Service.is_deleted == False
    ).scalar()
    if not first_service_created_at:
        return 0

    first_dt = datetime.fromtimestamp(first_service_created_at / 1000.0)
    now_dt = datetime.now()
    years = now_dt.year - first_dt.year
    if (now_dt.month, now_dt.day) < (first_dt.month, first_dt.day):
        years -= 1
    return max(0, years)


def hydrate_user_profile_metrics(db: Session, user: models.User):
    # 服务经验按首次发布服务日期动态计算，不依赖静态字段
    user.service_years = calculate_service_years_from_first_service(db, user.id)
    return user


def user_schema_for_viewer(db: Session, orm_user: models.User, viewer_id: str) -> schemas.User:
    """
    将 ORM 用户转为响应 Schema；对他人且开启身高体重隐私时，抹掉具体数值（仍保留 privacy 标记供客户端展示「已隐藏」）。
    """
    hydrated = hydrate_user_profile_metrics(db, orm_user)
    su = schemas.User.model_validate(hydrated, from_attributes=True)
    if viewer_id != hydrated.id and getattr(hydrated, "height_weight_private", False):
        dumped = su.model_dump()
        dumped["height_cm"] = 0
        dumped["weight_kg"] = 0.0
        return schemas.User(**dumped)
    return su


def serialize_conversation_item(conversation: models.Conversation, current_user: models.User, db: Session):
    member = db.query(models.ConversationMember).filter(
        models.ConversationMember.conversation_id == conversation.id,
        models.ConversationMember.user_id == current_user.id
    ).first()
    peer = get_direct_peer(conversation, current_user.id, db) if conversation.type == "direct" else None
    return schemas.ConversationListItem(
        conversation=conversation,
        member=member,
        peer=peer,
    )


def ensure_conversation_member(conversation_id: str, current_user: models.User, db: Session):
    member = db.query(models.ConversationMember).filter(
        models.ConversationMember.conversation_id == conversation_id,
        models.ConversationMember.user_id == current_user.id
    ).first()
    if member is None:
        raise HTTPException(status_code=403, detail="Not authorized to access this conversation")
    conversation = db.query(models.Conversation).filter(
        models.Conversation.id == conversation_id,
        models.Conversation.is_deleted == False
    ).first()
    if conversation is None:
        raise HTTPException(status_code=404, detail="Conversation not found")
    return conversation, member


def build_message_preview(message_type: str, content: str, attachment_name: str = "") -> str:
    if message_type == "image":
        return "[图片]"
    if message_type == "location":
        return content or "[位置]"
    if message_type == "service_card":
        return content or "[服务卡片]"
    if message_type == "file":
        return attachment_name or "[文件]"
    if message_type == "system":
        return content or "[系统消息]"
    return content or ""


def get_or_create_direct_conversation(db: Session, current_user: models.User, peer_id: str):
    if peer_id == current_user.id:
        raise HTTPException(status_code=400, detail="Cannot create chat with self")
    peer = db.query(models.User).filter(models.User.id == peer_id).first()
    if peer is None:
        raise HTTPException(status_code=404, detail="User not found")

    for conversation in db.query(models.Conversation).filter(
        models.Conversation.type == "direct",
        models.Conversation.is_deleted == False
    ).all():
        participants = set(conversation.participant_ids or [])
        if participants == {current_user.id, peer_id}:
            return conversation

    now = int(time.time() * 1000)
    conversation = models.Conversation(
        id=str(uuid.uuid4()),
        type="direct",
        title="",
        owner_id=current_user.id,
        avatar_url="",
        participant_ids=[current_user.id, peer_id],
        created_at=now,
        updated_at=now,
        last_message_at=now,
    )
    db.add(conversation)
    db.flush()

    for user_id in [current_user.id, peer_id]:
        db.add(models.ConversationMember(
            id=str(uuid.uuid4()),
            conversation_id=conversation.id,
            user_id=user_id,
            role="owner" if user_id == current_user.id else "member",
            unread_count=0,
            joined_at=now,
            updated_at=now,
        ))
    db.commit()
    db.refresh(conversation)
    return conversation


# 创建所有数据库表
# 如果表已存在，不会重新创建
models.Base.metadata.create_all(bind=engine)
sync_sqlite_schema(engine, models.Base.metadata)

app = FastAPI()

# --- 静态资源目录配置 ---
os.makedirs(AVATAR_DIR, exist_ok=True)
os.makedirs(CHAT_MEDIA_DIR, exist_ok=True)
os.makedirs(SERVICE_MEDIA_DIR, exist_ok=True)
os.makedirs(EXPERIENCE_MEDIA_DIR, exist_ok=True)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


@app.on_event("startup")
async def on_startup():
    await chat_hub.startup()


@app.on_event("shutdown")
async def on_shutdown():
    await chat_hub.shutdown()

# --- 用户相关 API (User APIs) ---

@app.post("/users/register", response_model=schemas.User)
def register_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    """
    用户注册
    """
    try:
        # 1. 检查手机号是否已存在
        if user.phone_number:
            existing_user = db.query(models.User).filter(models.User.phone_number == user.phone_number).first()
            if existing_user:
                raise HTTPException(status_code=400, detail="Phone number already registered")
                
        # 2. 检查 ID 是否已存在 (如果客户端提供了 ID)
        existing_user_id = db.query(models.User).filter(models.User.id == user.id).first()
        if existing_user_id:
            raise HTTPException(status_code=400, detail="User ID already exists")

        # 3. 创建用户对象
        hashed_password = get_password_hash(user.password)
        user_dict = user.dict(exclude={"password"})
        user_dict["hashed_password"] = hashed_password
        
        # 手动设置时间戳，防止 Pydantic 响应模型校验失败
        timestamp = int(time.time() * 1000)
        user_dict["created_at"] = timestamp
        user_dict["updated_at"] = timestamp
        
        print(f"DEBUG: Creating user with dict: {user_dict}")
        
        db_user = models.User(**user_dict)
        db.add(db_user)
        
        db.commit()
        db.refresh(db_user)
        print("DEBUG: User created successfully")
        return hydrate_user_profile_metrics(db, db_user)

    except HTTPException as he:
        raise he
    except Exception as e:
        import traceback
        traceback.print_exc()
        db.rollback()
        raise HTTPException(status_code=400, detail=f"Failed to register user: {str(e)}")

@app.post("/users/login", response_model=schemas.User)
def login_user(login_data: schemas.UserLogin, db: Session = Depends(get_db)):
    """
    用户登录
    """
    print(f"DEBUG: Login attempt for phone: {login_data.phone_number}")
    user = db.query(models.User).filter(models.User.phone_number == login_data.phone_number).first()
    if not user:
        print("DEBUG: User not found")
        raise HTTPException(status_code=404, detail="User not found")
        
    if not user.hashed_password:
        print("DEBUG: Password not set")
        # 如果是旧用户没有密码，可能需要特殊处理，或者提示重置密码
        raise HTTPException(status_code=400, detail="Password not set for this user")
        
    print(f"DEBUG: Verifying password. Input: {login_data.password}, Stored Hash: {user.hashed_password}")
    if not verify_password(login_data.password, user.hashed_password):
        print("DEBUG: Incorrect password")
        raise HTTPException(status_code=400, detail="Incorrect password")
        
    print("DEBUG: Login successful")
    return hydrate_user_profile_metrics(db, user)

@app.get("/users/search", response_model=schemas.User)
def search_user(query: str, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    """
    根据手机号或lulu号搜索用户
    """
    user = db.query(models.User).filter(
        or_(models.User.phone_number == query, models.User.pei_pei_id == query)
    ).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return user_schema_for_viewer(db, user, current_user.id)

@app.post("/users/match_contacts", response_model=List[schemas.User])
def match_contacts(request: schemas.ContactMatchRequest, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    """
    批量匹配通讯录中的手机号
    """
    if not request.phone_numbers:
        return []
        
    # Query all users where phone_number is in the provided list
    users = db.query(models.User).filter(
        models.User.phone_number.in_(request.phone_numbers)
    ).all()
    
    return [user_schema_for_viewer(db, item, current_user.id) for item in users]

@app.get("/users/{user_id}", response_model=schemas.User)
def read_user(
    user_id: str,
    db: Session = Depends(get_db),
    current_user: Optional[models.User] = Depends(get_optional_current_user),
):
    """
    根据用户 ID 获取用户信息。
    允许匿名访问：与 /services/discovery 一致，冷启动仅有本地 demo 用户、尚未拿 token 时仍可打开他人主页。
    """
    db_user = db.query(models.User).filter(models.User.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    viewer_id = current_user.id if current_user is not None else ""
    return user_schema_for_viewer(db, db_user, viewer_id)


@app.get("/users/discovery", response_model=List[schemas.User])
def discovery_users(
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    users = db.query(models.User).filter(
        models.User.id != current_user.id
    ).order_by(desc(models.User.updated_at), desc(models.User.created_at)).limit(limit).all()
    return [user_schema_for_viewer(db, item, current_user.id) for item in users]

@app.post("/upload/avatar")
async def upload_avatar(file: UploadFile = File(...), user_id: Optional[str] = Form(None), current_user: models.User = Depends(get_current_user)):
    """
    上传头像文件，返回文件的网络访问 URL
    """
    if user_id is not None:
        ensure_self_access(current_user, user_id)
    else:
        user_id = current_user.id
    result = _upload_media(file=file, current_user=current_user, biz="avatar", image_only=True)
    return {"url": result.url}


@app.post("/media/upload", response_model=schemas.MediaUploadResponse)
async def upload_media(
    biz: str = Query(..., pattern="^(avatar|service|chat)$"),
    file: UploadFile = File(...),
    current_user: models.User = Depends(get_current_user)
):
    return _upload_media(file=file, current_user=current_user, biz=biz, image_only=(biz == "avatar"))

@app.put("/users/{user_id}", response_model=schemas.User)
def update_user(user_id: str, user: schemas.User, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    """
    更新用户信息
    """
    ensure_self_access(current_user, user_id)
    if user.id != user_id:
        raise HTTPException(status_code=400, detail="User ID mismatch")
    db_user = db.query(models.User).filter(models.User.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Update fields
    user_data = user.dict(exclude_unset=True)
    # Filter out fields that shouldn't be updated directly via this endpoint if any
    # e.g., hashed_password usually via separate endpoint, but here User schema might not have it
    
    for key, value in user_data.items():
        # Skip id and other immutable fields if necessary
        if key == "id":
            continue
        setattr(db_user, key, value)
    
    db_user.updated_at = int(time.time() * 1000)
    
    try:
        db.commit()
        db.refresh(db_user)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
        
    return hydrate_user_profile_metrics(db, db_user)

@app.post("/auth/token", response_model=schemas.Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.phone_number == form_data.username).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if not user.hashed_password:
        raise HTTPException(status_code=400, detail="Password not set for this user")
    if not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(status_code=400, detail="Incorrect password")
    access_token = create_access_token({"sub": user.id})
    return {"access_token": access_token, "token_type": "bearer"}

@app.get("/auth/me", response_model=schemas.User)
def read_me(current_user: models.User = Depends(get_current_user)):
    db = SessionLocal()
    try:
        fresh_user = db.query(models.User).filter(models.User.id == current_user.id).first()
        if fresh_user is None:
            raise HTTPException(status_code=404, detail="User not found")
        return hydrate_user_profile_metrics(db, fresh_user)
    finally:
        db.close()


@app.get("/me/wishlist/services", response_model=schemas.FavoriteServicesResponse)
def get_my_favorite_services(current_user: models.User = Depends(get_current_user)):
    return schemas.FavoriteServicesResponse(service_ids=current_user.favorite_service_ids or [])


@app.get("/me/wishlist/profile", response_model=schemas.WishlistProfileResponse)
def get_my_wishlist_profile(
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    return _load_wishlist_profile(db, current_user)


@app.put("/me/wishlist/services", response_model=schemas.FavoriteServicesResponse)
def update_my_favorite_services(
    payload: schemas.FavoriteServicesUpdateRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    response = _persist_wishlist_profile(db, current_user, payload.service_ids)
    try:
        db.commit()
        db.refresh(current_user)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return schemas.FavoriteServicesResponse(service_ids=response.service_ids)


@app.put("/me/wishlist/profile", response_model=schemas.WishlistProfileResponse)
def update_my_wishlist_profile(
    payload: schemas.WishlistProfileUpdateRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    response = _persist_wishlist_profile(db, current_user, payload.service_ids, payload.groups)
    try:
        db.commit()
        db.refresh(current_user)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return response

@app.post("/service/upload", response_model=schemas.ChatUploadResponse)
async def upload_service_file(
    file: UploadFile = File(...),
    current_user: models.User = Depends(get_current_user)
):
    result = _upload_media(file=file, current_user=current_user, biz="service")
    return schemas.ChatUploadResponse(
        url=result.url,
        object_key=result.object_key
    )


@app.post("/experience/upload", response_model=schemas.ChatUploadResponse)
async def upload_experience_file(
    file: UploadFile = File(...),
    current_user: models.User = Depends(get_current_user)
):
    result = _upload_media(file=file, current_user=current_user, biz="experience")
    return schemas.ChatUploadResponse(
        url=result.url,
        object_key=result.object_key
    )

@app.post("/services/", response_model=schemas.Service)
def create_service(
    service: schemas.ServiceCreate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    ensure_field_matches_current_user(current_user, service.creator_id, "creator_id")
    db_item = models.Service(**service.model_dump())
    db.add(db_item)
    try:
        db.commit()
        db.refresh(db_item)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return db_item

@app.put("/services/{service_id}", response_model=schemas.Service)
def update_service(
    service_id: str,
    service: schemas.ServiceCreate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    db_item = db.query(models.Service).filter(
        models.Service.id == service_id,
        models.Service.is_deleted == False
    ).first()
    if db_item is None:
        raise HTTPException(status_code=404, detail="Service not found")
    ensure_record_owner(db_item, current_user, "creator_id")
    ensure_field_matches_current_user(current_user, service.creator_id, "creator_id")

    update_data = service.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        if key in {"id", "creator_id", "created_at"}:
            continue
        setattr(db_item, key, value)
    db_item.updated_at = int(time.time() * 1000)

    try:
        db.commit()
        db.refresh(db_item)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return db_item

@app.get("/services/", response_model=List[schemas.Service])
def read_services(
    user_id: Optional[str] = None,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    owner_id = ensure_optional_self_access(current_user, user_id)
    query = db.query(models.Service).filter(models.Service.is_deleted == False)
    if owner_id:
        query = query.filter(models.Service.creator_id == owner_id)
    return query.order_by(desc(models.Service.updated_at), desc(models.Service.created_at)).offset(skip).limit(limit).all()


@app.get("/services/discovery", response_model=List[schemas.Service])
def list_square_discovery_services(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=200),
    updated_after: Optional[int] = Query(None, ge=0),
    db: Session = Depends(get_db),
):
    """
    广场发现：全站未删除的服务（按更新时间倒序）。
    与客户端「发现」瀑布流一致；是否仅展示「同步到广场」可由后续产品开关再收紧。
    不强制登录：客户端冷启动本地 demo 用户尚未拿 token 时也能拉首屏；本接口不按用户过滤。
    必须声明在 /services/{service_id} 之前，避免 path 将 discovery 解析为 id。
    """
    query = db.query(models.Service).filter(models.Service.is_deleted == False)
    if updated_after is not None:
        query = query.filter(models.Service.updated_at > updated_after)
    items = (
        query.order_by(desc(models.Service.updated_at), desc(models.Service.created_at))
        .offset(skip)
        .limit(limit)
        .all()
    )
    return items


@app.get("/services/{service_id}", response_model=schemas.Service)
def read_service_detail(
    service_id: str,
    db: Session = Depends(get_db),
    current_user: Optional[models.User] = Depends(get_optional_current_user)
):
    db_item = db.query(models.Service).filter(
        models.Service.id == service_id,
        models.Service.is_deleted == False
    ).first()
    if db_item is None:
        raise HTTPException(status_code=404, detail="Service not found")
    return db_item

@app.delete("/services/{service_id}", response_model=schemas.Service)
def delete_service(
    service_id: str,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    db_item = db.query(models.Service).filter(models.Service.id == service_id).first()
    if db_item is None:
        raise HTTPException(status_code=404, detail="Service not found")
    ensure_record_owner(db_item, current_user, "creator_id")
    db_item.is_deleted = True
    db_item.updated_at = int(time.time() * 1000)
    try:
        db.commit()
        db.refresh(db_item)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return db_item


@app.post("/experiences/", response_model=schemas.Experience)
def create_experience(
    experience: schemas.ExperienceCreate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    ensure_field_matches_current_user(current_user, experience.host_id, "host_id")
    db_item = models.Experience(**experience.model_dump())
    db.add(db_item)
    try:
        db.commit()
        db.refresh(db_item)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return db_item


@app.put("/experiences/{experience_id}", response_model=schemas.Experience)
def update_experience(
    experience_id: str,
    experience: schemas.ExperienceCreate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    db_item = db.query(models.Experience).filter(
        models.Experience.id == experience_id,
        models.Experience.is_deleted == False
    ).first()
    if db_item is None:
        raise HTTPException(status_code=404, detail="Experience not found")
    ensure_record_owner(db_item, current_user, "host_id")
    ensure_field_matches_current_user(current_user, experience.host_id, "host_id")

    update_data = experience.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        if key in {"id", "host_id", "created_at"}:
            continue
        setattr(db_item, key, value)
    db_item.updated_at = int(time.time() * 1000)

    try:
        db.commit()
        db.refresh(db_item)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return db_item


@app.get("/experiences/", response_model=List[schemas.Experience])
def read_experiences(
    user_id: Optional[str] = None,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    owner_id = ensure_optional_self_access(current_user, user_id)
    query = db.query(models.Experience).filter(models.Experience.is_deleted == False)
    if owner_id:
        query = query.filter(models.Experience.host_id == owner_id)
    return query.order_by(desc(models.Experience.updated_at), desc(models.Experience.created_at)).offset(skip).limit(limit).all()


@app.get("/experiences/discovery", response_model=List[schemas.Experience])
def list_square_discovery_experiences(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=200),
    updated_after: Optional[int] = Query(None, ge=0),
    db: Session = Depends(get_db),
):
    query = db.query(models.Experience).filter(models.Experience.is_deleted == False)
    if updated_after is not None:
        query = query.filter(models.Experience.updated_at > updated_after)
    return (
        query.order_by(desc(models.Experience.updated_at), desc(models.Experience.created_at))
        .offset(skip)
        .limit(limit)
        .all()
    )


@app.get("/experiences/{experience_id}", response_model=schemas.Experience)
def read_experience_detail(
    experience_id: str,
    db: Session = Depends(get_db),
    current_user: Optional[models.User] = Depends(get_optional_current_user)
):
    db_item = db.query(models.Experience).filter(
        models.Experience.id == experience_id,
        models.Experience.is_deleted == False
    ).first()
    if db_item is None:
        raise HTTPException(status_code=404, detail="Experience not found")
    return db_item


@app.delete("/experiences/{experience_id}", response_model=schemas.Experience)
def delete_experience(
    experience_id: str,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    db_item = db.query(models.Experience).filter(models.Experience.id == experience_id).first()
    if db_item is None:
        raise HTTPException(status_code=404, detail="Experience not found")
    ensure_record_owner(db_item, current_user, "host_id")
    db_item.is_deleted = True
    db_item.updated_at = int(time.time() * 1000)
    try:
        db.commit()
        db.refresh(db_item)
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(e))
    return db_item

@app.get("/")
def read_root():
    """
    根路径，用于检查 API 是否运行
    """
    return {"message": "Lulu API is running"}

@app.get("/healthz")
def healthz():
    return {"status": "ok", "environment": APP_ENV}

if __name__ == "__main__":
    import uvicorn

    host = os.getenv("LULU_HOST", "0.0.0.0")
    port = int(os.getenv("LULU_PORT", "8000"))
    uvicorn.run(app, host=host, port=port)


@app.post("/chat/upload", response_model=schemas.ChatUploadResponse)
async def upload_chat_file(
    file: UploadFile = File(...),
    current_user: models.User = Depends(get_current_user)
):
    result = _upload_media(file=file, current_user=current_user, biz="chat")
    return schemas.ChatUploadResponse(
        url=result.url,
        object_key=result.object_key
    )


@app.post("/chat/conversations/direct/{peer_id}", response_model=schemas.ConversationListItem)
def create_direct_conversation(
    peer_id: str,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    conversation = get_or_create_direct_conversation(db, current_user, peer_id)
    return serialize_conversation_item(conversation, current_user, db)


@app.get("/chat/conversations", response_model=List[schemas.ConversationListItem])
def list_conversations(
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    members = db.query(models.ConversationMember).filter(
        models.ConversationMember.user_id == current_user.id
    ).all()
    conversation_ids = [member.conversation_id for member in members]
    if not conversation_ids:
        return []
    conversations = db.query(models.Conversation).filter(
        models.Conversation.id.in_(conversation_ids),
        models.Conversation.is_deleted == False
    ).order_by(desc(models.Conversation.last_message_at), desc(models.Conversation.updated_at)).all()
    return [serialize_conversation_item(item, current_user, db) for item in conversations]


@app.get("/chat/conversations/{conversation_id}/messages", response_model=List[schemas.ChatMessage])
def list_chat_messages(
    conversation_id: str,
    limit: int = Query(100, ge=1, le=500),
    before: Optional[int] = Query(None),
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    ensure_conversation_member(conversation_id, current_user, db)
    query = db.query(models.ChatMessage).filter(
        models.ChatMessage.conversation_id == conversation_id,
        models.ChatMessage.is_deleted == False
    )
    if before is not None:
        query = query.filter(models.ChatMessage.created_at < before)
    items = query.order_by(desc(models.ChatMessage.created_at)).limit(limit).all()
    return list(reversed(items))


@app.post("/chat/conversations/{conversation_id}/messages", response_model=schemas.ChatMessage)
async def send_chat_message(
    conversation_id: str,
    payload: schemas.ChatMessageCreate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    conversation, _ = ensure_conversation_member(conversation_id, current_user, db)
    now = int(time.time() * 1000)
    message = models.ChatMessage(
        id=str(uuid.uuid4()),
        conversation_id=conversation_id,
        sender_id=current_user.id,
        type=payload.type,
        content=payload.content,
        attachment_url=payload.attachment_url,
        attachment_name=payload.attachment_name,
        attachment_size=payload.attachment_size,
        object_key=payload.object_key,
        extra=payload.extra or {},
        client_message_id=payload.client_message_id,
        status="sent",
        created_at=now,
        updated_at=now,
    )
    db.add(message)
    preview = build_message_preview(payload.type, payload.content, payload.attachment_name)
    conversation.last_message_id = message.id
    conversation.last_message_preview = preview
    conversation.last_message_at = now
    conversation.updated_at = now

    participants = conversation.participant_ids or []
    members = db.query(models.ConversationMember).filter(
        models.ConversationMember.conversation_id == conversation_id
    ).all()
    for member in members:
        if member.user_id == current_user.id:
            member.last_read_message_id = message.id
            member.last_read_at = now
        else:
            member.unread_count = (member.unread_count or 0) + 1
        member.updated_at = now
    db.commit()
    db.refresh(message)

    event = {
        "type": "chat_message",
        "conversation_id": conversation_id,
        "message": schemas.ChatMessage.model_validate(message).model_dump(mode="json"),
    }
    await chat_hub.publish_to_users(participants, event)
    return message


@app.post("/chat/conversations/{conversation_id}/read", response_model=schemas.ConversationMember)
async def mark_conversation_read(
    conversation_id: str,
    payload: schemas.ChatMessageReadRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    conversation, member = ensure_conversation_member(conversation_id, current_user, db)
    now = int(time.time() * 1000)
    message_id = payload.message_id or conversation.last_message_id
    member.unread_count = 0
    member.last_read_message_id = message_id
    member.last_read_at = now
    member.updated_at = now
    db.commit()
    db.refresh(member)
    await chat_hub.publish_to_users(
        conversation.participant_ids or [],
        {
            "type": "conversation_read",
            "conversation_id": conversation_id,
            "user_id": current_user.id,
            "message_id": message_id,
            "read_at": now,
        }
    )
    return member


@app.websocket("/ws/chat")
async def chat_websocket(websocket: WebSocket, token: str):
    db = SessionLocal()
    user = None
    try:
        user = get_ws_user(token, db)
        await websocket.accept()
        await chat_hub.register(user.id, websocket)
        await websocket.send_json({"type": "connected", "user_id": user.id})
        while True:
            raw = await websocket.receive_text()
            try:
                payload = json.loads(raw)
            except Exception:
                await websocket.send_json({"type": "error", "detail": "Invalid JSON"})
                continue
            if payload.get("type") == "ping":
                await websocket.send_json({"type": "pong"})
    except WebSocketDisconnect:
        pass
    except HTTPException:
        try:
            await websocket.close(code=4401)
        except Exception:
            pass
    finally:
        if user is not None:
            await chat_hub.unregister(user.id, websocket)
        db.close()
