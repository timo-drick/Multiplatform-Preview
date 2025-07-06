import java.util.*

plugins { `kotlin-dsl` }

val properties = Properties()

project.file("../gradle.properties").inputStream().use { properties.load(it) }

val jdkLevel = properties.getProperty("jdk.level") as String

kotlin { jvmToolchain { languageVersion = JavaLanguageVersion.of(jdkLevel) } }

dependencies { implementation(libs.kotlin.gradlePlugin) }
