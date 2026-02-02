plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
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
    sourceSets {
        nativeTest.dependencies {
            implementation(libs.kotest.engine)
            implementation(libs.kotest.assertions)
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("kotest.framework.config.fqn", "io.github.jadarma.stego.TestConfig")

    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}
