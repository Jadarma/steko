plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

group = "io.github.jadarma.stego"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    linuxX64()
    linuxArm64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.cryptography.core)
            implementation(libs.kotlin.io.core)
        }
        commonTest.dependencies {
            implementation(libs.kotest.engine)
            implementation(libs.kotest.assertions)
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("kotest.framework.config.fqn", "io.github.jadarma.stego.core.TestConfig")

    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}
