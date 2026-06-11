// 文件说明：用户表的 Room DAO，本地用户资料与缓存。

package com.example.Lulu.data.local.dao

import androidx.room.*
import com.example.Lulu.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<User?>
    
    // 同步方法，用于非 Flow 场景
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdSuspend(userId: String): User?

    @Query("SELECT * FROM users WHERE peiPeiId = :peiPeiId")
    suspend fun getUserByLuluId(peiPeiId: String): User?

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber")
    suspend fun getUserByPhoneNumber(phoneNumber: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    /** 用于清理本地注入的假主理人（如 id 前缀 `seed-service-host-{viewer}-`）。 */
    @Query("DELETE FROM users WHERE id LIKE :idPrefix || '%'")
    suspend fun deleteUsersByIdPrefix(idPrefix: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
