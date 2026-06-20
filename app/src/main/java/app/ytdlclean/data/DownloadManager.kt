package app.ytdlclean.data

import android.content.Context
import app.ytdlclean.data.downloader.DownloadProgress
import app.ytdlclean.data.downloader.DownloadResult
import app.ytdlclean.data.downloader.Event
import app.ytdlclean.data.downloader.VideoDownloader
import app.ytdlclean.domain.DownloadStatus
import app.ytdlclean.domain.DownloadTask
import app.ytdlclean.domain.VideoInfo
import app.ytdlclean.service.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates the whole pipeline: analyze → enqueue → run (sequentially) → publish
 * to MediaStore. Holds a long-lived scope and keeps the foreground service alive
 * while any task is active. ViewModels talk only to this class.
 */
class DownloadManager(
    private val appContext: Context,
    private val downloader: VideoDownloader,
    val repository: DownloadRepository,
    val settings: Settings,
    private val output: OutputResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = mutableSetOf<String>()
    private val runLock = Mutex()

    private val _analyzing = MutableStateFlow(false)
    val analyzing: StateFlow<Boolean> = _analyzing.asStateFlow()

    // ---- Analysis ----

    suspend fun analyze(url: String): VideoInfo {
        _analyzing.value = true
        return try {
            downloader.fetchInfo(url)
        } finally {
            _analyzing.value = false
        }
    }

    // ---- Enqueue ----

    /** Build a [DownloadTask] ready for [enqueue]. Centralises path/ext logic. */
    fun newTask(
        info: VideoInfo,
        type: app.ytdlclean.domain.DownloadType,
        chosenFormat: app.ytdlclean.domain.Format?,
    ): DownloadTask {
        val (formatArg, ext) = when (type) {
            app.ytdlclean.domain.DownloadType.AUDIO -> null to "mp3"
            app.ytdlclean.domain.DownloadType.VIDEO -> {
                if (chosenFormat == null) {
                    null to "" // auto: let yt-dlp pick, accept any extension
                } else {
                    // For video-only DASH streams, mux in best audio via ffmpeg.
                    val selector = if (chosenFormat.hasBoth) chosenFormat.formatId
                    else "${chosenFormat.formatId}+bestaudio/best"
                    selector to chosenFormat.ext.ifBlank { "mp4" }
                }
            }
        }
        return DownloadTask(
            id = java.util.UUID.randomUUID().toString(),
            url = info.webpageUrl,
            title = info.title,
            thumbnailUrl = info.thumbnailUrl,
            type = type,
            formatArg = formatArg,
            ext = ext,
            outputDir = output.scratchDir().absolutePath,
        )
    }

    fun enqueue(task: DownloadTask) {
        repository.upsert(task)
        ensureServiceRunning()
        runNext()
    }

    // ---- Control ----

    fun cancel(id: String) {
        downloader.cancel(id)
        repository.update(id) {
            it.copy(status = DownloadStatus.CANCELED, progress = it.progress)
        }
    }

    fun retry(id: String) {
        val task = repository.get(id) ?: return
        repository.update(id) {
            it.copy(status = DownloadStatus.QUEUED, progress = 0f, error = null, filePath = null)
        }
        ensureServiceRunning()
        runNext()
    }

    fun remove(id: String) {
        cancel(id)
        repository.remove(id)
    }

    fun clearFinished() {
        repository.clearFinished()
    }

    // ---- Execution loop ----

    private fun runNext() {
        scope.launch {
            runLock.withLock {
                if (running.isNotEmpty()) return@withLock // already pumping the queue
                val next = repository.tasks.value.firstOrNull {
                    it.status == DownloadStatus.QUEUED && it.id !in running
                } ?: return@withLock
                running.add(next.id)
                execute(next)
            }
        }
    }

    private suspend fun execute(task: DownloadTask) {
        try {
            repository.update(task.id) { it.copy(status = DownloadStatus.DOWNLOADING) }
            downloader.download(task).collect { event ->
                when (event) {
                    is Event.Progress -> applyProgress(event.data)
                    is Event.Done -> handleDone(event.result)
                }
            }
        } finally {
            running.remove(task.id)
            // Pump the queue for any remaining queued tasks.
            val more = repository.tasks.value.any { it.status == DownloadStatus.QUEUED }
            if (more) {
                runNext()
            } else if (repository.activeCount == 0) {
                stopService()
            }
        }
    }

    private fun applyProgress(p: DownloadProgress) {
        repository.update(p.taskId) {
            it.copy(
                status = DownloadStatus.DOWNLOADING,
                progress = p.progress,
                speedText = p.speedText,
                etaText = p.etaText,
            )
        }
    }

    private suspend fun handleDone(result: DownloadResult) {
        when (result) {
            is DownloadResult.Success -> {
                val published = runCatching {
                    output.publish(java.io.File(result.filePath), isAudio = repository.get(result.taskId)?.type == app.ytdlclean.domain.DownloadType.AUDIO)
                }.getOrNull()
                repository.update(result.taskId) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1f,
                        filePath = published?.toString() ?: result.filePath,
                        speedText = null,
                        etaText = null,
                    )
                }
            }
            is DownloadResult.Failure -> {
                repository.update(result.taskId) {
                    it.copy(status = DownloadStatus.FAILED, error = result.error, speedText = null, etaText = null)
                }
            }
            is DownloadResult.Canceled -> {
                repository.update(result.taskId) {
                    it.copy(status = DownloadStatus.CANCELED, speedText = null, etaText = null)
                }
            }
        }
    }

    private fun ensureServiceRunning() {
        runCatching { DownloadService.start(appContext) }
    }

    private fun stopService() {
        runCatching { DownloadService.stop(appContext) }
    }
}
