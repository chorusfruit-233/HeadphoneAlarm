package com.headphonealarm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.ui.theme.LocalInterfaceStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.extra.DropdownDefaults
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsGroup(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        Card(modifier.fillMaxWidth()) { rows.forEach { it() } }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEachIndexed { index, row ->
                val top = if (index == 0) 16.dp else 4.dp
                val bottom = if (index == rows.lastIndex) 16.dp else 4.dp
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                ) { row() }
            }
        }
    }
}

@Composable
internal fun MiuixRowIcon(icon: ImageVector, enabled: Boolean = true) {
    val colors = MiuixTheme.colorScheme
    Icon(
        imageVector = icon, contentDescription = null, modifier = Modifier.padding(end = 14.dp),
        tint = if (enabled) colors.onBackground else colors.disabledOnSecondaryVariant
    )
}

@Composable
private fun MaterialSettingsRow(
    title: String,
    summary: String?,
    icon: ImageVector,
    modifier: Modifier,
    trailing: @Composable () -> Unit
) {
    ListItem(
        modifier = modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailing
    )
}

@Composable
internal fun SettingsLink(
    title: String,
    summary: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    value: String? = null
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        SuperArrow(
            title = title, summary = summary, rightText = value,
            leftAction = { MiuixRowIcon(icon) }, onClick = onClick
        )
    } else {
        MaterialSettingsRow(title, summary, icon, Modifier.clickable(role = Role.Button, onClick = onClick)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) Text(value)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
internal fun SettingsToggle(
    title: String,
    summary: String? = null,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        SuperSwitch(
            title = title, summary = summary, leftAction = { MiuixRowIcon(icon, enabled) },
            checked = checked, enabled = enabled, onCheckedChange = onChange
        )
    } else {
        val haptic = LocalHapticFeedback.current
        MaterialSettingsRow(
            title, summary, icon,
            Modifier.alpha(if (enabled) 1f else 0.38f).toggleable(
                value = checked, enabled = enabled, role = Role.Switch
            ) { next ->
                haptic.performHapticFeedback(if (next) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onChange(next)
            }
        ) {
            Switch(
                checked = checked, onCheckedChange = null, enabled = enabled,
                thumbContent = if (checked || enabled) {
                    {
                        Icon(
                            if (checked) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
internal fun SettingsDropdown(
    title: String,
    summary: String? = null,
    icon: ImageVector,
    choices: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    if (LocalInterfaceStyle.current == InterfaceStyle.MIUIX) {
        // This version of SuperDropdown supplies its own popup anchor in the leading slot.
        Box(Modifier.fillMaxWidth()) {
            SuperDropdown(
                title = title, summary = summary, items = choices,
                selectedIndex = selected, onSelectedIndexChange = onSelect,
                dropdownColors = DropdownDefaults.dropdownColors(selectedContainerColor = MiuixTheme.colorScheme.surface),
                insideMargin = PaddingValues(start = 54.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            )
            Box(Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) { MiuixRowIcon(icon) }
        }
    } else {
        val haptic = LocalHapticFeedback.current
        var expanded by remember { mutableStateOf(false) }
        var anchor by remember { mutableStateOf(IntOffset.Zero) }
        Box(Modifier.fillMaxWidth().pointerInput(Unit) {
            awaitEachGesture { anchor = awaitFirstDown(requireUnconsumed = false).position.round() }
        }) {
            MaterialSettingsRow(
                title, summary, icon,
                Modifier.clickable(role = Role.DropdownList) {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    expanded = true
                }
            ) {
                Text(
                    choices.getOrElse(selected) { "" },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(0.3f),
                    textAlign = TextAlign.End
                )
            }
            Box(Modifier.offset { anchor }) {
                DropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.widthIn(min = 136.dp)
                ) {
                    choices.forEachIndexed { index, label ->
                        val chosen = index == selected
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                if (chosen) Icon(Icons.Filled.Check, contentDescription = "已选择")
                                else Spacer(Modifier.size(24.dp))
                            },
                            modifier = Modifier.background(
                                if (chosen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            ),
                            onClick = {
                                expanded = false
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onSelect(index)
                            }
                        )
                    }
                }
            }
        }
    }
}
