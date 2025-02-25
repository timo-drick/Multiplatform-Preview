package de.drick.compose.hotpreview.plugin

import de.drick.compose.utils.lazySuspend
import java.net.URL
import java.net.URLClassLoader

private const val hotPreviewCompilerClassName = "de.drick.compose.hotpreview.HotPreviewCompiler"

class HotRecompileService(
    private val destFolder: String,
    private val jvmTarget: String,
    private val languageVersion: String
) {
    private val libsManager = EmbeddedLibrariesManager
    private val classLoader by lazySuspend {
        URLClassLoader(libsManager.getCompileTimeLibs().toTypedArray(), null)
    }

    suspend fun compile(
        module: String?,
        classPaths: Array<URL>,
        fileList: List<String>
    ) {
        println("Try to load class: $")
        val hotPreviewCompilerClass = classLoader.get().loadClass(hotPreviewCompilerClassName)
        println(hotPreviewCompilerClass)
        val hotPreviewCompiler = hotPreviewCompilerClass.getDeclaredConstructor(
            String::class.java, // jvmTarget
            String::class.java  // languageVersion
        ).newInstance(jvmTarget, languageVersion)
        val compileMethod = hotPreviewCompilerClass.getMethod("compile",
            String::class.java,
            String::class.java,
            List::class.java,
            List::class.java
        )
        println("Call method")
        //val cp = classPaths.map { it.path }
        compileMethod.invoke(hotPreviewCompiler,
            module,
            destFolder,
            classPaths.map { it.path }.toList(),
            fileList
        )
        println("Method executed")
    }
    /*fun compileMultiplatform(
        compiler: K2JVMCompiler,
        baseArguments: K2JVMCompilerArguments,
        module: String?,
        destFolder: String,
        classPaths: List<String>,
        jvmSources: List<String>,
        commonSources: List<String>,
    )*/
}