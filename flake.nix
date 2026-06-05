{
  description = "doughnut development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            permittedInsecurePackages = [];
            substituteOnDestination = true;
            # Use specific binary caches
            binaryCaches = [
              "https://cache.nixos.org"
              "https://nixcache.reflex-frp.org"
            ];
            trusted-binary-caches = [
              "https://cache.nixos.org"
              "https://nixcache.reflex-frp.org"
            ];
          };
        };

        inherit (pkgs) stdenv lib;
        apple_sdk = pkgs.darwin.apple_sdk.frameworks;

        # Python 3.14: stable on nixos-26.05; pydantic-core in the lockfile does not build on 3.13 yet.
        python314 = pkgs.python314;
        pythonWithTools = python314.withPackages (ps: with ps; [ pip setuptools wheel ]);
        # Poetry runs pytest in installCheck; one test flakes on darwin (full stderr pipe).
        poetryPkg =
          (pkgs.poetry.override { python3 = python314; }).overrideAttrs (_: {
            doInstallCheck = false;
          });
        poetryPath = "${poetryPkg}/bin";
        pythonPackages = [ pythonWithTools poetryPkg ];

        # Node 26 removed bundled corepack, and nixpkgs has no corepack_26.
        # Provide pnpm directly instead: pin the exact version from package.json's
        # `packageManager`/`engines` (11.5.2) and run it under nodejs_26 so the
        # engine check passes. See scripts/dev_setup.sh (no longer calls corepack).
        #
        # To bump the version: set `version` below, set `hash = lib.fakeHash;`,
        # then run `nix build '.#devShells.aarch64-darwin.default' --no-link` and
        # copy the real hash from the "got:" line of the mismatch error into `hash`.
        # (Alternatively: `nix store prefetch-file <url>` prints the SRI hash directly.)
        pnpmPkg = (pkgs.pnpm.override { nodejs = pkgs.nodejs_26; }).overrideAttrs (_: rec {
          version = "11.5.2";
          src = pkgs.fetchurl {
            url = "https://registry.npmjs.org/pnpm/-/pnpm-${version}.tgz";
            hash = "sha256-dJ3FT709zenkFLquMsF3yoR3DT/NaciBbVea3D5qLJk=";
          };
        });

        basePackages = with pkgs; [
          zulu25
          nodejs_26
          pnpmPkg
          lsof
          fzf
          git-secret
          gitleaks
          jq
          mysql_jdbc
          mysql84
          mariadb.client
          redis
          yamllint
          nixfmt
          hclfmt
          trash-cli
          process-compose
        ];

        darwinPackages = with pkgs; lib.optionals stdenv.isDarwin [ sequelpro ];

        linuxPackages = with pkgs; lib.optionals (!stdenv.isDarwin) [
          psmisc
          xclip
          libuuid
        ];

        linuxCypressPackages = with pkgs; lib.optionals (!stdenv.isDarwin) [
          xorg.xorgserver
          xorg.xauth
          xorg.libX11
          xorg.libXcomposite
          xorg.libXdamage
          xorg.libXext
          xorg.libXfixes
          xorg.libXi
          xorg.libXrandr
          xorg.libXrender
          xorg.libXtst
          xorg.libXScrnSaver
          xorg.libxshmfence
          gtk3
          gtk2
          glib
          nss
          alsa-lib
          atk
          at-spi2-atk
          libdrm
          dbus
          expat
          mesa
          nspr
          udev
          cups
          pango
          cairo
        ];

        shellBuildInputs = basePackages ++ darwinPackages ++ linuxPackages ++ pythonPackages;

      in {
        devShells.default = pkgs.mkShell {
          name = "doughnut";
          nativeBuildInputs = with pkgs; [ autoPatchelfHook ];
          buildInputs = shellBuildInputs;

          # Force binary substitutes for the shell
          preferLocalBuild = false;
          allowSubstitutes = true;

          shellHook = ''
            export LD_LIBRARY_PATH="${lib.makeLibraryPath shellBuildInputs}''${LD_LIBRARY_PATH:+:}''$LD_LIBRARY_PATH"
            export PATH="${pythonWithTools}/bin:${poetryPath}:$PATH"
            source ./scripts/nix_shell_hook.sh "${pkgs.fzf}" "${pkgs.mysql84}" "${pkgs.redis}" "${poetryPath}"
          '';
        };
      });
}
