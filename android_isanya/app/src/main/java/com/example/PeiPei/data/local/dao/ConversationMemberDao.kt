// 文件说明：会话成员表的 Room DAO，群成员与参与者关系。

package com.example.Lulu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.Lulu.data.model.ChatConversationMember
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMemberDao {
    @Query("SELECT * FROM chat_conversation_members WHERE conversationId = :conversationId AND userId = :userId LIMIT 1")
    suspend fun getMember(conversationId: String, userId: String): ChatConversationMember?

    @Query("SELECT * FROM chat_conversation_members WHERE currentUserId = :currentUserId")
    fun getMembersByUser(currentUserId: String): Flow<List<ChatConversationMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ChatConversationMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ChatConversationMember>)

    @Query("DELETE FROM chat_conversation_members WHERE currentUserId = :currentUserId")
    suspend fun deleteByUser(currentUserId: String)

    @Query("DELETE FROM chat_conversation_members")
    suspend fun deleteAll()
}
