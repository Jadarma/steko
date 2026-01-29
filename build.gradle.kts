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
            entryPoint = "io.github.jadarma.steggo.main"
        }
    }
}
