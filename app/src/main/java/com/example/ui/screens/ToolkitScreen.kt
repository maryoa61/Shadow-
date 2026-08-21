package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LogEntryEntity
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.TacticalGlassCard
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MonoMetrics
import com.example.ui.theme.MonoMetricsSmall
import com.example.ui.theme.OnPrimary
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
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TertiaryAmber
import com.example.ui.theme.TextOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VlessCyan
import com.example.viewmodel.VpnUiState
import com.example.viewmodel.VpnViewModel

enum class ToolkitSubTab(val title: String) {
    MULTI_HOP("Multi-Hop Matrix"),
    ANTI_DPI("Anti-DPI Toolkit"),
    SYSTEM_LOGS("System Logs")
}

@Composable
fun ToolkitScreen(
    viewModel: VpnViewModel,
    uiState: VpnUiState,
    logList: List<LogEntryEntity>
) {
    var selectedSubTab by remember { mutableStateOf(ToolkitSubTab.MULTI_HOP) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // Sub-tabs segment switcher (Sleek Theme segmented bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceContainer)
                .border(1.dp, OutlineVariant, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolkitSubTab.values().forEach { tab ->
                val isSelected = tab == selectedSubTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryContainer else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryCobalt else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSubTab = tab }
                        .padding(vertical = 8.dp)
                        .testTag("toolkit_subtab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tab) {
                            ToolkitSubTab.MULTI_HOP -> "Multi-Hop"
                            ToolkitSubTab.ANTI_DPI -> "Anti-DPI"
                            ToolkitSubTab.SYSTEM_LOGS -> "Logs"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        when (selectedSubTab) {
            ToolkitSubTab.MULTI_HOP -> MultiHopMatrixView(viewModel, uiState)
            ToolkitSubTab.ANTI_DPI -> AntiDpiToolkitView(viewModel, uiState)
            ToolkitSubTab.SYSTEM_LOGS -> SystemLogsView(viewModel, uiState, logList)
        }
    }
}

// -----------------------------------------------------------------------------------------
// 1. MULTI-HOP MATRIX VIEW
// -----------------------------------------------------------------------------------------
@Composable
fun MultiHopMatrixView(
    viewModel: VpnViewModel,
    uiState: VpnUiState
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Multi-Hop Matrix",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Cascade encrypted proxy hops across borders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACTIVE TOPOLOGY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontSize = 9.sp),
                    color = Color(0xFF001D36),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chain Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CASCADE PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val presets = listOf("Double-Hop NL", "WARP -> VLESS", "Shadow TLS Cascade")
                presets.forEach { preset ->
                    val isSelected = preset == uiState.chainPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryContainer else SurfaceBright)
                            .border(
                                1.dp,
                                if (isSelected) PrimaryCobalt else OutlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setChainPreset(preset) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset,
                            style = MonoMetrics,
                            color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Visual Flow Diagram
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Origin Node (Local Device)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceBright)
                        .border(1.dp, OutlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Device",
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("Local Device", style = MonoMetricsSmall, color = TextSecondary)
            }

            // Connecting line 1
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(PrimaryCobalt.copy(alpha = 0.5f))
            )

            // Hop 1 Card
            TacticalGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = PrimaryCobalt.copy(alpha = 0.4f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("HOP_01", style = MaterialTheme.typography.labelSmall, color = Color(0xFF001D36), fontWeight = FontWeight.Bold)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PulsingStatusDot(color = SecondaryEmerald, size = 6)
                                Text("ACTIVE", style = MonoMetricsSmall, color = SecondaryEmerald, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = SecondaryEmerald, modifier = Modifier.size(16.dp))
                            Text("${uiState.hop1Ping}ms", style = MonoMetricsSmall, color = SecondaryEmerald)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(uiState.hop1Server, style = MonoMetrics, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(uiState.hop1Proto, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = "More", tint = TextSecondary)
                        }
                    }
                }
            }

            // Connecting line 2
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(
                        Brush.verticalGradient(listOf(PrimaryCobalt.copy(alpha = 0.5f), OutlineVariant))
                    )
            )

            // Hop 2 Card
            TacticalGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceBright)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("HOP_02", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(TextOutline))
                                Text("STANDBY", style = MonoMetricsSmall, color = TextOutline)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = TextOutline, modifier = Modifier.size(16.dp))
                            Text("--ms", style = MonoMetricsSmall, color = TextOutline)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                            .clickable {
                                Toast.makeText(context, "Select server node for Hop 2", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(uiState.hop2Server, style = MonoMetrics, color = TextPrimary)
                                Text("Require exit node", style = MaterialTheme.typography.labelSmall, color = TextOutline)
                            }
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add node", tint = PrimaryCobalt)
                        }
                    }
                }
            }

            // Connecting line 3
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(OutlineVariant)
            )

            // Target Node (ClearNet)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceBright)
                        .border(1.dp, OutlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "ClearNet",
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("ClearNet", style = MonoMetricsSmall, color = TextSecondary)
            }
        }

        // Chain Parameters Card
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Params",
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Chain Parameters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }

                // Mux Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Multiplexing (Mux)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("CONCURRENCY LIMIT", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${uiState.muxConcurrency}", style = MonoMetrics, color = PrimaryCobalt, fontWeight = FontWeight.Bold)
                        }

                        Switch(
                            checked = uiState.muxEnabled,
                            onCheckedChange = { viewModel.toggleMux() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryCobalt
                            )
                        )
                    }
                }

                // Fragment Setting
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Packet Fragmentation", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("EVADE DPI SNIFFER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }

                        Switch(
                            checked = uiState.fragmentEnabled,
                            onCheckedChange = { viewModel.toggleFragment() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryCobalt
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("LENGTH (MIN-MAX)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                                Text("100-200", style = MonoMetricsSmall, color = TextPrimary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("INTERVAL (MS)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                                Text("10-20", style = MonoMetricsSmall, color = TextPrimary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // Deploy Chain Button
        Button(
            onClick = {
                Toast.makeText(context, "Deploying multi-hop chain topology...", Toast.LENGTH_SHORT).show()
                viewModel.setHopMode("2-HOP")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryCobalt,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("deploy_chain_button")
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = "Deploy",
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Deploy Chain",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -----------------------------------------------------------------------------------------
// 2. ANTI-DPI TOOLKIT VIEW
// -----------------------------------------------------------------------------------------
@Composable
fun AntiDpiToolkitView(
    viewModel: VpnViewModel,
    uiState: VpnUiState
) {
    val scrollState = rememberScrollState()
    var utlsDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Anti-DPI Toolkit",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = "Configure advanced obfuscation parameters to bypass Deep Packet Inspection systems.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // 1. Payload Obfuscation Card
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shuffle, contentDescription = "Payload", tint = PrimaryCobalt, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Payload Obfuscation",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // TLS Fragmentation
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TLS Fragmentation", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Split ClientHello to evade SNI detection.", style = MonoMetricsSmall, color = TextSecondary)
                        }
                        Switch(
                            checked = uiState.fragmentEnabled,
                            onCheckedChange = { viewModel.toggleFragment() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryCobalt
                            )
                        )
                    }

                    if (uiState.fragmentEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PACKETS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                                Text("${uiState.fragmentPackets}", style = MonoMetrics, color = PrimaryCobalt, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LENGTH", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                                Text("10-100", style = MonoMetrics, color = PrimaryCobalt, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("INTERVAL (ms)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                                Text("${uiState.fragmentIntervalMs}", style = MonoMetrics, color = PrimaryCobalt, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // uTLS Fingerprint Profile
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("uTLS Fingerprint Profile", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Spoof client TLS handshake signature.", style = MonoMetricsSmall, color = TextSecondary)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                                .clickable { utlsDropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${uiState.utlsProfile} (Default)", style = MonoMetrics, color = TextPrimary)
                                Text("▾", color = TextSecondary)
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = utlsDropdownExpanded,
                        onDismissRequest = { utlsDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceBright)
                    ) {
                        listOf("Chrome", "Firefox", "Safari", "iOS", "Randomized").forEach { prof ->
                            DropdownMenuItem(
                                text = { Text(prof, style = MonoMetrics, color = TextPrimary) },
                                onClick = {
                                    viewModel.setUtlsProfile(prof)
                                    utlsDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ECH (Encrypted Client Hello)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("ECH (Encrypted Client Hello)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryContainer)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("BETA", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color(0xFF001D36), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Requires server-side support.", style = MonoMetricsSmall, color = TextSecondary)
                    }

                    Switch(
                        checked = uiState.echEnabled,
                        onCheckedChange = { viewModel.toggleEch() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryCobalt
                        )
                    )
                }
            }
        }

        // 2. Routing & DNS Hardening Card
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Route, contentDescription = "DNS", tint = SecondaryEmerald, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Routing & DNS Hardening",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // Clean IP Override
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Clean IP Override", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Force connection through a specific unblocked IP.", style = MonoMetricsSmall, color = TextSecondary)

                    OutlinedTextField(
                        value = uiState.cleanIpOverride,
                        onValueChange = { viewModel.setCleanIpOverride(it) },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = "IP", tint = PrimaryCobalt, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceBright,
                            unfocusedContainerColor = SurfaceBright,
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = OutlineVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // DNS Configuration (DoH Selection)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DNS Configuration", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("SECURE DNS (DoH)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Cloudflare", "Google").forEach { provider ->
                            val isSelected = provider == uiState.dohProvider
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) PrimaryContainer else SurfaceBright
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryCobalt else OutlineVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setDohProvider(provider) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider,
                                    style = MonoMetrics,
                                    color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Enable Fake-IP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Fake-IP", style = MonoMetrics, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Reduce DNS leak risks.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    Switch(
                        checked = uiState.fakeIpEnabled,
                        onCheckedChange = { viewModel.toggleFakeIp() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SecondaryEmerald
                        )
                    )
                }

                // Domestic DNS Fallback
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DOMESTIC DNS FALLBACK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    OutlinedTextField(
                        value = uiState.domesticDnsFallback,
                        onValueChange = { viewModel.setDomesticDns(it) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceBright,
                            unfocusedContainerColor = SurfaceBright,
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = OutlineVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Strict Kill Switch Card
        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = ErrorRed.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Kill switch",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Strict Kill Switch",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ErrorRed
                        )
                        Text(
                            text = "Block all local network traffic instantly if the proxy connection drops. May cause temporary loss of internet connectivity.",
                            style = MonoMetricsSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Switch(
                    checked = uiState.strictKillSwitch,
                    onCheckedChange = { viewModel.toggleKillSwitch() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ErrorRed
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// -----------------------------------------------------------------------------------------
// 3. SYSTEM LOGS & TELEMETRY VIEW
// -----------------------------------------------------------------------------------------
@Composable
fun SystemLogsView(
    viewModel: VpnViewModel,
    uiState: VpnUiState,
    logList: List<LogEntryEntity>
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filteredLogs = when (uiState.activeLogFilter) {
        "ALL" -> logList
        "INFO" -> logList.filter { it.level == "INFO" || it.level == "OK" }
        "ERROR" -> logList.filter { it.level == "ERR" || it.level == "CRIT" }
        "DEBUG" -> logList.filter { it.level == "DBG" }
        else -> logList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Critical State Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Logs",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Real-time diagnostic stream & connection timeline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (uiState.isCriticalState) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ErrorContainer)
                        .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PulsingStatusDot(color = ErrorRed, size = 6)
                        Text(
                            text = "CRITICAL_STATE",
                            style = MonoMetricsSmall,
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Critical Zombie Tunnel Warning Banner
        if (uiState.isCriticalState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorContainer.copy(alpha = 0.5f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Zombie Tunnel Detected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ErrorRed
                        )
                        Text(
                            text = "The primary encrypted tunnel dropped unexpectedly, but the routing table remains modified. Traffic may leak outside the secure interface.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    viewModel.flushRoutes()
                                    Toast.makeText(context, "Routing tables flushed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Flush Routes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Tracing kernel socket descriptors...", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceBright,
                                    contentColor = ErrorRed
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            ) {
                                Text("View Traces", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Handshake Sequence Timeline
        TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Timeline, contentDescription = "Timeline", tint = PrimaryCobalt, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Handshake Sequence",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                TimelineStepItem(
                    time = "00:00:00.045",
                    title = "Dialing Endpoint",
                    subtitle = "TCP to 198.51.100.44:443",
                    isSuccess = true
                )

                TimelineStepItem(
                    time = "00:00:00.112",
                    title = "TLS Handshake",
                    subtitle = "Negotiated TLSv1.3 (ChaCha20-Poly1305)",
                    isSuccess = true
                )

                TimelineStepItem(
                    time = "00:00:00.230",
                    title = "Authenticating",
                    subtitle = "Exchanging reality certs...",
                    isSuccess = true
                )

                TimelineStepItem(
                    time = "00:00:04.891",
                    title = "Tunnel Established (Dropped)",
                    subtitle = "Keepalive timeout exceeded. Stream closed unexpectedly.",
                    isSuccess = false
                )
            }
        }

        // Terminal Log Viewer Card (daemon.log)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceBright)
                .border(1.dp, OutlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column {
                // Header & Filter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = "Log", tint = PrimaryCobalt, modifier = Modifier.size(16.dp))
                        Text("daemon.log", style = MonoMetrics, color = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "INFO", "ERROR", "DEBUG").forEach { filter ->
                            val isSelected = filter == uiState.activeLogFilter
                            val bg = when {
                                isSelected && filter == "ERROR" -> ErrorContainer
                                isSelected -> PrimaryContainer
                                else -> SurfaceBright
                            }
                            val textCol = when {
                                isSelected && filter == "ERROR" -> ErrorRed
                                isSelected -> Color(0xFF001D36)
                                else -> TextSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.dp, if (isSelected) OutlineVariant else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setLogFilter(filter) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = textCol,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Log Stream Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerLow)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredLogs.forEach { log ->
                        val levelColor = when (log.level) {
                            "INFO" -> PrimaryCobalt
                            "OK" -> SecondaryEmerald
                            "DBG" -> TextOutline
                            "ERR" -> ErrorRed
                            "CRIT" -> ErrorRed
                            else -> TextSecondary
                        }
                        val isCrit = log.level == "CRIT"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isCrit) Modifier
                                        .background(ErrorContainer.copy(alpha = 0.5f))
                                        .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = log.timeFormatted,
                                style = MonoMetricsSmall,
                                color = TextSecondary,
                                modifier = Modifier.width(64.dp)
                            )
                            Text(
                                text = "[${log.level}]",
                                style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold),
                                color = levelColor,
                                modifier = Modifier.width(52.dp)
                            )
                            Text(
                                text = log.message,
                                style = MonoMetricsSmall,
                                color = if (isCrit) ErrorRed else TextPrimary
                            )
                        }
                    }

                    // Terminal Cursor
                    Text(
                        text = "_",
                        style = MonoMetricsSmall,
                        color = PrimaryCobalt
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TimelineStepItem(
    time: String,
    title: String,
    subtitle: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSuccess) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SecondaryEmerald)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ErrorRed)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(time, style = MonoMetricsSmall, color = if (isSuccess) SecondaryEmerald else ErrorRed)
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = if (isSuccess) TextPrimary else ErrorRed)
            Text(subtitle, style = MonoMetricsSmall, color = TextSecondary)
        }
    }
}
