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

const val HOT_PREVIEW_ANNOTATION_VERSION = 2

/**
 * This is a copy of the HotPreview annotation.
 * Because of conflicts with the classloaders it is necessary to have a copy of the data structure here.
 */
data class HotPreviewModel(
    val name: String = "",
    val group: String = "",
    val widthDp: Int = -1,
    val heightDp: Int = -1,
    val locale: String = "",
    val layoutDirectionRTL: Boolean = false,
    val fontScale: Float = 1f,
    val density: Float = 1f,
    val darkMode: Boolean = true,
    val backgroundColor: Long = 0x0,
    val statusBar: Boolean = false,
    val navigationBar: NavigationModeModel = NavigationModeModel.Off,
    val navigationBarContrastEnforced: Boolean = true,
    val displayCutout: DisplayCutoutModeModel = DisplayCutoutModeModel.Off,
    val captionBar: Boolean = false,
    val inspectionMode: Boolean = true,
)

enum class NavigationModeModel {
    Off, GestureBottom, ThreeButtonBottom, ThreeButtonLeft, ThreeButtonRight
}

enum class DisplayCutoutModeModel {
    Off, CameraTop, CameraLeft, CameraRight, CameraBottom
}
