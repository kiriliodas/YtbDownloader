package app.ytdlclean

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytdlclean.di.AppContainer
import app.ytdlclean.ui.crash.CrashReportScreen
import app.ytdlclean.ui.navigation.AppNavHost
import app.ytdlclean.ui.theme.ThemeMode
import app.ytdlclean.ui.theme.YtdlTheme
import app.ytdlclean.util.CrashHandler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge can throw on some OEM builds; don't let it kill the app.
        runCatching { enableEdgeToEdge() }
        val sharedUrl = extractSharedText(intent)
        setContent {
            val settings = AppContainer.get(applicationContext).settings
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Show the captured crash report (if any) before the main UI.
            var crashReport by remember { mutableStateOf(CrashHandler.loadReport(this@MainActivity)) }

            YtdlTheme(darkTheme = dark, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val report = crashReport
                    if (report != null) {
                        CrashReportScreen(
                            report = report,
                            onDismiss = { crashReport = null },
                        )
                    } else {
                        AppNavHost(initialSharedUrl = sharedUrl)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        // Pull just the URL out of shared text like "Title - https://youtu.be/…"
        return Regex("https?://\\S+").find(text)?.value
    }
}
