package de.drick.compose.hotpreview.plugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

data class SettingsData(
    var gradleParametersEnabled: Boolean = false,
    var gradleParameters: String = "--build-cache --configuration-cache --configuration-cache-problems=warn",
    var recompileOnSave: Boolean = false,
    var recompileOnChange: Boolean = false, //Not supported yet
    var recompileOnChangeThresholdMilliseconds: Int = 2000 //Not supported yet
)

class HotPreviewSettingsConfigurable : Configurable {
    private val settings: SettingsData = HotPreviewSettings.getInstance().state
    private val mainPanel = panel {
        val recompileShortCut = KeymapManager.getInstance()
            .activeKeymap
            .getShortcuts("de.drick.compose.hotpreview.plugin.ReCompileShortcutAction")
            .joinToString(" | ") {
                KeymapUtil.getShortcutText(it)
            }
        row {
            checkBox("Recompile on save")
                .bindSelected(settings::recompileOnSave)
        }.rowComment(
            "When changes are saved to disk the task that compiles all Kotlin sources is executed for the module"
        )
        row("Recompile shortcut") {
            label(recompileShortCut)
            button("Change Shortcut") {
                ShowSettingsUtil.getInstance().showSettingsDialog(null, "Keymap")
                //TODO update shortcut text after change
            }
        }.rowComment(
            "You can change the shortcut in the settings menu. Search for HotPreview."
        )

        /*
        row {
            checkBox("Recompile on change")
                .bindSelected(settings::recompileOnChange)
        }
        row("Change threshold") {
            intTextField()
                .bindIntText(settings::recompileOnChangeThresholdMilliseconds)
            label("milliseconds")
        }*/

        group("Gradle Parameters") {
            lateinit var enabled: Cell<JBCheckBox>
            row {
                enabled = checkBox("Enabled")
                    .bindSelected(settings::gradleParametersEnabled)
            }
            row {
                textField()
                    .align(AlignX.FILL)
                    .resizableColumn()
                    .bindText(settings::gradleParameters)
                rowComment(
                    """
                When recompiling the Kotlin sources this additional parameters are added.<br/>
                Adding <b><code>--build-cache</code></b> will enable caching so only code will be recompiled that is changed.<br/>
                Adding <b><code>--configuration-cache</code></b> helps to reduce the time that gradle needs to configure the build.<br/>
                Adding <b><code>--configuration-cache-problems=warn</code></b> is needed because often configuration caching is not supported.
                But for our purpose it should be fine.
            """.trimIndent()
                )
            }.enabledIf(enabled.selected)
        }
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "HotPreview Settings"
    override fun createComponent(): JComponent = mainPanel
    override fun isModified() = mainPanel.isModified()
    override fun apply() {
        mainPanel.apply()
    }
    override fun reset() {
        mainPanel.reset()
    }
}

@State(
    name = "de.drick.compose.hotpreview.plugin.AppSettings",
    storages = [Storage("HotPreviewSettings.xml")]
)
class HotPreviewSettings: PersistentStateComponent<SettingsData> {
    companion object {
        fun getInstance(): HotPreviewSettings =
            ApplicationManager.getApplication().getService(HotPreviewSettings::class.java)
    }

    private var data = SettingsData()

    override fun getState(): SettingsData = data
    override fun loadState(loadedData: SettingsData) {
        data = loadedData
    }
}