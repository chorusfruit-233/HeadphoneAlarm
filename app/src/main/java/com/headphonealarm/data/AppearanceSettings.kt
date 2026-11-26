package com.headphonealarm.data

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

enum class InterfaceStyle(val label: String) {
    MIUIX("Miuix"),
    MATERIAL("Material")
}

enum class ColorMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
    AMOLED("纯黑");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK, AMOLED -> true
    }
}

enum class LegacyTheme(val key: String, val label: String) {
    AURORA("aurora", "极光"),
    CYBER("cyber", "赛博朋克"),
    MATERIAL("material", "安卓原生"),
    ANIME("anime", "动漫"),
    OCEAN("ocean", "海洋"),
    FOREST("forest", "森林");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

data class AppearanceSettings(
    val interfaceStyle: InterfaceStyle = InterfaceStyle.MIUIX,
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    val legacyTheme: LegacyTheme? = null,
    val miuixMonet: Boolean = false,
    val pageScale: Float = 1f,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    val enablePredictiveBack: Boolean = false,
    val enableSwipeDismiss: Boolean = true,
    val pagerInterceptionMode: Int = 1
) {
    val usesAmoledSurfaces: Boolean
        get() = interfaceStyle == InterfaceStyle.MATERIAL && colorMode == ColorMode.AMOLED && legacyTheme == null

    fun withKeyColor(color: Int) = copy(keyColor = color, legacyTheme = null)
}

val PaletteStyle.supports2025: Boolean
    get() = when (this) {
        PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Expressive -> true
        else -> false
    }
