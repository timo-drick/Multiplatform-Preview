package de.drick.compose.window_insets_simulation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview

enum class CameraCutoutMode {
    None, Middle, Start, End
}
private val cutoutSize = 80.dp

@HotPreview(heightDp = 200)
@Composable
private fun PreviewCameraCutoutVertical() {
    CameraCutout(
        cutoutSize = cutoutSize,
        cutoutMode = CameraCutoutMode.Middle,
        isVertical = true
    )
}
@HotPreview(widthDp = 200)
@Composable
private fun PreviewCameraCutoutHorizontal() {
    CameraCutout(
        cutoutSize = cutoutSize,
        cutoutMode = CameraCutoutMode.Middle,
        isVertical = false
    )
}

@Composable
fun CameraCutout(
    modifier: Modifier = Modifier,
    cutoutMode: CameraCutoutMode = CameraCutoutMode.Middle,
    isVertical: Boolean = false,
    cutoutSize: Dp = 24.dp
) {
    if (cutoutMode == CameraCutoutMode.None) return
    if (isVertical) {
        val alignment = when (cutoutMode) {
            CameraCutoutMode.None -> Arrangement.Center
            CameraCutoutMode.Middle -> Arrangement.Center
            CameraCutoutMode.Start -> Arrangement.Top
            CameraCutoutMode.End -> Arrangement.Bottom
        }
        Column(
            modifier = modifier
                .width(cutoutSize)
                .padding(8.dp)
                .fillMaxHeight(),
            verticalArrangement = alignment,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                imageVector = Icons_Filled_Lens,
                contentDescription = "Camera lens",
                contentScale = ContentScale.FillWidth
            )
        }
    } else {
        val alignment = when (cutoutMode) {
            CameraCutoutMode.None -> Arrangement.Center
            CameraCutoutMode.Middle -> Arrangement.Center
            CameraCutoutMode.Start -> Arrangement.Start
            CameraCutoutMode.End -> Arrangement.End
        }
        Row(
            modifier = modifier
                .height(cutoutSize)
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = alignment,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.fillMaxHeight(),
                imageVector = Icons_Filled_Lens,
                contentDescription = "Camera lens",
                contentScale = ContentScale.FillHeight
            )
        }
    }
}
