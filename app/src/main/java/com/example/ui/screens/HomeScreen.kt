package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.Sparkline
import com.example.ui.components.TacticalGlassCard
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MonoMetrics
import com.example.ui.theme.MonoMetricsLarge
import com.example.ui.theme.MonoMetricsSmall
import com.example.ui.theme.OnPrimaryContainer
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.RealityIndigo
import com.example.ui.theme.SecondaryContainer
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
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: VpnViewModel,
    uiState: VpnUiState,
    onNavigateToServers: () -> Unit
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
        // Main Connection Hero Card (Sleek Theme Sky Blue Banner)
        ConnectionHeroCard(
            uiState = uiState,
            onToggleConnection = { viewModel.toggleConnection() }
        )

        // Telemetry Metrics Grid (DL Rate, UL Rate, Total Usage)
        MetricsGridSection(uiState = uiState)

        // Active Server Information Card
        ActiveServerCard(
            uiState = uiState,
            onSwapServer = onNavigateToServers
        )

        // Configuration Toggles (Fragment, uTLS, Mux)
        ConfigurationSection(
            uiState = uiState,
            onToggleFragment = { viewModel.toggleFragment() },
            onSelectUtls = { viewModel.setUtlsProfile(it) },
            utlsDropdownExpanded = utlsDropdownExpanded,
            onSetUtlsDropdown = { utlsDropdownExpanded = it }
        )

        // Routing Chain Mode Selector
        RoutingChainSelector(
            currentMode = uiState.hopMode,
            onSelectMode = { viewModel.setHopMode(it) }
        )

        // Technical Readout & Connection Map
        TechnicalReadoutCard(uiState = uiState)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ConnectionHeroCard(
    uiState: VpnUiState,
    onToggleConnection: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(if (uiState.isConnected) PrimaryContainer else SurfaceContainer)
            .border(1.dp, OutlineVariant, RoundedCornerShape(32.dp))
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                if (uiState.isConnected) {
                    PulsingStatusDot(color = SecondaryEmerald, size = 8)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURE LINK ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = Color(0xFF001D36),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TextOutline)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISCONNECTED - STANDBY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Monospace Timer
            Text(
                text = if (uiState.isConnected) uiState.connectionDurationFormatted else "00:00:00",
                style = MonoMetricsLarge,
                color = if (uiState.isConnected) Color(0xFF001D36) else TextPrimary,
                fontSize = 38.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Connect / Disconnect Sleek Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .padding(4.dp)
            ) {
                if (uiState.isConnected) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .border(2.dp, ErrorRed.copy(alpha = 0.4f), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isConnected) ErrorRed else PrimaryCobalt)
                        .clickable { onToggleConnection() }
                        .testTag("connect_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isConnected) Icons.Default.Stop else Icons.Default.PowerSettingsNew,
                        contentDescription = if (uiState.isConnected) "Disconnect" else "Connect",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = if (uiState.isConnected) "TAP TO DISCONNECT" else "TAP TO CONNECT",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.isConnected) Color(0xFF001D36).copy(alpha = 0.8f) else TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun MetricsGridSection(uiState: VpnUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "TELEMETRY METRICS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // DL Rate Card
            TacticalGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(124.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DOWNLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "DL",
                                tint = SecondaryEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${uiState.dlRateMbps}",
                            style = MonoMetrics.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Mbps",
                            style = MonoMetricsSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Sparkline(
                        color = SecondaryEmerald,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        isDownload = true
                    )
                }
            }

            // UL Rate Card
            TacticalGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(124.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "UL",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${uiState.ulRateMbps}",
                            style = MonoMetrics.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Mbps",
                            style = MonoMetricsSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Sparkline(
                        color = PrimaryCobalt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        isDownload = false
                    )
                }
            }
        }

        // Total Usage Full Width Card
        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = "Usage",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "BANDWIDTH ALLOCATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.3f", uiState.totalUsageGb)} / ${uiState.usageLimitGb.toInt()} GB",
                        style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                val progress = (uiState.totalUsageGb / uiState.usageLimitGb).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceBright)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryCobalt)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveServerCard(
    uiState: VpnUiState,
    onSwapServer: () -> Unit
) {
    val server = uiState.selectedServer
    val serverName = server?.alias ?: "Frankfurt Edge 1"
    val proto = if (server != null) "${server.protocol}+${server.security}" else "VLESS+Reality"
    val regionTag = server?.bestForRegionTag ?: "Best for Performance"
    val rtt = server?.pingMs ?: uiState.pingMs
    val capacity = server?.capacity ?: "120Mbps"
    val ip = server?.ipAddress ?: "185.12.x.x"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ACTIVE GATEWAY",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🇩🇪",
                                fontSize = 22.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = serverName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = proto,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF001D36),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = regionTag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, CircleShape)
                            .clickable { onSwapServer() }
                            .testTag("swap_server_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Server",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OutlineVariant)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("LATENCY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${rtt}ms", style = MonoMetrics.copy(fontWeight = FontWeight.Bold), color = SecondaryEmerald)
                    }
                    Column {
                        Text("CAPACITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(capacity, style = MonoMetrics.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("IP ADDRESS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(ip, style = MonoMetrics.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigurationSection(
    uiState: VpnUiState,
    onToggleFragment: () -> Unit,
    onSelectUtls: (String) -> Unit,
    utlsDropdownExpanded: Boolean,
    onSetUtlsDropdown: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "PROTOCOL & ANTI-DPI CONTROLS",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Fragment Setting Card
        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Fragment",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text("Packet Fragmentation", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(uiState.fragmentLength, style = MonoMetricsSmall, color = PrimaryCobalt)
                    }
                }

                Switch(
                    checked = uiState.fragmentEnabled,
                    onCheckedChange = { onToggleFragment() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryCobalt,
                        uncheckedThumbColor = TextOutline,
                        uncheckedTrackColor = SurfaceContainerHigh
                    ),
                    modifier = Modifier.testTag("fragment_toggle")
                )
            }
        }

        // uTLS Profile Card with Dropdown
        Box(modifier = Modifier.fillMaxWidth()) {
            TacticalGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetUtlsDropdown(true) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "uTLS",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text("uTLS Client Spoofing", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(uiState.utlsProfile, style = MonoMetricsSmall, color = TextSecondary)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = TextSecondary
                    )
                }
            }

            DropdownMenu(
                expanded = utlsDropdownExpanded,
                onDismissRequest = { onSetUtlsDropdown(false) },
                modifier = Modifier.background(SurfaceBright)
            ) {
                listOf("Chrome", "Firefox", "Safari", "iOS", "Randomized").forEach { profile ->
                    DropdownMenuItem(
                        text = { Text(profile, style = MonoMetrics, color = TextPrimary) },
                        onClick = {
                            onSelectUtls(profile)
                            onSetUtlsDropdown(false)
                        }
                    )
                }
            }
        }

        // Mux Card (Locked / Info)
        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceBright)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = TextOutline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text("Multiplexing (Mux)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Disabled (Reality Conflict)", style = MaterialTheme.typography.labelSmall, color = TextOutline)
                    }
                }

                Switch(
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                    colors = SwitchDefaults.colors(
                        disabledUncheckedThumbColor = TextOutline.copy(alpha = 0.5f),
                        disabledUncheckedTrackColor = SurfaceContainerHigh.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun RoutingChainSelector(
    currentMode: String,
    onSelectMode: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "ROUTING TOPOLOGY",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modes = listOf("1-HOP", "2-HOP", "DIRECT")
                modes.forEachIndexed { index, mode ->
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryContainer else SurfaceBright)
                            .border(
                                1.dp,
                                if (isSelected) PrimaryCobalt else OutlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("hop_mode_$mode")
                    ) {
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    if (index < modes.size - 1) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "arrow",
                            tint = PrimaryCobalt,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicalReadoutCard(uiState: VpnUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DAEMON TELEMETRY & ROUTE",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        TacticalGlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Flow Control:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(uiState.flow, style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold), color = PrimaryCobalt)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Multiplex Mode:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("Off", style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold), color = TextOutline)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("uTLS Client Fingerprint:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(uiState.utlsProfile, style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold), color = SecondaryEmerald)
                }

                // Graphical Map Path
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceBright)
                        .border(1.dp, OutlineVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryEmerald)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tehran", style = MonoMetricsSmall.copy(fontSize = 10.sp), color = TextSecondary)
                        }

                        // Stream bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .padding(horizontal = 10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SecondaryEmerald, PrimaryCobalt, Color(0xFF001D36))
                                    )
                                )
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryCobalt)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Frankfurt", style = MonoMetricsSmall.copy(fontSize = 10.sp), color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
