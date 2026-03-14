package com.jipzeongit.arcsync.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private const val TAG = "ArcSync"
    private val buffer = CopyOnWriteArrayList<String>()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            exception(e)
        }
    }

    fun log(message: String) {
        val line = "${timestamp()} | $message"
        Log.d(TAG, line)
        buffer.add(line)
        trim()
    }

    fun exception(t: Throwable) {
        val line = "${timestamp()} | EXCEPTION | ${t.javaClass.simpleName}: ${t.message}"
        Log.e(TAG, line, t)
        buffer.add(line)
        trim()
    }

    fun exportToFile(): File? {
        val context = appContext ?: return null
        val file = File(context.cacheDir, "arcsync_logs_${System.currentTimeMillis()}.txt")
        file.writeText(buffer.joinToString(separator = "\n"))
        return file
    }

    private fun trim() {
        while (buffer.size > 400) {
            buffer.removeAt(0)
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}
