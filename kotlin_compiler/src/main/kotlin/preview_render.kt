package de.drick.compose.hotpreview


class HotPreviewCompiler {
    fun compile(
        classPaths: List<String>
    ) {
        println("Class path:")
        classPaths.forEach {
            println(it)
        }
    }
}