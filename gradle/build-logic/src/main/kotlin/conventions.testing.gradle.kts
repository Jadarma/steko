plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.kotest")
    id("com.google.devtools.ksp")
    id("com.goncalossilva.resources")
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.testResources)
            implementation(libs.kotest.engine)
            implementation(libs.kotest.assertions)
        }
    }
}

tasks.withType<Test>().configureEach {
    val module = project.name.removePrefix("stego-")
    systemProperty("kotest.framework.config.fqn", "$group.$module.test.TestConfig")

    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}
