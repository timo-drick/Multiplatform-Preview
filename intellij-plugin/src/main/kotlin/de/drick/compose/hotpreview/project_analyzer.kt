package de.drick.compose.hotpreview

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ProjectAnalyzer(
    private val project: Project
) {

    fun getModulePath(file: VirtualFile) =
        ExternalSystemApiUtil.getExternalProjectPath(getModule(file))

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

    private suspend fun getClassLoader(file: VirtualFile) =
        URLClassLoader(getClassPathForFile(file).toTypedArray(), ImageComposeScene::class.java.classLoader)

    private suspend fun getClassPathForFile(file: VirtualFile): Set<URL> {
        val commonModule = getModule(file)
        requireNotNull(commonModule) { "No module found!" }
        // TODO get the module instance
        val desktopModule = project.modules.find { it.name == "ShaderPainT.app.desktopMain" }//workspaceModel.currentSnapshot.resolve(ModuleId("ShaderPainT.app.desktopMain"))
        requireNotNull(desktopModule) { "No desktop module found!" }
        val appModule = project.modules.find { it.name == "ShaderPainT.app" }
        checkNotNull(appModule)
        fun getClassPathArray(module: Module) = ModuleRootManager.getInstance(module)
            .orderEntries()
            .classesRoots
            .map { File(it.presentableUrl).toURI().toURL() }
        val fullCP = getClassPathArray(desktopModule) + ModuleRootManager.getInstance(desktopModule)
            .dependencies
            .flatMap { getClassPathArray(it) }
        return fullCP.toSet()
    }

    private fun getModule(file: VirtualFile) =
        ProjectFileIndex.getInstance(project).getModuleForFile(file)
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
