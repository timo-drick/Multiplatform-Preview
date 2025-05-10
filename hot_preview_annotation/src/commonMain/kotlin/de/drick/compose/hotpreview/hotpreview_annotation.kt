package de.drick.compose.hotpreview

private const val hotPreviewAnnotationVersion = 1

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
    val statusBar: StatusBarConfig = StatusBarConfig(Visibility.Off),
    val navigationBar: NavigationBarConfig = NavigationBarConfig(Visibility.Off),
    val camera: CameraConfig = CameraConfig(Visibility.Off),
    val captionBar: CaptionBarConfig = CaptionBarConfig(Visibility.Off)
)

enum class Visibility {
    Visible,    // It is visible in the preview and WindowInsets are simulated
    Off,        // Not visible and no WindowInsets are simulated
    Invisible   // Currently it is identical to Off,
                // but I plan to support e.g. WindowInsets.systemBarsIgnoringVisibility
                // So the inset is than not shown but WindowInsets.systemBarsIgnoringVisibility is injected
}

annotation class StatusBarConfig(
    val visibility: Visibility = Visibility.Visible
)

enum class NavigationMode {
    GestureBottom, ThreeButtonBottom, ThreeButtonLeft, ThreeButtonRight
}

annotation class NavigationBarConfig(
    val visibility: Visibility = Visibility.Visible,
    val mode: NavigationMode = NavigationMode.ThreeButtonBottom,
    val contrastEnforced: Boolean = false,
)

enum class CameraPosition {
    Left, Top, Right, Bottom
}

annotation class CameraConfig(
    val visibility: Visibility = Visibility.Visible,
    val cameraPosition: CameraPosition = CameraPosition.Top,
)

annotation class CaptionBarConfig(
    val visibility: Visibility = Visibility.Visible,
)