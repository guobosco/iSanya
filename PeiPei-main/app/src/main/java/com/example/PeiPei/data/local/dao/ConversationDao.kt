// 文件说明：会话表的 Room DAO，会话列表与元数据访问。

package com.example.Lulu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.Lulu.data.model.ChatConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM chat_conversations WHERE currentUserId = :currentUserId ORDER BY lastMessageAt DESC, updatedAt DESC")
    fun getAllConversations(currentUserId: String): Flow<List<ChatConversation>>

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId AND currentUserId = :currentUserId LIMIT 1")
    fun getConversationById(conversationId: String, currentUserId: String): Flow<ChatConversation?>

    @Query("SELECT * FROM chat_conversations WHERE id = :conversationId AND currentUserId = :currentUserId LIMIT 1")
    suspend fun getConversationByIdSuspend(conversationId: String, currentUserId: String): ChatConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ChatConversation>)

    @Query("DELETE FROM chat_conversations WHERE currentUserId = :currentUserId")
    suspend fun deleteByUser(currentUserId: String)

    @Query("DELETE FROM chat_conversations")
    suspend fun deleteAll()
}
