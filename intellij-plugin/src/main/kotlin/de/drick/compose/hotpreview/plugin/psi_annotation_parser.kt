package de.drick.compose.hotpreview.plugin

import androidx.compose.runtime.Composable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.readAndWriteAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.idea.debugger.core.stepping.getLineRange
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.argumentIndex
import kotlin.collections.plus

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

val hotPreviewAnnotationClassId = ClassId.topLevel(FqName(fqNameHotPreview))
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

suspend fun findHotPreviewAnnotations(project: Project, file: VirtualFile): List<HotPreviewAnnotation> =
    project.analyzeFile(file) { ktFile ->
        ktFile.symbol.fileScope.declarations
            .flatMap { it.annotations }
            .filter { it.classId == hotPreviewAnnotationClassId }
            .mapNotNull {
                it.psi?.getLineRange()?.let { lineRange ->
                    HotPreviewAnnotation(lineRange, it.toHotPreviewAnnotation())
                }
            }
            .toList()
    } ?: emptyList()

private fun findAnnotationArgument(
    argumentNameList: List<String>,
    annotationEntry: KtAnnotationEntry,
    argumentName: String,
): KtValueArgument? {
    //call?.symbol?.valueParameters?.getOrNull(1)?.name?.asString()
    return annotationEntry.valueArgumentList?.arguments?.find {
        val name = it.getArgumentName()?.asName?.asString() ?: argumentNameList[it.argumentIndex]
        argumentName == name
    }
}

suspend fun checkAnnotationParameter(
    project: Project,
    file: VirtualFile,
    line: Int,  // Line number where the Annotation starts
    argumentName: String,
): Boolean = getPsiFileSafely(project, file)?.let { psiFile ->
    smartReadAction(project) {
        analyze(psiFile as KtFile) {
            val found = psiFile.symbol.fileScope.declarations
                .flatMap { it.annotations }
                .filter { it.classId == hotPreviewAnnotationClassId }
                .firstOrNull { it.psi?.getLineRange()?.first == line }
                ?.let {
                    val argumentList = it.arguments.map { it.name.toString() }
                    findAnnotationArgument(argumentList, it.psi as KtAnnotationEntry, argumentName)
                }
            found != null
        }
    }
} == true


interface UpdatePsiAnnotationDsl {
    fun string(key: String, value: String) {
        parameter(key, if (value.isNotBlank()) "\"$value\"" else null)
    }
    fun parameter(key: String, value: String?)
}

class AnnotationUpdate(
    val project: Project,
    val file: VirtualFile,
    val line: Int,  // Line number where the Annotation starts
) {
    suspend fun updateDsl(block: UpdatePsiAnnotationDsl.() -> Unit) {
        getPsiFileSafely(project, file)?.let { psiFile ->
            readAndWriteAction {
                val (annotation: KtAnnotationEntry?, argumentList: List<String>) = analyze(psiFile as KtFile) {
                    val found = psiFile.symbol.fileScope.getAllSymbols()
                        .flatMap { it.annotations }
                        .filter { it.classId == hotPreviewAnnotationClassId }
                        .firstOrNull { it.psi?.getLineRange()?.first == line }
                    val entry = found?.psi as? KtAnnotationEntry
                    val argumentList = found?.arguments?.map { it.name.toString() } ?: emptyList()
                    Pair(entry, argumentList)
                }
                val factory = KtPsiFactory(project)
                //println("Found: $annotation")
                val dsl = object : UpdatePsiAnnotationDsl {
                    override fun parameter(argumentName: String, newValue: String?) {
                        if (annotation != null) {
                            val argument = findAnnotationArgument(argumentList, annotation, argumentName)
                                //annotation.valueArgumentList?.arguments?.find { it.getArgumentName()?.asName?.asString() == argumentName }
                            if (newValue != null) {
                                val newExpression = factory.createExpression(newValue)
                                if (argument != null) {
                                    argument.getArgumentExpression()?.replace(newExpression)
                                } else {
                                    val newArgument = factory.createArgument(
                                        newExpression,
                                        Name.identifier(argumentName)
                                    )
                                    annotation.valueArgumentList?.addArgument(newArgument)
                                }
                            } else {
                                argument?.let { annotation.valueArgumentList?.removeArgument(it) }
                            }
                        }
                    }
                }
                writeAction {
                    WriteCommandAction.runWriteCommandAction(project) {
                        block(dsl)
                    }
                }
            }
        }
    }
}


suspend fun findFunctionsWithHotPreviewAnnotations(project: Project, file: VirtualFile): List<HotPreviewFunction> =
    project.analyzeFile(file) {  ktFile ->
        ktFile.symbol.fileScope.getAllSymbols()
            .filterIsInstance<KaFunctionSymbol>()
            .mapNotNull { function ->
                val annotations = checkFunctionForAnnotation(project, function)
                val lineRange = function.psi?.getLineRange()
                if (annotations.isEmpty() || lineRange == null) null
                else {
                    require(function.valueParameters.isEmpty()) {
                        "Function ${function.name} with @HotPreview annotation must not has parameters! See line: $lineRange"
                    }
                    HotPreviewFunction(
                        name = function.name.toString(),
                        annotation = annotations,
                        lineRange = lineRange
                    )
                }
            }.toList()
    } ?: emptyList()

suspend fun findFunctionsWithHotPreviewAnnotationsOld(project: Project, file: VirtualFile): List<HotPreviewFunction> =
    withContext(Dispatchers.Default) {
        getPsiFileSafely(project, file)?.let { psiFile ->
            LOG.debug("Find preview annotations for: $file")
            return@withContext analyzeFunctionsInPsiFile(project, psiFile)
        }
        emptyList()
    }

suspend fun analyzeFunctionsInPsiFile(project: Project, psiFile: PsiFile) = smartReadAction(project) {
    val functionList = mutableListOf<KtNamedFunction>()
    psiFile.accept(object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            functionList.add(function)
        }
    })
    functionList.mapNotNull { function ->
        val annotations = analyze(function) { checkFunctionForAnnotation(project, function.symbol) }
        val lineRange = function.getLineRange()
        if (annotations.isEmpty() || lineRange == null) null
        else {
            require(function.valueParameters.isEmpty()) {
                "Function ${function.name} with @HotPreview annotation must not has parameters! See line: ${function.getLineRange()}"
            }
            HotPreviewFunction(
                name = function.name ?: "",
                annotation = annotations,
                lineRange = lineRange
            )
        }
    }
}

fun KaSession.checkFunctionForAnnotation(project: Project, function: KaFunctionSymbol): List<HotPreviewAnnotation> {
    //TODO Find a solution which is also working in dumb mode.
    LOG.debug("Function: ${function.name}")
    val hotPreviewAnnotations = function.annotations
        .filter { it.classId == hotPreviewAnnotationClassId }
        .mapNotNull {
            it.psi?.getLineRange()?.let { lineRange ->
                HotPreviewAnnotation(
                    lineRange = lineRange,
                    annotation = it.toHotPreviewAnnotation()
                )
            }
        }
    val hotPreviewAnnotationClasses = mutableListOf<HotPreviewAnnotation>()
    //val module = checkNotNull(function.module)
    //val searchScope = GlobalSearchScope.moduleScope(module)
    val searchScope = GlobalSearchScope.everythingScope(project)
    function.annotations // Find Annotation classes which contain HotPreview annotation
        .filter { it.classId != composableClassId }
        .filter { it.classId != hotPreviewAnnotationClassId }
        .forEach { annotation ->
            //println("   Check: ${annotation.classId}")
            val fqn = annotation.classId?.asSingleFqName()
            //println("      Annotation: $fqn")
            fqn?.let {
                val clazz = KotlinFullClassNameIndex[fqn.toString(), project, searchScope]
                clazz.find {
                    it.symbol.psi?.containingFile?.name?.endsWith(".kt") == true
                }?.let {
                    try {
                        it.symbol.annotations
                            .filter { it.classId == hotPreviewAnnotationClassId }
                            .forEach {
                                annotation.psi?.getLineRange()?.let { lineRange ->
                                    hotPreviewAnnotationClasses.add(
                                        HotPreviewAnnotation(
                                            lineRange = lineRange,
                                            annotation = it.toHotPreviewAnnotation()
                                        )
                                    )
                                }
                            }
                    } catch (err: Throwable) {
                        LOG.debug(err)
                    }
                }
            }
        }
    //println("---")
    //println()
    return hotPreviewAnnotations + hotPreviewAnnotationClasses
}

suspend fun getPsiFileSafely(project: Project, virtualFile: VirtualFile): PsiFile? = readAction {
    if (project.isDisposed) return@readAction null
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@readAction null
    if (psiFile.isValid) psiFile else null
}
suspend fun <R>Project.smartReadActionPsiFile(
    virtualFile: VirtualFile,
    block: (PsiFile) -> R
): R? = smartReadAction(this) {
    if (this.isDisposed) return@smartReadAction null
    val psiFile = PsiManager.getInstance(this).findFile(virtualFile) ?: return@smartReadAction null
    if (psiFile.isValid) block(psiFile) else null
}

suspend fun <R>Project.analyzeFile(
    virtualFile: VirtualFile,
    action: org.jetbrains.kotlin.analysis.api.KaSession.(KtFile) -> R
) = withContext(Dispatchers.Default) {
    smartReadActionPsiFile(virtualFile) { psiFile ->
        analyze(psiFile as KtFile) {
            action(psiFile)
        }
    }
}