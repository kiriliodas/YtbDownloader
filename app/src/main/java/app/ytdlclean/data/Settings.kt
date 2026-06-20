package app.ytdlclean.data

import android.content.Context
import app.ytdlclean.domain.DownloadType
import app.ytdlclean.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed settings, exposed as reactive [StateFlow]s so the UI
 * (e.g. theme) updates live without an app restart.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("ytdlclean_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readTheme())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _defaultType = MutableStateFlow(readType())
    val defaultType: StateFlow<DownloadType> = _defaultType.asStateFlow()

    private val _preferredHeight = MutableStateFlow(prefs.getInt(KEY_HEIGHT, 0))
    val preferredHeight: StateFlow<Int> = _preferredHeight.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setDefaultType(type: DownloadType) {
        prefs.edit().putString(KEY_TYPE, type.name).apply()
        _defaultType.value = type
    }

    fun setPreferredHeight(height: Int) {
        prefs.edit().putInt(KEY_HEIGHT, height).apply()
        _preferredHeight.value = height
    }

    private fun readTheme() =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    private fun readType() =
        runCatching { DownloadType.valueOf(prefs.getString(KEY_TYPE, DownloadType.VIDEO.name)!!) }
            .getOrDefault(DownloadType.VIDEO)

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_TYPE = "default_type"
        const val KEY_HEIGHT = "preferred_height"
        const val KEY_DYNAMIC = "dynamic_color"
    }
}
