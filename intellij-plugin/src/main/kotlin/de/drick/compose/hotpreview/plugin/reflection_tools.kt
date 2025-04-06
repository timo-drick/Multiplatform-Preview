package de.drick.compose.hotpreview.plugin

import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.HotPreviewParameter
import de.drick.compose.hotpreview.plugin.service.RenderClassLoaderInstance
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import kotlin.reflect.full.functions

val fqNameHotPreview = requireNotNull(HotPreview::class.qualifiedName)
val fqNameHotPreviewParameter = requireNotNull(HotPreviewParameter::class.qualifiedName)

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

fun RenderClassLoaderInstance.getParameterList(parameter: HotPreviewParameterModel): List<*> {
    val providerClazz = classLoader.loadClass(parameter.providerClassName).kotlin
    val providerInstance = providerClazz.constructors.first().call()
    val getParametersRef = providerClazz.functions.find { it.name == "getParameters" }
    return getParametersRef?.call(providerInstance, parameter.limit) as List<*>
}
