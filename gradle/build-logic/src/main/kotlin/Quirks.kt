import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * VooDoo magic workaround for accessing Gradle Version Catalogs from within precompiled convention scripts.
 * Also see: https://github.com/gradle/gradle/issues/15383
 */
val Project.libs: LibrariesForLibs get() =
    rootProject.project.extensions.getByName<LibrariesForLibs>("libs")

/**
 * On Linux, do not link any shared libraries that aren't needed.
 * This is especially useful for NixOS.
 * Also see: https://youtrack.jetbrains.com/projects/KT/issues/KT-55643
 */
fun KotlinMultiplatformExtension.fixLinuxCompile() {
    targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.name.startsWith("linux") }
        .forEach { target ->
            target.binaries.all {
                linkerOpts("-Wl,--as-needed")
            }
        }
}
