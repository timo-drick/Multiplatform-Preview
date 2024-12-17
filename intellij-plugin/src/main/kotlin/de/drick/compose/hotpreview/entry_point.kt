package de.drick.compose.hotpreview

import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.k2.codeinsight.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.beans.PropertyChangeListener


class HotPreviewSplitEditor(
    editor: TextEditor,
    preview: HotPreviewView,
) : TextEditorWithPreview(
    editor, preview, "Kotlin editor with HotPreview", Layout.SHOW_EDITOR_AND_PREVIEW
)


// Source of file inspection from Jetbrains sources:
// https://github.com/JetBrains/android/blob/master/intellij.android.compose-common/src/com/android/tools/compose/inspection/BasePreviewAnnotationInspection.kt


class HotPreviewSplitEditorProvider : TextEditorWithPreviewProvider(HotPreviewViewProvider()) {
    override fun getEditorTypeId() = "hotpreview-preview-split-editor"
    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
    override fun createSplitEditor(
        firstEditor: TextEditor,
        secondEditor: FileEditor
    ): FileEditor {
        require(secondEditor is HotPreviewView) { "Secondary editor should be HotPreviewView" }
        return HotPreviewSplitEditor(firstEditor, secondEditor)
    }
    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (file.extension != "kt") return false
        return kotlinFileHasHotPreview(file)
        /*var annotationFound = false
        PsiManager.getInstance(project).findFile(file)?.let { psiFile ->
            /*psiFile.accept(object : KotlinRecursiveElementVisitor() {
                /*override fun visitImportList(importList: KtImportList) {
                    importList.imports.forEach {
                        println("Import: ${it.name}")
                    }
                }*/
            })*/
            psiFile.accept(object : KotlinRecursiveElementVisitor() {
                override fun visitNamedFunction(function: KtNamedFunction) {
                    val annotations = function.annotations
                    val functionName = function.name
                    val found = function.annotationEntries.any { annotationEntry ->
                        val name = annotationEntry.name
                        val shortName = annotationEntry.shortName
                        val type = annotationEntry.elementType
                        val typeRef = annotationEntry.typeReference
                        false
                    }
                }
            })
        }
        return annotationFound

         */
        //return true//checkedFile?.language == "Kotlin"
        //return true
    }
}

class HotPreviewViewProvider : WeighedFileEditorProvider() { //, AsyncFileEditorProvider {
    override fun accept(project: Project, file: VirtualFile) = file.extension == "kt"

    /*override suspend fun createFileEditor(
        project: Project,
        file: VirtualFile,
        document: Document?,
        editorCoroutineScope: CoroutineScope
    ): FileEditor = withContext(Dispatchers.Unconfined) {
        HotPreviewView(project, file)
    }*/

    override fun createEditor(
        project: Project,
        file: VirtualFile,
    ): FileEditor {
        return HotPreviewView(project, file)
    }

    override fun getEditorTypeId() = "hotpreview-preview-view"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class HotPreviewView(
    project: Project,
    private val file: VirtualFile
) : UserDataHolder by UserDataHolderBase(), FileEditor {

    private val mainComponent by lazy { HotPreviewWindow(project = project, file = file) }

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

class HotPreviewWindow(
    private val project: Project,
    private val file: VirtualFile
) : BorderLayoutPanel(), Disposable {
    val log = Logger.getInstance(HotPreviewWindow::class.java)
    init {

        /*PsiManager.getInstance(project).findFile(file)?.let { file ->
            file.accept(object : KotlinRecursiveElementVisitor() {
                override fun visitNamedFunction(function: KtNamedFunction) {
                    println("Function: ${function.fqName}")
                    function.accept(object : KotlinRecursiveElementVisitor() {
                        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                            println("Annotation entry: ${annotationEntry.name}")
                        }
                    })
                }
            })
        }*/
        val composePanel = ComposePanel().apply {
            setContent {
                MainScreen(project, file)
            }
        }
        composePanel.bounds = bounds
        add(composePanel)
    }

    override fun dispose() {
        println("Disposed")
    }
}
