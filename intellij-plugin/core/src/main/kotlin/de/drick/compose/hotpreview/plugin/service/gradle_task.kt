package de.drick.compose.hotpreview.plugin.service

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend fun executeGradleTask(project: Project, taskNameList: List<String>, parameters: String, path: String): Boolean {
    val gradleSettings = GradleSettings.getInstance(project)
    val gradleVmOptions = gradleSettings.gradleVmOptions
    val settings = ExternalSystemTaskExecutionSettings()
    settings.executionName = "HotPreview recompile"
    settings.externalProjectPath = path
    settings.taskNames = taskNameList
    settings.scriptParameters = parameters
    settings.vmOptions = gradleVmOptions
    settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id

    return suspendCoroutine { cont ->
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

/**
 * Executes a Gradle task to retrieve the classpath of a specific configuration.
 * This function uses the Gradle API to execute a task that prints the classpath.
 *
 * @param project The IntelliJ project
 * @param path The path to the Gradle project
 * @return A list of URLs representing the classpath
 */
suspend fun getClassPathFromGradleTask(
    project: Project,
    module: Module,
    path: String,
    recompileTask: String? = null,
    parameters: String
): List<URL> {
    val classPathTask = JvmRuntimeClasspathTask.create(module)
    val parameters = """--init-script "${classPathTask.initScriptFile.absolutePath}" $parameters"""
    val taskList = listOfNotNull(recompileTask, classPathTask.taskName)
    try {
        // Execute the task
        executeGradleTask(project, taskList, parameters, path)
        return classPathTask.readClassPath()
    } catch (e: Exception) {
        println("Error getting classpath from Gradle task: ${e.message}")
        return emptyList()
    }
}

class JvmRuntimeClasspathTask(
    val initScriptFile: File,
    private val buildDir: File,
) {
    private val runtimeClassPathFile = File(buildDir, "hotPreviewRuntimeClasspath.txt")
    // Create a temporary file for output
    val taskName = "hotPreviewDetectClasspath"

    fun readClassPath(): List<URL> {
        // Check if the output file exists and has content
        return if (runtimeClassPathFile.exists() && runtimeClassPathFile.length() > 0) {
            // Read the classpath entries from the file
            runtimeClassPathFile.readLines()
                .filter { it.isNotBlank() }
                .map { File(it.trim()).toURI().toURL() }
        } else {
            println("Output file not found or empty: $runtimeClassPathFile")
            emptyList()
        }
    }

    companion object {
        suspend fun create(module: Module): JvmRuntimeClasspathTask {
            val buildDir = requireNotNull(getBuildDirectoryFromGradle(module)) { "Build directory not defined" }
            val classPathGradleScript = RuntimeLibrariesManager.getClassPathGradleScript()
            return JvmRuntimeClasspathTask(
                initScriptFile = classPathGradleScript,
                buildDir = File(buildDir)
            )
        }
    }
}

/**
 * Gets the build directory for a module using Gradle module data.
 * This function extracts the build directory path from the ModuleData returned by GradleUtil.findGradleModuleData.
 *
 * @param module The IntelliJ module
 * @return The build directory path as a string, or null if it couldn't be determined
 */
private suspend fun getBuildDirectoryFromGradle(module: Module): String? = withContext(Dispatchers.Default) {
    val gradleData: DataNode<ModuleData>? = GradleUtil.findGradleModuleData(module)
    if (gradleData == null) {
        println("No gradle module data found for ${module.name}")
        return@withContext null
    }

    // Get the module data
    val moduleData = gradleData.data

    // Try to get the external project path
    val externalProjectPath = moduleData.linkedExternalProjectPath
    if (externalProjectPath.isNotEmpty()) {
        // Typically, the build directory is at <project_path>/build
        val buildDirPath = File(externalProjectPath, "build").absolutePath
        println("Build directory from external project path: $buildDirPath")
        return@withContext buildDirPath
    }

    // If all else fails, return null
    println("Could not determine build directory for module: ${module.name}")
    return@withContext null
}
