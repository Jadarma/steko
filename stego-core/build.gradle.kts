plugins {
    id("conventions.kotlin")
    id("conventions.testing")
}

group = "io.github.jadarma.stego"
version = "0.1.0-SNAPSHOT"

kotlin {
    listOf(
        linuxX64(),
        linuxArm64(),
        macosArm64(),
    )

    compilerOptions {
        explicitApi()
        allWarningsAsErrors = true

        optIn.addAll(
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.io.core)
            implementation(libs.kotlin.serialization.cbor)
            implementation(libs.kotlin.cryptography.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.cryptography.optimalProvider)
        }
    }

    fixLinuxCompile()
}
