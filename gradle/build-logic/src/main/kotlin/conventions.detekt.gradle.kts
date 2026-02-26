import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import org.gradle.kotlin.dsl.kotlin

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("dev.detekt")
}

detekt {
    buildUponDefaultConfig = false
    parallel = true
}

tasks {

    val detektMain by registering(Task::class)
    val detektTest by registering(Task::class)
    val detektBaselineMain by registering(Task::class)
    val detektBaselineTest by registering(Task::class)

    check {
        dependsOn(detektMain, detektTest)
    }

    named<DetektCreateBaselineTask>("detektBaseline") {
        dependsOn(detektBaselineMain, detektBaselineTest)
    }

    withType<Detekt> {
        reports {
            html.required = true
            sarif.required = true
            markdown.required = false
            checkstyle.required = false
        }

        // See https://github.com/detekt/detekt/issues/5611
        exclude { it.file.invariantSeparatorsPath.contains("build/generated/")}

        when {
            name.endsWith("MainSourceSet") -> {
                detektMain.configure { dependsOn(this@withType) }
                baseline = file("$rootDir/gradle/detekt/baseline/${project.name}-main.xml")
                config.setFrom(
                    "$rootDir/gradle/detekt/config/detekt.yml",
                    "$rootDir/gradle/detekt/config/${project.name}.yml",
                )
            }

            name.endsWith("TestSourceSet") -> {
                detektTest.configure { dependsOn(this@withType) }
                baseline = file("$rootDir/gradle/detekt/baseline/${project.name}-test.xml")
                config.setFrom(
                    "$rootDir/gradle/detekt/config/detekt.yml",
                    "$rootDir/gradle/detekt/config/${project.name}.yml",
                    "$rootDir/gradle/detekt/config/detekt-test.yml",
                )
            }
        }
    }

    withType<DetektCreateBaselineTask> {
        when {
            name.endsWith("MainSourceSet") -> {
                detektBaselineMain.configure { dependsOn(this@withType) }
                baseline = file("$rootDir/gradle/detekt/baseline/${project.name}-main.xml")
                config.setFrom(
                    "$rootDir/gradle/detekt/config/detekt.yml",
                    "$rootDir/gradle/detekt/config/${project.name}.yml",
                )
            }

            name.endsWith("TestSourceSet") -> {
                detektBaselineTest.configure { dependsOn(this@withType) }
                baseline = file("$rootDir/gradle/detekt/baseline/${project.name}-test.xml")
                config.setFrom(
                    "$rootDir/gradle/detekt/config/detekt.yml",
                    "$rootDir/gradle/detekt/config/${project.name}.yml",
                    "$rootDir/gradle/detekt/config/detekt-test.yml",
                )
            }
        }
    }
}
