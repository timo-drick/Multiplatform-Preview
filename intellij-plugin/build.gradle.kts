import org.intellij.lang.annotations.Language
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("java")
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("idea")

    id("com.github.ben-manes.versions") version "0.52.0"
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

//val ijPlatform = providers.environmentVariable("IJP_VERSION").getOrElse("2024.2.5")
//val branch = providers.environmentVariable("IJP_BRANCH").getOrElse("242")
val ijPlatform = providers.environmentVariable("IJP_VERSION").getOrElse("2024.3.2")
val branch = providers.environmentVariable("IJP_BRANCH").getOrElse("243")

val versionName = "0.9.1-$ijPlatform"

data class VersionInfo(
    val version: String,
    val items: MutableList<String> = mutableListOf<String>()
)

@Language("HTML")
val changeNotesText = run {
    val releaseNotesPath = Paths.get(projectDir.path, "RELEASE_NOTES_PLUGIN.md")
    val versionModel = mutableListOf<VersionInfo>()
    var currentVersion: VersionInfo? = null
    var versionCount = 4
    Files.readAllLines(releaseNotesPath).forEach { line ->
        if (line.startsWith("### ") && versionCount > 0) {
            versionCount--
            if (versionCount > 0) {
                val version = line.substring(3).trim()
                currentVersion = VersionInfo(version).also {
                    versionModel.add(it)
                }
            }
        } else if (versionCount > 0 && line.startsWith("- ")) {
            currentVersion?.items?.add(line.substring(2).trim())
        }
    }

    val htmlBuilder = StringBuilder()
    versionModel.forEach { versionInfo ->
        htmlBuilder.append("<h3>${versionInfo.version}</h3>")
        htmlBuilder.append("<ul>")
        versionInfo.items.forEach { items ->
            htmlBuilder.append("<li>$items</li>")
        }
        htmlBuilder.append("</ul>")
    }
    htmlBuilder.toString()
}

dependencies {

    intellijPlatform {
        //See this list for available versions: https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
        //androidStudio("2024.3.2.14") //Meerkat
        //androidStudio("2024.2.2.13") //Ladybug
        intellijIdeaCommunity(ijPlatform)
        pluginVerifier()
        zipSigner()
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin", "com.intellij.gradle") // Plugins must be also provided in plugin.xml!!!
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

    // needed for self preview only (jewel bridge cannot be used because there is no access to the plugin api)
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone-$branch:0.27.0") {
        exclude(group = "org.jetbrains.kotlinx")
    }

    implementation(compose.components.resources) {
        exclude(group = "org.jetbrains.kotlinx")
    }

    /*
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable") {
        exclude(group = "org.jetbrains.kotlinx")
    }*/

    implementation("de.drick.compose:hotpreview:0.2.0") {
        exclude(group = "org.jetbrains.kotlinx")
    }

    testImplementation("junit", "junit", "4.12")
}

tasks.register<GradleBuild>("buildRenderModule") {
    group = "build"
    dir = file("../")
    tasks = listOf(
        ":hot_preview_render_1_7:shadowJar",
        ":hot_preview_render_1_8:shadowJar",
        ":hot_preview_render_1_9:shadowJar"
    )
}

val renderModulePath17 = layout.projectDirectory.dir("../hot_preview_render_1_7/build/libs")
val renderModulePath18 = layout.projectDirectory.dir("../hot_preview_render_1_8/build/libs")
val renderModulePath19 = layout.projectDirectory.dir("../hot_preview_render_1_9/build/libs")

tasks.processResources {
    dependsOn("buildRenderModule")
    from(renderModulePath17, renderModulePath18, renderModulePath19)
}


// See https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
intellijPlatform {
    projectName = "HotPreviewPlugin"

    pluginConfiguration {
        id = "de.drick.compose.hotpreview.plugin"
        name = "Compose Multiplatform HotPreview"
        version = versionName

        ideaVersion {
            sinceBuild = branch
            untilBuild = "$branch.*"
        }

        description = "A plugin that shows previews of Compose Multiplatform files."

        vendor {
            name = "Timo Drick"
            url = "https://github.com/timo-drick/Mutliplatform-Preview"
        }

        changeNotes = changeNotesText
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

