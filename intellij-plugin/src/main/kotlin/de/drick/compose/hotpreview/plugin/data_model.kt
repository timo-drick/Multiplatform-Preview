package de.drick.compose.hotpreview.plugin

data class HotPreviewParameterModel(
    val name: String,
    val providerClassName: String,
    val limit: Int
)

data class HotPreviewFunction(
    val name: String,
    val parameter: HotPreviewParameterModel?,
    val annotation: List<HotPreviewAnnotation>,
    val lineRange: IntRange
)

data class HotPreviewAnnotation(
    val lineRange: IntRange,
    val annotation: HotPreviewModel,
    val isAnnotationClass: Boolean
)

/**
 * This is a copy of the HotPreview annotation.
 * Because of conflicts with the classloaders it is necessary to have a copy of the data structure here.
 */
data class HotPreviewModel(
    val name: String = "",
    val group: String = "",         // Not used yet!
    val widthDp: Int = -1,          // if < 0 it will adapt to content max 1024 dp
    val heightDp: Int = -1,         // if < 0 it will adapt to content max 1024 dp
    val locale: String = "",        // Not supported yet!
    val fontScale: Float = 1f,      // The scaling factor for fonts. Should be between 0.5f and 2.0f
    val density: Float = 1f,        // The logical density of the display. This is a scaling factor for the Dp unit.
    val darkMode: Boolean = true,   // Set the system theme to dark mode or light mode
    val statusBar: Boolean = false, // Shows a status bar at the top of the preview also simulating the WindowInsets
    val navigationBar: NavigationModeModel = NavigationModeModel.Off,
    val navigationBarContrastEnforced: Boolean = true,
    val displayCutout: DisplayCutoutModeModel = DisplayCutoutModeModel.Off,
    val captionBar: Boolean = false
)

enum class NavigationModeModel {
    Off, GestureBottom, ThreeButtonBottom, ThreeButtonLeft, ThreeButtonRight
}

enum class DisplayCutoutModeModel {
    Off, CameraTop, CameraLeft, CameraRight, CameraBottom
}
