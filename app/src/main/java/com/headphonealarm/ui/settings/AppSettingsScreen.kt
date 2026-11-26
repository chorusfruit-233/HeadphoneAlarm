package com.headphonealarm.ui.settings

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.ColorMode
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.data.LegacyTheme
import com.headphonealarm.keepalive.KeepAliveState
import com.headphonealarm.keepalive.ShizukuKeepAlive
import com.headphonealarm.keepalive.ShizukuStatus
import com.headphonealarm.keepalive.WatchdogStatus
import com.headphonealarm.util.AlarmPermissions

@Composable
fun AppSettingsScreen(
    appearance: AppearanceSettings,
    update: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
    keepAlive: ShizukuKeepAlive,
    onPalette: () -> Unit,
    onKeepAlive: () -> Unit
) {
    val state by keepAlive.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { keepAlive.refresh() }
    SettingsPage(title = "设置") {
        if (appearance.interfaceStyle == InterfaceStyle.MIUIX) {
            item {
                SettingsGroup(rows = listOf({
                    val needsShizuku = !state.keepAliveEnabled && state.status != ShizukuStatus.READY
                    SettingsToggle(
                        title = "强制保活",
                        summary = if (needsShizuku) "点按以配置 Shizuku 守护服务" else state.watchdogSummary,
                        icon = Icons.Rounded.Shield,
                        checked = state.keepAliveEnabled,
                        enabled = !state.savingKeepAlive,
                        onChange = { enabled -> if (enabled && needsShizuku) onKeepAlive() else keepAlive.setKeepAliveEnabled(enabled) }
                    )
                }))
            }
        }
        item {
            SettingsGroup(rows = listOf(
                {
                    val styles = listOf(InterfaceStyle.MIUIX, InterfaceStyle.MATERIAL)
                    SettingsDropdown(
                        title = "界面风格",
                        summary = "选择应用的界面风格",
                        icon = Icons.Rounded.DisplaySettings,
                        choices = listOf("Miuix", "Material"),
                        selected = styles.indexOf(appearance.interfaceStyle),
                        onSelect = { index ->
                            update { old ->
                                val style = styles[index]
                                val mode = if (style == InterfaceStyle.MIUIX && old.colorMode == ColorMode.AMOLED) {
                                    ColorMode.DARK
                                } else old.colorMode
                                old.copy(interfaceStyle = style, colorMode = mode)
                            }
                        }
                    )
                },
                {
                    SettingsLink(
                        title = "主题设置",
                        summary = "自定义更多主题选项",
                        icon = Icons.Rounded.Palette,
                        onClick = onPalette
                    )
                }
            ))
        }
        item {
            SettingsGroup(rows = listOf(
                {
                    SettingsLink(
                        title = "后台保活", icon = Icons.Rounded.Security, onClick = onKeepAlive
                    )
                },
                {
                    SettingsDropdown(
                        title = "经典主题",
                        icon = Icons.Rounded.Wallpaper,
                        choices = listOf("使用主题设置") + LegacyTheme.entries.map { it.label },
                        selected = appearance.legacyTheme?.let { it.ordinal + 1 } ?: 0,
                        onSelect = { index ->
                            update { it.copy(legacyTheme = LegacyTheme.entries.getOrNull(index - 1)) }
                        }
                    )
                }
            ))
        }
    }
}

private val KeepAliveState.watchdogSummary: String
    get() = when {
        keepAliveSaveFailed -> "无法读取或保存保活设置，请重试"
        savingKeepAlive -> "正在保存…"
        !keepAliveEnabled -> "系统回收后自动恢复闹钟"
        status != ShizukuStatus.READY -> "等待 Shizuku 连接"
        watchdog == WatchdogStatus.ACTIVE -> "守护运行中"
        watchdog == WatchdogStatus.STARTING -> "正在启动守护…"
        else -> "守护未运行"
    }

@Composable
fun KeepAliveSettingsScreen(keepAlive: ShizukuKeepAlive, onBack: () -> Unit) {
    SettingsPage(title = "后台保活", onBack = onBack) {
        item { KeepAlivePreferences(keepAlive) }
    }
}

@Composable
private fun KeepAlivePreferences(keepAlive: ShizukuKeepAlive) {
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
        ShizukuStatus.PERMISSION_REQUIRED -> "点按以授权耳机闹钟"
        ShizukuStatus.PERMISSION_DENIED -> "请在 Shizuku 的已授权应用中允许耳机闹钟"
        ShizukuStatus.READY -> "Shizuku 已授权"
    }
    val exemptionStatus = when {
        state.failed -> "系统未应用此次更改，可在系统电池设置中调整"
        state.changing -> "正在应用…"
        state.exempt -> "已豁免"
        else -> "未豁免"
    }
    SettingsGroup(rows = buildList {
        add {
            SettingsLink(
                title = "Shizuku",
                summary = shizukuStatus,
                icon = Icons.Rounded.Security,
                onClick = if (state.status == ShizukuStatus.PERMISSION_REQUIRED) keepAlive::requestPermission else ::openShizuku
            )
        }
        add {
            SettingsToggle(
                title = "强制保活",
                summary = state.watchdogSummary,
                icon = Icons.Rounded.Shield,
                checked = state.keepAliveEnabled,
                enabled = !state.savingKeepAlive && (state.keepAliveEnabled || state.status == ShizukuStatus.READY),
                onChange = keepAlive::setKeepAliveEnabled
            )
        }
        if (state.keepAliveEnabled && state.status == ShizukuStatus.READY &&
            state.watchdog in listOf(WatchdogStatus.IDLE, WatchdogStatus.FAILED)
        ) {
            add { SettingsLink("重试守护", icon = Icons.Rounded.RestartAlt, onClick = keepAlive::startWatchdogIfRequested) }
        }
        add {
            SettingsToggle(
                title = "电池优化豁免",
                summary = exemptionStatus,
                icon = Icons.Rounded.BatteryChargingFull,
                checked = state.exempt,
                enabled = state.status == ShizukuStatus.READY && !state.changing,
                onChange = keepAlive::setExempt
            )
        }
        add {
            SettingsLink(
                title = "系统电池设置",
                icon = Icons.Rounded.BatterySaver,
                onClick = {
                    val intent = if (state.exempt) AlarmPermissions.batteryOptimizationListIntent()
                        else AlarmPermissions.batteryOptimizationIntent(context)
                    AlarmPermissions.openSettingsSafely(context, intent)
                }
            )
        }
    })
}
