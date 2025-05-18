package de.drick.compose.hotpreview

data object Info {
    const val hotPreviewAnnotationVersion = 2
}

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Repeatable
annotation class HotPreview(
    val name: String = "",          // Name shown in the preview
    val group: String = "",         // Allows you to filter by group in the gallery view
    val widthDp: Int = -1,          // if < 0 it will adapt to content max 1024 dp
    val heightDp: Int = -1,         // if < 0 it will adapt to content max 1024 dp
    val locale: String = "",        // Localization
    val fontScale: Float = 1f,      // The scaling factor for fonts. Should be between 0.5f and 2.0f
    val density: Float = 1f,        // The logical density of the display. This is a scaling factor for the Dp unit.
                                    // Pixels per inch (dpi) -> density is (dpi / 160)
    val darkMode: Boolean = true,   // Set the system theme to dark mode or light mode

    // WindowInsets simulation
    val statusBar: Boolean = false, // Shows a status bar at the top of the preview
    val navigationBar: NavigationBarMode = NavigationBarMode.Off,
    val navigationBarContrastEnforced: Boolean = true,
    val displayCutout: DisplayCutoutMode = DisplayCutoutMode.Off,
    val captionBar: Boolean = false // Shows a caption bar at the top of the preview
)

enum class NavigationBarMode {
    Off, GestureBottom, ThreeButtonBottom, ThreeButtonLeft, ThreeButtonRight
}

enum class DisplayCutoutMode {
    Off, CameraTop, CameraLeft, CameraRight, CameraBottom
}
