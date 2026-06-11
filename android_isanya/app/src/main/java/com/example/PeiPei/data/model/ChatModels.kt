// 文件说明：即时通讯相关的数据模型（消息、会话摘要等）。

package com.example.Lulu.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "chat_conversations")
data class ChatConversation(
    @PrimaryKey
    val id: String,
    val type: String = "direct",
    val title: String = "",
    @SerializedName("owner_id")
    val ownerId: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String = "",
    @SerializedName("participant_ids")
    val participantIds: List<String> = emptyList(),
    @SerializedName("last_message_id")
    val lastMessageId: String? = null,
    @SerializedName("last_message_preview")
    val lastMessagePreview: String = "",
    @SerializedName("last_message_at")
    val lastMessageAt: Long = 0L,
    @SerializedName("created_at")
    val createdAt: Long = 0L,
    @SerializedName("updated_at")
    val updatedAt: Long = 0L,
    @SerializedName("is_deleted")
    val isDeleted: Boolean = false,
    val currentUserId: String = ""
)

@Entity(tableName = "chat_conversation_members")
data class ChatConversationMember(
    @PrimaryKey
    val id: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    val userId: String,
    val role: String = "member",
    @SerializedName("unread_count")
    val unreadCount: Int = 0,
    @SerializedName("last_read_message_id")
    val lastReadMessageId: String? = null,
    @SerializedName("last_read_at")
    val lastReadAt: Long = 0L,
    @SerializedName("is_muted")
    val isMuted: Boolean = false,
    @SerializedName("joined_at")
    val joinedAt: Long = 0L,
    @SerializedName("updated_at")
    val updatedAt: Long = 0L,
    val currentUserId: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("sender_id")
    val senderId: String,
    val type: String = "text",
    val content: String = "",
    @SerializedName("attachment_url")
    val attachmentUrl: String = "",
    @SerializedName("attachment_name")
    val attachmentName: String = "",
    @SerializedName("attachment_size")
    val attachmentSize: Long = 0L,
    @SerializedName("object_key")
    val objectKey: String = "",
    val extra: Map<String, String> = emptyMap(),
    @SerializedName("client_message_id")
    val clientMessageId: String? = null,
    val status: String = "sent",
    @SerializedName("created_at")
    val createdAt: Long = 0L,
    @SerializedName("updated_at")
    val updatedAt: Long = 0L,
    @SerializedName("is_deleted")
    val isDeleted: Boolean = false,
    val currentUserId: String = ""
)

data class ConversationListItem(
    val conversation: ChatConversation,
    val member: ChatConversationMember? = null,
    val peer: User? = null
)

data class CreateDirectConversationResponse(
    val conversation: ChatConversation,
    val member: ChatConversationMember? = null,
    val peer: User? = null
)

data class ChatMessageCreateRequest(
    val type: String = "text",
    val content: String = "",
    @SerializedName("attachment_url")
    val attachmentUrl: String = "",
    @SerializedName("attachment_name")
    val attachmentName: String = "",
    @SerializedName("attachment_size")
    val attachmentSize: Long = 0L,
    @SerializedName("object_key")
    val objectKey: String = "",
    val extra: Map<String, String> = emptyMap(),
    @SerializedName("client_message_id")
    val clientMessageId: String? = null
)

data class ChatReadRequest(
    @SerializedName("message_id")
    val messageId: String? = null
)

data class ChatUploadResponse(
    val url: String,
    @SerializedName("object_key")
    val objectKey: String
)

data class ChatSocketEvent(
    val type: String,
    @SerializedName("conversation_id")
    val conversationId: String? = null,
    val message: ChatMessage? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("read_at")
    val readAt: Long? = null
)
