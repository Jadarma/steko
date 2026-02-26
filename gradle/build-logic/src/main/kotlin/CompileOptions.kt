import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

object CompileOptions {

    object Kotlin {
        val languageVersion: KotlinVersion = KotlinVersion.KOTLIN_2_3
        val apiVersion: KotlinVersion = KotlinVersion.KOTLIN_2_3
        val toolChainVersion: JavaLanguageVersion = JavaLanguageVersion.of(21)
        val toolChainVendor: JvmVendorSpec = JvmVendorSpec.ADOPTIUM
    }
}
