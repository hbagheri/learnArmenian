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

rootProject.name = "learnArmenian"

include(":app")
include(":core:designsystem")
include(":core:database")
include(":core:data")
include(":core:audio")
include(":feature:home")
include(":feature:quiz")
include(":feature:phrases")
include(":feature:practice")
include(":feature:reviews")
include(":feature:lessons")
include(":feature:stories")
include(":feature:game")
