package de.drick.compose.hotpreview.plugin.ui

import ai.grazie.utils.capitalize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.plugin.*
import hotpreviewplugin.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.styling.Default
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.dark
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.light
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.styling.TabColors
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.theme.editorTabStyle

@Composable
fun SelfPreviewBase(data: List<UIHotPreviewData>) {
    val viewModel = remember {
        mockViewModel(data)
    }
    SelfPreviewBase(viewModel)
}

@Composable
fun SelfPreviewBase(viewModel: HotPreviewViewModelI) {
    SelfPreviewTheme {
        MainScreen(viewModel)
    }
}

@OptIn(InternalComposeUiApi::class)
@Composable
fun SelfPreviewTheme(content: @Composable () -> Unit) {
    val isDarkTheme = LocalSystemTheme.current == SystemTheme.Dark
    val themeDefinition = if (isDarkTheme) {
        JewelTheme.darkThemeDefinition()
    } else {
        JewelTheme.lightThemeDefinition()
    }
    val styling = if (isDarkTheme) {
        ComponentStyling.dark()
    } else {
        ComponentStyling.light(
            // This is a little bit closer to the IntelliJ theme
            editorTabStyle = TabStyle.Default.light(colors = TabColors.Default.light(background = Color.White))
        )
    }

    IntUiTheme(themeDefinition, styling) {
        Box(Modifier.background(JewelTheme.editorTabStyle.colors.background)) {
            content()
        }
    }
}


fun getMockData() = sampleImages.map { getHotPreviewDataItem(it) }

fun getHotPreviewDataItem(resourceName: String): UIHotPreviewData {
    try {
        val previewItem = getPreviewItem("drawable/$resourceName.png", if (resourceName == "login_light") 4f else 2f)
        return UIHotPreviewData(
            functionName = resourceName.capitalize(),
            annotations = listOf(
                UIAnnotation(
                    name = "Test1",
                    lineRange = null,
                ).also {
                    it.state = previewItem
                }
            ),
            lineRange = null
        )
    } catch (err: Throwable) {
        return UIHotPreviewData(
            functionName = resourceName.capitalize(),
            annotations = listOf(
                UIAnnotation(
                    name = "Test1",
                    lineRange = null,
                ).also {
                    it.state = RenderError(err.stackTraceToString())
                }
            ),
            lineRange = null
        )
    }
}

val sampleImages = listOf(
    "countposer_dialog",
    "countposer_start",
    "login_dark",
    "login_light",
    "error"
)

@OptIn(ExperimentalResourceApi::class)
private fun getPreviewItem(resource: String, density: Float): RenderedImage {
    val image = runBlocking {
        Res.readBytes(resource).decodeToImageBitmap()
    }
    //val image = useResource(resourcePath, ::loadImageBitmap)
    val size = DpSize((image.width / density).dp, (image.height / density).dp)
    return RenderedImage(image, size)
}

fun mockViewModel(
    mockData: List<UIHotPreviewData>,
) = object : HotPreviewViewModelI {
    override var scale: Float = 1f
    override val isPureTextEditor = false
    override var compilingInProgress = false
    override var errorMessage: Throwable? = null
    override var previewList = mockData
    override val groups = setOf("Group 1")
    override val selectedGroup: String? = null
    override fun selectGroup(group: String?) {}

    override fun changeScale(newScale: Float) { scale = newScale}
    override fun navigateCodeLine(line: Int) {}
    override fun monitorChanges(scope: CoroutineScope) {}
    override fun refresh() {}
}
