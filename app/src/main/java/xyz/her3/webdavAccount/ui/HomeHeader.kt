package xyz.her3.webdavAccount.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 首页仪表盘头部:tonal 大圆角卡(标题/日期/今日开销大数字/笔数/眼睛开关)
 * + "最近账单"区段行。整体放在 Scaffold topBar 槽位,固定不滚走。
 */
@Composable
fun HomeHeader(
    dateText: String,     // "周一 · 10月12日"
    labelText: String,    // "今日开销" / "当日开销"(筛选时)
    amountText: String,   // "¥128.50"
    countText: String,    // "今日共 6 笔支出"
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onMenuClick: () -> Unit,
    onCalendarClick: () -> Unit,
    filtering: Boolean,   // 日历筛选中(图标高亮)
    listTitle: String,    // 区段标题:"今日账单"/"10月11日 的账单"/"全部账单"
    listAction: String,   // 区段按钮:"查看全部"/"收起"
    onListAction: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    // 整个头部(状态栏区+卡片+区段行)铺不透明底色,
    // 列表上划时被头部盖住,不会从卡片两侧缝隙透出来
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "菜单")
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "记账",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = "按日期筛选",
                            tint = if (filtering) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        )
                    }
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                onClick = {
                                    menuOpen = false
                                    onRefresh()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    menuOpen = false
                                    onSettings()
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.size(12.dp))
                // 眼睛与大数字同一行,卡片底部不再留空
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            labelText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.size(2.dp))
                        Text(
                            if (hidden) "¥ ****" else amountText,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    IconButton(
                        onClick = onToggleHidden,
                        modifier = Modifier.size(36.dp),
                    ) {
                        EyeIcon(
                            open = !hidden,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                listTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onListAction) {
                Text(listAction)
            }
        }
    }
}

/** 手绘眼睛图标(避免引入 material-icons-extended) */
@Composable
private fun EyeIcon(open: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
        // 眼睛轮廓(横椭圆)
        drawOval(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.28f),
            size = Size(w * 0.84f, h * 0.44f),
            style = stroke,
        )
        // 瞳孔
        drawCircle(color = tint, radius = w * 0.11f, center = center)
        if (!open) {
            drawLine(
                color = tint,
                start = Offset(w * 0.18f, h * 0.82f),
                end = Offset(w * 0.82f, h * 0.18f),
                strokeWidth = w * 0.075f,
                cap = StrokeCap.Round,
            )
        }
    }
}
