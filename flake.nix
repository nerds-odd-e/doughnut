{
  description = "doughnut development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
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
          };
          overlays = [
            (final: prev: {
              boost = prev.boost.override {
                enableShared = true;
                enableStatic = true;
                extraB2Flags = [ "--without-stacktrace" ];
              };
            })
          ];
        };

        inherit (pkgs) stdenv lib;
        apple_sdk = pkgs.darwin.apple_sdk.frameworks;

        # Env var to check whether to run more nix init steps (helps speeds up CURSOR agent mode)
        cursorDevEnv = builtins.getEnv "CURSOR_DEV";
        # Env var to conditionally include Python packages
        pythonDevEnv = builtins.getEnv "PYTHON_DEV";
        pythonDev = pythonDevEnv == "true";
        poetryPath = "${pkgs.poetry}/bin";
        pythonPackages = if pythonDev then [
          pkgs.python313
          pkgs.poetry
          pkgs.python313Packages.pip
          pkgs.python313Packages.setuptools
          pkgs.python313Packages.wheel
        ] else [];

        basePackages = with pkgs; [
          zulu23
          nodejs_23
          corepack_23
          git
          git-secret
          gitleaks
          jq
          libmysqlclient
          mysql80
          mysql-client
          mysql_jdbc
          yamllint
          nixfmt-classic
          hclfmt
          fzf
        ];

        darwinPackages = with pkgs; lib.optionals stdenv.isDarwin [ sequelpro ];

        linuxPackages = with pkgs; lib.optionals (!stdenv.isDarwin) [
          psmisc
          xclip
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
          nativeBuildInputs = with pkgs;
            [
              autoPatchelfHook
            ];
          buildInputs = with pkgs;
            basePackages
            ++ pythonPackages
            ++ darwinPackages
            ++ linuxPackages
            ++ lib.optionals stdenv.isDarwin [ sequelpro ]
            ++ lib.optionals (!stdenv.isDarwin) [
              psmisc
              xclip
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

          shellHook = ''
            # Source helper scripts
            source ./scripts/shell_setup.sh
            source ./scripts/dev_setup.sh

            # Initialize basic shell environment
            setup_shell
            setup_logging
            setup_fzf "${pkgs.fzf}"

            # Add git push script alias
            alias g='./scripts/git_push.sh'

            # Deactivate nvm if exists
            deactivate_nvm

            # Setup core environment
            setup_env_vars

            # Setup MySQL environment
            setup_mysql_env "${pkgs.mysql80}"

            # Add Python to PATH if enabled
            if [ "''${PYTHON_DEV:-}" = "true" ] && command -v python >/dev/null 2>&1; then
              export PYTHON_PATH="$(dirname $(dirname $(readlink -f $(which python))))"
              export PATH="${poetryPath}:$PYTHON_PATH/bin:$PATH"

              # Setup Python environment if enabled
              setup_python "${poetryPath}"
            fi

            echo "CURSOR_DEV: ''${CURSOR_DEV:-}"
            if [ "''${CURSOR_DEV:-}" != "true" ]; then
              # Setup development environment
              setup_pnpm_and_biome
              setup_cypress

              # Start MySQL if not running
              if ! lsof -i :3309 -sTCP:LISTEN >/dev/null 2>&1; then
                log "Starting MySQL server..."
                ./scripts/init_mysql.sh
                check_mysql_ready
              else
                log "MySQL is running on port 3309 & ready to go! 🏃"
              fi
            fi

            # Print environment information
            print_env_info

            log "Environment setup complete! 🎉"
            return 0
          '';
        };
      });
}
