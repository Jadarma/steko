package io.github.jadarma.stego.core.test

import com.goncalossilva.resources.Resource
import io.github.jadarma.stego.core.Image

/** Load an image from a [resource]. */
fun Image.Companion.decodeFromRgba(resource: Resource, size: Pair<Int, Int>? = null): Image =
    decodeFromRgba(resource.readBytes(), size)
