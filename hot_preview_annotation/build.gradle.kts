import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.vanniktech.maven.publish") version Versions.vanniktechPlugin
}

val mavenGroupId = Versions.mavenGroupId
val mavenArtifactId = "hotpreview"

val mavenVersion = Versions.mavenLib


kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
            }
        }
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
    //signAllPublications()

    coordinates(mavenGroupId, mavenArtifactId, mavenVersion)

    pom {
        name.set("Compose Multiplatform Preview")
        description.set("""
            A plugin that shows previews of Compose Multiplatform files.
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
