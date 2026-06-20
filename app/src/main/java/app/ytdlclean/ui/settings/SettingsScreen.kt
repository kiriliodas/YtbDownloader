@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.ytdlclean.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ytdlclean.domain.DownloadType
import app.ytdlclean.ui.theme.ThemeMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(20.dp))

        Section("Appearance") {
            LabelledRow(title = "Theme") {
                val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                val labels = listOf("System", "Light", "Dark")
                SingleChoiceSegmentedButtonRow {
                    modes.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        ) { Text(labels[i]) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LabelledRow(
                title = "Dynamic colors",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "Palette from your wallpaper" else "Requires Android 12+",
            ) {
                Switch(
                    checked = state.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Section("Defaults") {
            LabelledRow(title = "Default type") {
                val types = listOf(DownloadType.VIDEO, DownloadType.AUDIO)
                SingleChoiceSegmentedButtonRow {
                    types.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = state.defaultType == t,
                            onClick = { viewModel.setDefaultType(t) },
                            shape = SegmentedButtonDefaults.itemShape(i, types.size),
                        ) { Text(if (t == DownloadType.VIDEO) "Video" else "Audio") }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            QualityPicker(
                selected = state.preferredHeight,
                onSelect = viewModel::setPreferredHeight,
            )
        }

        Spacer(Modifier.height(20.dp))

        Section("About") {
            Text("YtdlClean", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("A clean YouTube & video downloader powered by yt-dlp.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "For personal use only (your own uploads, Creative Commons, offline viewing). " +
                    "Respect YouTube's Terms of Service and your local copyright laws.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun LabelledRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun QualityPicker(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(0 to "Best", 2160 to "2160p", 1440 to "1440p", 1080 to "1080p", 720 to "720p", 480 to "480p")
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected } ?: options.first()

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Preferred quality", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("Used when a link is added without picking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            TextButton(onClick = { expanded = true }) { Text(current.second) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onSelect(value); expanded = false },
                    )
                }
            }
        }
    }
}
