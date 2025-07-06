import java.util.Properties

plugins { kotlin("jvm") }

val rootProperties = Properties()

rootProject.file("gradle.properties").inputStream().use { rootProperties.load(it) }

group = rootProperties.getProperty("pluginGroup")

version = rootProperties.getProperty("pluginVersion")

// Set the JVM language level used to build the project.
kotlin { jvmToolchain(rootProperties.getProperty("jdk.level").toInt()) }
