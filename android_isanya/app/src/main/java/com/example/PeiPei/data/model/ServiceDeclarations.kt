// 文件说明：服务声明平台默认条目与用户自定义条目的归一与合并。

package com.example.Lulu.data.model

object ServiceDeclarations {
    /** 平台默认声明，始终展示且不可由用户删除。 */
    val BUILTIN: List<String> = listOf(
        "发布内容需真实、准确，不得虚假宣传。",
        "价格与费用说明需清晰透明，额外收费需提前告知。",
        "服务过程中需尊重用户隐私与人身安全。",
        "涉及行程、接待或上门服务时需按约履约。"
    )

    private val builtinNormalized: Set<String> = BUILTIN.map { it.trim() }.toSet()

    /** 去空白、去与默认完全重复的项、去重、数量上限。 */
    fun normalizeExtra(raw: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val out = ArrayList<String>()
        for (line in raw) {
            val t = line.trim()
            if (t.isEmpty() || t in builtinNormalized || t in seen) continue
            seen.add(t)
            out.add(t)
            if (out.size >= 30) break
        }
        return out
    }

    fun mergedLines(extra: List<String>): List<String> = BUILTIN + normalizeExtra(extra)

    /** 详情页副标题：是否含发布者补充条目。 */
    fun declarationSummaryExtraCount(extra: List<String>): String {
        val n = normalizeExtra(extra).size
        return if (n == 0) "平台约定 4 条" else "平台约定 4 条 · 已补充 ${n} 条"
    }

    /** 详情页弹层正文：编号列表。 */
    fun declarationReaderBody(extra: List<String>): String =
        mergedLines(extra).mapIndexed { idx, line -> "${idx + 1}. $line" }.joinToString("\n\n")
}
