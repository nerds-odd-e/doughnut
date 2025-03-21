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

        # Check for PYTHON_DEV environment variable
        pythonDev = builtins.getEnv "PYTHON_DEV" == "true";
        pythonPackages = if pythonDev then [ pkgs.python313 pkgs.poetry ] else [];
      in {
        devShells.default = pkgs.mkShell {
          name = "doughnut";
          nativeBuildInputs = with pkgs;
            [
              autoPatchelfHook
            ];
          buildInputs = with pkgs;
            [
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
            ]
            ++ pythonPackages
            ++ lib.optionals stdenv.isDarwin [ sequelpro ]
            ++ lib.optionals (!stdenv.isDarwin) [
              psmisc
              xclip
              xorg.libX11
              xorg.libXcomposite
              xorg.libXdamage
              xorg.libXext
              xorg.libXfixes
              xorg.libXi
              xorg.libXrandr
              xorg.libXrender
              xorg.libXtst
              gtk3
              nss
              alsa-lib
              at-spi2-atk
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
            # Make script compatible with both bash and zsh
            # Set core environment variables
            if [ -n "''${ZSH_VERSION:-}" ]; then
              emulate -L bash
              export PS1="(nix)''${PS1:-%# }"
            else
              export PS1="(nix)''${PS1:-$ }"
            fi

            # Define and export logging function
            export LOG_FUNCTION='log() { echo "[$(date +"%Y-%m-%d %H:%M:%S")] $*"; }'
            eval "$LOG_FUNCTION"

            # Define error handler
            export ERROR_HANDLER='handle_error() { local error_code="$2"; log "Warning: Command exited with status $error_code"; return 0; }'
            eval "$ERROR_HANDLER"
            trap 'handle_error "0" "$?"' ERR

            # Configure fzf
            export FZF_DEFAULT_OPTS="--height 40% --layout=reverse --border"
            if [ -n "''${ZSH_VERSION:-}" ]; then
              if [ -e "${pkgs.fzf}/share/fzf/key-bindings.zsh" ]; then
                source ${pkgs.fzf}/share/fzf/key-bindings.zsh
              fi
              if [ -e "${pkgs.fzf}/share/fzf/completion.zsh" ]; then
                source ${pkgs.fzf}/share/fzf/completion.zsh
              fi
            else
              if [ -e "${pkgs.fzf}/share/fzf/key-bindings.bash" ]; then
                source ${pkgs.fzf}/share/fzf/key-bindings.bash
              fi
              if [ -e "${pkgs.fzf}/share/fzf/completion.bash" ]; then
                source ${pkgs.fzf}/share/fzf/completion.bash
              fi
            fi

            # Add git push script alias
            alias g='./scripts/git_push.sh'

            # Deactivate nvm if exists
            command -v nvm >/dev/null 2>&1 && { nvm deactivate; }

            # General settings
            export LANG="en_US.UTF-8"
            export SOURCE_REPO_NAME="''${PWD##*/}"

            # Export core paths first
            export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which javac))))"
            export NODE_PATH="$(dirname $(dirname $(readlink -f $(which node))))"
            export PNPM_HOME="$(dirname $(dirname $(readlink -f $(which pnpm))))"
            # Only set PYTHON_PATH if Python is available
            if command -v python >/dev/null 2>&1; then
              export PYTHON_PATH="$(dirname $(dirname $(readlink -f $(which python))))"
              export PATH=$JAVA_HOME/bin:$NODE_PATH/bin:$PNPM_HOME/bin:$PYTHON_PATH/bin:$PATH
            else
              export PATH=$JAVA_HOME/bin:$NODE_PATH/bin:$PNPM_HOME/bin:$PATH
            fi

            # Export MySQL configuration
            export MYSQL_BASEDIR="${pkgs.mysql80}"
            export MYSQL_HOME="''${PWD}/mysql"
            export MYSQL_DATADIR="''${MYSQL_HOME}/data"
            export MYSQL_UNIX_SOCKET="''${MYSQL_HOME}/mysql.sock"
            export MYSQLX_UNIX_SOCKET="''${MYSQL_HOME}/mysqlx.sock"
            export MYSQL_PID_FILE="''${MYSQL_HOME}/mysql.pid"
            export MYSQL_TCP_PORT="3309"
            export MYSQLX_TCP_PORT="33090"
            export MYSQL_LOG_FILE="''${MYSQL_HOME}/mysql.log"

            # Configure pnpm and start Biome
            log "Setting up PNPM..."
            corepack prepare pnpm@10.6.5 --activate
            corepack use pnpm@10.6.5
            pnpm --frozen-lockfile recursive install

            # Restart biome daemon
            if [[ -d "/etc/nixos" ]]; then
              BIOME_VERSION=$(node -p "require('./package.json').devDependencies.biome" 2>/dev/null || echo "")
              pgrep biome | xargs kill -9
              autoPatchelf "./node_modules/.pnpm/@biomejs+cli-linux-x64@''${BIOME_VERSION}/node_modules/@biomejs/cli-linux-x64"
            fi
            pnpm biome stop
            pnpm biome start

            # Setup Cypress with specific version
            (
              log "Setting up Cypress..."
              CYPRESS_VERSION=$(node -p "require('./package.json').devDependencies.cypress" 2>/dev/null || echo "")
              if [ -n "$CYPRESS_VERSION" ]; then
                if [[ ! -d "$HOME/.cache/Cypress/''${CYPRESS_VERSION//\"}" ]] && [[ ! -d "$HOME/Library/Caches/Cypress/''${CYPRESS_VERSION//\"}" ]]; then
                  pnpx cypress install --version ''${CYPRESS_VERSION//\"} --force
                fi
              fi
            )
            if [[ -d "/etc/nixos" ]]; then
              autoPatchelf "''${HOME}/.cache/Cypress/''${CYPRESS_VERSION}/Cypress/"
            fi
            #export CYPRESS_CACHE_FOLDER="$PWD/.cypress-cache"

            # Start process-compose for MySQL only
            ./scripts/init_mysql.sh &

            # Setup Poetry if Python development is enabled
            if [ "${builtins.toString pythonDev}" = "true" ]; then
              log "Setting up Python development environment..."
              if command -v poetry >/dev/null 2>&1; then
                poetry --version
                # Configure poetry to create virtual environments in the project directory
                poetry config virtualenvs.in-project true
                # Initialize poetry if pyproject.toml doesn't exist
                if [ ! -f pyproject.toml ]; then
                  log "No pyproject.toml found. You can initialize a new Python project with 'poetry init'"
                else
                  log "Installing Python dependencies..."
                  poetry install
                fi
              fi
            fi

            cat << 'EOF'
            ╔════════════════════════════════════════════════════════════════════════════════════╗
            ║                         NIX DEVELOPMENT ENVIRONMENT                                ║
            ╚════════════════════════════════════════════════════════════════════════════════════╝
            EOF

            printf "\n%s\n" "🚀 Project: $SOURCE_REPO_NAME"
            printf "📦 Versions:\n"
            printf "  • Nix:    %s\n" "$(nix --version)"
            printf "  • Java:   %s\n" "$(javac --version)"
            printf "  • Node:   %s\n" "$(node --version)"
            printf "  • PNPM:   %s\n" "$(pnpm --version)"
            printf "  • Biome:  %s\n" "$(pnpm biome --version)"
            if command -v python >/dev/null 2>&1; then
              printf "  • Python: %s\n" "$(python --version)"
              if command -v poetry >/dev/null 2>&1; then
                printf "  • Poetry: %s\n" "$(poetry --version)"
              fi
            fi

            printf "\n📂 Paths:\n"
            printf "  • JAVA_HOME:     %s\n" "$JAVA_HOME"
            printf "  • NODE_PATH:     %s\n" "$NODE_PATH"
            printf "  • PNPM_HOME:     %s\n" "$PNPM_HOME"
            if [ -n "$PYTHON_PATH" ]; then
              printf "  • PYTHON_PATH:   %s\n" "$PYTHON_PATH"
            fi
            printf "  • MYSQL_HOME:    %s\n" "$MYSQL_HOME"
            printf "  • MYSQL_DATADIR: %s\n" "$MYSQL_DATADIR"
            printf "\n"

            log "Environment setup complete! 🎉"
            return 0
          '';
        };
      });
}
