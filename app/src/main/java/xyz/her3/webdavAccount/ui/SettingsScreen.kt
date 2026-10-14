package xyz.her3.webdavAccount.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import xyz.her3.webdavAccount.WebdavConfig

/** WebDAV 设置:地址/账号/密码/文件路径,保存后即时生效 */
@Composable
fun SettingsScreen(
    config: WebdavConfig,
    onSave: (WebdavConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by remember(config) { mutableStateOf(config.url) }
    var user by remember(config) { mutableStateOf(config.user) }
    var pass by remember(config) { mutableStateOf(config.pass) }
    var path by remember(config) { mutableStateOf(config.path) }
    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("WebDAV 服务器", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("服务器地址") },
            placeholder = { Text("https://example.com:9171") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("用户名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("密码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "隐藏" else "显示")
                }
            },
        )
        OutlinedTextField(
            value = path,
            onValueChange = { path = it },
            label = { Text("账单文件路径") },
            placeholder = { Text("/Obsidian/Finance/expenses.md") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                val u = url.trim().trimEnd('/')
                val p = path.trim()
                when {
                    !u.startsWith("http") -> error = "服务器地址要以 http(s):// 开头"
                    !p.startsWith("/") -> error = "文件路径要以 / 开头"
                    else -> {
                        error = null
                        onSave(WebdavConfig(u, user.trim(), pass, p))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存")
        }
    }
}
