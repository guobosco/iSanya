// 文件说明：聊天消息表的 Room DAO，增删改查与 Flow 订阅。

package com.example.Lulu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.Lulu.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND currentUserId = :currentUserId ORDER BY createdAt ASC")
    fun getMessages(conversationId: String, currentUserId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE id = :messageId AND currentUserId = :currentUserId LIMIT 1")
    suspend fun getMessageById(messageId: String, currentUserId: String): ChatMessage?

    @Query(
        "SELECT * FROM chat_messages " +
            "WHERE conversationId = :conversationId AND currentUserId = :currentUserId " +
            "ORDER BY createdAt DESC, updatedAt DESC LIMIT 1"
    )
    suspend fun getLatestMessageByConversation(
        conversationId: String,
        currentUserId: String
    ): ChatMessage?

    @Query(
        "SELECT * FROM chat_messages " +
            "WHERE conversationId = :conversationId AND currentUserId = :currentUserId " +
            "ORDER BY createdAt ASC, updatedAt ASC LIMIT 1"
    )
    suspend fun getFirstMessageByConversation(
        conversationId: String,
        currentUserId: String
    ): ChatMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND currentUserId = :currentUserId")
    suspend fun deleteMessagesByConversation(conversationId: String, currentUserId: String)

    @Query("DELETE FROM chat_messages WHERE currentUserId = :currentUserId")
    suspend fun deleteByUser(currentUserId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
