package com.headphonealarm.keepalive

import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Runs as a short-lived Shizuku UserService. Its only target is HeadphoneAlarm. */
class BatteryExemptionService : IBatteryExemptionService.Stub() {

    override fun setExempt(enabled: Boolean): Boolean {
        val target = if (enabled) "+com.headphonealarm" else "-com.headphonealarm"
        val process = ProcessBuilder("/system/bin/cmd", "deviceidle", "whitelist", target)
            .redirectErrorStream(true)
            .start()
        return try {
            process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0
        } finally {
            if (process.isAlive) process.destroyForcibly()
            process.inputStream.close()
            process.outputStream.close()
            process.errorStream.close()
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
