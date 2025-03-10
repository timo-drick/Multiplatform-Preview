package de.drick.compose.hotpreview.plugin

data class HotPreviewFunction(
    val name: String,
    val annotation: List<HotPreviewAnnotation>,
    val lineRange: IntRange
)

data class HotPreviewAnnotation(
    val lineRange: IntRange,
    val annotation: HotPreviewModel
)

/**
 * This is a copy of the HotPreview annotation.
 * Because of conflicts with the classloaders it is necessary to have a copy of the data structure here.
 */
data class HotPreviewModel(
    val name: String = "",
    val group: String = "",    // Not used yet!
    val widthDp: Int = -1,     // if < 0 it will adapt to content max 1024 dp
    val heightDp: Int = -1,    // if < 0 it will adapt to content max 1024 dp
    val locale: String = "",   // Not supported yet!
    val fontScale: Float = 1f, // The scaling factor for fonts. Should be between 0.5f and 2.0f
    val density: Float = 1f,   // The logical density of the display. This is a scaling factor for the Dp unit.
    val darkMode: Boolean = true,
)