package de.drick.compose.utils


fun <K, V> lruCacheOf(capacity: Int): LRUCache<K, V> = LRUCache(capacity = capacity)

class LRUCache<K, V>(private val capacity: Int) {
    private val cache = LinkedHashMap<K, V>(capacity, 0.75f)

    operator fun get(key: K): V? = cache[key]?.let {
        remove(key)
        cache[key] = it
        it
    }

    fun clear() = cache.clear()

    fun remove(key: K) = cache.remove(key)

    operator fun set(key: K, value: V) {
        if (cache.containsKey(key)) {
            cache.remove(key)
        } else if (cache.size == capacity) {
            cache.remove(cache.keys.first())
        }
        cache[key] = value
    }

    val size: Int
        get() = cache.size

    fun isEmpty() = cache.isEmpty()

    fun containsKey(key: K) = cache.containsKey(key)

    fun containsValue(value: V) = cache.containsValue(value)

    fun putAll(from: Map<out K, V>) = cache.putAll(from)

    val keys: MutableSet<K>
        get() = cache.keys

    val values: MutableCollection<V>
        get() = cache.values

    val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = cache.entries
}