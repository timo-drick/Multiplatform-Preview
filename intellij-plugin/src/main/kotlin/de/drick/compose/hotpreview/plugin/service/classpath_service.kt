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

class HotPreviewClassLoader(
    private val local: ClassLoader,
    private val override: ClassLoader,
    private val libs: ClassLoader
) : ClassLoader(null) {
    override fun findClass(name: String): Class<*> {
        try {
            val localClazz = local.loadClass(name)
            resolveClass(localClazz)
            println("Loading class: $name from local")
            return localClazz
        } catch (e: ClassNotFoundException) {
            println("Loading class: $name from override")
        }
        try {
            val overrideClazz = override.loadClass(name)
            resolveClass(overrideClazz)
            return overrideClazz
        } catch (e: ClassNotFoundException) {
            println("Loading class: $name from libs")
        }
        val libsClass = libs.loadClass(name)
        resolveClass(libsClass)
        return libsClass
    }
}

class ClassPathService private constructor(
    val classPathOverride: Array<URL>,
    val classPathLibs: Array<URL>,
    classPathLocal: Array<URL>
) {
    val classPathFull = classPathOverride + classPathLocal + classPathLibs
    private val overrideLoader = URLClassLoader(classPathOverride, null)
    private val localLoader = URLClassLoader(classPathLocal, null)
    private val libsLoader = URLClassLoader(classPathLibs, null)
    val classLoader = URLClassLoader(classPathLocal + classPathLibs, overrideLoader)//HotPreviewClassLoader(localLoader, overrideLoader, libsLoader)

    companion object {
        suspend fun getInstance(
            project: Project,
            module: Module,
            gradleParameters: String,
            jvmRuntimeClasspathTask: JvmRuntimeClasspathTask,
            recompile: Boolean = false
        ) = withContext(Dispatchers.Default) {
            val ts = TimeSource.Monotonic
            val start = ts.markNow()
            val projectAnalyzer = ProjectAnalyzer(project)
            val jvmModule = projectAnalyzer.getJvmTargetModule(module)
            val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()
            val classOverrideLibs = RuntimeLibrariesManager.getClassOverrideLibs()
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
                getClassPathFromGradleTask(project, jvmRuntimeClasspathTask, path, compileTask, gradleParameters)
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
            classPathLibs.forEach { println("${it.path}") }

            ClassPathService(
                classPathLocal = classPathLocal.toTypedArray(),
                classPathOverride = classOverrideLibs,
                classPathLibs = classPathLibs.toTypedArray()
            )
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
        "hot_preview_render-all.jar",
        "hot_preview_window_insets.jar"
    )
    private val renderLib = listOf("hot_preview_render-all.jar")
    private val classOverrideLibs = arrayOf("hot_preview_window_insets.jar")

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
        renderLib.map {
            File(tmpFolder, it).toURI().toURL()
        }
    }

    suspend fun getClassOverrideLibs(): Array<URL> = withContext(Dispatchers.Default) {
        val tmpFolder = initialize()
        classOverrideLibs.map {
            File(tmpFolder, it).toURI().toURL()
        }.toTypedArray()
    }
}