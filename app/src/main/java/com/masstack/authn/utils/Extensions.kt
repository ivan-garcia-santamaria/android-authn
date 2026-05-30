package com.masstack.authn.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Extension functions for common operations
 */

/**
 * Show a short toast message
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show a long toast message
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Copy text to clipboard
 */
fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    showToast("Copied to clipboard")
}

/**
 * URL encode a string
 */
fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

/**
 * Build query parameters for URL
 */
fun Map<String, String>.toQueryString(): String {
    return entries.joinToString("&") { (key, value) ->
        "${key.urlEncode()}=${value.urlEncode()}"
    }
}

/**
 * Parse query parameters from URL
 */
fun String.parseQueryParams(): Map<String, String> {
    return this.split("&").mapNotNull { param ->
        val parts = param.split("=")
        if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            null
        }
    }.toMap()
}
