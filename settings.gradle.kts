rootProject.name = "stego"
include(":stego-core", ":stego-cli")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}
