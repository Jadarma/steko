package io.github.jadarma.steggo.core

interface ImageHandler : ImageReader, ImageWriter

interface ImageReader {
    fun read(path: String): Image
}

interface ImageWriter {
    fun write(image: Image, path: String)
}
