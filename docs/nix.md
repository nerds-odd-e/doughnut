# Local development machine development environment setup with nix

## :warning: 🚨 **ONLY PROCEED**  with the subsequent steps if `./setup-doughnut-dev.sh` (see [README.md](../README.md)) somehow failed horribly for you!!!

### 1. Install Nix

We use Nix installer by Determinate Systems to manage and ensure a reproducible development environment ([https://determinate.systems/posts/determinate-nix-installer/](https://determinate.systems/posts/determinate-nix-installer/)).

Full details on Nix installation via Determinate nix-installer [here](https://github.com/DeterminateSystems/nix-installer)

```bash
curl --proto '=https' --tlsv1.2 -sSf -L https://install.determinate.systems/nix | sh -s -- install --determinate
```

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
Run E2E profile springboot backend server with gradle (backend app started on port 9081 with auto-reload)

```bash
# from doughnut source root dir
pnpm backend:sut
```

The backend will automatically restart when you change Java code.
Navigate to README.md

### 3. Uninstalling

IF YOU REALLY REALLY NEED TO, you can remove Nix by running

```bash
/nix/nix-installer uninstall
```
