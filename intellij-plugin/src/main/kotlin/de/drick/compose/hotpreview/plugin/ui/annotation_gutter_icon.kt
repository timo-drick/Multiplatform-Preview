package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import de.drick.compose.hotpreview.plugin.updateAnnotationParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JComponent


/*@HotPreview
@Composable
fun GutterIconAnnotationSettingsPreview() {
    SelfPreviewTheme {
        GutterIconAnnotationSettings()
    }
}*/

@Composable
fun GutterIconAnnotationSettings(
    viewModel: GutterIconViewModel
) {
    Column(
        Modifier.width(IntrinsicSize.Min).padding(8.dp)
    ) {
        Text("HotPreview Settings")
        Divider(
            Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            //color = style.colors.divider,
            //thickness = style.metrics.dividerThickness,
        )
        Text("Name")
        val nameState = rememberTextFieldState(viewModel.name)
        LaunchedEffect(nameState) {
            snapshotFlow { nameState.text }.collect { text ->
                viewModel.updateName(text.toString())
            }
        }
        TextField(nameState)
        ActionButton(
            onClick = {}
        ) {
            Text("Ok")
        }
    }
}

@Service(Service.Level.PROJECT) private class ProjectScopeProviderService(val scope: CoroutineScope)

class GutterIconViewModel(
    val project: Project,
    private val file: VirtualFile,
    private val annotation: HotPreviewAnnotation
) {
    private val scope = project.service<ProjectScopeProviderService>().scope
    private val line = requireNotNull(annotation.lineRange?.first) { "Line should not be null!" }

    val name: String = annotation.annotation.name

    fun updateName(name: String) {
        val value = "\"$name\""
        //HotPreview::name.returnType
        scope.launch {
            updateAnnotationParameter(project, file, line, "name", value)
        }
    }
}

class HotPreviewGutterIcon(
    private val project: Project,
    private val file: VirtualFile,
    private val annotation: HotPreviewAnnotation
) : GutterIconRenderer() {
    override fun getIcon() = AllIcons.General.Settings
    override fun getTooltipText() = "Change preview settings"
    override fun isNavigateAction() = false

    @OptIn(ExperimentalJewelApi::class)
    override fun getClickAction(): AnAction = object : AnAction() {
        override fun actionPerformed(event: AnActionEvent) {
            val editor: Editor = event.getData(CommonDataKeys.EDITOR) ?: return
            val mouseEvent: MouseEvent = event.inputEvent as? MouseEvent ?: return
            val mousePoint = mouseEvent.point
            val viewModel = GutterIconViewModel(project, file, annotation)
            enableNewSwingCompositing()
            val component = JewelComposePanel {
                GutterIconAnnotationSettings(viewModel)
            }
            //component.preferredSize = Dimension(10, 10)
            val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, component)
                .setTitle("HotPreview Settings")
                .setFocusable(true)
                .setRequestFocus(true)
                .setShowShadow(true)
                .setCancelOnClickOutside(true)
                .setResizable(true)
                .createPopup()
            component.doLayout()
            component.preferredSize = component.size
            popup.pack(true, true)
            val relativePoint = RelativePoint(mouseEvent.component, mousePoint)
            popup.show(relativePoint)
        }
    }
    override fun equals(other: Any?) = (other as? HotPreviewGutterIcon)?.annotation == annotation
    override fun hashCode() = annotation.hashCode()
}
