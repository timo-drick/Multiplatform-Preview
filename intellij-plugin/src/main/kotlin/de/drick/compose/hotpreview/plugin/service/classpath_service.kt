package de.drick.compose.hotpreview.plugin.service

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import de.drick.compose.hotpreview.plugin.ClassPathMode
import de.drick.compose.hotpreview.plugin.ProjectAnalyzer
import de.drick.compose.hotpreview.plugin.useSuspendWorkspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.collections.plus
import kotlin.collections.toTypedArray
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue


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
        suspend fun getInstance(
            project: Project,
            module: Module,
            gradleParameters: String,
            recompile: Boolean = false
        ) = withContext(Dispatchers.Default) {
            val ts = TimeSource.Monotonic
            val start = ts.markNow()
            val projectAnalyzer = ProjectAnalyzer(project)
            val jvmModule = projectAnalyzer.getJvmTargetModule(module)
            val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()
            val path = requireNotNull(projectAnalyzer.getModulePath(module)) { "Module path $module not found!" }

            val compileTask = if (recompile) project.useSuspendWorkspace {
                val moduleEntity = requireNotNull(getModule(module)) { "Module ${module.name} not found!" }
                val desktopModule = getJvmTargetModule(moduleEntity)
                getGradleTaskName(desktopModule)
            } else null
            println("compile task: $compileTask path: $path")

            val preparation = ts.markNow() - start
            println("ClassPathService preparation time: $preparation")
            val (gradleTaskClassPath, duration) = measureTimedValue {
                getClassPathFromGradleTask(project, module, path, compileTask, gradleParameters)
            }
            println("Class path jvmRuntimeClasspath execution time: $duration")
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

            //println("Libs by Gradle analysis:")
            //classPathLibs.forEach { println("Gradle: ${it.path}") }

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

    private val classpathGradleScript = "classpath.gradle.kts"

    private val gradleScripts = listOf(
        classpathGradleScript
    )

    private fun getResUrl(name: String) = this.javaClass.classLoader.getResource(name)

    private suspend fun initialize(): File = withContext(Dispatchers.IO) {
        val dir = tmpFolder
        if (dir == null) {
            val dir = FileUtil.createTempDirectory("hotpreview", "libs", true)
            tmpFolder = dir
            //Copy libraries
            LOG.debug("Temp dir: $dir")
            (runtimeLibs+gradleScripts).forEach { fileName ->
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

    suspend fun getClassPathGradleScript(): File = withContext(Dispatchers.Default) {
        val tmpFolder = initialize()
        File(tmpFolder, classpathGradleScript)
    }
}