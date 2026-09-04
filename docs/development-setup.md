# Development Environment Setup

From the repository root, ensure Git respects the expected line endings:

```bash
git config --global core.autocrlf input
git add --renormalize .
```

Install the Nix package manager and repository tooling:

```bash
./setup-donut-dev.sh
```

On macOS 15 Sequoia, if Nix installation or startup fails, apply the
[Nix migration workaround](https://github.com/NixOS/nix/issues/10892):

```bash
curl --proto '=https' --tlsv1.2 -sSf -L https://github.com/NixOS/nix/raw/master/scripts/sequoia-nixbld-user-migration.sh | bash -
```

Install `direnv` (`brew install direnv`, `sudo apt-get install -y direnv`, or
`sudo dnf install direnv`), add its hook, and allow the repository:

```bash
echo 'eval "$(direnv hook bash)"' >> ~/.bashrc
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
direnv allow
```

Ensure `/bin/sh` points to Bash. On Ubuntu systems where it points to Dash, run
`sudo dpkg-reconfigure dash` and answer “No”. WSL2 developers should also read
[the WSL2 notes](./wsl2.md), clone the repository inside the Linux filesystem
rather than `/mnt/c/`, and be aware that Nix and sdkman may conflict.

Use [the manual Nix setup](./nix.md) only when the quick start fails.

## Run Donut

With `direnv` configured, entering the repository loads Nix automatically.
Otherwise run `nix develop`, then start the complete development environment:

```bash
pnpm sut
```

This starts the auto-reloading backend (port 9081), frontend, and Mountebank. To
start only the backend, use `pnpm backend:sut`. Run complete backend verification
with `pnpm backend:verify`.
