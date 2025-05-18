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
    val outdatedAnnotationVersion: Boolean
    val baseModel: HotPreviewModel
    val name: ArgumentField
    val group: ArgumentField
    val widthDp: ArgumentField
    val heightDp: ArgumentField
    val density: ArgumentField
    val locale: ArgumentField
    val fontScale: ArgumentField
    val darkMode: ArgumentTriStateBoolean
    val statusBar: ArgumentTriStateBoolean
    val captionBar: ArgumentTriStateBoolean
    val navigationBar: ArgumentFieldEnum<NavigationModeModel>
    val navigationBarContrastEnforced: ArgumentTriStateBoolean
    val displayCutout: ArgumentFieldEnum<DisplayCutoutModeModel>

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
    annotationVersion: Int
): GutterIconViewModelI {
    override val outdatedAnnotationVersion = annotationVersion < 2

    private val scope = project.service<ProjectScopeProviderService>().scope
    private val line = requireNotNull(annotation.lineRange.first) { "Line should not be null!" }

    private val annotationUpdate = AnnotationUpdate(project, file, line)

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
    private val defaultValue: T,
    private val fqName: String,
    private val useUpdateDsl: (block: UpdateAnnotationDsl.() -> Unit) -> Unit
) {
    var value: T by mutableStateOf(defaultValue)
        private set

    fun update(newValue: T) {
        useUpdateDsl {
            println("Update enum: $name -> $newValue default (${defaultValue.name})")
            if (newValue == defaultValue) {
                parameter(name, null)
            } else {
                parameter(name, "${fqName}.${newValue.name}")
            }
            value = newValue
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

    fun set(value: ToggleableState) {

    }

    fun toggle() {
        val newState = when (value) {
            ToggleableState.On -> ToggleableState.Off
            ToggleableState.Off -> ToggleableState.Indeterminate
            ToggleableState.Indeterminate -> ToggleableState.On
        }
        val newValue: String? = when (newState) {
            ToggleableState.On -> "true"
            ToggleableState.Off -> "false"
            ToggleableState.Indeterminate -> null
        }
        useUpdateDsl {
            println("New state: $newState -> $newValue")
            parameter(name, newValue)
            value = newState
            render()
        }
    }
}