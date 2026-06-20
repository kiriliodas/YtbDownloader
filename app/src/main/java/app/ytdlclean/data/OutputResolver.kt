package app.ytdlclean.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

/**
 * Resolves where downloads land and (on API 29+) registers them with the system
 * MediaStore so they show up in the user's Downloads/Music folder and gallery.
 *
 * On Android 10+ scoped storage, apps can't freely write to public folders by path,
 * so we: (1) let yt-dlp write to an app-private dir, then (2) "publish" the file via
 * MediaStore to a public collection. On API <= 28 we can write directly.
 */
class OutputResolver(private val context: Context) {

    /** App-private scratch dir yt-dlp writes into (no permissions needed). */
    fun scratchDir(): File {
        val dir = File(context.getExternalFilesDir(null), "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Move a scratch file into the public Downloads (video) or Music (audio) collection. */
    fun publish(file: File, isAudio: Boolean): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return publishViaMediaStore(file, isAudio)
        }
        return publishLegacy(file, isAudio)
    }

    private fun publishViaMediaStore(file: File, isAudio: Boolean): Uri? {
        val (collection, relativeRoot) = if (isAudio) {
            Pair(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MUSIC)
        } else {
            Pair(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES)
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(file.extension, isAudio))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$relativeRoot/YtdlClean")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { it.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        file.delete() // scratch copy no longer needed
        return uri
    }

    @Suppress("DEPRECATION")
    private fun publishLegacy(file: File, isAudio: Boolean): Uri? {
        val rootName = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val target = File(File(Environment.getExternalStoragePublicDirectory(rootName), "YtdlClean").apply { mkdirs() }, file.name)
        file.copyTo(target, overwrite = true)
        file.delete()
        return Uri.fromFile(target)
    }

    private fun mimeFor(ext: String, isAudio: Boolean): String = when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        else -> if (isAudio) "audio/*" else "video/*"
    }
}
