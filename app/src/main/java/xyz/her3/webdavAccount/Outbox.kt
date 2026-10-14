package xyz.her3.webdavAccount

import android.content.Context
import java.io.File

/**
 * 离线待同步队列:保存失败先写本地(filesDir 下每行一笔),
 * 下次加载/保存时尽力补传,全部成功才清空。
 */
object Outbox {
    private const val FILE = "pending_expenses.txt"

    private fun file(context: Context) = File(context.filesDir, FILE)

    fun add(context: Context, line: String) {
        file(context).appendText(line + "\n")
    }

    fun pending(context: Context): List<String> {
        val f = file(context)
        return if (f.exists()) f.readLines().filter { it.isNotBlank() } else emptyList()
    }

    fun remove(context: Context, line: String) {
        val f = file(context)
        if (!f.exists()) return
        val lines = f.readLines().toMutableList()
        lines.remove(line) // 只移除第一条匹配
        if (lines.isEmpty()) f.delete() else f.writeText(lines.joinToString("\n") + "\n")
    }

    /** 逐条补传;中断时保留未完成的,返回是否全部完成 */
    fun flush(context: Context, webdav: WebdavClient, path: String): Boolean {
        val lines = pending(context)
        if (lines.isEmpty()) return true
        var done = 0
        try {
            for (line in lines) {
                webdav.appendExpense(path, line.substringBefore(" "), line)
                done++
            }
        } catch (_: Exception) {
            val remaining = lines.drop(done)
            file(context).writeText(remaining.joinToString("\n") + "\n")
            return false
        }
        file(context).delete()
        return true
    }
}
