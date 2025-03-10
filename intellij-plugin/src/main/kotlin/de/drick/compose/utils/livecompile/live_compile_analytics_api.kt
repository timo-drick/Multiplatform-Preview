package de.drick.compose.utils.livecompile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.hotpreview.plugin.analyzeFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaCompilerTarget
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModuleProvider
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import java.io.File

@OptIn(KaExperimentalApi::class)
suspend fun compileAnalyticsAPI(
    project: Project,
    classPath: List<File>,
    virtualFile: VirtualFile,
) {
    project.analyzeFile(virtualFile) { file ->
        val module = virtualFile.getModule(project)
        val config = CompilerConfiguration().apply {
            /*addJvmClasspathRoots(classPath)
            val javaHome = System.getProperty("java.home") //TODO maybe get it from IDE
            val jmods = File(javaHome, "jmods")
            if (jmods.exists()) {
                addJvmClasspathRoots(listOf(
                    File(jmods, "java.base.jmod"),
                    File(jmods, "java.desktop.jmod")
                ))
            }
            put(JVMConfigurationKeys.ENABLE_JVM_PREVIEW, true)
            put(JVMConfigurationKeys.IR, true)*/
        }
        /*val target = KaCompilerTarget.Jvm(false)

        val result = useSiteSession.compile(file, config, target) {
            it.severity != KaSeverity.ERROR
        }
        println(result)*/
    }
}