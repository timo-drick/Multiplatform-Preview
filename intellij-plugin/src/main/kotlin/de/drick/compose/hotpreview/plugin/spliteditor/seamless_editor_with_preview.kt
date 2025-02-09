package de.drick.compose.hotpreview.plugin.spliteditor

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DynamicActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import java.awt.Component
import java.awt.Container


private class HideAbleActionGroup(
    private val actions: Array<AnAction>
) : ActionGroup(), DynamicActionGroup {
    private var enabledActions = actions

    override fun getChildren(e: AnActionEvent?): Array<AnAction> = enabledActions

    fun setVisible(isVisible: Boolean) {
        enabledActions = if (isVisible) actions else emptyArray()
    }
}

class SeamlessEditorWithPreview(
    myEditor: TextEditor,
    myPreview: FileEditor,
    name: String = "SeamlessTextEditorWithPreview",
    defaultLayout: Layout = Layout.SHOW_EDITOR_AND_PREVIEW,
) : TextEditorWithPreview(myEditor, myPreview, name, defaultLayout) {

    private var toolbarComponent: ActionToolbar? = null

    /*override fun getComponent(): JComponent {
        val mainComponent = super.getComponent()
        //toolbarComponent = mainComponent.findFirst { it is SplitEditorToolbar }
        toolbarComponent = UIUtil.findComponentOfType(mainComponent, SplitEditorToolbar::class.java)
        println("Toolbar found: $toolbarComponent")
        //TODO search for toolbar component
        return mainComponent
    }*/

    private var actionGroup: HideAbleActionGroup? = null

    override fun getTabActions(): ActionGroup {
        val group = HideAbleActionGroup(arrayOf(
            showEditorAction,
            showEditorAndPreviewAction,
            showPreviewAction,
        ))
        actionGroup = group
        return group//ActionGroup.EMPTY_GROUP//super.getTabActions()
    }

    override fun createLeftToolbarActionGroup(): ActionGroup? {
        val leftToolbar = super.createLeftToolbarActionGroup()
        println("createLeftToolbarActionGroup $leftToolbar")
        return leftToolbar
    }

    override fun createRightToolbar(): ActionToolbar {
        println("createRightToolbar")
        val toolbar = super.createRightToolbar()
        toolbarComponent = toolbar
        return toolbar
    }

    override fun createRightToolbarActionGroup(): ActionGroup? {
        val actionGroup = super.createRightToolbarActionGroup()
        println("createRightToolbarActionGroup $actionGroup")
        return actionGroup
    }

    var isPureTextEditor: Boolean = true
        set(value) {
            println("Set pure text editor: $value")
            println("ActionGroup: $actionGroup")
            //toolbarComponent?.component?.isEnabled = !value
            actionGroup?.setVisible(!value)
            if (value) {
                setLayout(Layout.SHOW_EDITOR)
            }
            field = value
        }

}

fun Component.findFirst(filter: (Component) -> Boolean): Component? {
    println("find in component: ${this.hashCode()} ${this.javaClass.name}")
    val self = this
    if (filter(self)) return this
    if (self is Container) {
        val components = self.components
        //println("Search in container size: ${components.size}")
        components.filter { self != it }.forEach { component ->
            //println("Open component: ${component.hashCode()}")
            component.findFirst(filter)?.let {
                return it
            }
        }
    }
    return null
}

fun Component.findFirstUpStream(filter: (Component) -> Boolean): Component? {
    println("find in component: ${this.hashCode()} ${this.javaClass.name}")
    val self = this
    if (filter(self)) return this
    if (parent is Container) {
        val components = parent.components
        //println("Search in container size: ${components.size}")
        components.filter { self != it }.forEach { component ->
            //println("Open component: ${component.hashCode()}")
            component.findFirst(filter)?.let {
                return it
            }
        }
    }
    return null
}