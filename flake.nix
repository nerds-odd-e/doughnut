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
        cursorDevEnv = builtins.getEnv "CURSOR_DEV";
        pythonDevEnv = builtins.getEnv "PYTHON_DEV";
        pythonDev = pythonDevEnv == "true";
        poetryPath = "${pkgs.poetry}/bin";
        pythonPackages = if pythonDev then [
          pkgs.python314
          pkgs.poetry
          pkgs.python314Packages.pip
          pkgs.python314Packages.setuptools
          pkgs.python314Packages.wheel
        ] else [];

        basePackages = with pkgs; [
          zulu25
          nodejs_24
          corepack_24
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
          buildInputs = basePackages ++ darwinPackages ++ linuxPackages;

          # Force binary substitutes for the shell
          preferLocalBuild = false;
          allowSubstitutes = true;

          shellHook = ''
            source ./scripts/nix_shell_hook.sh "${pkgs.fzf}" "${pkgs.mysql84}" "${pkgs.redis}" "${poetryPath}"
          '';
        };
      });
}
