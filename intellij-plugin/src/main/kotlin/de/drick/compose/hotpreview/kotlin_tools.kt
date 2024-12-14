package de.drick.compose.hotpreview

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope


inline fun <R> CoroutineScope.runCatchingCancellationAware(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}