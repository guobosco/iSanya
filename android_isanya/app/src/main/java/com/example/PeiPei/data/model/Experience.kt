package com.example.Lulu.data.model

import com.google.gson.annotations.SerializedName

data class Experience(
    val id: String,
    val title: String = "",
    val description: String = "",
    @SerializedName("cover_image_url")
    val coverImageUrl: String = "",
    @SerializedName("image_urls")
    val imageUrls: List<String> = emptyList(),
    val location: String = "",
    @SerializedName("price_text")
    val priceText: String = "",
    @SerializedName("price_basis_text")
    val priceBasisText: String = "",
    val category: String = "",
    @SerializedName("duration_text")
    val durationText: String = "",
    @SerializedName("badge_text")
    val badgeText: String = "",
    val tags: List<String> = emptyList(),
    @SerializedName("host_id")
    val hostId: String = "",
    @SerializedName("host_name")
    val hostName: String = "",
    @SerializedName("created_at")
    val createdAt: Long = 0L,
    @SerializedName("updated_at")
    val updatedAt: Long = 0L,
    @SerializedName("is_deleted")
    val isDeleted: Boolean = false,
    @SerializedName("is_synced")
    val isSynced: Boolean = true,
)
