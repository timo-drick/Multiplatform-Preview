package de.drick.compose.window_insets_simulation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.CameraPositionModel
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel
import de.drick.compose.hotpreview.plugin.VisibilityModel
import kotlin.math.roundToInt

val insetsPhonePortraitNormal = WindowInsetsDeviceConfig(
    cameraCutout = InsetConfigs(top = InsetConfig(52.dp)),
    statusBar = InsetConfigs(top = InsetConfig(24.dp)),
    navigationBar = InsetConfigs(bottom = InsetConfig(48.dp)),
    captionBar = InsetConfigs(),
)

fun HotPreviewModel.toWindowInsetsDeviceConfig(): WindowInsetsDeviceConfig {
    val cameraInset = InsetConfig(52.dp)
    val cameraInsetsConfig = if (camera.visibility == VisibilityModel.Visible) {
        when (camera.cameraPosition) {
            CameraPositionModel.Left -> InsetConfigs(left = cameraInset)
            CameraPositionModel.Top -> InsetConfigs(top = cameraInset)
            CameraPositionModel.Right -> InsetConfigs(right = cameraInset)
            CameraPositionModel.Bottom -> InsetConfigs(bottom = cameraInset)
        }
    } else InsetConfigs()
    val statusBarInsetsConfig = if (statusBar.visibility == VisibilityModel.Visible)
        InsetConfigs(top = InsetConfig(24.dp))
    else InsetConfigs()
    val navigationBarInsetsConfig = if (navigationBar.visibility == VisibilityModel.Visible) {
        when (navigationBar.mode) {
            NavigationModeModel.GestureBottom -> InsetConfigs(bottom = InsetConfig(32.dp))
            NavigationModeModel.ThreeButtonBottom -> InsetConfigs(bottom = InsetConfig(48.dp))
            NavigationModeModel.ThreeButtonLeft -> InsetConfigs(left = InsetConfig(48.dp))
            NavigationModeModel.ThreeButtonRight -> InsetConfigs(right = InsetConfig(48.dp))
        }
    } else InsetConfigs()
    val captionBarInsetsConfig = if (captionBar.visibility == VisibilityModel.Visible)
        InsetConfigs(top = InsetConfig(42.dp))
    else InsetConfigs()

    return WindowInsetsDeviceConfig(
        cameraCutout = cameraInsetsConfig,
        statusBar = statusBarInsetsConfig,
        navigationBar = navigationBarInsetsConfig,
        captionBar = captionBarInsetsConfig,
    )
}

fun InsetConfig.toInsetValueString() = if (visibility != InsetVisibility.Off)
    size.value.roundToInt().toString()
else
    "0"

fun InsetConfigs.toWindowInsetsString() =
    "${left.toInsetValueString()},${top.toInsetValueString()},${right.toInsetValueString()},${bottom.toInsetValueString()}"

fun WindowInsetsDeviceConfig.toWindowInsetsString() = """
    captionBarInsets(${captionBar.toWindowInsetsString()})|
    displayCutoutInsets(${cameraCutout.toWindowInsetsString()})|
    navigationBarsInsets(${navigationBar.toWindowInsetsString()})|
    statusBarsInsets(${statusBar.toWindowInsetsString()})
""".trimIndent().replace("\n", "")

@Composable
fun WindowInsetsDeviceSimulation(
    deviceConfig: WindowInsetsDeviceConfig,
    hotPreviewAnnotation: HotPreviewModel,
) {
    Box(Modifier.fillMaxSize()) {
        if (hotPreviewAnnotation.statusBar.visibility == VisibilityModel.Visible) {
            StatusBar(
                modifier = Modifier.align(Alignment.TopStart).height(deviceConfig.statusBar.top.size),
                isDarkMode = hotPreviewAnnotation.darkMode,
            )
        }
        if (hotPreviewAnnotation.captionBar.visibility == VisibilityModel.Visible) {
            CaptionBar(
                modifier = Modifier.align(Alignment.TopStart).height(deviceConfig.captionBar.top.size),
                isDarkMode = hotPreviewAnnotation.darkMode,
            )
        }
        if (hotPreviewAnnotation.camera.visibility == VisibilityModel.Visible) {
            val alignment = when (hotPreviewAnnotation.camera.cameraPosition) {
                CameraPositionModel.Left -> AbsoluteAlignment.TopLeft
                CameraPositionModel.Top -> AbsoluteAlignment.TopLeft
                CameraPositionModel.Right -> AbsoluteAlignment.TopRight
                CameraPositionModel.Bottom -> AbsoluteAlignment.BottomLeft
            }
            val sizeModifier = when (hotPreviewAnnotation.camera.cameraPosition) {
                CameraPositionModel.Left -> Modifier.width(deviceConfig.cameraCutout.left.size)
                CameraPositionModel.Top -> Modifier.height(deviceConfig.cameraCutout.top.size)
                CameraPositionModel.Right -> Modifier.width(deviceConfig.cameraCutout.right.size)
                CameraPositionModel.Bottom -> Modifier.height(deviceConfig.cameraCutout.bottom.size)
            }
            CameraCutout(
                modifier = Modifier.align(alignment).then(sizeModifier),
            )
        }
        hotPreviewAnnotation.navigationBar.let { navBar ->
            if (navBar.visibility == VisibilityModel.Visible) {
                val size = when (navBar.mode) {
                    NavigationModeModel.GestureBottom -> deviceConfig.navigationBar.bottom.size
                    NavigationModeModel.ThreeButtonBottom -> deviceConfig.navigationBar.bottom.size
                    NavigationModeModel.ThreeButtonLeft -> deviceConfig.navigationBar.left.size
                    NavigationModeModel.ThreeButtonRight -> deviceConfig.navigationBar.right.size
                }
                val alignment = when (navBar.mode) {
                    NavigationModeModel.GestureBottom -> AbsoluteAlignment.BottomLeft
                    NavigationModeModel.ThreeButtonBottom -> AbsoluteAlignment.BottomLeft
                    NavigationModeModel.ThreeButtonLeft -> AbsoluteAlignment.TopLeft
                    NavigationModeModel.ThreeButtonRight -> AbsoluteAlignment.TopRight
                }
                NavigationBar(
                    modifier = Modifier.align(alignment),
                    navMode = navBar.mode,
                    size = size,
                    isDarkMode = hotPreviewAnnotation.darkMode,
                    backgroundAlpha = if (navBar.contrastEnforced) 0.5f else 0f,
                )
            }
        }
    }
}
