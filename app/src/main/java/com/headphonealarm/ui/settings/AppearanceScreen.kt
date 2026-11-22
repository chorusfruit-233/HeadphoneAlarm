package com.headphonealarm.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.headphonealarm.data.AccentSource
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.ColorMode
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.data.LegacyTheme
import com.headphonealarm.data.supports2025
import com.headphonealarm.ui.theme.LocalInterfaceStyle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.extra.SuperDropdown

@Composable
fun AppearanceScreen(
    appearance: AppearanceSettings,
    update: ((AppearanceSettings) -> AppearanceSettings) -> Unit,
    onBack: () -> Unit
) {
    SettingsPage(title = "外观与主题", onBack = onBack) {
        item { AppearancePreview() }
        item {
            SettingsGroup("界面风格") {
                StyleOptions(
                    labels = InterfaceStyle.entries.map { it.label },
                    selected = appearance.interfaceStyle.ordinal,
                    onSelect = { index -> update { it.copy(interfaceStyle = InterfaceStyle.entries[index]) } }
                )
            }
        }
        if (appearance.legacyTheme == null) {
            item {
                SettingsGroup("明暗模式") {
                    StyleOptions(
                        labels = ColorMode.entries.map { it.label },
                        selected = appearance.colorMode.ordinal,
                        onSelect = { index -> update { it.copy(colorMode = ColorMode.entries[index]) } }
                    )
                }
            }
            item {
                SettingsGroup("主题色") {
                    AccentOptions(
                        appearance = appearance,
                        onSource = { source -> update { it.copy(accentSource = source) } },
                        onColor = { color -> update { it.copy(accentSource = AccentSource.CUSTOM, accentColor = color) } }
                    )
                }
            }
            item {
                SettingsGroup("调色板") {
                    ChoicePreference(
                        title = "色彩风格",
                        icon = Icons.Outlined.Palette,
                        labels = PaletteStyle.entries.map { it.displayName },
                        selected = appearance.paletteStyle.ordinal,
                        onSelect = { index -> update { it.copy(paletteStyle = PaletteStyle.entries[index]) } }
                    )
                    if (appearance.paletteStyle.supports2025) {
                        ChoicePreference(
                            title = "配色规范",
                            icon = Icons.Outlined.AutoAwesome,
                            labels = listOf("Material 2021", "Material 2025"),
                            selected = appearance.colorSpec.ordinal,
                            onSelect = { index -> update { it.copy(colorSpec = ColorSpec.SpecVersion.entries[index]) } }
                        )
                    } else {
                        Text(
                            "当前色彩风格使用 Material 2021 配色规范。",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            SettingsGroup("经典主题") {
                val themes = listOf(null) + LegacyTheme.entries
                ChoicePreference(
                    title = "主题方案",
                    icon = Icons.Outlined.Style,
                    labels = themes.map { it?.label ?: "现代配色" },
                    selected = themes.indexOf(appearance.legacyTheme),
                    onSelect = { index -> update { it.copy(legacyTheme = themes[index]) } }
                )
                if (appearance.legacyTheme != null) {
                    Text(
                        "经典主题使用固定配色。选择现代配色可调整明暗和主题色。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearancePreview() {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("实时预览", style = MaterialTheme.typography.labelLarge)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = colors.surface,
                contentColor = colors.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("07:30", style = MaterialTheme.typography.displaySmall)
                        Text("仅耳机播放", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Surface(shape = CircleShape, color = colors.secondaryContainer, contentColor = colors.onSecondaryContainer) {
                        Icon(Icons.Outlined.Headphones, contentDescription = null, modifier = Modifier.padding(13.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(colors.primary, colors.secondary, colors.tertiary, colors.surface).forEach { color ->
                    Spacer(Modifier.weight(1f).size(8.dp).background(color, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun StyleOptions(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        MiuixTabRow(tabs = labels, selectedTabIndex = selected, minWidth = 58.dp, height = 48.dp, onTabSelected = onSelect)
    } else {
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEachIndexed { index, label ->
                FilterChip(selected = selected == index, onClick = { onSelect(index) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun AccentOptions(
    appearance: AppearanceSettings,
    onSource: (AccentSource) -> Unit,
    onColor: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val supportsWallpaper = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = appearance.accentSource == AccentSource.DEFAULT ||
                    (!supportsWallpaper && appearance.accentSource == AccentSource.WALLPAPER),
                onClick = { onSource(AccentSource.DEFAULT) },
                label = { Text("默认") }
            )
            if (supportsWallpaper) {
                FilterChip(
                    selected = appearance.accentSource == AccentSource.WALLPAPER,
                    onClick = { onSource(AccentSource.WALLPAPER) },
                    label = { Text("壁纸取色") }
                )
            }
        }
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            accentColors.forEach { (label, argb) ->
                val selected = appearance.accentSource == AccentSource.CUSTOM && appearance.accentColor == argb
                val color = Color(argb)
                val border = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
                Box(
                    modifier = Modifier.size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, border, CircleShape)
                        .selectable(selected, role = Role.RadioButton, onClick = { onColor(argb) })
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(36.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                        if (selected) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.18f) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoicePreference(
    title: String,
    icon: ImageVector,
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        SuperDropdown(
            items = labels,
            selectedIndex = selected,
            title = title,
            maxHeight = 340.dp,
            insideMargin = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            onSelectedIndexChange = onSelect
        )
        return
    }

    var open by rememberSaveable { mutableStateOf(false) }
    SettingsLink(title = title, summary = labels[selected], icon = icon, onClick = { open = true })
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()).selectableGroup()) {
                    labels.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(selected == index, role = Role.RadioButton, onClick = {
                                    open = false
                                    onSelect(index)
                                }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(selected = selected == index, onClick = null)
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("关闭") } }
        )
    }
}

private val PaletteStyle.displayName: String
    get() = when (this) {
        PaletteStyle.TonalSpot -> "柔和"
        PaletteStyle.Neutral -> "中性"
        PaletteStyle.Vibrant -> "鲜艳"
        PaletteStyle.Expressive -> "表现力"
        PaletteStyle.Rainbow -> "彩虹"
        PaletteStyle.FruitSalad -> "缤纷"
        PaletteStyle.Monochrome -> "单色"
        PaletteStyle.Fidelity -> "还原主题色"
        PaletteStyle.Content -> "邻近色"
    }

private val accentColors = listOf(
    "珊瑚红" to 0xFFDF594F.toInt(),
    "莓粉" to 0xFFBA456F.toInt(),
    "樱花粉" to 0xFFE7A0B9.toInt(),
    "兰紫" to 0xFF9E56C4.toInt(),
    "薰衣草紫" to 0xFF6750A4.toInt(),
    "靛蓝" to 0xFF565BBC.toInt(),
    "晴空蓝" to 0xFF3482FF.toInt(),
    "湖蓝" to 0xFF149BB3.toInt(),
    "青绿" to 0xFF008878.toInt(),
    "森林绿" to 0xFF4E943B.toInt(),
    "柠檬黄" to 0xFFE8CB58.toInt(),
    "琥珀" to 0xFFD49622.toInt(),
    "橙色" to 0xFFE37832.toInt(),
    "暖棕" to 0xFF916354.toInt(),
    "蓝灰" to 0xFF64788A.toInt()
)
