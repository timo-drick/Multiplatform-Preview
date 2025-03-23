import org.intellij.lang.annotations.Language

plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
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
val ijPlatform = providers.environmentVariable("IJP_VERSION").getOrElse("2024.3.4")
val branch = providers.environmentVariable("IJP_BRANCH").getOrElse("243")

val versionName = "0.5.0-$ijPlatform"

@Language("HTML")
val changeNotesText = """
<h3>V 0.5.0</h3>
<ul>
    <li>Added gutter icon to HotPreview annotations with a graphical editor</li>
    <li>Added Horizontal scrolling when single preview is too wide</li>
    <li>Added gallery preview mode</li>
    <li>Fixed stability issues when rendering previews</li>
</ul>
    
<h3>V 0.4.0</h3>
<ul>
    <li>Added shortcut key to recompile</li>
    <li>Added settings to change gradle compile parameters and recompiling behaviour</li>
    <li>Improved rendering performance.</li>
    <li>Added foldable sections for preview functions</li>
    <li>Added support for locale</li>
    <li>Added support for Groups</li>
</ul>

<h3>V 0.3.1</h3>
<ul>
    <li>Added support for Android Studio Ladybug and Intellij 2024.2.5</li>
</ul>

<h3>V 0.3.0</h3>
<ul>
    <li>Improved error handling and visualization.</li>
    <li>
        Plugin does override the default editor now all the time for kotlin files.
        This is necessary because otherwise it is not possible to show preview when the user add @HotPreview annotation.
    </li>
    <li>
        Added support for annotation classes. You can now create annotation classes with @HotPreview annotations.
    </li>
    <li>Self preview is now working. So the source of this plugin can also be previewed.</li>
</ul>

<h3>V 0.2.0</h3>
<ul>
    <li>Added support for macos. Window x64/arm64 should also work (untested)</li>
    <li>Using an independent classloader now. Could solved some issues with resource loading.</li>
    <li>Turned off recompile when opening a file.</li>
    <li>Wait until indexing is finished before analyzing files.</li>
    <li>Replaced @HotPreview annotation reflection code analyzer by psi file analyzer.</li>
    <li>Set LocalInspectionMode to true in previews.</li>
    <li>Added navigation to @HotPreview annotation on preview click.</li>
    <li>Added on hover focus for preview items.</li>
</ul>

<h3>V 0.1.1</h3>
<ul>
    <li>Show errors when trying to compile and render the preview.</li>
</ul>
""".trimIndent()

dependencies {

    intellijPlatform {
        //See this list for available versions: https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
        androidStudio("2024.3.1.13") //Meerkat
        //androidStudio("2024.2.2.13") //Ladybug
        //intellijIdeaCommunity(ijPlatform)
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

    implementation("de.drick.compose:hotpreview:0.1.6") {
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
        version = versionName

        ideaVersion {
            sinceBuild = branch
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
