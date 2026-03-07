# Steko CLI User Guide

This document describes the CLI arguments and exemplifies common use-cases.
Most of these are also documented in the built-in `--help` of the command. 

## 📥 Hiding

The `hide` subcommand is used to hide messages and file attachments into images.

The associated key is printed to _STDOUT_ and should be stored securely, you could:
- pipe it to the clipboard (` | wl-copy` on Linux, ` | pbcopy` on Mac) and store it in a password manager.
- redirect it to a file (`> secret.key`) that is stored securely _(and separately from the images, of course!)_.

### CLI Options

`steko hide [<options>] [<attachment>]...`

|          Option           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|:-------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|    `-i`, `--in=<path>`    | The path to the original image.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|   `-o`, `--out=<path>`    | The path where to write the modified image. _(Overwrites previous data!)_ The file extension must match that of the input image.                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|   `-e`, `--edit=<path>`   | Hides the payload in-place, reading the original image and writing it back to the same path. Doubles as explicit user consent, and is mutually exclusive with `--in` and `--out`.                                                                                                                                                                                                                                                                                                                                                                                    |
| `-m`, `--message=<text>`  | Hide the given plain-text string. Ideal for short memos or giving instructions for file attachments.                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|   `-p`, `--passphrase`    | Prompts for a passphrase instead of generating a random key. More convenient, but less secure. Prefer not overusing the same passphrase for important files.                                                                                                                                                                                                                                                                                                                                                                                                         |
| `-b`, `--bitmask=<value>` | An unsigned 32-bit mask _(given as hex or decimal)_ for an RGBA pixel value, set bits will overwrite the original image value with data from the payload. It is recommended to use the least significant bits to make it more stealthy and not use the alpha channel, especially if the original image doesn't make use of transparency. One can also use many or all bits, leading to chaos resembling glitch art. For obvious reasons, value cannot be zero. NOTE: When using a custom bitmask, the `--passphrase` option cannot be used! Default is `0x01010100`. |
|  `--noise`, `--no-noise`  | Choose whether to write random data in the pixels not used to encode the payload. The noise attempts to explain away imperfections caused by the payload bitmask as noise or compression artifacts in the carrier image, as well as make it more difficult to locate pixels of the payload when the original image is known. Enabled by default.                                                                                                                                                                                                                     |

The arguments are an optional list of files, which should be saved as attachments.

### Examples

**Hiding a simple text message.**

```shell
steko hide -e image.png -m 'Hello, World!' > secret.key
```

**Hiding entire files.**

```shell
steko hide -e image.png example.md launch.sh > secret.key
```

**Using a passphrase.**

```shell 
steko hide -e image.png -p -m 'Will prompt you for a password.'
```

**Saving a copy.**

```shell 
steko hide -i original.png -o modified.png -m 'Original left alone' > secret.key
```

## 📤 Showing

The `show` subcommand is used to extract a previously hidden payload.

The key is read from _STDIN_ and should be retrieved securely, you could:

- copy it from clipboard (`< wl-paste` on Linux, `< pbcopy` on Mac) after reading it from a password manager.
- redirect it from a file (`< secret.key`)

If the payload contains a message, it will be printed to _STDOUT_.
File attachments won't be extracted unless specified by an option.
The list of attachments and their sizes is also printed out when the `show` command is called from an interactive
terminal.

**NOTE:** _It is also possible the payload doesn't contain the default Steko metadata. In that case, the entire raw 
          payload will be printed to STDOUT._

### CLI Options

`steko hide [<options>] <image>`

|        Option         | Description                                                                                          |
|:---------------------:|:-----------------------------------------------------------------------------------------------------|
| `-p`, `--passphrase`  | Treats the input as a passphrase to derive from, instead of reading the actual key in hex format.    |
| `-o`-, `--out=<path>` | The directory to write the attachments to. The file(s) will be named according to it's own metadata. |

### Examples

**Preview a payload:**

```shell
steko show image.png < secret.key
```

**Extract all attachments:**

```shell
steko show -o /tmp image.png < secret.key
```

**Use a passphrase:**

```shell
steko show -p image.png
```

## 🖼️ Supported Formats

The `steko` command is minimal, and only directly supports PNG images.
However, since the algorithm is designed for lossless formats, these can be converted via an external application, like
[Image Magick](https://imagemagick.org), which allows far more encoding customization.

**Hiding Example:**

Any image can be used as an input _(even lossy formats like JPEG)_, by first converting it to PNG.
The resulting PNG can be converted into any other lossless format.
Note that some formats require special parameters to be passed to `magick` to ensure lossless encoding.
Please consult the relevant documentation and make test runs for decoding afterward.

```shell 
magick input.avif output.png
steko hide -e output.png -m 'Top Secret!' > secret.key
magick -define webp:lossless=true output.png output.webp
```

**Showing Example:**

To show the payload from any format, it needs to be converted to PNG again:

```shell 
magick output.webp output.png
steko show output.png < secret.key
```

**Using raw RGBA files.**

Steko can also work with raw pixel values in `.rgba` files instead of PNG to skip an extra encoding step:

```shell
magick input.avif output.rgba
steko hide -e output.rgba -m 'Top Secret!' > secret.key
IMG_SIZE="$(magick identify -ping -format "%[w]x%[h]" input.avif)"
magick -define webp:lossless=true -size "$IMG_SIZE" -depth 8 output.rgba output.webp
magick output.webp input.rgba
steko show input.rgba < secret.key
rm input.rgba output.rgba
```
