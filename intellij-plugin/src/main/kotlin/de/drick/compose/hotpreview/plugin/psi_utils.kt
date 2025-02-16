package de.drick.compose.hotpreview.plugin

import androidx.compose.runtime.Composable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.idea.debugger.core.stepping.getLineRange
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import kotlin.collections.plus

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()


suspend fun getPsiFileSafely(project: Project, virtualFile: VirtualFile): PsiFile? = readAction {
    if (project.isDisposed) return@readAction null
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@readAction null
    if (psiFile.isValid) psiFile else null
}

private val hotPreviewAnnotationClassId = ClassId.topLevel(FqName(fqNameHotPreview))
private val composableClassId = ClassId.topLevel(FqName(Composable::class.qualifiedName!!))

private val hotPreviewDefaultValues = HotPreviewModel()

private fun KaAnnotation.toHotPreviewAnnotation(): HotPreviewModel {
    // Build a map
    val map = arguments.associateBy { it.name.toString() }
        .mapValues { (it.value.expression as? KaAnnotationValue.ConstantValue)?.value?.value }
    val dv = hotPreviewDefaultValues
    return HotPreviewModel(
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

suspend fun analyzePsiFile(project: Project, psiFile: PsiFile) = smartReadAction(project) {
    val functionList = mutableListOf<KtNamedFunction>()
    psiFile.accept(object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            functionList.add(function)
        }
    })
    functionList.mapNotNull { function ->
            val annotations = checkFunctionForAnnotation(function)
            if (annotations.isEmpty()) null
            else {
                require(function.valueParameters.isEmpty()) {
                    "Function ${function.name} with @HotPreview annotation must not has parameters! See line: ${function.getLineRange()}"
                }
                HotPreviewFunction(
                    name = function.name ?: "",
                    annotation = annotations,
                    lineRange = function.getLineRange()
                )
            }
        }
    }

fun checkFunctionForAnnotation(function: KtNamedFunction): List<HotPreviewAnnotation> {
    //TODO Find a solution which is also working in dumb mode.
    analyze(function) {
        val mySymbol = function.symbol
        LOG.debug("Function: ${function.name}")
        val hotPreviewAnnotations = mySymbol.annotations
            .filter { it.classId == hotPreviewAnnotationClassId }
            .map {
                HotPreviewAnnotation(
                    lineRange = it.psi?.getLineRange(),
                    annotation = it.toHotPreviewAnnotation()
                )
            }
        val hotPreviewAnnotationClasses = mutableListOf<HotPreviewAnnotation>()
        mySymbol.annotations // Find Annotation classes which contain HotPreview annotation
            .filter { it.classId != composableClassId }
            .filter { it.classId != hotPreviewAnnotationClassId }
            .forEach {
                LOG.debug("Check: $it")
                mySymbol.annotations.forEach { annotation ->
                    val fqn = annotation.classId?.asSingleFqName()
                    LOG.debug("Annotation: $fqn")
                    fqn?.let {
                        val project = function.project
                        val clazz =
                            KotlinFullClassNameIndex.Helper[fqn.toString(), project, GlobalSearchScope.allScope(project)]
                        clazz.forEach {
                            try {
                                it.symbol.annotations
                                    .filter { it.classId == hotPreviewAnnotationClassId }
                                    .forEach {
                                        hotPreviewAnnotationClasses.add(
                                            HotPreviewAnnotation(
                                                lineRange = annotation.psi?.getLineRange(),
                                                annotation = it.toHotPreviewAnnotation()
                                            )
                                        )
                                    }
                            } catch (err: Throwable) {
                                LOG.error(err)
                            }
                        }
                    }
                }
            }
        return hotPreviewAnnotations + hotPreviewAnnotationClasses
    }
}