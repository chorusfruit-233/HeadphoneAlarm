package com.headphonealarm.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headphonealarm.AlarmApp
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.ColorMode
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.ui.theme.paletteKeyColors
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val seedLabels = listOf(
    "默认", "红色", "粉色", "紫色", "深紫", "靛青", "蓝色", "青色",
    "青绿", "绿色", "黄色", "琥珀", "橙色", "棕色", "灰蓝", "樱花"
)

@Composable
fun ColorPaletteScreen(
    appearance: AppearanceSettings,
    update: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
    onBack: () -> Unit
) {
    val miuix = appearance.interfaceStyle == InterfaceStyle.MIUIX
    val changeColors: ((AppearanceSettings) -> AppearanceSettings) -> Unit = { transform ->
        update { settings -> transform(settings).copy(legacyTheme = null) }
    }
    val seeds = listOf(0) + paletteKeyColors
    // Keep colors chosen in older releases selectable when they are outside the current palette.
    val custom = appearance.keyColor !in seeds
    val choices = if (custom) seeds + appearance.keyColor else seeds
    val labels = if (custom) seedLabels + "自定义" else seedLabels
    val setSeed: (Int) -> Unit = { color -> changeColors { it.withKeyColor(color) } }
    val setMode: (ColorMode) -> Unit = { mode -> changeColors { it.copy(colorMode = mode) } }
    val setScale: (Float) -> Unit = { scale -> update { it.copy(pageScale = scale) } }
    val hasBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val hasPredictiveBack = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    val styleRow: @Composable () -> Unit = {
        SettingsDropdown(
            title = "色彩风格", icon = Icons.Rounded.Style, choices = PaletteStyle.entries.map { it.name },
            selected = appearance.paletteStyle.ordinal,
            onSelect = { i -> changeColors { it.copy(paletteStyle = PaletteStyle.entries[i]) } }
        )
    }
    val specRow: @Composable () -> Unit = {
        SettingsDropdown(
            title = "色彩标准", icon = Icons.Rounded.DesignServices,
            choices = ColorSpec.SpecVersion.entries.map { it.name }, selected = appearance.colorSpec.ordinal,
            onSelect = { i -> changeColors { it.copy(colorSpec = ColorSpec.SpecVersion.entries[i]) } }
        )
    }
    val badgeRow: @Composable () -> Unit = {
        val alarms by AlarmApp.from(LocalContext.current).alarmRepository.alarms
            .collectAsStateWithLifecycle(initialValue = emptyList())
        SettingsToggle(
            title = "导航栏角标", summary = "在导航栏显示已启用闹钟数量（当前 ${alarms.count { it.enabled }}）",
            icon = Icons.Rounded.Pin, checked = appearance.enableNavigationBadge,
            onChange = { show -> update { it.copy(enableNavigationBadge = show) } }
        )
    }
    val backRow: @Composable () -> Unit = {
        SettingsToggle(
            title = "预测性返回手势", summary = "启用对预测性返回手势的支持",
            icon = Icons.AutoMirrored.Rounded.MenuOpen, checked = appearance.enablePredictiveBack,
            onChange = { enabled -> update { it.copy(enablePredictiveBack = enabled) } }
        )
    }
    val swipeRow: @Composable () -> Unit = {
        SettingsToggle(
            title = "横移返回手势", summary = "从屏幕边缘横移返回上一页", icon = Icons.Rounded.Swipe,
            checked = appearance.enableSwipeDismiss,
            onChange = { enabled -> update { it.copy(enableSwipeDismiss = enabled) } }
        )
    }
    val pagerRow: @Composable () -> Unit = {
        SettingsDropdown(
            title = "页面切换手势", icon = Icons.Rounded.ViewCarousel,
            choices = listOf("默认", "跨轴拦截", "iOS 风格"), selected = appearance.pagerInterceptionMode,
            onSelect = { index -> update { it.copy(pagerInterceptionMode = index) } }
        )
    }

    SettingsPage(title = "主题设置", onBack = onBack, palette = true) {
        item {
            Column {
                if (miuix) Spacer(Modifier.height(44.dp))
                ColorPalettePreview(appearance)
            }
        }
        if (miuix) {
            item {
                Column {
                    Spacer(Modifier.height(72.dp))
                    MiuixModePicker(appearance.colorMode, setMode)
                }
            }
            item {
                SettingsGroup(modifier = Modifier.padding(top = 12.dp), rows = listOf(
                    {
                        SettingsToggle(
                            title = "启用 Monet 颜色", icon = Icons.Rounded.Wallpaper, checked = appearance.miuixMonet,
                            onChange = { enabled -> changeColors { it.copy(miuixMonet = enabled) } }
                        )
                    },
                    {
                        AnimatedVisibility(appearance.miuixMonet) {
                            Column {
                                SettingsDropdown(
                                    title = "强调色", icon = Icons.Rounded.Colorize,
                                    choices = labels, selected = choices.indexOf(appearance.keyColor),
                                    onSelect = { index -> setSeed(choices[index]) }
                                )
                                AnimatedVisibility(appearance.keyColor != 0) {
                                    Column {
                                        styleRow()
                                        specRow()
                                    }
                                }
                            }
                        }
                    }
                ))
            }
            item {
                val rows = buildList<@Composable () -> Unit> {
                    if (hasBackdrop) add {
                        SettingsToggle(
                            title = "模糊", summary = "启用顶栏和底栏的模糊效果", icon = Icons.Rounded.BlurOn,
                            checked = appearance.enableBlur, onChange = { show -> update { it.copy(enableBlur = show) } }
                        )
                    }
                    add {
                        SettingsToggle(
                            title = "悬浮底栏", summary = "使用 Apple 风格的悬浮底栏", icon = Icons.Rounded.CallToAction,
                            checked = appearance.enableFloatingBottomBar,
                            onChange = { show -> update { it.copy(enableFloatingBottomBar = show) } }
                        )
                    }
                    if (hasBackdrop && appearance.enableFloatingBottomBar) add {
                        SettingsToggle(
                            title = "液态玻璃", summary = "启用悬浮底栏的液态玻璃效果", icon = Icons.Rounded.WaterDrop,
                            checked = appearance.enableFloatingBottomBarBlur,
                            onChange = { show -> update { it.copy(enableFloatingBottomBarBlur = show) } }
                        )
                    }
                    add(badgeRow)
                }
                SettingsGroup(modifier = Modifier.padding(top = 12.dp), rows = rows)
            }
            item {
                val rows = buildList<@Composable () -> Unit> {
                    if (hasPredictiveBack) add(backRow)
                    add(swipeRow)
                    add(pagerRow)
                    add { MiuixScaleRow(appearance.pageScale, setScale) }
                }
                SettingsGroup(modifier = Modifier.padding(top = 12.dp), rows = rows)
            }
        } else {
            item { MaterialSeedPicker(appearance, choices, labels, setSeed) }
            item { MaterialModePicker(appearance.colorMode, setMode) }
            val sides = Modifier.padding(horizontal = 16.dp)
            item { SettingsGroup(modifier = sides, rows = listOf(styleRow, specRow)) }
            item { SettingsGroup(modifier = sides, rows = listOf(badgeRow)) }
            if (hasPredictiveBack) item { SettingsGroup(modifier = sides, rows = listOf(backRow)) }
            item { SettingsGroup(modifier = sides, rows = listOf(swipeRow, pagerRow)) }
            item { MaterialScaleCard(appearance.pageScale, setScale) }
        }
    }
}

@Composable
private fun MiuixModePicker(mode: ColorMode, onSelect: (ColorMode) -> Unit) {
    val options = listOf(ColorMode.SYSTEM, ColorMode.LIGHT, ColorMode.DARK)
    val index = if (mode == ColorMode.AMOLED) 2 else options.indexOf(mode)
    val colors = MiuixTheme.colorScheme
    val curve = RoundedCornerShape(12.dp)
    val gap = 9.dp
    BoxWithConstraints(Modifier.fillMaxWidth().height(42.dp)) {
        val width = (maxWidth - gap * (options.size - 1)) / options.size
        val offset by animateDpAsState((width + gap) * index, label = "themeMode")
        Spacer(Modifier.offset(x = offset).width(width).fillMaxHeight().background(colors.surface, curve))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, option ->
                val selected = index == i
                val outline = if (selected) Modifier else Modifier.border(1.dp, colors.outline, curve)
                Box(
                    Modifier.weight(1f).fillMaxHeight().then(outline).clip(curve)
                        .selectable(selected, role = Role.Tab, onClick = { onSelect(option) }),
                    contentAlignment = Alignment.Center
                ) {
                    MiuixText(
                        text = option.label,
                        color = if (selected) colors.onBackground else colors.onSurfaceVariantSummary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
