package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.ui.state.ToggleableState
import de.drick.compose.hotpreview.plugin.DisplayCutoutModeModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel

data class DeviceTemplate(
    val name: String,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val statusBar: ToggleableState = ToggleableState.Indeterminate,
    val captionBar: ToggleableState = ToggleableState.Indeterminate,
    val navigationBar: NavigationModeModel = NavigationModeModel.Off,
    val navigationBarContrastEnforced: ToggleableState = ToggleableState.Indeterminate,
    val displayCutout: DisplayCutoutModeModel = DisplayCutoutModeModel.Off
)

private const val mdpi = 160f

val deviceTemplatesV1 = listOf(
    DeviceTemplate(name = "Phone", widthDp = 411, heightDp = 891, density = 420/mdpi),
    DeviceTemplate(name = "Phone Landscape", widthDp = 891, heightDp = 411, density = 420/mdpi),
    DeviceTemplate(name = "Foldable", widthDp = 673, heightDp = 841, density = 420/mdpi),
    DeviceTemplate(name = "Tablet", widthDp = 1280, heightDp = 800, density = 240/mdpi),
    DeviceTemplate(name = "Desktop", widthDp = 1920, heightDp = 1080, density = 160/mdpi),
)
val deviceTemplatesV2 = listOf(
    DeviceTemplate(
        name = "Phone", widthDp = 411, heightDp = 891, density = 420/mdpi,
        statusBar = ToggleableState.On,
        navigationBar = NavigationModeModel.GestureBottom,
        displayCutout = DisplayCutoutModeModel.CameraTop,
    ),
    DeviceTemplate(
        name = "Phone Landscape", widthDp = 891, heightDp = 411, density = 420/mdpi,
        statusBar = ToggleableState.On,
        navigationBar = NavigationModeModel.GestureBottom,
        displayCutout = DisplayCutoutModeModel.CameraRight,
    ),
    DeviceTemplate(
        name = "Foldable", widthDp = 673, heightDp = 841, density = 420/mdpi,
        statusBar = ToggleableState.On,
        navigationBar = NavigationModeModel.GestureBottom,
        displayCutout = DisplayCutoutModeModel.CameraTop
    ),
    DeviceTemplate(
        name = "Tablet", widthDp = 1280, heightDp = 800, density = 240/mdpi,
        statusBar = ToggleableState.On,
        navigationBar = NavigationModeModel.ThreeButtonBottom
    ),
    DeviceTemplate(
        name = "Desktop", widthDp = 1920, heightDp = 1080, density = 160/mdpi,
        captionBar = ToggleableState.On,
    ),
)


fun GutterIconViewModelI.findDeviceTemplate() = deviceTemplates.find {
    density.value.toFloat() == it.density &&
    widthDp.value.toIntOrNull() == it.widthDp &&
    heightDp.value.toIntOrNull() == it.heightDp &&
    statusBar.value == it.statusBar &&
    captionBar.value == it.captionBar &&
    navigationBar.value == it.navigationBar &&
    displayCutout.value == it.displayCutout
}
