package app.ytdlclean.domain

/** What the user wants out of a given URL. */
enum class DownloadType { VIDEO, AUDIO }

/** Lifecycle of a [DownloadTask]. */
enum class DownloadStatus {
    QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELED
}

/**
 * A single available stream returned by yt-dlp during analysis.
 * Mirrors the relevant fields of yt-dlp's `--list-formats` output.
 */
data class Format(
    val formatId: String,
    val ext: String,
    val height: Int? = null,
    val width: Int? = null,
    val fps: Int? = null,
    val vcodec: String,
    val acodec: String,
    val filesizeBytes: Long? = null,
    val tbr: Double? = null, // total bitrate in Kbps
) {
    val isAudioOnly: Boolean get() = vcodec == "none" && acodec != "none"
    val isVideoOnly: Boolean get() = vcodec != "none" && acodec == "none"
    val hasBoth: Boolean get() = vcodec != "none" && acodec != "none"

    /** Human label e.g. "1080p60 • mp4 • 12.4 MB". */
    fun label(): String = buildString {
        height?.let { append("${it}p") }
        fps?.takeIf { it > 30 }?.let { append(it) }
        if (height == null) append("audio")
        ext.takeIf { it.isNotBlank() }?.let { append(" • $it") }
        tbr?.let { append(" • ${it.toInt()} kbps") }
        filesizeBytes?.let { append(" • ${formatBytes(it)}") }
    }.trim().trimEnd('•', ' ')

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}

/** Parsed metadata for a URL, used to drive the format picker. */
data class VideoInfo(
    val id: String,
    val title: String,
    val uploader: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long? = null,
    val webpageUrl: String,
    val formats: List<Format>,
    val isPlaylist: Boolean = false,
    val playlistCount: Int? = null,
) {
    val bestVideoFormats: List<Format>
        get() = formats.filter { it.hasBoth || it.isVideoOnly }
            .sortedByDescending { it.height ?: 0 }

    /** One representative format per resolution, best bitrate first, audio+video preferred. */
    val qualityOptions: List<Format>
        get() = formats
            .filter { it.hasBoth || it.isVideoOnly }
            .sortedWith(
                compareByDescending<Format> { it.height ?: 0 }
                    .thenByDescending { if (it.hasBoth) 1 else 0 }
                    .thenByDescending { it.tbr ?: 0.0 }
            )
            .distinctBy { it.height }
}
