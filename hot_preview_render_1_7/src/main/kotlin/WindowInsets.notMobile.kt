package androidx.compose.foundation.layout

import de.drick.compose.hotpreview.WindowInsetsProvider

val WindowInsets.Companion.captionBar: WindowInsets
    get() = WindowInsetsProvider.insets.captionBarInsets

val WindowInsets.Companion.displayCutout: WindowInsets
    get() = WindowInsetsProvider.insets.displayCutoutInsets

val WindowInsets.Companion.ime: WindowInsets
    get() = WindowInsetsProvider.insets.imeInsets

val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    get() = WindowInsetsProvider.insets.mandatorySystemGesturesInsets

val WindowInsets.Companion.navigationBars: WindowInsets
    get() = WindowInsetsProvider.insets.navigationBarsInsets

val WindowInsets.Companion.statusBars: WindowInsets
    get() = WindowInsetsProvider.insets.statusBarsInsets

val WindowInsets.Companion.systemBars: WindowInsets
    get() = statusBars.union(navigationBars)

val WindowInsets.Companion.systemGestures: WindowInsets
    get() = WindowInsetsProvider.insets.systemGesturesInsets

val WindowInsets.Companion.tappableElement: WindowInsets
    get() = WindowInsetsProvider.insets.tappableElementInsets

val WindowInsets.Companion.waterfall: WindowInsets
    get() = WindowInsetsProvider.insets.waterfallInsets

val WindowInsets.Companion.safeDrawing: WindowInsets
    get() = systemBars.union(displayCutout)

val WindowInsets.Companion.safeGestures: WindowInsets
    get() = systemGestures.union(mandatorySystemGestures)

val WindowInsets.Companion.safeContent: WindowInsets
    get() = safeDrawing.union(safeGestures)