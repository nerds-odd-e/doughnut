# To learn more about how to use Nix to configure your environment
# see: https://developers.google.com/idx/guides/customize-idx-env
{ pkgs, ... }: {
  # Which nixpkgs channel to use.
  channel = "stable-24.11"; # or "unstable"

  # Use https://search.nixos.org/packages to find packages
  packages = [
    pkgs.direnv
    pkgs.corepack_23
    pkgs.nodejs_23
    pkgs.zulu23
    pkgs.git
    pkgs.git-secret
    pkgs.libmysqlclient
    pkgs.mysql80
    pkgs.mysql-client
    pkgs.mysql_jdbc
    pkgs.bat
    pkgs.fzf
    pkgs.hclfmt
    pkgs.jq
    pkgs.lsd
    pkgs.nixfmt-classic
    pkgs.yamlint
    pkgs.psmisc
    pkgs.xclip
    pkgs.xorg.libX11
    pkgs.xorg.libXcomposite
    pkgs.xorg.libXdamage
    pkgs.xorg.libXext
    pkgs.xorg.libXfixes
    pkgs.xorg.libXi
    pkgs.xorg.libXrandr
    pkgs.xorg.libXrender
    pkgs.xorg.libXtst
    pkgs.xorg.libXScrnSaver
    pkgs.xorg.libxshmfence
    pkgs.gtk3
    pkgs.gtk2
    pkgs.glib
    pkgs.nss
    pkgs.atk
    pkgs.at-spi2-atk
    pkgs.libdrm
    pkgs.dbus
    pkgs.expat
    pkgs.mesa
    pkgs.nspr
    pkgs.udev
    pkgs.cups
    pkgs.pango
    pkgs.cairo
  ];

  # Sets environment variables in the workspace
  env = {};
  idx = {
    # Search for the extensions you want on https://open-vsx.org/ and use "publisher.id"
    extensions = [
      "vscodevim.vim"
    ];

    # Enable previews
    previews = {
      enable = true;
      previews = {
        # web = {
        #   # Example: run "npm run dev" with PORT set to IDX's defined port for previews,
        #   # and show it in IDX's web preview panel
        #   command = ["pnpm" "frontend:sut"];
        #   manager = "web";
        #   env = {
        #     # Environment variables to set for your server
        #     PORT = "$PORT";
        #   };
        # };
      };
    };

    # Workspace lifecycle hooks
    workspace = {
      # Runs when a workspace is first created
      onCreate = {
        # Example: install JS dependencies from NPM
        direnv-allow = "direnv allow";
        direnv-shell-hook = "sh -c 'echo \"eval \\\"\\$(direnv hook bash)\\\"\" >> ~/.bashrc && echo \"eval \\\"\\$(direnv hook zsh)\\\"\" >> ~/.zshrc'";
        pnpm-install = "pnpm recursive install";
      };
      # Runs when the workspace is (re)started
      onStart = {
        nix-develop = "nix develop";
      };
    };
  };
}
