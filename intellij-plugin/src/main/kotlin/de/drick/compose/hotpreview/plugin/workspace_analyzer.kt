package de.drick.compose.hotpreview.plugin

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.modules
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.idea.workspaceModel.kotlinSettings
import java.io.File
import java.net.URL
import kotlin.collections.find

private val LOG = logger<WorkspaceAnalyzer>()

interface WorkspaceDsl {
    fun getModule(module: Module): ModuleEntity?
    fun getModule(file: VirtualFile): ModuleEntity?
    fun getJvmTargetModule(module: ModuleEntity): ModuleEntity
    fun getGradleTaskName(module: ModuleEntity): String
    fun getModulePath(module: ModuleEntity): String?
    suspend fun getClassPath(file: VirtualFile): List<URL>
}

fun ModuleEntity.isAndroid() = facets.find { it.typeId.name == "android" } != null
fun ModuleEntity.isMultiplatform() = kotlinSettings.firstOrNull()?.isHmppEnabled == true

fun <R>Project.useWorkspace(block: WorkspaceDsl.() -> R): R {
    val env = WorkspaceAnalyzer(this)
    return block(env)
}

suspend fun <R>Project.useSuspendWorkspace(block: suspend WorkspaceDsl.() -> R): R {
    val env = WorkspaceAnalyzer(this)
    return withContext(Dispatchers.Default) { block(env) }
}


private class WorkspaceAnalyzer(
    private val project: Project
): WorkspaceDsl {
    private val workspaceModel = WorkspaceModel.getInstance(project)
    private val currentSnapshot = workspaceModel.currentSnapshot

    override fun getModule(file: VirtualFile): ModuleEntity? {
        return file.getModule(project)?.let { module ->
            getModule(module)
        }
    }
    override fun getModule(module: Module): ModuleEntity? = currentSnapshot.resolve(ModuleId(module.name))

    override fun getJvmTargetModule(module: ModuleEntity): ModuleEntity {
        val baseModuleName = module.name.substringBeforeLast(".")
        val isMultiplatform = module.isMultiplatform()
        val modules = currentSnapshot.entities(ModuleEntity::class.java)
            .filter { it.name.startsWith(baseModuleName) }
        val desktopModule = modules
            .find {
                if (isMultiplatform) {
                    it.name.contains("jvmMain") || it.name.contains("desktopMain")
                } else {
                    it.name.contains("main")
                }
            }
        requireNotNull(desktopModule) { "No desktop module found!" }
        return desktopModule
    }

    override fun getGradleTaskName(module: ModuleEntity): String {
        val tokens = module.name.split(".")
        val moduleName = tokens.drop(1).joinToString(":")
        val taskName = ":${moduleName}Classes"
        return taskName
    }

    override suspend fun getClassPath(file: VirtualFile): List<URL> = withContext(Dispatchers.Default) {
        val fileModule = getModule(file)
        requireNotNull(fileModule) { "No module found!" }
        val desktopModule = getJvmTargetModule(fileModule)
        println(fileModule)
        getClassPath(desktopModule)
            .filterNot { it.contains("hotpreview-jvm") }
            .map { File(it) }
            .filter { it.exists() }
            .map { it.toURI().toURL() }
    }

    fun getClassPath(module: ModuleEntity): Set<String> {
        println("Content roots:")
        module.dependencies.filterIsInstance<ModuleSourceDependency>().forEach {
            println("$it")
        }
        println("Facets:")
        module.facets.forEach {
            println(it)
        }
        val moduleClassPath = getClassPathArray(module)
        val depModulesClassPath = module.dependencies
            .filterIsInstance<ModuleDependency>()
            .mapNotNull { currentSnapshot.resolve(it.module) }
            .flatMap { getClassPathArray(it) }
        return (moduleClassPath + depModulesClassPath).toSet()
    }

    fun getClassPathArray(module: ModuleEntity): List<String> {
        // TODO get the path of the module itself.
        val libs = getClassPathLibs(module)
        return libs
    }
    fun getClassPathLibs(module: ModuleEntity): List<String> = module.dependencies
        .filterIsInstance<LibraryDependency>()
        .mapNotNull { currentSnapshot.resolve(it.library) }
        .mapNotNull { library ->
            library.roots.find {
                it.type == LibraryRootTypeId.COMPILED
            }?.url?.presentableUrl
        }


    fun analyzeModule(module: ModuleEntity) {
        println("Module: ${module.name} type: ${module.type}")
        module.facets.forEach {
            println("Facet: ${it.name} type: ${it.typeId.name}")
        }
        module.dependencies.forEach { dep ->
            println("Dep: $dep")
        }
    }

    override fun getModulePath(module: ModuleEntity): String? {
        val pModule = project.modules.find { it.name == module.name }
        //TODO find other solution
        return ExternalSystemApiUtil.getExternalProjectPath(pModule)
    }

}
