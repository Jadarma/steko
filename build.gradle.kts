plugins {
    kotlin("multiplatform") version "2.3.0"
}

group = "io.github.jadarma"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    listOf(
        linuxX64(),
        linuxArm64(),
        macosArm64(),
    ).forEach { target ->
        target.binaries.executable {
            entryPoint = "io.github.jadarma.steggo.main"
        }
    }
}
