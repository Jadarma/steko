rootProject.name = "steko"
includeBuild("gradle/build-logic")
include(":steko-core", ":steko-cli")

pluginManagement {
    repositories {
        includeBuild("gradle/build-logic")
    }
}

plugins {
    id("conventions.project")
}
