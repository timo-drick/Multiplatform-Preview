package de.drick.compose.hotpreview.plugin.service

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import de.drick.compose.hotpreview.plugin.ui.HotPreviewSettings
import de.drick.compose.hotpreview.plugin.useSuspendWorkspace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ModulePreviewService(
    private val project: Project,
    private val module: Module,
    private val scope: CoroutineScope
) {
    // flow that updates on compile and classpath updates

    enum class State {
        NotInitialized, Ready, Error
    }

    private val settings = HotPreviewSettings.getInstance()

    private val classPathServiceMutableFlow = MutableStateFlow<ClassPathService?>(null)
    val classPathServiceFlow = classPathServiceMutableFlow.asStateFlow()

    init {
        scope.launch {
            try {
                updateClasspathService()
            } catch (err: Throwable) {
                println(err)
            }
        }
    }

    suspend fun recompile() {
        project.useSuspendWorkspace {
            val moduleEntity = requireNotNull(getModule(module)) { "Module ${module.name} not found!" }
            val desktopModule = getJvmTargetModule(moduleEntity)
            val gradleTask = getGradleTaskName(desktopModule)
            val path = requireNotNull(getModulePath(moduleEntity)) { "No module path found!" }
            println("task: $gradleTask path: $path")
            val parameters = if (settings.state.gradleParametersEnabled) settings.state.gradleParameters else ""
            executeGradleTask(project, gradleTask, parameters, path)
            updateClasspathService()
        }
    }

    suspend fun updateClasspathService() {
        val newClassPathService = ClassPathService.getInstance(project, module)
        classPathServiceMutableFlow.emit(newClassPathService)
    }

}