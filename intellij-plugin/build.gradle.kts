plugins {
    id("com.github.ben-manes.versions") version "0.52.0"
}

tasks {
    named<Wrapper>("wrapper") {
        gradleVersion = project.property("gradleVersion") as String?
        distributionType = Wrapper.DistributionType.ALL
    }
}

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