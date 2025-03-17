package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.drick.compose.hotpreview.plugin.AnnotationUpdate
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.UpdatePsiAnnotationDsl
import de.drick.compose.hotpreview.plugin.checkAnnotationParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT) class ProjectScopeProviderService(val scope: CoroutineScope)

interface UpdateAnnotationDsl : UpdatePsiAnnotationDsl {
    fun render()
}

interface GutterIconViewModelI {
    val baseModel: HotPreviewModel
    val name: ArgumentField
    val group: ArgumentField
    val widthDp: ArgumentField
    val heightDp: ArgumentField
    val density: ArgumentField
    val locale: ArgumentField
    val fontScale: ArgumentField

    fun update(dsl: UpdateAnnotationDsl.() -> Unit)
    fun render()
    suspend fun checkParameterExists(parameterName: String): Boolean
}

class GutterIconViewModel(
    val project: Project,
    private val file: VirtualFile,
    private val annotation: HotPreviewAnnotation,
    private val groups: Set<String>,
    private val requestRender: () -> Unit
): GutterIconViewModelI {
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
