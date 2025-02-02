plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("java")
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")

    id("com.github.ben-manes.versions") version "0.51.0"
}

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

val ijPlatform = "2024.3.2"
val branch = "243"

dependencies {

    intellijPlatform {
        // androidStudio(ijPlatform)
        intellijIdeaCommunity(ijPlatform)
        pluginVerifier()
        zipSigner()
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.gradle") // Plugins must be also provided in plugin.xml!!!
    }

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
    /*implementation(compose.desktop.windows_arm64) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }*/

    // See https://github.com/JetBrains/Jewel/releases for the release notes
    // The platform version is a supported major IJP version (e.g., 232 or 233 for 2023.2 and 2023.3 respectively)
    implementation("org.jetbrains.jewel:jewel-ide-laf-bridge-$branch:0.27.0")

    implementation(compose.components.resources) {
        exclude(group = "org.jetbrains.kotlinx")
    }

    /*
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable") {
        exclude(group = "org.jetbrains.kotlinx")
    }*/

    implementation("de.drick.compose:hotpreview:0.1.4") {
        exclude(group = "org.jetbrains.kotlinx")
    }

    testImplementation("junit", "junit", "4.12")
}

tasks.register<GradleBuild>("buildRenderModule") {
    group = "build"
    dir = file("../")
    tasks = listOf(":hot_preview_render:shadowJar")
}

val renderModulePath = layout.projectDirectory.dir("../hot_preview_render/build/libs")

tasks.processResources {
    dependsOn("buildRenderModule")
    from(renderModulePath)
}


// See https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    projectName = "HotPreviewPlugin"

    pluginConfiguration {
        id = "de.drick.compose.hotpreview.plugin"
        name = "Compose Multiplatform HotPreview"
        version = "0.2.0"

        ideaVersion {
            sinceBuild = branch
        }

        description = "A plugin that shows previews of Compose Multiplatform files."

        vendor {
            name = "Timo Drick"
            url = "https://github.com/timo-drick/Mutliplatform-Preview"
        }

        changeNotes = """
            V 0.2.0
            - Added support for macos, window x64/arm64 should work (untested)
            - Using an independent classloader now. Could solved some issues with resource loading.
            - Turned off recompile when opening a file.
            - Wait until indexing is finished before analyzing files.
            - Replaced @HotPreview annotation reflection code analzer by psi file analyzer.
            - Set LocalInspectionMode to true in previews.
            - Added navigation to @HotPreview annotation on preview click.
            - Added on hover focus for preview items.
            
            V 0.1.1
            - Show errors when trying to compile and render the preview.
        """.trimIndent()
    }

    signing {
        certificateChain = providers.environmentVariable("PLUGIN_CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PLUGIN_PRIVATE_KEY")
        password = providers.environmentVariable("PLUGIN_PRIVATE_KEY_PASSWORD")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("PLUGIN_PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = listOf("default")
    }
}

/*kotlin {
    jvmToolchain(21)
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
