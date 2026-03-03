import CompileOptions.Kotlin

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = CompileOptions.GROUP
version = CompileOptions.VERSION

kotlin {
    compilerOptions {
        languageVersion = Kotlin.languageVersion
        apiVersion = Kotlin.apiVersion

        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
        )
    }

    jvmToolchain {
        languageVersion = Kotlin.toolChainVersion
        vendor = Kotlin.toolChainVendor
    }
}
