package de.drick.compose.hotpreview

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
annotation class HotPreview(
    val name: String = "",
    val group: String = "",    // Not used yet!
    val widthDp: Int = -1,     // if < 0 it will adapt to content max 1024 dp
    val heightDp: Int = -1,    // if < 0 it will adapt to content max 1024 dp
    val locale: String = "",   // Not supported yet!
    val fontScale: Float = 1f, // Should be between 0.5f and 2.0f
    val darkMode: Boolean = true,
)
