package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.hotpreview.plugin.AnnotationUpdate
import de.drick.compose.hotpreview.plugin.DisplayCutoutModeModel
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel
import de.drick.compose.hotpreview.plugin.UpdatePsiAnnotationDsl
import de.drick.compose.hotpreview.plugin.checkAnnotationParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT) class ProjectScopeProviderService(val scope: CoroutineScope)

interface UpdateAnnotationDsl : UpdatePsiAnnotationDsl {
    fun render()
}

interface GutterIconViewModelI {
    val annotationVersion: Int
    val baseModel: HotPreviewModel
    val name: ArgumentField
    val group: ArgumentField
    val widthDp: ArgumentField
    val heightDp: ArgumentField
    val density: ArgumentField
    val locale: ArgumentField
    val layoutDirectionRTL: ArgumentTriStateBoolean
    val fontScale: ArgumentField
    val darkMode: ArgumentTriStateBoolean
    val statusBar: ArgumentTriStateBoolean
    val captionBar: ArgumentTriStateBoolean
    val navigationBar: ArgumentFieldEnum<NavigationModeModel>
    val navigationBarContrastEnforced: ArgumentTriStateBoolean
    val displayCutout: ArgumentFieldEnum<DisplayCutoutModeModel>

    val deviceTemplates: List<DeviceTemplate>

    fun update(dsl: UpdateAnnotationDsl.() -> Unit)
    fun render()
    suspend fun checkParameterExists(parameterName: String): Boolean
}

class GutterIconViewModel(
    val project: Project,
    private val file: VirtualFile,
    private val annotation: HotPreviewAnnotation,
    private val groups: Set<String>,
    private val requestRender: () -> Unit,
    override val annotationVersion: Int
): GutterIconViewModelI {

    private val scope = project.service<ProjectScopeProviderService>().scope
    private val line = requireNotNull(annotation.lineRange.first) { "Line should not be null!" }

    private val annotationUpdate = AnnotationUpdate(project, file, line)

    override val deviceTemplates = if (annotationVersion < 2) deviceTemplatesV1 else deviceTemplatesV2

    override val baseModel = annotation.annotation
    override val name = ArgumentField(
        name = "name",
        defaultValue = baseModel.name,
        isString = true,
        useUpdateDsl = ::update
    )
    override val group = ArgumentField(
        name = "group",
        defaultValue = baseModel.group,
        isString = true,
        useUpdateDsl = ::update
    )
    override val widthDp = ArgumentField(
        name = "widthDp",
        defaultValue = baseModel.widthDp.toString(),
        isString = false,
        useUpdateDsl = ::update
    )
    override val heightDp = ArgumentField(
        name = "heightDp",
        defaultValue = baseModel.heightDp.toString(),
        isString = false,
        useUpdateDsl = ::update
    )
    override val density = ArgumentField(
        name = "density",
        defaultValue = "${baseModel.density}f",
        isString = false,
        useUpdateDsl = ::update
    )
    override val locale = ArgumentField(
        name = "locale",
        defaultValue = baseModel.locale,
        isString = true,
        useUpdateDsl = ::update
    )
    override val layoutDirectionRTL = ArgumentTriStateBoolean(
        name = "layoutDirectionRTL",
        defaultValue = baseModel.layoutDirectionRTL,
        useUpdateDsl = ::update
    )
    override val fontScale = ArgumentField(
        name = "fontScale",
        defaultValue = "${baseModel.fontScale}f",
        isString = false,
        useUpdateDsl = ::update
    )

    override val darkMode = ArgumentTriStateBoolean(
        name = "darkMode",
        defaultValue = baseModel.darkMode,
        useUpdateDsl = ::update
    )

    override val statusBar = ArgumentTriStateBoolean(
        name = "statusBar",
        defaultValue = baseModel.statusBar,
        useUpdateDsl = ::update
    )
    override val captionBar = ArgumentTriStateBoolean(
        name = "captionBar",
        defaultValue = baseModel.captionBar,
        useUpdateDsl = ::update
    )
    override val navigationBar = ArgumentFieldEnum(
        name = "navigationBar",
        nullValue = NavigationModeModel.Off,
        defaultValue = baseModel.navigationBar,
        fqName = "de.drick.compose.hotpreview.NavigationBarMode",
        useUpdateDsl = ::update
    )
    override val navigationBarContrastEnforced = ArgumentTriStateBoolean(
        name = "navigationBarContrastEnforced",
        defaultValue = baseModel.navigationBarContrastEnforced,
        useUpdateDsl = ::update
    )

    override val displayCutout = ArgumentFieldEnum(
        name = "displayCutout",
        nullValue = DisplayCutoutModeModel.Off,
        defaultValue = baseModel.displayCutout,
        fqName = "de.drick.compose.hotpreview.DisplayCutoutMode",
        useUpdateDsl = ::update
    )

    init {
        update {
            darkMode.init(this)
            statusBar.init(this)
            captionBar.init(this)
            navigationBarContrastEnforced.init(this)
        }
    }

    override fun update(block: UpdateAnnotationDsl.() -> Unit) {
        scope.launch {
            annotationUpdate.updateDsl {
                val psiDsl = this
                val dsl = object : UpdateAnnotationDsl, UpdatePsiAnnotationDsl by psiDsl {
                    override fun render() {
                        requestRender()
                    }
                }
                block(dsl)
            }
        }
    }
    override fun render() {
        val type = baseModel::name
        type.name
        scope.launch {
            requestRender()
        }
    }
    override suspend fun checkParameterExists(parameterName: String) =
        checkAnnotationParameter(project, file, line, parameterName)
}

class ArgumentField(
    val name: String,
    defaultValue: String,
    val isString: Boolean,
    private val useUpdateDsl: (block: UpdateAnnotationDsl.() -> Unit) -> Unit
) {
    var value by mutableStateOf(defaultValue)
        private set

    fun update(dsl: UpdateAnnotationDsl, newValue: String) {
        if (value == newValue) return
        value = newValue
        if (isString) {
            dsl.string(name, newValue)
        } else {
            dsl.parameter(name, newValue)
        }
    }

    fun update(newValue: String, renderUpdate: Boolean = false) {
        useUpdateDsl {
            update(this, newValue)
            if (renderUpdate) render()
        }
    }
}

class ArgumentFieldEnum<T: Enum<T>>(
    val name: String,
    private val nullValue: T,
    defaultValue: T,
    private val fqName: String,
    private val useUpdateDsl: (block: UpdateAnnotationDsl.() -> Unit) -> Unit
) {
    var value: T by mutableStateOf(defaultValue)
        private set

    fun update(dsl: UpdateAnnotationDsl, newValue: T) {
        println("Update enum: $name -> $newValue default (${nullValue.name})")
        if (newValue == nullValue) {
            dsl.parameter(name, null)
        } else {
            dsl.parameter(name, "${fqName}.${newValue.name}")
        }
        value = newValue
    }

    fun update(newValue: T) {
        useUpdateDsl {
            update(this, newValue)
            render()
        }
    }
}

class ArgumentTriStateBoolean(
    val name: String,
    defaultValue: Boolean,
    private val useUpdateDsl: (block: UpdateAnnotationDsl.() -> Unit) -> Unit
) {
    var value: ToggleableState by mutableStateOf(if (defaultValue) ToggleableState.On else ToggleableState.Off)
        private set

    fun init(dsl: UpdateAnnotationDsl) {
        if (dsl.checkParameter(name).not()) value = ToggleableState.Indeterminate
    }

    fun set(dsl: UpdateAnnotationDsl, newState: ToggleableState) {
        val newValue: String? = when (newState) {
            ToggleableState.On -> "true"
            ToggleableState.Off -> "false"
            ToggleableState.Indeterminate -> null
        }
        println("New state for $name: $newState -> $newValue")
        dsl.parameter(name, newValue)
        value = newState
    }

    fun toggle() {
        useUpdateDsl {
            toggle(this)
            render()
        }
    }
    fun toggle(dsl: UpdateAnnotationDsl) {
        val newState = when (value) {
            ToggleableState.On -> ToggleableState.Off
            ToggleableState.Off -> ToggleableState.Indeterminate
            ToggleableState.Indeterminate -> ToggleableState.On
        }
        set(dsl, newState)
    }
}