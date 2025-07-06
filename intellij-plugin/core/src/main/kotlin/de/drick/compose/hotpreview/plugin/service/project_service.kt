package de.drick.compose.hotpreview.plugin.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.getOrCreate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.util.projectStructure.getModule

@Service(Service.Level.PROJECT) class ProjectPreviewProviderService(
    private val project: Project,
    private val scope: CoroutineScope
) {
    // Singletons for the Modules maybe we will not keep all. So maybe using weak references?
    private val modulePreviewServiceInstanceMap = mutableMapOf<Module, ModulePreviewService>()
    private val compileMutex = Mutex()
    suspend fun getModulePreviewService(
        file: VirtualFile
    ): ModulePreviewService = withContext(Dispatchers.Default) {
        /*val module = project.useSuspendWorkspace {
            requireNotNull(getModule(file)) { "No module found for file: ${file.name}" }
        }*/
        val module = file.getModule(project) ?: throw Error("Can't find module for file: $file")
        modulePreviewServiceInstanceMap.getOrCreate(module) {
            ModulePreviewService(project, module, scope, compileMutex)
        }
    }
    fun createRenderService() = RenderService(compileMutex)
}
