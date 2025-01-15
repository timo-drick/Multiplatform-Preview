# Multiplatform Preview

Because the current Compose Multiplatform (CMP) preview does not have a lot of features and did not continue in development since years I started implementing my own preview system.

![](screenshots/compose_dev_challenge_home_screen.png)

## Requirements

Supported are projects with:

- Compose for Desktop (CFD) only code.
- Multiplatform projects
    - Common code target
    - JVM code target

This is because the previews are rendered using CMP itself.

Supported IDEs:

- IntelliJ 2024.3 or later
- Android Studio Meerkat or later

(AS Meerkat limitation exists only because of problems with Jewel that does not work correctly in older IDEs)

## Usage

```kotlin
dependencies {
    implementation("de.drick.compose:hotpreview:0.1.3")
}
```

```kotlin
@HotPreview(name = "phone dark", widthDp = 400, heightDp = 800, fontScale = 1f, darkMode = true)
@HotPreview(name = "phone", widthDp = 400, heightDp = 800, fontScale = 1.5f, darkMode = false, density = 1f)
@HotPreview(name = "dark", widthDp = 1000, heightDp = 800, fontScale = 1f, density = 1f)
@HotPreview(widthDp = 1000, heightDp = 800, fontScale = 1.5f, darkMode = false)
@Composable
fun PreviewHomeScreen() {
  MyTheme {
    HomeScreen()
  }
}
```

You also need the plugin. It is published alread to the  marketplace so just search for HotPreview.
If you want to compile it yourself please see documentation in the intellij project:
[intellij-plugin](intellij-plugin/README.md)

Here is a sample project using the @HotPreview annotation:
https://github.com/timo-drick/compose_desktop_dev_challenge


## Known limitations

- When at IDE startup a file with previews is opened sometimes the resources can not be loaded. Not sure how to avoid this. If you reopen the file everything should work.
- Following resource resolving functions will not work!
  - Res.getUri()
  - Res.readBytes()
