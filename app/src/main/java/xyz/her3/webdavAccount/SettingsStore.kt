package xyz.her3.webdavAccount

import android.content.Context

data class WebdavConfig(
    val url: String,
    val user: String,
    val pass: String,
    val path: String,
)

/** WebDAV 配置:仅来自应用内设置页,无内置默认值 */
object SettingsStore {
    private const val PREFS = "settings"
    private const val KEY_URL = "webdav_url"
    private const val KEY_USER = "webdav_user"
    private const val KEY_PASS = "webdav_pass"
    private const val KEY_PATH = "webdav_path"

    fun load(context: Context): WebdavConfig {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WebdavConfig(
            url = p.getString(KEY_URL, null) ?: "",
            user = p.getString(KEY_USER, null) ?: "",
            pass = p.getString(KEY_PASS, null) ?: "",
            path = p.getString(KEY_PATH, null) ?: "",
        )
    }

    fun save(context: Context, config: WebdavConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_URL, config.url)
            .putString(KEY_USER, config.user)
            .putString(KEY_PASS, config.pass)
            .putString(KEY_PATH, config.path)
            .apply()
    }
}
