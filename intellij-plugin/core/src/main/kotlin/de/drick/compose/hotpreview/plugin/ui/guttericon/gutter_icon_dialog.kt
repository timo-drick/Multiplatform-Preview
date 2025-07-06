package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import de.drick.compose.hotpreview.plugin.ui.components.PanelDialog


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogGutterIconSettings(
    viewModel: GutterIconViewModelI?,
    onClose: () -> Unit
) {
    PanelDialog(
        data = viewModel,
        onClose = onClose,
        content = { viewModel ->
            GutterIconAnnotationSettings(viewModel)
        }
    )
}