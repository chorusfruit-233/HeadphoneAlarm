package com.headphonealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_store")

/** 应用级设置持久化。 */
class SettingsRepository(private val context: Context) {

    val appearance: Flow<AppearanceSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toAppearance() }

    suspend fun updateAppearance(update: (AppearanceSettings) -> AppearanceSettings) {
        context.settingsDataStore.edit { preferences ->
            val appearance = update(preferences.toAppearance())
            preferences[KEY_INTERFACE] = appearance.interfaceStyle.name
            preferences[KEY_MODE] = appearance.colorMode.name
            preferences[KEY_ACCENT_SOURCE] = appearance.accentSource.name
            preferences[KEY_ACCENT] = appearance.accentColor
            preferences[KEY_PALETTE] = appearance.paletteStyle.name
            preferences[KEY_SPEC] = appearance.colorSpec.name
            val legacy = appearance.legacyTheme
            if (legacy == null) preferences.remove(KEY_THEME) else preferences[KEY_THEME] = legacy.key
        }
    }

    private fun Preferences.toAppearance() = AppearanceSettings(
        interfaceStyle = enumSetting(this[KEY_INTERFACE], defaults.interfaceStyle),
        colorMode = enumSetting(this[KEY_MODE], defaults.colorMode),
        accentSource = enumSetting(this[KEY_ACCENT_SOURCE], defaults.accentSource),
        accentColor = this[KEY_ACCENT] ?: defaults.accentColor,
        paletteStyle = enumSetting<PaletteStyle>(this[KEY_PALETTE], defaults.paletteStyle),
        colorSpec = enumSetting<ColorSpec.SpecVersion>(this[KEY_SPEC], defaults.colorSpec),
        legacyTheme = LegacyTheme.fromKey(this[KEY_THEME])
    )

    private inline fun <reified T : Enum<T>> enumSetting(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        val defaults = AppearanceSettings()
        // v1.0.x 已保存的主题仍按原样读取。
        val KEY_THEME = stringPreferencesKey("theme_key")
        val KEY_INTERFACE = stringPreferencesKey("interface_style")
        val KEY_MODE = stringPreferencesKey("color_mode")
        val KEY_ACCENT_SOURCE = stringPreferencesKey("accent_source")
        val KEY_ACCENT = intPreferencesKey("accent_color")
        val KEY_PALETTE = stringPreferencesKey("palette_style")
        val KEY_SPEC = stringPreferencesKey("color_spec")
    }
}
