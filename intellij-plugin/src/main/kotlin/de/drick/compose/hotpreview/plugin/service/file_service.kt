package de.drick.compose.hotpreview.plugin.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope

class FilePreviewService(
    private val project: Project,
    private val file: VirtualFile,
    private val scope: CoroutineScope
) {
    // Maybe this will be just what is in the viewmodel not sure yet.
}