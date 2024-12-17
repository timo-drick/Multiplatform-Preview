package de.drick.compose.hotpreview

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.gradleTooling.get
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import kotlin.reflect.jvm.kotlinFunction

private val fqNameHotPreview = requireNotNull(HotPreview::class.qualifiedName)

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
                        it.toString().startsWith("@$fqNameHotPreview")
                    }.map {
                        HotPreview(
                            name = it["name"]?.toString() ?: "",
                            group = it["group"]?.toString() ?: "",
                            widthDp = it["widthDp"] as Int,
                            heightDp = it["heightDp"] as Int,
                            locale = it["locale"] as String,
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

fun kotlinFileHasHotPreview(kotlinFile: VirtualFile): Boolean = kotlinFile.inputStream
    .bufferedReader()
    .useLines { lines ->
        lines.any { it.contains(fqNameHotPreview) }
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