import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

object CompileOptions {

    const val GROUP = "io.github.jadarma.stego"
    const val VERSION = "0.1.0"

    object Kotlin {
        val languageVersion: KotlinVersion = KotlinVersion.KOTLIN_2_3
        val apiVersion: KotlinVersion = KotlinVersion.KOTLIN_2_3
        val toolChainVersion: JavaLanguageVersion = JavaLanguageVersion.of(21)
        val toolChainVendor: JvmVendorSpec = JvmVendorSpec.ADOPTIUM
    }
}
