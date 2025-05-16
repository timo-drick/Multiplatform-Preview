package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.CameraPositionModel
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel
import de.drick.compose.hotpreview.plugin.ui.preview_window.SelfPreviewTheme
import de.drick.compose.hotpreview.plugin.ui.components.GenericComboBox
import de.drick.compose.hotpreview.plugin.ui.Typography
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.TriStateCheckbox
import org.jetbrains.jewel.ui.theme.groupHeaderStyle


data class ComboBoxEntry(
    val name: String,
    val value: String
)

val fontScaleTemplates = listOf(
    ComboBoxEntry("Default (100%)", "1f"),
    ComboBoxEntry("85%", "0.85f"),
    ComboBoxEntry("115%", "1.15f"),
    ComboBoxEntry("130%", "1.30f"),
    ComboBoxEntry("150%", "1.50f"),
    ComboBoxEntry("180%", "1.80f"),
    ComboBoxEntry("200%", "2.00f")
)

val densityTemplates = listOf(
    Pair("l", 120),
    Pair("m", 160),
    Pair("h", 240),
    Pair("xh", 320),
    Pair("xx", 420),
    Pair("xxh", 480)
).map { (name, dpi) ->
    ComboBoxEntry("${name}dpi ($dpi dpi)", "${dpi.toFloat() / 160f}f")
}

fun List<ComboBoxEntry>.findEntry(name: String) = find { it.name.toFloatOrNull() == name.toFloatOrNull() }

data class DeviceTemplate(
    val name: String,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float
)

private const val mdpi = 160f

val deviceTemplates = listOf(
    DeviceTemplate(name = "Phone", widthDp = 411, heightDp = 891, density = 420/mdpi),
    DeviceTemplate(name = "Phone Landscape", widthDp = 891, heightDp = 411, density = 420/mdpi),
    DeviceTemplate(name = "Foldable", widthDp = 673, heightDp = 841, density = 420/mdpi),
    DeviceTemplate(name = "Tablet", widthDp = 1280, heightDp = 800, density = 240/mdpi),
    DeviceTemplate(name = "Desktop", widthDp = 1920, heightDp = 1080, density = 160/mdpi),
)

fun findDeviceTemplate(widthDp: Int, heightDp: Int, density: Float) = deviceTemplates.find {
    density == it.density && widthDp == it.widthDp && heightDp == it.heightDp
}

private fun createMockArgumentField(value: String) = ArgumentField(
    name = "dummy",
    defaultValue = value,
    isString = false,
    useUpdateDsl = {}
)
private fun <T: Enum<T>>createMockArgumentEnum(value: T) = ArgumentFieldEnum(
    name = "dummy",
    defaultValue = value,
    fqName = "",
    useUpdateDsl = {}
)

private fun createMockArgumentFieldTrieState(value: Boolean?) = ArgumentTriStateBoolean(
    name = "dummy",
    defaultValue = value ?: false,
    useUpdateDsl = {}
)

val mockGutterIconViewModel = object: GutterIconViewModelI {
    override val baseModel = HotPreviewModel(
        name = "Test name"
    )
    override val name = createMockArgumentField(baseModel.name)
    override val group = createMockArgumentField(baseModel.group)
    override val widthDp = createMockArgumentField(baseModel.widthDp.toString())
    override val heightDp = createMockArgumentField(baseModel.heightDp.toString())
    override val density = createMockArgumentField("${baseModel.density}f")
    override val locale = createMockArgumentField("de")
    override val fontScale = createMockArgumentField("1f")
    override val darkMode = createMockArgumentFieldTrieState(null)
    override val statusBar = createMockArgumentFieldTrieState(null)
    override val captionBar = createMockArgumentFieldTrieState(null)
    override val navigationBar = createMockArgumentEnum(NavigationModeModel.Off)
    override val navigationBarContrastEnforced = createMockArgumentFieldTrieState(null)
    override val camera = createMockArgumentEnum(CameraPositionModel.Off)

    override fun update(dsl: UpdateAnnotationDsl.() -> Unit) {}
    override fun render() {}
    override suspend fun checkParameterExists(parameterName: String) = false
}

@HotPreview(density = 2f, name = "")
@HotPreview(density = 2f, darkMode = false)
@HotPreview(density = 2f, fontScale = 2.00f)
@Composable
fun GutterIconAnnotationSettingsPreview() {
    SelfPreviewTheme {
        GutterIconAnnotationSettings(mockGutterIconViewModel)
    }
}

@Composable
fun SettingsRow(
    description: String,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = description,
            style = Typography.labelTextStyle()
        )
        Spacer(Modifier.weight(1f))
        content()
    }
}

@Composable
fun GutterIconAnnotationSettings(
    vm: GutterIconViewModelI,
    modifier: Modifier = Modifier
) {
    fun Modifier.updateOnFocusLoss(
        field: ArgumentField? = null
    ) = this.onFocusChanged {
        if (it.hasFocus) return@onFocusChanged
        if (field != null) {
            field.update(field.value, true)
        } else {
            vm.render()
        }
    }

    Column(
        modifier.width(IntrinsicSize.Min)
    ) {
        Text(
            modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally),
            text = "HotPreview Configuration"
        )
        val groupHeaderStyle = JewelTheme.groupHeaderStyle
        Divider(
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = groupHeaderStyle.colors.divider,
            thickness = groupHeaderStyle.metrics.dividerThickness,
            startIndent = groupHeaderStyle.metrics.indent,
        )
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsRow("Name") {
                //var name by remember { mutableStateOf(base.name) }
                //val update = { string("name", name) }
                TextField(
                    modifier = Modifier.updateOnFocusLoss(vm.name),
                    value = vm.name.value,
                    onValueChange = vm.name::update
                )
            }
            SettingsRow("Group") {
                //var name by remember { mutableStateOf(base.name) }
                //val update = { string("name", name) }
                TextField(
                    modifier = Modifier.updateOnFocusLoss(vm.group),
                    value = vm.group.value,
                    onValueChange = vm.group::update
                )
            }
            GroupHeader("Hardware", Modifier.padding(vertical = 4.dp))
            SettingsRow("Device") {
                var selectedDevice: DeviceTemplate? = remember(
                    vm.widthDp.value,
                    vm.heightDp.value,
                    vm.density.value
                ) {
                    findDeviceTemplate(
                        widthDp = vm.widthDp.value.toIntOrNull() ?: -1,
                        heightDp = vm.heightDp.value.toIntOrNull() ?: -1,
                        density = vm.density.value.toFloatOrNull() ?: 1f
                    )
                }

                var deviceName = remember(selectedDevice) { selectedDevice?.name ?: "Custom" }
                GenericComboBox(
                    modifier = Modifier.width(180.dp),
                    labelText = deviceName,
                    items = deviceTemplates,
                    selectedItem = selectedDevice,
                    onSelectItem = {
                        vm.update {
                            vm.widthDp.update(this, it.widthDp.toString())
                            vm.heightDp.update(this, it.heightDp.toString())
                            vm.density.update(this, "${it.density}f")
                            render()
                        }
                        deviceName = it.name
                        selectedDevice = it
                    },
                    listItemContent = { item, isSelected, _, isItemHovered, isPreviewSelection ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered, isPreviewSelection)
                        )
                    }
                )
            }
            SettingsRow("Dimensions") {
                TextField(
                    modifier = Modifier.width(70.dp).updateOnFocusLoss(vm.widthDp),
                    value = vm.widthDp.value,
                    onValueChange = vm.widthDp::update,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    )
                )
                Text("x")
                TextField(
                    modifier = Modifier.width(70.dp).updateOnFocusLoss(vm.heightDp),
                    value = vm.heightDp.value,
                    onValueChange = vm.heightDp::update,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                Text("dp")
            }
            SettingsRow("Density") {
                val selectedDensity = remember(vm.density.value) {
                    densityTemplates.findEntry(vm.density.value)
                }
                GenericComboBox(
                    modifier = Modifier.width(180.dp),
                    labelText = selectedDensity?.name ?: vm.density.value,
                    selectedItem = selectedDensity,
                    onSelectItem = { item ->
                        vm.density.update(item.value, true)
                    },
                    items = densityTemplates,
                    listItemContent = { item, isSelected, _, isItemHovered, isPreviewSelection ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered, isPreviewSelection)
                        )
                    }
                )
            }
            SettingsRow("Statusbar") {
                TriStateCheckbox(
                    state = vm.statusBar.value,
                    onClick = {
                        vm.statusBar.toggle()
                    }
                )
            }
            SettingsRow("Captionbar") {
                TriStateCheckbox(
                    state = vm.captionBar.value,
                    onClick = {
                        vm.captionBar.toggle()
                    }
                )
            }
            SettingsRow("Navigationbar") {
                GenericComboBox<NavigationModeModel>(
                    modifier = Modifier.width(180.dp),
                    labelText = vm.navigationBar.value.name,
                    selectedItem = vm.navigationBar.value,
                    onSelectItem = { item ->
                        vm.navigationBar.update(item)
                    },
                    items = NavigationModeModel.entries,
                    listItemContent = { item, isSelected, _, isItemHovered, isPreviewSelection ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered, isPreviewSelection)
                        )
                    }
                )
            }
            SettingsRow("Navigationbar contrast enforced") {
                TriStateCheckbox(
                    state = vm.navigationBarContrastEnforced.value,
                    onClick = {
                        vm.navigationBarContrastEnforced.toggle()
                    }
                )
            }
            SettingsRow("Camera") {
                GenericComboBox<CameraPositionModel>(
                    modifier = Modifier.width(180.dp),
                    labelText = vm.camera.value.name,
                    selectedItem = vm.camera.value,
                    onSelectItem = { item ->
                        vm.camera.update(item)
                    },
                    items = CameraPositionModel.entries,
                    listItemContent = { item, isSelected, _, isItemHovered, isPreviewSelection ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered, isPreviewSelection)
                        )
                    }
                )
            }
            GroupHeader("Display", Modifier.padding(vertical = 4.dp))
            SettingsRow("locale") {
                TextField(
                    modifier = Modifier.width(70.dp).updateOnFocusLoss(),
                    value = vm.locale.value,
                    onValueChange = vm.locale::update,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
            SettingsRow("fontScale") {
                val selectedScale = remember(vm.fontScale.value) {
                    densityTemplates.findEntry(vm.fontScale.value)
                }
                GenericComboBox(
                    modifier = Modifier.width(180.dp),
                    labelText = vm.fontScale.value,
                    items = fontScaleTemplates,
                    selectedItem = selectedScale,
                    onSelectItem = { item ->
                        vm.fontScale.update(item.value, true)
                    },
                    listItemContent = { item, isSelected, _, isItemHovered, isPreviewSelection ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered, isPreviewSelection)
                        )
                    }
                )
            }
            SettingsRow("Dark mode") {
                TriStateCheckbox(
                    state = vm.darkMode.value,
                    onClick = {
                        vm.darkMode.toggle()
                    }
                )
            }
        }
    }
}
