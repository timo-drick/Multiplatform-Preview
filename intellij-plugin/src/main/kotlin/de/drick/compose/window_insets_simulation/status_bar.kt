package de.drick.compose.window_insets_simulation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview

@HotPreview(widthDp = 250, density = 2.0f)
@HotPreview(widthDp = 250, darkMode = false, density = 2.0f)
@HotPreview(widthDp = 250, fontScale = 1.5f)
@HotPreview(widthDp = 250, fontScale = 2.0f)
@HotPreview(widthDp = 250, fontScale = 3.0f)
@Composable
private fun PreviewStatusBar() {
    val fontScaling = LocalDensity.current.fontScale
    val backgroundColor = if (isSystemInDarkTheme())
        Color.Black
    else
        Color.White
    Box(
        modifier = Modifier.background(backgroundColor)
    ) {
        StatusBar(Modifier.height(22.dp * fontScaling))
    }
}

@Composable
fun StatusBar(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    val contentColor = if (isDarkMode) Color.White else Color.Black
    val fontScaling = LocalDensity.current.fontScale
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "11:52",
            style = TextStyle.Default.copy(color = contentColor)
        )
        Spacer(Modifier.weight(1f))
        Image(
            modifier = Modifier.size(18.dp * fontScaling),
            imageVector = Icons_Filled_Wifi,
            contentDescription = "Wifi icon",
            colorFilter = ColorFilter.tint(contentColor),
        )
        Image(
            modifier = Modifier.size(18.dp * fontScaling),
            imageVector = Icons_Filled_BatteryChargingFull,
            contentDescription = "Battery icon",
            colorFilter = ColorFilter.tint(contentColor)
        )
    }
}
