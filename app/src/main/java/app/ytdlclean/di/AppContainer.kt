package app.ytdlclean.di

import android.app.Application
import android.content.Context
import android.util.Log
import app.ytdlclean.data.DownloadManager
import app.ytdlclean.data.DownloadRepository
import app.ytdlclean.data.OutputResolver
import app.ytdlclean.data.Settings
import app.ytdlclean.data.downloader.YoutubedlAndroidDownloader
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Lightweight manual DI. Google's recommended approach for apps without complex
 * dependency graphs — no Hilt/KSP needed, so the build stays fast and simple.
 */
object AppContainer {

    private const val TAG = "AppContainer"
    @Volatile private var instance: DownloadManager? = null
    private val ready = CompletableDeferred<Unit>()

    /**
     * Kick off yt-dlp runtime init in the background (avoids an ANR on cold start).
     * FFmpeg must also be initialized for audio extraction (`-x`) and DASH merging.
     */
    fun init(app: Application) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
            }.onFailure { Log.e(TAG, "yt-dlp init failed", it) }
            ready.complete(Unit)
        }
    }

    fun get(context: Context): DownloadManager {
        instance?.let { return it }
        synchronized(this) {
            instance?.let { return it }
            val app = context.applicationContext
            val mgr = DownloadManager(
                appContext = app,
                downloader = YoutubedlAndroidDownloader(ready),
                repository = DownloadRepository(),
                settings = Settings(app),
                output = OutputResolver(app),
            )
            instance = mgr
            return mgr
        }
    }
}
