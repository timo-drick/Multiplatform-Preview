package de.drick.compose.hotpreview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(project: Project, file: VirtualFile) {
    var previewList: List<HotPreviewData> by remember { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val projectAnalyzer = remember {
        ProjectAnalyzer(project)
    }
    suspend fun render() {
        val fileClass = projectAnalyzer.loadFileClass(file)
        previewList = renderPreviewForClass(fileClass)
    }
    fun refresh() {
        scope.launch(Dispatchers.Default) {
            runCatchingCancellationAware {
                projectAnalyzer.executeGradleTask(file)
                render()
            }.onFailure { err ->
                err.printStackTrace()
            }
        }
    }
    LaunchedEffect(Unit) {
        runCatchingCancellationAware {
            render()
            refresh()
        }.onFailure { err ->
            err.printStackTrace()
        }
    }

    HotPreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                PreviewGridPanel(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    hotPreviewList = previewList
                )
            }
        }
    }
}