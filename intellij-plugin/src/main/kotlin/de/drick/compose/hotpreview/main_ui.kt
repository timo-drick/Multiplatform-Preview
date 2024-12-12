package de.drick.compose.hotpreview

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        scope.launch {
            projectAnalyzer.executeGradleTask(file)
            render()
        }
    }
    LaunchedEffect(Unit) {
        render()
        refresh()
    }

    HotPreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                PreviewGridPanel(
                    modifier = Modifier.weight(1f),
                    hotPreviewList = previewList
                )
            }
        }
    }
}