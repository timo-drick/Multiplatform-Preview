package de.drick.compose.utils.classpath

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.module.Module
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.gradle.configuration.KotlinOutputPathsData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.net.URL
import kotlin.collections.map
import kotlin.collections.plus


/**
 * Trying to get classpath for this module using gradle project data from intellij api.
 * Unfortunately transitive dependencies are not resolved.
 */
suspend fun getClassPathFromGradle(module: Module): List<URL> = withContext(Dispatchers.Default) {
    val gradleData: DataNode<ModuleData>? = GradleUtil.findGradleModuleData(module)
    requireNotNull(gradleData) { "No gradle module data found for ${module.name}" }
    val allChildren = gradleData.children
        .filter { it.data is GradleSourceSetData }
        .filterIsInstance<DataNode<GradleSourceSetData>>()

    // Check if we have jvmMain, commonMain, or main source sets
    val hasJvmMain = allChildren.any { it.data.moduleName == "jvmMain" }
    val hasMain = allChildren.any { it.data.moduleName == "main" }

    // Select appropriate source sets based on what's available
    val children = when {
        hasJvmMain -> {
            // For multiplatform projects with jvmMain
            val selected = allChildren.filter {
                it.data.moduleName == "jvmMain" || it.data.moduleName == "commonMain"
            }
            println("Using jvmMain and commonMain source sets")
            selected
        }
        hasMain -> {
            // For JVM-only projects with main
            val selected = allChildren.filter { it.data.moduleName == "main" }
            println("Using main source set")
            selected
        }
        else -> {
            // If we can't find specific source sets, use all available ones
            println("Using all available source sets")
            allChildren
        }
    }

    // Get output paths from selected source sets
    val outputPaths = children.flatMap { getOutputPaths(it) }

    // Get direct library dependencies from selected source sets
    val libPaths = children.flatMap { getLibPaths(it) }

    // Get transitive dependencies by recursively processing all dependencies
    val processedNodes = mutableSetOf<DataNode<*>>()
    val allLibPaths = mutableListOf<String>()

    // Add direct dependencies
    allLibPaths.addAll(libPaths)

    // Process each child to collect transitive dependencies
    children.forEach { child ->
        collectTransitiveDependencies(child, processedNodes, allLibPaths)
    }

    // Also process the module itself to get any module-level dependencies
    collectTransitiveDependencies(gradleData, processedNodes, allLibPaths)

    val paths = outputPaths + allLibPaths
    paths.map { File(it) }
        .filter { it.exists() }
        .map { it.toURI().toURL() }
}

private fun getLibPaths(data: DataNode<*>): List<String> {
    val libraryPaths = data.children
        .map { it.data }
        .filterIsInstance<LibraryDependencyData>()

    // Get all binary paths
    val binaryPaths = libraryPaths.flatMap { it.target.getPaths(LibraryPathType.BINARY) }

    return binaryPaths
}

/**
 * Recursively collects all transitive dependencies from the given node
 */
private fun collectTransitiveDependencies(
    node: DataNode<*>,
    processedNodes: MutableSet<DataNode<*>>,
    result: MutableList<String>
) {
    // Skip if we've already processed this node to avoid cycles
    if (!processedNodes.add(node)) return

    // Get direct library dependencies from this node
    val directLibPaths = getLibPaths(node)

    // Log node type and any .klib files found
    println("Processing node: ${node.data.javaClass.simpleName}")
    directLibPaths.filter { it.endsWith(".klib") }.forEach {
        println("Found .klib in transitive dependency: $it")
    }

    result.addAll(directLibPaths)

    // Process all children that might have dependencies
    node.children.forEach { child ->
        collectTransitiveDependencies(child, processedNodes, result)
    }
}

private fun getOutputPaths(data: DataNode<*>) = data.children
    .map { it.data }
    .filterIsInstance<KotlinOutputPathsData>()
    .flatMap {
        it.paths.values()
    }
