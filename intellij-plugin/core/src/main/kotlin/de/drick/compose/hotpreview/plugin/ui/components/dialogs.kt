package de.drick.compose.hotpreview.plugin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.jewel.foundation.theme.JewelTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T>PanelDialog(
    data: T?,
    onClose: () -> Unit,
    content: @Composable (T) -> Unit
) {
    val dialogProperties = remember {
        DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            scrimColor = Color.Transparent
        )
    }
    if (data != null) {
        if (LocalInspectionMode.current) {
            Box(
                Modifier
                    .shadow(16.dp)
                    .background(JewelTheme.globalColors.panelBackground)
            ) {
                content(data)
            }
        } else {
            Dialog(
                properties = dialogProperties,
                onDismissRequest = onClose
            ) {
                Box(
                    Modifier
                        .shadow(16.dp)
                        .background(JewelTheme.globalColors.panelBackground)
                ) {
                    content(data)
                }
            }
        }
    }
}