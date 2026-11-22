package com.headphonealarm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.headphonealarm.data.AccentSource
import com.headphonealarm.data.AppearanceSettings
import com.headphonealarm.data.ColorMode
import com.headphonealarm.data.InterfaceStyle
import com.headphonealarm.data.supports2025
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColors
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColors
import kotlin.math.cos
import kotlin.math.sin

val LocalInterfaceStyle = staticCompositionLocalOf { InterfaceStyle.MATERIAL }
val LocalClassicTheme = staticCompositionLocalOf { false }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium)
    )
}

private val MiuixShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val MiuixTypography = AppTypography.copy(
    headlineMedium = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    bodyLarge = AppTypography.bodyLarge.copy(letterSpacing = 0.sp)
)

@Composable
fun HeadphoneAlarmTheme(
    appearance: AppearanceSettings,
    content: @Composable () -> Unit
) {
    val legacy = appearance.legacyTheme
    val classicBackdrop = legacy?.let(::backdropFor)
    val systemDark = isSystemInDarkTheme()
    val dark = classicBackdrop?.let { !it.isLight } ?: when (appearance.colorMode) {
        ColorMode.SYSTEM -> systemDark
        ColorMode.LIGHT -> false
        ColorMode.DARK, ColorMode.AMOLED -> true
    }
    val miuix = appearance.interfaceStyle == InterfaceStyle.MIUIX
    val amoled = legacy == null && appearance.colorMode == ColorMode.AMOLED
    val targetScheme = if (legacy == null) {
        modernColors(appearance, dark, amoled, miuix)
    } else {
        remember(legacy) { colorSchemeFor(legacy) }
    }
    val scheme = animateColorScheme(targetScheme)
    val backdrop = classicBackdrop ?: remember(scheme, dark, miuix) {
        val stops = if (miuix) {
            listOf(scheme.background, scheme.background)
        } else {
            listOf(scheme.surfaceContainerLowest, scheme.background)
        }
        AppBackdrop(stops, emptyList(), stops, Decoration.NONE, isLight = !dark)
    }

    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(view, context, dark) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    val miuixColors = remember(scheme, dark) {
        val base = if (dark) miuixDarkColors() else miuixLightColors()
        base.copy(
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            primaryVariant = scheme.primary,
            onPrimaryVariant = scheme.onPrimary,
            primaryContainer = scheme.primaryContainer,
            onPrimaryContainer = scheme.onPrimaryContainer,
            secondary = scheme.secondaryContainer,
            onSecondary = scheme.onSecondaryContainer,
            tertiaryContainer = scheme.tertiaryContainer,
            onTertiaryContainer = scheme.onTertiaryContainer,
            background = scheme.background,
            onBackground = scheme.onBackground,
            onBackgroundVariant = scheme.onSurfaceVariant,
            surface = scheme.surfaceContainer,
            surfaceVariant = scheme.surfaceContainerHigh,
            onSurface = scheme.onSurface,
            onSurfaceSecondary = scheme.onSurfaceVariant,
            onSurfaceVariantSummary = scheme.onSurfaceVariant,
            onSurfaceVariantActions = scheme.onSurfaceVariant,
            surfaceContainer = scheme.surfaceContainer,
            onSurfaceContainer = scheme.onSurface,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerHighest = scheme.surfaceContainerHighest,
            outline = scheme.outline,
            dividerLine = scheme.outlineVariant
        )
    }

    CompositionLocalProvider(
        LocalAppBackdrop provides backdrop,
        LocalInterfaceStyle provides appearance.interfaceStyle,
        LocalClassicTheme provides (legacy != null)
    ) {
        // Keep both providers mounted so a style switch preserves navigation and editor state.
        MiuixTheme(colors = miuixColors) {
            MaterialTheme(
                colorScheme = scheme,
                shapes = if (miuix) MiuixShapes else AppShapes,
                typography = if (miuix) MiuixTypography else AppTypography,
                content = content
            )
        }
    }
}

@Composable
private fun modernColors(
    appearance: AppearanceSettings,
    dark: Boolean,
    amoled: Boolean,
    miuix: Boolean
): ColorScheme {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val defaultSeed = if (miuix) Color(0xFF3482FF) else Color(0xFF6750A4)
    val seed = when (appearance.accentSource) {
        AccentSource.DEFAULT -> defaultSeed
        AccentSource.CUSTOM -> Color(appearance.accentColor)
        AccentSource.WALLPAPER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            remember(context, configuration) { dynamicLightColorScheme(context).primary }
        } else {
            defaultSeed
        }
    }
    val scheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = dark,
        isAmoled = amoled,
        style = appearance.paletteStyle,
        specVersion = if (appearance.paletteStyle.supports2025) {
            appearance.colorSpec
        } else {
            ColorSpec.SpecVersion.SPEC_2021
        }
    )
    return remember(scheme, amoled) {
        if (amoled) scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black
        ) else scheme
    }
}

/**
 * 主题化背景：竖向渐变 + 若干径向光斑；对需要动效的主题额外叠加 [AnimatedDecoration]。
 * 当装饰为 NONE（极光/安卓原生）时不合成动画层，避免每帧无谓重绘。
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bd = LocalAppBackdrop.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bd.gradient))
    ) {
        if (bd.glows.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rad = size.minDimension
                bd.glows.forEach { g ->
                    val c = Offset(size.width * g.cx, size.height * g.cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(g.color, Color.Transparent), center = c, radius = rad * g.r),
                        radius = rad * g.r,
                        center = c
                    )
                }
            }
        }
        if (bd.decoration != Decoration.NONE) {
            AnimatedDecoration(bd.decoration)
        }
        content()
    }
}

/** 各主题专属的循环装饰层（0→1 进度驱动，仅在该主题下才被组合） */
@Composable
private fun AnimatedDecoration(decoration: Decoration) {
    val periodMs = when (decoration) {
        Decoration.GRID -> 5000
        Decoration.BUBBLES -> 7000
        Decoration.LEAVES -> 10000
        Decoration.PETALS -> 9000
        Decoration.NONE -> 6000
    }
    val transition = rememberInfiniteTransition(label = "deco")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (decoration) {
            Decoration.GRID -> {
                val neon = Color(0xFF00F2C3)
                val magenta = Color(0xFFFF2DF9)
                var y = (p * 60f) % 60f
                while (y < h) {
                    drawLine(neon.copy(alpha = 0.06f), Offset(0f, y), Offset(w, y), 1f)
                    y += 60f
                }
                var x = 0f
                while (x < w) {
                    drawLine(magenta.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, h), 1f)
                    x += 60f
                }
                val sy = p * h
                drawLine(neon.copy(alpha = 0.10f), Offset(0f, sy), Offset(w, sy), 2f)
            }

            Decoration.BUBBLES -> {
                val bubble = Color(0xFFFFFFFF)
                val spots = arrayOf(floatArrayOf(0.20f, 10f), floatArrayOf(0.50f, 6f), floatArrayOf(0.78f, 12f), floatArrayOf(0.38f, 5f), floatArrayOf(0.62f, 8f))
                spots.forEachIndexed { i, s ->
                    val speed = 0.6f + i * 0.12f
                    val yy = h * (1f - ((p * speed + i * 0.2f) % 1f))
                    val xx = w * s[0] + sin(p * 6.283f + i) * 10f
                    drawCircle(bubble.copy(alpha = 0.16f), radius = s[1], center = Offset(xx, yy))
                }
            }

            Decoration.LEAVES -> {
                val blobs = arrayOf(floatArrayOf(0.25f, 0.20f, 90f), floatArrayOf(0.75f, 0.42f, 120f), floatArrayOf(0.52f, 0.78f, 80f))
                val dapple = Color(0xFFE6E182)
                blobs.forEachIndexed { i, s ->
                    val cx = w * s[0] + sin(p * 6.283f + i) * 12f
                    val cy = h * s[1] + cos(p * 6.283f + i) * 12f
                    val c = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(dapple.copy(alpha = 0.12f), Color.Transparent), center = c, radius = s[2]),
                        radius = s[2],
                        center = c
                    )
                }
            }

            Decoration.PETALS -> {
                val petal = Color(0xFFFFFFFF)
                val spots = arrayOf(floatArrayOf(0.20f, 7f), floatArrayOf(0.72f, 5f), floatArrayOf(0.46f, 6f), floatArrayOf(0.86f, 4f), floatArrayOf(0.30f, 6f))
                spots.forEachIndexed { i, s ->
                    val speed = 0.5f + i * 0.1f
                    val yy = h * ((p * speed + i * 0.18f) % 1f)
                    val xx = w * s[0] + sin(p * 12.566f + i) * 14f
                    drawCircle(petal.copy(alpha = 0.65f), radius = s[1], center = Offset(xx, yy))
                }
            }

            Decoration.NONE -> Unit
        }
    }
}
