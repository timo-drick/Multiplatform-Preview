package de.drick.compose.hotpreview.plugin

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.utils.livecompile.SourceSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.facet.isMultiPlatformModule
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.text.contains


private val LOG = logger<ProjectAnalyzer>()

enum class ClassPathMode {
    ALL, ONLY_LIBS, ONLY_LOCAL
}

class ProjectAnalyzer(
    private val project: Project
) {

    suspend fun getSdkInfo(): String {
        return readAction {
            val projectSdk = ProjectRootManager.getInstance(project)
                .projectSdk
            LOG.debug("Sdk: ${projectSdk?.name}")
            projectSdk?.homePath ?: ""
        }
    }

    suspend fun getOutputFolder(module: Module): String {
        return readAction {
            val compiler = CompilerModuleExtension.getInstance(module)
            requireNotNull(compiler?.compilerOutputUrl) { "Compiler output path not found!" }
        }
    }

    suspend fun getSourceFolder(file: VirtualFile): SourceSet {
        val module = getModule(file)
        val jvmSource = getSourcePath(getJvmTargetModule(module))
        val commonSource = getCommonTargetModule(module)?.let { getSourcePath(it) }
        LOG.debug("jvm source:    $jvmSource")
        LOG.debug("common source: $commonSource")
        return SourceSet(
            commonSrcDir = commonSource,
            jvmSrcDir = jvmSource
        )
    }

    suspend fun executeGradleTask(file: VirtualFile) {
        val module = getModule(file)
        val modulePath = requireNotNull(getModulePath(module))
        val desktopModule = getJvmTargetModule(module)
        /*
        val gradleData = GradleUtil.findGradleModuleData(desktopModule)
        gradleData?.let { gradle ->
            println("Gradle data: ${gradle.data.moduleName} ${gradle.data.gradlePath}")
        }
        */
        val tokens = desktopModule.name.split(".")
        val moduleName = tokens.drop(1).joinToString(":")
        val taskName = ":${moduleName}Classes"
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

    suspend fun getClassPath(file: VirtualFile): List<URL> = withContext(Dispatchers.Default) {
        val desktopModule = getJvmTargetModule(file)
        getClassPath(desktopModule).distinct()
    }

    suspend fun getJvmTargetModule(file: VirtualFile): Module =
        getJvmTargetModule(getModule(file))

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

    suspend fun getClassPath(module: Module, mode: ClassPathMode = ClassPathMode.ALL): List<URL> = readAction {
        val moduleClassPath = getClassPathArray(module, mode)
        val dependencyClassPath = ModuleRootManager.getInstance(module)
            .dependencies
            .flatMap { getClassPathArray(it, mode) }
        moduleClassPath + dependencyClassPath
    }

    private fun getClassPathArray(module: Module, mode: ClassPathMode): List<URL> {
        val rm = ModuleRootManager.getInstance(module)
        val classPath = rm
            .orderEntries()
            .classesRoots
            .filter {
                when (mode) {
                    ClassPathMode.ALL -> true
                    ClassPathMode.ONLY_LIBS -> !it.isInLocalFileSystem
                    ClassPathMode.ONLY_LOCAL -> it.isInLocalFileSystem
                }
            }
            .map { File(it.presentableUrl) }
            .filter { it.exists() }
            .map { it.toURI().toURL() }
        return classPath
    }

    private suspend fun getSourcePath(module: Module) = readAction {
        ModuleRootManager.getInstance(module)
            .sourceRoots
            .firstOrNull { it.name == "kotlin" }
            ?.presentableUrl
    }

    suspend fun getModule(file: VirtualFile) =
        readAction {
            requireNotNull(file.getModule(project)) { "Module for file: $file not found!" }
        }

    private suspend fun getModulePath(module: Module) =
        readAction { ExternalSystemApiUtil.getExternalProjectPath(module) }
}

