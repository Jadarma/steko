rootProject.name = "stego"
includeBuild("gradle/build-logic")
include(":stego-core", ":stego-cli")

pluginManagement {
    repositories {
        includeBuild("gradle/build-logic")
    }
}

plugins {
    id("conventions.project")
}
