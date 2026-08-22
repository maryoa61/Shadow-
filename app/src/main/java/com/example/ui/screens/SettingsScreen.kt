package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.PrimaryCobalt
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VpnUiState
import com.example.viewmodel.VpnViewModel

@Composable
fun SettingsScreen(viewModel: VpnViewModel, uiState: VpnUiState) {
    Column(Modifier.fillMaxSize().background(BackgroundDeep).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary); Text("Control how Shadow Net connects", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
        SettingGroup("Routing") {
            SettingRow("Routing mode", uiState.hopMode, action = { viewModel.setHopMode(if (uiState.hopMode == "DIRECT") "2-HOP" else "DIRECT") })
            SettingRow("Custom domain rules", "Manage bypass and block lists")
        }
        SettingGroup("DNS") {
            SettingRow("Secure DNS", uiState.dohProvider, action = { viewModel.setDohProvider(if (uiState.dohProvider == "Cloudflare") "Google" else "Cloudflare") })
            ToggleRow("FakeIP mode", "Resolve domains without leaking lookups", uiState.fakeIpEnabled, viewModel::toggleFakeIp)
        }
        SettingGroup("Fragmentation") {
            ToggleRow("TLS fragmentation", "Split handshakes to reduce DPI detection", uiState.fragmentEnabled, viewModel::toggleFragment)
            SettingRow("Length & interval", "${uiState.fragmentLength} · ${uiState.fragmentIntervalMs} ms")
        }
        SettingGroup("Fingerprint") { SettingRow("TLS client profile", uiState.utlsProfile, action = { viewModel.setUtlsProfile(if (uiState.utlsProfile == "Chrome") "Firefox" else "Chrome") }) }
        SettingGroup("Safety") { ToggleRow("Kill switch", "Block all traffic if the tunnel disconnects", uiState.strictKillSwitch, viewModel::toggleKillSwitch) }
    }
}
@Composable private fun SettingGroup(title: String, content: @Composable () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold); Column(Modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceContainer).border(1.dp, OutlineVariant, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp), content = { content() }) } }
@Composable private fun SettingRow(title: String, subtitle: String, action: () -> Unit = {}) { Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge); Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ChevronRight, null, tint = TextSecondary) } }
@Composable private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onChange() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge); Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }; Switch(checked, { onChange() }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryCobalt, checkedThumbColor = Color.White)) } }
