package com.headphonealarm.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun freshInstallFollowsSystemWithNativeMiuixColors() {
        val appearance = emptyPreferences().toAppearance()
        assertEquals(InterfaceStyle.MIUIX, appearance.interfaceStyle)
        assertEquals(ColorMode.SYSTEM, appearance.colorMode)
        assertFalse(appearance.miuixMonet)
        assertEquals(0, appearance.keyColor)
        assertEquals(PaletteStyle.TonalSpot, appearance.paletteStyle)
        assertEquals(ColorSpec.SpecVersion.SPEC_2025, appearance.colorSpec)
    }

    @Test
    fun v1_0ClassicKeyOpensTheOriginalTheme() {
        val preferences = mutablePreferencesOf(stringPreferencesKey("theme_key") to "ocean")
        val appearance = preferences.toAppearance()
        assertEquals(LegacyTheme.OCEAN, appearance.legacyTheme)
        assertEquals(InterfaceStyle.MATERIAL, appearance.interfaceStyle)
        assertEquals(null, appearance.withKeyColor(0).legacyTheme)
    }

    @Test
    fun v1_1MiuixHonorsBothCustomAndOldBuiltInSeeds() {
        val coral = 0xFFDF594F.toInt()
        val source = stringPreferencesKey("accent_source")
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("interface_style") to "MIUIX",
            stringPreferencesKey("color_mode") to "AMOLED",
            source to "CUSTOM",
            intPreferencesKey("accent_color") to coral
        )
        val custom = preferences.toAppearance()
        assertTrue(custom.miuixMonet)
        assertEquals(coral, custom.keyColor)
        assertEquals(ColorMode.AMOLED, custom.colorMode)
        assertFalse(custom.usesAmoledSurfaces)

        preferences[source] = "DEFAULT"
        assertEquals(0xFF3482FF.toInt(), preferences.toAppearance().keyColor)
    }

    @Test
    fun appearanceWritesPreserveKeepAliveAndRememberTheSeedWhenMonetIsOff() {
        val keepAlive = booleanPreferencesKey("shizuku_keep_alive")
        val preferences = mutablePreferencesOf(keepAlive to true)
        val saved = AppearanceSettings().withKeyColor(0xFFF44336.toInt()).copy(
            miuixMonet = true, pageScale = 0.9f, enableBlur = true,
            enableFloatingBottomBar = true, enableFloatingBottomBarBlur = true,
            enableNavigationBadge = false, enablePredictiveBack = true,
            enableSwipeDismiss = false, pagerInterceptionMode = 2
        )
        preferences.saveAppearance(saved)
        assertEquals(saved, preferences.toAppearance())

        val noMonet = saved.copy(miuixMonet = false)
        preferences.saveAppearance(noMonet)
        assertEquals(noMonet, preferences.toAppearance())
        assertEquals(true, preferences[keepAlive])

        preferences.saveAppearance(noMonet.withKeyColor(0))
        assertEquals(0, preferences.toAppearance().keyColor)
        assertFalse(preferences.toAppearance().miuixMonet)
    }

    @Test
    fun materialModesResolveAgainstSystemAndAmoledOnlyBlacksOutModernMaterial() {
        assertFalse(ColorMode.SYSTEM.isDark(systemDark = false))
        assertTrue(ColorMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ColorMode.LIGHT.isDark(systemDark = true))
        assertTrue(ColorMode.DARK.isDark(systemDark = false))
        assertTrue(ColorMode.AMOLED.isDark(systemDark = false))
        val black = AppearanceSettings(interfaceStyle = InterfaceStyle.MATERIAL, colorMode = ColorMode.AMOLED)
        assertTrue(black.usesAmoledSurfaces)
        assertFalse(black.copy(colorMode = ColorMode.DARK).usesAmoledSurfaces)
        assertFalse(black.copy(legacyTheme = LegacyTheme.FOREST).usesAmoledSurfaces)
    }
}
