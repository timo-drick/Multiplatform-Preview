package de.drick.compose.hotpreview

import com.intellij.openapi.vfs.VirtualFile
import io.github.classgraph.ClassGraph
import io.github.classgraph.MethodInfo
import org.jetbrains.kotlin.idea.gradleTooling.get
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
annotation class HotPreview(
    val name: String = "",
    val group: String = "",    // Not used yet!
    val widthDp: Int = -1,
    val heightDp: Int = -1,
    //val locale: String = "", // Not supported yet!
    val fontScale: Float = 1f, // Should be between 0.5f and 2.0f
    val darkMode: Boolean = true,
)

data class ComposableFunctionInfo(
    val name: String,
    val sourceFileName: String,
    val className: String,
    val lineNumber: Int
)

data class HotPreviewFunction(
    val name: String,
    val annotation: List<HotPreview>
)

fun analyzeClass(clazz: Class<*>): List<HotPreviewFunction> {
    return clazz.declaredMethods
        .mapNotNull { method ->
            method.kotlinFunction?.let { function ->
                val annotations = function.annotations
                    .filter {
                        it.toString().startsWith("@de.drick.compose.hotpreview.HotPreview")
                    }.map {
                        HotPreview(
                            name = it["name"]?.toString() ?: "",
                            group = it["group"]?.toString() ?: "",
                            widthDp = it["widthDp"] as Int,
                            heightDp = it["heightDp"] as Int,
                            fontScale = it["fontScale"] as Float,
                            darkMode = it["darkMode"] as Boolean
                        )
                    }
                if (annotations.isEmpty())
                    null
                else HotPreviewFunction(
                    name = function.name,
                    annotation = annotations
                )
            }
        }
}

private val packageMatcher = Regex("""package\s+([a-z][a-z0-9_]*(\.[a-z0-9_]+)*[a-z0-9_]*)""")
fun kotlinFileClassName(kotlinFile: VirtualFile): String {
    //TODO use PsiFile to analyze the file
    //TODO support also the @JvmName annotation
    val packageName = kotlinFile.inputStream.bufferedReader().useLines { lines ->
        lines.find { it.trimStart().startsWith("package") }?.let { packageLine ->
            packageMatcher
                .find(packageLine)
                ?.groupValues
                ?.getOrNull(1)
        }
    } ?: ""
    val className = PackagePartClassUtils.getPackagePartFqName(FqName(packageName), kotlinFile.name)
        .toString()
    return className
}