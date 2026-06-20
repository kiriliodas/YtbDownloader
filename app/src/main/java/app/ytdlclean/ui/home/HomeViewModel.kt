package app.ytdlclean.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ytdlclean.data.DownloadManager
import app.ytdlclean.domain.DownloadType
import app.ytdlclean.domain.Format
import app.ytdlclean.domain.VideoInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val url: String = "",
    val analyzing: Boolean = false,
    val info: VideoInfo? = null,
    val error: String? = null,
    val type: DownloadType = DownloadType.VIDEO,
    val selectedFormat: Format? = null, // null => auto best
)

sealed interface HomeEvent {
    data object Started : HomeEvent
}

class HomeViewModel(private val manager: DownloadManager) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(type = manager.settings.defaultType.value))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onUrlChange(url: String) {
        _state.update { it.copy(url = url, error = null) }
    }

    fun onTypeChange(type: DownloadType) {
        _state.update { it.copy(type = type, selectedFormat = null) }
    }

    fun onSelectFormat(format: Format?) {
        _state.update { it.copy(selectedFormat = format) }
    }

    fun dismissInfo() {
        _state.update { it.copy(info = null, error = null, selectedFormat = null) }
    }

    fun loadSharedUrl(url: String) {
        if (url.isNotBlank() && _state.value.url.isBlank()) {
            _state.update { it.copy(url = url) }
            analyze()
        }
    }

    fun analyze() {
        val url = _state.value.url.trim()
        if (url.isBlank()) {
            _state.update { it.copy(error = "Please paste a video URL first.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(analyzing = true, error = null, info = null) }
            runCatching { manager.analyze(url) }
                .onSuccess { info ->
                    _state.update {
                        it.copy(
                            analyzing = false,
                            info = info,
                            type = manager.settings.defaultType.value,
                            selectedFormat = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(analyzing = false, error = friendlyError(e))
                    }
                }
        }
    }

    fun startDownload() {
        val s = _state.value
        val info = s.info ?: return
        val task = manager.newTask(info, s.type, s.selectedFormat)
        manager.enqueue(task)
        _state.update {
            it.copy(url = "", info = null, selectedFormat = null)
        }
        viewModelScope.launch { _events.send(HomeEvent.Started) }
    }

    private fun friendlyError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("Unsupported URL", true) -> "That URL isn't supported by yt-dlp."
            msg.contains("Video unavailable", true) -> "This video is unavailable/private."
            else -> msg.ifBlank { "Could not analyze this link." }
        }
    }
}
