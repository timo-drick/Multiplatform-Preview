package de.drick.compose.hotpreview

import kotlin.reflect.KClass


/**
 * Interface to be implemented by any provider of values that you want to be injected as @[HotPreview]
 * parameters. This allows providing sample information for previews.
 */
interface HotPreviewParameterProvider<T> {
    /**
     * [Sequence] of values of type [T] to be passed as @[HotPreview] parameter.
     */
    val values: Sequence<T>

    /**
     * Returns the number of elements in the [values] [Sequence].
     */
    val count get() = values.count()

    fun getParameters(limit: Int): List<T> = values.take(limit).toList()
}

/**
 * [HotPreviewParameter] can be applied to any parameter of a @[HotPreview].
 *
 * @param provider A [HotPreviewParameterProvider] class to use to inject values to the annotated
 * parameter.
 * @param limit Max number of values from [provider] to inject to this parameter.
 */
annotation class HotPreviewParameter(
    val provider: KClass<out HotPreviewParameterProvider<*>>,
    val limit: Int = Int.MAX_VALUE
)
