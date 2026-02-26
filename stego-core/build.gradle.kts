plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.testResources)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

group = "io.github.jadarma.stego"
version = "0.1.0-SNAPSHOT"

kotlin {
    listOf(
        linuxX64(),
        linuxArm64(),
        macosArm64(),
    ).forEach { target ->
        // On Linux, do not link any shared libraries that aren't needed.
        // This is especially useful for NixOS.
        if(target.name.startsWith("linux")) {
            target.binaries.all {
                linkerOpts("-Wl,--as-needed")
            }
        }
    }

    compilerOptions {
        explicitApi()
        optIn.addAll(
            "kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.cryptography.core)
            implementation(libs.kotlin.io.core)
            implementation(libs.kotlin.serialization.cbor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.testResources)
            implementation(libs.kotest.engine)
            implementation(libs.kotest.assertions)
            implementation(libs.kotlin.cryptography.optimalProvider)
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("kotest.framework.config.fqn", "io.github.jadarma.stego.core.test.TestConfig")

    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}
