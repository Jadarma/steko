package io.github.jadarma.stego.core

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode

class TestConfig : AbstractProjectConfig() {
    override val specExecutionMode = SpecExecutionMode.Concurrent
    override val dumpConfig = true
    override val displayFullTestPath: Boolean = true
}
