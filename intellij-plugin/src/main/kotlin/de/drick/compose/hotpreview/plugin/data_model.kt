package de.drick.compose.hotpreview.plugin

import de.drick.compose.hotpreview.HotPreview

data class HotPreviewFunction(
    val name: String,
    val annotation: List<HotPreviewAnnotation>,
    val lineRange: IntRange?
)

data class HotPreviewAnnotation(
    val lineRange: IntRange?,
    val annotation: HotPreview
)