package com.headphonealarm.keepalive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPolicyTest {
    @Test
    fun onlyRestartsAnInstalledAndRunningPackageForTheObservedUser() {
        val dump = """
            Packages:
              Package [com.headphonealarm]:
                appId=10142
                User 0: installed=true hidden=false suspended=false stopped=true enabled=0
                User 10: installed=true hidden=false suspended=false stopped=false enabled=0
        """.trimIndent()

        assertFalse(RecoveryPolicy.packageCanRestart(dump, 10142))
        assertTrue(RecoveryPolicy.packageCanRestart(dump, 1010142))
        assertFalse(RecoveryPolicy.packageCanRestart(dump, 1010143))
        assertFalse(RecoveryPolicy.packageCanRestart(dump.replace("suspended=false", "suspended=true"), 1010142))
        assertFalse(RecoveryPolicy.packageCanRestart(dump.replace("enabled=0", "enabled=3"), 1010142))
        assertFalse(RecoveryPolicy.packageCanRestart("", 10142))
    }

    @Test
    fun distinguishesManualStopFromMemoryReclaimAndRecentTaskRemoval() {
        val manualStop = """
              ApplicationExitInfo #0:
                timestamp=2026-11-22 12:00:00.000 pid=1234 realUid=10142 packageUid=10142 definingUid=10142 user=0
                process=com.headphonealarm reason=10 (USER REQUESTED) subreason=23 (STOP APP) status=0
        """.trimIndent()

        assertTrue(RecoveryPolicy.userStoppedProcess(manualStop, 10142, 1234))
        assertFalse(RecoveryPolicy.userStoppedProcess(manualStop, 10142, 123))
        assertFalse(RecoveryPolicy.userStoppedProcess(manualStop, 10143, 1234))
        assertFalse(RecoveryPolicy.userStoppedProcess(manualStop.replace("reason=10", "reason=3"), 10142, 1234))
        assertFalse(RecoveryPolicy.userStoppedProcess(manualStop.replace("subreason=23", "subreason=22"), 10142, 1234))
    }
}
