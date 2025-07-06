@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins { id("dev.panuszewski.typesafe-conventions") version "0.6.0" }

dependencyResolutionManagement {
  repositories {
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    gradlePluginPortal()
    mavenCentral()
  }
}
