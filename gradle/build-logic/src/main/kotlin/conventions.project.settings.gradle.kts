import org.gradle.api.initialization.resolve.RepositoriesMode

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

pluginManagement {
    repositories {
        // Applied from precompiled plugins.
        removeAll { true }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        removeAll { true }
        mavenCentral()
    }
}
