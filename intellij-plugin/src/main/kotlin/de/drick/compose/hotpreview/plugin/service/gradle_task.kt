package de.drick.compose.hotpreview.plugin.service

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


suspend fun executeGradleTask(project: Project, taskName: String, parameters: String, path: String): Boolean {
    val gradleSettings = GradleSettings.getInstance(project)
    val gradleVmOptions = gradleSettings.gradleVmOptions
    val settings = ExternalSystemTaskExecutionSettings()
    settings.executionName = "HotPreview recompile"
    settings.externalProjectPath = path
    settings.taskNames = listOf(taskName)
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
suspend fun getClassPathFromGradleTask(project: Project, configurationName: String = "jvmRuntimeClasspath", path: String): List<URL> {
    // Create a task script that prints the classpath
    val taskName = "printClasspath${System.currentTimeMillis()}"

    // Create a temporary file for output
    val outputFile = FileUtil.createTempFile("hotpreview-classpath-", ".txt")
    val outputFilePath = outputFile.absolutePath

    // Create the init script
    val initScriptFile = createInitScript(taskName, configurationName, outputFilePath)
    println("Init script file: $initScriptFile")

    val parameters = """
        --init-script "${initScriptFile.absolutePath}"
    """.trimIndent()

    try {
        // Execute the task
        executeGradleTask(project, taskName, parameters, path)

        // Check if the output file exists and has content
        return if (outputFile.exists() && outputFile.length() > 0) {
            // Read the classpath entries from the file
            outputFile.readLines()
                .filter { it.isNotBlank() }
                .map { File(it.trim()).toURI().toURL() }
        } else {
            println("Output file not found or empty: $outputFilePath")
            emptyList()
        }
    } catch (e: Exception) {
        println("Error getting classpath from Gradle task: ${e.message}")
        return emptyList()
    } finally {
        // Clean up
        initScriptFile.delete()
        outputFile.delete()
    }
}

/**
 * Creates an init script for the Gradle task that prints the classpath.
 * 
 * @param taskName The name of the task to create
 * @param configurationName The name of the configuration to get the classpath for
 * @param outputFilePath The path to the file where the classpath will be written
 * @return The created init script file
 */
private fun createInitScript(taskName: String, configurationName: String, outputFilePath: String): File {
    // Create the init script in Kotlin DSL
    val initScript = """
        allprojects {
            tasks.register("$taskName") {
                val classpathRuntime = project.configurations.findByName("$configurationName")?.map { it.absolutePath }
                doLast {
                    val text = classpathRuntime?.joinToString("\n")
                    println(text)
                    File("$outputFilePath").writeText(text ?: "")
                }
            }
        }
    """.trimIndent()
    val initScriptFile = FileUtil.createTempFile("hotpreview-init-", ".gradle.kts")
    initScriptFile.writeText(initScript)
    return initScriptFile
}
