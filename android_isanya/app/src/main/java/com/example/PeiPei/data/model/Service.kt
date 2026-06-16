// 文件说明：服务/陪玩单等业务实体的数据模型。

package com.example.Lulu.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "services")
data class Service(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    @SerializedName("cover_image_url")
    val coverImageUrl: String = "",
    @SerializedName("image_urls")
    val imageUrls: List<String> = emptyList(),
    val location: String = "",
    @SerializedName("price_text")
    val priceText: String = "",
    /** 计价说明：如「任意4小时」「每天（晚上10点前）」「每次（不超过2小时）」等，展示在详情页价格下方 */
    @SerializedName("price_basis_text")
    @ColumnInfo(defaultValue = "")
    val priceBasisText: String = "",
    /** 预付款占订单金额比例 0–100，默认 30 */
    @SerializedName("prepayment_percent")
    @ColumnInfo(defaultValue = "30")
    val prepaymentPercent: Int = 30,
    /**
     * 用户需早于服务开始至少该天数取消预定，方可全额退还预付款；0 表示不限制提前天数（开始服务前均可申请全额退）。
     * 取值 0–10，默认 1。
     */
    @SerializedName("full_refund_cancel_lead_days")
    @ColumnInfo(defaultValue = "1")
    val fullRefundCancelLeadDays: Int = 1,
    @SerializedName("category")
    @ColumnInfo(defaultValue = "其他服务")
    val category: String = ServiceCategories.DEFAULT,
    @SerializedName("service_mode")
    val serviceMode: String = "",
    /** 可预约时段 JSON：`[{"start":"08:00","end":"22:00"},...]`；空数组表示默认全天24小时均可提供服务 */
    @SerializedName("booking_time_ranges_json")
    @ColumnInfo(defaultValue = "")
    val bookingTimeRangesJson: String = "",
    /** 用户至少需提前多少小时发起预定；0 表示无需提前 */
    @SerializedName("booking_lead_hours")
    @ColumnInfo(defaultValue = "0")
    val bookingLeadHours: Float = 0f,
    /** 从今天起开放用户选择服务日期的天数窗口（含当日为第 1 天），默认 30，有效 7-180。 */
    @SerializedName("booking_future_open_days")
    @ColumnInfo(defaultValue = "30")
    val bookingFutureOpenDays: Int = 30,
    /** 用户付款后是否自动接单；false 表示需手动确认订单信息后接单 */
    @SerializedName("auto_accept_after_payment")
    @ColumnInfo(defaultValue = "1")
    val autoAcceptAfterPayment: Boolean = true,
    @SerializedName("sync_to_square")
    val syncToSquare: Boolean = false,
    @SerializedName("participant_ids")
    val participantIds: List<String> = emptyList(),
    val creator: String = "",
    @SerializedName("creator_id")
    val creatorId: String = "",
    @SerializedName("is_important")
    val isImportant: Boolean = false,
    @SerializedName("is_deleted")
    val isDeleted: Boolean = false,
    /** 未正式发布：仅创建者可见，不出现在广场与访客主页 */
    @SerializedName("is_draft")
    @ColumnInfo(defaultValue = "0")
    val isDraft: Boolean = false,
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("is_synced")
    val isSynced: Boolean = true,
    /** 用户追加的服务声明（不含平台默认四条，默认条目由客户端合并展示） */
    @SerializedName("service_declarations_extra")
    val serviceDeclarationsExtra: List<String> = emptyList(),
    /** 发布时选择的服务特点标签。 */
    @SerializedName("service_feature_tags")
    val serviceFeatureTags: List<String> = emptyList(),
    /** 发布时选择的额外费用标签。 */
    @SerializedName("service_extra_fee_tags")
    val serviceExtraFeeTags: List<String> = emptyList(),
)
