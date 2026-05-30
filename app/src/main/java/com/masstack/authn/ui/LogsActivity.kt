package com.masstack.authn.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masstack.authn.utils.Logger
import kotlinx.coroutines.delay

class LogsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                LogsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit) {
    var logs by remember { mutableStateOf(Logger.getLogs()) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-refresh logs every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            logs = Logger.getLogs()
        }
    }

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Copy all logs button
                    IconButton(onClick = {
                        if (logs.isNotEmpty()) {
                            val allLogs = logs.joinToString("\n") { log ->
                                "${log.timestamp} ${log.level.name.first()} [${log.tag}] ${log.message}"
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("all_logs", allLogs)
                            clipboard.setPrimaryClip(clip)
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy All Logs")
                    }
                    // Clear logs button
                    IconButton(onClick = {
                        Logger.clear()
                        logs = emptyList()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Info bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Total logs: ${logs.size} (max 500)",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Logs list
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "No logs yet. Perform actions in the app to see logs here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        LogEntryItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(log: Logger.LogEntry) {
    val context = LocalContext.current
    val levelColor = when (log.level) {
        Logger.LogLevel.DEBUG -> Color(0xFF808080)
        Logger.LogLevel.INFO -> Color(0xFF4CAF50)
        Logger.LogLevel.WARN -> Color(0xFFFFC107)
        Logger.LogLevel.ERROR -> Color(0xFFF44336)
    }

    val fullLogText = "${log.timestamp} ${log.level.name.first()} [${log.tag}] ${log.message}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = log.timestamp,
            color = Color(0xFF9E9E9E),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = log.level.name.first().toString(),
            color = levelColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = "[${log.tag}]",
            color = Color(0xFF2196F3),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = log.message,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy log",
            tint = Color(0xFF9E9E9E),
            modifier = Modifier
                .size(16.dp)
                .padding(start = 4.dp)
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("log", fullLogText)
                    clipboard.setPrimaryClip(clip)
                }
        )
    }
}
