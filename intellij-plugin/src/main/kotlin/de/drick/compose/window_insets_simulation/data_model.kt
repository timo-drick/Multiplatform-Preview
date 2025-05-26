package de.drick.compose.window_insets_simulation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class InsetPos {
    LEFT, TOP, RIGHT, BOTTOM
}

enum class InsetVisibility {
    Visible,
    /**
     * Setting status or navigation bars visibility to Hidden can be used to test
     * code that uses WindowInsets.systemBarsIgnoringVisibility.
     * inset will be 0 but insetIgnoringVisibility will be normal
     */
    Hidden,
    /**
     * Insets visible and hidden insets are 0
     */
    Off
}

data class InsetConfig(
    val size: Dp,
    val visibility: InsetVisibility = InsetVisibility.Visible
)

data class InsetConfigs(
    val left: InsetConfig = InsetConfig(0.dp),
    val top: InsetConfig = InsetConfig(0.dp),
    val right: InsetConfig = InsetConfig(0.dp),
    val bottom: InsetConfig = InsetConfig(0.dp)
)

data class WindowInsetsDeviceConfig(
    val captionBar: InsetConfigs = InsetConfigs(),
    val displayCutout: InsetConfigs = InsetConfigs(),
    val ime: InsetConfigs = InsetConfigs(),
    val mandatorySystemGestures: InsetConfigs = InsetConfigs(),
    val navigationBars: InsetConfigs = InsetConfigs(),
    val statusBars: InsetConfigs = InsetConfigs(),
    val systemGestures: InsetConfigs = InsetConfigs(),
    val tappableElement: InsetConfigs = InsetConfigs(),
    val waterfall: InsetConfigs = InsetConfigs(),
)



