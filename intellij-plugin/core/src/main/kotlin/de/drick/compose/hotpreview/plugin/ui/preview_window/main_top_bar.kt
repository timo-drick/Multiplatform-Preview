package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.ui.components.PanelDialog
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys


@Composable
fun MainTopBar(
    compilingInProgress: Boolean,
    groups: Set<String>,
    selectedGroup: String?,
    isOutdatedHotPreviewAnnotation: Boolean,
    modifier: Modifier = Modifier,
    onAction: (HotPreviewAction) -> Unit,
) {
    //var showOutdatedDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groups.isNotEmpty()) {
            Dropdown(
                menuContent = {
                    selectableItem(
                        selected = selectedGroup == null,
                        onClick = {
                            onAction(HotPreviewAction.SelectGroup(null))
                        }
                    ) {
                        Text("All")
                    }
                    separator()
                    groups.forEach { group ->
                        selectableItem(
                            selected = group == selectedGroup,
                            onClick = {
                                onAction(HotPreviewAction.SelectGroup(group))
                            }
                        ) {
                            Text(group)
                        }
                    }
                }
            ) {
                Text(selectedGroup ?: "All")
            }
        }
        ActionButton(
            onClick = { onAction(HotPreviewAction.ToggleLayout) }
        ) {
            Icon(key = AllIconsKeys.Debugger.RestoreLayout, contentDescription = "Layout switch")
        }

        Spacer(Modifier.weight(1f))
        ActionButton(
            onClick = { onAction(HotPreviewAction.Refresh) },
            enabled = compilingInProgress.not(),
        ) {
            if (compilingInProgress) {
                CircularProgressIndicator()
            } else {
                Icon(key = AllIconsKeys.General.Refresh, contentDescription = "Refresh")
            }
        }
        ActionButton(
            onClick = { onAction(HotPreviewAction.OpenSettings) }
        ) {
            Icon(key = AllIconsKeys.General.Settings, contentDescription = "Settings")
        }
        /*if (isOutdatedHotPreviewAnnotation) {
            DialogOutdatedAnnotation(
                isShown = showOutdatedDialog,
                onCancel = { showOutdatedDialog = false }
            )
            ActionButton(
                onClick = { showOutdatedDialog = true }
            ) {
                Icon(AllIconsKeys.General.Information, contentDescription = "Update annotation")
            }
        }*/
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogOutdatedAnnotation(isShown: Boolean, onCancel: () -> Unit) {
    PanelDialog(data = isShown, onClose = onCancel) {
        Text("HotPreview annotation is outdated. Please update the annotation in your code.")
    }
}

@HotPreview(widthDp = 300)
@Composable
private fun PreviewTopAppBar() {
    SelfPreviewTheme {
        MainTopBar(
            modifier = Modifier.fillMaxWidth(),
            compilingInProgress = false,
            groups = emptySet(),
            selectedGroup = null,
            isOutdatedHotPreviewAnnotation = true,
            onAction = {}
        )
    }
}

@HotPreview(widthDp = 300)
@HotPreview(group = "Light", widthDp = 300, darkMode = false)
@Composable
private fun PreviewTopAppBarGroups() {
    val groups = setOf(
        "Dark",
        "Light"
    )
    SelfPreviewTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainTopBar(
                modifier = Modifier.fillMaxWidth(),
                compilingInProgress = false,
                groups = groups,
                selectedGroup = groups.first(),
                isOutdatedHotPreviewAnnotation = false,
                onAction = {}
            )
        }
    }
}

@HotPreview(widthDp = 300, heightDp = 100)
@Composable
private fun PreviewAnnotationOutdatedDialog() {
    SelfPreviewTheme {
        DialogOutdatedAnnotation(
            isShown = true,
            onCancel = {}
        )
    }
}