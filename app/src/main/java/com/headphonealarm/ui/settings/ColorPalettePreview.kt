package com.headphonealarm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headphonealarm.R
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.ColorMode
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.ui.useHomeRail
import com.headphonealarm.ui.theme.rememberPaletteColors
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ColorPalettePreview(appearance: AppearanceSettings) {
    val window = LocalWindowInfo.current.containerSize
    val ratio = window.width.coerceAtLeast(1).toFloat() / window.height.coerceAtLeast(1)
    val miuix = appearance.interfaceStyle == InterfaceStyle.MIUIX
    val floating = miuix && appearance.enableFloatingBottomBar
    val rail = useHomeRail(floating)
    val dark = when (appearance.colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK, ColorMode.AMOLED -> true
    }
    val native = MiuixTheme.colorScheme
    val useMiuixAccent = miuix && appearance.keyColor == 0
    val palette = rememberPaletteColors(
        if (useMiuixAccent) native.primary.toArgb() else appearance.keyColor,
        dark,
        appearance.colorMode == ColorMode.AMOLED && !miuix,
        if (useMiuixAccent) PaletteStyle.TonalSpot else appearance.paletteStyle,
        if (useMiuixAccent) ColorSpec.SpecVersion.Default else appearance.colorSpec
    )
    val tinted = !miuix || appearance.miuixMonet
    val background = when {
        !miuix -> palette.surfaceContainer
        tinted -> palette.background
        else -> native.background
    }
    val status = when {
        tinted -> palette.secondaryContainer
        dark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val card = when {
        !miuix -> palette.surfaceBright
        tinted -> palette.surfaceContainerHighest
        else -> native.surfaceVariant
    }
    val text = if (tinted) palette.onSurface else native.onBackground
    val bar = if (tinted) palette.surfaceContainer else native.background
    val outline = if (miuix) native.outline else palette.outlineVariant
    val frame = RoundedCornerShape(20.dp)

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth(0.4f).aspectRatio(ratio).clip(frame)
                .background(background).border(1.dp, outline, frame)
                .clearAndSetSemantics { contentDescription = "主题预览" }
        ) {
            val railWidth = if (miuix) 30.dp else 36.dp
            Column(
                Modifier.fillMaxSize().padding(
                    start = if (rail) railWidth else 0.dp,
                    bottom = if (!miuix && !rail) 40.dp else 0.dp
                )
            ) {
                Box(
                    Modifier.fillMaxWidth().height(if (rail) 36.dp else 48.dp)
                        .padding(start = 12.dp, top = if (rail) 4.dp else if (miuix) 24.dp else 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val style = MaterialTheme.typography.bodyMedium
                    Text(stringResource(R.string.app_name), color = text,
                        style = if (miuix) style.copy(fontSize = 12.sp) else style, maxLines = 1)
                }
                val corners = RoundedCornerShape(if (miuix) 6.dp else 8.dp)
                Column(Modifier.padding(horizontal = if (miuix) 8.dp else 6.dp)) {
                    Spacer(Modifier.fillMaxWidth().height(if (miuix) 45.dp else 40.dp).background(status, corners))
                    BoxWithConstraints(Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                        val extraRows = when {
                            !miuix -> 0
                            maxHeight >= 84.dp -> 2
                            maxHeight >= 60.dp -> 1
                            else -> 0
                        }
                        if (miuix || maxHeight >= 26.dp) {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Spacer(Modifier.fillMaxWidth().weight(1f).background(card, corners))
                                repeat(extraRows) {
                                    Spacer(Modifier.fillMaxWidth().height(12.dp).background(card, corners))
                                }
                            }
                        }
                    }
                }
            }
            val navColor = if (miuix) native.onBackground else palette.primary
            if (rail) {
                Row(Modifier.align(Alignment.CenterStart).fillMaxHeight()) {
                    Column(
                        Modifier.fillMaxHeight().width(railWidth).background(bar),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (miuix) repeat(4) { PreviewTab(navColor, it, Modifier.size(13.dp)) }
                        else Icon(Icons.Filled.Home, contentDescription = null, tint = navColor)
                    }
                    if (miuix) Spacer(Modifier.fillMaxHeight().width(0.5.dp).background(text.copy(alpha = 0.1f)))
                }
            } else if (floating) {
                val pill = RoundedCornerShape(14.dp)
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).height(28.dp)
                        .background(if (appearance.enableFloatingBottomBarBlur) bar.copy(alpha = 0.5f) else bar, pill)
                        .border(0.5.dp, text.copy(alpha = 0.1f), pill).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val icon = if (index == 0) (if (tinted) palette.primary else native.primary) else text
                        Spacer(Modifier.size(13.dp).background(icon, RoundedCornerShape(2.dp)))
                    }
                }
            } else if (miuix) {
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(bar)) {
                    Spacer(Modifier.fillMaxWidth().height(0.5.dp).background(text.copy(alpha = 0.1f)))
                    Row(
                        Modifier.fillMaxWidth().height(36.dp).padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { PreviewTab(navColor, it, Modifier.size(15.dp)) }
                    }
                }
            } else {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = navColor)
                }
            }
        }
    }
}

@Composable
private fun PreviewTab(color: Color, index: Int, modifier: Modifier) {
    Spacer(modifier.background(if (index == 0) color else color.copy(alpha = 0.5f), RoundedCornerShape(3.dp)))
}
