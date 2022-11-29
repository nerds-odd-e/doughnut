# Local development machine development environment setup with nix

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
git clone git@github.com:nerds-odd-e/doughnut.git
```

Boot up your doughnut development environment.
MySQL DB server is started and initialised on entering the `nix develop`.

```bash
cd doughnut
nix develop
```

All development tool commands henceforth should be run within `nix develop -c $SHELL`
Run E2E profile springboot backend server with gradle (backend app started on port 9081)

```bash
# from doughnut source root dir
dum install && dum frontend:build
dum sut
open http://localhost:5173
```

Run E2E profile with backend server & frontend in dev mode & Cypress IDE (frontend app on port 5173; backend app on port 9081)
For MS Windows users, you need to ensure your WSL2 Linux has `xvfb` installed. This is not managed by Nix!

```bash
# from doughnut source root dir
dum install && dum frontend:sut
dum sut
dum start:mb
dum cy:open
```

Run E2E (same as Dev) profile springboot backend server with gradle (doughnut full stack started on port 9081)

```bash
# from doughnut source root dir
dum install && dum frontend:build
dum sut
open http://localhost:5173
```
