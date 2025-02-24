package de.drick.compose.hotpreview.plugin

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

    fun float(key: String, defaultValue: Float): PersistentValue<Float> =
        PersistentFloat(properties, "de.drick.compose.hotpreview.plugin:${file.hashCode()}_$key", defaultValue)

    fun stringNA(key: String, defaultValue: String?): PersistentValue<String?> =
        PersistentStringNA(properties, "de.drick.compose.hotpreview.plugin:${file.hashCode()}_$key", defaultValue)
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