package app.ytdlclean

import android.app.Application
import android.util.Log
import app.ytdlclean.di.AppContainer
import app.ytdlclean.util.CrashHandler

class YtdlApp : Application() {
    override fun onCreate() {
        // Install FIRST, so even an early init failure gets captured.
        super.onCreate()
        try {
            CrashHandler.install(this)
        } catch (t: Throwable) {
            Log.e("YtdlApp", "Could not install crash handler", t)
        }
        // Initialize the yt-dlp backend (bundled via youtubedl-android) in the background.
        AppContainer.init(this)
    }
}
