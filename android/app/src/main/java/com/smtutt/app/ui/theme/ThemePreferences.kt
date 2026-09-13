package com.smtutt.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String, val subtitle: String) {
    SYSTEM("Системная (авто)", "Автоматически повторяет тему вашей системы Android"),
    DARK("Тёмная (VS Code Dark+)", "Тёмный стиль редактора VS Code (#1E1E1E)"),
    LIGHT("Светлая (VS Code Light+)", "Светлый строгий стиль VS Code (#F3F3F3)")
}

object ThemePreferences {

    private const val PREFS_NAME = "smtu_theme_prefs"
    private const val KEY_THEME_MODE = "app_theme_mode"
    private const val KEY_AUTO_REFRESH = "app_auto_refresh_on_launch"

    private val _themeModeState = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeModeState: StateFlow<AppThemeMode> = _themeModeState.asStateFlow()

    private val _autoRefreshState = MutableStateFlow(true)
    val autoRefreshState: StateFlow<Boolean> = _autoRefreshState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val mode = try {
            AppThemeMode.valueOf(savedName ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
        _themeModeState.value = mode
        _autoRefreshState.value = prefs.getBoolean(KEY_AUTO_REFRESH, true)
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        _themeModeState.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun isAutoRefreshEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_REFRESH, true)
    }

    fun setAutoRefreshEnabled(context: Context, enabled: Boolean) {
        _autoRefreshState.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply()
    }
}
