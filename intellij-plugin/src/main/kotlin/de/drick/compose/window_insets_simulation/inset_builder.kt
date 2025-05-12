package de.drick.compose.window_insets_simulation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.plugin.CameraPositionModel
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel
import de.drick.compose.hotpreview.plugin.VisibilityModel
import kotlin.math.roundToInt

fun VisibilityModel.isVisible() = this == VisibilityModel.Visible

fun HotPreviewModel.toWindowInsetsDeviceConfig(): WindowInsetsDeviceConfig {
    val cameraInset = InsetConfig(52.dp)
    val cameraInsetsConfig = if (camera.visibility.isVisible()) {
        when (camera.cameraPosition) {
            CameraPositionModel.Left -> InsetConfigs(left = cameraInset)
            CameraPositionModel.Top -> InsetConfigs(top = cameraInset)
            CameraPositionModel.Right -> InsetConfigs(right = cameraInset)
            CameraPositionModel.Bottom -> InsetConfigs(bottom = cameraInset)
        }
    } else InsetConfigs()
    val statusBarInsetsConfig = if (statusBar.visibility.isVisible())
        InsetConfigs(top = InsetConfig(24.dp))
    else InsetConfigs()
    val navigationBarInsetsConfig = if (navigationBar.visibility.isVisible()) {
        when (navigationBar.mode) {
            NavigationModeModel.GestureBottom -> InsetConfigs(bottom = InsetConfig(32.dp))
            NavigationModeModel.ThreeButtonBottom -> InsetConfigs(bottom = InsetConfig(48.dp))
            NavigationModeModel.ThreeButtonLeft -> InsetConfigs(left = InsetConfig(48.dp))
            NavigationModeModel.ThreeButtonRight -> InsetConfigs(right = InsetConfig(48.dp))
        }
    } else InsetConfigs()
    val captionBarInsetsConfig = if (captionBar.visibility.isVisible())
        InsetConfigs(top = InsetConfig(42.dp))
    else InsetConfigs()

    val allInsets = listOf(
        cameraInsetsConfig,
        statusBarInsetsConfig,
        navigationBarInsetsConfig,
        captionBarInsetsConfig,
    ).unionVisible()

    val systemGesturesHorizontal = if (
        navigationBar.mode == NavigationModeModel.GestureBottom && navigationBar.visibility.isVisible()
    ) 30.dp else 0.dp

    val systemGesturesInsetsConfig = InsetConfigs(
        left = InsetConfig(systemGesturesHorizontal + allInsets.left.size),
        top = InsetConfig(allInsets.top.size),
        right = InsetConfig(systemGesturesHorizontal + allInsets.right.size),
        bottom = InsetConfig(allInsets.bottom.size),
    )

    return WindowInsetsDeviceConfig(
        displayCutout = cameraInsetsConfig,
        statusBars = statusBarInsetsConfig,
        navigationBars = navigationBarInsetsConfig,
        captionBar = captionBarInsetsConfig,
        systemGestures = systemGesturesInsetsConfig,
        mandatorySystemGestures = allInsets,
        tappableElement = allInsets
    )
}

fun InsetConfig.toInsetValueString() = if (visibility != InsetVisibility.Off)
    size.value.roundToInt().toString()
else
    "0"

fun InsetConfigs.toWindowInsetsString() =
    "${left.toInsetValueString()},${top.toInsetValueString()},${right.toInsetValueString()},${bottom.toInsetValueString()}"

fun WindowInsetsDeviceConfig.toWindowInsetsString() = listOf(
    "captionBarInsets(${captionBar.toWindowInsetsString()})",
    "displayCutoutInsets(${displayCutout.toWindowInsetsString()})",
    "imeInsets(${ime.toWindowInsetsString()})",
    "mandatorySystemGestureInsets(${mandatorySystemGestures.toWindowInsetsString()})",
    "navigationBarsInsets(${navigationBars.toWindowInsetsString()})",
    "statusBarsInsets(${statusBars.toWindowInsetsString()})",
    "systemGesturesInsets(${systemGestures.toWindowInsetsString()})",
    "tappableElementInsets(${tappableElement.toWindowInsetsString()})",
    "waterfallInsets(${waterfall.toWindowInsetsString()})"
).joinToString("|")

@Composable
fun WindowInsetsDeviceSimulation(
    deviceConfig: WindowInsetsDeviceConfig,
    hotPreviewAnnotation: HotPreviewModel,
) {
    Box(Modifier.fillMaxSize()) {
        if (hotPreviewAnnotation.statusBar.visibility == VisibilityModel.Visible) {
            val padding = PaddingValues.Absolute(
                left = deviceConfig.navigationBars.left.size + deviceConfig.displayCutout.left.size,
                right = deviceConfig.navigationBars.right.size + deviceConfig.displayCutout.right.size
            )
            StatusBar(
                modifier = Modifier.align(Alignment.TopStart).padding(padding).height(deviceConfig.statusBars.top.size),
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
            val size = when (hotPreviewAnnotation.camera.cameraPosition) {
                CameraPositionModel.Left -> deviceConfig.displayCutout.left.size
                CameraPositionModel.Top -> deviceConfig.displayCutout.top.size
                CameraPositionModel.Right -> deviceConfig.displayCutout.right.size
                CameraPositionModel.Bottom -> deviceConfig.displayCutout.bottom.size
            }
            CameraCutout(
                modifier = Modifier.align(alignment),
                isVertical = with(hotPreviewAnnotation.camera.cameraPosition) {
                    this == CameraPositionModel.Left || this == CameraPositionModel.Right
                },
                cutoutSize = size
            )
        }
        hotPreviewAnnotation.navigationBar.let { navBar ->
            if (navBar.visibility == VisibilityModel.Visible) {
                val size = when (navBar.mode) {
                    NavigationModeModel.GestureBottom -> deviceConfig.navigationBars.bottom.size
                    NavigationModeModel.ThreeButtonBottom -> deviceConfig.navigationBars.bottom.size
                    NavigationModeModel.ThreeButtonLeft -> deviceConfig.navigationBars.left.size
                    NavigationModeModel.ThreeButtonRight -> deviceConfig.navigationBars.right.size
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

fun Collection<InsetConfigs>.unionVisible() = InsetConfigs(
    left = InsetConfig(this.filter { it.left.visibility == InsetVisibility.Visible }.maxOf { it.left.size }),
    top = InsetConfig(this.filter { it.top.visibility == InsetVisibility.Visible }.maxOf { it.top.size }),
    right = InsetConfig(this.filter { it.right.visibility == InsetVisibility.Visible }.maxOf { it.right.size }),
    bottom = InsetConfig(this.filter { it.bottom.visibility == InsetVisibility.Visible }.maxOf { it.bottom.size })
)
