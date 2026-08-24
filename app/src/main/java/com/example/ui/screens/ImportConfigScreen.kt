package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.importer.ConfigLinkParser
import com.example.data.importer.ConfigParseResult
import com.example.ui.components.TacticalGlassCard
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MonoMetrics
import com.example.ui.theme.MonoMetricsSmall
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.SurfaceBright
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.TextOutline
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Full-screen overlay that lets the user paste (or later scan) a share link
 * for a supported protocol and import it into the server database.
 *
 * The parsing itself is delegated to [ConfigLinkParser] so the UI stays
 * framework-light and the parser can be unit tested on the JVM.
 */
@Composable
fun ImportConfigScreen(
    onImportServer: (rawLink: String, onResult: (ConfigParseResult) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var linkText by remember { mutableStateOf("") }
    var parseState by remember { mutableStateOf<ConfigParseResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val parse: (String) -> Unit = { raw ->
        parseState = ConfigLinkParser.parse(raw)
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
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("import_config_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Import Configuration",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                IconButton(
                    onClick = {
                        val pasted = clipboardManager.getText()?.text.orEmpty()
                        if (pasted.isBlank()) {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        } else {
                            linkText = pasted.trim()
                            parse(linkText)
                        }
                    },
                    modifier = Modifier.testTag("import_config_paste")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = PrimaryCobalt
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Input Card
            TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link",
                            tint = PrimaryCobalt,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CONFIGURATION LINK",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCobalt,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    OutlinedTextField(
                        value = linkText,
                        onValueChange = {
                            linkText = it
                            parseState = if (it.isBlank()) null else ConfigLinkParser.parse(it)
                        },
                        placeholder = {
                            Text(
                                "vless://   vmess://   trojan://   ss://   hysteria2://",
                                style = MonoMetricsSmall,
                                color = TextOutline
                            )
                        },
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceBright,
                            unfocusedContainerColor = SurfaceBright,
                            focusedBorderColor = PrimaryCobalt,
                            unfocusedBorderColor = OutlineVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MonoMetrics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_config_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val pasted = clipboardManager.getText()?.text.orEmpty()
                                if (pasted.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Clipboard is empty",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    linkText = pasted.trim()
                                    parse(linkText)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = PrimaryCobalt,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Paste", color = PrimaryCobalt)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "QR scanner will be available in a future update",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Scan QR", color = TextSecondary)
                        }
                    }
                }
            }

            // 2. Parse result / preview
            when (val result = parseState) {
                is ConfigParseResult.Success -> {
                    val server = result.server
                    TacticalGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = SecondaryEmerald
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid",
                                    tint = SecondaryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Ready to import",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary
                                )
                            }

                            PreviewRow("Name", server.alias)
                            PreviewRow("Protocol", server.protocol)
                            PreviewRow("Address", "${server.address}:${server.port}")
                            if (server.security.isNotBlank()) {
                                PreviewRow("Security", server.security)
                            }
                            if (server.transport.isNotBlank()) {
                                PreviewRow("Transport", server.transport)
                            }
                            if (server.sni.isNotBlank()) {
                                PreviewRow("SNI", server.sni)
                            }
                            if (server.flow.isNotBlank()) {
                                PreviewRow("Flow", server.flow)
                            }
                            if (server.publicKey.isNotBlank()) {
                                PreviewRow(
                                    "Public Key",
                                    server.publicKey.take(16) + "…"
                                )
                            }
                        }
                    }
                }
                is ConfigParseResult.Failure -> {
                    if (linkText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ErrorContainer)
                                .border(1.dp, ErrorRed, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("import_config_error")
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = result.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }
                null -> {
                    TacticalGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SUPPORTED SCHEMES",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Paste a vless, vmess, trojan, ss or hysteria2 share link. " +
                                    "The parser extracts host, port, UUID/password, security (Reality/TLS), " +
                                    "transport and SNI automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 3. Import action
            Button(
                onClick = {
                    val raw = linkText.trim()
                    if (raw.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Paste a configuration link first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    isImporting = true
                    onImportServer(raw) { result ->
                        isImporting = false
                        when (result) {
                            is ConfigParseResult.Success -> {
                                Toast.makeText(
                                    context,
                                    "Imported ${result.server.alias}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            }
                            is ConfigParseResult.Failure -> {
                                parseState = result
                                Toast.makeText(
                                    context,
                                    result.reason,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                enabled = !isImporting && parseState is ConfigParseResult.Success,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCobalt,
                    contentColor = Color.White,
                    disabledContainerColor = SurfaceContainerHigh,
                    disabledContentColor = TextOutline
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("import_config_save")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Import",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isImporting) "Importing…" else "Import node",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MonoMetricsSmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MonoMetricsSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}

