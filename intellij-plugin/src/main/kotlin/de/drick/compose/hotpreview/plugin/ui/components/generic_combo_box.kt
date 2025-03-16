package de.drick.compose.hotpreview.plugin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.ui.preview_window.SelfPreviewTheme
import de.drick.compose.hotpreview.plugin.ui.guttericon.fontScaleTemplates
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.lazy.SelectableLazyColumn
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListScope
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.items
import org.jetbrains.jewel.foundation.lazy.rememberSelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.visibleItemsRange
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.ComboBox
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.theme.comboBoxStyle


@HotPreview(widthDp = 200)
@Composable
private fun PreviewGenericComboBox() {
    val list = fontScaleTemplates
    SelfPreviewTheme {
        GenericComboBox(
            labelText = "Test",
            items = list,
            selectedItem = null,
            onSelectItem = {},
            listItemContent = { item, isSelected, _, isItemHovered, isPreviewSelection ->
                SimpleListItem(
                    text = item.name,
                    state = ListItemState(isSelected, isItemHovered, isPreviewSelection)
                )
            }
        )
    }
}

@Composable
fun <T: Any>GenericComboBox(
    labelText: String,
    items: List<T>,
    selectedItem: T?,
    onSelectItem: (T) -> Unit = {},
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    maxPopupHeight: Dp = Dp.Unspecified,
    onHoverItemChange: (T) -> Unit = {},
    onListHoverChange: (Boolean) -> Unit = {},
    onPopupStateChange: (Boolean) -> Unit = {},
    listItemContent: @Composable (T, Boolean, Boolean, Boolean, Boolean) -> Unit,
) {
    val scrollState = rememberSelectableLazyListState()
    val selectedItem = remember(selectedItem) {
        if (selectedItem == null)
            -1
        else
            items.indexOf(selectedItem)
    }
    var isListHovered by remember { mutableStateOf(false) }
    var hoverItemIndex: Int? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedItem) {
        scrollState.selectedKeys = items.getOrNull(selectedItem)?.let { setOf(it) } ?: emptySet()
    }
    val contentPadding = JewelTheme.comboBoxStyle.metrics.popupContentPadding
    val popupMaxHeight =
        if (maxPopupHeight == Dp.Unspecified) {
            JewelTheme.comboBoxStyle.metrics.maxPopupHeight
        } else {
            maxPopupHeight
        }
    fun selectItem(index: Int) {
        onSelectItem(items[index])
    }
    val onArrowDownPress: () -> Unit = {
        hoverItemIndex?.let { hoveredIndex ->
            selectItem(hoveredIndex)
            hoverItemIndex = null
        }
        selectItem(selectedItem.plus(1).coerceAtMost(items.lastIndex))
        scope.launch { scrollState.lazyListState.scrollToIndex(selectedItem) }
    }
    val onArrowUpPress: () -> Unit = {
        hoverItemIndex?.let { hoveredIndex ->
            selectItem(hoveredIndex)
            hoverItemIndex = null
        }
        selectItem(selectedItem.minus(1).coerceAtLeast(0))
        scope.launch { scrollState.lazyListState.scrollToIndex(selectedItem) }
    }

    fun onSelectedIndexChange(selectedItemIndex: Int) {
        selectItem(selectedItemIndex)
    }

    fun contentItems(
        items: List<T>,
        onHoverItemChange: (T) -> Unit,
        listItemContent: @Composable (T, Boolean, Boolean, Boolean, Boolean) -> Unit,
    ): SelectableLazyListScope.() -> Unit = {
        items(
            items = items,
            itemContent = { item ->
                var isItemHovered by remember { mutableStateOf(false) }
                Box(
                    modifier =
                        Modifier.onHover {
                            isItemHovered = it
                            if (isItemHovered) {
                                hoverItemIndex = items.indexOf(item)
                                onHoverItemChange(item)
                            }
                        }
                ) {
                    listItemContent(
                        item,
                        isSelected,
                        isActive,
                        isItemHovered || items.indexOf(item) == hoverItemIndex,
                        hoverItemIndex != null,
                    )
                }
            },
        )
    }

    @Composable
    fun list(
        items: List<T>,
        scrollState: SelectableLazyListState,
        contentPadding: PaddingValues,
        onListHoverChange: (Boolean) -> Unit,
        onHoverItemChange: (T) -> Unit,
        listItemContent: @Composable (T, Boolean, Boolean, Boolean, Boolean) -> Unit,
    ) {
        VerticallyScrollableContainer(
            scrollState = scrollState.lazyListState,
            modifier =
                Modifier.onHover {
                    isListHovered = it
                    onListHoverChange(it)
                },
        ) {
            SelectableLazyColumn(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                selectionMode = SelectionMode.Single,
                state = scrollState,
                onSelectedIndexesChange = { selectedItemsIndexes ->
                    if (selectedItemsIndexes.isEmpty()) return@SelectableLazyColumn
                    if (selectedItemsIndexes.first() == selectedItem) return@SelectableLazyColumn
                    onSelectedIndexChange(selectedItemsIndexes.first())
                },
                content = contentItems(items, onHoverItemChange, listItemContent),
            )
        }
    }

    ComboBox(
        modifier = modifier,
        isEnabled = isEnabled,
        labelText = labelText,
        outline = Outline.None,
        maxPopupHeight = popupMaxHeight,
        style = JewelTheme.comboBoxStyle,
        textStyle = JewelTheme.defaultTextStyle,
        onArrowDownPress = onArrowDownPress,
        onArrowUpPress = onArrowUpPress,
        onPopupStateChange = onPopupStateChange,
    ) {
        list(
            items,
            scrollState,
            contentPadding,
            onListHoverChange,
            onHoverItemChange,
            listItemContent,
        )
    }
}

@Composable
fun <T: Any>GenericSelectableList(
    items: List<T>,
    modifier: Modifier = Modifier,
    maxPopupHeight: Dp = Dp.Unspecified,
    onSelectedItemChange: (T) -> Unit = {},
    onHoverItemChange: (T) -> Unit = {},
    onListHoverChange: (Boolean) -> Unit = {},
    listItemContent: @Composable (T, Boolean, Boolean, Boolean, Boolean) -> Unit,
) {
    val scrollState = rememberSelectableLazyListState()
    var selectedItem by remember { mutableIntStateOf(0) }
    var isListHovered by remember { mutableStateOf(false) }
    var hoverItemIndex: Int? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedItem) { scrollState.selectedKeys = setOf(items[selectedItem]) }
    val contentPadding = JewelTheme.comboBoxStyle.metrics.popupContentPadding
    val popupMaxHeight =
        if (maxPopupHeight == Dp.Unspecified) {
            JewelTheme.comboBoxStyle.metrics.maxPopupHeight
        } else {
            maxPopupHeight
        }

    val onArrowDownPress: () -> Unit = {
        hoverItemIndex?.let { hoveredIndex ->
            selectedItem = hoveredIndex
            hoverItemIndex = null
        }
        selectedItem = selectedItem.plus(1).coerceAtMost(items.lastIndex)
        scope.launch { scrollState.lazyListState.scrollToIndex(selectedItem) }
    }
    val onArrowUpPress: () -> Unit = {
        hoverItemIndex?.let { hoveredIndex ->
            selectedItem = hoveredIndex
            hoverItemIndex = null
        }
        selectedItem = selectedItem.minus(1).coerceAtLeast(0)
        scope.launch { scrollState.lazyListState.scrollToIndex(selectedItem) }
    }

    fun onSelectedIndexChange(selectedItemIndex: Int) {
        selectedItem = selectedItemIndex
        onSelectedItemChange(items[selectedItemIndex])
    }

    fun contentItems(
        items: List<T>,
        onHoverItemChange: (T) -> Unit,
        listItemContent: @Composable (T, Boolean, Boolean, Boolean, Boolean) -> Unit,
    ): SelectableLazyListScope.() -> Unit = {
        items(
            items = items,
            itemContent = { item ->
                var isItemHovered by remember { mutableStateOf(false) }
                Box(
                    modifier =
                        Modifier.onHover {
                            isItemHovered = it
                            if (isItemHovered) {
                                hoverItemIndex = items.indexOf(item)
                                onHoverItemChange(item)
                            }
                        }
                ) {
                    listItemContent(
                        item,
                        isSelected,
                        isActive,
                        isItemHovered || items.indexOf(item) == hoverItemIndex,
                        hoverItemIndex != null,
                    )
                }
            },
        )
    }

    VerticallyScrollableContainer(
        scrollState = scrollState.lazyListState,
        modifier =
            modifier.heightIn(max = popupMaxHeight).onHover {
                isListHovered = it
                onListHoverChange(it)
            },
    ) {
        SelectableLazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = popupMaxHeight).padding(contentPadding),
            selectionMode = SelectionMode.Single,
            state = scrollState,
            onSelectedIndexesChange = { selectedItemsIndexes ->
                if (selectedItemsIndexes.isEmpty()) return@SelectableLazyColumn
                if (selectedItemsIndexes.first() == selectedItem) return@SelectableLazyColumn
                onSelectedIndexChange(selectedItemsIndexes.first())
            },
            content = contentItems(items, onHoverItemChange, listItemContent),
        )
    }
}

private suspend fun LazyListState.scrollToIndex(itemIndex: Int) {
    val isFirstItemFullyVisible = firstVisibleItemScrollOffset == 0

    val lastItemInfo = layoutInfo.visibleItemsInfo.lastOrNull()
    val isLastItemFullyVisible =
        if (lastItemInfo != null) {
            layoutInfo.viewportEndOffset - lastItemInfo.offset >= lastItemInfo.size
        } else {
            false
        }

    val lastItemInfoSize = lastItemInfo?.size ?: 0
    when {
        itemIndex < visibleItemsRange.first -> scrollToItem((itemIndex - 1).coerceAtLeast(0))
        itemIndex == visibleItemsRange.first && !isFirstItemFullyVisible -> scrollToItem(itemIndex)
        itemIndex == visibleItemsRange.last && !isLastItemFullyVisible -> {
            scrollToItem(itemIndex, layoutInfo.viewportEndOffset - lastItemInfoSize)
        }

        itemIndex > visibleItemsRange.last -> {
            // First scroll assuming the new item has the same height as the current last item
            scrollToItem(itemIndex, layoutInfo.viewportEndOffset - lastItemInfoSize)

            // After scrolling, check if we need to adjust due to different item sizes
            val newLastItemInfo = layoutInfo.visibleItemsInfo.lastOrNull() ?: return
            if (newLastItemInfo.size != lastItemInfoSize) {
                scrollToItem(itemIndex, layoutInfo.viewportEndOffset - newLastItemInfo.size)
            }
        }
    }
}
