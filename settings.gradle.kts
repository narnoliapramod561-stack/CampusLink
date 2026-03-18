pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.1.0"
        id("org.jetbrains.kotlin.android") version "2.1.0"
        id("com.google.dagger.hilt.android") version "2.51"
        id("com.google.devtools.ksp") version "2.1.0-1.0.29"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
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
