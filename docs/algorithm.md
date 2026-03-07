# Steko Algorithm Specification

This document describes the implementation steps in the Steko algorithm.

The algorithm is mostly symmetric, so each step will describe both encoding and decoding variants.

## 🏞️ Image Data

The algorithm is image format agnostic, provided it losslessly encodes pixel data.
Steko itself operates on the uncompressed, raw array of pixel values in RGBA format _(32-bit)_.

## 🔑 The Key

To begin, a 256-bit secret key is required, which will be used for multiple purposes.
The first 32 bits are interpreted as a bitmask for interpreting pixel data: set bits will indicate the payload bits,
while unset bits will keep the original values from the carrier image. At least one bit _MUST_ be enabled, for obvious
reasons. This is a configuration of sorts, a trade-off between using fewer bits will yield better image quality, and
using more to increase payload capacity, and also choosing which channels to use, leveraging the color profile of the
carrier image.

The rest of the key should be random, and is recommended to generate a new key for each important encoding in order to
maximize security.
However, for less critical applications, a key derivation function _MAY_ be used to translate from a human-friendly
passphrase.
In the reference implementation, the remaining bits of the _SHA256_ digest are used.

**Example:**

This is an example key, in hexadecimal:

`01010100b2b3b86ff88ef6c490628285f482af15ddcb29541f94bcf526a3f6c7`

Where:
 - The **bitmask** is `01010100`, meaning it will encode in the least significant bit (LSB) of the R, G, and B color
   channels, while not touching the alpha channel.
 - The rest of the bits could be random, but here they are the trailing characters of the SHA of the satirical password
   `hunter2`.

## 🔀 (Un-)Shuffling

Steko does not read pixels sequentially, rather it uses a randomized order obtained by reordering its pixels using the 
[modern Fisher-Yates shuffle](https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle).
For a deterministic, environment-agnostic PRNG implementation,
[Xoshiro256++](https://prng.di.unimi.it/xoshiro256plusplus.c) is used, as the key fits nicely as its 256-bit seed.

This ordering shall be used for both reading and writing.
The image itself is not shuffled, instead a remapping is done by applying the shuffling on an array of the pixels'
indices. 


**Example:**

```
Colors  |   R  B  G  Y  M                 Y  M  R  G  B
Indices | [ 0, 1, 2, 3, 4 ] ==> key ==> [ 3, 4, 0, 2, 1 ]`
``` 

To get the third pixel, instead of `pixels[2]`, which would've been **G**reen, it is now `pixels[reorder[2]]`, which is
`pixels[0]`, meaning **R**ed.

## 😷 Bit-Masking

To hide the payload, bit-masking is used to encode / decode payload bits in the pixel color data.
The pixels, read in the shuffled order, are combined with the bitmask:

- When writing, payload bits overwrite pixel data according to the bitmask.
- When reading, pixel data not matching the bitmask is discarded, and the remaining bits are read in sequence.

**Example:**

```
Pixels  | 01101000 01010010 10101110 10110001 | 11001001 10010101 00101010 10110111 | 
Bitmas  | 00000001 00000011 00000100 00000000 | 00000001 00000011 00000100 00000000 |
Payload |        0       10      1            |        1       01      0            |
```

Applying the `0x01030400` bitmask on the data above, which has four set bits, requires reading two pixels to obtain the
first byte of the payload, `01011010` _(here, UTF-8 `Z`)_.

## 🔍 Header

The first 8 bytes constitute a header, interpreted as two 32-bit values. When writing, these are prepended to the
actual payload.

The first value is a **challenge** used as a pre-flight check, to determine whether decoding the rest of the payload isn't a
waste of time.
If the value found in the image does not match the one expected from the key, then the image does not contain any
payload for that key.
To calculate the challenge, the _SHA256_ of the key is split into eight 32-bit numbers, XOR-ed together.

The second value is the **length** of the payload, in bytes.
Only this amount should be extracted from the pixel data.
This value can be trusted since it is very unlikely that by shuffling the pixels randomly will yield this magic 
checksum value associated with the key.

The length is a **signed integer**, and only its _absolute value_ represents the length.
The sign bit is a deserialization hint, if it is set, the payload is interpreted as _"raw"_ data instead of the
default format _(described later)_. 

**Example:**

Given the header `8e5c125c fff99227`:

- The expected challenge is `8e5c125c`.
- The payload is `421337` bytes long.
- The payload contains raw data.

Validating the challenge:
``` 
Key       | 01010100   b2b3b86f   f88ef6c4   90628285   f482af15   ddcb2954   1f94bcf5   26a3f6c7 
Digest    | e7f328da ^ 4221d750 ^ 8854aca5 ^ 495c4276 ^ e260b525 ^ f85b7627 ^ cbc23f4a ^ 3b7fff4d
Challenge |                                                                            = 8e5c125c ✔ ️       
```

## 🔐 Encryption

The payload is encrypted with the entire key using **AES-GCM-256** before being written.
As it is an authenticated cipher, data integrity is validated upon decryption, in case the header checks happened to be
a mathematical coincidence.

## 📦 Payload

Steko offers a standard payload shape to facilitate working with messages and files.
As mentioned in the header section, a bit indicates whether this format is used:

- If the bit was `1`, then the payload has no particular format and should be read as a literal byte string.
  This allows users to hide custom data and use their own serialization logic.
- If the bit was `0`, then the standard payload is used.

The standard payload consists of either a message, a list of file attachments, or both.
Attachments are a simple dictionary emulating a flat mini-filesystem, associating the filename to its contents.
The following validations are in place:

- File names may not be empty, or padded with whitespace.
- File names must be at most 255 bytes _(not necessarily characters because UTF-8)_ long.
- File names must be simple names, and contain no paths or illegal characters (`/` and `:`).
- File contents cannot be empty.

This structure is serialized as a CBOR array, using definite length encoding and explicit nulls.
The payload shall have an object tag of `0x53544547` _("STEG" in ASCII)_ for extra disambiguity.

**Example:**

A payload containing a message and two files can look like this:

```json5
{
  "message": "Which pill will you take, Neo?",
  "attachments": {
    "blue.txt": "Wake Up",
    "red.txt": "Stay in Wonderland" 
  }
}
```

And it would be encoded as:

```
da53 5445 4782 781e 5768 6963 6820 7069
6c6c 2077 696c 6c20 796f 7520 7461 6b65
2c20 4e65 6f3f a268 626c 7565 2e74 7874
4757 616b 6520 5570 6772 6564 2e74 7874
5253 7461 7920 696e 2057 6f6e 6465 726c
616e 64
```

CBOR visualization:

```
DA 53544547                   # tag(1398031687) STEG
   82                         # array(2)
      78 1E                   # text(30)
         5768<...>6F3F        # "Which pill will you take, Neo?"
      A2                      # map(2)
         68                   # text(8)
            626C75652E747874  # "blue.txt"
         47                   # bytes(7)
            57616B65205570    # "Wake Up"
         67                   # text(7)
            7265642E747874    # "red.txt"
         52                   # bytes(18)
            5374<...>6E64     # "Stay in Wonderland"
```

## 📝 Recap

A short recap of the algorithm steps.

**Hiding:**

1. Choose a bitmask and generate a key.
2. Encrypt the payload with the key.
3. Calculate the key challenge, the ciphertext length, and the payload type.
4. Hash the key to get a seed for the PRNG.
5. Determine pixel processing order using the seeded random source.
6. Write the header and ciphertext according to that order and bitmask.

**Showing:**

1. Provide the key.
2. Hash the key to get a seed for the PRNG.
3. Determine pixel processing order using the seeded random source.
4. Read the first 8 bytes of the pixel order using the bitmask.
5. Validate the challenge, otherwise stop.
6. Read the remaining length of the payload according to the header value.
7. Decrypt the payload with the key.
8. Perform any further deserialization as needed.
