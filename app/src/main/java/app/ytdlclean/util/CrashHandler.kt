package app.ytdlclean.util

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures uncaught exceptions (the thing that crashes the app), writes the full
 * stack trace to a file, then lets the app die. On the next launch,
 * [MainActivity] detects the file and shows it so you can share the report
 * without needing ADB / logcat / Android Studio.
 *
 * This is a debug aid — safe to ship. It only writes when a crash actually happens.
 */
object CrashHandler {

    private const val CRASH_FILE = "last_crash.txt"

    /** Install the global handler. Call once from Application.onCreate(). */
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveReport(context, thread, throwable)
            } catch (_: Throwable) {
                // best-effort; never throw from here
            }
            // Defer to the platform's normal crash handling so the process still dies.
            previous?.uncaughtException(thread, throwable)
            Process.killProcess(Process.myPid())
        }
    }

    private fun saveReport(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val report = buildString {
            append("=== YtdlClean Crash Report ===\n")
            append("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            append("Thread: ${thread.name}\n")
            append("Device: ${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("App version: ${try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Throwable) { "?" }}\n")
            append("\n--- Stack trace ---\n")
            append(sw.toString())
        }
        // filesDir is always writable, no permissions needed.
        File(context.filesDir, CRASH_FILE).writeText(report)
    }

    /** The text of the last crash, or null if there was none. */
    fun loadReport(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE)
        return if (file.exists()) file.readText() else null
    }

    /** Delete the saved report (call once the user has seen/shared it). */
    fun clearReport(context: Context) {
        File(context.filesDir, CRASH_FILE).delete()
    }
}
