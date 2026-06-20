package app.ytdlclean.data

import app.ytdlclean.domain.DownloadStatus
import app.ytdlclean.domain.DownloadTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for download tasks. In-memory for v1 (the running
 * [app.ytdlclean.service.DownloadService] keeps the process alive while work is
 * active). Swap the backing list for a Room database to persist across reboots.
 */
class DownloadRepository {

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    val activeCount: Int get() = _tasks.value.count { it.isActive }

    fun upsert(task: DownloadTask) {
        _tasks.update { list ->
            val idx = list.indexOfFirst { it.id == task.id }
            if (idx >= 0) list.toMutableList().apply { set(idx, task) }.toList()
            else listOf(task) + list // newest first
        }
    }

    fun get(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    fun remove(id: String) {
        _tasks.update { list -> list.filterNot { it.id == id } }
    }

    fun clearFinished() {
        _tasks.update { list -> list.filter { it.isActive } }
    }

    /** Patch a single task by id. Not inline so it can touch the private backing flow. */
    fun update(id: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.update { list ->
            list.map { if (it.id == id) block(it) else it }
        }
    }

    fun queuedTasks(): List<DownloadTask> =
        _tasks.value.filter { it.status == DownloadStatus.QUEUED }
}
