package androidx.compose.foundation.layout

private val NoneZeroInsets = WindowInsets(100, 200, 300, 400)

val WindowInsets.Companion.captionBar: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.displayCutout: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.ime: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.navigationBars: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.statusBars: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.systemBars: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.systemGestures: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.tappableElement: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.waterfall: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.safeDrawing: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.safeGestures: WindowInsets
    get() = NoneZeroInsets

val WindowInsets.Companion.safeContent: WindowInsets
    get() = NoneZeroInsets
