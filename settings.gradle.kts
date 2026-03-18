pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.3.2"
        id("org.jetbrains.kotlin.android") version "2.0.0"
        id("com.google.dagger.hilt.android") version "2.51"
        id("com.google.devtools.ksp") version "2.0.0-1.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "CampusLink"
include(":app")
