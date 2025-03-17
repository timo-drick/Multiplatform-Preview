package de.drick.compose.hotpreview.plugin

import com.intellij.codeInspection.reference.EntryPoint
import com.intellij.codeInspection.reference.RefElement
import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import de.drick.compose.hotpreview.plugin.spliteditor.SeamlessEditorWithPreview
import de.drick.compose.hotpreview.plugin.ui.preview_window.HotPreviewAction
import de.drick.compose.hotpreview.plugin.ui.preview_window.HotPreviewViewModel
import de.drick.compose.hotpreview.plugin.ui.preview_window.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import org.jdom.Element
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.testIntegration.framework.KotlinPsiBasedTestFramework.Companion.asKtNamedFunction
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import java.beans.PropertyChangeListener

class HotPreviewSplitEditorProvider : TextEditorWithPreviewProvider(HotPreviewViewProvider()) {
    override fun getEditorTypeId() = "hotpreview-preview-split-editor"
    override fun getPolicy() = FileEditorPolicy.HIDE_OTHER_EDITORS // Maybe just HIDE_DEFAULT? Maybe configurable?
    override fun createSplitEditor(
        firstEditor: TextEditor,
        secondEditor: FileEditor
    ): FileEditor {
        require(secondEditor is HotPreviewView) { "Secondary editor should be HotPreviewView" }
        return SeamlessEditorWithPreview(firstEditor, secondEditor, "Kotlin editor with HotPreview")
    }
    override fun accept(project: Project, file: VirtualFile) = when {
        file.extension != "kt" -> false
        file.getModule(project) == null -> false
        project.useWorkspace { getModule(file)?.isAndroid() == true } -> false
        else -> true
    }
}

class HotPreviewViewProvider : WeighedFileEditorProvider(), AsyncFileEditorProvider {
    override fun accept(project: Project, file: VirtualFile) = file.extension == "kt"

    override suspend fun createFileEditor(
        project: Project,
        file: VirtualFile,
        document: Document?,
        editorCoroutineScope: CoroutineScope
    ): FileEditor = withContext(editorCoroutineScope.coroutineContext) {
        HotPreviewView(project, file, editorCoroutineScope)
    }

    override fun createEditor(project: Project, file: VirtualFile) = HotPreviewView(project, file, GlobalScope)
    override fun getEditorTypeId() = "hotpreview-preview-view"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class HotPreviewView(
    project: Project,
    private val file: VirtualFile,
    private val scope: CoroutineScope
) : UserDataHolder by UserDataHolderBase(), FileEditor {

    val model by lazy {
        HotPreviewViewModel(project, this@HotPreviewView, file, scope)
    }

    @OptIn(ExperimentalJewelApi::class)
    private val mainComponent by lazy {
        enableNewSwingCompositing()
        JewelComposePanel {
            MainScreen(model)
        }
    }

    override fun getName() = "HotPreview"
    override fun getComponent() = mainComponent
    override fun getPreferredFocusedComponent() = mainComponent
    override fun dispose() {}
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) {}
    override fun isModified() = false
    override fun isValid() = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
}

class ReCompileShortcutAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project  = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file) ?: return
        (fileEditor as? SeamlessEditorWithPreview)?.let {
            val previewEditor = it.previewEditor
            if (previewEditor is HotPreviewView) previewEditor.model.onAction(HotPreviewAction.Refresh)
        }
    }
}

/**
 * [EntryPoint] implementation to mark `@HotPreview` functions as entry points and avoid them being flagged as unused.
 *
 * Based on
 * com.android.tools.idea.compose.preview.PreviewEntryPoint from AOSP
 * with modifications
 */
class HotPreviewEntryPoint : EntryPoint() {
    private var ADD_PREVIEW_TO_ENTRIES: Boolean = true

    override fun isEntryPoint(refElement: RefElement, psiElement: PsiElement): Boolean = isEntryPoint(psiElement)
    override fun isEntryPoint(psiElement: PsiElement): Boolean =
        psiElement is PsiMethod && checkForAnnotationClass(psiElement)
    override fun readExternal(element: Element) = element.deserializeInto(this)
    override fun writeExternal(element: Element) {
        serializeObjectInto(this, element)
    }
    override fun getDisplayName(): String = "Compose Preview"
    override fun isSelected(): Boolean = ADD_PREVIEW_TO_ENTRIES
    override fun setSelected(selected: Boolean) {
        this.ADD_PREVIEW_TO_ENTRIES = selected
    }

    private fun checkForAnnotationClass(psiMethod: PsiMethod): Boolean {
        if (psiMethod.hasAnnotation(fqNameHotPreview)) return true
        psiMethod.asKtNamedFunction()?.let {
            return analyze(it) { checkFunctionForAnnotation(psiMethod.project, it.symbol).isNotEmpty() }
        }
        return false
    }
}