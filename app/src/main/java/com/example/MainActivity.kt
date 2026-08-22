package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ServerEntity
import com.example.ui.components.NavigationTab
import com.example.ui.components.TacticalBottomNavBar
import com.example.ui.components.TacticalTopAppBar
import com.example.ui.screens.EditServerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.QrScannerDialog
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ShadowNetApp()
            }
        }
    }
}

@Composable
fun ShadowNetApp(vpnViewModel: VpnViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by vpnViewModel.uiState.collectAsState()
    val serverList by vpnViewModel.servers.collectAsState()
    val logList by vpnViewModel.logs.collectAsState()

    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var editingServer by remember { mutableStateOf<ServerEntity?>(null) }
    var isEditingServerOpen by remember { mutableStateOf(false) }
    var isQrScannerOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        Scaffold(
            topBar = {
                TacticalTopAppBar(
                    title = "SHADOW_NET",
                    onLeftIconClick = {
                        Toast.makeText(
                            context,
                            "Zero-Knowledge Tunnel Active (${uiState.activeProtocolName})",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onRightIconClick = {
                        Toast.makeText(
                            context,
                            "Latency RTT: ${uiState.pingMs}ms | Jitter: ${uiState.jitterMs}ms",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            bottomBar = {
                TacticalBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { tab -> currentTab = tab }
                )
            },
            containerColor = BackgroundDeep
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        NavigationTab.HOME -> HomeScreen(
                            viewModel = vpnViewModel,
                            uiState = uiState,
                            onNavigateToServers = { currentTab = NavigationTab.SERVERS }
                        )

                        NavigationTab.SERVERS -> ServersScreen(
                            viewModel = vpnViewModel,
                            uiState = uiState,
                            serverList = serverList,
                            onOpenEditServer = { server ->
                                editingServer = server
                                isEditingServerOpen = true
                            },
                            onOpenQrScanner = {
                                isQrScannerOpen = true
                            }
                        )

                        NavigationTab.LOGS -> LogsScreen(
                            viewModel = vpnViewModel,
                            uiState = uiState,
                            logs = logList
                        )

                        NavigationTab.SETTINGS -> SettingsScreen(
                            viewModel = vpnViewModel,
                            uiState = uiState
                        )
                    }
                }
            }
        }

        // Full Screen Edit Server Overlay
        if (isEditingServerOpen) {
            EditServerScreen(
                serverToEdit = editingServer,
                viewModel = vpnViewModel,
                uiState = uiState,
                onDismiss = {
                    isEditingServerOpen = false
                    editingServer = null
                }
            )
        }

        // QR Scanner Overlay
        if (isQrScannerOpen) {
            QrScannerDialog(
                viewModel = vpnViewModel,
                onDismiss = {
                    isQrScannerOpen = false
                }
            )
        }
    }
}
