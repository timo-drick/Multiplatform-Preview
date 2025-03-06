package de.drick.compose.hotpreview

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
annotation class HotPreview(
    val name: String = "",
    val group: String = "",    // You can filter by group.
    val widthDp: Int = -1,     // if < 0 it will adapt to content max 1024 dp
    val heightDp: Int = -1,    // if < 0 it will adapt to content max 1024 dp
    val locale: String = "",   // Localization
    val fontScale: Float = 1f, // The scaling factor for fonts. Should be between 0.5f and 2.0f
    //val dpi: Int = 160,        // pixels per inch (dpi) -> density is (dpi / 160)
    val density: Float = 1f,   // The logical density of the display. This is a scaling factor for the Dp unit.
    val darkMode: Boolean = true,
)
