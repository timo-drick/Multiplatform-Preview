package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.Action
import javax.swing.JComponent

class HotPreviewGutterIcon(
    private val project: Project,
    private val file: VirtualFile,
    private val annotation: HotPreviewAnnotation,
    private val requestRender: () -> Unit
) : GutterIconRenderer() {
    override fun getIcon() = AllIcons.General.Settings
    override fun getTooltipText() = "Change preview settings"
    override fun isNavigateAction() = false

    override fun getClickAction(): AnAction = object : AnAction() {
        override fun actionPerformed(event: AnActionEvent) {
            val mouseEvent: MouseEvent = event.inputEvent as? MouseEvent ?: return
            val relativePoint = RelativePoint(mouseEvent.component, mouseEvent.point)

            val viewModel = GutterIconViewModel(project, file, annotation, requestRender)
            val dialog = MyCustomDialog(project, viewModel, relativePoint, requestRender)
            dialog.show()
            /*createJBDialog(
                viewModel = viewModel,
                mouseEvent = mouseEvent,
                requestRender = requestRender
            )*/
        }
    }
    override fun equals(other: Any?) = (other as? HotPreviewGutterIcon)?.annotation == annotation
    override fun hashCode() = annotation.hashCode()
}

class MyCustomDialog(
    project: Project,
    private val viewModel: GutterIconViewModelI,
    private val relativePoint: RelativePoint,
    requestRender: () -> Unit
) : DialogWrapper(project, true, IdeModalityType.MODELESS) {
    init {
        init()
        title = null
        setUndecorated(true)
        // Close dialog when clicking outside
        window?.addWindowFocusListener(object : WindowAdapter() {
            override fun windowLostFocus(e: WindowEvent?) {
                requestRender()
                close(OK_EXIT_CODE)  // Close the dialog when losing focus
            }
        })
    }

    override fun getInitialLocation(): Point? = relativePoint.screenPoint

    override fun createActions(): Array<out Action?> = emptyArray()

    @OptIn(ExperimentalJewelApi::class)
    override fun createCenterPanel(): JComponent {
        enableNewSwingCompositing()
        val component = JewelComposePanel {
            GutterIconAnnotationSettings(viewModel)
        }
        return component
    }
}

const val preferredWidth = 300
const val preferredHeight = 400

@OptIn(ExperimentalJewelApi::class)
fun createJBDialog(
    viewModel: GutterIconViewModelI,
    relativePoint: RelativePoint,
    requestRender: () -> Unit
) {
    enableNewSwingCompositing()
    val component = JewelComposePanel {
        GutterIconAnnotationSettings(
            modifier = Modifier.fillMaxSize(),
            vm = viewModel
        )
    }
    component.preferredSize = Dimension(preferredWidth * 2, preferredHeight * 2)
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, component)
        .setTitle("HotPreview Settings")
        .setFocusable(true)
        .setRequestFocus(true)
        .setShowShadow(true)
        .setCancelOnClickOutside(true)
        //.setResizable(true)
        .setShowBorder(true)
        .setCancelCallback {
            println("Closed")
            requestRender()
            true
        }
        .createPopup()
    //component.doLayout()
    println("Panel size: ${component.size}")
    popup.pack(true, true)
    popup.show(relativePoint)
}