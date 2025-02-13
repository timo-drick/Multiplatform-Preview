package de.drick.compose.hotpreview.plugin

import androidx.compose.ui.awt.ComposePanel
import com.intellij.codeInspection.reference.EntryPoint
import com.intellij.codeInspection.reference.RefElement
import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.ui.components.BorderLayoutPanel
import de.drick.compose.hotpreview.plugin.spliteditor.SeamlessEditorWithPreview
import de.drick.compose.hotpreview.plugin.ui.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jdom.Element
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.kotlin.idea.base.util.isAndroidModule
import org.jetbrains.kotlin.idea.testIntegration.framework.KotlinPsiBasedTestFramework.Companion.asKtNamedFunction
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
    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (file.extension != "kt") return false
        return runBlocking {
                val analyzer = ProjectAnalyzer(project)
                val fileModule = analyzer.getModule(file)
            fileModule != null && !fileModule.isAndroidModule() // We do not want to override the default android preview
        }
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
        HotPreviewView(project, file)
    }

    override fun createEditor(project: Project, file: VirtualFile) = HotPreviewView(project, file)
    override fun getEditorTypeId() = "hotpreview-preview-view"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS
}

class HotPreviewView(
    project: Project,
    private val file: VirtualFile
) : UserDataHolder by UserDataHolderBase(), FileEditor {
    @OptIn(ExperimentalJewelApi::class)
    private val mainComponent by lazy {
        val model = HotPreviewViewModel(project, this, file)
        ComposePanel().apply {
            setContent {
                SwingBridgeTheme {
                    MainScreen(model)
                }
            }
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
            return checkFunctionForAnnotation(it).isNotEmpty()
        }
        return false
    }
}