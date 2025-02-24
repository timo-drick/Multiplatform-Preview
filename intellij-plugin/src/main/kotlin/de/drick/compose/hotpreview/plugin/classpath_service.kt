package de.drick.compose.hotpreview.plugin

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val classPathLocal: Array<URL>,
    val fileClassName: String
) {
    companion object {
        suspend fun getInstance(project: Project, file: VirtualFile) = withContext(Dispatchers.Default) {
            val projectAnalyzer = ProjectAnalyzer(project)
            val jvmModule = projectAnalyzer.getJvmTargetModule(file)
            val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()
            val classPathLibs = projectAnalyzer.getClassPath(jvmModule, ClassPathMode.ONLY_LIBS) + skikoLibs
            val classPathLocal = projectAnalyzer
                .getClassPath(jvmModule, ClassPathMode.ONLY_LOCAL)
                .distinct() + classPathLibs
            val fileClassName = kotlinFileClassName(file)
            ClassPathService(classPathLibs.toTypedArray(), classPathLocal.toTypedArray(), fileClassName)
        }
    }
    val classPathFull = classPathLocal + classPathLibs
    fun createRenderClassLoaderInstance(): RenderClassLoaderInstance {
        val classLoader = URLClassLoader(classPathFull, null)
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