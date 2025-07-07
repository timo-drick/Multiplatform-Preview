package de.drick.compose.window_insets_simulation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import de.drick.compose.hotpreview.DisplayCutoutMode
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.NavigationBarMode

@Composable
fun HighlightInsets(
    windowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    fill: Boolean = false
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    Spacer(
        modifier.fillMaxSize().drawWithCache {
            val leftSize = windowInsets.getLeft(density, layoutDirection).toFloat()
            val topSize = windowInsets.getTop(density).toFloat()
            val rightSize = windowInsets.getRight(density, layoutDirection).toFloat()
            val bottomSize = windowInsets.getBottom(density).toFloat()
            val color = Color.Red
            val alpha = .6f
            val rectStyle = Fill
            val lineStyle = Stroke(width = 4f)
            onDrawWithContent {
                drawContent()
                if (fill) {
                    drawRect(color, Offset.Zero, Size(leftSize, size.height), alpha, rectStyle)
                    drawRect(color, Offset.Zero, size = Size(size.width, topSize), alpha, rectStyle)
                    drawRect(color, Offset(size.width - rightSize, 0f), size = Size(rightSize, size.height), alpha, rectStyle)
                    drawRect(color, Offset(0f, size.height - bottomSize), size = Size(size.width, bottomSize), alpha, rectStyle)
                } else {
                    if (leftSize > 0)
                        drawLine(color, Offset(leftSize, 0f), Offset(leftSize, size.height), 5f)
                    if (rightSize > 0)
                        drawLine(color, Offset(size.width - rightSize, 0f), Offset(size.width - rightSize, size.height), 5f)
                    if (topSize > 0)
                        drawLine(color, Offset(0f, topSize), Offset(size.width, topSize), 5f)
                    if (bottomSize > 0)
                        drawLine(color, Offset(0f, size.height - bottomSize), Offset(size.width, size.height - bottomSize), 5f)
                }
            }
        }
    )
}

@HotPreview(
    widthDp = 411, heightDp = 891, density = 2.625f,
    statusBar = true,
    navigationBar = NavigationBarMode.ThreeButtonBottom,
    displayCutout = DisplayCutoutMode.CameraTop,
)
@HotPreview(
    widthDp = 891, heightDp = 411, density = 2.625f,
    statusBar = true,
    navigationBar = NavigationBarMode.GestureBottom,
    displayCutout = DisplayCutoutMode.CameraRight,
)
@Composable
private fun PreviewHighlightInsets() {
    HighlightInsets(WindowInsets.safeContent, Modifier.fillMaxSize())
}