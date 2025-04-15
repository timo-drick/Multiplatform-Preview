package androidx.compose.foundation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
class FixedIntInsets(
    private val leftVal: Int,
    private val topVal: Int,
    private val rightVal: Int,
    private val bottomVal: Int
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = leftVal
    override fun getTop(density: Density): Int = topVal
    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = rightVal
    override fun getBottom(density: Density): Int = bottomVal

    override fun toString(): String {
        return "WurstInsets(left=$leftVal, top=$topVal, right=$rightVal, bottom=$bottomVal)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FixedIntInsets) {
            return false
        }

        return leftVal == other.leftVal && topVal == other.topVal &&
                rightVal == other.rightVal && bottomVal == other.bottomVal
    }

    override fun hashCode(): Int {
        var result = leftVal
        result = 31 * result + topVal
        result = 31 * result + rightVal
        result = 31 * result + bottomVal
        return result
    }
}

/**
 * A representation of window insets that tracks access to enable recomposition,
 * relayout, and redrawing when values change. These values should not be read during composition
 * to avoid doing composition for every frame of an animation. Use methods like
 * [Modifier.windowInsetsPadding], [Modifier.systemBarsPadding], and
 * [Modifier.windowInsetsTopHeight] for Modifiers that will not cause recomposition when values
 * change.
 *
 * Use the [WindowInsets.Companion] extensions to retrieve [WindowInsets] for the current
 * window.
 */
/*@Stable
interface WindowInsets {
    /**
     * The space, in pixels, at the left of the window that the inset represents.
     */
    fun getLeft(density: Density, layoutDirection: LayoutDirection): Int

    /**
     * The space, in pixels, at the top of the window that the inset represents.
     */
    fun getTop(density: Density): Int

    /**
     * The space, in pixels, at the right of the window that the inset represents.
     */
    fun getRight(density: Density, layoutDirection: LayoutDirection): Int

    /**
     * The space, in pixels, at the bottom of the window that the inset represents.
     */
    fun getBottom(density: Density): Int

    companion object {
        val captionBar: WindowInsets get() = WindowInsets(10.dp, 20.dp, 30.dp, 40.dp)
    }
}*/

private val ZeroInsets = WindowInsets(10.dp, 20.dp, 30.dp, 40.dp)

/**
 * This [WindowInsets] represents the area with the display cutout (e.g. for camera).
 */
val WindowInsets.Companion.displayCutout: WindowInsets get() = ZeroInsets

/**
 * An insets type representing the window of the software keyboard.
 */
val WindowInsets.Companion.ime: WindowInsets get() = ZeroInsets

/**
 * These insets represent the space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets get() = ZeroInsets

/**
 * These insets represent where system UI places navigation bars.
 * Interactive UI should avoid the navigation bars area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets get() = ZeroInsets

/**
 * These insets represent status bar.
 */
val WindowInsets.Companion.statusBars: WindowInsets get() = ZeroInsets

/**
 * These insets represent all system bars.
 * Includes [statusBars], [captionBar] as well as [navigationBars], but not [ime].
 */
val WindowInsets.Companion.systemBars: WindowInsets get() = ZeroInsets

/**
 * The [systemGestures] insets represent the area of a window where system gestures have
 * priority and may consume some or all touch input, e.g. due to the system bar
 * occupying it, or it being reserved for touch-only gestures.
 */
val WindowInsets.Companion.systemGestures: WindowInsets get() = ZeroInsets

/**
 * Returns the tappable element insets.
 */
val WindowInsets.Companion.tappableElement: WindowInsets get() = ZeroInsets

/**
 * The insets for the curved areas in a waterfall display.
 */
val WindowInsets.Companion.waterfall: WindowInsets get() = ZeroInsets

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [systemBars], [displayCutout], and [ime].
 */
val WindowInsets.Companion.safeDrawing: WindowInsets get() = ZeroInsets

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [systemGestures], [mandatorySystemGestures], [waterfall], and [tappableElement].
 */
val WindowInsets.Companion.safeGestures: WindowInsets get() = ZeroInsets

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
val WindowInsets.Companion.safeContent: WindowInsets get() = ZeroInsets
