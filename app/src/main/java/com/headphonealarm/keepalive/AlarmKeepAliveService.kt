package com.headphonealarm.keepalive

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.headphonealarm.AlarmApp
import com.headphonealarm.R
import com.headphonealarm.alarmApp
import com.headphonealarm.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

class AlarmKeepAliveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lifetime = Binder()
    private var monitoring: Job? = null
    private var stoppedStatus = WatchdogStatus.IDLE

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_DISABLE -> alarmApp.shizukuKeepAlive.setKeepAliveEnabled(false)
            ACTION_START, ACTION_RECOVER -> if (monitoring == null) {
                beginMonitoring(recovery = action == ACTION_RECOVER)
            }
            else -> if (monitoring == null) stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun beginMonitoring(recovery: Boolean) {
        val keepAlive = alarmApp.shizukuKeepAlive
        keepAlive.updateWatchdog(WatchdogStatus.STARTING)
        try {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(active = false), serviceType)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Android rejected the foreground notification", error)
            stoppedStatus = WatchdogStatus.FAILED
            stopSelf()
            return
        }

        monitoring = scope.launch {
            try {
                if (!alarmApp.settingsRepository.keepAliveEnabled.first()) return@launch
                val shizukuReady = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                    keepAlive.state.first { it.status == ShizukuStatus.READY }
                }
                if (shizukuReady == null) {
                    stoppedStatus = WatchdogStatus.FAILED
                    return@launch
                }
                watchWithShizuku(recovery)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Could not monitor the app with Shizuku", error)
                stoppedStatus = WatchdogStatus.FAILED
            } finally {
                stopSelf()
            }
        }
    }

    private suspend fun watchWithShizuku(recovery: Boolean) {
        val user = Process.myUid() / UID_RANGE_PER_USER
        val args = Shizuku.UserServiceArgs(ComponentName(this, AlarmWatchdog::class.java))
            .processNameSuffix("alarm_watchdog")
            .tag("alarm-watchdog-$user")
            .version(1)
            .daemon(true)
        val connected = CompletableDeferred<IAlarmWatchdog>()
        val disconnected = CompletableDeferred<Unit>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                connected.complete(IAlarmWatchdog.Stub.asInterface(binder))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                connected.cancel()
                disconnected.complete(Unit)
            }
        }

        try {
            Shizuku.bindUserService(args, connection)
            val watchdog = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) { connected.await() }
                ?: error("Timed out connecting to the Shizuku UserService")
            val backend = Shizuku.getBinder() ?: error("Shizuku is no longer running")
            withContext(Dispatchers.IO) { watchdog.observe(lifetime, backend) }
            alarmApp.shizukuKeepAlive.updateWatchdog(WatchdogStatus.ACTIVE)
            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification(active = true))
            if (recovery) {
                try {
                    withContext(Dispatchers.IO) { alarmApp.alarmOperations.rescheduleAll() }
                    Log.i(TAG, "Restored alarm schedules after recovery")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "Could not restore alarm schedules after recovery", error)
                }
            }
            disconnected.await()
            stoppedStatus = if (Shizuku.pingBinder()) WatchdogStatus.FAILED else WatchdogStatus.IDLE
        } finally {
            runCatching { Shizuku.unbindUserService(args, null, true) }
                .onFailure { Log.d(TAG, "Shizuku UserService was already unavailable", it) }
            runCatching { Shizuku.unbindUserService(args, connection, false) }
                .onFailure { Log.d(TAG, "Shizuku connection was already unavailable", it) }
        }
    }

    private fun notification(active: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this, NOTIFICATION_ID, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val disable = PendingIntent.getService(
            this, NOTIFICATION_ID, Intent(this, AlarmKeepAliveService::class.java).setAction(ACTION_DISABLE),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, AlarmApp.CHANNEL_KEEP_ALIVE)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(getString(R.string.channel_keep_alive_name))
            .setContentText(getString(if (active) R.string.keep_alive_active else R.string.keep_alive_connecting))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.keep_alive_disable), disable)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        alarmApp.shizukuKeepAlive.updateWatchdog(stoppedStatus)
        super.onDestroy()
    }

    companion object {
        internal const val ACTION_RECOVER = "com.headphonealarm.action.RECOVER_KEEP_ALIVE"
        private const val ACTION_START = "com.headphonealarm.action.START_KEEP_ALIVE"
        private const val ACTION_DISABLE = "com.headphonealarm.action.DISABLE_KEEP_ALIVE"
        private const val TAG = "AlarmKeepAliveService"
        private const val NOTIFICATION_ID = 712
        private const val CONNECTION_TIMEOUT_MS = 25_000L

        internal fun start(context: Context) {
            val intent = Intent(context, AlarmKeepAliveService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
