package de.drick.compose.hotpreview.plugin.tools

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.getOrCreate

interface PersistentValue<T> {
    fun get(): T
    fun set(value: T)
}

interface PersistentStoreI {
    fun boolean(key: String, defaultValue: Boolean = false): PersistentValue<Boolean>
    fun float(key: String, defaultValue: Float): PersistentValue<Float>
    fun int(key: String, defaultValue: Int): PersistentValue<Int>
    fun stringNA(key: String, defaultValue: String?): PersistentValue<String?>
}

/**
 * Persistently stores values for specific opened files.
 * The values will be persisted in the properties with a key that is related to the file.
 */
class PluginPersistentStore(
    project: Project,
    private val file: VirtualFile
): PersistentStoreI {
    private val properties = PropertiesComponent.getInstance(project)

    private fun String.toFileUniqueKey() = "de.drick.compose.hotpreview.plugin:${file.hashCode()}_$this"

    override fun boolean(key: String, defaultValue: Boolean): PersistentValue<Boolean> =
        PersistentBoolean(properties, key.toFileUniqueKey(), defaultValue)

    override fun float(key: String, defaultValue: Float): PersistentValue<Float> =
        PersistentFloat(properties, key.toFileUniqueKey(), defaultValue)

    override fun int(key: String, defaultValue: Int): PersistentValue<Int> =
        PersistentInt(properties, key.toFileUniqueKey(), defaultValue)

    override fun stringNA(key: String, defaultValue: String?): PersistentValue<String?> =
        PersistentStringNA(properties, key.toFileUniqueKey(), defaultValue)
}

private class PersistentBoolean(
    private val p: PropertiesComponent,
    private val key: String,
    private val defaultValue: Boolean
): PersistentValue<Boolean> {
    override fun get(): Boolean = p.getBoolean(key, defaultValue)
    override fun set(value: Boolean) {
        p.setValue(key, value, defaultValue)
    }
}
private class PersistentFloat(
    private val p: PropertiesComponent,
    private val key: String,
    private val defaultValue: Float
): PersistentValue<Float> {
    override fun get(): Float = p.getFloat(key, defaultValue)
    override fun set(value: Float) {
        p.setValue(key, value, defaultValue)
    }
}
private class PersistentInt(
    private val p: PropertiesComponent,
    private val key: String,
    private val defaultValue: Int
): PersistentValue<Int> {
    override fun get(): Int = p.getInt(key, defaultValue)
    override fun set(value: Int) {
        p.setValue(key, value, defaultValue)
    }
}
private class PersistentStringNA(
    private val p: PropertiesComponent,
    private val key: String,
    private val defaultValue: String?
): PersistentValue<String?> {
    override fun get(): String? = p.getValue(key)
    override fun set(value: String?) {
        p.setValue(key, value, defaultValue)
    }
}


class MockPersistentValue<T>(default: T) : PersistentValue<T> {
    private var value: T = default
    override fun get() = value
    override fun set(newValue: T) {
        value = newValue
    }
}

class MockPersistentStore : PersistentStoreI {
    val booleanMap = mutableMapOf<String, PersistentValue<Boolean>>()
    override fun boolean(key: String, defaultValue: Boolean) = booleanMap.getOrCreate(key) {
        MockPersistentValue(defaultValue)
    }
    val floatMap = mutableMapOf<String, PersistentValue<Float>>()
    override fun float(
        key: String,
        defaultValue: Float
    ): PersistentValue<Float> = floatMap.getOrCreate(key) {
        MockPersistentValue(defaultValue)
    }

    val intMap = mutableMapOf<String, PersistentValue<Int>>()
    override fun int(
        key: String,
        defaultValue: Int
    ): PersistentValue<Int> = intMap.getOrCreate(key) {
        MockPersistentValue(defaultValue)
    }

    val stringNAMap = mutableMapOf<String, PersistentValue<String?>>()
    override fun stringNA(
        key: String,
        defaultValue: String?
    ): PersistentValue<String?> = stringNAMap.getOrCreate(key) {
        MockPersistentValue(defaultValue)
    }
}