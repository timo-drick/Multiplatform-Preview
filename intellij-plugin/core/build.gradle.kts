import java.util.*

plugins {
  id("ijp-plugin-base")
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.intelliJModule)
  alias(libs.plugins.composePlugin)
  kotlin("jvm")
  id("idea")
}

repositories {
  maven("https://packages.jetbrains.team/maven/p/kpm/public/")
  mavenCentral()
  google()

  intellijPlatform { defaultRepositories() }
}

val rootProperties = Properties()
rootProject.file("gradle.properties").inputStream().use { rootProperties.load(it) }


dependencies {

  // Add kotlinx.coroutines as compileOnly dependency to avoid class loading conflicts
  //compileOnly("om.intellij.platform:kotlinx-coroutines-core-jvm:1.8.0-intellij-11")
  //compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0-intellij-11")

  /*implementation(libs.jewel.intUi) {
    exclude("org.jetbrains.compose")
    exclude("org.jetbrains.compose.foundation")
    exclude("org.jetbrains.compose.runtime")
    exclude("org.jetbrains.compose.ui")
    exclude("org.jetbrains.jewel")
    exclude("org.jetbrains.kotlin")
    exclude("org.jetbrains.kotlinx")
    exclude("org.jetbrains.skiko")
  }*/

  implementation(libs.compose.components.resources) {
    exclude("org.jetbrains.compose")
    exclude("org.jetbrains.compose.foundation")
    exclude("org.jetbrains.compose.runtime")
    exclude("org.jetbrains.compose.ui")
    exclude("org.jetbrains.jewel")
    exclude("org.jetbrains.kotlin")
    exclude("org.jetbrains.kotlinx")
    exclude("org.jetbrains.skiko")
  }

  implementation(libs.drick.compose.hotpreview) {
    exclude("org.jetbrains.compose")
    exclude("org.jetbrains.compose.foundation")
    exclude("org.jetbrains.compose.runtime")
    exclude("org.jetbrains.compose.ui")
    exclude("org.jetbrains.jewel")
    exclude("org.jetbrains.kotlin")
    exclude("org.jetbrains.kotlinx")
    exclude("org.jetbrains.skiko")
  }

  testImplementation(libs.junit)

  intellijPlatform {
    create(
      rootProperties.getProperty("platformType"),
      rootProperties.getProperty("platformVersion"),
    )

    bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle") // Plugins must be also provided in plugin.xml!!!

    // Add dependency on Compose and Jewel modules
    bundledModule("intellij.platform.jewel.foundation")
    bundledModule("intellij.platform.jewel.ui")
    bundledModule("intellij.platform.jewel.ideLafBridge")
    bundledModule("intellij.libraries.compose.foundation.desktop")
    bundledModule("intellij.libraries.skiko")

    pluginVerifier()
    //zipSigner()
  }
}

tasks.test { useJUnitPlatform() }

tasks.register<GradleBuild>("buildRenderModule") {
  group = "build"
  dir = file("../../")
  tasks = listOf(
    ":hot_preview_render_1_7:shadowJar",
    ":hot_preview_render_1_8:shadowJar",
    ":hot_preview_render_1_9:shadowJar"
  )
}

val renderModulePath17 = layout.projectDirectory.dir("../../hot_preview_render_1_7/build/libs")
val renderModulePath18 = layout.projectDirectory.dir("../../hot_preview_render_1_8/build/libs")
val renderModulePath19 = layout.projectDirectory.dir("../../hot_preview_render_1_9/build/libs")

tasks.processResources {
  dependsOn("buildRenderModule")
  from(renderModulePath17, renderModulePath18, renderModulePath19)
}
