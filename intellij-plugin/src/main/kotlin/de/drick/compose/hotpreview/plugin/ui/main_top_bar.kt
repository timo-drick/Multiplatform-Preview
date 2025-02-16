package de.drick.compose.hotpreview.plugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.PreviewGroup
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys

interface TopBarAction {
    data object Refresh: TopBarAction
    data class UpdateGroup(val previewGroup: PreviewGroup): TopBarAction
}

@Composable
fun MainTopBar(
    compilingInProgress: Boolean,
    groups: List<PreviewGroup> = emptyList(),
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
            val selectedGroups = remember(groups) {
                groups.filter { it.enabled }
                    .joinToString(",") { it.name }
            }
            Text("Group filter")
            Dropdown(
                menuContent = {
                    groups.forEach { group ->
                        selectableItem(
                            selected = group.enabled,
                            onClick = {
                                val updatedGroup = group.copy(enabled = group.enabled.not())
                                onAction(TopBarAction.UpdateGroup(updatedGroup))
                            }
                        ) {
                            Text(group.name)
                        }
                    }
                }
            ) {
                Text(selectedGroups)
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
            groups = emptyList(),
            onAction = {}
        )
    }
}

@HotPreview(widthDp = 500)
@HotPreview(group = "Light", widthDp = 500, darkMode = false)
@Composable
private fun PreviewTopAppBarGroups() {
    val groups = listOf(
        PreviewGroup("Dark", true),
        PreviewGroup("Light", false)
    )
    SelfPreviewTheme {
        MainTopBar(
            modifier = Modifier.fillMaxWidth(),
            compilingInProgress = false,
            groups = groups,
            onAction = {}
        )
    }
}