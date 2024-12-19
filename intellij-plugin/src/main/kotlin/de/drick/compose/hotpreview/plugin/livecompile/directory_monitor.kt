package de.drick.compose.hotpreview.plugin.livecompile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey

fun Flow<List<FileEvent>>.filterKotlinFiles() = mapNotNull { eventList ->
    eventList.filter { it.file.extension == "kt" }.ifEmpty { null }
}

/**
 * Monitors all file changes of the specified directory and all child directories.
 * Also monitor automatically new directories that are child directories of the dir.
 */
fun directoryMonitorFlow(dirList: List<File>): Flow<List<FileEvent>> = flow {
    val watcher = DirectoryWatcher(dirList)
    try {
        while (true) {
            val events = watcher.awaitEvent()
            if (!events.isNullOrEmpty()) emit(events)
        }
    } catch (err: CancellationException) {
        watcher.close()
    }
}

/**
 * Monitors all file changes of the specified directory and all child directories.
 * Also monitor automatically new directories that are child directories of the dir.
 */
fun directoryMonitorFlow(dir: File): Flow<List<FileEvent>> = flow {
    val watcher = DirectoryWatcher(listOf(dir))
    try {
        while (true) {
            val events = watcher.awaitEvent()
            if (!events.isNullOrEmpty()) emit(events)
        }
    } catch (err: CancellationException) {
        watcher.close()
    }
}

data class FileEvent(
    val file: File,
    val type: Type
) {
    enum class Type { INIT, MODIFIED, CREATED, DELETED }
}

private class DirectoryWatcher(dirList: List<File>) {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val initEvents: List<FileEvent>
    init {
        initEvents = dirList.map { dir ->
            dir.walkTopDown().filter {
                val isDirectory = it.isDirectory
                if (isDirectory) registerDir(it.toPath())
                isDirectory.not()
            }.map {
                FileEvent(it, FileEvent.Type.INIT)
            }.toList()
        }.flatten()
    }

    private fun registerDir(path: Path) {
        println("Register dir: $path")
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE
        )
    }

    private val lastChange = mutableMapOf<File, Long>()
    private var initEventsEmitted = false
    suspend fun awaitEvent(): List<FileEvent>? = withContext(Dispatchers.IO) {
        if (initEventsEmitted.not() && initEvents.isNotEmpty()) {
            initEventsEmitted = true
            return@withContext initEvents
        }
        val watchKey: WatchKey = watchService.take()
        val dirPath = watchKey.watchable() as? Path ?: return@withContext emptyList<FileEvent>()
        val events = watchKey.pollEvents()
            .mapNotNull { event ->
                val changedFile = dirPath.resolve(event.context() as Path)
                //println("Changed file: $changedFile event: ${event.kind()} context: ${event.context()} ${event.count()}")
                val file = changedFile.toFile()
                val type = when (event.kind()) {
                    StandardWatchEventKinds.ENTRY_DELETE -> FileEvent.Type.DELETED
                    StandardWatchEventKinds.ENTRY_MODIFY -> FileEvent.Type.MODIFIED
                    StandardWatchEventKinds.ENTRY_CREATE -> FileEvent.Type.CREATED
                    else -> null
                }
                when {
                    file.isDirectory && type == FileEvent.Type.CREATED -> {
                        registerDir(file.toPath())
                        null
                    }
                    type != null -> FileEvent(file, type)
                    else -> null
                }
            }.filter {
                // Avoid two events for one file change
                // File editors normally first change the content of the file and than change
                // the attribute last modified from the file
                it.file.lastModified() != lastChange[it.file]
            }
        if (watchKey.reset().not()) {
            watchKey.cancel()
            watchService.close()
            null
        } else {
            events.forEach {
                lastChange[it.file] = it.file.lastModified()
            }
            events
        }
    }
    fun close() {
        watchService.close()
    }
}
