package de.drick.compose.hotpreview.plugin.spliteditor

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DynamicActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.LayoutActionsFloatingToolbar
import com.intellij.openapi.fileEditor.SplitEditorToolbar
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.ui.components.JBLayeredPane
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel


class SeamlessEditorWithPreview(
    myEditor: TextEditor,
    myPreview: FileEditor,
    name: String = "SeamlessTextEditorWithPreview",
    defaultLayout: Layout = Layout.SHOW_EDITOR_AND_PREVIEW,
) : TextEditorWithPreview(myEditor, myPreview, name, defaultLayout) {


    private val actionGroup = HideAbleActionGroup(arrayOf(
        showEditorAction,
        showEditorAndPreviewAction,
        showPreviewAction,
    ))

    private var mainComponent: JComponent? = null
    private var toolbarComponent: JComponent? = null

    override fun getComponent(): JComponent {
        val component = super.getComponent()
        if (!isShowActionsInTabs && toolbarComponent == null) {
            toolbarComponent = if (isShowFloatingToolbar) {
                UIUtil.findComponentOfType(mainComponent, LayoutActionsFloatingToolbar::class.java)
            } else {
                UIUtil.findComponentOfType(mainComponent, SplitEditorToolbar::class.java)
            }
        }
        mainComponent = component
        return component
    }

    override fun getTabActions(): ActionGroup = actionGroup // in tab toolbar mode
    override fun createViewActionGroup(): ActionGroup =  actionGroup // in floating toolbar mode

    private var pureTextModeWasOn = false

    var isPureTextEditor: Boolean = false
        set(value) {
            actionGroup.setVisible(!value)
            val container = mainComponent
            val toolbar = toolbarComponent
            if (container != null && toolbar != null && !isShowActionsInTabs) {
                when (container) {
                    is JBLayeredPane -> {
                        container.setLayer(toolbar, if (value) 0 else JBLayeredPane.POPUP_LAYER)
                    }
                    is JPanel -> {
                        if (value) {
                            container.remove(toolbar)
                        } else if (container.components.find { it == toolbar } == null) {
                            container.add(toolbar, BorderLayout.NORTH)
                        }
                    }
                }
            }

            if (value) { // is pure text mode
                pureTextModeWasOn = true
                setLayout(Layout.SHOW_EDITOR)
            } else {
                if (pureTextModeWasOn) {
                    pureTextModeWasOn = false
                    setLayout(Layout.SHOW_EDITOR_AND_PREVIEW)
                }
            }
            field = value
        }
}

private class HideAbleActionGroup(
    private val actions: Array<AnAction>
) : ActionGroup(), DynamicActionGroup {
    private var enabledActions = actions
    override fun getChildren(e: AnActionEvent?): Array<AnAction> = enabledActions
    fun setVisible(isVisible: Boolean) {
        enabledActions = if (isVisible) actions else emptyArray()
    }
}
