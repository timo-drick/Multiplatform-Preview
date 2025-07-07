import java.util.*

plugins {
  id("ijp-plugin-base")
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.intelliJPlugin)
}

val pluginProperties = Properties()

project.file("plugin.properties").inputStream().use { pluginProperties.load(it) }

val rootProperties = Properties()

rootProject.file("gradle.properties").inputStream().use { rootProperties.load(it) }

version = "$version-${pluginProperties.getProperty("ijpTarget")}"

repositories {
  google()
  maven("https://packages.jetbrains.team/maven/p/kpm/public/")
  mavenCentral()

  intellijPlatform { defaultRepositories() }
}

dependencies {
  implementation(projects.core)

  testImplementation(libs.junit)

  intellijPlatform {
    create(
      rootProperties.getProperty("platformType"),
      rootProperties.getProperty("platformVersion"),
    )

    pluginVerifier()
    zipSigner()

    pluginModule(projects.core)

    // Add dependency on Compose and Jewel modules
    bundledModule("intellij.platform.jewel.foundation")
    bundledModule("intellij.platform.jewel.ui")
    bundledModule("intellij.platform.jewel.ideLafBridge")
    bundledModule("intellij.libraries.compose.foundation.desktop")
    bundledModule("intellij.libraries.skiko")
  }
}

intellijPlatform {
  pluginConfiguration {
    id = rootProperties.getProperty("pluginId")
    name = rootProperties.getProperty("pluginName")
    version = project.version.toString()
    description = rootProperties.getProperty("pluginDescription")
    changeNotes = getChangeNotesText(rootProject)

    ideaVersion {
      sinceBuild = pluginProperties.getProperty("pluginSinceBuild")
      untilBuild = pluginProperties.getProperty("pluginUntilBuild")
    }

    vendor {
      name = "Timo Drick"
      url = "https://github.com/timo-drick/Mutliplatform-Preview"
    }
  }

  signing {
    certificateChain = providers.environmentVariable("PLUGIN_CERTIFICATE_CHAIN")
    privateKey = providers.environmentVariable("PLUGIN_PRIVATE_KEY")
    password = providers.environmentVariable("PLUGIN_PRIVATE_KEY_PASSWORD")
  }

  publishing {
    token = providers.environmentVariable("PLUGIN_PUBLISH_TOKEN")
    // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    val channel: String = providers.environmentVariable("PLUGIN_PUBLISH_CHANNEL").getOrElse("dev")
    channels = listOf(channel)
  }

  pluginVerification { ides { recommended() } }
}

tasks.test { useJUnitPlatform() }
