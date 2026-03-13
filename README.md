# Steko

[![GitHub Release](https://img.shields.io/github/v/release/Jadarma/steko?include_prereleases&sort=semver&display_name=release&style=flat-square&logo=gitlfs&logoColor=%23F05032&label=Release&labelColor=%23181717&color=%23F05032)](https://github.com/Jadarma/steko/releases)
[![Flake](https://img.shields.io/badge/nix-Flake-%235277C3?style=flat-square&logo=nixos&label=Nix&labelColor=%23181717)](docs/installation.md#-nix)
[![GitHub License](https://img.shields.io/github/license/Jadarma/steko?style=flat-square&logo=opensourceinitiative&logoColor=%233DA639&label=License&labelColor=%23181717&color=%233DA639)](LICENSE.md)

A simple and secure steganography tool using AES, the LSB method and pseudorandom pixel order to hide payloads in
lossless image formats.

![Asciinema Demo Gif](docs/demo.gif)

## ✨ Features

- **Mandatory Encryption** - Your secrets are not only hidden, they are encrypted.
- **Stored in Scrambled Order** - Makes detection even harder, and brute force virtually impossible.
- **Customizable Bitmask** - Allows for multiple payloads, better stealth in some color contexts, and variable capacity.
- **Designed for Lossless Formats** - Images can be converted between any present or future format and still work.
- **Single Binary** - With convenient CLI usage for Linux and macOS.

## 📜 Documentation

- [**Installation**](docs/installation.md) - Instructions for downloading the binaries.
- [**CLI User Guide**](docs/cli_userguide.md) - Describes the CLI arguments and exemplifies common use-cases
- [**Algorithm Specification**](docs/algorithm.md) - Describes implementation steps.
- [**Security**](docs/security_analysis.md) - Comments on robustness.
- [**Party Tricks**](docs/party_tricks.md) - Examples of bonus niche uses.
- [**F.A.Q.**](docs/faq.md) - Frequently asked questions.

## 👉 Example

<img width="340px" align="left" style="margin-right: 2em" src="docs/example.png" alt="Shakespeare hiding behind Mona Lisa">

This image of _Mona Lisa_ also contains within it the complete works of William Shakespeare.
_(Talk about multiculturalism!)_

- Original Image:
  [Mona Lisa, by Leonardo da Vinci](https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg/1920px-Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg),
  from [C2RMF via Wikimedia](https://commons.wikimedia.org/wiki/File:Mona_Lisa,_by_Leonardo_da_Vinci,_from_C2RMF_retouched.jpg)
  _(converted to PNG, 1920 x 2861 pixels, 11.8 MiB)_
- Payload: [The Complete Works of William Shakespeare, EPUB3 by Project Gutenberg](https://www.gutenberg.org/ebooks/100), _(2.8 MiB)_
- Bitmask: 2R/1G/2B, 5 / 24 total bits overwritten.
- Modified Image: [Full-Size Here](docs/example.png), _(12.5 MiB)_

Try it out:

```shell
steko show -o /tmp docs/example.png <<< '03010300f3e33a394b6f4643debff21a571be07326a381501c2b62105a69034d' 
```

Should produce:

```text
To hide or not to hide, that is the question!
─────────────────────────────────────────────────
Extracted Attachments (1):
 • shakespeare.epub (2MiB)
```

<div style="clear: both;"></div>

## ⚖️ License

Copyright © 2026 Dan Cîmpianu

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 