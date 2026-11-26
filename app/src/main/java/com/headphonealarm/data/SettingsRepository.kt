package com.headphonealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_store")

/** 应用级设置持久化。 */
class SettingsRepository(private val context: Context) {

    val keepAliveEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_KEEP_ALIVE] ?: false }
        .distinctUntilChanged()

    suspend fun setKeepAliveEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_KEEP_ALIVE] = enabled }
    }

    val appearance: Flow<AppearanceSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toAppearance() }

    suspend fun updateAppearance(update: (AppearanceSettings) -> AppearanceSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences.saveAppearance(update(preferences.toAppearance()))
        }
    }
}

internal fun MutablePreferences.saveAppearance(appearance: AppearanceSettings) {
    this[KEY_INTERFACE] = appearance.interfaceStyle.name
    this[KEY_MODE] = appearance.colorMode.name
    this[KEY_ACCENT_SOURCE] = if (appearance.keyColor == 0) "WALLPAPER" else "CUSTOM"
    this[KEY_ACCENT] = appearance.keyColor
    this[KEY_PALETTE] = appearance.paletteStyle.name
    this[KEY_SPEC] = appearance.colorSpec.name
    this[KEY_MIUIX_MONET] = appearance.miuixMonet
    this[KEY_PAGE_SCALE] = appearance.pageScale.coerceIn(0.8f, 1.1f)
    this[KEY_BLUR] = appearance.enableBlur
    this[KEY_FLOATING_BAR] = appearance.enableFloatingBottomBar
    this[KEY_FLOATING_BAR_BLUR] = appearance.enableFloatingBottomBarBlur
    this[KEY_NAV_BADGE] = appearance.enableNavigationBadge
    this[KEY_PREDICTIVE_BACK] = appearance.enablePredictiveBack
    this[KEY_SWIPE_DISMISS] = appearance.enableSwipeDismiss
    this[KEY_PAGER_MODE] = appearance.pagerInterceptionMode.coerceIn(0, 2)
    val legacy = appearance.legacyTheme
    if (legacy == null) remove(KEY_THEME) else this[KEY_THEME] = legacy.key
}

internal fun Preferences.toAppearance(): AppearanceSettings {
    val legacy = LegacyTheme.fromKey(this[KEY_THEME])
    val style = enumSetting(this[KEY_INTERFACE], if (legacy == null) defaults.interfaceStyle else InterfaceStyle.MATERIAL)
    // v1.1.0/1.1.1 always generated Miuix colors with Monet, even for their “default” seed.
    val oldMiuix = this[KEY_INTERFACE] == InterfaceStyle.MIUIX.name
    val seed = when (this[KEY_ACCENT_SOURCE]) {
        "DEFAULT" -> if (oldMiuix) 0xFF3482FF.toInt() else 0xFF6750A4.toInt()
        "CUSTOM" -> this[KEY_ACCENT] ?: defaults.keyColor
        else -> defaults.keyColor
    }
    return AppearanceSettings(
        interfaceStyle = style,
        colorMode = enumSetting(this[KEY_MODE], defaults.colorMode),
        keyColor = seed,
        paletteStyle = enumSetting<PaletteStyle>(this[KEY_PALETTE], defaults.paletteStyle),
        colorSpec = enumSetting<ColorSpec.SpecVersion>(this[KEY_SPEC], defaults.colorSpec),
        legacyTheme = legacy,
        miuixMonet = this[KEY_MIUIX_MONET] ?: oldMiuix,
        pageScale = this[KEY_PAGE_SCALE]?.coerceIn(0.8f, 1.1f) ?: defaults.pageScale,
        enableBlur = this[KEY_BLUR] ?: defaults.enableBlur,
        enableFloatingBottomBar = this[KEY_FLOATING_BAR] ?: defaults.enableFloatingBottomBar,
        enableFloatingBottomBarBlur = this[KEY_FLOATING_BAR_BLUR] ?: defaults.enableFloatingBottomBarBlur,
        enableNavigationBadge = this[KEY_NAV_BADGE] ?: defaults.enableNavigationBadge,
        enablePredictiveBack = this[KEY_PREDICTIVE_BACK] ?: defaults.enablePredictiveBack,
        enableSwipeDismiss = this[KEY_SWIPE_DISMISS] ?: defaults.enableSwipeDismiss,
        pagerInterceptionMode = this[KEY_PAGER_MODE]?.coerceIn(0, 2) ?: defaults.pagerInterceptionMode
    )
}

private inline fun <reified T : Enum<T>> enumSetting(value: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default

private val defaults = AppearanceSettings()
// v1.0.x 已保存的主题仍按原样读取。
private val KEY_THEME = stringPreferencesKey("theme_key")
private val KEY_INTERFACE = stringPreferencesKey("interface_style")
private val KEY_MODE = stringPreferencesKey("color_mode")
private val KEY_ACCENT_SOURCE = stringPreferencesKey("accent_source")
private val KEY_ACCENT = intPreferencesKey("accent_color")
private val KEY_PALETTE = stringPreferencesKey("palette_style")
private val KEY_SPEC = stringPreferencesKey("color_spec")
private val KEY_MIUIX_MONET = booleanPreferencesKey("miuix_monet")
private val KEY_PAGE_SCALE = floatPreferencesKey("page_scale")
private val KEY_BLUR = booleanPreferencesKey("enable_blur")
private val KEY_FLOATING_BAR = booleanPreferencesKey("enable_floating_bottom_bar")
private val KEY_FLOATING_BAR_BLUR = booleanPreferencesKey("enable_floating_bottom_bar_blur")
private val KEY_NAV_BADGE = booleanPreferencesKey("enable_navigation_badge")
private val KEY_PREDICTIVE_BACK = booleanPreferencesKey("enable_predictive_back")
private val KEY_SWIPE_DISMISS = booleanPreferencesKey("enable_swipe_dismiss")
private val KEY_PAGER_MODE = intPreferencesKey("pager_interception_mode")
private val KEY_KEEP_ALIVE = booleanPreferencesKey("shizuku_keep_alive")
