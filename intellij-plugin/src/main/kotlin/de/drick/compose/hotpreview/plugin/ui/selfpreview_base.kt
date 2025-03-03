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
import hotpreviewplugin.generated.resources.*
import hotpreviewplugin.generated.resources.Res
import hotpreviewplugin.generated.resources.countposer_dialog
import hotpreviewplugin.generated.resources.countposer_start
import hotpreviewplugin.generated.resources.login_dark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.*
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


@OptIn(ExperimentalResourceApi::class)
fun getMockData(
    environment: ResourceEnvironment
) = SamplePreviewItem.entries.chunked(2).map { items ->
    val name = items.first().name
    getHotPreviewDataItem(environment, name, *items.toTypedArray())
}

@OptIn(ExperimentalResourceApi::class)
fun getHotPreviewDataItem(
    environment: ResourceEnvironment,
    functionName: String,
    vararg samples: SamplePreviewItem
): UIHotPreviewData {
    try {
        return UIHotPreviewData(
            functionName = functionName,
            annotations = samples.map { item ->
                UIAnnotation(
                    name = "Test1",
                    lineRange = null,
                ).also {
                    it.state = getPreviewItem(environment, item)
                }
            },
            lineRange = null
        )
    } catch (err: Throwable) {
        return UIHotPreviewData(
            functionName = functionName,
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

@OptIn(InternalResourceApi::class)
enum class SamplePreviewItem(
    val drawableResource: DrawableResource,
    val density: Float
) {
    countposer_dialog(Res.drawable.countposer_dialog, 2f),
    countposer_start(Res.drawable.countposer_start, 2f),
    login_dark(Res.drawable.login_dark, 2f),
    login_light(Res.drawable.login_light, 4f),
    error_test(DrawableResource("error", emptySet()), 2f)
}

val sampleImages = listOf(
    "countposer_dialog",
    "countposer_start",
    "login_dark",
    "login_light",
    "error"
)

@OptIn(ExperimentalResourceApi::class)
private fun getPreviewItem(resource: String): RenderedImage {
    val density = if (resource == "login_light") 4f else 2f
    val image = runBlocking {
        Res.readBytes(resource).decodeToImageBitmap()
    }
    //val image = useResource(resourcePath, ::loadImageBitmap)
    val size = DpSize((image.width / density).dp, (image.height / density).dp)
    return RenderedImage(image, size)
}

@OptIn(ExperimentalResourceApi::class)
private fun getPreviewItem(
    environment: ResourceEnvironment,
    resource: SamplePreviewItem
): RenderedImage {
    val density = resource.density
    val image = runBlocking {
        getDrawableResourceBytes(environment, resource.drawableResource).decodeToImageBitmap()
    }
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
    override fun openSettings() {}
}
