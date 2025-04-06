package de.drick.compose.hotpreview.plugin

import de.drick.compose.hotpreview.plugin.service.RenderClassLoaderInstance
import de.drick.compose.hotpreview.plugin.ui.preview_window.UIRenderState
import de.drick.compose.utils.LRUCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RenderCacheKey(
    val name: String,
    val parameter: Any?,
    val annotation: HotPreviewModel
)

class RenderService {

    private val renderCache = LRUCache<RenderCacheKey, RenderedImage>(20)
    private val renderLock = Mutex()
    private var renderStateMap = mapOf<RenderCacheKey, UIRenderState>()

    fun requestPreviews(keys: Set<RenderCacheKey>): Map<RenderCacheKey, UIRenderState> {
        val newMap = keys.associate { key ->
            val value = renderStateMap[key] ?: UIRenderState(
                widthDp = key.annotation.widthDp,
                heightDp = key.annotation.heightDp
            ).also { state ->
                renderCache[key]?.let { state.state = it }
            }
            Pair(key, value)
        }
        renderStateMap = newMap
        return newMap
    }

    suspend fun render(renderClassLoader: RenderClassLoaderInstance) {
        if (renderStateMap.isEmpty()) return
        withContext(Dispatchers.Default) {
            renderLock.withLock {
                renderStateMap.forEach { (key, renderState) ->
                    // Workaround for legacy resource loading in old compose code
                    // See androidx.compose.ui.res.ClassLoaderResourceLoader
                    // It uses the contextClassLoader to load the resources.
                    val previousContextClassLoader = Thread.currentThread().contextClassLoader
                    // For new compose.components resource system a LocalCompositionProvider is used.
                    Thread.currentThread().contextClassLoader = renderClassLoader.classLoader
                    val state = try {
                        renderPreview(renderClassLoader, key)
                    } finally {
                        Thread.currentThread().contextClassLoader = previousContextClassLoader
                    }
                    renderState.state = state
                    // Update render cache
                    if (state is RenderedImage) renderCache[key] = state
                }
            }
        }
    }
}