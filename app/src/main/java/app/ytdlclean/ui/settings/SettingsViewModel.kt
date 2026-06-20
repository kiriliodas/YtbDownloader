package app.ytdlclean.ui.settings

import androidx.lifecycle.ViewModel
import app.ytdlclean.data.DownloadManager
import app.ytdlclean.data.Settings
import app.ytdlclean.domain.DownloadType
import app.ytdlclean.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val defaultType: DownloadType,
    val preferredHeight: Int,
)

class SettingsViewModel(private val manager: DownloadManager) : ViewModel() {

    private val settings: Settings = manager.settings

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun snapshot() = SettingsUiState(
        themeMode = settings.themeMode.value,
        dynamicColor = settings.dynamicColor.value,
        defaultType = settings.defaultType.value,
        preferredHeight = settings.preferredHeight.value,
    )

    fun setTheme(mode: ThemeMode) {
        settings.setTheme(mode)
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        settings.setDynamicColor(enabled)
        _state.value = _state.value.copy(dynamicColor = enabled)
    }

    fun setDefaultType(type: DownloadType) {
        settings.setDefaultType(type)
        _state.value = _state.value.copy(defaultType = type)
    }

    fun setPreferredHeight(height: Int) {
        settings.setPreferredHeight(height)
        _state.value = _state.value.copy(preferredHeight = height)
    }
}
