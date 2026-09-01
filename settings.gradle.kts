pluginManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/WL-Gaming/packages-android")
            credentials {
                username = System.getProperty("gpr.wlgaming.usr") ?: System.getenv("GPR_WLGAMING_USR")
                password = System.getProperty("gpr.wlgaming.key") ?: System.getenv("GPR_WLGAMING_KEY")
            }
        }
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // shared Gaming Maven repository
        maven {
            url = uri("https://maven.pkg.github.com/WL-Gaming/packages-android")
            credentials {
                username = System.getProperty("gpr.wlgaming.usr") ?: System.getenv("GPR_WLGAMING_USR")
                password = System.getProperty("gpr.wlgaming.key") ?: System.getenv("GPR_WLGAMING_KEY")
            }
        }
        // project Maven repository
        maven {
            url = uri("https://maven.pkg.github.com/WL-Gaming/packages-android-dt")
            credentials {
                username = System.getProperty("gpr.wlgaming.usr") ?: System.getenv("GPR_WLGAMING_USR")
                password = System.getProperty("gpr.wlgaming.key") ?: System.getenv("GPR_WLGAMING_KEY")
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("gaming") {
            from("com.engagecraft.gaming.core:catalog-shared:2026.08.01")
        }
    }
}

rootProject.name = "epllastmanstanding"

include(":app")
include(":epllastmanstanding")
