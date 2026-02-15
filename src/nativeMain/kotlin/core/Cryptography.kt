package io.github.jadarma.steggo.core

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.io.bytestring.ByteString
import kotlin.random.Random

/** Provides helper methods for cryptography-related functions. */
internal object Cryptography {

    private val provider = CryptographyProvider.Default
    private val aesGcm = provider.get(AES.GCM)
    private val aesKey = aesGcm.keyDecoder()
    private val aesKeySize = KEY_SIZE_BITS.bits
    private val aesTagSize = 128.bits

    /** A source of truly random numbers fit for cryptography purposes. */
    val secureRandom: Random = CryptographyRandom.Default

    const val KEY_SIZE_BITS: Int = 128
    const val KEY_SIZE_BYTES: Int = KEY_SIZE_BITS / UByte.SIZE_BITS

    /** Randomly generates a new AES encryption key. */
    fun generateKey(): ByteString =
        aesGcm
            .keyGenerator(aesKeySize)
            .generateKeyBlocking()
            .encodeToByteStringBlocking(AES.Key.Format.RAW)

    /** Tests the validity of an AES [key] in its binary form. */
    fun isKeyValid(key: ByteString): Boolean =
        runCatching {
            require(key.size == aesKeySize.inBytes) { "Key size mismatch."}
            aesKey.decodeFromByteStringBlocking(AES.Key.Format.RAW, key)
        }.isSuccess

    /** Encrypt the [data] with the [key]. */
    fun encrypt(data: ByteString, key: ByteString): ByteString =
        aesKey
            .decodeFromByteStringBlocking(AES.Key.Format.RAW, key)
            .cipher(aesTagSize)
            .encryptBlocking(data)

    /** Decrypt the [data] with the [key]. */
    fun decrypt(data: ByteString, key: ByteString): ByteString =
        aesKey
            .decodeFromByteStringBlocking(AES.Key.Format.RAW, key)
            .cipher(aesTagSize)
            .decryptBlocking(data)
}
