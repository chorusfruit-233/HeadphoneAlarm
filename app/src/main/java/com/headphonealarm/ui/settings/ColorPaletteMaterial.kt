package com.headphonealarm.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.ColorMode
import com.headphonealarm.ui.theme.rememberPaletteColors

@Composable
internal fun MaterialSeedPicker(
    appearance: AppearanceSettings,
    choices: List<Int>,
    labels: List<String>,
    onSelect: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val dark = when (appearance.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK, ColorMode.AMOLED -> true
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(choices, key = { _, seed -> seed }) { index, seed ->
            val selected = appearance.keyColor == seed
            val colors = rememberPaletteColors(
                seed, dark, appearance.colorMode == ColorMode.AMOLED, appearance.paletteStyle, appearance.colorSpec
            )
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainer)
                    .selectable(selected, role = Role.RadioButton) {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onSelect(seed)
                    }
                    .semantics { contentDescription = labels[index] },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(48.dp)) {
                    drawArc(colors.primaryContainer, 180f, 180f, useCenter = true)
                    drawArc(colors.tertiaryContainer, 0f, 180f, useCenter = true)
                }
                val zoom by animateFloatAsState(if (selected) 1.1f else 1f, label = "swatch")
                if (selected) {
                    Box(Modifier.size(56.dp).scale(zoom).border(2.dp, colors.primary, CircleShape))
                }
                val size by animateDpAsState(if (selected) 24.dp else 20.dp, label = "selection")
                Box(
                    Modifier.size(size).scale(zoom).background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = colors.onPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MaterialModePicker(mode: ColorMode, onSelect: (ColorMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        ColorMode.entries.forEach { candidate ->
            ToggleButton(
                checked = mode == candidate,
                onCheckedChange = { checked ->
                    if (checked) {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onSelect(candidate)
                    }
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = when (candidate) {
                    ColorMode.SYSTEM -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ColorMode.AMOLED -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = colors.primary,
                    checkedContentColor = colors.onPrimary,
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurface
                )
            ) {
                val icon = when (candidate) {
                    ColorMode.SYSTEM -> Icons.Filled.Brightness4
                    ColorMode.LIGHT -> Icons.Filled.Brightness7
                    ColorMode.DARK -> Icons.Filled.Brightness3
                    ColorMode.AMOLED -> Icons.Filled.Brightness1
                }
                Icon(icon, candidate.label)
            }
        }
    }
}
