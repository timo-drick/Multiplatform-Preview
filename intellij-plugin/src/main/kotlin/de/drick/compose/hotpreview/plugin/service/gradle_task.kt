package de.drick.compose.hotpreview.plugin.service

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
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
 * @param configurationName The name of the Gradle configuration to get the classpath for (e.g., "runtimeClasspath")
 * @param path The path to the Gradle project
 * @return A list of URLs representing the classpath
 */
suspend fun getClassPathFromGradleTask(
    project: Project,
    classPathTask: JvmRuntimeClasspathTask,
    path: String,
    recompileTask: String? = null,
    parameters: String
): List<URL> {
    //val classPathTask = JvmRuntimeClasspathTask.create()

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
    private val outputFile: File,
) {
    // Create a temporary file for output
    val taskName = TASK_NAME

    fun readClassPath(): List<URL> {
        // Check if the output file exists and has content
        return if (outputFile.exists() && outputFile.length() > 0) {
            // Read the classpath entries from the file
            outputFile.readLines()
                .filter { it.isNotBlank() }
                .map { File(it.trim()).toURI().toURL() }
        } else {
            println("Output file not found or empty: $outputFile")
            emptyList()
        }
    }

    companion object {
        private const val TASK_NAME = "hotPreviewPrintClasspath"
        private fun initScriptString(outputFilePath: String) = """
            allprojects {
                tasks.register("$TASK_NAME") {
                    val classpathRuntime = project.configurations.findByName("jvmRuntimeClasspath")?.map { it.absolutePath }
                    doLast {
                        val text = classpathRuntime?.joinToString("\n")
                        File("$outputFilePath").writeText(text ?: "")
                    }
                }
            }
        """.trimIndent()

        suspend fun create(): JvmRuntimeClasspathTask = withContext(Dispatchers.IO) {
            val outputFile = FileUtil.createTempFile("hotpreview-classpath-", ".txt")
            val initScriptFile = FileUtil.createTempFile("hotpreview-init-", ".gradle.kts")
            val initScriptString = initScriptString(outputFile.absolutePath)
            initScriptFile.writeText(initScriptString)
            JvmRuntimeClasspathTask(initScriptFile, outputFile)
        }
    }
}
