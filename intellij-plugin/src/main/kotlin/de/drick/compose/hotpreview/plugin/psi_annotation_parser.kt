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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

inline fun <reified T>Map<String, Any?>.withDefault(key: String, defaultValue: T): T =
    this[key] as? T ?: defaultValue

private fun KaAnnotation.toHotPreviewAnnotation(): HotPreviewModel {
    // Build a map
    val map = arguments.associateBy { it.name.toString() }
        .mapValues { (it.value.expression as? KaAnnotationValue.ConstantValue)?.value?.value }
    val dv = hotPreviewDefaultValues
    val dpi = map["dpi"] as? Int?
    val defaultDensity = if (dpi != null) dpi.toFloat() / 160f else dv.density
    return HotPreviewModel(
        name = map.withDefault("name", dv.name),
        group = map.withDefault("group", dv.group),
        widthDp = map.withDefault("widthDp", dv.widthDp),
        heightDp = map.withDefault("heightDp", dv.heightDp),
        locale = map.withDefault("locale", dv.locale),
        fontScale = map.withDefault("fontScale", dv.fontScale),
        darkMode = map.withDefault("darkMode", dv.darkMode),
        density = map.withDefault("density", defaultDensity)
    )
}

suspend fun findPreviewAnnotations(project: Project, file: VirtualFile): List<HotPreviewFunction> =
    withContext(Dispatchers.Default) {
        getPsiFileSafely(project, file)?.let { psiFile ->
            LOG.debug("Find preview annotations for: $file")
            return@withContext analyzePsiFile(project, psiFile)
        }
        emptyList()
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
                                LOG.debug(err)
                            }
                        }
                    }
                }
            }
        return hotPreviewAnnotations + hotPreviewAnnotationClasses
    }
}