// 文件说明：行政区划文案匹配（首页卡片发布地等展示用）。

package com.example.Lulu.ui.util

/**
 * 自左向右尝试匹配行政区划连写（长路径优先）。
 * 含：直辖市+区县、省/自治区+市+区县、省/自治区+自治州+区县、省/自治区+盟+旗县、省/自治区+自治州、省/自治区+盟、省/自治区+市、省/自治区+无「市」的县/区。
 */
private val AdminLocationMatchers: List<Regex> = listOf(
    Regex("""^(北京市|上海市|天津市|重庆市)(.+(?:林区|自治县|区|县|旗|自治旗|特区))$"""),
    Regex("""^(.+?(?:省|自治区))(.+?市)(.+(?:林区|自治县|自治旗|特区|区|县|旗))$"""),
    Regex("""^(.+?(?:省|自治区))(.+?自治州)(.+(?:林区|自治县|自治旗|特区|区|县|旗))$"""),
    Regex("""^(.+?(?:省|自治区))(.+?盟)(.+(?:林区|自治旗|旗|县|区|特区))$"""),
    Regex("""^(.+?(?:省|自治区))(.+?自治州)$"""),
    Regex("""^(.+?(?:省|自治区))(.+?盟)$"""),
    Regex("""^(.+?(?:省|自治区))(.+?市)$"""),
    Regex("""^(.+?(?:省|自治区))([^市]+(?:县|自治县|林区|旗|自治旗|特区|区))$"""),
)

fun matchAdminLocation(trimmed: String): MatchResult? =
    AdminLocationMatchers.firstNotNullOfOrNull { it.find(trimmed) }

private val MunicipalityNames = setOf("北京市", "上海市", "天津市", "重庆市")

/**
 * 服务区域等展示：若为「省/自治区 + 地级市/州/盟 + 区县」或「直辖市 + 区县」，去掉最末一级区县，
 * 例如 `海南省三亚市天涯区` → `海南省三亚市`，`北京市朝阳区` → `北京市`。
 * 不匹配行政区划模式或已是末级的文案原样返回。
 */
fun compactAdminLocationToCityPrefectureLevel(trimmed: String): String {
    val t = trimmed.trim()
    if (t.isEmpty()) return t
    val m = matchAdminLocation(t) ?: return t
    val g = m.groupValues.drop(1).filter { it.isNotBlank() }
    return when {
        g.size >= 3 -> g[0] + g[1]
        g.size == 2 && g[0] in MunicipalityNames -> g[0]
        else -> t
    }
}
