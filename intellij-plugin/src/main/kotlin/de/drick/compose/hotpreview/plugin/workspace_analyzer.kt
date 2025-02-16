package de.drick.compose.hotpreview.plugin

import androidx.compose.ui.ImageComposeScene
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.collections.find
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val LOG = logger<WorkspaceAnalyzer>()
/**
 * TODO Replace the project_analyzer with this implementation.
 */
class WorkspaceAnalyzer(
    private val project: Project
) {
    private val workspaceModel = com.intellij.platform.backend.workspace.WorkspaceModel.getInstance(project)
    private val currentSnapshot
        get() = workspaceModel.currentSnapshot

    private fun getClassLoader(file: VirtualFile) =
        URLClassLoader(getClassPathForFile(file).toTypedArray(), ImageComposeScene::class.java.classLoader)

    suspend fun getJvmTargetModule(module: ModuleEntity): ModuleEntity {
        val baseModuleName = module.name.substringBeforeLast(".")

        val desktopModule = currentSnapshot.entities(ModuleEntity::class.java)
            .filter { it.name.startsWith(baseModuleName) }
            //.filter { it.isTestModule.not() }
            .find {
                it.name.contains("jvmMain") || it.name.contains("desktopMain") || it.name.substringAfterLast(".") == "main"
            }
        requireNotNull(desktopModule) { "No desktop module found!" }
        return desktopModule
    }

    fun getClassPathForFile(file: VirtualFile): Set<URL> {
        val fileModule = getModule(file)
        requireNotNull(fileModule) { "No module found!" }
        val baseModuleName = fileModule.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        LOG.debug("Base module: $baseModuleName")
        val desktopModule = currentSnapshot.entities(ModuleEntity::class.java)
            .filter { it.name.startsWith(baseModuleName) }
            //.filter { it.isTestModule.not() }
            .find { it.name.contains("jvmMain") || it.name.contains("desktopMain") }
        requireNotNull(desktopModule) { "No desktop module found!" }
        val modules = desktopModule.dependencies
            .filterIsInstance<ModuleDependency>()
            .mapNotNull { currentSnapshot.resolve(it.module) } + desktopModule

        val classPath = modules.flatMap { module ->
            module.contentRoots.forEach {
                LOG.debug(it.toString())
            }
            module.dependencies
                .filterIsInstance<LibraryDependency>()
                .mapNotNull { currentSnapshot.resolve(it.library) }
                .mapNotNull { library ->
                    library.roots.find {
                        it.type == LibraryRootTypeId.COMPILED
                    }?.url?.presentableUrl
                }
        }.map { File(it).toURI().toURL() }
        return classPath.toSet()
    }

    fun getModule(file: VirtualFile): ModuleEntity? {
        return file.getModule(project)?.let { module ->
            currentSnapshot.resolve(ModuleId(module.name))
        }
    }

    fun isAndroid(file: VirtualFile) =
        getModule(file)?.facets?.find { it.typeId.name == "android" } != null

    fun analyzeModule(module: ModuleEntity) {
        println("Module: ${module.name} type: ${module.type}")
        module.facets.forEach {
            println("Facet: ${it.name} type: ${it.typeId.name}")
        }
        module.dependencies.forEach { dep ->
            println("Dep: $dep")
        }
    }

    private fun getModulePath(file: VirtualFile): String? {
        val module = ProjectFileIndex.getInstance(project).getModuleForFile(file)
        return ExternalSystemApiUtil.getExternalProjectPath(module)
    }

    suspend fun loadFileClass(file: VirtualFile): Class<*> = withContext(Dispatchers.Default) {
        val fileClassName = kotlinFileClassName(file)
        getClassLoader(file).loadClass(fileClassName)
    }

    suspend fun executeGradleTask(file: VirtualFile) = suspendCoroutine { cont ->
        val gradleVmOptions = GradleSettings.getInstance(project).gradleVmOptions
        val settings = ExternalSystemTaskExecutionSettings()
        settings.executionName = "HotPreview recompile"
        settings.externalProjectPath = getModulePath(file)
        settings.taskNames = listOf("desktopMainClasses")
        settings.vmOptions = gradleVmOptions
        settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            object : TaskCallback {
                override fun onSuccess() {
                    cont.resume(true)
                }
                override fun onFailure() {
                    cont.resume(false)
                }
            },
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false,
            UserDataHolderBase()
        )
    }
}
