package app.ytdlclean.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ytdlclean.data.DownloadManager
import app.ytdlclean.domain.DownloadStatus
import app.ytdlclean.domain.DownloadTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(private val manager: DownloadManager) : ViewModel() {

    val tasks: StateFlow<List<DownloadTask>> = manager.repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(id: String) = manager.cancel(id)
    fun retry(id: String) = manager.retry(id)
    fun remove(id: String) = manager.remove(id)
    fun clearFinished() = manager.clearFinished()

    fun open(task: DownloadTask, context: android.content.Context) {
        val path = task.filePath ?: return
        runCatching {
            val uri = if (path.startsWith("content://")) android.net.Uri.parse(path)
            else android.net.Uri.parse("file://$path")
            val mime = if (task.type == app.ytdlclean.domain.DownloadType.AUDIO) "audio/*" else "video/*"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Open with"))
        }
    }

    val hasFinished: Boolean
        get() = tasks.value.any { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELED }
}
