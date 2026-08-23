package com.example

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.screens.ImportConfigScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.ToolkitScreen
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
    val uiState by vpnViewModel.uiState.collectAsStateWithLifecycle()
    val serverList by vpnViewModel.servers.collectAsStateWithLifecycle()
    val logList by vpnViewModel.logs.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var editingServer by remember { mutableStateOf<ServerEntity?>(null) }
    var isEditingServerOpen by remember { mutableStateOf(false) }
    var isImportConfigOpen by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vpnViewModel.startConnection()
        } else {
            vpnViewModel.reportVpnPermissionDenied()
        }
    }

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
                            if (uiState.isConnected) {
                                "VPN active • ${uiState.activeCore} • ${uiState.activeProtocolName}"
                            } else {
                                "VPN disconnected"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onRightIconClick = {
                        Toast.makeText(
                            context,
                            if (uiState.isConnected) {
                                "Verified route RTT: ${uiState.pingMs}ms"
                            } else {
                                "No active VPN route"
                            },
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
                            onNavigateToServers = { currentTab = NavigationTab.SERVERS },
                            onToggleConnection = {
                                when {
                                    uiState.isConnected || uiState.isConnecting -> {
                                        vpnViewModel.stopConnection()
                                    }
                                    uiState.selectedServer == null -> {
                                        Toast.makeText(
                                            context,
                                            "Add and select a real server first",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        currentTab = NavigationTab.SERVERS
                                    }
                                    else -> {
                                        val permissionIntent = VpnService.prepare(context)
                                        if (permissionIntent == null) {
                                            vpnViewModel.startConnection()
                                        } else {
                                            vpnPermissionLauncher.launch(permissionIntent)
                                        }
                                    }
                                }
                            }
                        )

                        NavigationTab.SERVERS -> ServersScreen(
                            viewModel = vpnViewModel,
                            uiState = uiState,
                            serverList = serverList,
                            onOpenEditServer = { server ->
                                editingServer = server
                                isEditingServerOpen = true
                            },
                            onOpenImportConfig = { isImportConfigOpen = true }
                        )

                        NavigationTab.TOOLKIT -> ToolkitScreen(
                            viewModel = vpnViewModel,
                            uiState = uiState,
                            logList = logList
                        )

                        NavigationTab.PROFILE -> ProfileScreen(
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

        // Full Screen Import Configuration Overlay
        if (isImportConfigOpen) {
            ImportConfigScreen(
                onImportServer = { rawLink, onResult ->
                    vpnViewModel.importConfigFromLink(rawLink, onResult)
                },
                onDismiss = { isImportConfigOpen = false }
            )
        }

    }
}
