package de.drick.compose.hotpreview

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkspaceAnalyzer(
    private val project: Project
) {
    private val workspaceModel = com.intellij.platform.backend.workspace.WorkspaceModel.getInstance(project)
    private val currentSnapshot = workspaceModel.currentSnapshot

    private fun getClassLoader(file: VirtualFile) =
        URLClassLoader(getClassPathForFile(file).toTypedArray(), ImageComposeScene::class.java.classLoader)

    private fun getClassPathForFile(file: VirtualFile): Set<URL> {
        val fileModule = getModule(file)
        requireNotNull(fileModule) { "No module found!" }
        val baseModuleName = fileModule.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        val desktopModule = currentSnapshot.entities(ModuleEntity::class.java)
            .filter { it.name.startsWith(baseModuleName) }
            //.filter { it.isTestModule.not() }
            .find { it.name.contains("desktopMain") }
        requireNotNull(desktopModule) { "No desktop module found!" }
        val modules = desktopModule.dependencies
            .filterIsInstance<ModuleDependency>()
            .mapNotNull { currentSnapshot.resolve(it.module) } + desktopModule

        val classPath = modules.flatMap { module ->
            module.dependencies
                .filterIsInstance<LibraryDependency>()
                .mapNotNull { currentSnapshot.resolve(it.library) }
                .mapNotNull { library ->
                    library.roots.find {
                        it.type == LibraryRootTypeId.SOURCES
                    }?.url?.presentableUrl
                }
        }.map { File(it).toURI().toURL() }
        return classPath.toSet()
    }

    private fun getModule(file: VirtualFile): ModuleEntity? {
        val module = ModuleUtil.findModuleForFile(file, project)
        val fileManager = workspaceModel.getVirtualFileUrlManager()
        val virtualFileUrl = file.toVirtualFileUrl(fileManager)
        val result = currentSnapshot.getVirtualFileUrlIndex().findEntitiesByUrl(virtualFileUrl)

        val moduleName = module?.name
        checkNotNull(moduleName)
        return currentSnapshot.resolve(ModuleId(moduleName))
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

class ProjectAnalyzer(
    private val project: Project
) {

    private fun getModulePath(file: VirtualFile) =
        ExternalSystemApiUtil.getExternalProjectPath(getModule(file))

    suspend fun loadFileClass(file: VirtualFile): Class<*> = withContext(Dispatchers.Default) {
        val fileClassName = kotlinFileClassName(file)
        getClassLoader(file).loadClass(fileClassName)
    }

    suspend fun executeGradleTask(file: VirtualFile) = suspendCoroutine { cont ->
        val desktopModule = getDesktopModule(file)
        val moduleName = desktopModule.name.substringAfterLast(".")
        val taskName = "${moduleName}Classes"
        val gradleVmOptions = GradleSettings.getInstance(project).gradleVmOptions
        val settings = ExternalSystemTaskExecutionSettings()
        settings.executionName = "HotPreview recompile"
        settings.externalProjectPath = getModulePath(file)
        settings.taskNames = listOf(taskName)
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

    private fun getClassLoader(file: VirtualFile) =
        URLClassLoader(getClassPathForFile(file).toTypedArray(), ImageComposeScene::class.java.classLoader)

    private fun getDesktopModule(file: VirtualFile): Module {
        val fileModule = getModule(file)
        requireNotNull(fileModule) { "No module found!" }
        val baseModuleName = fileModule.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        return runReadAction {
            val desktopModule = project.modules.filter { it.name.startsWith(baseModuleName) }
                //.filter { it.isTestModule.not() }
                .find { it.name.contains("jvmMain") || it.name.contains("desktopMain") }
            requireNotNull(desktopModule) { "No desktop module found!" }
        }
    }

    private fun getClassPathForFile(file: VirtualFile): Set<URL> {
        val desktopModule = getDesktopModule(file)
        val fullCP = runReadAction {
            fun getClassPathArray(module: Module) = ModuleRootManager.getInstance(module)
                .orderEntries()
                .classesRoots
                .map { File(it.presentableUrl).toURI().toURL() }
            getClassPathArray(desktopModule) + ModuleRootManager.getInstance(desktopModule)
                .dependencies
                .flatMap { getClassPathArray(it) }
        }
        return fullCP.toSet()
    }

    private fun getModule(file: VirtualFile) =
        runReadAction { ProjectFileIndex.getInstance(project).getModuleForFile(file) }
}


data class HotPreviewData(
    val function: HotPreviewFunction,
    val image: List<RenderedImage?>,
)

fun renderPreviewForClass(clazz: Class<*>): List<HotPreviewData> =
    analyzeClass(clazz).map { function ->
        println("F: $function")
        val method = clazz.declaredMethods.find { it.name == function.name }
        method?.isAccessible = true
        val images = function.annotation.map { annotation ->
            val widthDp = annotation.widthDp.dp
            val heightDp = annotation.heightDp.dp
            method?.let {
                renderMethod(
                    method = it,
                    size = DpSize(widthDp, heightDp),
                    density = Density(2f, annotation.fontScale),
                    isDarkTheme = annotation.darkMode
                )
            }
        }
        HotPreviewData(
            function = function,
            image = images
        )
    }
