plugins {
    id("conventions.kotlin")
    id("conventions.library")
    id("conventions.testing")
    id("conventions.detekt")
}

kotlin {
    linuxX64()
    linuxArm64()
    macosArm64()

    compilerOptions {
        optIn.addAll(
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.coroutines.core)
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
