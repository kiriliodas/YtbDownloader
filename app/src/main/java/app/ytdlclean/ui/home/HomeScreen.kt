@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.ytdlclean.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import app.ytdlclean.domain.DownloadType
import app.ytdlclean.domain.Format
import app.ytdlclean.domain.VideoInfo
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    initialSharedUrl: String?,
    onNavigateLibrary: () -> Unit,
) {
    val state = viewModel.state.value

    // Hoisted out of the paste button's onClick: LocalClipboardManager.current is
    // @Composable and can't be read inside the non-composable onClick lambda.
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(initialSharedUrl) {
        initialSharedUrl?.let { viewModel.loadSharedUrl(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is HomeEvent.Started) onNavigateLibrary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Header(title = "Download", subtitle = "Paste any video link to begin")

        Spacer(Modifier.height(16.dp))

        // --- URL input ---
        OutlinedTextField(
            value = state.url,
            onValueChange = viewModel::onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Video URL") },
            placeholder = { Text("https://youtu.be/…") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = {
                    val text = clipboardManager.getText()?.text
                    if (text != null) viewModel.onUrlChange(text)
                }) { Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste") }
            },
            shape = RoundedCornerShape(16.dp),
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = viewModel::analyze,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !state.analyzing && state.url.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.analyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Text("Analyzing…")
            } else {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze")
            }
        }

        state.error?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(
                err,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.info?.let { info ->
            Spacer(Modifier.height(20.dp))
            FormatCard(
                info = info,
                state = state,
                onTypeChange = viewModel::onTypeChange,
                onSelectFormat = viewModel::onSelectFormat,
                onDownload = viewModel::startDownload,
            )
        }

        if (state.info == null && !state.analyzing) {
            Spacer(Modifier.height(24.dp))
            HintCard()
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FormatCard(
    info: VideoInfo,
    state: HomeUiState,
    onTypeChange: (DownloadType) -> Unit,
    onSelectFormat: (Format?) -> Unit,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                info.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 96.dp, height = 56.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        info.uploader,
                        info.durationSeconds?.let { formatDuration(it) },
                    ).joinToString("  ·  ")
                    if (meta.isNotBlank()) {
                        Text(meta, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Type toggle
            val types = listOf(DownloadType.VIDEO, DownloadType.AUDIO)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    ) {
                        Text(if (type == DownloadType.VIDEO) "Video" else "Audio")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.type == DownloadType.VIDEO) {
                Text("Quality", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                QualityRow(
                    title = "Auto",
                    subtitle = "Recommended · best available",
                    selected = state.selectedFormat == null,
                    onSelect = { onSelectFormat(null) },
                )
                info.qualityOptions.take(8).forEach { f ->
                    QualityRow(
                        title = "${f.height ?: 0}p",
                        subtitle = f.label(),
                        selected = state.selectedFormat?.formatId == f.formatId,
                        onSelect = { onSelectFormat(f) },
                    )
                }
            } else {
                Text(
                    "Extracts the audio track and converts it to MP3 (320 kbps).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.type == DownloadType.VIDEO) "Download video" else "Extract audio")
            }
        }
    }
}

@Composable
private fun QualityRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "1. Paste a link\n2. Pick video or audio\n3. Choose a quality\n4. Download — it runs in the background",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
