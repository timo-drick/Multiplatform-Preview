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
import com.intellij.util.concurrency.annotations.RequiresReadLock
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
import kotlin.enums.enumEntries

@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

val hotPreviewAnnotationClassId = ClassId.topLevel(FqName(fqNameHotPreview))
val hotPreviewParameterClassId = ClassId.topLevel(FqName(fqNameHotPreviewParameter))
private val composableClassId = ClassId.topLevel(FqName(Composable::class.qualifiedName!!))

private val hotPreviewDefaultValues = HotPreviewModel()

inline fun <reified T>KaAnnotation.findParameterWithDefault(name: String, defaultValue: T): T =
    arguments.find { it.name.toString() == name }?.expression?.let { expression ->
        val value = (expression as? KaAnnotationValue.ConstantValue)?.value?.value
        value as? T
    } ?: defaultValue

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T: Enum<T>>KaAnnotation.findEnumParameterWithDefault(name: String, defaultValue: T): T =
    arguments.find { it.name.toString() == name }?.expression?.let { expression ->
        val value = (expression as? KaAnnotationValue.EnumEntryValue)?.callableId?.callableName?.identifier?.let { value ->
            enumEntries<T>().find { it.name == value }
        }
        value
    } ?: defaultValue


/*
// Code to find nested annotations but not needed anymore

fun KaAnnotation.findNestedAnnotationParameter(name: String): KaAnnotation? {
    val nestedAnnotation = arguments.find { it.name.toString() == name }?.expression as? NestedAnnotationValue
    return nestedAnnotation?.annotation
}
private fun KaAnnotation.toNavigationBarModel() = NavigationBarConfigModel(
    visibility = findEnumParameterWithDefault("visibility", VisibilityModel.Visible),
    mode = findEnumParameterWithDefault("mode", NavigationModeModel.ThreeButtonBottom),
    contrastEnforced = findParameterWithDefault("contrastEnforced", false)
)
private fun KaAnnotation.toCameraModel() = CameraConfigModel(
    visibility = findEnumParameterWithDefault("visibility", VisibilityModel.Visible),
    cameraPosition = findEnumParameterWithDefault("cameraPosition", CameraPositionModel.Top)
)
private fun KaAnnotation.toCaptionBarModel() = CaptionBarConfigModel(
    visibility = findEnumParameterWithDefault("visibility", VisibilityModel.Visible)
)
 */

private fun KaAnnotation.toHotPreviewAnnotation(): HotPreviewModel {
    // Build a map
    val dv = hotPreviewDefaultValues
    val dpi = findParameterWithDefault<Int?>("dpi", null) // This was in an old version of the HotPreview annotation
    val defaultDensity = if (dpi != null) dpi.toFloat() / 160f else dv.density
    return HotPreviewModel(
        name = findParameterWithDefault("name", dv.name),
        group = findParameterWithDefault("group", dv.group),
        widthDp = findParameterWithDefault("widthDp", dv.widthDp),
        heightDp = findParameterWithDefault("heightDp", dv.heightDp),
        locale = findParameterWithDefault("locale", dv.locale),
        layoutDirectionRTL = findParameterWithDefault("layoutDirectionRTL", dv.layoutDirectionRTL),
        fontScale = findParameterWithDefault("fontScale", dv.fontScale),
        darkMode = findParameterWithDefault("darkMode", dv.darkMode),
        backgroundColor = findParameterWithDefault("backgroundColor", dv.backgroundColor),
        density = findParameterWithDefault("density", defaultDensity),
        statusBar = findParameterWithDefault("statusBar", dv.statusBar),
        navigationBar = findEnumParameterWithDefault("navigationBar", dv.navigationBar),
        navigationBarContrastEnforced = findParameterWithDefault("navigationBarContrastEnforced", dv.navigationBarContrastEnforced),
        displayCutout = findEnumParameterWithDefault("displayCutout", dv.displayCutout),
        captionBar = findParameterWithDefault("captionBar", dv.captionBar),
        inspectionMode = findParameterWithDefault("inspectionMode", dv.inspectionMode)
    )
}

/**
 * This will just find all annotations. It will not search for functions.
 */
suspend fun findHotPreviewAnnotations(project: Project, file: VirtualFile): List<HotPreviewAnnotation> =
    project.analyzeFile(file) { ktFile ->
        ktFile.symbol.fileScope.declarations
            .flatMap { it.annotations }
            .filter { it.classId == hotPreviewAnnotationClassId }
            .mapNotNull {
                it.psi?.getLineRange()?.let { lineRange ->
                    HotPreviewAnnotation(lineRange, it.toHotPreviewAnnotation(), false)
                }
            }
            .toList()
    } ?: emptyList()

private fun findAnnotationArgument(
    argumentNameList: List<String>,
    annotationEntry: KtAnnotationEntry,
    argumentName: String,
): KtValueArgument? {
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
): Boolean = project.analyzeFile(file) { psiFile ->
    val found = psiFile.symbol.fileScope.declarations
        .flatMap { it.annotations }
        .filter { it.classId == hotPreviewAnnotationClassId }
        .firstOrNull { it.psi?.getLineRange()?.first == line }
        ?.let {
            val argumentList = it.arguments.map { it.name.toString() }
            findAnnotationArgument(argumentList, it.psi as KtAnnotationEntry, argumentName)
        }
    found != null
} == true


interface UpdatePsiAnnotationDsl {
    fun string(key: String, value: String?) {
        parameter(key, if (value.isNullOrBlank()) null else "\"$value\"")
    }
    fun parameter(key: String, value: String?)
    fun checkParameter(argumentName: String): Boolean
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
                    val found = psiFile.symbol.fileScope.declarations
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
                                    val argumentList = annotation.valueArgumentList
                                    if (argumentList != null) {
                                        argumentList.addArgument(newArgument)
                                    } else {
                                        // Create a new value argument list and add it to the annotation
                                        val valueArgumentList = factory.createCallArguments("(${argumentName} = ${newValue})")
                                        annotation.add(valueArgumentList)
                                    }
                                }
                            } else {
                                argument?.let { annotation.valueArgumentList?.removeArgument(it) }
                            }
                        }
                    }

                    override fun checkParameter(argumentName: String): Boolean {
                        return if (annotation != null)
                            findAnnotationArgument(argumentList, annotation, argumentName) != null
                        else
                            false
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

private fun KaAnnotation.toHotPreviewParameter(name: String): HotPreviewParameterModel {
    val clazz = arguments
        .find { it.name.toString() == "provider" }
        ?.let { it.expression as KaAnnotationValue.ClassLiteralValue }
    val provider = checkNotNull(clazz?.classId)
    return HotPreviewParameterModel(
        name = name,
        providerClassName = provider.asFqNameString(),
        limit = findParameterWithDefault("limit", Int.MAX_VALUE)
    )
}

fun KaSession.analyzeFunction(project: Project, function: KaFunctionSymbol): HotPreviewFunction? {
    val annotations = checkFunctionForAnnotation(project, function)
    val lineRange = function.psi?.getLineRange()
    return if (annotations.isEmpty() || lineRange == null) null
    else {
        val parameter = function.valueParameters.firstOrNull()?.let { valueParameterSymbol ->
            valueParameterSymbol.annotations.find { it.classId == hotPreviewParameterClassId }?.let { parameterAnnotation ->
                val parameter = parameterAnnotation.toHotPreviewParameter(valueParameterSymbol.name.toString())
                println("Parameter 1 name: ${valueParameterSymbol.name} parameter annotation: $parameter")
                parameter
            }
        }
        //TODO
        /*require(function.valueParameters.isEmpty()) {
            "Function ${function.name} with @HotPreview annotation must not has parameters! See line: $lineRange"
        }*/
        HotPreviewFunction(
            name = function.name.toString(),
            parameter = parameter,
            annotation = annotations,
            lineRange = lineRange
        )
    }
}

/**
 * Unfortunately this approach is unreliable. Sometimes returns no symbols for a file.
 * Not sure why. But can not be used for now!
 */
suspend fun findFunctionsWithHotPreviewAnnotationsNew(project: Project, file: VirtualFile): List<HotPreviewFunction>? =
    project.analyzeFile(file) {  ktFile ->
        println("Analyzing file: ${ktFile.name} valid: ${ktFile.isValid}")
        val symbols = ktFile.symbol.fileScope.callables.toList()
        println("Symbols: ${symbols.size}")
        symbols.filterIsInstance<KaFunctionSymbol>()
            .mapNotNull { function ->
                analyzeFunction(project, function)
            }.toList()
    }

suspend fun findFunctionsWithHotPreviewAnnotations(project: Project, file: VirtualFile): List<HotPreviewFunction> =
    withContext(Dispatchers.Default) {
        project.smartReadActionPsiFile(file) { psiFile ->
            val functionList = mutableListOf<KtNamedFunction>()
            psiFile.accept(object : KtTreeVisitorVoid() {
                override fun visitNamedFunction(function: KtNamedFunction) {
                    functionList.add(function)
                }
            })
            functionList.mapNotNull { function ->
                analyze(function) { analyzeFunction(project, function.symbol) }
            }
        } ?: emptyList()
    }

@RequiresReadLock
fun KaSession.checkFunctionForAnnotation(project: Project, function: KaFunctionSymbol): List<HotPreviewAnnotation> {
    val hotPreviewAnnotations = function.annotations
        .filter { it.classId == hotPreviewAnnotationClassId }
        .mapNotNull {
            it.psi?.getLineRange()?.let { lineRange ->
                HotPreviewAnnotation(
                    lineRange = lineRange,
                    annotation = it.toHotPreviewAnnotation(),
                    isAnnotationClass = false
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
                clazz.find { ktClass ->
                    ktClass.containingFile.name.endsWith(".kt")
                }?.let { ktClass ->
                    try {
                        // Create a new analysis session for this class to avoid context issues
                        analyze(ktClass) {
                            ktClass.symbol.annotations
                                .filter { it.classId == hotPreviewAnnotationClassId }
                                .forEach {
                                    annotation.psi?.getLineRange()?.let { lineRange ->
                                        hotPreviewAnnotationClasses.add(
                                            HotPreviewAnnotation(
                                                lineRange = lineRange,
                                                annotation = it.toHotPreviewAnnotation(),
                                                isAnnotationClass = true
                                            )
                                        )
                                    }
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
    action: KaSession.(KtFile) -> R
) = withContext(Dispatchers.Default) {
    smartReadActionPsiFile(virtualFile) { psiFile ->
        analyze(psiFile as KtFile) {
            action(psiFile)
        }
    }
}
