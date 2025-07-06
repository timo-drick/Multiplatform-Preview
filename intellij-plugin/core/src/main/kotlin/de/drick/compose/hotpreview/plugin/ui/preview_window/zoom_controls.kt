package de.drick.compose.hotpreview.plugin.ui.preview_window

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.HotPreviewLightDark
import de.drick.compose.hotpreview.plugin.tools.MockPersistentStore
import de.drick.compose.hotpreview.plugin.tools.PersistentStoreI
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.IconButtonState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.LocalIconButtonStyle
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.PainterHint
import org.jetbrains.jewel.ui.painter.hints.Stroke
import org.jetbrains.jewel.ui.theme.iconButtonStyle
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@HotPreview
@HotPreviewLightDark
@Composable
private fun ZoomControlsPreview() {
    val scaleState = remember {
        ScaleState(MockPersistentStore())
    }
    SelfPreviewTheme {
        ZoomControls(
            visible = true,
            scaleState = scaleState
        )
    }
}

@Composable
fun IconButtonState.hint() = if (isEnabled) {
    PainterHint.None
} else {
    Stroke(LocalIconButtonStyle.current.colors.foregroundSelectedActivated.copy(alpha = 0.3f))
}

@Composable
fun ZoomControls(
    visible: Boolean,
    scaleState: ScaleState,
    modifier: Modifier = Modifier,
) {
    var percentVisible by remember { mutableStateOf(true) }
    val scale = scaleState.scale
    val inactiveIconColor = JewelTheme.iconButtonStyle.colors.foregroundSelectedActivated
        .copy(alpha = 0.5f)

    LaunchedEffect(scale) {
        percentVisible = true
        delay(3.seconds)
        percentVisible = false
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = visible
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnimatedVisibility(visible = percentVisible) {
                Column(
                    modifier = Modifier
                        .background(JewelTheme.globalColors.panelBackground, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text("${(100f * scale).roundToInt()}%")
                }
            }
            Column(
                modifier = Modifier
                    .background(JewelTheme.globalColors.panelBackground, RoundedCornerShape(4.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = scaleState.maxReached().not(),
                    onClick = { scaleState.zoomIn() }
                ) { state ->
                    Icon(
                        key = AllIconsKeys.General.Add,
                        contentDescription = "Zoom in",
                        hint = state.hint()
                    )
                }
                IconButton(
                    enabled = scaleState.minReached().not(),
                    onClick = { scaleState.zoomOut() }
                ) { state ->
                    Icon(
                        key = AllIconsKeys.General.Remove,
                        contentDescription = "Zoom out",
                        hint = state.hint()
                    )
                }
                IconButton(
                    enabled = scaleState.fitToContent || scale != 1f,
                    onClick = { scaleState.setNeutral() }
                ) { state ->
                    Icon(
                        key = AllIconsKeys.General.ActualZoom,
                        contentDescription = "100%",
                        hint = state.hint()
                    )
                }
                IconButton(
                    enabled = scaleState.fitToContent.not(),
                    onClick = { scaleState.fitToContent() }
                ) { state ->
                    Icon(
                        key = AllIconsKeys.General.FitContent,
                        contentDescription = "Fit to content",
                        hint = state.hint()
                    )
                }
            }
        }
    }
}

class ScaleState(store: PersistentStoreI) {
    private val prefScaleStep = store.int("scaleStep", 0)
    private val prefFitToContent = store.boolean("scaleFitToContent", false)

    companion object {
        const val STEP = 1.15f
        const val MIN_STEP = -16
        const val MAX_STEP = 16
    }

    private var scaleStep = prefScaleStep.get()

    var scale by mutableStateOf(STEP.pow(scaleStep))
        private set
    var fitToContent by mutableStateOf(prefFitToContent.get())
        private set

    fun maxReached() = scaleStep >= MAX_STEP
    fun minReached() = scaleStep <= MIN_STEP

    fun zoomIn() {
        if (scaleStep < MAX_STEP) {
            fitToContent = false
            val newStep = scaleStep + 1
            setStep(newStep)
        }
    }
    fun zoomOut() {
        if (scaleStep > MIN_STEP) {
            fitToContent = false
            val newStep = scaleStep - 1
            setStep(newStep)
        }
    }

    fun setNeutral() {
        fitToContent = false
        setStep(0)
    }

    fun fitOut() {
        if (scaleStep > MIN_STEP) {
            fitToContent = true
            val newStep = scaleStep - 1
            setStep(newStep)
        }
    }
    fun fitIn() {
        if (scaleStep < MAX_STEP) {
            fitToContent = true
            val newStep = scaleStep + 1
            setStep(newStep)
        }
    }

    private fun setStep(step: Int) {
        scaleStep = step
        scale = STEP.pow(step)
        prefScaleStep.set(step)
        prefFitToContent.set(fitToContent)
    }

    fun fitToContent() {
        fitToContent = true
        prefFitToContent.set(true)
    }
}