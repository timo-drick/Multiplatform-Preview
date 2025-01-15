plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("java")
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")

    id("com.github.ben-manes.versions") version "0.51.0"
}

//group = "de.drick.compose.hotpreview"
//version = "0.1.0"

repositories {
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
        // releases()
        // marketplace()
    }
    mavenLocal()
}

dependencies {
    val ijPlatform = "2024.3"
    val branch = "243"

    intellijPlatform {
        // androidStudio(ijPlatform)
        intellijIdeaCommunity(ijPlatform)
        pluginVerifier()
        zipSigner()
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.gradle") // Plugins must be also provided in plugin.xml!!!
    }

    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }

    // See https://github.com/JetBrains/Jewel/releases for the release notes
    // The platform version is a supported major IJP version (e.g., 232 or 233 for 2023.2 and 2023.3 respectively)
    implementation("org.jetbrains.jewel:jewel-ide-laf-bridge-$branch:0.27.0")

    implementation(compose.components.resources) {
        exclude(group = "org.jetbrains.kotlinx")
    }

    implementation("org.jetbrains.kotlin:kotlin-reflect") {
        exclude(group = "org.jetbrains.kotlinx")
    }
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable") {
        exclude(group = "org.jetbrains.kotlinx")
    }

    implementation("de.drick.compose:hotpreview:0.1.3") {
        exclude(group = "org.jetbrains.kotlinx")
    }

    testImplementation("junit", "junit", "4.12")
}

// See https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    projectName = "HotPreviewPlugin"

    pluginVerification {
        ides {
            recommended()
        }
    }

    pluginConfiguration {
        id = "de.drick.compose.hotpreview.plugin"
        name = "Compose Multiplatform HotPreview"
        version = "0.1.0"

        ideaVersion {
            sinceBuild = "243"
        }

        description = "A plugin that shows previews of Compose Multiplatform files."

        vendor {
            name = "Timo Drick"
            url = "https://github.com/timo-drick/Mutliplatform-Preview"
        }

        //changeNotes = ""
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
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
        (isNonStable(candidate.version) && isNonStable(currentVersion).not())
    }
}
