// 文件说明：服务/商品相关表的 Room DAO，本地服务数据访问。

package com.example.Lulu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.Lulu.data.model.Service
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Query(
        "SELECT * FROM services WHERE creatorId = :userId AND isDeleted = 0 AND isDraft = 0 " +
            "ORDER BY updatedAt DESC, createdAt DESC"
    )
    fun getServicesFlow(userId: String): Flow<List<Service>>

    @Query(
        "SELECT * FROM services WHERE creatorId = :userId AND isDeleted = 0 AND isDraft = 0 " +
            "ORDER BY updatedAt DESC, createdAt DESC"
    )
    suspend fun getServices(userId: String): List<Service>

    /** 本人「我发布的服务」管理页：含草稿与已下架 */
    @Query(
        "SELECT * FROM services WHERE creatorId = :userId ORDER BY updatedAt DESC, createdAt DESC"
    )
    fun getMyCreatedServicesHubFlow(userId: String): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE id = :serviceId LIMIT 1")
    fun getServiceById(serviceId: String): Flow<Service?>

    @Query("SELECT * FROM services WHERE id = :serviceId LIMIT 1")
    suspend fun getServiceByIdSync(serviceId: String): Service?

    @Query("SELECT * FROM services WHERE creatorId = :userId AND isSynced = 0")
    suspend fun getUnsyncedServices(userId: String): List<Service>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<Service>)

    @Update
    suspend fun updateService(service: Service)

    @Query("DELETE FROM services WHERE id = :serviceId")
    suspend fun deleteServiceById(serviceId: String)

    @Query("DELETE FROM services WHERE creatorId = :userId")
    suspend fun deleteByUser(userId: String)

    /** 广场发现：全站未删除服务缓存（与 /services/discovery 一致）；不含本地开发种子 id（seed-home-%） */
    @Query(
        "SELECT * FROM services WHERE isDeleted = 0 AND isDraft = 0 AND id NOT LIKE 'seed-home-%' " +
            "ORDER BY updatedAt DESC, createdAt DESC"
    )
    fun getSquareDiscoveryServicesFlow(): Flow<List<Service>>

    @Query(
        "SELECT * FROM services WHERE isDeleted = 0 AND isDraft = 0 AND id NOT LIKE 'seed-home-%' " +
            "ORDER BY updatedAt DESC, createdAt DESC"
    )
    suspend fun getSquareDiscoveryServices(): List<Service>

    /**
     * 首页瀑布流用：只取最近更新的前若干条，避免全表加载与 UI 对数千条 shuffle/map。
     * 全量缓存仍由 [getSquareDiscoveryServices] / 同步写入维护。
     */
    @Query(
        """
        SELECT * FROM services WHERE isDeleted = 0 AND isDraft = 0 AND id NOT LIKE 'seed-home-%'
        ORDER BY updatedAt DESC, createdAt DESC
        LIMIT 500
        """
    )
    fun getHomeDiscoveryFeedServicesFlow(): Flow<List<Service>>

    @Query("SELECT EXISTS(SELECT 1 FROM services WHERE isDeleted = 0)")
    suspend fun hasAnyNonDeletedService(): Boolean

    @Query("DELETE FROM services")
    suspend fun deleteAllServices()
}
