from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, field_validator

# --- User Schemas (用户相关 Schema) ---

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

class UserBase(BaseModel):
    """
    用户基础 Schema，包含创建和读取时共有的字段
    """
    name: str = ""
    email: Optional[str] = None
    phone_number: Optional[str] = None
    pei_pei_id: Optional[str] = None
    photo_url: str = ""
    remark_name: str = ""
    tags: List[str] = Field(default_factory=list)
    favorite_service_ids: List[str] = Field(default_factory=list)
    signature: str = ""
    memo: str = ""
    gender: str = ""
    region: str = ""
    job_title: str = ""
    education: str = ""
    birth_decade: str = ""
    height_cm: int = 0
    weight_kg: float = 0.0
    height_weight_private: bool = False
    middle_school_favorite_song: str = ""
    spoken_languages: List[str] = Field(default_factory=list)
    living_city: str = ""
    living_country: str = ""
    identity_verified: bool = False
    review_count: int = 0
    average_rating: float = 0.0
    service_years: int = 0
    review_summaries: List[str] = Field(default_factory=list)
    is_phone_verified: bool = False
    is_profile_completed: bool = False
    address_detail: str = ""
    address_recipient_name: str = ""
    address_phone_number: str = ""
    id_modification_count: int = 0
    last_id_modification_year: int = 0

    @field_validator("tags", "favorite_service_ids", "spoken_languages", "review_summaries", mode="before")
    @classmethod
    def normalize_list_fields(cls, value):
        """ORM JSON 列可能为 NULL；缺省则 Pydantic 校验失败并导致 /users/{id} 500，客户端 Gson 再失败。"""
        if value is None:
            return []
        return value

class UserCreate(UserBase):
    """
    创建用户时使用的 Schema
    """
    # 客户端生成 UUID 或服务端生成。
    # 通常由服务端生成，但为了同步方便，客户端可能提供。
    id: str 
    password: str 

class UserLogin(BaseModel):
    phone_number: str
    password: str

class ContactMatchRequest(BaseModel):
    phone_numbers: List[str]


class FavoriteServicesUpdateRequest(BaseModel):
    service_ids: List[str] = Field(default_factory=list)


class FavoriteServicesResponse(BaseModel):
    service_ids: List[str] = Field(default_factory=list)

class User(UserBase):
    """
    返回给客户端的用户信息 Schema (Response Model)
    包含数据库生成的字段
    """
    id: str
    created_at: int
    updated_at: int
    
    class Config:
        # 允许 Pydantic 模型从 ORM 对象读取数据
        from_attributes = True

# --- Service Schemas (服务相关 Schema) ---

class ServiceBase(BaseModel):
    title: str = ""
    description: str = ""
    cover_image_url: str = ""
    image_urls: List[str] = Field(default_factory=list)
    location: str = ""
    price_text: str = ""
    price_basis_text: str = ""
    prepayment_percent: int = 30
    full_refund_cancel_lead_days: int = 1
    category: str = "其他服务"
    service_mode: str = ""
    service_time: int = 0
    booking_time_ranges_json: str = ""
    booking_lead_hours: float = 0.0
    booking_future_open_days: int = 30
    auto_accept_after_payment: bool = True
    sync_to_square: bool = False
    participant_ids: List[str] = Field(default_factory=list)
    service_declarations_extra: List[str] = Field(default_factory=list)
    creator: str = ""
    is_important: bool = False
    is_deleted: bool = False
    is_synced: bool = True

    @field_validator("image_urls", "participant_ids", "service_declarations_extra", mode="before")
    @classmethod
    def normalize_service_list_fields(cls, value):
        if value is None:
            return []
        return value

    @field_validator("price_basis_text", mode="before")
    @classmethod
    def none_price_basis_text(cls, value):
        return "" if value is None else value

    @field_validator("prepayment_percent", mode="before")
    @classmethod
    def normalize_prepayment_percent(cls, value):
        if value is None:
            return 30
        try:
            x = int(value)
        except (TypeError, ValueError):
            return 30
        return max(0, min(x, 100))

    @field_validator("full_refund_cancel_lead_days", mode="before")
    @classmethod
    def normalize_full_refund_cancel_lead_days(cls, value):
        if value is None:
            return 1
        try:
            x = int(value)
        except (TypeError, ValueError):
            return 1
        return max(0, min(x, 10))

    @field_validator("booking_time_ranges_json", mode="before")
    @classmethod
    def none_booking_time_ranges_json(cls, value):
        return "" if value is None else value

    @field_validator("booking_lead_hours", mode="before")
    @classmethod
    def normalize_booking_lead_hours(cls, value):
        if value is None:
            return 0.0
        try:
            x = float(value)
        except (TypeError, ValueError):
            return 0.0
        if x != x:  # NaN
            return 0.0
        return max(0.0, min(x, 168.0))

    @field_validator("booking_future_open_days", mode="before")
    @classmethod
    def normalize_booking_future_open_days(cls, value):
        if value is None:
            return 30
        try:
            x = int(value)
        except (TypeError, ValueError):
            return 30
        if x < 7:
            return 30
        if x > 180:
            return 180
        return x

    @field_validator("auto_accept_after_payment", mode="before")
    @classmethod
    def none_auto_accept_after_payment(cls, value):
        return True if value is None else value

    @field_validator("category", mode="before")
    @classmethod
    def none_category(cls, value):
        if value is None:
            return "其他服务"
        text = str(value).strip()
        return "其他服务" if not text else text


class ServiceCreate(ServiceBase):
    id: str
    creator_id: str
    created_at: int
    updated_at: int


class Service(ServiceBase):
    id: str
    creator_id: str
    created_at: int
    updated_at: int

    class Config:
        from_attributes = True

class ConversationBase(BaseModel):
    type: str = "direct"
    title: str = ""
    owner_id: Optional[str] = None
    avatar_url: str = ""
    participant_ids: List[str] = Field(default_factory=list)
    last_message_id: Optional[str] = None
    last_message_preview: str = ""
    last_message_at: int = 0
    created_at: int = 0
    updated_at: int = 0
    is_deleted: bool = False


class Conversation(ConversationBase):
    id: str

    class Config:
        from_attributes = True


class ConversationMemberBase(BaseModel):
    conversation_id: str
    user_id: str
    role: str = "member"
    unread_count: int = 0
    last_read_message_id: Optional[str] = None
    last_read_at: int = 0
    is_muted: bool = False
    joined_at: int = 0
    updated_at: int = 0


class ConversationMember(ConversationMemberBase):
    id: str

    class Config:
        from_attributes = True


class ChatMessageBase(BaseModel):
    conversation_id: str
    sender_id: str
    type: str = "text"
    content: str = ""
    attachment_url: str = ""
    attachment_name: str = ""
    attachment_size: int = 0
    object_key: str = ""
    extra: Dict[str, Any] = Field(default_factory=dict)
    client_message_id: Optional[str] = None
    status: str = "sent"
    created_at: int = 0
    updated_at: int = 0
    is_deleted: bool = False


class ChatMessageCreate(BaseModel):
    type: str = "text"
    content: str = ""
    attachment_url: str = ""
    attachment_name: str = ""
    attachment_size: int = 0
    object_key: str = ""
    extra: Dict[str, Any] = Field(default_factory=dict)
    client_message_id: Optional[str] = None


class ChatMessage(ChatMessageBase):
    id: str

    class Config:
        from_attributes = True


class ConversationListItem(BaseModel):
    conversation: Conversation
    member: Optional[ConversationMember] = None
    peer: Optional[User] = None


class ChatMessageReadRequest(BaseModel):
    message_id: Optional[str] = None


class ChatUploadResponse(BaseModel):
    url: str
    object_key: str


class MediaUploadResponse(ChatUploadResponse):
    mime_type: str = ""
    size: int = 0
