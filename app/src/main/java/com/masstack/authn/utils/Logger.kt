package com.masstack.authn.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Custom logger that keeps logs in memory for debugging
 */
object Logger {
    private const val TAG = "AutonApp"
    private const val MAX_LOGS = 500

    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val tag: String,
        val message: String
    )

    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
        Log.d(TAG, "[$tag] $message")
    }

    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
        Log.i(TAG, "[$tag] $message")
    }

    fun w(tag: String, message: String) {
        log(LogLevel.WARN, tag, message)
        Log.w(TAG, "[$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message: ${throwable.message}\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        log(LogLevel.ERROR, tag, fullMessage)
        if (throwable != null) {
            Log.e(TAG, "[$tag] $message", throwable)
        } else {
            Log.e(TAG, "[$tag] $message")
        }
    }

    private fun log(level: LogLevel, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, level, tag, message)

        logs.add(entry)

        // Keep only last MAX_LOGS entries
        while (logs.size > MAX_LOGS) {
            logs.poll()
        }
    }

    fun getLogs(): List<LogEntry> = logs.toList()

    fun clear() {
        logs.clear()
    }
}
