package io.github.jadarma.stego.core.test

import dev.whyoleg.cryptography.CryptographySystem
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode

@Suppress("unused")
class TestConfig : AbstractProjectConfig() {
    override val specExecutionMode = SpecExecutionMode.Concurrent
    override val dumpConfig = true

    override suspend fun beforeProject() {
        CryptographySystem.setDefaultRandom(ConstantRandom)
        super.beforeProject()
    }
}
