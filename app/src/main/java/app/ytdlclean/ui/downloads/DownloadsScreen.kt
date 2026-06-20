@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.ytdlclean.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Download
import compose.icons.fontawesomeicons.solid.ExternalLinkAlt
import compose.icons.fontawesomeicons.solid.Film
import compose.icons.fontawesomeicons.solid.Music
import compose.icons.fontawesomeicons.solid.Redo
import compose.icons.fontawesomeicons.solid.Times
import compose.icons.fontawesomeicons.solid.Trash
import app.ytdlclean.domain.DownloadStatus
import app.ytdlclean.domain.DownloadTask
import app.ytdlclean.domain.DownloadType
import coil.compose.AsyncImage

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        // ── Gradient header ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Your downloads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                if (viewModel.hasFinished) {
                    TextButton(
                        onClick = viewModel::clearFinished,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                        ),
                    ) { Text("Clear") }
                }
            }
        }

        if (tasks.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    DownloadRow(
                        task = task,
                        onCancel = { viewModel.cancel(task.id) },
                        onRetry = { viewModel.retry(task.id) },
                        onRemove = { viewModel.remove(task.id) },
                        onOpen = { viewModel.open(task, context) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (task.thumbnailUrl != null) {
                        AsyncImage(
                            model = task.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            if (task.type == DownloadType.AUDIO) FontAwesomeIcons.Solid.Music else FontAwesomeIcons.Solid.Film,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        statusLine(task),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor(task),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(4.dp))
                RowActions(task, onCancel, onRetry, onRemove, onOpen)
            }

            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.QUEUED) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (task.status == DownloadStatus.QUEUED) 0f else task.progress },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun RowActions(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    when (task.status) {
        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> Row {
            IconButton(onClick = onCancel) { Icon(FontAwesomeIcons.Solid.Times, "Cancel", modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onRemove) { Icon(FontAwesomeIcons.Solid.Trash, "Remove", modifier = Modifier.size(18.dp)) }
        }
        DownloadStatus.FAILED, DownloadStatus.CANCELED -> Row {
            IconButton(onClick = onRetry) { Icon(FontAwesomeIcons.Solid.Redo, "Retry", modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onRemove) { Icon(FontAwesomeIcons.Solid.Trash, "Remove", modifier = Modifier.size(18.dp)) }
        }
        DownloadStatus.COMPLETED -> Row {
            IconButton(onClick = onOpen) { Icon(FontAwesomeIcons.Solid.ExternalLinkAlt, "Open", modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onRemove) { Icon(FontAwesomeIcons.Solid.Trash, "Remove", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                FontAwesomeIcons.Solid.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("No downloads yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Paste a link on the Download tab to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusLine(task: DownloadTask): String = when (task.status) {
    DownloadStatus.QUEUED -> "Queued…"
    DownloadStatus.DOWNLOADING -> buildString {
        append("${(task.progress * 100).toInt()}%")
        task.speedText?.let { append(" · $it") }
        task.etaText?.let { append(" · ETA $it") }
    }
    DownloadStatus.COMPLETED -> "Saved"
    DownloadStatus.FAILED -> task.error ?: "Failed"
    DownloadStatus.CANCELED -> "Canceled"
}

@Composable
private fun statusColor(task: DownloadTask) = when (task.status) {
    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
    DownloadStatus.CANCELED -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
