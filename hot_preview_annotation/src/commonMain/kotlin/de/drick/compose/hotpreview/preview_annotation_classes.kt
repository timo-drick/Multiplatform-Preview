package de.drick.compose.hotpreview

/**
 * Copied from the Android sources: androidx.compose.ui.tooling.preview.MultiPreviews.android.kt
 */

/*@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "Phone", device = PHONE, showSystemUi = true)
@HotPreview(name = "Phone - Landscape",
    device = "spec:width = 411dp, height = 891dp, orientation = landscape, dpi = 420",
    showSystemUi = true)
@HotPreview(name = "Unfolded Foldable", device = FOLDABLE, showSystemUi = true)
@HotPreview(name = "Tablet", device = TABLET, showSystemUi = true)
@HotPreview(name = "Desktop", device = DESKTOP, showSystemUi = true)
annotation class HotPreviewScreenSizes

    // Reference devices
    const val PHONE = "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420"
    const val FOLDABLE =
        "spec:id=reference_foldable,shape=Normal,width=673,height=841,unit=dp,dpi=420"
    const val TABLET = "spec:id=reference_tablet,shape=Normal,width=1280,height=800,unit=dp,dpi=240"
    const val DESKTOP =
        "spec:id=reference_desktop,shape=Normal,width=1920,height=1080,unit=dp,dpi=160"

    // TV devices (not adding 4K since it will be very heavy for preview)
    const val TV_720p = "spec:shape=Normal,width=1280,height=720,unit=dp,dpi=420"
    const val TV_1080p = "spec:shape=Normal,width=1920,height=1080,unit=dp,dpi=420"
*/

private const val mdpi = 160f

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "Phone", widthDp = 411, heightDp = 891, density = 420/mdpi)
@HotPreview(name = "Phone - Landscape", widthDp = 891, heightDp = 411, density = 420/mdpi)
@HotPreview(name = "Unfolded Foldable", widthDp = 673, heightDp = 841, density = 420/mdpi)
@HotPreview(name = "Tablet", widthDp = 1280, heightDp = 800, density = 240/mdpi)
@HotPreview(name = "Desktop", widthDp = 1920, heightDp = 1080, density = 160/mdpi)
annotation class HotPreviewScreenSizes

/**
 * A MultiPreview annotation for displaying a @[Composable] method using seven standard font sizes.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "85%", fontScale = 0.85f)
@HotPreview(name = "100%", fontScale = 1.0f)
@HotPreview(name = "115%", fontScale = 1.15f)
@HotPreview(name = "130%", fontScale = 1.3f)
@HotPreview(name = "150%", fontScale = 1.5f)
@HotPreview(name = "180%", fontScale = 1.8f)
@HotPreview(name = "200%", fontScale = 2f)
annotation class HotPreviewFontScale

/**
 * A MultiPreview annotation for displaying a @[Composable] method using light and dark themes.
 *
 * Note that the app theme should support dark and light modes for these HotPreviews to be different.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "Light", darkMode = false)
@HotPreview(name = "Dark", darkMode = true)
annotation class HotPreviewLightDark

/**
 * A MultiHotPreview annotation for displaying a @[Composable] method using four different wallpaper colors.
 *
 * Note that the app should use a dynamic theme for these HotPreviews to be different.
 */
/*@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "Red", wallpaper = RED_DOMINATED_EXAMPLE)
@HotPreview(name = "Blue", wallpaper = BLUE_DOMINATED_EXAMPLE)
@HotPreview(name = "Green", wallpaper = GREEN_DOMINATED_EXAMPLE)
@HotPreview(name = "Yellow", wallpaper = YELLOW_DOMINATED_EXAMPLE)
annotation class HotPreviewDynamicColors
*/
//TODO dynamic themes are not supported by multiplatform i think. So not sure if this is possible to support.


/**
 * Annotation classes to check if the App handles WindowInsets correctly.
 * Also known as edge-to-edge support.
 * @see https://developer.android.com/develop/ui/compose/system/insets
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(
    name = "Phone portrait Gesture", widthDp = 411, heightDp = 891,
    camera = CameraPosition.Top,
    statusBar = true,
    navigationBar = NavigationMode.GestureBottom,
)
@HotPreview(
    name = "Phone portrait 3button", widthDp = 411, heightDp = 891,
    camera = CameraPosition.Top,
    statusBar = true,
    navigationBar = NavigationMode.ThreeButtonBottom,
)
@HotPreview(
    name = "Phone landscape gesture", widthDp = 891, heightDp = 411,
    camera = CameraPosition.Left,
    statusBar = true,
    navigationBar = NavigationMode.GestureBottom,
)
@HotPreview(
    name = "Phone landscape nav left", widthDp = 891, heightDp = 411,
    camera = CameraPosition.Right,
    statusBar = true,
    navigationBar = NavigationMode.ThreeButtonLeft,
)
@HotPreview(
    name = "Phone landscape nav right", widthDp = 891, heightDp = 411,
    camera = CameraPosition.Left,
    statusBar = true,
    navigationBar = NavigationMode.ThreeButtonRight,
)
@HotPreview(
    name = "Tablet window mode", widthDp = 673, heightDp = 841,
    captionBar = true
)
annotation class HotPreviewWindowInsets

/**
 * Annotation classes to check if the App handles WindowInsets correctly.
 * It simulates `window.isNavigationBarContrastEnforced = false`
 * @see https://developer.android.com/develop/ui/compose/system/system-bars
 *
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(
    name = "Phone portrait Gesture", widthDp = 411, heightDp = 891,
    camera = CameraPosition.Top,
    statusBar = true,
    navigationBar = NavigationMode.GestureBottom,
)
@HotPreview(
    name = "Phone portrait 3button", widthDp = 411, heightDp = 891,
    camera = CameraPosition.Top,
    statusBar = true,
    navigationBar = NavigationMode.ThreeButtonBottom,
    navigationBarContrastEnforced = false
)
@HotPreview(
    name = "Phone landscape gesture", widthDp = 891, heightDp = 411,
    camera = CameraPosition.Left,
    statusBar = true,
    navigationBar = NavigationMode.GestureBottom,
    navigationBarContrastEnforced = false
)
@HotPreview(
    name = "Phone landscape nav left", widthDp = 891, heightDp = 411,
    camera = CameraPosition.Right,
    statusBar = true,
    navigationBar = NavigationMode.ThreeButtonLeft,
    navigationBarContrastEnforced = false
)
@HotPreview(
    name = "Phone landscape nav right", widthDp = 891, heightDp = 411,
    camera = CameraPosition.Left,
    statusBar = true,
    navigationBar = NavigationMode.ThreeButtonRight,
    navigationBarContrastEnforced = false
)
@HotPreview(
    name = "Tablet window mode", widthDp = 673, heightDp = 841,
    captionBar = true
)
annotation class HotPreviewWindowInsetsNC