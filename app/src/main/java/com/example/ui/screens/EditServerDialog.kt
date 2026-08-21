package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ServerEntity
import com.example.ui.components.TacticalGlassCard
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MonoMetrics
import com.example.ui.theme.MonoMetricsSmall
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.TextOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VpnUiState
import com.example.viewmodel.VpnViewModel

@Composable
fun EditServerScreen(
    serverToEdit: ServerEntity?,
    viewModel: VpnViewModel,
    uiState: VpnUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var alias by remember { mutableStateOf(serverToEdit?.alias ?: "US-East-Alpha-01") }
    var address by remember { mutableStateOf(serverToEdit?.address ?: "192.168.1.100") }
    var port by remember { mutableStateOf(serverToEdit?.port?.toString() ?: "443") }
    var uuid by remember { mutableStateOf(serverToEdit?.uuid ?: "a1b2c3d4-e5f6-7890-1234-567890abcdef") }
    var showPassword by remember { mutableStateOf(false) }

    var selectedProtocol by remember { mutableStateOf(serverToEdit?.protocol ?: "VLESS") }
    var selectedSecurity by remember { mutableStateOf(serverToEdit?.security ?: "Reality") }
    var publicKey by remember { mutableStateOf(serverToEdit?.publicKey ?: "abcd1234efgh5678ijkl9012mnop") }
    var shortId by remember { mutableStateOf(serverToEdit?.shortId ?: "16") }
    var spiderX by remember { mutableStateOf(serverToEdit?.spiderX ?: "/") }
    var sni by remember { mutableStateOf(serverToEdit?.sni ?: "microsoft.com") }
    var flow by remember { mutableStateOf(serverToEdit?.flow ?: "xtls-rprx-vision") }
    var transport by remember { mutableStateOf(serverToEdit?.transport ?: "TCP") }
    var cleanIp by remember { mutableStateOf(serverToEdit?.cleanIp ?: "") }

    val handleSave = {
        val parsedPort = port.toIntOrNull() ?: 443
        val updated = (serverToEdit ?: ServerEntity(
            alias = alias,
            address = address,
            port = parsedPort,
            uuid = uuid
        )).copy(
            alias = alias.ifEmpty { "Custom-Node" },
            address = address.ifEmpty { "127.0.0.1" },
            port = parsedPort,
            uuid = uuid,
            protocol = selectedProtocol,
            security = selectedSecurity,
            publicKey = publicKey,
            shortId = shortId,
            spiderX = spiderX,
            sni = sni,
            flow = flow,
            transport = transport,
            cleanIp = cleanIp
        )
        viewModel.saveOrUpdateServer(updated) {
            Toast.makeText(context, "Server configuration saved", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // Top App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(SurfaceBright)
                .border(1.dp, OutlineVariant)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("edit_server_back")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Edit Server",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val configLink = "$selectedProtocol://$uuid@$address:$port?security=$selectedSecurity&sni=$sni&flow=$flow#$alias"
                            clipboardManager.setText(AnnotatedString(configLink))
                            Toast.makeText(context, "Config link copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR",
                            tint = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = handleSave,
                        modifier = Modifier.testTag("edit_server_save")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = PrimaryCobalt
                        )
                    }
                }
            }
        }

        // Form Fields Scrollable Canvas
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TARGET IDENTITY SECTION
            TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Identity",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TARGET IDENTITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCobalt,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Alias
                    DarkInputField(
                        label = "Alias (Name)",
                        value = alias,
                        onValueChange = { alias = it },
                        placeholder = "US-East-Alpha-01"
                    )

                    // Address and Port in 2 columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DarkInputField(
                            label = "Address / Host",
                            value = address,
                            onValueChange = { address = it },
                            placeholder = "192.168.1.100",
                            modifier = Modifier.weight(2f)
                        )

                        DarkInputField(
                            label = "Port",
                            value = port,
                            onValueChange = { port = it },
                            placeholder = "443",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // UUID / Password with eye visibility
                    Column {
                        Text(
                            text = "UUID / Password",
                            style = MonoMetricsSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                        OutlinedTextField(
                            value = uuid,
                            onValueChange = { uuid = it },
                            placeholder = { Text("UUID", style = MonoMetricsSmall, color = TextOutline) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
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

            // 2. PROTOCOL SELECTOR
            TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val protocols = listOf("VLESS", "VMess", "Trojan", "Shadowsocks", "Hysteria2")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        protocols.forEach { proto ->
                            val isSelected = proto == selectedProtocol
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryContainer else SurfaceBright)
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryCobalt else OutlineVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedProtocol = proto }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = proto,
                                    style = MonoMetrics,
                                    color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Protocol Info notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryContainer.copy(alpha = 0.5f))
                            .border(1.dp, PrimaryCobalt.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$selectedProtocol protocol selected. Ensure parameters match server.",
                                style = MonoMetricsSmall,
                                color = Color(0xFF001D36)
                            )
                        }
                    }
                }
            }

            // 3. SECURITY LAYER
            TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SECURITY LAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCobalt,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Security Type Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainer)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("None", "TLS", "Reality").forEach { sec ->
                            val isSelected = sec == selectedSecurity
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) PrimaryContainer else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryCobalt else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedSecurity = sec }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sec,
                                    style = MonoMetrics,
                                    color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (selectedSecurity == "Reality") {
                        DarkInputField(
                            label = "PublicKey",
                            value = publicKey,
                            onValueChange = { publicKey = it },
                            placeholder = "abcd1234efgh5678ijkl9012mnop"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DarkInputField(
                                label = "ShortId",
                                value = shortId,
                                onValueChange = { shortId = it },
                                placeholder = "16",
                                modifier = Modifier.weight(1f)
                            )
                            DarkInputField(
                                label = "SpiderX",
                                value = spiderX,
                                onValueChange = { spiderX = it },
                                placeholder = "/",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        DarkInputField(
                            label = "SNI / Dest",
                            value = sni,
                            onValueChange = { sni = it },
                            placeholder = "microsoft.com"
                        )

                        DarkInputField(
                            label = "Flow Selector",
                            value = flow,
                            onValueChange = { flow = it },
                            placeholder = "xtls-rprx-vision"
                        )

                        // Vision Flow Warning
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ErrorContainer.copy(alpha = 0.5f))
                                .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Mux will be forced OFF with Vision flow.",
                                    style = MonoMetricsSmall,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }
            }

            // 4. TRANSPORT MATRIX
            TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "Transport",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TRANSPORT MATRIX",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCobalt,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    val transports = listOf("TCP", "WS", "gRPC", "XHTTP", "H2", "mKCP")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        transports.forEach { t ->
                            val isSelected = t == transport
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) PrimaryContainer else SurfaceBright
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryCobalt else OutlineVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { transport = t }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = t,
                                    style = MonoMetricsSmall,
                                    color = if (isSelected) Color(0xFF001D36) else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 5. Clean IP Section
            TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DarkInputField(
                            label = "Clean IP (Optional)",
                            value = cleanIp,
                            onValueChange = { cleanIp = it },
                            placeholder = "e.g. 104.16.0.0",
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (cleanIp.isNotEmpty()) {
                                    viewModel.testCleanIp(cleanIp)
                                } else {
                                    Toast.makeText(context, "Enter an IP address to test", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceBright,
                                contentColor = SecondaryEmerald
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .border(1.dp, SecondaryEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            if (uiState.isTestingCleanIp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SecondaryEmerald,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = "Test",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TEST", style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    if (uiState.cleanIpTestResult != null) {
                        Text(
                            text = uiState.cleanIpTestResult,
                            style = MonoMetricsSmall,
                            color = SecondaryEmerald,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Bottom Actions (Copy Link, Generate QR)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val configLink = "$selectedProtocol://$uuid@$address:$port?security=$selectedSecurity&sni=$sni&flow=$flow#$alias"
                        clipboardManager.setText(AnnotatedString(configLink))
                        Toast.makeText(context, "Config link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryCobalt)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        tint = PrimaryCobalt,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Link", color = PrimaryCobalt, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "QR code generated for $alias", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryCobalt,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate QR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DarkInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MonoMetricsSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MonoMetricsSmall, color = TextOutline) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
