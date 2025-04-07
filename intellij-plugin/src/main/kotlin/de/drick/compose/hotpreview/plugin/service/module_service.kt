package de.drick.compose.hotpreview.plugin.service

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import de.drick.compose.hotpreview.plugin.ui.HotPreviewSettings
import de.drick.compose.utils.lazySuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class ModulePreviewService(
    private val project: Project,
    private val module: Module,
    private val scope: CoroutineScope,
    private val compileMutex: Mutex
) {
    // flow that updates on compile and classpath updates

    enum class State {
        NotInitialized, Ready, Error
    }

    private val settings = HotPreviewSettings.getInstance()

    private val jvmRuntimeClasspathTask by lazySuspend {
        JvmRuntimeClasspathTask.create()
    }

    private val classPathServiceMutableFlow = MutableStateFlow<ClassPathService?>(null)
    val classPathServiceFlow = classPathServiceMutableFlow.asStateFlow()

    suspend fun getClassPathService(): ClassPathService =
        classPathServiceFlow.value ?: updateClasspathService()


    suspend fun recompile() {
        updateClasspathService(recompile = true)
    }

    suspend fun updateClasspathService(recompile: Boolean = false): ClassPathService {
        val newClassPathService = compileMutex.withLock {
            val parameters = if (settings.state.gradleParametersEnabled) settings.state.gradleParameters else ""
            ClassPathService.getInstance(project, module, parameters, jvmRuntimeClasspathTask.get(), recompile)
        }
        classPathServiceMutableFlow.emit(newClassPathService)
        return newClassPathService
    }

}