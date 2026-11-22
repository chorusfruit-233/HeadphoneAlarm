package com.headphonealarm.keepalive

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.os.IBinder
import android.util.Log
import com.headphonealarm.util.AlarmPermissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

enum class ShizukuStatus { NOT_RUNNING, OUTDATED, PERMISSION_REQUIRED, PERMISSION_DENIED, READY }

data class KeepAliveState(
    val status: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val exempt: Boolean = false,
    val changing: Boolean = false,
    val failed: Boolean = false
)

class ShizukuKeepAlive(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(KeepAliveState())
    val state = mutableState.asStateFlow()

    init {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener { refresh() }
        Shizuku.addRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == REQUEST_PERMISSION) refresh()
        }
        refresh()
    }

    fun refresh() {
        val status = try {
            when {
                !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
                Shizuku.isPreV11() || Shizuku.getVersion() < 12 -> ShizukuStatus.OUTDATED
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.READY
                Shizuku.shouldShowRequestPermissionRationale() -> ShizukuStatus.PERMISSION_DENIED
                else -> ShizukuStatus.PERMISSION_REQUIRED
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not read Shizuku status", error)
            ShizukuStatus.NOT_RUNNING
        }
        val exempt = AlarmPermissions.isIgnoringBatteryOptimizations(context)
        mutableState.update { it.copy(status = status, exempt = exempt) }
    }

    fun requestPermission() {
        refresh()
        if (state.value.status != ShizukuStatus.PERMISSION_REQUIRED) return
        try {
            Shizuku.requestPermission(REQUEST_PERMISSION)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not request Shizuku permission", error)
            refresh()
        }
    }

    fun setExempt(enabled: Boolean) {
        refresh()
        val current = state.value
        if (current.status != ShizukuStatus.READY || current.changing || current.exempt == enabled) return
        mutableState.update { it.copy(changing = true, failed = false) }
        scope.launch {
            try {
                if (!changeExemption(enabled)) Log.w(TAG, "Shizuku could not change the exemption")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Could not change the battery optimization exemption", error)
            } finally {
                refresh()
                mutableState.update { it.copy(changing = false, failed = it.exempt != enabled) }
            }
        }
    }

    private suspend fun changeExemption(enabled: Boolean): Boolean {
        val args = Shizuku.UserServiceArgs(ComponentName(context, BatteryExemptionService::class.java))
            .processNameSuffix("battery_exemption")
            .version(1)
            .daemon(false)
        val service = CompletableDeferred<IBatteryExemptionService>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                service.complete(IBatteryExemptionService.Stub.asInterface(binder))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service.completeExceptionally(DeadObjectException())
            }
        }

        try {
            Shizuku.bindUserService(args, connection)
            val remote = withTimeoutOrNull(10_000) { service.await() } ?: return false
            return withContext(Dispatchers.IO) { remote.setExempt(enabled) }
        } finally {
            runCatching { Shizuku.unbindUserService(args, null, true) }
                .onFailure { Log.w(TAG, "Could not stop the Shizuku UserService", it) }
            // The remove=true call leaves the SDK's cached connection until Binder death arrives.
            runCatching { Shizuku.unbindUserService(args, connection, false) }
                .onFailure { Log.w(TAG, "Could not detach the Shizuku UserService", it) }
        }
    }

    private companion object {
        const val TAG = "ShizukuKeepAlive"
        const val REQUEST_PERMISSION = 100
    }
}
