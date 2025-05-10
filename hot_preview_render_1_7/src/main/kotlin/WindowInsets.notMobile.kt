package androidx.compose.foundation.layout

var captionBarInsets = WindowInsets(0, 0, 0, 0)
var displayCutoutInsets = WindowInsets(0, 0, 0, 0)
var imeInsets = WindowInsets(0, 0, 0, 0)
var mandatorySystemGesturesInsets = WindowInsets(0, 0, 0, 0)
var navigationBarsInsets = WindowInsets(0, 0, 0, 0)
var statusBarsInsets = WindowInsets(0, 0, 0, 0)
var systemGesturesInsets = WindowInsets(0, 0, 0, 0)
var tappableElementInsets = WindowInsets(0, 0, 0, 0)
var waterfallInsets = WindowInsets(0, 0, 0, 0)

fun resetWindowInsets() {
    captionBarInsets = WindowInsets(0, 0, 0, 0)
    displayCutoutInsets = WindowInsets(0, 0, 0, 0)
    imeInsets = WindowInsets(0, 0, 0, 0)
    mandatorySystemGesturesInsets = WindowInsets(0, 0, 0, 0)
    navigationBarsInsets = WindowInsets(0, 0, 0, 0)
    statusBarsInsets = WindowInsets(0, 0, 0, 0)
    systemGesturesInsets = WindowInsets(0, 0, 0, 0)
    tappableElementInsets = WindowInsets(0, 0, 0, 0)
    waterfallInsets = WindowInsets(0, 0, 0, 0)
}

val WindowInsets.Companion.captionBar: WindowInsets
    get() = captionBarInsets

val WindowInsets.Companion.displayCutout: WindowInsets
    get() = displayCutoutInsets

val WindowInsets.Companion.ime: WindowInsets
    get() = imeInsets

val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    get() = mandatorySystemGesturesInsets

val WindowInsets.Companion.navigationBars: WindowInsets
    get() = navigationBarsInsets

val WindowInsets.Companion.statusBars: WindowInsets
    get() = statusBarsInsets

val WindowInsets.Companion.systemBars: WindowInsets
    get() = statusBars.union(navigationBars)

val WindowInsets.Companion.systemGestures: WindowInsets
    get() = systemGesturesInsets

val WindowInsets.Companion.tappableElement: WindowInsets
    get() = tappableElementInsets

val WindowInsets.Companion.waterfall: WindowInsets
    get() = waterfallInsets

val WindowInsets.Companion.safeDrawing: WindowInsets
    get() = systemBars.union(displayCutout)

val WindowInsets.Companion.safeGestures: WindowInsets
    get() = systemGesturesInsets.union(mandatorySystemGesturesInsets)

val WindowInsets.Companion.safeContent: WindowInsets
    get() = safeDrawing.union(safeGestures)