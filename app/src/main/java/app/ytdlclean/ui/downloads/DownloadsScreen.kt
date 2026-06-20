@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.ytdlclean.ui.downloads

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytdlclean.domain.DownloadStatus
import app.ytdlclean.domain.DownloadTask
import app.ytdlclean.domain.DownloadType
import coil.compose.AsyncImage

@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    // Hoisted out of the row callback: LocalContext.current is @Composable and
    // cannot be read inside the non-composable () -> Unit lambdas below.
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "Your downloads",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (viewModel.hasFinished) {
                TextButton(onClick = viewModel::clearFinished) { Text("Clear") }
            }
        }

        if (tasks.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
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
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 40.dp)
                        .clip(RoundedCornerShape(8.dp)),
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
                            if (task.type == DownloadType.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                Spacer(Modifier.width(8.dp))
                RowActions(task, onCancel, onRetry, onRemove, onOpen)
            }

            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.QUEUED) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (task.status == DownloadStatus.QUEUED) 0f else task.progress },
                    modifier = Modifier.fillMaxWidth(),
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
            IconButton(onClick = onCancel) { Icon(Icons.Outlined.Cancel, "Cancel") }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove") }
        }
        DownloadStatus.FAILED, DownloadStatus.CANCELED -> Row {
            IconButton(onClick = onRetry) { Icon(Icons.Outlined.Refresh, "Retry") }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove") }
        }
        DownloadStatus.COMPLETED -> Row {
            IconButton(onClick = onOpen) { Icon(Icons.Outlined.OpenInNew, "Open") }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove") }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Movie,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(12.dp))
            Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
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
