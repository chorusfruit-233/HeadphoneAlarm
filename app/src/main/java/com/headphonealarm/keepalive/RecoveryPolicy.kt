package com.headphonealarm.keepalive

/** Android assigns one UID range to each user, with the same app ID in each range. */
internal const val UID_RANGE_PER_USER = 100_000

internal object RecoveryPolicy {
    fun packageCanRestart(dump: String, uid: Int): Boolean {
        val appId = uid % UID_RANGE_PER_USER
        val user = uid / UID_RANGE_PER_USER
        val lines = dump.lineSequence().map(String::trim)
        if (lines.none { it == "appId=$appId" || it == "userId=$appId" }) return false
        val flags = lines.firstOrNull { it.startsWith("User $user:") }
            ?.split(' ')?.toSet() ?: return false
        return "installed=true" in flags && "stopped=false" in flags &&
            "hidden=true" !in flags && "suspended=true" !in flags &&
            ("enabled=0" in flags || "enabled=1" in flags)
    }

    fun userStoppedProcess(dump: String, uid: Int, pid: Int): Boolean {
        val reasonLine = dump.lineSequence().zipWithNext().firstOrNull { (line, _) ->
            " pid=$pid " in line && " packageUid=$uid " in line
        }?.second ?: return false
        // Android's foreground-service stop and force-stop use USER_REQUESTED.
        // Removing the recent-tasks card is reported separately as REMOVE_TASK.
        return " reason=10 " in reasonLine && " subreason=22 " !in reasonLine
    }
}
