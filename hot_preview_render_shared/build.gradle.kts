plugins {
    kotlin("jvm")
    kotlin("plugin.compose") version Versions.kotlin
    id("org.jetbrains.compose") version "1.7.3"
    id("com.gradleup.shadow") version Versions.shadow
}

dependencies {
    implementation(compose.desktop.common)
    implementation(compose.components.resources)
}