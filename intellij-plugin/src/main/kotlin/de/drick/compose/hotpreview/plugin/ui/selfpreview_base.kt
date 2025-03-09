package de.drick.compose.hotpreview.plugin.ui

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

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SelfPreviewBase(
    env: ResourceEnvironment,
    data: List<UIHotPreviewData>
) {
    val viewModel = remember {
        mockViewModel(env, data)
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


fun getMockData(
) = SamplePreviewItem.entries.groupBy { it.functionName }.map { (functionName, items) ->
    getHotPreviewDataItem(functionName, *items.toTypedArray())
}

fun getHotPreviewDataItem(
    functionName: String,
    vararg samples: SamplePreviewItem
): UIHotPreviewData {
    try {
        return UIHotPreviewData(
            functionName = functionName,
            annotations = samples.map { item ->
                UIAnnotation(
                    name = "Test1",
                    lineRange = 0..1,
                    renderCacheKey = item.toRenderCacheKey()
                )
            },
            lineRange = 0..1
        )
    } catch (err: Throwable) {
        return UIHotPreviewData(
            functionName = functionName,
            annotations = listOf(
                UIAnnotation(
                    name = "Test1",
                    lineRange = 0..1,
                    renderCacheKey = SamplePreviewItem.error_test.toRenderCacheKey()
                )
            ),
            lineRange = 0..1
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
fun resolveRenderState(
    env: ResourceEnvironment,
    requestedKeys: Set<HotPreviewViewModel.RenderCacheKey>
) =
    SamplePreviewItem.entries.associate {
        val uiState = UIRenderState()
        try {
            val resource = runBlocking { getDrawableResourceBytes(env, it.drawableResource) }
            val image = resource.decodeToImageBitmap()
            val size = DpSize((image.width / it.density).dp, (image.height / it.density).dp)
            uiState.state = RenderedImage(image, size)
        } catch (err: Throwable) {
            uiState.state = RenderError(err.message ?: "Error")
        }
        Pair(it.toRenderCacheKey(), uiState)
    }

@OptIn(InternalResourceApi::class)
enum class SamplePreviewItem(
    val functionName: String,
    val drawableResource: DrawableResource,
    val density: Float
) {
    countposer_dialog("countposer_dialog", Res.drawable.countposer_dialog, 2f),
    countposer_start("countposer_start", Res.drawable.countposer_start, 2f),
    login_dark("login", Res.drawable.login_dark, 2f),
    login_light("login", Res.drawable.login_light, 4f),
    error_test("error", DrawableResource("error", emptySet()), 2f)
}

private fun SamplePreviewItem.toRenderCacheKey() = HotPreviewViewModel.RenderCacheKey(
    name = functionName,
    annotation = HotPreviewModel(name = name)
)

@OptIn(ExperimentalResourceApi::class)
fun getPreviewItem(
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

@OptIn(ExperimentalResourceApi::class)
fun mockViewModel(
    env: ResourceEnvironment,
    mockData: List<UIHotPreviewData>,
) = object : HotPreviewViewModelI {
    override var scale: Float = 1f
    override val isPureTextEditor = false
    override var compilingInProgress = false
    override var errorMessage: Throwable? = null
    override var previewList = mockData
    override val groups = setOf("Group 1")
    override val selectedGroup: String? = null
    override val selectedTab: Int? = null
    override fun selectGroup(group: String?) {}
    override fun selectTab(tabIndex: Int) {}
    override fun changeScale(newScale: Float) {
        scale = newScale
    }
    override fun toggleLayout() {}
    override fun navigateCodeLine(line: Int) {}
    override fun monitorChanges(scope: CoroutineScope) {}
    override fun refresh() {}
    override fun openSettings() {}
    override fun requestPreviews(keys: Set<HotPreviewViewModel.RenderCacheKey>): Map<HotPreviewViewModel.RenderCacheKey, UIRenderState> =
        resolveRenderState(env, keys)
}
