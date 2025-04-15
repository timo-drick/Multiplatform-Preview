plugins {
    kotlin("jvm")
    kotlin("plugin.compose") version Versions.kotlin
    id("org.jetbrains.compose") version Versions.composeMP
    id("com.gradleup.shadow") version Versions.shadow
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
}