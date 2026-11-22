package com.headphonealarm.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.keepalive.ShizukuKeepAlive
import com.headphonealarm.keepalive.ShizukuStatus
import com.headphonealarm.ui.components.GlassCard
import com.headphonealarm.ui.theme.LocalInterfaceStyle
import com.headphonealarm.util.AlarmPermissions
import top.yukonga.miuix.kmp.extra.SuperSwitch

@Composable
fun AppSettingsScreen(
    appearance: AppearanceSettings,
    keepAlive: ShizukuKeepAlive,
    onAppearance: () -> Unit,
    onBack: () -> Unit
) {
    SettingsPage(title = "设置", onBack = onBack) {
        item {
            GlassCard {
                SettingsLink(
                    title = "外观与主题",
                    summary = appearance.interfaceStyle.label + " · " +
                        (appearance.legacyTheme?.label ?: appearance.colorMode.label),
                    icon = Icons.Outlined.Palette,
                    onClick = onAppearance
                )
            }
        }
        item { KeepAliveSettings(keepAlive) }
    }
}

@Composable
private fun KeepAliveSettings(keepAlive: ShizukuKeepAlive) {
    val context = LocalContext.current
    val state by keepAlive.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { keepAlive.refresh() }

    fun openShizuku() {
        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            ?: Intent(Intent.ACTION_VIEW, "https://shizuku.rikka.app/download/".toUri())
        AlarmPermissions.openSettingsSafely(context, intent)
    }

    val shizukuStatus = when (state.status) {
        ShizukuStatus.NOT_RUNNING -> "Shizuku 未运行"
        ShizukuStatus.OUTDATED -> "请更新至 Shizuku v12 或更高版本"
        ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku 等待授权"
        ShizukuStatus.PERMISSION_DENIED -> "请在 Shizuku 的已授权应用中允许耳机闹钟"
        ShizukuStatus.READY -> "Shizuku 已授权"
    }
    val canChange = state.status == ShizukuStatus.READY && !state.changing
    val exemptionStatus = when {
        state.changing -> "正在应用…"
        state.exempt -> "已豁免"
        else -> "未豁免"
    }

    SettingsGroup("后台保活") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(shizukuStatus, style = MaterialTheme.typography.titleMedium)
            Text(
                "豁免电池优化可减少系统省电对闹钟后台唤醒的影响。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
                SuperSwitch(
                    checked = state.exempt,
                    onCheckedChange = keepAlive::setExempt,
                    title = "电池优化豁免",
                    summary = exemptionStatus,
                    insideMargin = PaddingValues(vertical = 10.dp),
                    enabled = canChange
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .toggleable(state.exempt, enabled = canChange, role = Role.Switch, onValueChange = keepAlive::setExempt)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("电池优化豁免", style = MaterialTheme.typography.bodyLarge)
                        Text(exemptionStatus, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.exempt, onCheckedChange = null, enabled = canChange)
                }
            }
            if (state.failed) {
                Text(
                    "系统未应用此次更改，请重试或在系统设置中调整。",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            when (state.status) {
                ShizukuStatus.PERMISSION_REQUIRED -> {
                    Button(onClick = keepAlive::requestPermission) { Text("授权 Shizuku") }
                }
                ShizukuStatus.READY -> Unit
                else -> Button(onClick = ::openShizuku) { Text("打开 Shizuku") }
            }
            TextButton(
                onClick = {
                    val intent = if (state.exempt) {
                        AlarmPermissions.batteryOptimizationListIntent()
                    } else {
                        AlarmPermissions.batteryOptimizationIntent(context)
                    }
                    AlarmPermissions.openSettingsSafely(context, intent)
                }
            ) { Text("系统电池设置") }
        }
    }
}
