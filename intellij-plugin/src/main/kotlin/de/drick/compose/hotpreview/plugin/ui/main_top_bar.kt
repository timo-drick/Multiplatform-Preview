package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

interface TopBarAction {
    data object Refresh: TopBarAction
    data object OpenSettings: TopBarAction
    data object ToggleLayout: TopBarAction
    data class SelectGroup(val group: String?): TopBarAction
}

@Composable
fun MainTopBar(
    compilingInProgress: Boolean,
    groups: Set<String>,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
    onAction: (TopBarAction) -> Unit,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groups.isNotEmpty()) {
            Dropdown(
                menuContent = {
                    selectableItem(
                        selected = selectedGroup == null,
                        onClick = {
                            onAction(TopBarAction.SelectGroup(null))
                        }
                    ) {
                        Text("All")
                    }
                    separator()
                    groups.forEach { group ->
                        selectableItem(
                            selected = group == selectedGroup,
                            onClick = {
                                onAction(TopBarAction.SelectGroup(group))
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
            onClick = { onAction(TopBarAction.ToggleLayout) }
        ) {
            Icon(AllIconsKeys.Debugger.RestoreLayout, contentDescription = "Layout switch")
        }

        Spacer(Modifier.weight(1f))
        ActionButton(
            onClick = { onAction(TopBarAction.Refresh) },
            enabled = compilingInProgress.not(),
        ) {
            if (compilingInProgress) {
                CircularProgressIndicator()
            } else {
                Icon(AllIconsKeys.General.Refresh, contentDescription = "Refresh")
            }
        }
        ActionButton(
            onClick = { onAction(TopBarAction.OpenSettings) }
        ) {
            Icon(AllIconsKeys.General.Settings, contentDescription = "Settings")
        }
    }
}

@HotPreview(widthDp = 500)
@Composable
private fun PreviewTopAppBar() {
    SelfPreviewTheme {
        MainTopBar(
            modifier = Modifier.fillMaxWidth(),
            compilingInProgress = false,
            groups = emptySet(),
            selectedGroup = null,
            onAction = {}
        )
    }
}

@HotPreview(widthDp = 500)
@HotPreview(group = "Light", widthDp = 500, darkMode = false)
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
                onAction = {}
            )
            Text("More content", modifier = Modifier.padding(100.dp))
        }
    }
}