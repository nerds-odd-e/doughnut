{
  description = "donut development environment";

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
        # The project venv is built against 3.14 (see pyproject `python = "^3.14"`);
        # dev_setup.sh runs `poetry env use python3.14` before `poetry install`.
        python314 = pkgs.python314;
        pythonWithTools = python314.withPackages (ps: with ps; [ pip setuptools wheel ]);
        # Use upstream poetry (built against the branch-default python 3.13) so it
        # is a cached binary substitute. Poetry only manages the venv — it does not
        # need to *run* on 3.14, and the 3.14 interpreter above is on PATH for it to
        # create the project venv. Overriding python3 here would force a from-source
        # build of poetry's entire closure (incl. the flaky `sh` test suite).
        poetryPkg = pkgs.poetry;
        poetryPath = "${poetryPkg}/bin";
        pythonPackages = [ pythonWithTools poetryPkg ];

        # Node 26 removed bundled corepack, and nixpkgs has no corepack_26.
        # Provide pnpm directly instead: pin the exact version from package.json's
        # `packageManager`/`engines` (12.12.0) and run it under nodejs-slim_26 so the
        # engine check passes. See scripts/dev_setup.sh (no longer calls corepack).
        #
        # To bump the version: follow .agents/skills/pnpm-12-upgrade/SKILL.md.
        # pnpm 12's npm package is a launcher; the real program is a native binary
        # in @pnpm/exe.<target>, which the sandboxed build cannot download itself.
        pnpmExeTarget = {
          aarch64-darwin = "darwin-arm64";
          x86_64-darwin = "darwin-x64";
          x86_64-linux = "linux-x64";
          aarch64-linux = "linux-arm64";
        }.${system};
        pnpmExeHashes = {
          darwin-arm64 = "sha256-gbPoGkEGeaR3bbLjUISTBFm8oCdHfdS2mgtNtKVt9uY=";
          darwin-x64 = "sha256-+Z+GmeDuCiTPXsdCeSyB3sgswPNczwU4vdmGGj9rlUQ=";
          linux-x64 = "sha256-m+xnxulX+K1jmPvKJvpb0tv2PlVYUWDmMk11oZbIFLw=";
          linux-arm64 = "sha256-kGGL1j4yPjGidnF8XASNl7UJ/aoQlSglYQKqIirBDnE=";
        };
        pnpmPkg = (pkgs.pnpm.override { nodejs-slim = pkgs.nodejs-slim_26; }).overrideAttrs (old: rec {
          version = "12.12.0";
          src = pkgs.fetchurl {
            url = "https://registry.npmjs.org/pnpm/-/pnpm-${version}.tgz";
            hash = "sha256-BVWXIjo8Kz5DGY34kUK1rkHDgm0cnPOs1dgQhebVwEo=";
          };
          exeSrc = pkgs.fetchurl {
            url = "https://registry.npmjs.org/@pnpm/exe.${pnpmExeTarget}/-/exe.${pnpmExeTarget}-${version}.tgz";
            hash = pnpmExeHashes.${pnpmExeTarget};
          };
          nativeBuildInputs = old.nativeBuildInputs ++ lib.optionals stdenv.isLinux [ pkgs.autoPatchelfHook ];
          buildInputs = old.buildInputs ++ lib.optionals stdenv.isLinux [ stdenv.cc.cc.lib ];
          postUnpack = ''
            tar xzf $exeSrc -O package/pnpm > package/pnpm
            chmod +x package/pnpm
          '';
          installPhase = ''
            runHook preInstall
            install -d $out/{bin,libexec}
            cp -R . $out/libexec/pnpm
            for b in pnpm pn pnpx pnx; do ln -s $out/libexec/pnpm/$b $out/bin/$b; done
            runHook postInstall
          '';
          postInstall = lib.replaceStrings [ "node $out/bin/pnpm" ] [ "$out/bin/pnpm" ] old.postInstall;
        });

        basePackages = with pkgs; [
          zulu25
          nodejs_26
          pnpmPkg
          lsof
          fzf
          # A real git: Apple's /usr/bin/git shim costs ~17ms per call under this
          # shell's DEVELOPER_DIR/SDKROOT, and the CLI and git-lfs spawn git often.
          git
          git-secret
          git-lfs
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
          name = "donut";
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
