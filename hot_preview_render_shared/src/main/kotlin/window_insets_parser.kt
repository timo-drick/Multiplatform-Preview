package de.drick.compose.hotpreview

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

// Sample string: "captionBarInsets(0,100,0,0)|statusBarInsets(0,0,0,0)|navigationBarInsets(0,0,0,0)|systemGestureInsets(0,0,0,0)|imeInsets(0,0,0,0)"

data class WindowInsetsModel(
    var captionBarInsets: WindowInsets,
    var displayCutoutInsets: WindowInsets,
    var imeInsets: WindowInsets,
    var mandatorySystemGesturesInsets: WindowInsets,
    var navigationBarsInsets: WindowInsets,
    var statusBarsInsets: WindowInsets,
    var systemGesturesInsets: WindowInsets,
    var tappableElementInsets: WindowInsets,
    var waterfallInsets: WindowInsets
)

fun newModel() = WindowInsetsModel(
    captionBarInsets = WindowInsets(0, 0, 0, 0),
    displayCutoutInsets = WindowInsets(0, 0, 0, 0),
    imeInsets = WindowInsets(0, 0, 0, 0),
    mandatorySystemGesturesInsets = WindowInsets(0, 0, 0, 0),
    navigationBarsInsets = WindowInsets(0, 0, 0, 0),
    statusBarsInsets = WindowInsets(0, 0, 0, 0),
    systemGesturesInsets = WindowInsets(0, 0, 0, 0),
    tappableElementInsets = WindowInsets(0, 0, 0, 0),
    waterfallInsets = WindowInsets(0, 0, 0, 0)
)

object WindowInsetsProvider {
    var insets: WindowInsetsModel = newModel()
        private set

    fun setInsetsFromString(insetsString: String, density: Float) {
        val insetMap = parseWindowInsets(insetsString, density)
        insets.captionBarInsets = insetMap["captionBarInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.displayCutoutInsets = insetMap["displayCutoutInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.imeInsets = insetMap["imeInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.mandatorySystemGesturesInsets = insetMap["mandatorySystemGesturesInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.navigationBarsInsets = insetMap["navigationBarsInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.statusBarsInsets = insetMap["statusBarsInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.systemGesturesInsets = insetMap["systemGesturesInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.tappableElementInsets = insetMap["tappableElementInsets"] ?: WindowInsets(0, 0, 0, 0)
        insets.waterfallInsets = insetMap["waterfallInsets"] ?: WindowInsets(0, 0, 0, 0)
    }
}

private fun parseWindowInsets(insets: String, density: Float) = insets.split("|").associate { token ->
    val name = token.substringBefore("(")
    val sides = token.substringAfter("(")
        .substringBefore(")")
        .split(",")
        .map { (it.toInt() * density).roundToInt() }
    Pair<String, WindowInsets>(name, PreviewIntInsets(name, sides[0], sides[1], sides[2], sides[3]))
}


@Immutable
private class PreviewIntInsets(
    private val name: String = "Insets",
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
        return "$name(left=$leftVal, top=$topVal, right=$rightVal, bottom=$bottomVal)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PreviewIntInsets) {
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