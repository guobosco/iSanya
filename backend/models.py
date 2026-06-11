from sqlalchemy import Boolean, Column, ForeignKey, Integer, String, JSON, BigInteger, Float, Text
from sqlalchemy.orm import relationship
try:
    from .database import Base
except ImportError:
    from database import Base

class User(Base):
    """
    用户模型 (User Model)
    存储用户的核心身份信息、个人资料和必要的系统字段。
    """
    __tablename__ = "users"

    # --- 核心身份信息 (Core Identity) ---
    # 用户唯一标识，UUID 或服务端生成的 ID
    id = Column(String, primary_key=True, index=True) 
    # lulu号，用户可见的唯一 ID，用于搜索添加好友等
    pei_pei_id = Column(String, unique=True, index=True) 
    # 用户昵称
    name = Column(String, index=True)
    # 邮箱地址，唯一，可选
    email = Column(String, unique=True, index=True, nullable=True)
    # 手机号码，可选
    phone_number = Column(String, index=True, nullable=True)
    # 密码哈希
    hashed_password = Column(String, nullable=True)
    
    # --- 个人资料 (Profile Info) ---
    # 头像 URL
    photo_url = Column(String, nullable=True)
    # 备注名
    remark_name = Column(String, default="")
    # 标签列表，存储为 JSON 数组
    tags = Column(JSON, default=[]) 
    # 收藏的服务 ID 列表（心愿单）
    favorite_service_ids = Column(JSON, default=[])
    # 个性签名
    signature = Column(String, default="")
    # 备忘录/备注信息
    memo = Column(String, default="")
    # 性别
    gender = Column(String, default="")
    # 地区
    region = Column(String, default="")
    # 工作
    job_title = Column(String, default="")
    # 教育背景
    education = Column(String, default="")
    # 出生年代/年龄段（约五年一档，如 90后、95后）
    birth_decade = Column(String, default="")
    # 身高（厘米）、体重（千克）；0 表示未填
    height_cm = Column(Integer, default=0)
    weight_kg = Column(Float, default=0.0)
    # 对他人隐藏身高体重（本人仍可见）
    height_weight_private = Column(Boolean, default=False)
    # 中学时最喜欢的歌曲
    middle_school_favorite_song = Column(String, default="")
    # 掌握语言
    spoken_languages = Column(JSON, default=[])
    # 常住城市
    living_city = Column(String, default="")
    # 常住国家
    living_country = Column(String, default="")
    # 身份认证状态
    identity_verified = Column(Boolean, default=False)
    # 评价条数
    review_count = Column(Integer, default=0)
    # 平均评分
    average_rating = Column(Float, default=0.0)
    # 服务经验（年）
    service_years = Column(Integer, default=0)
    # 评价摘要
    review_summaries = Column(JSON, default=[])
    
    # --- 状态与标志 (Status & Flags) ---
    # 手机号是否已验证
    is_phone_verified = Column(Boolean, default=False)
    # 个人资料是否已完善
    is_profile_completed = Column(Boolean, default=False)
    
    # --- 地址信息 (Address Info) ---
    # 详细地址
    address_detail = Column(String, default="")
    # 收件人姓名
    address_recipient_name = Column(String, default="")
    # 收件人电话
    address_phone_number = Column(String, default="")
    
    # --- 逻辑控制 (Logic Controls) ---
    # ID 修改次数
    id_modification_count = Column(Integer, default=0)
    # 上次修改 ID 的年份
    last_id_modification_year = Column(Integer, default=0)
    
    # 创建时间戳
    created_at = Column(BigInteger)
    # 更新时间戳
    updated_at = Column(BigInteger)
    is_synced = Column(Boolean, default=True)

    # --- 关系 (Relationships) ---
    # 用户发布的服务
    services = relationship("Service", back_populates="creator_user")

class Service(Base):
    """
    服务模型 (Service Model)
    存储用户发布的服务信息，用于服务详情、列表展示和聊天服务卡片分享。
    """
    __tablename__ = "services"

    id = Column(String, primary_key=True, index=True)
    title = Column(String, default="")
    description = Column(Text, default="")
    cover_image_url = Column(String, default="")
    image_urls = Column(JSON, default=[])
    location = Column(String, default="")
    price_text = Column(String, default="")
    # 计价说明（与客户端 price_basis_text 一致）
    price_basis_text = Column(String, default="")
    prepayment_percent = Column(Integer, default=30)
    full_refund_cancel_lead_days = Column(Integer, default=1)
    # 服务类目（与客户端 category / ServiceCategories 一致）
    category = Column(String, default="其他服务")
    service_mode = Column(String, default="")
    service_time = Column(BigInteger, default=0)
    booking_time_ranges_json = Column(String, default="")
    booking_lead_hours = Column(Float, default=0.0)
    booking_future_open_days = Column(Integer, default=30, nullable=False)
    auto_accept_after_payment = Column(Boolean, default=True)
    sync_to_square = Column(Boolean, default=False)
    # 用户追加的服务声明（不含平台默认四条，由客户端合并展示）
    service_declarations_extra = Column(JSON, default=[])
    participant_ids = Column(JSON, default=[])
    is_important = Column(Boolean, default=False)
    creator_id = Column(String, ForeignKey("users.id"), index=True)
    creator = Column(String, default="")
    created_at = Column(BigInteger, default=0)
    updated_at = Column(BigInteger, default=0)
    is_deleted = Column(Boolean, default=False)
    is_synced = Column(Boolean, default=True)

    creator_user = relationship("User", back_populates="services")

class Conversation(Base):
    __tablename__ = "conversations"

    id = Column(String, primary_key=True, index=True)
    type = Column(String, default="direct", index=True)  # direct | group | service
    title = Column(String, default="")
    owner_id = Column(String, ForeignKey("users.id"), nullable=True)
    avatar_url = Column(String, default="")
    participant_ids = Column(JSON, default=[])
    last_message_id = Column(String, nullable=True)
    last_message_preview = Column(String, default="")
    last_message_at = Column(BigInteger, default=0)
    created_at = Column(BigInteger, default=0)
    updated_at = Column(BigInteger, default=0)
    is_deleted = Column(Boolean, default=False)


class ConversationMember(Base):
    __tablename__ = "conversation_members"

    id = Column(String, primary_key=True, index=True)
    conversation_id = Column(String, ForeignKey("conversations.id"), index=True)
    user_id = Column(String, ForeignKey("users.id"), index=True)
    role = Column(String, default="member")
    unread_count = Column(Integer, default=0)
    last_read_message_id = Column(String, nullable=True)
    last_read_at = Column(BigInteger, default=0)
    is_muted = Column(Boolean, default=False)
    joined_at = Column(BigInteger, default=0)
    updated_at = Column(BigInteger, default=0)


class ChatMessage(Base):
    __tablename__ = "chat_messages"

    id = Column(String, primary_key=True, index=True)
    conversation_id = Column(String, ForeignKey("conversations.id"), index=True)
    sender_id = Column(String, ForeignKey("users.id"), index=True)
    type = Column(String, default="text", index=True)  # text | image | file | system
    content = Column(Text, default="")
    attachment_url = Column(String, default="")
    attachment_name = Column(String, default="")
    attachment_size = Column(BigInteger, default=0)
    object_key = Column(String, default="")
    extra = Column(JSON, default={})
    client_message_id = Column(String, index=True, nullable=True)
    status = Column(String, default="sent")
    created_at = Column(BigInteger, default=0)
    updated_at = Column(BigInteger, default=0)
    is_deleted = Column(Boolean, default=False)


