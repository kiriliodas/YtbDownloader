package app.ytdlclean.data.downloader

import app.ytdlclean.domain.DownloadTask
import app.ytdlclean.domain.VideoInfo
import kotlinx.coroutines.flow.Flow

/** Emitted continuously while a download runs. */
data class DownloadProgress(
    val taskId: String,
    val progress: Float,   // 0f..1f
    val speedText: String?,
    val etaText: String?,
    val consoleLine: String?,
)

/** Result of a finished download. */
sealed interface DownloadResult {
    data class Success(val taskId: String, val filePath: String) : DownloadResult
    data class Failure(val taskId: String, val error: String) : DownloadResult
    data class Canceled(val taskId: String) : DownloadResult
}

/**
 * Backend-agnostic download engine. The current implementation wraps the
 * youtubedl-android library (which bundles yt-dlp). Swap this interface to
 * use NewPipe Extractor, Chaquopy, etc. without touching the UI or service.
 */
interface VideoDownloader {

    /** Resolve a URL into metadata + available formats. Throws on failure. */
    suspend fun fetchInfo(url: String): VideoInfo

    /** Run a task to completion, emitting progress then a terminal result. */
    fun download(task: DownloadTask): Flow<Event>

    /** Stop a running task. */
    fun cancel(taskId: String)
}

/** Union of progress ticks and terminal outcomes, for a clean single stream. */
sealed interface Event {
    data class Progress(val data: DownloadProgress) : Event
    data class Done(val result: DownloadResult) : Event
}
