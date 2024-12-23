import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version Versions.kotlin
    id("com.android.application") version Versions.androidGraglePlugin
    id("com.vanniktech.maven.publish") version Versions.vanniktechPlugin
}

val mavenGroupId = Versions.mavenGroupId
val mavenArtifactId = "hotpreview"

val mavenVersion = Versions.mavenLib


kotlin {

    jvm()

    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class) wasmJs() { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
    }
}

android {
    compileSdk = Versions.androidCompileSdk
    namespace = "de.drick.compose.hotpreview"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.androidMinSdk
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

// https://vanniktech.github.io/gradle-maven-publish-plugin/central/

mavenPublishing {
    configure(
        KotlinMultiplatform(
            sourcesJar = true,
        )
    )
    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
    signAllPublications()

    coordinates(mavenGroupId, mavenArtifactId, mavenVersion)

    pom {
        name.set("Compose Multiplatform Preview")
        description.set("""
            A plugin that shows previews of Compose Multiplatform kotlin files.
        """.trimIndent())
        url.set("https://github.com/timo-drick/Mutliplatform-Preview")
        licenses {
            license {
                name = "The Unlicense"
                url = "https://unlicense.org/"
            }
        }
        developers {
            developer {
                id.set("timo-drick")
                name.set("Timo Drick")
                url.set("https://github.com/timo-drick")
            }
        }
        scm {
            url.set("https://github.com/timo-drick/Mutliplatform-Preview")
            connection.set("scm:git:ssh://git@github.com:timo-drick/Mutliplatform-Preview.git")
            developerConnection.set("scm:git:ssh://git@github.com:timo-drick/Mutliplatform-Preview.git")
        }
    }
}
