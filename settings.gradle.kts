rootProject.name = "Multiplatform-Preview"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    //repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":hot_preview_annotation")
include(":hot_preview_render_1_7")
include(":hot_preview_render_1_8")