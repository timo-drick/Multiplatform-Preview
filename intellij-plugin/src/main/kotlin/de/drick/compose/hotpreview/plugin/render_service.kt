package de.drick.compose.hotpreview.plugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.collections.plus
import kotlin.collections.toTypedArray

private val LOG = logger<RenderService>()

@Service(Service.Level.PROJECT)
class RenderService(
    private val project: Project,
    private val scope: CoroutineScope
) {
    private val projectAnalyzer = ProjectAnalyzer(project)

    suspend fun render(file: VirtualFile) {
        scope.launch {

        }
        val skikoLibs = RuntimeLibrariesManager.getRuntimeLibs()
        LOG.debug(skikoLibs.toString())
        val classPath = projectAnalyzer.getClassPath(file) + skikoLibs
        //Add skiko libs

        val classLoader = URLClassLoader(
            classPath.toTypedArray(),
            null
        )
        val fileClassName = kotlinFileClassName(file)
        val previewFunctions = findPreviewAnnotations(project, file)

        // Workaround for legacy resource loading in old compose code
        // See androidx.compose.ui.res.ClassLoaderResourceLoader
        // It uses the contextClassLoader to load the resources.
        val previousContextClassLoader = Thread.currentThread().contextClassLoader
        // For new compose.components resource system a LocalCompositionProvider is used.
        Thread.currentThread().contextClassLoader = classLoader
        val data = try {
            renderPreview(classLoader, fileClassName, previewFunctions)
        } finally {
            Thread.currentThread().contextClassLoader = previousContextClassLoader
        }
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