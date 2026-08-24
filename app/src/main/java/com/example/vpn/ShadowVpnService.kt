package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.ServerEntity
import go.Seq
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class ShadowVpnService : VpnService(), CoreCallbackHandler {
    companion object {
        private const val TAG = "ShadowNet/VPN"
        private const val ACTION_START = "com.example.vpn.START"
        private const val ACTION_STOP = "com.example.vpn.STOP"
        private const val EXTRA_CORE = "core"
        private const val CHANNEL_ID = "shadow_net_vpn"
        private const val NOTIFICATION_ID = 4101
        private const val MTU = 1500
        private const val CORE_PREFS = "shadownet.core"
        private const val XUDP_BASE_KEY_PREF = "xudp_base_key"

        fun start(context: Context, preferredCore: CoreEngine) {
            val intent = Intent(context, ShadowVpnService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_CORE, preferredCore.persistedValue)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ShadowVpnService::class.java).setAction(ACTION_STOP)
            )
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    private var startJob: Job? = null
    private var telemetryJob: Job? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private var coreController: CoreController? = null
    private var singBoxProcess: SingBoxProcess? = null
    private var connectedAtElapsedMs = 0L
    private var totalDownloadBytes = 0L
    private var totalUploadBytes = 0L
    @Volatile private var userStopping = false
    @Volatile private var destroyed = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        singBoxProcess = SingBoxProcess(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userStopping = false
                startForegroundCompat(buildNotification("Preparing secure tunnel…"))
                val preferred = CoreEngine.fromPersisted(intent.getStringExtra(EXTRA_CORE))
                startJob?.cancel()
                startJob = serviceScope.launch {
                    operationMutex.withLock {
                        startConnection(preferred)
                    }
                }
            }
            ACTION_STOP -> requestStop()
            else -> {
                VpnRuntime.set(VpnRuntimeState())
                stopSelf(startId)
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onRevoke() {
        VpnRuntime.set(
            VpnRuntimeState(
                phase = ConnectionPhase.ERROR,
                error = "Android revoked VPN permission."
            )
        )
        requestStop(keepError = true)
        super.onRevoke()
    }

    override fun onDestroy() {
        destroyed = true
        startJob?.cancel()
        telemetryJob?.cancel()
        runCatching {
            runBlocking(Dispatchers.IO) { stopEngines(closeTun = true) }
        }
        serviceScope.cancel()
        if (!userStopping && VpnRuntime.state.value.phase != ConnectionPhase.ERROR) {
            VpnRuntime.set(VpnRuntimeState())
        }
        super.onDestroy()
    }

    private suspend fun startConnection(requestedCore: CoreEngine) {
        telemetryJob?.cancel()
        stopEngines(closeTun = true)
        totalDownloadBytes = 0
        totalUploadBytes = 0

        val database = AppDatabase.getDatabase(applicationContext)
        val server = database.serverDao().getSelectedServerNow()
        if (server == null) {
            fail("No server is selected. Add and select a real server first.")
            return
        }
        val settings = database.appSettingsDao().getSettings().first()
        val preferred = if (requestedCore == CoreEngine.AUTO) {
            CoreEngine.fromPersisted(settings?.preferredCore)
        } else {
            requestedCore
        }
        val candidates = CoreResolver.candidates(preferred, server.protocol, server.transport)

        VpnRuntime.set(
            VpnRuntimeState(
                phase = ConnectionPhase.STARTING,
                serverName = server.alias
            )
        )
        notifyState("Starting ${candidates.first().displayName}…")

        try {
            if (prepare(this) != null) error("VPN permission was not granted.")
            vpnInterface = establishTunnel(server)
                ?: error("Android could not create the VPN interface.")
            initializeXray()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            fail(readableError(error))
            return
        }

        var lastError = "The selected server could not be reached."
        for (candidate in candidates) {
            currentCoroutineContext().ensureActive()
            VpnRuntime.update {
                it.copy(
                    phase = ConnectionPhase.STARTING,
                    activeCore = candidate,
                    error = null
                )
            }
            notifyState("Starting ${candidate.displayName}…")

            try {
                startCandidate(candidate, server, settings)
                currentCoroutineContext().ensureActive()

                VpnRuntime.update { it.copy(phase = ConnectionPhase.VERIFYING) }
                notifyState("Verifying ${candidate.displayName} connection…")
                val ping = verifyConnection()
                currentCoroutineContext().ensureActive()

                connectedAtElapsedMs = SystemClock.elapsedRealtime()
                VpnRuntime.set(
                    VpnRuntimeState(
                        phase = ConnectionPhase.CONNECTED,
                        activeCore = candidate,
                        serverName = server.alias,
                        pingMs = ping
                    )
                )
                notifyState("Connected • ${candidate.displayName} • ${ping}ms")
                startTelemetry(candidate)
                return
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                lastError = "${candidate.displayName}: ${readableError(error)}"
                Log.w(TAG, "Core candidate failed: $candidate", error)
                stopEngines(closeTun = false)
            }
        }

        fail(lastError)
    }

    private fun establishTunnel(server: ServerEntity): ParcelFileDescriptor? {
        return Builder()
            .setSession("SHADOW_NET • ${server.alias}")
            .setMtu(MTU)
            .addAddress("26.26.26.1", 30)
            .addAddress("fdfe:dcba:9876::1", 126)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("2606:4700:4700::1111")
            .addDisallowedApplication(packageName)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setMetered(false)
            }
            .establish()
    }

    private fun initializeXray() {
        Seq.setContext(applicationContext)
        Libv2ray.initCoreEnv(filesDir.absolutePath, xudpBaseKey())
    }

    /**
     * Returns a persistent XUDP base key, generating and storing one on first
     * use. The key must survive restarts so already-open connections on the
     * server side keep decrypting correctly.
     */
    private fun xudpBaseKey(): String {
        val prefs = getSharedPreferences(CORE_PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(XUDP_BASE_KEY_PREF, null)
        if (existing != null && XudpBaseKey.isValid(existing)) return existing
        val fresh = XudpBaseKey.generate()
        prefs.edit().putString(XUDP_BASE_KEY_PREF, fresh).apply()
        return fresh
    }

    private fun startCandidate(
        candidate: CoreEngine,
        server: ServerEntity,
        settings: AppSettingsEntity?
    ) {
        VpnConfigBuilder.validate(server, candidate)?.let { error(it) }
        val tunFd = vpnInterface?.fd ?: error("VPN interface is closed.")
        val utlsProfile = settings?.utlsProfile ?: "Chrome"

        val xrayConfig = when (candidate) {
            CoreEngine.XRAY -> VpnConfigBuilder.buildXray(
                server = server,
                utlsProfile = utlsProfile,
                mtu = MTU,
                fragmentEnabled = settings?.fragmentEnabled == true,
                fragmentLength = settings?.fragmentLength ?: "50-100",
                fragmentIntervalMs = settings?.fragmentIntervalMs ?: 10
            )
            CoreEngine.SING_BOX -> {
                val singConfig = VpnConfigBuilder.buildSingBox(server, utlsProfile)
                singBoxProcess?.start(singConfig)?.getOrThrow()
                    ?: error("sing-box process manager is unavailable.")
                VpnConfigBuilder.buildXrayToSingBoxBridge(MTU)
            }
            CoreEngine.AUTO -> error("AUTO is not a runtime core.")
        }

        val controller = Libv2ray.newCoreController(this)
        coreController = controller
        try {
            controller.startLoop(xrayConfig, tunFd)
            if (!controller.isRunning) error("Xray core failed to enter the running state.")
        } catch (error: Throwable) {
            runCatching { controller.stopLoop() }
            coreController = null
            throw error
        }
    }

    private fun verifyConnection(): Int {
        val controller = coreController ?: error("VPN core is not running.")
        val delay = controller.measureDelay("https://cp.cloudflare.com/generate_204")
        if (delay < 0) error("Secure route verification failed. Check the server settings and network.")
        return delay.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun startTelemetry(activeCore: CoreEngine) {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch {
            while (isActive && !userStopping) {
                delay(1000)
                val controller = coreController ?: break
                if (!controller.isRunning || (activeCore == CoreEngine.SING_BOX && singBoxProcess?.isAlive != true)) {
                    fail("${activeCore.displayName} stopped unexpectedly.")
                    return@launch
                }

                val (download, upload) = parseTrafficStats(controller.queryAllOutboundTrafficStats())
                totalDownloadBytes += download
                totalUploadBytes += upload
                val elapsed = ((SystemClock.elapsedRealtime() - connectedAtElapsedMs) / 1000).coerceAtLeast(0)
                VpnRuntime.update {
                    it.copy(
                        phase = ConnectionPhase.CONNECTED,
                        elapsedSeconds = elapsed,
                        downloadBytesPerSecond = download,
                        uploadBytesPerSecond = upload,
                        totalDownloadBytes = totalDownloadBytes,
                        totalUploadBytes = totalUploadBytes
                    )
                }
                if (elapsed % 5L == 0L) {
                    notifyState(
                        "${activeCore.displayName} • ↓ ${formatRate(download)} • ↑ ${formatRate(upload)}"
                    )
                }
            }
        }
    }

    private fun parseTrafficStats(raw: String?): Pair<Long, Long> {
        var download = 0L
        var upload = 0L
        raw.orEmpty().split(';').forEach { record ->
            val parts = record.split(',')
            if (parts.size != 3 || parts[0] != "proxy") return@forEach
            val value = parts[2].toLongOrNull()?.coerceAtLeast(0) ?: return@forEach
            when (parts[1].lowercase()) {
                "downlink" -> download += value
                "uplink" -> upload += value
            }
        }
        return download to upload
    }

    private fun requestStop(keepError: Boolean = false) {
        userStopping = true
        startJob?.cancel()
        telemetryJob?.cancel()
        if (!keepError) {
            VpnRuntime.update { it.copy(phase = ConnectionPhase.STOPPING, error = null) }
        }
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        serviceScope.launch {
            operationMutex.withLock {
                stopEngines(closeTun = true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (!keepError) VpnRuntime.set(VpnRuntimeState())
                stopSelf()
            }
        }
    }

    private suspend fun stopEngines(closeTun: Boolean) {
        telemetryJob?.cancel()
        telemetryJob = null
        val controller = coreController
        coreController = null
        if (controller != null) {
            runCatching { controller.stopLoop() }
                .onFailure { Log.w(TAG, "Failed to stop Xray", it) }
        }
        runCatching { singBoxProcess?.stop() }
        if (closeTun) {
            runCatching { vpnInterface?.close() }
            vpnInterface = null
        }
    }

    private fun fail(message: String) {
        val cleanMessage = message.ifBlank { "VPN connection failed." }.take(320)
        Log.e(TAG, cleanMessage)
        VpnRuntime.set(
            VpnRuntimeState(
                phase = ConnectionPhase.ERROR,
                activeCore = VpnRuntime.state.value.activeCore,
                serverName = VpnRuntime.state.value.serverName,
                error = cleanMessage
            )
        )
        notifyState("Connection failed")
        serviceScope.launch {
            operationMutex.withLock {
                stopEngines(closeTun = true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun readableError(error: Throwable): String {
        val raw = error.message?.lineSequence()?.firstOrNull()?.trim()
        return raw?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
    }

    private fun formatRate(bytes: Long): String = when {
        bytes >= 1_000_000 -> String.format("%.1f MB/s", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB/s", bytes / 1_000.0)
        else -> "$bytes B/s"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "VPN connection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows the active SHADOW_NET VPN connection"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyState(text: String) {
        if (destroyed) return
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ShadowVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vpn_status)
            .setContentTitle("SHADOW_NET")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_pause, "Disconnect", stopIntent)
            .build()
    }

    override fun startup(): Long {
        Log.i(TAG, "Xray core started")
        return 0
    }

    override fun shutdown(): Long {
        Log.i(TAG, "Xray core stopped")
        return 0
    }

    override fun onEmitStatus(code: Long, message: String?): Long {
        if (!message.isNullOrBlank()) Log.i(TAG, "Core status $code: $message")
        return 0
    }
}
