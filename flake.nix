{
  description = "doughnut development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
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

        # Python 3.12: stable on nixos-25.11; pydantic-core in the lockfile does not build on 3.13 yet.
        python312 = pkgs.python312;
        pythonWithTools = python312.withPackages (ps: with ps; [ pip setuptools wheel ]);
        poetryPkg = pkgs.poetry.override { python3 = python312; };
        poetryPath = "${poetryPkg}/bin";
        pythonPackages = [ pythonWithTools poetryPkg ];

        basePackages = with pkgs; [
          zulu25
          nodejs_24
          corepack_24
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
          nixfmt-classic
          hclfmt
          trash-cli
          process-compose
        ];

        darwinPackages = with pkgs; lib.optionals stdenv.isDarwin [ sequelpro ];

        linuxPackages = with pkgs; lib.optionals (!stdenv.isDarwin) [
          psmisc
          xclip
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

      in {
        devShells.default = pkgs.mkShell {
          name = "doughnut";
          nativeBuildInputs = with pkgs; [ autoPatchelfHook ];
          buildInputs = basePackages ++ darwinPackages ++ linuxPackages ++ pythonPackages;

          # Force binary substitutes for the shell
          preferLocalBuild = false;
          allowSubstitutes = true;

          shellHook = ''
            export PATH="${pythonWithTools}/bin:${poetryPath}:$PATH"
            source ./scripts/nix_shell_hook.sh "${pkgs.fzf}" "${pkgs.mysql84}" "${pkgs.redis}" "${poetryPath}"
          '';
        };
      });
}
