package com.headphonealarm.data

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

enum class InterfaceStyle(val label: String) {
    MATERIAL("Material 3"),
    MIUIX("Miuix")
}

enum class ColorMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
    AMOLED("纯黑")
}

enum class AccentSource {
    DEFAULT, WALLPAPER, CUSTOM
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
    val interfaceStyle: InterfaceStyle = InterfaceStyle.MATERIAL,
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val accentSource: AccentSource = AccentSource.WALLPAPER,
    val accentColor: Int = 0xFF6750A4.toInt(),
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    val legacyTheme: LegacyTheme? = null
)

val PaletteStyle.supports2025: Boolean
    get() = when (this) {
        PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Expressive -> true
        else -> false
    }
