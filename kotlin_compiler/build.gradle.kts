plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version Versions.shadow
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")
}