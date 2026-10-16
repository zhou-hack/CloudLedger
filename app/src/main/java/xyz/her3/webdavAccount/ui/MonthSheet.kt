package xyz.her3.webdavAccount.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.her3.webdavAccount.Entry
import xyz.her3.webdavAccount.MonthlyReport
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 月度概览:本月支出/笔数/日均 + 余额块(来自 AI 月度文件)+ 按天明细,可左右翻月 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSheet(
    entries: List<Entry>,
    reports: Map<String, MonthlyReport>,
    onDismiss: () -> Unit,
) {
    var monthOffset by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val monthTitle = SimpleDateFormat("yyyy年M月", Locale.CHINA).format(cal.time)

        val monthEntries = entries.filter { !it.income && it.dateKey.startsWith(monthKey) }
        val total = monthEntries.sumOf { it.amountValue ?: 0.0 }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // 当月按已过天数算日均,历史月份按整月
        val elapsedDays = if (monthOffset == 0) cal.get(Calendar.DAY_OF_MONTH) else daysInMonth
        val dailyAvg = if (elapsedDays > 0) total / elapsedDays else 0.0

        val daily = monthEntries
            .groupBy { it.dateKey }
            .map { (day, list) -> day to list.sumOf { it.amountValue ?: 0.0 } }
            .sortedByDescending { it.first }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "上个月")
                }
                Text(
                    monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "下个月")
                }
            }

            Spacer(Modifier.size(12.dp))
            Text(
                "本月支出",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "¥" + BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "共 ${monthEntries.size} 笔 · 日均 ¥" +
                    BigDecimal.valueOf(dailyAvg).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 余额块(来自 AI 月度文件的"余额"一节)
            reports[monthKey]?.let { rep ->
                if (rep.balance != null) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        buildString {
                            rep.openingBalance?.let { append("月初 ¥$it") }
                            rep.monthExpense?.let {
                                if (isNotEmpty()) append(" · ")
                                append("开销 ¥$it")
                            }
                            rep.incomeAdded?.let {
                                if (isNotEmpty()) append(" · ")
                                append("新增 ¥$it")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "剩余余额 ¥${rep.balance}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.size(12.dp))
            if (daily.isEmpty()) {
                Text(
                    "这个月没有记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(daily) { (day, sum) ->
                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                day.substring(5).replace('-', '.'),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "¥" + BigDecimal.valueOf(sum).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
