package xyz.her3.webdavAccount

/** expenses.md 一行的解析结果;解析失败时 fallback 为原文展示 */
data class Entry(
    val raw: String,
    val time: String?,      // "17:05",旧数据可能没有时间
    val dateLabel: String,  // "10.09"
    val detail: String,
    val income: Boolean,
    val amountText: String, // "¥14" / "+¥200" / "20 USD"
) {
    companion object {
        // 日期 [时间] 详情 [+]货币金额;货币可省略(默认CNY)
        private val LINE =
            Regex("""^(\S+)\s+(?:(\d{1,2}:\d{2})\s+)?(.+?)\s+(\+?)([a-zA-Z]{0,3})([\d.]+)$""")

        fun parse(line: String): Entry {
            val m = LINE.find(line)
                ?: return Entry(line, null, "", line, false, "")
            val (token, time, detail, sign, currency, amount) = m.destructured

            val dateLabel = when {
                token.matches(Regex("""\d{6}""")) -> "${token.substring(2, 4)}.${token.substring(4, 6)}"
                token.length >= 10 -> token.substring(5).replace('-', '.')
                else -> token
            }
            val cur = currency.lowercase()
            val amountText = when {
                cur.isEmpty() || cur == "cny" -> "$sign¥$amount"
                else -> "$sign$amount ${currency.uppercase()}"
            }
            return Entry(
                raw = line,
                time = time.ifBlank { null },
                dateLabel = dateLabel,
                detail = detail,
                income = sign == "+",
                amountText = amountText,
            )
        }
    }
}
