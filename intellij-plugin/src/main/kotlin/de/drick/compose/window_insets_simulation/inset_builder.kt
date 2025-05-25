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
import androidx.compose.ui.unit.max
import de.drick.compose.hotpreview.plugin.DisplayCutoutModeModel
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel
import kotlin.math.roundToInt

fun HotPreviewModel.toWindowInsetsDeviceConfig(): WindowInsetsDeviceConfig {
    val cameraInset = InsetConfig(52.dp)
    val cameraInsetsConfig = when (displayCutout) {
        DisplayCutoutModeModel.Off -> InsetConfigs()
        DisplayCutoutModeModel.CameraLeft -> InsetConfigs(left = cameraInset)
        DisplayCutoutModeModel.CameraTop -> InsetConfigs(top = cameraInset)
        DisplayCutoutModeModel.CameraRight -> InsetConfigs(right = cameraInset)
        DisplayCutoutModeModel.CameraBottom -> InsetConfigs(bottom = cameraInset)
    }
    val statusBarHeight = max(22.dp * fontScale, cameraInsetsConfig.top.size)
    val statusBarInsetsConfig = if (statusBar) InsetConfigs(top = InsetConfig(statusBarHeight)) else InsetConfigs()
    val navigationBarInsetsConfig = when (navigationBar) {
        NavigationModeModel.Off -> InsetConfigs()
        NavigationModeModel.GestureBottom -> InsetConfigs(bottom = InsetConfig(32.dp + cameraInsetsConfig.bottom.size))
        NavigationModeModel.ThreeButtonBottom -> InsetConfigs(bottom = InsetConfig(48.dp + cameraInsetsConfig.bottom.size))
        NavigationModeModel.ThreeButtonLeft -> InsetConfigs(left = InsetConfig(48.dp + cameraInsetsConfig.left.size))
        NavigationModeModel.ThreeButtonRight -> InsetConfigs(right = InsetConfig(48.dp + cameraInsetsConfig.right.size))
    }
    val captionBarInsetsConfig = if (captionBar) InsetConfigs(top = InsetConfig(42.dp)) else InsetConfigs()

    val allInsets = listOf(
        cameraInsetsConfig,
        statusBarInsetsConfig,
        navigationBarInsetsConfig,
        captionBarInsetsConfig,
    ).unionVisible()

    val systemGesturesHorizontal = if (navigationBar == NavigationModeModel.GestureBottom) 30.dp else 0.dp

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
        val statusBarPadding = PaddingValues.Absolute(
            left = deviceConfig.navigationBars.left.size + deviceConfig.displayCutout.left.size,
            right = deviceConfig.navigationBars.right.size + deviceConfig.displayCutout.right.size
        )
        if (hotPreviewAnnotation.statusBar) {
            StatusBar(
                modifier = Modifier.align(Alignment.TopStart).padding(statusBarPadding).height(deviceConfig.statusBars.top.size),
                isDarkMode = hotPreviewAnnotation.darkMode,
            )
        }
        if (hotPreviewAnnotation.captionBar) {
            CaptionBar(
                modifier = Modifier.align(Alignment.TopStart).padding(statusBarPadding).height(deviceConfig.captionBar.top.size),
                isDarkMode = hotPreviewAnnotation.darkMode,
            )
        }
        if (hotPreviewAnnotation.displayCutout != DisplayCutoutModeModel.Off) {
            val alignment = when (hotPreviewAnnotation.displayCutout) {
                DisplayCutoutModeModel.CameraLeft -> AbsoluteAlignment.TopLeft
                DisplayCutoutModeModel.CameraTop -> AbsoluteAlignment.TopLeft
                DisplayCutoutModeModel.CameraRight -> AbsoluteAlignment.TopRight
                DisplayCutoutModeModel.CameraBottom -> AbsoluteAlignment.BottomLeft
                DisplayCutoutModeModel.Off -> AbsoluteAlignment.TopLeft // This case is already handled above
            }
            val size = when (hotPreviewAnnotation.displayCutout) {
                DisplayCutoutModeModel.CameraLeft -> deviceConfig.displayCutout.left.size
                DisplayCutoutModeModel.CameraTop -> deviceConfig.displayCutout.top.size
                DisplayCutoutModeModel.CameraRight -> deviceConfig.displayCutout.right.size
                DisplayCutoutModeModel.CameraBottom -> deviceConfig.displayCutout.bottom.size
                DisplayCutoutModeModel.Off -> 0.dp // This case is already handled above
            }
            CameraCutout(
                modifier = Modifier.align(alignment),
                isVertical = with(hotPreviewAnnotation.displayCutout) {
                    this == DisplayCutoutModeModel.CameraLeft || this == DisplayCutoutModeModel.CameraRight
                },
                cutoutSize = size
            )
        }
        hotPreviewAnnotation.navigationBar.let { navBar ->
            if (navBar != NavigationModeModel.Off) {
                val size = when (navBar) {
                    NavigationModeModel.GestureBottom -> 32.dp
                    NavigationModeModel.Off -> 0.dp // This case is already handled above
                    else -> 42.dp
                }
                val alignment = when (navBar) {
                    NavigationModeModel.GestureBottom -> AbsoluteAlignment.BottomLeft
                    NavigationModeModel.ThreeButtonBottom -> AbsoluteAlignment.BottomLeft
                    NavigationModeModel.ThreeButtonLeft -> AbsoluteAlignment.TopLeft
                    NavigationModeModel.ThreeButtonRight -> AbsoluteAlignment.TopRight
                    NavigationModeModel.Off -> AbsoluteAlignment.TopLeft // This case is already handled above
                }
                /*
                    // It looks like Android itself do not prevent the navigation bar from overlapping the display cutout
                    // Or it does it by just choosing the opposite side for the NavigationBar to the DisplayCutout
                    val padding = when (navBar) {
                    NavigationModeModel.Off -> PaddingValues(0.dp)
                    NavigationModeModel.GestureBottom,
                    NavigationModeModel.ThreeButtonBottom -> PaddingValues(bottom = deviceConfig.displayCutout.bottom.size)
                    NavigationModeModel.ThreeButtonLeft -> PaddingValues.Absolute(left = deviceConfig.displayCutout.left.size)
                    NavigationModeModel.ThreeButtonRight -> PaddingValues.Absolute(right = deviceConfig.displayCutout.right.size)
                }*/
                NavigationBar(
                    modifier = Modifier.align(alignment),//.padding(padding),
                    navMode = navBar,
                    size = size,
                    isDarkMode = hotPreviewAnnotation.darkMode,
                    backgroundAlpha = if (hotPreviewAnnotation.navigationBarContrastEnforced) 0.5f else 0f,
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
