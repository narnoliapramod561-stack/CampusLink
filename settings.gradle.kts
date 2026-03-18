pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    plugins {
        id("com.android.application") version "9.1.0"
        id("org.jetbrains.kotlin.android") version "2.2.10"
        id("com.google.dagger.hilt.android") version "2.51"
        id("com.google.devtools.ksp") version "2.3.2"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "CampusLink"
include(":app")
