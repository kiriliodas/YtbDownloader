package app.ytdlclean.data.downloader

/** Parses human-friendly speed/ETA fragments out of yt-dlp's progress lines. */
object ProgressParser {

    private val SPEED = Regex("""(\d+\.?\d*)(KiB|MiB|GiB)/s""")
    private val ETA = Regex("""ETA\s+(\d{1,2}):(\d{2})""")

    fun extractSpeed(line: String?): String? {
        if (line.isNullOrBlank()) return null
        val m = SPEED.find(line) ?: return null
        val value = m.groupValues[1].toDouble()
        val unit = m.groupValues[2]
        return "%.1f %s/s".format(value, unit)
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
