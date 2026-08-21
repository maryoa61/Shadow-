package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ServerEntity
import com.example.ui.components.FlagBadge
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.TacticalGlassCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GrpcPink
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
import com.example.ui.theme.SurfaceDim
import com.example.ui.theme.TertiaryAmber
import com.example.ui.theme.TextOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VlessCyan
import com.example.viewmodel.VpnUiState
import com.example.viewmodel.VpnViewModel

@Composable
fun ServersScreen(
    viewModel: VpnViewModel,
    uiState: VpnUiState,
    serverList: List<ServerEntity>,
    onOpenEditServer: (ServerEntity?) -> Unit,
    onOpenQrScanner: () -> Unit
) {
    var expandedServerId by remember { mutableStateOf<Long?>(null) }

    val filteredServers = serverList.filter { server ->
        val matchesProtocol = when (uiState.currentFilterProtocol) {
            "All" -> true
            "VLESS" -> server.protocol.equals("VLESS", ignoreCase = true)
            "Reality" -> server.security.equals("Reality", ignoreCase = true) || server.protocol.equals("Reality", ignoreCase = true)
            "Trojan" -> server.protocol.equals("Trojan", ignoreCase = true)
            "VMess" -> server.protocol.equals("VMess", ignoreCase = true)
            "Shadowsocks" -> server.protocol.equals("Shadowsocks", ignoreCase = true)
            "Hysteria2" -> server.protocol.equals("Hysteria2", ignoreCase = true)
            else -> true
        }
        val matchesQuery = uiState.searchQuery.isEmpty() ||
                server.alias.contains(uiState.searchQuery, ignoreCase = true) ||
                server.address.contains(uiState.searchQuery, ignoreCase = true) ||
                server.countryName.contains(uiState.searchQuery, ignoreCase = true)
        matchesProtocol && matchesQuery
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Search Input Field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search nodes, protocols, or locations...", style = MaterialTheme.typography.bodyMedium, color = TextOutline) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryCobalt
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceBright,
                        unfocusedContainerColor = SurfaceContainer,
                        focusedBorderColor = PrimaryCobalt,
                        unfocusedBorderColor = OutlineVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_search_input")
                )
            }

            item {
                // Protocol Filter Chips Horizontal Scroll
                val filterOptions = listOf("All", "VLESS", "Reality", "Trojan", "VMess", "Shadowsocks", "Hysteria2")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = filter == uiState.currentFilterProtocol

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PrimaryContainer else SurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryCobalt else OutlineVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.setFilterProtocol(filter) }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                                .testTag("filter_chip_$filter")
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            items(filteredServers, key = { it.id }) { server ->
                ServerListItemCard(
                    server = server,
                    isExpanded = expandedServerId == server.id,
                    onToggleExpand = {
                        expandedServerId = if (expandedServerId == server.id) null else server.id
                    },
                    onSelect = {
                        viewModel.selectServer(server)
                    },
                    onEdit = {
                        onOpenEditServer(server)
                    },
                    onDelete = {
                        viewModel.deleteServer(server)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Buttons on Bottom Right (Sleek Theme FAB)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = { onOpenEditServer(null) },
                containerColor = SurfaceBright,
                contentColor = PrimaryCobalt,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, OutlineVariant, RoundedCornerShape(16.dp))
                    .testTag("add_server_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Paste / Add",
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = onOpenQrScanner,
                containerColor = PrimaryContainer,
                contentColor = Color(0xFF001D36),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .size(56.dp)
                    .border(1.dp, PrimaryCobalt.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .testTag("qr_scanner_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "QR Scanner",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun ServerListItemCard(
    server: ServerEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val pingColor = when {
        server.pingMs < 60 -> SecondaryEmerald
        server.pingMs < 120 -> TertiaryAmber
        else -> ErrorRed
    }

    TacticalGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .testTag("server_card_${server.id}"),
        borderColor = if (server.isSelected) PrimaryCobalt else OutlineVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    FlagBadge(countryCode = server.countryCode)

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = server.alias,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            if (server.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Selected",
                                    tint = PrimaryCobalt,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryContainer)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = server.protocol.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color(0xFF001D36),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (server.bestForRegionTag.isNotEmpty()) {
                                Text(
                                    text = server.bestForRegionTag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Ping & latency indicator
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (server.isSelected) {
                            PulsingStatusDot(color = SecondaryEmerald, size = 6)
                        }
                        Text(
                            text = "${server.pingMs}ms",
                            style = MonoMetrics.copy(fontWeight = FontWeight.Bold),
                            color = pingColor
                        )
                    }

                    val latencyProgress = (1f - (server.pingMs / 300f)).coerceIn(0.15f, 1f)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceBright)
                            .padding(top = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(latencyProgress)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(pingColor)
                        )
                    }
                }
            }

            // Expanded Specs & Actions
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricSmallBox(
                            title = "THROUGHPUT",
                            value = server.throughput,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSmallBox(
                            title = "PACKET LOSS",
                            value = server.packetLoss,
                            valueColor = if (server.packetLoss == "0.0%") SecondaryEmerald else TertiaryAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSmallBox(
                            title = "HANDSHAKE",
                            value = server.handshake,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (server.isSelected) PrimaryContainer else PrimaryCobalt,
                                contentColor = if (server.isSelected) Color(0xFF001D36) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Connect",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (server.isSelected) "ACTIVE NODE" else "CONNECT TO NODE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceBright)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSmallBox(
    title: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceBright)
            .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextSecondary
            )
            Text(
                text = value,
                style = MonoMetrics.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = valueColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
