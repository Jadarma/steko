package io.github.jadarma.stego.core.internal

import kotlin.random.Random

/**
 * Kotlin's built-in pseudo-random generator does not guarantee stability between runtime versions, and as such is not
 * suitable for Stego's multiplatform and backwards compatible requirements.
 *
 * This class implements a pseudo-random number generator using the _Xoshiro256++_ algorithm introduced in 2019 by
 * David Blackman and Sebastiano Vigna, adapted from the original Public Domain
 * [source code](https://prng.di.unimi.it/xoshiro256plusplus.c).
 */
internal class Xoshiro256PlusPlus(
    private var s0: Long,
    private var s1: Long,
    private var s2: Long,
    private var s3: Long,
) : Random() {

    @Suppress("MagicNumber")
    private fun next(): Long {
        val result = (s0 + s3).rotateLeft(23) + s0
        val t = s1 shl 17
        s2 = s2 xor s0
        s3 = s3 xor s1
        s1 = s1 xor s2
        s0 = s0 xor s3
        s2 = s2 xor t
        s3 = s3.rotateLeft(45)
        return result
    }

    override fun nextBits(bitCount: Int): Int = next().ushr(Long.SIZE_BITS - bitCount).toInt()
}
