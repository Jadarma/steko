import CompileOptions.GROUP
import org.gradle.internal.classpath.Instrumented.systemProperty

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

tasks.withType<AbstractTestTask>().configureEach {
    val module = project.name.removePrefix("steko-")
    systemProperty("kotest.framework.config.fqn", "$GROUP.$module.test.TestConfig")
    failOnNoDiscoveredTests = false
    logger.lifecycle("UP-TO-DATE check for $name is disabled, forcing it to run.")
    outputs.upToDateWhen { false }
}
