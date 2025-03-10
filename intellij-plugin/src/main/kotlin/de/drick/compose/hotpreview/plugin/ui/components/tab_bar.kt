package de.drick.compose.hotpreview.plugin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.ui.SelfPreviewTheme
import de.drick.compose.hotpreview.plugin.ui.jewel.Typography
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.iconButtonStyle
import org.jetbrains.jewel.ui.theme.segmentedControlStyle


@HotPreview(widthDp = 200)
@HotPreview(widthDp = 200, darkMode = false)
@Composable
private fun PreviewTabBar() {
    val tabNames = listOf("Preview", "Code", "Setting", "Preview2")

    SelfPreviewTheme {
        TabBar(
            selectedTab = 1,
            tabNames = tabNames,
            onSelectTab = {}
        )
    }
}

@Composable
fun TabBar(
    selectedTab: Int,
    tabNames: List<String>,
    onSelectTab: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(tabNames) { index, name ->
                val isSelected = index == selectedTab
                val background = if (isSelected)
                    JewelTheme.segmentedControlStyle.colors.borderPressed
                else
                    SolidColor(Color.Transparent)
                ActionButton(
                    modifier = Modifier.background(
                        brush = background,
                        shape = RoundedCornerShape(JewelTheme.iconButtonStyle.metrics.cornerSize)
                    ),
                    onClick = { onSelectTab(index) }
                ) {
                    Text(
                        text = name,
                        style = Typography.labelTextStyle(),
                    )
                }
            }
        }
        if (listState.canScrollForward || listState.canScrollBackward) {
            Dropdown(
                modifier = Modifier.width(24.dp),
                menuContent = {
                    tabNames.forEachIndexed { index, item ->
                        selectableItem(
                            selected = index == selectedTab,
                            onClick = {
                                onSelectTab(index)
                                scope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        ) {
                            Text(item)
                        }
                    }
                },
            ) {
                //Spacer(Modifier.width(8.dp))
            }
        }
    }
}


/*TabData.Default(
                selected = index == selectedTab,
                closable = false,
                content = { tabState ->
                    SimpleTabContent(
                        state = tabState,
                        label = { Text(data.toName()) }
                    )
                },
                onClick = { onSelectTab(index) }
            )*/

/*TabStrip(
            modifier = Modifier.fillMaxWidth(),
            tabs = tabs,
            style = JewelTheme.editorTabStyle
        )*/