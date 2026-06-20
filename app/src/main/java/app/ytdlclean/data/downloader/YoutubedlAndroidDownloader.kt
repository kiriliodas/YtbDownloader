package app.ytdlclean.data.downloader

import app.ytdlclean.domain.DownloadType
import app.ytdlclean.domain.Format
import app.ytdlclean.domain.VideoInfo
import app.ytdlclean.domain.DownloadTask
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * [VideoDownloader] backed by the youtubedl-android library (bundles yt-dlp + ffmpeg).
 *
 * Why raw JSON parsing instead of `YoutubeDL.getInfo(...)`? The library's parsed
 * [VideoInfo] field accessors change across versions; parsing `--dump-json` output
 * ourselves is stable and gives us every format field yt-dlp exposes.
 */
class YoutubedlAndroidDownloader(
    private val ready: Deferred<Unit> = CompletableDeferred(Unit),
) : VideoDownloader {

    private companion object {
        const val OUTPUT_TEMPLATE = "%(title).100B.%(ext)s"
    }

    override suspend fun fetchInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        ready.await() // ensure yt-dlp runtime is initialized
        val request = YoutubeDLRequest(url).apply {
            addOption("-j")              // --dump-json
            addOption("--no-warnings")
            addOption("--no-playlist")   // analyse the single video even if part of a playlist
        }
        val response = runCatching {
            YoutubeDL.getInstance().execute(request, processId("info")) { _, _, _ -> }
        }.getOrElse { throw DownloadException("Could not analyze URL: ${it.message}", it) }

        val firstJson = response.out.lineSequence().firstOrNull { it.isNotBlank() }
            ?: throw DownloadException("Empty response from yt-dlp")

        val json = JSONObject(firstJson)

        val isPlaylist = json.opt("_type") == "playlist"
        val formats = parseFormats(json.optJSONArray("formats"))
        val title = json.optString("title").ifBlank { json.optString("id").ifBlank { "Video" } }

        VideoInfo(
            id = json.optString("id"),
            title = title,
            uploader = json.optString("uploader").ifBlank { null },
            thumbnailUrl = json.optString("thumbnail").ifBlank { null },
            durationSeconds = json.optLong("duration").takeIf { it > 0 },
            webpageUrl = json.optString("webpage_url").ifBlank { url },
            formats = formats,
            isPlaylist = isPlaylist,
            playlistCount = if (isPlaylist) json.optJSONArray("entries")?.length() else null,
        )
    }

    override fun download(task: DownloadTask) = callbackFlow {
        val request = YoutubeDLRequest(task.url).apply {
            addOption("-o", File(task.outputDir, OUTPUT_TEMPLATE).absolutePath)
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("--newline")
            when (task.type) {
                DownloadType.AUDIO -> {
                    // Extract audio + transcode to the requested container via ffmpeg
                    addOption("-x")
                    addOption("--audio-format", audioContainer(task.ext))
                    addOption("--audio-quality", "0")
                }
                DownloadType.VIDEO -> {
                    if (task.formatArg != null) {
                        addOption("-f", task.formatArg)
                        addOption("--merge-output-format", "mp4")
                    }
                    // null formatArg => yt-dlp's default "best*" selection
                }
            }
        }

        val job = launch(Dispatchers.IO) {
            try {
                val result = YoutubeDL.getInstance().execute(request, task.id) { progress, eta, line ->
                    val speed = ProgressParser.extractSpeed(line)
                    val etaText = eta.takeIf { it > 0 }?.let { ProgressParser.formatEta(it) }
                    trySend(
                        Event.Progress(
                            DownloadProgress(
                                taskId = task.id,
                                progress = (progress / 100f).coerceIn(0f, 1f),
                                speedText = speed,
                                etaText = etaText,
                                consoleLine = line,
                            )
                        )
                    )
                }
                // yt-dlp returned without throwing => resolve the produced file.
                trySend(finish(task, result.out))
            } catch (e: YoutubeDLException) {
                trySend(canceledOrFailed(task, e.message ?: "yt-dlp error"))
            } catch (e: CancellationException) {
                trySend(Event.Done(DownloadResult.Canceled(task.id)))
                throw e
            } catch (e: Throwable) {
                trySend(canceledOrFailed(task, e.message ?: "Unknown error"))
            } finally {
                close()
            }
        }

        awaitClose {
            // Flow cancelled (e.g. user tapped Cancel) -> stop the underlying yt-dlp process
            YoutubeDL.getInstance().destroyProcessById(task.id)
            job.cancel()
        }
    }.flowOn(Dispatchers.IO)

    override fun cancel(taskId: String) {
        cancelledIds.add(taskId)
        runCatching { YoutubeDL.getInstance().destroyProcessById(taskId) }
    }

    // ---- helpers ----

    private val cancelledIds = java.util.Collections.synchronizedSet(HashSet<String>())

    private fun finish(task: DownloadTask, stdout: String): Event {
        if (task.id in cancelledIds) {
            cancelledIds.remove(task.id)
            return Event.Done(DownloadResult.Canceled(task.id))
        }
        val file = resolveOutputFile(task.outputDir, task.ext, stdout)
        return if (file != null) {
            Event.Done(DownloadResult.Success(task.id, file.absolutePath))
        } else {
            Event.Done(DownloadResult.Failure(task.id, "Download finished but the output file could not be located"))
        }
    }

    private fun canceledOrFailed(task: DownloadTask, message: String): Event.Done {
        return if (task.id in cancelledIds) {
            cancelledIds.remove(task.id)
            Event.Done(DownloadResult.Canceled(task.id))
        } else {
            Event.Done(DownloadResult.Failure(task.id, message))
        }
    }

    private fun processId(prefix: String) = "$prefix-${System.nanoTime()}"

    private fun audioContainer(ext: String): String = when (ext.lowercase()) {
        "m4a" -> "m4a"
        "aac" -> "aac"
        "wav" -> "wav"
        "flac" -> "flac"
        else -> "mp3"
    }

    /** After a successful run, find the file yt-dlp just wrote. */
    private fun resolveOutputFile(outputDir: String, ext: String, stdout: String): File? {
        // Best signal: yt-dlp prints "[download] Destination: <path>" / final move lines.
        val fromStdout = stdout.lineSequence()
            .mapNotNull { line ->
                line.substringAfter("Destination: ", "").trim().takeIf { it.startsWith("/") }
            }.lastOrNull { File(it).exists() }
        if (fromStdout != null) return File(fromStdout)

        // Fallback: newest file in the output dir (filter by ext when known).
        val target = File(outputDir)
        return target.listFiles { f -> f.isFile && (ext.isBlank() || f.extension.equals(ext, true)) }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun parseFormats(arr: org.json.JSONArray?): List<Format> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val vcodec = o.optString("vcodec", "none")
            val acodec = o.optString("acodec", "none")
            // Skip non-asset pseudo-formats (storyboard images, etc.)
            if (vcodec == "none" && acodec == "none") return@mapNotNull null
            Format(
                formatId = o.optString("format_id"),
                ext = o.optString("ext", "mp4"),
                height = o.optInt("height").takeIf { it > 0 },
                width = o.optInt("width").takeIf { it > 0 },
                fps = o.optDouble("fps").takeIf { it > 0 }?.toInt(),
                vcodec = vcodec,
                acodec = acodec,
                filesizeBytes = (o.optLong("filesize").takeIf { it > 0 }
                    ?: o.optLong("filesize_approx").takeIf { it > 0 }),
                tbr = o.optDouble("tbr").takeIf { it > 0 },
            )
        }
    }
}

class DownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)
