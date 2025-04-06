package de.drick.compose.hotpreview.plugin.service

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.hotpreview.plugin.ClassPathMode
import de.drick.compose.hotpreview.plugin.ProjectAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.collections.plus
import kotlin.collections.toTypedArray
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions


data class RenderClassLoaderInstance(
    val classLoader: ClassLoader,
    val renderClassInstance: Any,
    val renderFunctionRef: KFunction<*>,
    val fileClass: Class<*>
)

private const val previewRenderImplFqn = "de.drick.compose.hotpreview.RenderPreviewImpl"

class ClassPathService private constructor(
    val classPathLibs: Array<URL>,
    classPathLocal: Array<URL>
) {
    val classPathFull = classPathLocal + classPathLibs
    val classLoader = URLClassLoader(classPathFull, null)

    companion object {
        suspend fun getInstance(project: Project, file: VirtualFile) = withContext(Dispatchers.Default) {
            val module = requireNotNull(file.getModule(project)) { "Module for file: $file not found!" }
            getInstance(project, module)
        }
        suspend fun getInstance(project: Project, module: Module) = withContext(Dispatchers.Default) {
            val projectAnalyzer = ProjectAnalyzer(project)
            val jvmModule = projectAnalyzer.getJvmTargetModule(module)
            val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()

            // Get classpath using Gradle task
            val path = requireNotNull(projectAnalyzer.getModulePath(module)) { "Module path $module not found!" }

            // Try to get classpath using Gradle task first
            println("Getting classpath using Gradle task. Path: $path")
            val gradleTaskClassPath = getClassPathFromGradleTask(project, "jvmRuntimeClasspath", path)

            // Fall back to the old method if the task method fails
            val gradleClassPath = if (gradleTaskClassPath.isNotEmpty()) {
                println("Successfully retrieved classpath using Gradle task")
                gradleTaskClassPath
            } else {
                println("Falling back to old method for getting classpath")
                projectAnalyzer.getClassPath(jvmModule)
            }

            // Combine classpaths and add skiko libs
            val classPathLibs = (gradleClassPath + skikoLibs).distinct()

            val classPathLocal = projectAnalyzer
                .getClassPath(jvmModule, ClassPathMode.ONLY_LOCAL)
                .distinct()
            //val fileClassName = kotlinFileClassName(file)

            println("Libs by Gradle analysis:")
            classPathLibs.forEach { println("Gradle: ${it.path}") }

            ClassPathService(classPathLibs.toTypedArray(), classPathLocal.toTypedArray())
        }
    }

    fun getRenderClassLoaderInstance(fileClassName: String): RenderClassLoaderInstance {
        val fileClass = classLoader.loadClass(fileClassName)
        requireNotNull(fileClass) { "Unable to find class: $fileClassName" }
        val renderClazz = classLoader.loadClass(previewRenderImplFqn).kotlin
        val renderClassInstance = renderClazz.constructors.first().call()
        val renderFunctionRef = renderClazz.declaredFunctions.find {
            it.name == "render"
        }
        requireNotNull(renderFunctionRef) { "Unable to find method: $previewRenderImplFqn:render" }
        return RenderClassLoaderInstance(
            classLoader = classLoader,
            renderClassInstance = renderClassInstance,
            renderFunctionRef = renderFunctionRef,
            fileClass = fileClass
        )
    }
}

object RuntimeLibrariesManager {
    private val LOG = logger<RuntimeLibrariesManager>()

    private var tmpFolder: File? = null

    private val runtimeLibs = listOf(
        "hot_preview_render-all.jar"
    )

    private fun getResUrl(name: String) = this.javaClass.classLoader.getResource(name)

    private suspend fun initialize(): File = withContext(Dispatchers.IO) {
        val dir = tmpFolder
        if (dir == null) {
            val dir = FileUtil.createTempDirectory("hotpreview", "libs", true)
            tmpFolder = dir
            //Copy libraries
            LOG.debug("Temp dir: $dir")
            runtimeLibs.forEach { fileName ->
                val url = getResUrl(fileName)
                val outputFile = File(dir, fileName)
                url?.openStream()?.use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            dir
        } else {
            dir
        }
    }

    suspend fun getRuntimeLibs(): List<URL> = withContext(Dispatchers.Default) {
        val tmpFolder = initialize()
        runtimeLibs.map {
            File(tmpFolder, it).toURI().toURL()
        }
    }
}