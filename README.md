# Multiplatform Preview

Because the current Compose Multiplatform (CMP) preview does not have a lot of features and did not continue in development since years I started implementing my own preview system.

![](screenshots/compose_dev_challenge_home_screen.png)

## Requirements

Supported are projects with:

- Compose for Desktop (CFD) only code.
- Multiplatform projects
    - Common code
    - JVM code
- The module where you use the @HotPreview annotation has to have a jvm target configured.

This is because the previews are rendered using CFD itself.

Supported IDEs:

- IntelliJ 2024.3 or later
- Android Studio Meerkat RC1 or later

(Android Studio Meerkat limitation exists only because of problems with Jewel that does not work correctly in older IDEs)

## Usage

[![Maven Central](https://img.shields.io/maven-central/v/de.drick.compose/hotpreview.svg)](https://mvnrepository.com/artifact/de.drick.compose/hotpreview)

```kotlin
dependencies {
    implementation("de.drick.compose:hotpreview:<current version>")
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

You also need the plugin. It is published already to the  marketplace so just search for HotPreview.

If you want to compile it yourself please see documentation in the intellij project:
[intellij-plugin](intellij-plugin/README.md)

Here is a sample project using the @HotPreview annotation:
https://github.com/timo-drick/compose_desktop_dev_challenge

### Using HotPreviewParameterProvider

You can use `HotPreviewParameterProvider` to provide mock data for your previews. This allows you to test your composable with different data sets without creating multiple preview functions.

To use this feature:

1. Create a class that implements `HotPreviewParameterProvider<T>` where `T` is the type of data you want to provide:

```kotlin
class WeatherProvider : HotPreviewParameterProvider<Weather> {
    override val values: Sequence<Weather> = weatherForecast.asSequence()
}
```

2. Use the `@HotPreviewParameter` annotation on a parameter in your preview function:

```kotlin
@HotPreview(widthDp = 200, heightDp = 200)
@Composable
private fun PreviewWeatherCanvas(
    @HotPreviewParameter(WeatherProvider::class) weather: Weather
) {
    WeatherCanvas(
        modifier = Modifier.fillMaxSize(),
        seconds = 20.0,
        weather = weather,
    )
}
```

The plugin will use the values provided by your `HotPreviewParameterProvider` to generate previews with different data.

![](screenshots/hotpreview_parameter_provider_sample.png)

## Coil image preview

If you are using Coil 3 for multiplatform image loading and want to provide a preview image just have a look at the official documentation of coil here: https://coil-kt.github.io/coil/compose/#previews
Of course, it depends on you code how to integrate this into previews. You could also use this approach: https://coil-kt.github.io/coil/compose/#compose-multiplatform-resources

But both ways do work in HotPreview previews.


## Known limitations

- When at IDE startup a file with previews is opened sometimes the resources can not be loaded. Not sure how to avoid this. If you reopen the file everything should work.
- Recompilation only happens automatically when the source file is saved.
- ~~When adding a @HotPreview annotation to the file you have to close and open the file otherwise Android Studio / IntelliJ will not recognize that it contains a preview.~~
- ~~Following resource resolving functions will not work!~~
  - ~~Res.getUri()~~ (Does work since 0.2.0)
  - ~~Res.readBytes()~~ (Does work since 0.2.0)

## TODO list

- Improve rendering performance
  - increase code analysing performance
  - only render previews which are visible
- Implement a hierarchy viewer to be able to see composable components.
- Implement animation preview.
- Implement interactive mode.
- Maybe support also Android platform for previews using the layoutlib for rendering.
- ~~Implement PreviewParameterProvider like in android~~
- ~~Add support for Annotation classes. Make it possible to create Annotation class with HotPreview annotations.~~
