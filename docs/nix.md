# Local development machine development environment setup with nix

## :warning: 🚨 **ONLY PROCEED**  with the subsequent steps if `./setup-doughnut-dev.sh` (see [README.md](../README.md)) somehow failed for you!!!

### 1. Install Lix

We use Lix (a variant of Nix package manager) to manage and ensure a reproducible development environment ([https://lix.systems/](https://lix.systems/)).

Full details on Lix installation via lix-installer [here](https://git.lix.systems/lix-project/lix-installer)

```bash
curl --proto '=https' --tlsv1.2 -sSf -L https://install.lix.systems/lix | sh -s -- install
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
Run E2E profile springboot backend server with gradle (backend app started on port 9081)

```bash
# from doughnut source root dir
pnpm sut
```

Open your browser to visit [local doughnut product](http://localhost:9081)

Run backend unit tests

```bash
# from doughnut source root dir
pnpm backend:verify
```

Run frontend unit tests

```bash
# from doughnut source root dir
pnpm frontend:verify
```

Run E2E profile with backend server & frontend in dev mode & Cypress IDE (frontend app on port 5173; backend app on port 9081)

#### :warning: For MS Windows users, you need to ensure your WSL2 Linux has `xvfb` installed. This is not managed by Nix! (see [Additional things to note for Microsoft Windows10/Windows11 developers using WSL2g with Ubuntu-23.04.](./wsl2.md))

```bash
# from doughnut source root dir
pnpm test:open
```

Run headless E2E (doughnut full stack started on port 9081)

```bash
# from doughnut source root dir
pnpm verify
```

### 3. Upgrade Lix

You can upgrade Lix with:

```bash
sudo -i nix upgrade-nix
```

### 4. Uninstalling

I hope you don't, but IF YOU REALLY REALLY NEED TO, you can remove a lix-installer-installed Nix by running

```bash
/nix/lix-installer uninstall
```
