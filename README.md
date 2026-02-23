# Stego

A simple and secure steganography tool using AES, the LSB method and pseudorandom pixel order to hide payloads in
lossless image formats.

## 📜 Documentation

- [**CLI User Guide**](docs/cli_userguide.md) - Describes the CLI arguments and exemplifies common use-cases
- [**Algorithm Specification**](docs/algorithm.md) - Describes implementation steps.
- [**Security**](docs/security.md) - Comments on robustness.

## 📚 Lore 

**When did this start?**

The first time I implemented this was back in 2017 for my bachelor's thesis.
Back then it was a Windows GUI app written in C# and WPF.
I've since learned my lesson, so this is an homage to the memory, and a rewrite as a Kotlin Native CLI that does *not*
support Windows.

**Why the name?**

I found no clever pun with `kt` in it so I reused the original codename I gave it in 2017.
Unfortunately no clever pun here, I just needed a short alias, I used the first convenient thing that popped to
mind, this is a shortening of "steganography".
It has nothing to with dinosaurs or golang.

## ⚖️ License

Copyright © 2026 Dan Cristian Cîmpianu

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
 