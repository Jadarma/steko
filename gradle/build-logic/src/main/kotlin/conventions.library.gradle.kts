plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    compilerOptions {
        explicitApi()
        allWarningsAsErrors = true
    }
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
        strictValidation = true
    }
}
