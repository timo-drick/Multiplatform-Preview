package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.jewel.foundation.theme.JewelTheme


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogGutterIconSettings(
    viewModel: GutterIconViewModelI?,
    onClose: () -> Unit
) {
    val dialogProperties = remember {
        DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            scrimColor = Color.Transparent
        )
    }
    if (viewModel != null) {
        Dialog(
            properties = dialogProperties,
            onDismissRequest = onClose
        ) {
            Box(Modifier
                .background(JewelTheme.globalColors.panelBackground)
                .shadow(16.dp)
            ) {
                GutterIconAnnotationSettings(viewModel)
            }
        }
    }
}