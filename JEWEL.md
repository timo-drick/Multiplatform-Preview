# Using previews for IntelliJ plugin development with Jewel UI

Jewel is a Compose UI for IntelliJ: https://github.com/JetBrains/intellij-community/tree/master/platform/jewel

## Setup dependencies

Please follow the documentation from the Jewel project for general setup.
To preview Jewel compose code add the following dependency to your build.gradle.kts file:

```kotlin
implementation("org.jetbrains.jewel:jewel-int-ui-standalone-$branch:0.27.0") {
    exclude(group = "org.jetbrains.kotlinx")
}
```
Replace `$branch` with the branch you're using (e.g., 243 or 242) according to the Jewel documentation.

This dependency is needed for preview only because the preview cannot access the IntelliJ plugin API.

## Preview theme

To preview Jewel compose UI, a Jewel theme is required. For now, you can use the default IntUiTheme. The following PreviewTheme definition works best in my experience.

Here's the theme implementation:
```kotlin
@OptIn(InternalComposeUiApi::class)
@Composable
fun JewelPreviewTheme(content: @Composable () -> Unit) {
    val isDarkTheme = LocalSystemTheme.current == SystemTheme.Dark
    val themeDefinition = if (isDarkTheme) {
        JewelTheme.darkThemeDefinition()
    } else {
        JewelTheme.lightThemeDefinition()
    }
    val styling = if (isDarkTheme) {
        ComponentStyling.dark()
    } else {
        ComponentStyling.light(
            // This is a little bit closer to the IntelliJ theme
            editorTabStyle = TabStyle.Default.light(colors = TabColors.Default.light(background = Color.White))
        )
    }

    IntUiTheme(themeDefinition, styling) {
        Box(Modifier.background(JewelTheme.editorTabStyle.colors.background)) {
            content()
        }
    }
}
```

## Usage of HotPreview annotation

```kotlin
@HotPreview
@Composable
private fun PreviewYourComponent() {
    JewelPreviewTheme {
        YourUiComponent()
    }
}
```

Everything else is identical to normal Compose Multiplatform preview.
