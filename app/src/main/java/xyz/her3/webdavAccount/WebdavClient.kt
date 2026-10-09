package xyz.her3.webdavAccount

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * WebDAV 读写(自签证书,信任所有;Basic auth)
 * 所有方法都是同步阻塞的,调用方需在子线程使用。
 */
class WebdavClient(baseUrl: String, user: String, pass: String) {

    private val baseUrl = baseUrl.trimEnd('/')
    private val credential = Credentials.basic(user, pass)

    private val client: OkHttpClient = run {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun get(path: String): String {
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", credential)
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("GET $path → HTTP ${resp.code}")
            return resp.body?.string() ?: throw Exception("GET $path → 空响应")
        }
    }

    fun put(path: String, content: String) {
        val body = content.toRequestBody("text/markdown; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", credential)
            .put(body)
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("PUT $path → HTTP ${resp.code}")
        }
    }

    /**
     * 按 expenses.md 的约定追加一行:
     * 同一天(日期 token 相同)紧接上一条;跨天则先空一行。
     */
    fun appendExpense(path: String, dateToken: String, line: String) {
        val current = get(path)
        val lastEntry = current.lines()
            .lastOrNull { it.isNotBlank() && !it.trimStart().startsWith("#") }
        val sameDay = lastEntry?.startsWith(dateToken) == true

        val sb = StringBuilder(current.trimEnd('\n')).append('\n')
        if (lastEntry != null && !sameDay) sb.append('\n')
        sb.append(line).append('\n')
        put(path, sb.toString())
    }
}
