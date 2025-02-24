package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

interface TopBarAction {
    data object Refresh: TopBarAction
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
        modifier = modifier
            .background(JewelTheme.globalColors.panelBackground)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (groups.isNotEmpty()) {
            Text("Group")
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
        Spacer(Modifier.weight(1f))
        ActionButton(
            onClick = { onAction(TopBarAction.Refresh) },
            enabled = compilingInProgress.not(),
            focusable = false
        ) {
            if (compilingInProgress) {
                CircularProgressIndicator()
            } else {
                Icon(AllIconsKeys.General.Refresh, contentDescription = "Refresh")
            }
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
        MainTopBar(
            modifier = Modifier.fillMaxWidth(),
            compilingInProgress = false,
            groups = groups,
            selectedGroup = groups.first(),
            onAction = {}
        )
    }
}