package com.headphonealarm.keepalive

import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import java.util.ArrayDeque
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** A Shizuku UserService that waits for Binder death without polling the app process. */
class AlarmWatchdog : IAlarmWatchdog.Stub() {
    private val lock = Any()
    private val worker = ScheduledThreadPoolExecutor(1)
    private val attempts = ArrayDeque<Long>()
    private var backend: IBinder? = null
    private var registration: Registration? = null

    init {
        worker.schedule({
            synchronized(lock) {
                if (registration == null) destroy()
            }
        }, ATTACH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private inner class Registration(val uid: Int, val pid: Int, val binder: IBinder) : IBinder.DeathRecipient {
        override fun binderDied() {
            worker.schedule({ recover(this, retry = false) }, SETTLE_SECONDS, TimeUnit.SECONDS)
        }
    }

    override fun observe(appLifetime: IBinder, shizukuBackend: IBinder) {
        val caller = Registration(Binder.getCallingUid(), Binder.getCallingPid(), appLifetime)
        synchronized(lock) {
            check(registration?.uid.let { it == null || it == caller.uid })
            val currentBackend = backend
            if (currentBackend == null) {
                shizukuBackend.linkToDeath({ destroy() }, 0)
                backend = shizukuBackend
            } else {
                check(currentBackend == shizukuBackend && currentBackend.isBinderAlive)
            }
            appLifetime.linkToDeath(caller, 0)
            val previous = registration
            registration = caller
            previous?.let { runCatching { it.binder.unlinkToDeath(it, 0) } }
        }
        Log.i(TAG, "Monitoring app process ${caller.pid}")
    }

    private fun recover(dead: Registration, retry: Boolean) {
        if (!isCurrent(dead)) return
        try {
            val pkg = command("/system/bin/dumpsys", "-t", "5", "package", PACKAGE)
            if (pkg == null || !RecoveryPolicy.packageCanRestart(pkg, dead.uid)) {
                stopIfCurrent(dead, "Package unavailable or stopped")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val exits = command("/system/bin/dumpsys", "-t", "5", "activity", "exit-info", PACKAGE)
                if (exits == null || "dumpsys activity exit-info" !in exits ||
                    RecoveryPolicy.userStoppedProcess(exits, dead.uid, dead.pid)
                ) {
                    stopIfCurrent(dead, "Process was stopped by the user or exit information was unavailable")
                    return
                }
            }
            if (!isCurrent(dead)) return
            val now = SystemClock.elapsedRealtime()
            while (attempts.isNotEmpty() && now - attempts.first > RESTART_WINDOW_MS) attempts.removeFirst()
            if (attempts.size >= MAX_RESTARTS) {
                stopIfCurrent(dead, "Too many restarts")
                return
            }
            attempts.addLast(now)
            Log.i(TAG, "Recovering app after process ${dead.pid} exited")
            // Resolve a package-scoped intent so Android also applies its stopped-package filter.
            val result = command(
                "/system/bin/cmd", "activity", "start-foreground-service",
                "--user", (dead.uid / UID_RANGE_PER_USER).toString(),
                "-a", AlarmKeepAliveService.ACTION_RECOVER,
                "-p", PACKAGE, "--exclude-stopped-packages"
            )
            if (result == null || result.lineSequence().any { it.startsWith("Error:") }) {
                if (retry) stopIfCurrent(dead, "Android rejected the service start")
                else worker.schedule({ recover(dead, retry = true) }, SETTLE_SECONDS, TimeUnit.SECONDS)
            } else {
                worker.schedule({
                    stopIfCurrent(dead, "The restarted service did not attach")
                }, ATTACH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not recover the foreground service", error)
            stopIfCurrent(dead, "Recovery failed")
        }
    }

    private fun isCurrent(dead: Registration): Boolean = synchronized(lock) {
        registration === dead && backend?.isBinderAlive == true
    }

    private fun stopIfCurrent(dead: Registration, message: String) {
        synchronized(lock) {
            if (registration === dead) {
                Log.i(TAG, message)
                destroy()
            }
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun command(vararg arguments: String): String? {
        val process = ProcessBuilder(*arguments).redirectErrorStream(true).start()
        val output = FutureTask { process.inputStream.bufferedReader().use { it.readText() } }
        Thread(output, "watchdog-command-output").apply { isDaemon = true }.start()
        return try {
            if (process.waitFor(8, TimeUnit.SECONDS) && process.exitValue() == 0) {
                output.get(1, TimeUnit.SECONDS)
            } else {
                null
            }
        } finally {
            if (process.isAlive) process.destroyForcibly()
            process.inputStream.close()
            process.outputStream.close()
            process.errorStream.close()
        }
    }

    private companion object {
        const val TAG = "AlarmWatchdog"
        const val PACKAGE = "com.headphonealarm"
        const val SETTLE_SECONDS = 2L
        const val ATTACH_TIMEOUT_SECONDS = 60L
        const val RESTART_WINDOW_MS = 120_000L
        const val MAX_RESTARTS = 3
    }
}
