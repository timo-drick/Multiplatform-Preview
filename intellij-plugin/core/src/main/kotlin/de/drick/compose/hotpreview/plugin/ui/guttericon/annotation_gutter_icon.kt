package de.drick.compose.hotpreview.plugin.ui.guttericon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import de.drick.compose.hotpreview.plugin.DisplayCutoutModeModel
import de.drick.compose.hotpreview.plugin.HOT_PREVIEW_ANNOTATION_VERSION
import de.drick.compose.hotpreview.plugin.HotPreviewModel
import de.drick.compose.hotpreview.plugin.NavigationModeModel
import de.drick.compose.hotpreview.plugin.ui.preview_window.SelfPreviewTheme
import de.drick.compose.hotpreview.plugin.ui.components.GenericComboBox
import de.drick.compose.hotpreview.plugin.ui.Typography
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListItemState
import org.jetbrains.jewel.ui.component.SimpleListItem
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.TriStateCheckbox
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.groupHeaderStyle
import kotlin.math.roundToInt


data class ComboBoxEntry(
    val name: String,
    val value: String?
)

val fontScaleTemplates = listOf(
    ComboBoxEntry("Default (100%)", null),
    ComboBoxEntry("85%", "0.85f"),
    ComboBoxEntry("100%", "1.00f"),
    ComboBoxEntry("115%", "1.15f"),
    ComboBoxEntry("130%", "1.30f"),
    ComboBoxEntry("150%", "1.50f"),
    ComboBoxEntry("180%", "1.80f"),
    ComboBoxEntry("200%", "2.00f")
)

val densityTemplates = listOf(ComboBoxEntry("Default (mdpi) 160dpi", null)) + listOf(
    Pair("l", 120f / 160f),
    Pair("m", 160f / 160f),
    Pair("h", 240f / 160f),
    Pair("xh", 320f / 160f),
    Pair("xx", 420f / 160f),
    Pair("xxh", 480f / 160f)
).map { (name, density) ->
    ComboBoxEntry("${name}dpi (${(density * 160f).roundToInt()} dpi)", "${density}f")
}

fun List<ComboBoxEntry>.findEntry(value: String) = find { it.value?.toFloatOrNull() == value.toFloatOrNull() }

private fun createMockArgumentField(value: String) = ArgumentField(
    name = "dummy",
    defaultValue = value,
    isString = false,
    useUpdateDsl = {}
)
private fun <T: Enum<T>>createMockArgumentEnum(value: T) = ArgumentFieldEnum(
    name = "dummy",
    nullValue = value,
    defaultValue = value,
    fqName = "",
    useUpdateDsl = {}
)

private fun createMockArgumentFieldTrieState(value: Boolean?) = ArgumentTriStateBoolean(
    name = "dummy",
    defaultValue = value ?: false,
    useUpdateDsl = {}
)

@OptIn(ExperimentalStdlibApi::class)
val mockGutterIconViewModel = object: GutterIconViewModelI {
    override val annotationVersion = 2

    override val deviceTemplates = deviceTemplatesV2
    override val baseModel = HotPreviewModel(
        name = "Test name"
    )
    override val name = createMockArgumentField(baseModel.name)
    override val group = createMockArgumentField(baseModel.group)
    override val widthDp = createMockArgumentField(baseModel.widthDp.toString())
    override val heightDp = createMockArgumentField(baseModel.heightDp.toString())
    override val density = createMockArgumentField("${baseModel.density}f")
    override val locale = createMockArgumentField("de")
    override val layoutDirectionRTL = createMockArgumentFieldTrieState(null)
    override val fontScale = createMockArgumentField("1.50f")
    override val darkMode = createMockArgumentFieldTrieState(null)
    override val backgroundColor = createMockArgumentField(baseModel.backgroundColor.toHexString())
    override val statusBar = createMockArgumentFieldTrieState(null)
    override val captionBar = createMockArgumentFieldTrieState(null)
    override val navigationBar = createMockArgumentEnum(NavigationModeModel.ThreeButtonBottom)
    override val navigationBarContrastEnforced = createMockArgumentFieldTrieState(null)
    override val displayCutout = createMockArgumentEnum(DisplayCutoutModeModel.Off)

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

private val outdatedWarningMessage = """
    Some settings are not supported by the old @HotPreview annotation dependency.
    Maybe update your dependency.
""".trimIndent()

@OptIn(ExperimentalJewelApi::class)
@Composable
fun HPTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)
    var lastTextValue by remember(value) { mutableStateOf(value) }

    TextField(
        modifier = modifier,
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) {
                onValueChange(newTextFieldValueState.text)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
        )
    )
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
        if (vm.annotationVersion < HOT_PREVIEW_ANNOTATION_VERSION && vm.annotationVersion > 0) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(key = AllIconsKeys.General.Warning, contentDescription = "Warning")
                Text(text = outdatedWarningMessage, style = Typography.labelTextStyle())
            }
        }
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
                HPTextField(
                    modifier = Modifier.updateOnFocusLoss(vm.name),
                    value = vm.name.value,
                    onValueChange = vm.name::update
                )
            }
            SettingsRow("Group") {
                HPTextField(
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
                    vm.density.value,
                    vm.statusBar.value,
                    vm.captionBar.value,
                    vm.navigationBar.value,
                    vm.displayCutout.value
                ) {
                    vm.findDeviceTemplate()
                }

                var deviceName = remember(selectedDevice) { selectedDevice?.name ?: "Custom" }
                GenericComboBox(
                    modifier = Modifier.width(180.dp),
                    labelText = deviceName,
                    items = vm.deviceTemplates,
                    selectedItem = selectedDevice,
                    onSelectItem = {
                        vm.update {
                            vm.widthDp.update(this, it.widthDp.toString())
                            vm.heightDp.update(this, it.heightDp.toString())
                            vm.density.update(this, "${it.density}f")
                            vm.statusBar.set(this, it.statusBar)
                            vm.captionBar.set(this, it.captionBar)
                            vm.navigationBar.update(this, it.navigationBar)
                            vm.displayCutout.update(this, it.displayCutout)
                            render()
                        }
                        deviceName = it.name
                        selectedDevice = it
                    },
                    listItemContent = { item, isSelected, _, isItemHovered, _ ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered)
                        )
                    }
                )
            }
            SettingsRow("Dimensions") {
                HPTextField(
                    modifier = Modifier.width(70.dp).updateOnFocusLoss(vm.widthDp),
                    value = vm.widthDp.value,
                    onValueChange = vm.widthDp::update,
                    keyboardType = KeyboardType.Number
                )
                Text("x")
                HPTextField(
                    modifier = Modifier.width(70.dp).updateOnFocusLoss(vm.heightDp),
                    value = vm.heightDp.value,
                    onValueChange = vm.heightDp::update,
                    keyboardType = KeyboardType.Number
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
                    listItemContent = { item, isSelected, _, isItemHovered, _ ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered)
                        )
                    }
                )
            }
            SettingsRow("Statusbar", isNewerVersion = vm.annotationVersion < 2) {
                TriStateCheckbox(
                    state = vm.statusBar.value,
                    onClick = {
                        vm.update {
                            vm.statusBar.toggle(this)
                            if (vm.statusBar.value == ToggleableState.On) {
                                vm.captionBar.set(this, ToggleableState.Indeterminate)
                            }
                            render()
                        }
                    }
                )
            }
            SettingsRow("Captionbar", isNewerVersion = vm.annotationVersion < 2) {
                TriStateCheckbox(
                    state = vm.captionBar.value,
                    onClick = {
                        vm.update {
                            vm.captionBar.toggle(this)
                            if (vm.captionBar.value == ToggleableState.On) {
                                vm.statusBar.set(this, ToggleableState.Indeterminate)
                            }
                            render()
                        }
                    }
                )
            }
            SettingsRow("Navigationbar", isNewerVersion = vm.annotationVersion < 2) {
                GenericComboBox<NavigationModeModel>(
                    modifier = Modifier.width(180.dp),
                    labelText = vm.navigationBar.value.name,
                    selectedItem = vm.navigationBar.value,
                    onSelectItem = { item ->
                        vm.update {
                            vm.navigationBar.update(this, item)
                            val cutOut = vm.displayCutout.value
                            val nav = vm.navigationBar.value
                            when { // Update displayCutout if needed
                                cutOut == DisplayCutoutModeModel.CameraBottom &&
                                        nav == NavigationModeModel.ThreeButtonBottom -> {
                                    vm.displayCutout.update(this, DisplayCutoutModeModel.CameraTop)
                                }
                                cutOut == DisplayCutoutModeModel.CameraLeft &&
                                        nav == NavigationModeModel.ThreeButtonLeft -> {
                                    vm.displayCutout.update(this, DisplayCutoutModeModel.CameraRight)
                                }
                                cutOut == DisplayCutoutModeModel.CameraRight &&
                                        nav == NavigationModeModel.ThreeButtonRight -> {
                                    vm.displayCutout.update(this, DisplayCutoutModeModel.CameraLeft)
                                }
                            }
                            render()
                        }
                    },
                    items = NavigationModeModel.entries,
                    listItemContent = { item, isSelected, _, isItemHovered, _ ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered)
                        )
                    }
                )
            }
            SettingsRow("Navigationbar contrast enforced", isNewerVersion = vm.annotationVersion < 2) {
                TriStateCheckbox(
                    state = vm.navigationBarContrastEnforced.value,
                    onClick = {
                        vm.navigationBarContrastEnforced.toggle()
                    }
                )
            }
            SettingsRow("Display cutout", isNewerVersion = vm.annotationVersion < 2) {
                GenericComboBox<DisplayCutoutModeModel>(
                    modifier = Modifier.width(180.dp),
                    labelText = vm.displayCutout.value.name,
                    selectedItem = vm.displayCutout.value,
                    onSelectItem = { item ->
                        vm.update {
                            vm.displayCutout.update(this, item)
                            val cutOut = vm.displayCutout.value
                            val nav = vm.navigationBar.value
                            when { // Update navigation bar if needed
                                cutOut == DisplayCutoutModeModel.CameraBottom &&
                                        nav == NavigationModeModel.ThreeButtonBottom -> {
                                    vm.navigationBar.update(this, NavigationModeModel.ThreeButtonBottom)
                                }
                                cutOut == DisplayCutoutModeModel.CameraLeft &&
                                        nav == NavigationModeModel.ThreeButtonLeft -> {
                                    vm.navigationBar.update(this, NavigationModeModel.ThreeButtonRight)
                                }
                                cutOut == DisplayCutoutModeModel.CameraRight &&
                                        nav == NavigationModeModel.ThreeButtonRight -> {
                                    vm.navigationBar.update(this, NavigationModeModel.ThreeButtonLeft)
                                }
                            }
                            render()
                        }
                    },
                    items = DisplayCutoutModeModel.entries,
                    listItemContent = { item, isSelected, _, isItemHovered, _ ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered)
                        )
                    }
                )
            }
            GroupHeader("Display", Modifier.padding(vertical = 4.dp))
            SettingsRow("locale") {
                HPTextField(
                    modifier = Modifier.width(70.dp).updateOnFocusLoss(),
                    value = vm.locale.value,
                    onValueChange = vm.locale::update,
                    keyboardType = KeyboardType.Text
                )
            }
            SettingsRow("layout direction RTL", isNewerVersion = vm.annotationVersion < 2) {
                TriStateCheckbox(
                    state = vm.layoutDirectionRTL.value,
                    onClick = {
                        vm.layoutDirectionRTL.toggle()
                    }
                )
            }
            SettingsRow("fontScale") {
                val selectedScale = remember(vm.fontScale.value) {
                    fontScaleTemplates.findEntry(vm.fontScale.value)
                }
                GenericComboBox(
                    modifier = Modifier.width(80.dp),
                    labelText = selectedScale?.name ?: vm.fontScale.value,
                    items = fontScaleTemplates,
                    selectedItem = selectedScale,
                    onSelectItem = { item ->
                        vm.fontScale.update(item.value, true)
                    },
                    listItemContent = { item, isSelected, _, isItemHovered, _ ->
                        SimpleListItem(
                            text = item.name,
                            state = ListItemState(isSelected, isItemHovered)
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
            /*SettingsRow("Background color") {
                ActionButton(
                    onClick = {
                        /*ColorPicker.showDialog(null, "Select Background Color", vm.backgroundColor.value) { color ->
                            vm.backgroundColor.update(color.toHexString(), true)
                        }*/
                    }
                ) {
                    Icon(AllIconsKeys.Ide.Pipette, contentDescription = "Color picker")
                }
                TextField(
                    modifier = Modifier.width(120.dp).updateOnFocusLoss(),
                    value = vm.backgroundColor.value,
                    onValueChange = vm.backgroundColor::update,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )
            }*/
        }
    }
}

@Composable
fun SettingsRow(
    description: String,
    isNewerVersion: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box {
        Row(
            modifier = if (isNewerVersion) Modifier.alpha(0.5f) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = description,
                style = Typography.labelTextStyle()
            )
            if (isNewerVersion) {
                Icon(key = AllIconsKeys.General.Warning, contentDescription = "Warning")
            }
            Spacer(Modifier.weight(1f))
            content()
        }
        if (isNewerVersion) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { /* Consume all input */
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
            )
        }
    }
}