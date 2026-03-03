plugins {
    id("conventions.kotlin")
    id("conventions.testing")
    id("conventions.detekt")
}

kotlin {
    listOf(
        linuxX64(),
        linuxArm64(),
        macosArm64(),
    ).forEach { target ->
        target.compilations.getByName("main") {
            cinterops {
                val stbImage by creating {
                    val dir = project.file("src/nativeInterop/cinterop/stbImage")
                    definitionFile.set(dir.resolve("stb_image.def"))
                    packageName("stbImage")
                    compilerOpts("-I/${dir}")
                }
            }
        }
        target.binaries.executable {
            baseName = "stego"
            entryPoint = "$group.cli.main"
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(project(":stego-core"))
            implementation(libs.kotlin.cryptography.optimalProvider)
            implementation(libs.kotlin.io.core)
            implementation(libs.kotlin.serialization.core)
            implementation(libs.kotlin.serialization.cbor)
            implementation(libs.clikt.core)
            implementation(libs.clikt.markdown)
        }
    }

    fixLinuxCompile()
}
