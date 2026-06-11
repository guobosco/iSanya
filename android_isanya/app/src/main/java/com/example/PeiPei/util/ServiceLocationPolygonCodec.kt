// 文件说明：服务区域「地图多边形」与展示文案在单字段 location 中的编解码（兼容纯文本旧数据）。

package com.example.Lulu.util

import com.example.Lulu.ui.util.compactAdminLocationToCityPrefectureLevel
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private data class StoredLatLngPoints(
    @SerializedName("points") val points: List<List<Double>>
)

object ServiceLocationPolygonCodec {
    private const val SEP = "|||PEIPEI_GEO1|||"
    private val gson = Gson()

    /** 用户可见的地点/区域说明（去掉尾部多边形 JSON；省+市+区县折叠为省+市）。 */
    fun displayLine(raw: String): String {
        val i = raw.indexOf(SEP)
        val base = if (i < 0) raw.trim() else raw.substring(0, i).trim()
        return compactAdminLocationToCityPrefectureLevel(base)
    }

    /** 解码多边形顶点，顺序为纬度、经度。无编码数据时返回 null。 */
    fun decodePolygonLatLng(raw: String): List<Pair<Double, Double>>? {
        val i = raw.indexOf(SEP)
        if (i < 0) return null
        val json = raw.substring(i + SEP.length).trim()
        if (json.isEmpty()) return null
        return try {
            val data = gson.fromJson(json, StoredLatLngPoints::class.java)
            data.points.map { it[0] to it[1] }
        } catch (_: Exception) {
            null
        }
    }

    fun encode(displayLine: String, latLngPoints: List<Pair<Double, Double>>): String {
        if (latLngPoints.size < 3) return displayLine.trim()
        val payload = StoredLatLngPoints(latLngPoints.map { listOf(it.first, it.second) })
        return displayLine.trim() + SEP + gson.toJson(payload)
    }
}
