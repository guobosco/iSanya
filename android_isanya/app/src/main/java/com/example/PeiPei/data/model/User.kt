// 文件说明：用户资料、账号等核心用户数据模型。

package com.example.Lulu.data.model

/**
 * 用户实体定义文件。
 * 用于统一描述用户在本地数据库、网络序列化和 UI 层中的字段结构。
 */

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

const val MAX_PROFILE_WALL_IMAGE_COUNT = 18

/**
 * 用户数据模型
 * 功能：表示一个应用用户，包含用户的基本信息和认证相关数据
 * @property id 用户唯一标识符
 * @property name 用户姓名
 * @property email 用户邮箱
 * @property photoUrl 用户头像URL
 * @property createdAt 用户创建时间
 * @property updatedAt 用户信息更新时间
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val email: String? = "", // Changed to nullable to match optional
    @SerializedName("photo_url")
    val photoUrl: String = "",
    @SerializedName("profile_image_urls")
    val profileImageUrls: List<String> = emptyList(), // 资料照片墙
    val tags: List<String> = emptyList(), // 用户标签
    @SerializedName("pei_pei_id")
    val peiPeiId: String = "", // SanyaGo ID
    @SerializedName("remark_name")
    val remarkName: String = "", // 备注名
    @SerializedName("phone_number")
    val phoneNumber: String = "", // 电话
    @SerializedName("favorite_service_ids")
    val favoriteServiceIds: List<String> = emptyList(), // 心愿单收藏服务ID
    @SerializedName("is_phone_verified")
    val isPhoneVerified: Boolean = false, // 手机号是否已验证
    @SerializedName("is_profile_completed")
    val isProfileCompleted: Boolean = false, // 个人资料是否已完善
    val signature: String = "", // 自我介绍（沿用 signature 字段）
    val memo: String = "", // 备忘
    val gender: String = "", // 性别
    val region: String = "", // 地区
    @SerializedName("job_title")
    val jobTitle: String = "", // 工作
    @SerializedName("education")
    val education: String = "", // 教育背景
    @SerializedName("birth_decade")
    val birthDecade: String = "", // 出生年代/年龄段（约五年一档，如 90后、95后）
    @SerializedName("height_cm")
    val heightCm: Int = 0, // 身高（厘米），0 表示未填
    @SerializedName("weight_kg")
    val weightKg: Float = 0f, // 体重（千克），0 表示未填
    @SerializedName("height_weight_private")
    val heightWeightPrivate: Boolean = false, // 对他人隐藏身高体重
    @SerializedName("middle_school_favorite_song")
    val middleSchoolFavoriteSong: String = "", // 中学时最喜欢的歌曲
    @SerializedName("spoken_languages")
    val spokenLanguages: List<String> = emptyList(), // 会说语言
    @SerializedName("living_city")
    val livingCity: String = "", // 常住城市
    @SerializedName("living_country")
    val livingCountry: String = "", // 常住国家
    @SerializedName("identity_verified")
    val identityVerified: Boolean = false, // 身份认证状态
    @SerializedName("review_count")
    val reviewCount: Int = 0, // 评价数量
    @SerializedName("average_rating")
    val averageRating: Double = 0.0, // 平均评分
    @SerializedName("service_years")
    val serviceYears: Int = 0, // 服务经验年限
    @SerializedName("review_summaries")
    val reviewSummaries: List<String> = emptyList(), // 评价摘要
    @SerializedName("address_detail")
    val addressDetail: String = "", // 详细地址
    @SerializedName("address_recipient_name")
    val addressRecipientName: String = "", // 收货人姓名
    @SerializedName("address_phone_number")
    val addressPhoneNumber: String = "", // 收货人手机号
    @SerializedName("id_modification_count")
    val idModificationCount: Int = 0, // 当年修改SanyaGo ID的次数
    @SerializedName("last_id_modification_year")
    val lastIdModificationYear: Int = 0, // 上次修改SanyaGo ID的年份
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

fun User.withAvatarIncludedInPhotoWall(
    avatarUrl: String,
    maxPhotoCount: Int = MAX_PROFILE_WALL_IMAGE_COUNT
): User {
    val normalizedAvatarUrl = avatarUrl.trim()
    if (normalizedAvatarUrl.isEmpty()) {
        return this
    }
    val mergedPhotoWallUrls = buildList {
        add(normalizedAvatarUrl)
        addAll(profileImageUrls.filterNot { it == normalizedAvatarUrl })
    }.take(maxPhotoCount)
    return copy(
        photoUrl = normalizedAvatarUrl,
        profileImageUrls = mergedPhotoWallUrls,
        updatedAt = System.currentTimeMillis()
    )
}

fun User.hasCompletedRequiredProfileFields(): Boolean {
    return gender.trim().isNotEmpty() &&
        region.trim().isNotEmpty() &&
        photoUrl.trim().isNotEmpty()
}

fun User.hasCompletedOnboardingProfile(): Boolean {
    return name.trim().isNotEmpty() && hasCompletedRequiredProfileFields()
}

/** 体重展示为整数或一位小数 + kg */
fun formatWeightKgForDisplay(kg: Float): String {
    if (kg <= 0f) return ""
    val scaled = (kg * 10f).roundToInt() / 10f
    return if (abs(scaled - scaled.toInt()) < 1e-4f) "${scaled.toInt()}kg"
    else String.format(Locale.US, "%.1fkg", scaled)
}

/**
 * 资料卡上的身高体重文案。
 * @param isViewerSelf 是否为本人查看自己的资料
 */
fun User.heightWeightDisplayForProfile(isViewerSelf: Boolean): String {
    if (heightWeightPrivate && !isViewerSelf) return "已隐藏"
    val hasH = heightCm > 0
    val hasW = weightKg > 0f
    if (!hasH && !hasW) return "未设置"
    val parts = buildList {
        if (hasH) add("${heightCm}cm")
        if (hasW) add(formatWeightKgForDisplay(weightKg))
    }
    val base = parts.joinToString(" · ")
    return if (heightWeightPrivate && isViewerSelf) "$base（对他人隐藏）" else base
}

/** 编辑资料列表中的摘要（不含「未设置」前缀以外的长句） */
fun User.heightWeightEditRowValue(): String {
    val hasH = heightCm > 0
    val hasW = weightKg > 0f
    if (!hasH && !hasW) return "未设置"
    val parts = buildList {
        if (hasH) add("${heightCm}cm")
        if (hasW) add(formatWeightKgForDisplay(weightKg))
    }
    val base = parts.joinToString(" · ")
    return if (heightWeightPrivate) "$base · 仅自己可见" else base
}
