package de.drick.compose.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeferredSuspend<T>(
    private val initBlock: suspend () -> T
) {
    private val lock = Mutex()
    @Volatile
    private var instance: T? = null
    suspend fun get(): T =
        instance ?: lock.withLock {
            initBlock().also {
                instance = it
            }
        }
    suspend fun reset() {
        lock.withLock {
            instance = null
        }
    }
}

fun <T> lazySuspend(block: suspend () -> T): Lazy<DeferredSuspend<T>> {
    return lazy {
        DeferredSuspend<T>(block)
    }
}
