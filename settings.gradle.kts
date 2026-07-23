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
    }
}

rootProject.name = "TransitOS"

include(":app")
include(":core")
include(":core-data")
include(":core-network")
include(":core-design")
include(":core-ui")
include(":feature-home")
include(":feature-planner")
include(":feature-search")
include(":feature-settings")
include(":provider-metrovalencia")
