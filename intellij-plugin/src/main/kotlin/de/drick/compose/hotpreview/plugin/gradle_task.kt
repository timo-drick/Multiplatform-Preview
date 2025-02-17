package de.drick.compose.hotpreview.plugin

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.gradle.configuration.KotlinOutputPathsData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun getClassPathFromGradle(desktopModule: Module): List<URL> = withContext(Dispatchers.Default) {
    val gradleData: DataNode<ModuleData>? = GradleUtil.findGradleModuleData(desktopModule)
    requireNotNull(gradleData) { "No gradle module data found for ${desktopModule.name}" }
    val allChildren = gradleData.children
        .filter { it.data is GradleSourceSetData }
        .filterIsInstance<DataNode<GradleSourceSetData>>()
    val jvmMainFound = allChildren.find { it.data.moduleName == "jvmMain" } != null
    val children = allChildren.filter {
        if (jvmMainFound) {
            it.data.moduleName == "jvmMain"
        } else {
            it.data.moduleName == "main"
        }
    }

    println("Children: ${children.size}")
    val outputPaths = children.flatMap { getOutputPaths(it) }
    println("Output paths:")
    outputPaths.forEach {
        println(it)
    }
    //TODO looks like the dependencies are still missing in the classpath
    val libPaths = children.flatMap { getLibPaths(it) }
    println("Lib paths:")
    libPaths.forEach {
        println(it)
    }
    val paths = outputPaths + libPaths
    paths.filterNot { it.contains("hotpreview-jvm") }
        .map { File(it) }
        .filter { it.exists() }
        .map { it.toURI().toURL() }
}

private fun getLibPaths(data: DataNode<*>) = data.children
    .map { it.data }
    .filterIsInstance<LibraryDependencyData>()
    .flatMap { it.target.getPaths(LibraryPathType.BINARY) }

private fun getOutputPaths(data: DataNode<*>) = data.children
    .map { it.data }
    .filterIsInstance<KotlinOutputPathsData>()
    .flatMap {
        it.paths.values()
    }


suspend fun executeGradleTask(project: Project, taskName: String, path: String) {
    val gradleSettings = GradleSettings.getInstance(project)
    val gradleVmOptions = gradleSettings.gradleVmOptions
    val settings = ExternalSystemTaskExecutionSettings()
    settings.executionName = "HotPreview recompile"
    settings.externalProjectPath = path
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