package xyz.her3.webdavAccount

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.her3.webdavAccount.ui.AddExpenseSheet
import xyz.her3.webdavAccount.ui.AppHeader
import xyz.her3.webdavAccount.ui.AppTheme
import xyz.her3.webdavAccount.ui.EntryCard
import xyz.her3.webdavAccount.ui.FloatingNavBar
import xyz.her3.webdavAccount.ui.HomeHeader
import xyz.her3.webdavAccount.ui.SettingsScreen
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighRefreshRate()
        setContent {
            AppTheme {
                var config by remember { mutableStateOf(SettingsStore.load(this)) }
                val webdav = remember(config) { WebdavClient(config.url, config.user, config.pass) }
                MainScreen(
                    config = config,
                    onConfigSave = { new ->
                        SettingsStore.save(this, new)
                        config = new
                    },
                    onLoadRecent = { path, limit, onDone ->
                        Thread {
                            runCatching {
                                webdav.get(path).lines()
                                    .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                                    .takeLast(limit)
                                    .asReversed()
                                    .map { Entry.parse(it) }
                            }.let(onDone)
                        }.start()
                    },
                    onSave = { path, line, token, onDone ->
                        Thread {
                            runCatching { webdav.appendExpense(path, token, line) }
                                .let(onDone)
                        }.start()
                    },
                )
            }
        }
    }

    /** 请求屏幕支持的最高刷新率(适配 120Hz;部分 OEM 默认把 App 限在 60) */
    private fun requestHighRefreshRate() {
        val best = display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = best.modeId
            // Android 15+: 触摸交互期间提升到高刷
            if (Build.VERSION.SDK_INT >= 35) {
                frameRateBoostOnTouchEnabled = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    config: WebdavConfig,
    onConfigSave: (WebdavConfig) -> Unit,
    onLoadRecent: (path: String, limit: Int, onDone: (Result<List<Entry>>) -> Unit) -> Unit,
    onSave: (path: String, line: String, dateToken: String, onDone: (Result<Unit>) -> Unit) -> Unit,
) {
    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var hideAmount by remember { mutableStateOf(false) }
    var showFilterPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val todayToken = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    // null = 全部;默认只显示当日
    var filterDate by remember { mutableStateOf<String?>(todayToken) }
    val dateText = remember { SimpleDateFormat("E · M月d日", Locale.CHINA).format(Date()) }

    LaunchedEffect(refreshTick, config, filterDate) {
        if (config.url.isBlank() || config.path.isBlank()) {
            entries = emptyList()
            loading = false
            loadError = null
            return@LaunchedEffect
        }
        loading = true
        loadError = null
        onLoadRecent(config.path, 1000) { result ->
            loading = false
            result
                .onSuccess { entries = it }
                .onFailure { loadError = it.message ?: "网络错误" }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // 筛选其他日期时,大数字显示那天的开销;当日/全部视图都显示今天的
    val isOtherDay = filterDate != null && filterDate != todayToken
    val statDate = filterDate ?: todayToken
    val statEntries = entries.filter { it.dateKey == statDate && !it.income }
    val statExpense = statEntries.sumOf { it.amountValue ?: 0.0 }
    val amountText = if (config.url.isBlank()) {
        "¥ --"
    } else {
        "¥" + BigDecimal.valueOf(statExpense).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
    }
    val countText = if (config.url.isBlank()) {
        "还没配置 WebDAV,点右上角 ⋮ 进设置"
    } else {
        (if (isOtherDay) "当日" else "今日") + "共 ${statEntries.size} 笔支出"
    }
    // 展示列表:默认只显示当日
    val shownEntries = if (filterDate == null) entries else entries.filter { it.dateKey == filterDate }
    val listTitle = when {
        filterDate == null -> "全部账单"
        filterDate == todayToken -> "今日账单"
        else -> filterDate!!.let {
            "${it.substring(5, 7).toInt()}月${it.substring(8, 10).toInt()}日 的账单"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedTab == 0) {
                HomeHeader(
                    dateText = dateText,
                    labelText = if (isOtherDay) "当日开销" else "今日开销",
                    amountText = amountText,
                    countText = countText,
                    hidden = hideAmount,
                    onToggleHidden = { hideAmount = !hideAmount },
                    onMenuClick = { snackbarMessage = "菜单还没做" },
                    onCalendarClick = {
                        // 当日视图点开日期选择;其他状态点一下回到当日
                        if (filterDate == todayToken) {
                            showFilterPicker = true
                        } else {
                            filterDate = todayToken
                            snackbarMessage = "已回到当日"
                        }
                    },
                    filtering = isOtherDay,
                    listTitle = listTitle,
                    listAction = if (filterDate == null) "收起" else "查看全部",
                    onListAction = {
                        filterDate = if (filterDate == null) todayToken else null
                    },
                    onRefresh = { refreshTick++ },
                    onSettings = { selectedTab = 1 },
                )
            } else {
                AppHeader(label = "设置", headline = "WebDAV 同步")
            }
        },
    ) { padding ->
        // 浮栏叠在内容上层:列表全高延伸到底部,不被底栏挤开
        Box(modifier = Modifier.fillMaxSize()) {
            val topPad = padding.calculateTopPadding()
            when {
                selectedTab == 1 -> SettingsScreen(
                    config = config,
                    onSave = {
                        onConfigSave(it)
                        snackbarMessage = "已保存"
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPad),
                )
                loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = topPad)
                )
                loadError != null -> Text(
                    "加载失败:$loadError",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                entries.isEmpty() -> Text(
                    if (config.url.isBlank()) "还没配置 WebDAV,点下面的齿轮去设置" else "还没有记录,点下面的 + 记一笔",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = topPad),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shownEntries.isEmpty() -> Text(
                    if (filterDate == null) "还没有记录,点下面的 + 记一笔" else "这一天没有记录",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = topPad),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = topPad + 8.dp,
                        bottom = 104.dp,
                    ),
                ) {
                    items(shownEntries) { entry ->
                        EntryCard(entry, modifier = Modifier.animateItem())
                    }
                }
            }

            // 浮栏后于列表组合,自然绘制在上层
            FloatingNavBar(
                selectedTab = selectedTab,
                onListClick = {
                    selectedTab = 0
                    refreshTick++
                },
                onSettingsClick = {
                    selectedTab = 1
                },
                onAddClick = { showSheet = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showSheet) {
        AddExpenseSheet(
            onDismiss = { showSheet = false },
            onSaved = {
                refreshTick++
                snackbarMessage = "已保存"
            },
            onSave = { line, token, done -> onSave(config.path, line, token, done) },
        )
    }

    if (showFilterPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showFilterPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        filterDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                    }
                    showFilterPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showFilterPicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
