import java.util.*

plugins {
    id("ijp-plugin-base")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.intelliJPlugin)
    alias(libs.plugins.composePlugin)
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
    implementation(libs.jewel.ideLafBridge) {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
    //implementation("org.jetbrains.jewel:jewel-ide-laf-bridge-243:0.27.0")
    implementation(projects.core)

    implementation(compose.desktop.linux_x64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation(compose.desktop.linux_arm64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation(compose.desktop.macos_x64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation(compose.desktop.macos_arm64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation(compose.desktop.windows_x64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation(compose.desktop.windows_arm64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }

    testImplementation(libs.junit)

    intellijPlatform {
        create(
            rootProperties.getProperty("platformType"),
            pluginProperties.getProperty("platformVersion"),
        )

        pluginVerifier()
        zipSigner()

        pluginModule(projects.core)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = rootProperties.getProperty("pluginId")
        name = rootProperties.getProperty("pluginName")
        version = project.version.toString()
        description = rootProperties.getProperty("pluginDescription")

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
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }

    pluginVerification { ides { recommended() } }
}

tasks.test { useJUnitPlatform() }
