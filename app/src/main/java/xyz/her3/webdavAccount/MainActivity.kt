package xyz.her3.webdavAccount

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import xyz.her3.webdavAccount.ui.AddExpenseSheet
import xyz.her3.webdavAccount.ui.AppTheme
import xyz.her3.webdavAccount.ui.EntryCard
import xyz.her3.webdavAccount.ui.FloatingNavBar

class MainActivity : ComponentActivity() {

    private val webdav by lazy {
        WebdavClient(BuildConfig.WEBDAV_URL, BuildConfig.WEBDAV_USER, BuildConfig.WEBDAV_PASS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainScreen(
                    onLoadRecent = { onDone ->
                        Thread {
                            runCatching {
                                webdav.get(EXPENSES_PATH).lines()
                                    .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                                    .takeLast(30)
                                    .asReversed()
                                    .map { Entry.parse(it) }
                            }.let(onDone)
                        }.start()
                    },
                    onSave = { line, token, onDone ->
                        Thread {
                            runCatching { webdav.appendExpense(EXPENSES_PATH, token, line) }
                                .let(onDone)
                        }.start()
                    },
                )
            }
        }
    }

    companion object {
        const val EXPENSES_PATH = "/Obsidian/Finance/expenses.md"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLoadRecent: (onDone: (Result<List<Entry>>) -> Unit) -> Unit,
    onSave: (line: String, dateToken: String, onDone: (Result<Unit>) -> Unit) -> Unit,
) {
    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var barVisible by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val hideBarConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -8f -> barVisible = false
                    available.y > 8f -> barVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(refreshTick) {
        loading = true
        loadError = null
        onLoadRecent { result ->
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("WebDAV记账") },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
        bottomBar = {
            FloatingNavBar(
                visible = barVisible,
                onListClick = { refreshTick++ },
                onSettingsClick = { snackbarMessage = "设置还没做" },
                onAddClick = { showSheet = true },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(hideBarConnection)
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                loadError != null -> Text(
                    "加载失败:$loadError",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                entries.isEmpty() -> Text(
                    "还没有记录,点下面的 + 记一笔",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                ) {
                    items(entries) { entry ->
                        EntryCard(entry, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }

    if (showSheet) {
        AddExpenseSheet(
            onDismiss = { showSheet = false },
            onSaved = {
                refreshTick++
                snackbarMessage = "已保存"
            },
            onSave = onSave,
        )
    }
}
