package com.example.vpn

import android.content.Context
import android.util.Log
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

internal class SingBoxProcess(private val context: Context) {
    companion object {
        private const val TAG = "ShadowNet/SingBox"
        private const val LOCAL_HOST = "127.0.0.1"
        private const val LOCAL_PORT = 10808
    }

    private var process: Process? = null
    private var logThread: Thread? = null

    val isAlive: Boolean
        get() = process?.let(::processIsAlive) == true

    fun version(): String? {
        val binary = binary() ?: return null
        return runCatching {
            val process = ProcessBuilder(binary.absolutePath, "version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            Regex("sing-box version\\s+(\\S+)", RegexOption.IGNORE_CASE)
                .find(output)?.groupValues?.getOrNull(1)
        }.getOrNull()
    }

    fun start(config: String): Result<Unit> = runCatching {
        stop()
        val binary = binary() ?: error(
            "sing-box core is missing for this device ABI. Install an APK that includes the matching core."
        )
        val workDir = File(context.filesDir, "sing-box").apply { mkdirs() }
        val configFile = File(workDir, "config.json").apply { writeText(config) }

        checkConfig(binary, configFile, workDir)

        val started = ProcessBuilder(
            binary.absolutePath,
            "run",
            "-c",
            configFile.absolutePath,
            "-D",
            workDir.absolutePath
        )
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        process = started
        logThread = Thread({
            runCatching {
                started.inputStream.bufferedReader().forEachLine { line ->
                    Log.i(TAG, line)
                }
            }
        }, "sing-box-log").apply {
            isDaemon = true
            start()
        }

        if (!waitForProxy()) {
            val exit = if (processIsAlive(started)) "did not open its local proxy" else "exited with code ${started.exitValue()}"
            stop()
            error("sing-box $exit.")
        }
    }

    fun stop() {
        val current = process
        process = null
        if (current != null) {
            runCatching {
                current.destroy()
                if (!waitForExit(current, 1500)) {
                    // Process.destroy() is available on every supported API.
                    // A second signal is preferable to calling destroyForcibly(),
                    // which does not exist on Android 7 (API 24/25).
                    current.destroy()
                    waitForExit(current, 500)
                }
            }.onFailure { Log.w(TAG, "Failed to stop sing-box", it) }
        }
        logThread?.interrupt()
        logThread = null
    }

    private fun checkConfig(binary: File, config: File, workDir: File) {
        val check = ProcessBuilder(
            binary.absolutePath,
            "check",
            "-c",
            config.absolutePath,
            "-D",
            workDir.absolutePath
        )
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        val output = check.inputStream.bufferedReader().readText().trim()
        check.waitFor()
        if (check.exitValue() != 0) {
            error(output.ifBlank { "sing-box rejected the server configuration." })
        }
    }

    private fun waitForProxy(timeoutMs: Long = 5000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!isAlive) return false
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(LOCAL_HOST, LOCAL_PORT), 250)
                    return true
                }
            } catch (_: Exception) {
                Thread.sleep(100)
            }
        }
        return false
    }

    private fun processIsAlive(target: Process): Boolean = try {
        target.exitValue()
        false
    } catch (_: IllegalThreadStateException) {
        true
    }

    private fun waitForExit(target: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (processIsAlive(target) && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }
        return !processIsAlive(target)
    }

    private fun binary(): File? {
        val candidate = File(context.applicationInfo.nativeLibraryDir, "libsingbox.so")
        return candidate.takeIf { it.isFile && it.canExecute() }
    }
}
