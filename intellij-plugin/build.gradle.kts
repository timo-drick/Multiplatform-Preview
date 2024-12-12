plugins {
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("java")
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")
}

group = "de.drick.compose.hotpreview"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
        //releases()
        //marketplace()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        instrumentationTools()
        pluginVerifier()
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.gradle") // Plugins must be also provided in plugin.xml!!!
    }
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.uiToolingPreview)
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellijPlatform {
    projectName = "HotPreviewPlugin"

    pluginVerification {
        ides {
            recommended()
        }
    }

    pluginConfiguration {
        id = "de.drick.compose.hotpreview.plugin"
        name = "HotPreview plugin"
        version = "0.1.0"
    }
}

/*kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}*/