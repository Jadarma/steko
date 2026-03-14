{
  description = "A simple and secure steganography CLI tool.";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-25.11";
  };

  outputs =
    inputs@{ self, nixpkgs, ... }:
    let
      pname = "steko";
      version = "0.1.0";
      repo = "https://github.com/Jadarma/steko";

      variants = {
        "aarch64-darwin" = "sha256:e1ac39518d257a40b4e9de178dcdb5f2dc7558984731f9d02d348979afe217ec";
        "aarch64-linux" = "sha256:0d299f788b01ad13100586e7198ca2d216ab805f828ff4e1db94b7db5bfb1374";
        "x86_64-linux" = "sha256:b1dc8267b0d530646c22e8cb21b08b37f4afd8d4c342fcbdabad64214d5d1ce8";
      };

      package =
        {
          lib,
          stdenv,
          fetchurl,
          autoPatchelfHook,
          zlib,
          libgcc,
        }:
        stdenv.mkDerivation {
          inherit pname version;

          src = fetchurl {
            url = "${repo}/releases/download/v${version}/steko-${stdenv.hostPlatform.system}.kexe";
            sha256 = variants."${stdenv.hostPlatform.system}";
          };

          dontUnpack = true;

          nativeBuildInputs = [
            autoPatchelfHook
          ];

          buildInputs = [
            zlib
            libgcc
          ];

          installPhase = ''
            mkdir -p $out/bin
            cp $src $out/bin/${pname}
            chmod +x $out/bin/${pname}
          '';

          meta = with lib; {
            description = "A simple and secure steganography CLI tool.";
            homepage = repo;
            downloadPage = "${repo}/releases/tag/v${version}";
            changelog = "${repo}/releases/tag/v${version}";
            license = lib.licenses.gpl3Plus;
            mainProgram = pname;
            platforms = map lib.systems.parse.mkSystemFromString (builtins.attrNames variants);
            sourceProvenance = [ lib.sourceTypes.binaryNativeCode ];
          };
        };
    in
    rec {
      overlays.default = final: prev: { steko = final.callPackage package { }; };

      packages = builtins.mapAttrs (system: _: {
        default =
          (import nixpkgs {
            inherit system;
            overlays = [ overlays.default ];
          }).steko;
      }) variants;

      nixosModules.default = {
        nixpkgs.overlays = [ overlays.default ];
      };

      darwinModules.default = {
        nixpkgs.overlays = [ overlays.default ];
      };
    };
}
