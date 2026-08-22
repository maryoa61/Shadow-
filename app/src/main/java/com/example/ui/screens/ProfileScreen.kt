package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.TacticalGlassCard
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MonoMetrics
import com.example.ui.theme.MonoMetricsSmall
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.RealityIndigo
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.TextOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VlessCyan
import com.example.viewmodel.VpnUiState
import com.example.viewmodel.VpnViewModel

@Composable
fun ProfileScreen(
    viewModel: VpnViewModel,
    uiState: VpnUiState
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Hero Branding Card (Sleek Theme Sky Blue Banner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(PrimaryContainer)
                .border(1.dp, OutlineVariant, RoundedCornerShape(32.dp))
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceBright)
                        .border(1.dp, OutlineVariant, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Logo",
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "SHADOW_NET",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnPrimaryContainer
                    )
                    Text(
                        text = "Next-Gen Anti-Censorship Proxy Core",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        PulsingStatusDot(color = SecondaryEmerald, size = 6)
                        Text(
                            text = "ENGINE ONLINE",
                            style = MonoMetricsSmall,
                            color = SecondaryEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Engine & Environment Diagnostics
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CORE DIAGNOSTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                DiagnosticRow(label = "Application Version", value = "v2.4.0 (Enterprise)")
                DiagnosticRow(label = "Proxy Daemon Core", value = "Xray-core v1.8.4")
                DiagnosticRow(label = "Binary Architecture", value = "arm64-v8a (JIT Enabled)")
                DiagnosticRow(label = "Memory Footprint", value = "34.2 MB RSS")
                DiagnosticRow(label = "GeoIP / GeoSite Database", value = "v2024.08.15-Loyalsoldier")
                DiagnosticRow(label = "Crypto Engine", value = "BoringSSL / Go crypto/tls")
            }
        }

        // Configuration & Storage Controls
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "DATA MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                ActionItemButton(
                    icon = Icons.Default.CloudDownload,
                    title = "Export Backup JSON",
                    subtitle = "Export all nodes and anti-DPI settings",
                    onClick = {
                        val sampleBackup = """{"version": 2, "activeNode": "${uiState.selectedServer?.alias}", "hopMode": "${uiState.hopMode}"}"""
                        clipboardManager.setText(AnnotatedString(sampleBackup))
                        Toast.makeText(context, "Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                ActionItemButton(
                    icon = Icons.Default.CloudUpload,
                    title = "Import Backup Config",
                    subtitle = "Restore subscription URL or JSON file",
                    onClick = {
                        Toast.makeText(context, "Ready to import configuration", Toast.LENGTH_SHORT).show()
                    }
                )

                ActionItemButton(
                    icon = Icons.Default.CleaningServices,
                    title = "Flush Routing Tables",
                    subtitle = "Clear cached interfaces & zombie tunnels",
                    onClick = {
                        viewModel.flushRoutes()
                        Toast.makeText(context, "Kernel routing table flushed", Toast.LENGTH_SHORT).show()
                    }
                )

                ActionItemButton(
                    icon = Icons.Default.DeleteSweep,
                    title = "Purge Telemetry Logs",
                    subtitle = "Remove historical daemon.log entries",
                    iconTint = ErrorRed,
                    onClick = {
                        viewModel.clearLogs()
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Community & Protocol Links
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "COMMUNITY & RESOURCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                Toast.makeText(context, "Opening GitHub repository...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "GitHub", tint = PrimaryCobalt, modifier = Modifier.size(18.dp))
                            Text("GitHub", style = MonoMetrics, color = TextPrimary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                            .clickable {
                                Toast.makeText(context, "Opening Telegram channel...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Telegram", tint = PrimaryCobalt, modifier = Modifier.size(18.dp))
                            Text("Telegram", style = MonoMetrics, color = PrimaryCobalt)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
    }
}

@Composable
fun ActionItemButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = PrimaryCobalt,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBright)
            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainer)
                        .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, style = MonoMetricsSmall, color = TextSecondary)
                }
            }
            Text("→", color = TextSecondary, fontSize = 16.sp)
        }
    }
}
