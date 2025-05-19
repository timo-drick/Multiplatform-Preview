package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import de.drick.compose.hotpreview.WindowInsetsProvider


val WindowInsets.Companion.captionBar: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.captionBarInsets

val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.displayCutoutInsets

val WindowInsets.Companion.ime: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.imeInsets

val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.mandatorySystemGesturesInsets

val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.navigationBarsInsets

val WindowInsets.Companion.statusBars: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.statusBarsInsets

val WindowInsets.Companion.systemBars: WindowInsets
    @Composable get() = statusBars.union(navigationBars)

val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.systemGesturesInsets

val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.tappableElementInsets

val WindowInsets.Companion.waterfall: WindowInsets
    @Composable get() = WindowInsetsProvider.insets.waterfallInsets

val WindowInsets.Companion.safeDrawing: WindowInsets
    @Composable get() = systemBars.union(displayCutout)

val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable get() = systemGestures.union(mandatorySystemGestures)

val WindowInsets.Companion.safeContent: WindowInsets
    @Composable get() = safeDrawing.union(safeGestures)