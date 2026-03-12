# Installation

This document describes how to download and install Steko on your system.

## 📦 Pre-built Binaries

Steko pre-built binaries are available on the [GitHub Releases](https://github.com/Jadarma/steko/releases) page.
Simply download it and add it to your path:

```shell
VERSION='0.1.0'
PLATFORM='x86_64-linux' # or 'aarch64-linux`, or `aarch64-darwin`
INSTALLDIR='/usr/local/bin' # or somewhere else on your $PATH.
curl "https://github.com/Jadarma/steko/releases/download/v$VERSION/steko-$PLATFORM.kexe" -o steko
chmod +x steko
sha256sum steko # Compare to release assets!
sudo mv steko "$INSTALLDIR/steko"
```

## 🛠️ From Source

To build from source, a JDK installation needs to be present.
Exact version doesn't matter, Gradle will download a specific one to run the project with automatically.

From the repository root, run the task appropriate for your platform:

```shell
./gradlew :steko-cli:linkReleaseExecutableLinuxX64
./gradlew :steko-cli:linkReleaseExecutableLinuxArm64
./gradlew :steko-cli:linkReleaseExecutableMacosArm64
```

The binaries are available in `./steko-cli/build/bin/<platform>/releaseExecutable`.

## ❄️ Nix

This repository is also available as a flake for use with [Nix](https://nix.dev/install-nix#install-nix),
[NixOS](https://nixos.org/) or [nix-darwin](https://github.com/nix-darwin/nix-darwin).

You can temporarily add Steko to your shell and play around:

```shell
nix shell https://github.com/Jadarma/steko
```

You can also execute the binary directly for one run, without installation:

```shell 
nix run https://github.com/Jadarma/steko -- 'args go here'
```

For permanent installation, add the flake to your inputs and load the respective module:

```nix
{
    inputs = {
        nixpkgs = {
            url = "github:NixOS/nixpkgs/nixpkgs-unstable";
        };
        nix-darwin = {
            url = "github:nix-darwin/nix-darwin/master";
            inputs.nixpkgs.follows = "nixpkgs";     
        };
        steko = { 
            url = "github:Jadarma/steko";
            inputs.nixpkgs.follows = "nixpkgs";
        };
    };
    
    outputs = inputs@{ self, nixpkgs, nix-darwin, steko, ... }: {
        nixosConfigurations.default = nixpkgs.lib.nixosSystem {
            system = "x86_64-linux"; # or aarch64-linux
            specialArgs = inputs;
            modules = [ 
                steko.nixosModules.default
                ./nixOSConfiguration.nix
            ];
        };
        
        darwinConfigurations.default = nix-darwin.lib.darwinSystem {
            specialArgs = inputs;
            modules = [ 
                steko.darwinModules.default
                ./darwinConfiguration.nix 
            ];
        };
    };
}
```

Then, from your `configuration.nix` you can declare it like any other package:

```nix
{ pkgs, ... }: {
    environment.systemPackages = with pkgs; [
        steko
    ];
}
```
