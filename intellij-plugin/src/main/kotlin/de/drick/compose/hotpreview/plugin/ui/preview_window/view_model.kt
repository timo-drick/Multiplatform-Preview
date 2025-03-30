package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import de.drick.compose.hotpreview.plugin.ClassPathService
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import de.drick.compose.hotpreview.plugin.HotPreviewFunction
import de.drick.compose.hotpreview.plugin.HotPreviewView
import de.drick.compose.hotpreview.plugin.NotRenderedYet
import de.drick.compose.hotpreview.plugin.RenderCacheKey
import de.drick.compose.hotpreview.plugin.RenderService
import de.drick.compose.hotpreview.plugin.RenderState
import de.drick.compose.hotpreview.plugin.executeGradleTask
import de.drick.compose.hotpreview.plugin.findFunctionsWithHotPreviewAnnotations
import de.drick.compose.hotpreview.plugin.findHotPreviewAnnotations
import de.drick.compose.hotpreview.plugin.getParameterList
import de.drick.compose.hotpreview.plugin.runCatchingCancellationAware
import de.drick.compose.hotpreview.plugin.spliteditor.SeamlessEditorWithPreview
import de.drick.compose.hotpreview.plugin.tools.PluginPersistentStore
import de.drick.compose.hotpreview.plugin.ui.guttericon.HotPreviewGutterIcon
import de.drick.compose.hotpreview.plugin.ui.HotPreviewSettings
import de.drick.compose.hotpreview.plugin.ui.HotPreviewSettingsConfigurable
import de.drick.compose.hotpreview.plugin.ui.guttericon.GutterIconViewModel
import de.drick.compose.hotpreview.plugin.ui.guttericon.GutterIconViewModelI
import de.drick.compose.hotpreview.plugin.useSuspendWorkspace
import de.drick.compose.utils.lazySuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("UnstableApiUsage")
private val LOG = fileLogger()

data class UIHotPreviewData(
    val functionName: String,
    val lineRange: IntRange,
    val annotations: List<UIAnnotation>
)

data class UIAnnotation(
    val name: String,
    val lineRange: IntRange,
    val renderCacheKey: RenderCacheKey,
    val isAnnotationClass: Boolean
)

data class UIFunctionAnnotation(
    val functionName: String,
    val annotation: UIAnnotation
)

class UIRenderState(
    widthDp: Int = -1,
    heightDp: Int = -1
) {
    var state: RenderState by mutableStateOf(NotRenderedYet(widthDp, heightDp))
}

sealed interface HotPreviewAction {
    data object OpenSettings : HotPreviewAction
    data object ToggleLayout : HotPreviewAction
    data object Refresh : HotPreviewAction
    data class SelectGroup(val group: String?) : HotPreviewAction
    data class SelectTab(val tabIndex: Int) : HotPreviewAction
    data class ChangeScale(val newScale: Float) : HotPreviewAction
    data class NavigateCodeLine(val line: Int) : HotPreviewAction
    data class MonitorChanges(val scope: CoroutineScope) : HotPreviewAction

}

interface HotPreviewViewModelI {
    val scale: Float
    val isPureTextEditor: Boolean
    val compilingInProgress: Boolean
    val errorMessage: Throwable?
    val previewList: List<UIHotPreviewData>
    val groups: Set<String>
    val selectedGroup: String?
    val selectedTab: Int? // null when in gridlayout mode
    fun onAction(action: HotPreviewAction)
    fun requestPreviews(keys: Set<RenderCacheKey>): Map<RenderCacheKey, UIRenderState>
    fun getGutterIconViewModel(annotation: UIAnnotation): GutterIconViewModelI
}

class HotPreviewViewModel(
    private val project: Project,
    private val previewEditor: HotPreviewView,
    private val file: VirtualFile,
    private val scope: CoroutineScope
): HotPreviewViewModelI {

    private val splitEditor
        get() = requireNotNull(TextEditorWithPreview.getParentSplitEditor(previewEditor) as? SeamlessEditorWithPreview)
    private val textEditor
        get() = splitEditor.textEditor

    val settings = HotPreviewSettings.getInstance().state

    private val classPathService by lazySuspend {
        ClassPathService.Companion.getInstance(project, file)
    }
    private val renderService = RenderService()

    private val properties = PluginPersistentStore(project, file)

    private val scaleProperty = properties.float("scale", 1f)
    override var scale: Float by mutableStateOf(scaleProperty.get())
        private set

    private val selectGroupProperty = properties.stringNA("selected_group", null)
    override var selectedGroup by mutableStateOf(selectGroupProperty.get())
        private set

    private val isGridLayoutProperty = properties.boolean("is_grid_layout", true)
    private var isGridLayout = isGridLayoutProperty.get()
    override var selectedTab by mutableStateOf<Int?>(if (isGridLayout) null else 0)
        private set

    override var groups: Set<String> by mutableStateOf(emptySet())
        private set

    override var isPureTextEditor by mutableStateOf(splitEditor.isPureTextEditor)
    override var compilingInProgress by mutableStateOf(false)
    override var errorMessage: Throwable? by mutableStateOf(null)
    override var previewList: List<UIHotPreviewData> by mutableStateOf(emptyList())

    private var compileCounter = 0

    override fun onAction(action: HotPreviewAction) {
        when (action) {
            HotPreviewAction.OpenSettings -> openSettings()
            HotPreviewAction.Refresh -> refresh()
            HotPreviewAction.ToggleLayout -> toggleLayout()
            is HotPreviewAction.ChangeScale -> changeScale(action.newScale)
            is HotPreviewAction.MonitorChanges -> monitorChanges(action.scope)
            is HotPreviewAction.NavigateCodeLine -> navigateCodeLine(action.line)
            is HotPreviewAction.SelectGroup -> selectGroup(action.group)
            is HotPreviewAction.SelectTab -> selectTab(action.tabIndex)
        }
    }

    private fun selectGroup(group: String?) {
        selectGroupProperty.set(group)
        val previousSelected = selectedGroup
        selectedGroup = group
        if (previousSelected != group) {
            scope.launch {
                render()
            }
        }
    }

    private fun selectTab(tabIndex: Int) {
        selectedTab = tabIndex
        scope.launch {
            render()
        }
    }

    private fun updateGroups(previewFunctions: List<HotPreviewFunction>) {
        groups = previewFunctions.flatMap { function ->
            function.annotation.groupBy { it.annotation.group }.keys.filter { it.isNotBlank() }
        }.toSet()
        if (groups.contains(selectedGroup).not()) {
            selectGroup(null)
        }
    }

    private suspend fun updatePreviewList(previewFunctions: List<HotPreviewFunction>) {
        val classLoader = classPathService.get().getRenderClassLoaderInstance(compileCounter)
        previewList = previewFunctions.map { function ->
            println("Update function: $function")
            val annotations = function.annotation.flatMap {
                val parameterList: List<*> = function.parameter?.let { classLoader.getParameterList(it) } ?: listOf(null)
                parameterList.mapIndexed { index, parameter ->
                    val pName = function.parameter?.let { " ${function.parameter.name}: $index" } ?: ""
                    val name = "${it.annotation.name}$pName"
                    val key = RenderCacheKey(function.name, parameter, it.annotation)
                    UIAnnotation(
                        name = name,
                        lineRange = it.lineRange,
                        renderCacheKey = key,
                        isAnnotationClass = it.isAnnotationClass
                    )
                }
            }
            UIHotPreviewData(
                functionName = function.name,
                lineRange = function.lineRange,
                annotations = annotations
            )
        }
    }

    private fun changeScale(newScale: Float) {
        scaleProperty.set(newScale)
        scale = newScale
    }

    private fun toggleLayout() {
        isGridLayout = isGridLayout.not()
        isGridLayoutProperty.set(isGridLayout)
        if (isGridLayout) {
            selectedTab = null
        } else {
            selectedTab = 0
        }
    }

    private fun navigateCodeLine(line: Int) {
        val pos = LogicalPosition(line, 0)
        textEditor.editor.apply {
            caretModel.moveToLogicalPosition(pos)
            scrollingModel.scrollTo(pos, ScrollType.MAKE_VISIBLE)
        }
    }

    private fun refresh() {
        scope.launch {
            classPathService.reset()
            compilingInProgress = true
            errorHandling {
                val functions = analyzePreviewAnnotations()
                if (functions.isNotEmpty()) {
                    project.useSuspendWorkspace {
                        val module = requireNotNull(getModule(file)) { "No module found for file: ${file.name}" }
                        val desktopModule = getJvmTargetModule(module)
                        val gradleTask = getGradleTaskName(desktopModule)
                        val path = requireNotNull(getModulePath(module)) { "No module path found!" }
                        println("task: $gradleTask path: $path")
                        val parameters = if (settings.gradleParametersEnabled) settings.gradleParameters else ""
                        executeGradleTask(project, gradleTask, parameters, path)
                        compileCounter++
                    }
                    classPathService.reset()
                }
                updatePreviewList(functions)
                render()
            }
            compilingInProgress = false
        }
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, HotPreviewSettingsConfigurable::class.java)
    }

    private fun monitorChanges(scope: CoroutineScope) {
        scope.launch {
            compilingInProgress = true
            errorHandling {
                updatePreviewList(analyzePreviewAnnotations())
                render()
            }
            compilingInProgress = false
        }
        subscribeForFileChanges(scope) {
            scope.launch {
                if (settings.recompileOnSave) {
                    refresh()
                } else {
                    errorHandling {
                        updatePreviewList(analyzePreviewAnnotations())
                        render()
                    }
                }
            }
        }
    }

    private fun setPureTextEditorMode(pureTextEditorMode: Boolean) {
        isPureTextEditor = pureTextEditorMode
        splitEditor.isPureTextEditor = isPureTextEditor
    }

    private suspend fun analyzePreviewAnnotations(): List<HotPreviewFunction> {
        val previewFunctions = findFunctionsWithHotPreviewAnnotations(project, file)
        //println("Preview functions found: ${previewFunctions.size}")
        setPureTextEditorMode(previewFunctions.isEmpty())
        updateGroups(previewFunctions)
        updateGutterIcons()
        return previewFunctions
    }

    private val updatePreviewAnnotations: () -> Unit = {
        scope.launch {
            errorHandling {
                updatePreviewList(analyzePreviewAnnotations())
                render()
            }
        }
    }

    private suspend fun updateGutterIcons() {
        val annotations = findHotPreviewAnnotations(project, file)
        withContext(Dispatchers.Main) {
            val editor = textEditor.editor
            editor.markupModel.allHighlighters
                .filter { it.gutterIconRenderer is HotPreviewGutterIcon }
                .forEach { editor.markupModel.removeHighlighter(it) }
            if (settings.showGutterIcon) {
                annotations.forEach { annotation ->
                    val highlighter =
                        editor.markupModel.addLineHighlighter(annotation.lineRange.first, 0, TextAttributes())
                    highlighter.gutterIconRenderer =
                        HotPreviewGutterIcon(project, file, annotation, groups, requestRender = updatePreviewAnnotations)
                }
            }
        }
    }

    override fun getGutterIconViewModel(annotation: UIAnnotation): GutterIconViewModelI {
        val annotation = HotPreviewAnnotation(
            lineRange = annotation.lineRange,
            annotation = annotation.renderCacheKey.annotation,
            isAnnotationClass = annotation.isAnnotationClass
        )
        return GutterIconViewModel(
            project = project,
            file = file,
            annotation = annotation,
            groups = groups,
            requestRender = updatePreviewAnnotations
        )
    }

    private suspend fun errorHandling(block: suspend () -> Unit) {
        runCatchingCancellationAware {
            block()
            errorMessage = null
        }.onFailure { err ->
            errorMessage = err
            LOG.warn(err)
        }
    }


    override fun requestPreviews(keys: Set<RenderCacheKey>): Map<RenderCacheKey, UIRenderState> {
        val previews = renderService.requestPreviews(keys)
        scope.launch {
            render()
        }
        return previews
    }
    suspend fun render() {
        errorHandling { //TODO maybe handle error so that it can be displayed in the render state
            renderService.render(classPathService.get().getRenderClassLoaderInstance(compileCounter))
        }
    }
    private fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit) {
        project.messageBus.connect(scope).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    // Check file is part of the current module
                    //TODO check for source folders
                    var changedKotlinFile = false
                    events.forEach { event ->
                        event.file?.let { file ->
                            if (file.extension == "kt") {
                                LOG.debug("File event: $event")
                                changedKotlinFile = true
                            }
                        }
                    }
                    if (changedKotlinFile) onChanged()
                }
            }
        )
    }

    //TODO
    private fun subscribeForFileEditing() {
        FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                LOG.debug("Document event: $event")
            }
        })
    }
}
