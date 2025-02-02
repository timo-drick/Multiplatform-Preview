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
import de.drick.compose.hotpreview.plugin.HotPreviewAnnotation
import de.drick.compose.hotpreview.plugin.HotPreviewData
import de.drick.compose.hotpreview.plugin.HotPreviewFunction
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.HotPreviewViewModelI
import de.drick.compose.hotpreview.plugin.RenderedImage
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
import kotlin.math.roundToInt

@Composable
fun SelfPreviewBase(data: List<HotPreviewData>) {
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

fun getHotPreviewDataItem(resourceName: String): HotPreviewData {
    val previewItem = getPreviewItem("drawable/$resourceName.png", if (resourceName == "login_light") 4f else 2f)
    return HotPreviewData(
        function = HotPreviewFunction(
            name = resourceName.capitalize(),
            annotation = listOf(
                HotPreviewAnnotation(
                    lineRange = null,
                    HotPreviewModel(
                        name = "",
                        widthDp = previewItem.size.width.value.roundToInt(),
                        heightDp = previewItem.size.width.value.roundToInt()
                    )
                )
            ),
            lineRange = null
        ),
        image = listOf(previewItem)
    )
}

val sampleImages = listOf(
    "countposer_dialog",
    "countposer_start",
    "login_dark",
    "login_light"
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

fun mockViewModel(mockData: List<HotPreviewData>) = object : HotPreviewViewModelI {
    override var scale: Float = 1f
    override fun changeScale(newScale: Float) { scale = newScale}
    override fun navigateCodeLine(line: Int) {}
    override suspend fun render(): List<HotPreviewData> = mockData
    override suspend fun executeGradleTask() {}
    override fun subscribeForFileChanges(scope: CoroutineScope, onChanged: () -> Unit) {}
}
