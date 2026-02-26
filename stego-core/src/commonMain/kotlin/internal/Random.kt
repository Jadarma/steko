package io.github.jadarma.stego.core.internal

import dev.whyoleg.cryptography.CryptographySystem
import dev.whyoleg.cryptography.algorithms.SHA256
import io.github.jadarma.stego.core.Image
import io.github.jadarma.stego.core.Key
import kotlinx.io.Buffer
import kotlin.random.Random

/**
 * Kotlin's built-in pseudo-random generator does not guarantee stability between runtime versions, and as such is not
 * suitable for Stego's multiplatform and backwards compatible requirements.
 *
 * This class implements a pseudo-random number generator using the _Xoshiro256++_ algorithm introduced in 2019 by
 * David Blackman and Sebastiano Vigna, adapted from the original Public Domain
 * [source code](https://prng.di.unimi.it/xoshiro256plusplus.c).
 */
private class Xoshiro256PlusPlus(
    private var s0: Long,
    private var s1: Long,
    private var s2: Long,
    private var s3: Long,
) : Random() {

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

    override fun nextBits(bitCount: Int): Int = next().ushr(64 - bitCount).toInt()
}

/**
 * Create a pseudo-random number generator from this key as a seed.
 * Even though it would be very hard to determine the specific order of a few pixels by analyzing the carrier image,
 * since Xoshiro is not cryptographically secure, it is vulnerable to state reconstruction.
 * As a preventive measure, the seed is taken from the hash of the key, and not the key itself.
 */
internal fun Key.toRandom(): Random {
    val sha256 = CryptographySystem.getDefaultProvider().get(SHA256).hasher()
    val digest = sha256.hashBlocking(this.toByteArray())
    return Buffer().use { buffer ->
        buffer.write(digest)
        Xoshiro256PlusPlus(
            s0 = buffer.readLong(),
            s1 = buffer.readLong(),
            s2 = buffer.readLong(),
            s3 = buffer.readLong(),
        )
    }
}

/**
 * Using the [key] as a seed, shuffle the pixel order of this image.
 * Returns an array the same size as the image, containing the indexes of pixels to read in a pseudo-random order.
 * Based on the [Fisher–Yates shuffle](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm).
 */
internal fun Image.shufflePixels(key: Key): IntArray {
    val random = key.toRandom()
    val indices = IntArray(pixels.size) { it }
    for (i in indices.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = indices[i]
        indices[i] = indices[j]
        indices[j] = tmp
    }
    return indices
}
