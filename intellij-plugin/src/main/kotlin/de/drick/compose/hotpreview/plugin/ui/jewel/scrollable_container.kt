package de.drick.compose.hotpreview.plugin.ui.jewel

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.ui.SelfPreviewTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.HorizontalScrollbar
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.component.styling.ScrollbarStyle
import org.jetbrains.jewel.ui.component.styling.ScrollbarVisibility.AlwaysVisible
import org.jetbrains.jewel.ui.component.styling.ScrollbarVisibility.WhenScrolling
import org.jetbrains.jewel.ui.theme.scrollbarStyle
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import kotlin.math.max
import kotlin.time.Duration


@OptIn(ExperimentalLayoutApi::class)
@HotPreview(widthDp = 250, heightDp = 300)
@Composable
private fun PreviewScrollableFlowRow() {
    SelfPreviewTheme {
        val hScrollState = rememberScrollState()
        val vScrollState = rememberScrollState()
        ScrollableContainer(
            verticalScrollState = vScrollState,
            horizontalScrollState = hScrollState
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(vScrollState)
                    .intrinsicScrollModifier(hScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(10) { index ->
                    Text(
                        text = "Test $index",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color.Green).requiredSize(100.dp, 100.dp)
                    )
                }
            }
        }
    }
}

fun Modifier.forwardMinIntrinsicWidth() = this then object : LayoutModifier {
    var minIntrinsicWidth = 0
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val intrinsic = measurable.minIntrinsicWidth(constraints.maxHeight)
        minIntrinsicWidth = intrinsic
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = if (minIntrinsicWidth != 0) minIntrinsicWidth else measurable.minIntrinsicWidth(height)
}

fun Modifier.intrinsicScrollModifier(scrollState: ScrollState) = horizontalScroll(scrollState) then object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val intrinsic = measurable.minIntrinsicWidth(constraints.maxHeight)
        val maxViewport = max(constraints.minWidth, scrollState.viewportSize)
        val innerConstraints = constraints.copy(
            maxWidth = max(maxViewport, intrinsic)
        )
        val placeable = measurable.measure(innerConstraints)
        val width = max(placeable.width, intrinsic)
        return layout(width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
fun SmartScrollableFlowRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 4.dp,
    verticalPadding: Dp = 4.dp,
    scrollState: ScrollState,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier.horizontalScroll(scrollState),
        content = {
            content()
        },
    ) { measurables, constraints ->
        val paddingX = horizontalPadding.roundToPx()
        val paddingY = verticalPadding.roundToPx()
        var largestChildWidth = 0
        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(constraints)
            measurable.minIntrinsicWidth(constraints.maxHeight)
            largestChildWidth = max(largestChildWidth, placeable.width)
            placeable
        }
        val width = max(scrollState.viewportSize, largestChildWidth)
        var posX = 0
        var posY = 0
        var maxRowHeight = 0
        var maxRowWidth = 0
        var heightSum = 0
        val offset = placeables.map {
            if (posX > 0 && (posX + it.width) > width) {
                //Next row
                posX = 0
                posY += paddingY + maxRowHeight
                maxRowHeight = 0
            }
            val pos = IntOffset(posX, posY)
            maxRowHeight = max(maxRowHeight, it.height)
            maxRowWidth = max(maxRowWidth, posX + it.width)
            heightSum = posY + it.height
            posX += it.width + paddingX
            pos
        }

        layout(maxRowWidth, heightSum) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(offset[index])
            }
        }
    }
}


@Composable
fun ScrollableContainer(
    modifier: Modifier = Modifier,
    verticalScrollState: ScrollState = rememberScrollState(),
    horizontalScrollState: ScrollState = rememberScrollState(),
    verticalScrollbarModifier: Modifier = Modifier,
    horizontalScrollbarModifier: Modifier = Modifier,
    style: ScrollbarStyle = JewelTheme.scrollbarStyle,
    content: @Composable BoxScope.() -> Unit,
) {
    var keepVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScrollableContainerImpl(
        verticalScrollbar = {
            VerticalScrollbar(verticalScrollState, verticalScrollbarModifier, style = style, keepVisible = keepVisible)
        },
        horizontalScrollbar = {
            HorizontalScrollbar(
                horizontalScrollState,
                horizontalScrollbarModifier,
                style = style,
                keepVisible = keepVisible,
            )
        },
        modifier = modifier.withKeepVisible(style.scrollbarVisibility.lingerDuration, scope) { keepVisible = it },
        scrollbarStyle = style,
    ) {
        Box(Modifier.layoutId(ID_CONTENT).verticalScroll(verticalScrollState).horizontalScroll(horizontalScrollState)) {
            content()
        }
    }
}

@Composable
fun ScrollbarContainer(
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier,
    verticalScrollbarModifier: Modifier = Modifier,
    horizontalScrollbarModifier: Modifier = Modifier,
    style: ScrollbarStyle = JewelTheme.scrollbarStyle,
    content: @Composable () -> Unit,
) {
    var keepVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScrollableContainerImpl(
        verticalScrollbar = {
            VerticalScrollbar(verticalScrollState, verticalScrollbarModifier, style = style, keepVisible = keepVisible)
        },
        horizontalScrollbar = {
            HorizontalScrollbar(
                horizontalScrollState,
                horizontalScrollbarModifier,
                style = style,
                keepVisible = keepVisible,
            )
        },
        modifier = modifier.withKeepVisible(style.scrollbarVisibility.lingerDuration, scope) { keepVisible = it },
        scrollbarStyle = style,
    ) {
        Box(Modifier.layoutId(ID_CONTENT)) { content() }
    }
}

private const val ID_CONTENT = "VerticallyScrollableContainer_content"
private const val ID_VERTICAL_SCROLLBAR = "VerticallyScrollableContainer_verticalScrollbar"
private const val ID_HORIZONTAL_SCROLLBAR = "VerticallyScrollableContainer_horizontalScrollbar"

private fun Modifier.withKeepVisible(
    lingerDuration: Duration,
    scope: CoroutineScope,
    onKeepVisibleChange: (Boolean) -> Unit,
) =
    pointerInput(scope) {
        var delayJob: Job? = null
        awaitEachGesture {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Move) {
                delayJob?.cancel()
                onKeepVisibleChange(true)
                delayJob =
                    scope.launch {
                        delay(lingerDuration)
                        onKeepVisibleChange(false)
                    }
            }
        }
    }

@Composable
private fun ScrollableContainerImpl(
    verticalScrollbar: (@Composable () -> Unit)?,
    horizontalScrollbar: (@Composable () -> Unit)?,
    modifier: Modifier,
    scrollbarStyle: ScrollbarStyle,
    content: @Composable () -> Unit,
) {
    Layout(
        content = {
            content()

            if (verticalScrollbar != null) {
                Box(Modifier.layoutId(ID_VERTICAL_SCROLLBAR)) { verticalScrollbar() }
            }

            if (horizontalScrollbar != null) {
                Box(Modifier.layoutId(ID_HORIZONTAL_SCROLLBAR)) { horizontalScrollbar() }
            }
        },
        modifier,
    ) { measurables, incomingConstraints ->
        val verticalScrollbarMeasurable = measurables.find { it.layoutId == ID_VERTICAL_SCROLLBAR }
        val horizontalScrollbarMeasurable = measurables.find { it.layoutId == ID_HORIZONTAL_SCROLLBAR }

        // Leaving the bottom-end corner empty when both scrollbars visible at the same time
        val sizeOffsetWhenBothVisible =
            if (verticalScrollbarMeasurable != null && horizontalScrollbarMeasurable != null) {
                scrollbarStyle.scrollbarVisibility.trackThicknessExpanded.roundToPx()
            } else 0

        val verticalScrollbarPlaceable =
            if (verticalScrollbarMeasurable != null) {
                val verticalScrollbarConstraints =
                    Constraints.fixedHeight(incomingConstraints.maxHeight - sizeOffsetWhenBothVisible)
                verticalScrollbarMeasurable.measure(verticalScrollbarConstraints)
            } else null

        val horizontalScrollbarPlaceable =
            if (horizontalScrollbarMeasurable != null) {
                val horizontalScrollbarConstraints =
                    Constraints.fixedWidth(incomingConstraints.maxWidth - sizeOffsetWhenBothVisible)
                horizontalScrollbarMeasurable.measure(horizontalScrollbarConstraints)
            } else null

        val isMacOs = hostOs == OS.MacOS
        val contentMeasurable = measurables.find { it.layoutId == ID_CONTENT } ?: error("Content not provided")
        val contentConstraints =
            computeContentConstraints(
                scrollbarStyle,
                isMacOs,
                incomingConstraints,
                verticalScrollbarPlaceable,
                horizontalScrollbarPlaceable,
            )
        val contentPlaceable = contentMeasurable.measure(contentConstraints)

        val isAlwaysVisible = scrollbarStyle.scrollbarVisibility is AlwaysVisible
        val vScrollbarWidth =
            when {
                !isMacOs -> 0
                isAlwaysVisible -> verticalScrollbarPlaceable?.width ?: 0
                else -> 0
            }
        val width = contentPlaceable.width + vScrollbarWidth

        val hScrollbarHeight =
            when {
                !isMacOs -> 0
                isAlwaysVisible -> horizontalScrollbarPlaceable?.height ?: 0
                else -> 0
            }
        val height = contentPlaceable.height + hScrollbarHeight

        layout(width, height) {
            contentPlaceable.placeRelative(x = 0, y = 0, zIndex = 0f)
            verticalScrollbarPlaceable?.placeRelative(x = width - verticalScrollbarPlaceable.width, y = 0, zIndex = 1f)
            horizontalScrollbarPlaceable?.placeRelative(
                x = 0,
                y = height - horizontalScrollbarPlaceable.height,
                zIndex = 1f,
            )
        }
    }
}

private fun computeContentConstraints(
    scrollbarStyle: ScrollbarStyle,
    isMacOs: Boolean,
    incomingConstraints: Constraints,
    verticalScrollbarPlaceable: Placeable?,
    horizontalScrollbarPlaceable: Placeable?,
): Constraints {
    val visibility = scrollbarStyle.scrollbarVisibility

    fun width() =
        if (incomingConstraints.hasBoundedWidth) {
            val maxWidth = incomingConstraints.maxWidth
            when {
                !isMacOs -> maxWidth // Scrollbars on Win/Linux are always overlaid
                visibility is AlwaysVisible -> maxWidth - (verticalScrollbarPlaceable?.width ?: 0)
                visibility is WhenScrolling -> maxWidth
                else -> error("Unsupported visibility style: $visibility")
            }
        } else {
            error("Incoming constraints have infinite width, should not use fixed width")
        }

    fun height() =
        if (incomingConstraints.hasBoundedHeight) {
            val maxHeight = incomingConstraints.maxHeight
            when {
                !isMacOs -> maxHeight // Scrollbars on Win/Linux are always overlaid
                visibility is AlwaysVisible -> maxHeight - (horizontalScrollbarPlaceable?.height ?: 0)
                visibility is WhenScrolling -> maxHeight
                else -> error("Unsupported visibility style: $visibility")
            }
        } else {
            error("Incoming constraints have infinite height, should not use fixed height")
        }

    return when {
        incomingConstraints.hasBoundedWidth && incomingConstraints.hasBoundedHeight -> {
            Constraints.fixed(width(), height())
        }
        !incomingConstraints.hasBoundedWidth && incomingConstraints.hasBoundedHeight -> {
            Constraints.fixedHeight(height())
        }
        incomingConstraints.hasBoundedWidth && !incomingConstraints.hasBoundedHeight -> {
            Constraints.fixedWidth(width())
        }
        else -> Constraints()
    }
}
