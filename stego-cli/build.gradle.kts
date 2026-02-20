plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.testResources)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

group = "io.github.jadarma.stego"
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
            baseName = "stego"
            entryPoint = "io.github.jadarma.stego.cli.main"

            if(target.name.contains("linux")) {
                linkerOpts("-Wl,--as-needed")
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters"
        )
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
        nativeTest.dependencies {
            implementation(libs.kotlin.testResources)
            implementation(libs.kotest.engine)
            implementation(libs.kotest.assertions)
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("kotest.framework.config.fqn", "io.github.jadarma.stego.cli.TestConfig")

    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}
