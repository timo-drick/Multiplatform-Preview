package de.drick.compose.hotpreview.plugin

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.idea.debugger.core.stepping.getLineRange
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid


suspend fun getPsiFileSafely(project: Project, virtualFile: VirtualFile): PsiFile? = readAction {
    if (project.isDisposed) return@readAction null
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@readAction null
    if (psiFile.isValid) psiFile else null
}

private val hotPreviewAnnotationClassId = ClassId.topLevel(FqName(fqNameHotPreview))

private val hotPreviewDefaultValues = HotPreview()

private fun KaAnnotation.toHotPreviewAnnotation(): HotPreview {
    // Build a map
    val map = arguments.associateBy { it.name.toString() }
        .mapValues { (it.value.expression as? KaAnnotationValue.ConstantValue)?.value?.value }
    val dv = hotPreviewDefaultValues
    return HotPreview(
        name = map["name"]?.toString() ?: dv.name,
        group = map["group"]?.toString() ?: dv.group,
        widthDp = map["widthDp"] as Int? ?: dv.widthDp,
        heightDp = map["heightDp"] as Int? ?: dv.heightDp,
        locale = map["locale"] as String? ?: dv.locale,
        fontScale = map["fontScale"] as Float? ?: dv.fontScale,
        darkMode = map["darkMode"] as Boolean? ?: dv.darkMode,
        density = map["density"] as Float? ?: dv.density
    )
}

suspend fun analyzePsiFile(project: Project, psiFile: PsiFile): List<HotPreviewFunction> =
    //TODO Find a solution which is also working in dumb mode.
    smartReadAction(project) {
        val functionList = mutableListOf<KtNamedFunction>()
        val t = object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                functionList.add(function)
            }
        }
        psiFile.accept(t)
        functionList.mapNotNull { function ->
            analyze(function) {
                val mySymbol = function.symbol
                val hotPreviewAnnotations = mySymbol.annotations
                    .filter { it.classId == hotPreviewAnnotationClassId }
                    .map {
                        HotPreviewAnnotation(
                            lineRange = it.psi?.getLineRange(),
                            annotation = it.toHotPreviewAnnotation()
                        )
                    }
                if (hotPreviewAnnotations.isEmpty()) null
                else HotPreviewFunction(
                    name = function.name ?: "",
                    annotation = hotPreviewAnnotations,
                    lineRange = function.getLineRange()
                )
            }
        }
    }
