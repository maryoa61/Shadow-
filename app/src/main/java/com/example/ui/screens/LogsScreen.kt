package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LogEntryEntity
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.TertiaryAmber
import com.example.ui.theme.TextOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VpnUiState
import com.example.viewmodel.VpnViewModel

@Composable
fun LogsScreen(viewModel: VpnViewModel, uiState: VpnUiState, logs: List<LogEntryEntity>) {
    val filters = listOf("ALL", "INFO", "SUCCESS", "WARNING", "ERROR")
    val visible = logs.filter { log ->
        uiState.activeLogFilter == "ALL" || normalize(log.level) == uiState.activeLogFilter
    }
    Column(Modifier.fillMaxSize().background(BackgroundDeep)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Activity log", style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Connection events and diagnostics", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = viewModel::clearLogs) {
                Icon(Icons.Default.DeleteOutline, "Clear logs", tint = TextSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                val selected = uiState.activeLogFilter == filter
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) PrimaryCobalt else SurfaceContainer)
                    .border(1.dp, if (selected) PrimaryCobalt else OutlineVariant, RoundedCornerShape(10.dp))
                    .clickable { viewModel.setLogFilter(filter) }
                    .padding(horizontal = 13.dp, vertical = 8.dp)) {
                    Text(filter.lowercase().replaceFirstChar { it.uppercase() }, color = if (selected) Color.White else TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (visible.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Description, null, tint = TextOutline, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(12.dp)); Text("No events in this view", color = TextSecondary)
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.id }) { LogRow(it) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable private fun LogRow(log: LogEntryEntity) {
    val severity = normalize(log.level)
    val color = when (severity) { "ERROR" -> ErrorRed; "WARNING" -> TertiaryAmber; "SUCCESS" -> SecondaryEmerald; else -> PrimaryCobalt }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceContainer).border(1.dp, OutlineVariant, RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(severity, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(log.timeFormatted, color = TextOutline, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp)); Text(log.message, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
private fun normalize(level: String) = when (level.uppercase()) { "OK", "SUCCESS" -> "SUCCESS"; "WARN", "WARNING" -> "WARNING"; "ERR", "ERROR" -> "ERROR"; else -> "INFO" }
