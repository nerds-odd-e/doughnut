# Local development machine development environment setup with nix

## :warning: ONLY PROCEED  with the subsequent steps if `./setup-doughnut-dev.sh` (see [README.md](../README.md)) somehow failed for you!!!

### 1. Install nix

We use nix to manage and ensure a reproducible development environment ([nixos.org](https://nixos.org)).

#### For macOS:

Full details on nix installation on macOS [here](https://nixos.org/manual/nix/stable/#sect-macos-installation)

```bash
 sh < (curl -k -L https://nixos.org/nix/install) --darwin-use-unencrypted-nix-store-volume
```

#### For Linux:

```bash
sh < (curl -k -L https://nixos.org/nix/install) --no-daemon
```

(NB: if the install script fails to add sourcing of `nix.sh` in `.bashrc` or `.profile`, you can do it manually `source /etc/profile.d/nix.sh`)

### 2. Setup and run doughnut for the first time (local development profile)

```bash
mkdir -p ~/.config/nix
echo 'experimental-features = nix-command flakes' >> ~/.config/nix/nix.conf
```

Launch a new terminal in your favourite shell (I highly recommend zsh).
Clone full all-in-one doughnut codebase from Github (Microsoft Windows OS users, please clone the repo to a non-Windows mount directory)

```bash
git config --global core.autocrlf input
git clone git@github.com:nerds-odd-e/doughnut.git
git add --renormalize .
```

Boot up your doughnut development environment.
MySQL DB server wil be started and initialised on entering the local cloned `doughnut` source directory via `direnv` else run `nix develop`.

```bash
cd doughnut
```

All development tool commands henceforth should work when in the `nix` development environment that will be bootstrapped by `direnv` (if installed and configured correctly), else run `nix develop` to get the necessary tooling installed correctly.
Run E2E profile springboot backend server with gradle (backend app started on port 9081)

```bash
# from doughnut source root dir
pnpm sut
```

Open your browser to visit http://localhost:9081

Run backend unit tests

```
# from doughnut source root dir
pnpm backend:verify
```

Run frontend unit tests

```
# from doughnut source root dir
pnpm frontend:verify
```

Run E2E profile with backend server & frontend in dev mode & Cypress IDE (frontend app on port 5173; backend app on port 9081)
##### :warning: For MS Windows users, you need to ensure your WSL2 Linux has `xvfb` installed. This is not managed by Nix! (see [Additional things to note for Microsoft Windows10/Windows11 developers using WSL2g with Ubuntu-23.04.](./wsl2.md))

```bash
# from doughnut source root dir
pnpm test:open
```

Run headless E2E (doughnut full stack started on port 9081)

```bash
# from doughnut source root dir
pnpm verify
```
