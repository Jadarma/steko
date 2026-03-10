# Frequently Asked Questions

### 🖼️ What images work best as carriers?

Ideally, original images that do not already exist on the internet work best, because it makes it harder to tell if they
have been altered.
Regarding contents, photos with natural grain lend themselves better because they camouflage the modified bits.
Good examples are nighttime photos where sensors often add noise, natural landscapes with vegetation that have a lot of
fine detail, or even deep-fried memes which look low-quality on purpose.

### 📦 How much can I store in a photo?

The payload capacity is dependent on the image size and bitmask used.
Steko itself adds a small but easily dismissible overhead.
The general formula is: `width * height * bitmaskBits / 8`.
The bitmask can be changed to cram in more at the cost of more quality loss.
Assuming the default LSB bitmask _(3 bits)_, here are some examples:

| Photo | Capacity |
|:-----:|:--------:|
| 1080p | 0.7 MiB  |
| 1440p | 1.3 MiB  |
|  4K   | 2.9 MiB  | 
| 12MP  | 4.2 MiB  |

### ➕ Why do my images get larger if the payload overwrites pixel data?

It is true that the pixel data itself is modified _(not appended)_, however you may notice larger file sizes of modified
versus original images.
The actual data stored is AES ciphertext, which looks random, and then shuffled across the pixels, again randomly.
The image's codec has to losslessly compress this randomness, but random data is harder to compress.
If more control is needed, Steko can export as raw `.rgba` for a specialized external application to encode.

### 🔍 Are these images detectable by existing scanners?

No.
Typical steganography detection methods look for magic patterns embedded in the binary, such as data inside padding,
EXIF, etc.
Steko is format-agnostic and only operates on the raw uncompressed image.
Scanners that look for LSB try to forcibly extract a payload and look for markers there, but usually it's done
sequentially.
Because Steko uses a variable bitmask and pixel shuffling, only signal analysis may be used to determine the existence
of a secret payload.
If best practices are used, the confidence of these heuristics can be greatly diminished.

### 🏷️ Why the name?
    
_"Steko"_ is a portmanteau of _"steganography"_ and _"Kotlin"_.
No clever pun here!

### 🚀 Why the prerelease status?

The core algorithm is feature complete and designed to be as minimalist as possible in order to ensure forward
compatibility: it only ensures encryption and integrity of the payload, which is arbitrary binary data.
It is expected it won't be changed in future versions.
However, some direct dependencies of the project are still considered experimental, so until then it will remain a
pre-release as well.
During this time, feedback is greatly appreciated.

### 🪟 Will Windows targets be supported?

No. Use WSL.
