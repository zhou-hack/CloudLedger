package xyz.her3.webdavAccount

import java.util.Locale

/**
 * AI 分类工具生成的 monthly/yyyy-MM.md 的解析结果。
 * categories: "yyyy-MM-dd|描述|金额(2位小数)" -> 种类
 */
data class MonthlyReport(
    val month: String,
    val categories: Map<String, String>,
    val openingBalance: String?,  // 月初余额
    val monthExpense: String?,    // 本月开销
    val incomeAdded: String?,     // 新增余额
    val balance: String?,         // 剩余余额
) {
    companion object {
        private val TABLE_ROW =
            Regex("""^\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|""")

        fun keyOf(dateKey: String, detail: String, amount: Double): String =
            "$dateKey|${detail.trim()}|" + String.format(Locale.US, "%.2f", amount)

        fun parse(md: String, month: String): MonthlyReport {
            val cats = mutableMapOf<String, String>()
            var opening: String? = null
            var expense: String? = null
            var added: String? = null
            var balance: String? = null

            fun grab(label: String, line: String): String? =
                Regex("""^-\s*$label：([\d,]+(?:\.\d+)?)""").find(line)?.groupValues?.get(1)

            for (line in md.lines()) {
                TABLE_ROW.find(line)?.let { m ->
                    val (dateCell, desc, amt, cat) = m.destructured
                    if (dateCell == "日期" || dateCell.startsWith("---")) return@let
                    // 日期单元格可能带时间 "2026-10-10 11:14:00"
                    val date = dateCell.trim().substringBefore(" ")
                    val amount = amt.replace("+", "").replace("-", "")
                        .replace(",", "").toDoubleOrNull()
                    if (amount != null) {
                        cats[keyOf(date, desc, amount)] = cat.trim()
                    }
                }
                if (opening == null) opening = grab("月初余额", line)
                if (expense == null) expense = grab("本月开销", line)
                if (added == null) added = grab("新增余额", line)
                if (balance == null) balance = grab("剩余余额", line)
            }
            return MonthlyReport(month, cats, opening, expense, added, balance)
        }
    }
}
