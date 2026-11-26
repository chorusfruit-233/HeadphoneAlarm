package com.headphonealarm.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.headphonealarm.data.supports2025
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import top.yukonga.miuix.kmp.theme.Colors

val paletteKeyColors = listOf(
    0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
    0xFF2196F3, 0xFF00BCD4, 0xFF009688, 0xFF4FAF50, 0xFFFFEB3B,
    0xFFFFC107, 0xFFFF9800, 0xFF795548, 0xFF607D8F, 0xFFFF9CA8
).map { it.toInt() }

@Composable
fun rememberPaletteColors(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    style: PaletteStyle,
    spec: ColorSpec.SpecVersion
): ColorScheme {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val seed = when {
        keyColor != 0 -> Color(keyColor)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> remember(context, configuration, isDark) {
            val system = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            system.primary
        }
        else -> Color(0xFF6750A4)
    }
    val generated = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = style,
        specVersion = if (style.supports2025) spec else ColorSpec.SpecVersion.SPEC_2021
    )
    return remember(generated, isAmoled) {
        if (isAmoled) generated.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black
        ) else generated
    }
}

/** Miuix 0.5 draws its page with background and its card with surface. */
internal fun ColorScheme.asMiuixColors(base: Colors): Colors {
    fun Color.over(background: Color, alpha: Float) = copy(alpha = alpha).compositeOver(background)
    val mutedPrimary = primary.over(surface, 0.38f)
    val mutedSecondary = outlineVariant.over(surface, 0.5f)
    val mutedVariant = surfaceContainerHigh.over(surface, 0.6f)
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryFixed,
        onPrimaryVariant = onPrimaryFixed,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        disabledPrimary = mutedPrimary,
        disabledOnPrimary = onPrimary.over(mutedPrimary, 0.38f),
        disabledPrimaryButton = mutedPrimary,
        disabledOnPrimaryButton = onPrimary.over(mutedPrimary, 0.6f),
        disabledPrimarySlider = mutedPrimary,
        secondary = outlineVariant,
        onSecondary = outline,
        secondaryVariant = surfaceContainerHigh,
        onSecondaryVariant = onSurface,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        secondaryContainerVariant = surfaceContainerHighest,
        onSecondaryContainerVariant = onSurfaceVariant,
        disabledSecondary = mutedSecondary,
        disabledOnSecondary = onSurface.over(mutedSecondary, 0.38f),
        disabledSecondaryVariant = mutedVariant,
        disabledOnSecondaryVariant = onSurface.over(mutedVariant, 0.38f),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = onTertiaryContainer,
        background = surface,
        onBackground = onSurface,
        onBackgroundVariant = primary,
        surface = surfaceContainer,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceSecondary = onSurface.over(surface, 0.8f),
        onSurfaceVariantSummary = onSurfaceVariant,
        onSurfaceVariantActions = onSurfaceVariant,
        disabledOnSurface = onSurface,
        surfaceContainer = surfaceContainer,
        onSurfaceContainer = onSurface,
        onSurfaceContainerVariant = onSurfaceVariant,
        surfaceContainerHigh = surfaceContainerHigh,
        onSurfaceContainerHigh = onSurface.over(surfaceContainerHigh, 0.8f),
        surfaceContainerHighest = surfaceContainerHighest,
        onSurfaceContainerHighest = onSurface,
        outline = outline,
        dividerLine = outlineVariant
    )
}

/** Alarm editing and ringing still use Material widgets in the Miuix interface. */
internal fun Colors.materialColors(dark: Boolean): ColorScheme {
    val defaults = if (dark) darkColorScheme() else lightColorScheme()
    return defaults.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = tertiaryContainer,
        onPrimaryContainer = onTertiaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = secondaryVariant,
        onSecondaryContainer = onSecondaryVariant,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariantSummary,
        surfaceTint = primary,
        surfaceDim = background,
        surfaceBright = surface,
        surfaceContainerLowest = background,
        surfaceContainerLow = surfaceContainer,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = outline,
        outlineVariant = dividerLine
    )
}
