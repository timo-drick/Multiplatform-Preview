package de.drick.compose.hotpreview.plugin.tools

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface PersistentValue<T> {
    fun get(): T
    fun set(value: T)
}

class PluginPersistentStore(
    project: Project,
    private val file: VirtualFile
) {
    private val properties = PropertiesComponent.getInstance(project)

    private fun String.toFileUniqueKey() = "de.drick.compose.hotpreview.plugin:${file.hashCode()}_$this"

    fun boolean(key: String, defaultValue: Boolean = false): PersistentValue<Boolean> =
        PersistentBoolean(properties, key.toFileUniqueKey(), defaultValue)

    fun float(key: String, defaultValue: Float): PersistentValue<Float> =
        PersistentFloat(properties, key.toFileUniqueKey(), defaultValue)

    fun stringNA(key: String, defaultValue: String?): PersistentValue<String?> =
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