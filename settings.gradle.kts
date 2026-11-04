pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Blanko - Listen to different sounds"

// Kotlin Multiplatform code for the desktop app (UI, view models, data, DI).
include(":shared")

// Desktop app for Windows and Linux (Compose Multiplatform, Java Sound based audio).
include(":desktopApp")

// Pure-JVM audio engine used by the desktop app (decoders + looping mixer).
include(":desktop-audio")
