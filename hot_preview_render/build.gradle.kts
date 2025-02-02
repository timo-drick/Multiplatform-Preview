plugins {
    kotlin("jvm")
    kotlin("plugin.compose") version Versions.kotlin
    id("org.jetbrains.compose") version Versions.composeMP
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.linux_arm64)
    implementation(compose.desktop.macos_x64)
    implementation(compose.desktop.macos_arm64)
    implementation(compose.desktop.windows_x64)
    implementation(compose.desktop.windows_arm64)
}