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
Otherwise run `nix develop`.

### Persistent Development (manual feedback)

From the **unconfigured primary** checkout only:

```bash
pnpm dev
```

This starts the reload-capable Development stack (Spring profile `dev`, database
`doughnut_development`) on backend **8081**, browser/LB **5175**, and Vite
**5176**. Open **http://127.0.0.1:5175/**. Sign in with a local account such as
`manual` / `password`. Logs: `dev.log`; PID: `dev.pid`. Restart only that stack
(without deleting Development data) with `pnpm dev:restart`.

Linked or configured worktrees refuse `pnpm dev` / `pnpm dev:restart`. E2E
testability and reset endpoints are unavailable under `dev`.

### E2E system under test (`pnpm sut`)

```bash
pnpm sut
```

This starts the disposable E2E stack (profile `e2e`, typically
`doughnut_e2e_test`) on backend **9081**, browser/LB **5173**, Vite **5174**,
and Mountebank on the primary checkout. Logs: `sut.log`. Use `pnpm sut:restart`
for that stack. Do not put lasting manual work in E2E — use `pnpm dev` instead.

To start only the E2E backend, use `pnpm backend:sut`. Run complete backend
verification with `pnpm backend:verify`.
