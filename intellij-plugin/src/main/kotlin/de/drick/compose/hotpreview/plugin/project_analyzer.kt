package de.drick.compose.hotpreview.plugin

import androidx.compose.runtime.Composer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.readAction
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.hotpreview.RenderPreview
import de.drick.compose.hotpreview.plugin.livecompile.SourceSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.externalSystem.findAll
import org.jetbrains.kotlin.idea.base.facet.isMultiPlatformModule
import org.jetbrains.kotlin.idea.gradle.configuration.KotlinOutputPathsData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass
import kotlin.text.contains


class ProjectAnalyzer(
    private val project: Project
) {

    suspend fun getSdkInfo(): String {
        return readAction {
            val projectSdk = ProjectRootManager.getInstance(project)
                .projectSdk
            println("Sdk: ${projectSdk?.name}")
            projectSdk?.homePath ?: ""
        }
    }

    suspend fun findPreviewAnnotations(file: VirtualFile): List<HotPreviewFunction> {
        getPsiFileSafely(project, file)?.let { psiFile ->
            println("Find preview annotations for: $file")
            return analyzePsiFile(project, psiFile)
        }
        return emptyList()
    }

    suspend fun getOutputFolder(file: VirtualFile): String {
        val module = requireNotNull(getModule(file)) { "Module for file: $file not found!" }
        val jvmModule = getJvmTargetModule(module)
        return readAction {
            val compiler = CompilerModuleExtension.getInstance(jvmModule)
            requireNotNull(compiler?.compilerOutputUrl) { "Compiler output path not found!" }
        }
    }

    suspend fun getSourceFolder(file: VirtualFile): SourceSet {
        val module = requireNotNull(getModule(file)) { "Module for file: $file not found!" }
        val jvmSource = getSourcePath(getJvmTargetModule(module))
        val commonSource = getCommonTargetModule(module)?.let { getSourcePath(it) }
        println("jvm source:    $jvmSource")
        println("common source: $commonSource")
        return SourceSet(
            commonSrcDir = commonSource,
            jvmSrcDir = jvmSource
        )
    }

    suspend fun executeGradleTask(file: VirtualFile) {
        val module = requireNotNull(getModule(file))
        val modulePath = requireNotNull(getModulePath(module))
        val desktopModule = getJvmTargetModule(module)
        val moduleName = desktopModule.name.substringAfterLast(".")
        val taskName = "${moduleName}Classes"
        val gradleSettings = GradleSettings.getInstance(project)
        val gradleVmOptions = gradleSettings.gradleVmOptions
        val settings = ExternalSystemTaskExecutionSettings()
        settings.executionName = "HotPreview recompile"
        settings.externalProjectPath = modulePath
        settings.taskNames = listOf(taskName)
        settings.vmOptions = gradleVmOptions
        settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id

        suspendCoroutine { cont ->
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

    private suspend fun getClassPathFromGradle(file: VirtualFile) {
        val desktopModule = getJvmTargetModule(requireNotNull(getModule(file)))
        val gradleData = GradleUtil.findGradleModuleData(desktopModule)
        println(gradleData)
        gradleData?.let { data ->
            data.children
                .filter { it.data is GradleSourceSetData }
                .filter { (it.data as GradleSourceSetData).moduleName == "jvmMain" }
                .forEach { sourceSetData ->
                    val children = sourceSetData.children.map { it.data }
                    children.filterIsInstance<KotlinOutputPathsData>()
                        .map { it.paths }
                        .forEach { println(it) }
                    children.filterIsInstance<LibraryDependencyData>()
                        .map { it.target.getPaths(LibraryPathType.SOURCE) }
                        .forEach { println(it) }
                    println(sourceSetData)
                }
        }
    }

    suspend fun getClassPath(file: VirtualFile): List<URL> {
        val desktopModule = getJvmTargetModule(requireNotNull(getModule(file)))
        return getClassPath(desktopModule)
            .filterNot { it.contains("hotpreview-jvm") }
            .map { File(it) }
            .filter { it.exists() }
            .map { it.toURI().toURL() }
    }

    suspend fun getJvmTargetModule(module: Module): Module {
        val baseModuleName = module.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        return readAction {
            if (module.isMultiPlatformModule) {
                val desktopModule = project.modules.filter { it.name.startsWith(baseModuleName) }
                    //.filter { it.isTestModule.not() }
                    .find { it.name.contains("jvmMain") || it.name.contains("desktopMain") }
                requireNotNull(desktopModule) { "No desktop module found!" }
            } else {
                val desktopModule = project.modules.filter { it.name.startsWith(baseModuleName) }
                    //.filter { it.isTestModule.not() }
                    .find { it.name.substringAfterLast(".") == "main" }
                requireNotNull(desktopModule) { "No desktop module found!" }
            }
        }
    }

    private suspend fun getCommonTargetModule(module: Module): Module? {
        val baseModuleName = module.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        return readAction {
            project.modules
                .filter { it.name.startsWith(baseModuleName) }
                .find { it.name.contains("commonMain") }
        }
    }

    private suspend fun getClassPath(module: Module): Set<String> = readAction {
        val moduleClassPath = getClassPathArray(module)
        val dependencyClassPath = ModuleRootManager.getInstance(module)
            .dependencies
            .flatMap { getClassPathArray(it) }
        moduleClassPath + dependencyClassPath
    }.toSet()

    private fun getClassPathArray(module: Module): List<String> {
        val rm = ModuleRootManager.getInstance(module)
        val classPath = rm
            .orderEntries()
            .classesRoots
            .map { it.presentableUrl }
        return classPath
    }

    private suspend fun getSourcePath(module: Module) = readAction {
        ModuleRootManager.getInstance(module)
            .sourceRoots
            .firstOrNull { it.name == "kotlin" }
            ?.presentableUrl
    }

    suspend fun getModule(file: VirtualFile) =
        readAction { ProjectFileIndex.getInstance(project).getModuleForFile(file) }

    private suspend fun getModulePath(module: Module) =
        readAction { ExternalSystemApiUtil.getExternalProjectPath(module) }
}

data class HotPreviewData(
    val function: HotPreviewFunction,
    val image: List<RenderedImage?>,
)
