// 文件说明：发布服务多档位价格与 priceText / priceBasisText 的互转（v2 结构化 + v1/纯文本兼容）。

package com.example.Lulu.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

const val DURATION_UNIT_HOURS = "小时"
const val DURATION_UNIT_MINUTES = "分钟"

const val PUBLISH_PRICE_TIERS_PREFIX_V1 = "lulu_publish_tiers_v1:"
const val PUBLISH_PRICE_TIERS_PREFIX_V2 = "lulu_publish_tiers_v2:"

data class PublishPriceTierRow(
    /** 自定义档位名称；空则 UI 显示「档位 n」 */
    val name: String = "",
    val price: String = "",
    val durationAmount: Int = 4,
    val durationUnit: String = DURATION_UNIT_HOURS,
)

private data class TierPersistV2(
    @SerializedName("name") val name: String = "",
    @SerializedName("price") val price: String = "",
    @SerializedName("duration_amount") val durationAmount: Int = 4,
    @SerializedName("duration_unit") val durationUnit: String = DURATION_UNIT_HOURS,
)

private data class TierLegacyV1(
    @SerializedName("name") val name: String = "",
    @SerializedName("price") val price: String = "",
    @SerializedName("duration") val duration: String = "",
)

private data class TiersEnvelopeV1(@SerializedName("tiers") val tiers: List<TierLegacyV1> = emptyList())
private data class TiersEnvelopeV2(@SerializedName("tiers") val tiers: List<TierPersistV2> = emptyList())

private val gson = Gson()

fun normalizeDurationUnit(unit: String): String =
    when (unit.trim()) {
        DURATION_UNIT_MINUTES -> DURATION_UNIT_MINUTES
        else -> DURATION_UNIT_HOURS
    }

private fun parseLegacyDurationText(raw: String): Pair<Int, String> {
    val s = raw.trim()
    if (s.isBlank()) return 4 to DURATION_UNIT_HOURS
    Regex("""(\d+)\s*分钟""").find(s)?.let {
        return it.groupValues[1].toInt().coerceIn(1, 99_999) to DURATION_UNIT_MINUTES
    }
    Regex("""(\d+)\s*小时""").find(s)?.let {
        return it.groupValues[1].toInt().coerceIn(1, 99_999) to DURATION_UNIT_HOURS
    }
    Regex("""(\d+)""").find(s)?.let {
        return it.groupValues[1].toInt().coerceIn(1, 99_999) to DURATION_UNIT_HOURS
    }
    return 4 to DURATION_UNIT_HOURS
}

private fun mapV1ToRows(v1: List<TierLegacyV1>): List<PublishPriceTierRow> =
    v1.map { t ->
        val (amt, u) = parseLegacyDurationText(t.duration)
        PublishPriceTierRow(
            name = t.name.trim(),
            price = t.price.trim(),
            durationAmount = amt,
            durationUnit = u,
        )
    }.ifEmpty { listOf(PublishPriceTierRow()) }

fun decodePublishPriceTiers(priceText: String, priceBasisText: String): List<PublishPriceTierRow> {
    val basis = priceBasisText.trim()
    if (basis.startsWith(PUBLISH_PRICE_TIERS_PREFIX_V2)) {
        val json = basis.removePrefix(PUBLISH_PRICE_TIERS_PREFIX_V2)
        return try {
            gson.fromJson(json, TiersEnvelopeV2::class.java).tiers.map {
                PublishPriceTierRow(
                    name = it.name.trim(),
                    price = it.price.trim(),
                    durationAmount = it.durationAmount.coerceIn(1, 99_999),
                    durationUnit = normalizeDurationUnit(it.durationUnit),
                )
            }.ifEmpty { listOf(PublishPriceTierRow()) }
        } catch (_: Exception) {
            listOf(PublishPriceTierRow(price = priceText.trim()))
        }
    }
    if (basis.startsWith(PUBLISH_PRICE_TIERS_PREFIX_V1)) {
        val json = basis.removePrefix(PUBLISH_PRICE_TIERS_PREFIX_V1)
        return try {
            mapV1ToRows(gson.fromJson(json, TiersEnvelopeV1::class.java).tiers)
        } catch (_: Exception) {
            listOf(PublishPriceTierRow(price = priceText.trim()))
        }
    }
    val (amt, u) = parseLegacyDurationText(basis)
    return listOf(PublishPriceTierRow(price = priceText.trim(), durationAmount = amt, durationUnit = u))
}

fun encodePublishPriceTiers(tiers: List<PublishPriceTierRow>): Pair<String, String> {
    val trimmed = tiers.map {
        PublishPriceTierRow(
            name = it.name.trim(),
            price = it.price.trim(),
            durationAmount = it.durationAmount.coerceIn(1, 99_999),
            durationUnit = normalizeDurationUnit(it.durationUnit),
        )
    }.filter { it.price.isNotBlank() }
    val list = trimmed.ifEmpty { listOf(PublishPriceTierRow()) }
    val persist = list.map {
        TierPersistV2(it.name, it.price, it.durationAmount, it.durationUnit)
    }
    val basisOut = PUBLISH_PRICE_TIERS_PREFIX_V2 + gson.toJson(TiersEnvelopeV2(persist))
    val headline = summarizePriceTextForPublish(list)
    return headline to basisOut
}

fun summarizePriceTextForPublish(tiers: List<PublishPriceTierRow>): String {
    val nonEmptyPrice = tiers.filter { it.price.isNotBlank() }
    if (nonEmptyPrice.isEmpty()) return ""
    if (tiers.size == 1) {
        val t = tiers.first()
        return buildString {
            if (t.price.isNotBlank()) append(t.price)
            append(" · ")
            append(t.durationAmount)
            append(t.durationUnit)
            if (t.name.isNotBlank()) {
                append(" · ")
                append(t.name.trim())
            }
        }
    }
    val nums = tiers.mapNotNull { extractYuanFromPriceLabel(it.price) }
    val minY = nums.minOrNull()
    return if (minY != null) {
        "¥${minY}起 · ${tiers.size}档"
    } else {
        "${nonEmptyPrice.first().price} 等${tiers.size}档"
    }
}

fun publishPriceTiersValidForSubmit(tiers: List<PublishPriceTierRow>): Boolean =
    tiers.any { it.price.trim().isNotBlank() }

private val yuanRegex = Regex("""[\u00a5¥]\s*(\d+)""")

fun extractYuanFromPriceLabel(label: String): Int? =
    yuanRegex.find(label.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()

/** 服务详情底部栏：仅「￥」+ 整数金额；无法解析为「价格面议」。 */
fun serviceDetailBottomPriceHeadline(priceText: String): String {
    val n = extractYuanFromPriceLabel(priceText)
    return if (n != null) "￥$n" else "价格面议"
}

/** 详情/询价等 UI：档位 JSON 不直接展示，用 service_mode 等兜底。 */
fun priceBasisTextForUiDisplay(priceBasisText: String, serviceModeFallback: String): String {
    val t = priceBasisText.trim()
    if (t.startsWith(PUBLISH_PRICE_TIERS_PREFIX_V2) || t.startsWith(PUBLISH_PRICE_TIERS_PREFIX_V1)) {
        return serviceModeFallback.trim().ifBlank { "以档位说明为准" }
    }
    return t
}

/** 解析「仅周末」等规则时，把档位里的时长拼进文本，避免 JSON 干扰。 */
fun textBundleForWeekdayParsing(
    title: String,
    description: String,
    priceText: String,
    priceBasisText: String,
): String {
    val basisPart = if (
        priceBasisText.trim().startsWith(PUBLISH_PRICE_TIERS_PREFIX_V2) ||
        priceBasisText.trim().startsWith(PUBLISH_PRICE_TIERS_PREFIX_V1)
    ) {
        decodePublishPriceTiers(priceText, priceBasisText)
            .joinToString(" ") { "${it.name} ${it.durationAmount}${it.durationUnit}".trim() }
            .trim()
    } else {
        priceBasisText
    }
    return listOf(title, description, basisPart).joinToString(" ")
}
