package io.github.jadarma.stego.core

import kotlinx.coroutines.runBlocking

/** Blocking variant of [Key.Companion.generate]. */
public fun Key.Companion.generateBlocking(passphrase: String): Key =
    runBlocking { Key.generate(passphrase) }

/** Blocking variant of [Image.hide]. */
public fun Image.hideBlocking(key: Key, payload: StegoPayload, noise: Boolean = true): Image =
    runBlocking { hide(key, payload, noise) }

/** Blocking variant of [Image.show]. */
public fun Image.showBlocking(key: Key): StegoPayload? =
    runBlocking { show(key) }

/** Blocking variant of [Image.show]. */
public fun <T : Any> Image.showBlocking(key: Key, convert: (ByteArray) -> T): T? =
    runBlocking { show(key, convert) }
