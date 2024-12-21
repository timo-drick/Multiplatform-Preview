# Multiplatform Preview

Because the current Compose Multiplatform (CMP) preview does not have a lot of features and did not continued in development since years I started implementing my own preview system.

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
    implementation("de.drick.compose:hotpreview:0.1.0")
}
```

You also need the plugin. Until it is published to the marketplace you need to build it yourself.
Please see documentation in intellij project:
[intellij-plugin](intellij-plugin/README.md)

## Known limitations

Following resource resolving function will not work:
- Res.getUri()
- Res.readBytes()
