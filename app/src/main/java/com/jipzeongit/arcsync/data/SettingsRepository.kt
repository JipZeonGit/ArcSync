package com.jipzeongit.arcsync.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    enum class ThemeMode { OFF, ON, SYSTEM }
    enum class ThemeStyle { DYNAMIC, OCEAN, PURPLE, FOREST, SLATE, AMBER }

    private val themeKey = stringPreferencesKey("theme_mode")
    private val themeStyleKey = stringPreferencesKey("theme_style")
    private val amoledKey = booleanPreferencesKey("amoled_enabled")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color_enabled")
    private val langKey = stringPreferencesKey("app_lang")

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            ThemeMode.OFF.name -> ThemeMode.OFF
            ThemeMode.ON.name -> ThemeMode.ON
            else -> ThemeMode.SYSTEM
        }
    }

    val amoledEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[amoledKey] ?: false
    }

    val themeStyleFlow: Flow<ThemeStyle> = context.dataStore.data.map { prefs ->
        when (prefs[themeStyleKey]) {
            ThemeStyle.DYNAMIC.name -> ThemeStyle.DYNAMIC
            ThemeStyle.PURPLE.name -> ThemeStyle.PURPLE
            ThemeStyle.FOREST.name -> ThemeStyle.FOREST
            ThemeStyle.SLATE.name -> ThemeStyle.SLATE
            ThemeStyle.AMBER.name -> ThemeStyle.AMBER
            ThemeStyle.OCEAN.name -> ThemeStyle.OCEAN
            null -> if (prefs[dynamicColorKey] ?: true) ThemeStyle.DYNAMIC else ThemeStyle.OCEAN
            else -> ThemeStyle.OCEAN
        }
    }

    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[dynamicColorKey] ?: true
    }

    val appLangFlow: Flow<AppLang> = context.dataStore.data.map { prefs ->
        when (prefs[langKey]) {
            AppLang.ZH_TW.code -> AppLang.ZH_TW
            AppLang.EN.code -> AppLang.EN
            else -> AppLang.ZH_CN
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[themeKey] = mode.name }
    }

    suspend fun setAmoledEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[amoledKey] = enabled }
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        context.dataStore.edit { prefs ->
            prefs[themeStyleKey] = style.name
            prefs[dynamicColorKey] = style == ThemeStyle.DYNAMIC
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[dynamicColorKey] = enabled
            prefs[themeStyleKey] = if (enabled) ThemeStyle.DYNAMIC.name else ThemeStyle.OCEAN.name
        }
    }

    suspend fun setAppLang(lang: AppLang) {
        context.dataStore.edit { prefs -> prefs[langKey] = lang.code }
    }
}
