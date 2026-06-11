// 文件说明：服务声明平台默认条目与用户自定义条目的归一与合并。

package com.example.Lulu.data.model

object ServiceDeclarations {
    /** 平台默认声明，始终展示且不可由用户删除。 */
    val BUILTIN: List<String> = listOf(
        "沟通后前往指定的地点回合。",
        "杜绝线下交易。",
        "不提供非法涉黄服务。",
        "人身安全由个人负责。",
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
        return if (n == 0) "平台默认约定（4条）" else "平台约定 · 已补充 ${n} 条"
    }

    /** 详情页弹层正文：编号列表。 */
    fun declarationReaderBody(extra: List<String>): String =
        mergedLines(extra).mapIndexed { idx, line -> "${idx + 1}. $line" }.joinToString("\n\n")
}
