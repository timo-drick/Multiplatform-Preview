plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("java")
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")

    id("com.github.ben-manes.versions") version "0.51.0"
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
        pluginVerifier()
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.gradle") // Plugins must be also provided in plugin.xml!!!
    }
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.uiToolingPreview)
    implementation(kotlin("reflect"))
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

        description = "A plugin that shows previews of Compose Multiplatform files."
    }
}

/*kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}*/

fun isNonStable(version: String): Boolean {
    val unStableKeyword = listOf("alpha", "beta", "rc", "cr", "m", "preview", "dev").any { version.contains(it, ignoreCase = true) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = unStableKeyword.not() || regex.matches(version)
    return isStable.not()
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        //isNonStable(candidate.version)
        (isNonStable(candidate.version) && isNonStable(currentVersion).not())
    }
}
