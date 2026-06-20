pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
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
        // youtubedl-android (the yt-dlp backend, io.github.junkfood02...) is on
        // Maven Central now; JitPack kept only as a fallback for older forks.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "YtdlClean"
include(":app")
