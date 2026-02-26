plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("conventions.testing")
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
            implementation(libs.kotlin.cryptography.optimalProvider)
        }
    }
}
