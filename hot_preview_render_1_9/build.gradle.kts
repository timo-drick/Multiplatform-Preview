plugins {
    kotlin("jvm")
    kotlin("plugin.compose") version Versions.kotlin
    id("org.jetbrains.compose") version "1.9.0+dev2416"
    id("com.gradleup.shadow") version Versions.shadow
}

dependencies {
    implementation(project(":hot_preview_render_shared"))

    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.linux_arm64)
    implementation(compose.desktop.macos_x64)
    implementation(compose.desktop.macos_arm64)
    implementation(compose.desktop.windows_x64)
    implementation(compose.desktop.windows_arm64)
    implementation(compose.components.resources)
}