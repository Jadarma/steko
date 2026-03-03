package io.github.jadarma.stego.core.test

import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * A very bad idea, but required for testing for now.
 * Since we have no control over the random IV that gets assigned to AES encryption, for the tests we overwrite the
 * random source to produce the exact number every time it is requested.
 * The stego algorithm uses this only for noise and IV initialization, both are fine accepting repetitive junk.
 */
object ConstantRandom : CryptographyRandom() {
    override fun nextBits(bitCount: Int): Int = 0xDEADBEEF.toInt().shr(Int.SIZE_BITS - bitCount)
    override fun nextBoolean(): Boolean = true
    override fun nextBytes(array: ByteArray, fromIndex: Int, toIndex: Int): ByteArray = array.apply {
        for(i in fromIndex until toIndex) {
            set(i, 0x99.toByte())
        }
    }
    override fun nextBytes(array: ByteArray): ByteArray = nextBytes(array, 0, array.size)
    override fun nextBytes(size: Int): ByteArray = ByteArray(size) { 0x99.toByte() }
    override fun nextDouble(): Double = 1234.56789
    override fun nextDouble(until: Double): Double = nextDouble().coerceAtMost(until - 0.001)
    override fun nextDouble(from: Double, until: Double): Double = nextDouble().coerceIn(from, until - 0.001)
    override fun nextFloat(): Float = 1234.567F
    override fun nextInt(): Int = 0xDEADBEEF.toInt()
    override fun nextInt(until: Int): Int = nextInt().coerceAtMost(until - 1)
    override fun nextInt(from: Int, until: Int): Int = nextInt().coerceIn(from, until - 1)
    override fun nextLong(): Long = 0x1234DEADBEEFCAFEL
    override fun nextLong(until: Long): Long = nextLong().coerceAtMost(until - 1)
    override fun nextLong(from: Long, until: Long): Long = nextLong().coerceIn(from, until - 1)
}
