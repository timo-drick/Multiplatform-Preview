package de.drick.compose.hotpreview.plugin

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.utils.livecompile.SourceSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.facet.isMultiPlatformModule
import org.jetbrains.kotlin.idea.base.facet.isTestModule
import org.jetbrains.kotlin.idea.base.facet.platform.platform
import org.jetbrains.kotlin.idea.base.util.isAndroidModule
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import java.io.File
import java.net.URL
import kotlin.text.contains


private val LOG = logger<ProjectAnalyzer>()

enum class ClassPathMode {
    ALL, ONLY_LIBS, ONLY_LOCAL
}

class ProjectAnalyzer(
    private val project: Project
) {

    suspend fun getSdkInfo(): String {
        return smartReadAction(project) {
            val projectSdk = ProjectRootManager.getInstance(project)
                .projectSdk
            LOG.debug("Sdk: ${projectSdk?.name}")
            projectSdk?.homePath ?: ""
        }
    }

    suspend fun getOutputFolder(module: Module): String {
        return smartReadAction(project) {
            val compiler = CompilerModuleExtension.getInstance(module)
            requireNotNull(compiler?.compilerOutputUrl) { "Compiler output path not found!" }
        }
    }

    suspend fun getSourceFolder(file: VirtualFile): SourceSet {
        val module = getModule(file)
        val jvmSource = getSourcePath(getJvmTargetModule(module))
        val commonSource = getCommonTargetModule(module)?.let { getSourcePath(it) }
        LOG.debug("jvm source:    $jvmSource")
        LOG.debug("common source: $commonSource")
        return SourceSet(
            commonSrcDir = commonSource,
            jvmSrcDir = jvmSource
        )
    }

    suspend fun getClassPath(file: VirtualFile): List<URL> = withContext(Dispatchers.Default) {
        val desktopModule = getJvmTargetModule(file)
        getClassPath(desktopModule).distinct()
    }

    suspend fun getJvmTargetModule(file: VirtualFile): Module =
        getJvmTargetModule(getModule(file))

    suspend fun getJvmTargetModule(module: Module): Module {
        val baseModuleName = module.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        return smartReadAction(project) {
            if (module.isMultiPlatformModule) {
                val modules = project.modules
                    .filter { it.name.startsWith(baseModuleName) && it.name != baseModuleName }
                    .filter { it.isTestModule.not() }
                val desktopModule = modules
                    .find { module ->
                        val isJvmPlatform = module.platform.firstOrNull { it.platformName == "JVM" }
                        isJvmPlatform != null && module.platform.size == 1 && module.isAndroidModule().not()
                    }
                requireNotNull(desktopModule) { "No desktop module found!" }
            } else {
                val desktopModule = project.modules.filter { it.name.startsWith(baseModuleName) }
                    .filter { it.isTestModule.not() }
                    .find { it.name.substringAfterLast(".") == "main" }
                requireNotNull(desktopModule) { "No desktop module found!" }
            }
        }
    }

    private suspend fun getCommonTargetModule(module: Module): Module? {
        val baseModuleName = module.name.substringBeforeLast(".")
        // TODO not sure if the name is always desktop for jvm modules
        return smartReadAction(project) {
            project.modules
                .filter { it.name.startsWith(baseModuleName) }
                .find { it.name.contains("commonMain") }
        }
    }

    suspend fun getClassPath(module: Module, mode: ClassPathMode = ClassPathMode.ALL): List<URL> = smartReadAction(project) {
        val moduleClassPath = getClassPathArray(module, mode)
        val processedModules = mutableSetOf<Module>()
        processedModules.add(module)
        val dependencyClassPath = collectDependenciesClassPath(module, mode, processedModules)
        moduleClassPath + dependencyClassPath
    }

    private fun collectDependenciesClassPath(module: Module, mode: ClassPathMode, processedModules: MutableSet<Module>): List<URL> {
        val dependencies = ModuleRootManager.getInstance(module).dependencies
        val result = mutableListOf<URL>()

        for (dependency in dependencies) {
            if (processedModules.add(dependency)) {
                // Add this dependency's classpath
                result.addAll(getClassPathArray(dependency, mode))
                // Recursively add transitive dependencies
                result.addAll(collectDependenciesClassPath(dependency, mode, processedModules))
            }
        }

        return result
    }

    private fun getClassPathArray(module: Module, mode: ClassPathMode): List<URL> {
        val rm = ModuleRootManager.getInstance(module)
        val classPath = rm
            .orderEntries()
            .classesRoots
            .filter {
                when (mode) {
                    ClassPathMode.ALL -> true
                    ClassPathMode.ONLY_LIBS -> !it.isInLocalFileSystem
                    ClassPathMode.ONLY_LOCAL -> it.isInLocalFileSystem
                }
            }
            .map { File(it.presentableUrl) }
            .filter { it.exists() }
            .map { it.toURI().toURL() }
        return classPath
    }

    private suspend fun getSourcePath(module: Module) = smartReadAction(project) {
        ModuleRootManager.getInstance(module)
            .sourceRoots
            .firstOrNull { it.name == "kotlin" }
            ?.presentableUrl
    }

    suspend fun getModule(file: VirtualFile) =
        smartReadAction(project) {
            requireNotNull(file.getModule(project)) { "Module for file: $file not found!" }
        }

    suspend fun getModulePath(module: Module) =
        smartReadAction(project) { ExternalSystemApiUtil.getExternalProjectPath(module) }

    fun getGradleCompileTaskName(module: Module): String {
        val tokens = module.name.split(".")
        val moduleName = tokens.drop(1).joinToString(":")
        val taskName = ":${moduleName}Classes"
        return taskName
    }
}
