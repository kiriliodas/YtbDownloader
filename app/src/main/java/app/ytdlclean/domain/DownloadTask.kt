package app.ytdlclean.domain

/**
 * A unit of work the download pipeline runs. Immutable snapshot; mutations
 * produce a new copy through the repository.
 */
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val type: DownloadType,
    val formatArg: String?,     // raw -f selector for yt-dlp (null => auto best)
    val ext: String,
    val outputDir: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,   // 0f..1f
    val speedText: String? = null,
    val etaText: String? = null,
    val filePath: String? = null,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isActive: Boolean
        get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING
}
