# Donut

![dough CI CD](https://github.com/nerds-odd-e/doughnut/actions/workflows/ci.yml/badge.svg) [![Join the chat at https://gitter.im/Odd-e-doughnut/community](https://badges.gitter.im/Odd-e-doughnut/community.svg)](https://gitter.im/Odd-e-doughnut/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## About

Donut is a Personal Knowledge
Management ([PKM](https://en.wikipedia.org/wiki/Personal_knowledge_management)) tool
combining [zettelkasten](https://eugeneyan.com/writing/note-taking-zettelkasten/) style of knowledge
capture with spaced repetition and recall, and the ability to
share knowledge bits with other people (for buddies and teams).

For more background info you can read::

- [Scholarship & Learning](https://www.lesswrong.com/tag/scholarship-and-learning)
- [Knowledge Acquisition & Documentation Structuring](https://en.m.wikipedia.org/wiki/Knowledge_Acquisition_and_Documentation_Structuring)

## [Product Backlog](https://doughnut.odd-e.com/n24336)

[Story Map](https://miro.com/app/board/o9J_lTB77Mc=/)

## Donut CLI

```bash
# Install (macOS, Linux, WSL)
curl https://doughnut.odd-e.com/install -fsS | bash
# Install (Windows PowerShell)
irm 'https://doughnut.odd-e.com/install?win32=true' | iex
# Run CLI
donut
```

## [Donut Technology Stack](./docs/tech_stack.md)

## [Current Architecture Videos](./docs/current_architecture_workshops.md)

## Getting started

### 1. [Development environment setup](./docs/development-setup.md)

### 2. Git Pre-commit Hook

Format changed components explicitly before staging:

```bash
./scripts/run.sh pnpm format:changed
git add <intended-paths>
git commit -m "..."
```

`format:changed` considers staged, unstaged, and nonignored untracked paths,
then formats only the affected repository components. Review those changes
before staging the intended commit.

A pre-commit hook then validates affected staged components with
`pnpm lint:changed`. It is check-only: it does not format files or mutate the
working tree or Git index.

**Setup:**
The git hooks are version-controlled in `scripts/git-hooks/`. To install them, run:

```bash
./scripts/setup-git-hooks.sh
```

This will copy the hooks from `scripts/git-hooks/` to `.git/hooks/` and make them executable. You only need to run this once after cloning the repository, or whenever hooks are updated.

**Behavior:**
- The hook runs automatically on every `git commit`
- It lints only the components affected by staged paths
- If linting succeeds, the commit proceeds
- If linting fails, the commit is blocked without staging or rewriting files

**Note:** The hook uses `./scripts/run.sh` which automatically handles the nix environment, so it works whether you're in a nix shell or not.

### 3. [IntelliJ IDEA settings](./docs/idea.md)

### 4. [End-to-end testing](./docs/end-to-end-testing.md)

### 5. Database migrations

You can find the database migrations in `backend/src/main/resources/db/migration/`.
The migrations are run automatically when the backend app starts up.
It will also run the migrations for test when you run `pnpm backend:test`.
To trigger the test DB migration manually, run `backend/gradlew migrateTestDB`.
To connect to the local DB: `mysql -S $MYSQL_HOME/mysql.sock -u doughnut -p` (password=doughnut).

### 6. Vue3 web-app frontend

We chose Vue3 + Vite to build our frontend.

The TypeScript code calling the backend services is generated from the backend code. Run

```bash
pnpm generateTypeScript
```

To do the code generation. There are two steps in this command:

1. Generate openAPI docs from the backend service into `./open_api_docs.yaml`.
2. Generate TypeScript interfaces from the openAPI docs, into `frontend/src/generated`.

If the step 1 is not done, a unit test will fail. If the step 2 is not done, CI will fail (`./assert_generated_type_script_up_to_date.sh`).

#### How-to

##### Run frontend unit tests (with Vitest)

From `doughnut` source root directory

```bash
pnpm frontend:verify
```

##### Run frontend dev server only (Vite; use **`pnpm sut`** for the usual browser URL — see [prod_env.md](docs/gcp/prod_env.md))

```bash
pnpm frontend:sut
```

##### Build & Bundle Vue3 frontend web-app assets and startup backend app (backend webapp will launch on port 9081)

```bash
pnpm frontend:build
pnpm backend:sut
```

Expect the Vue production build under `frontend/dist`. The CLI install URL is served from GCS in prod; locally, `pnpm cli:bundle` produces `cli/dist/donut-cli.bundle.mjs`, and the local LB (`scripts/local-lb.mjs` via `pnpm sut` / `pnpm test`) serves `/doughnut-cli-latest/doughnut` from that file — not Spring on 9081.

### 7. [Integrating MCP server for IDE](./\.cursor/rules/mcp-server.mdc#how-to-use-this-mcp-server)

### 8. Manual testing locally — see `.agents/skills/manual-testing/SKILL.md`

### 9. [Style Guide & Code linting/formating](./docs/linting_formating.md)

### 10. Production environment

- [GCP production notes](./docs/gcp/prod_env.md) — includes **conditional backend deploy** (when CI skips GCS/MIG on unchanged jar) and how to **force a full deploy** with `force-deployment: true` in the deploy commit message; details in [conditional-backend-deploy.md](./docs/gcp/conditional-backend-deploy.md).

### 11. [Donut source code secrets management](./docs/secrets_management.md)

### 12. Architecture and Design documentation

### 13. Teardown and cleanup

- pnpm: To clean up packages installed by pnpm, you can run pnpm store prune to remove unused packages from the store. To remove all packages for a specific project, navigate to the project directory and run pnpm recursive uninstall to uninstall all dependencies in the project and its subdirectories.
- direnv: To stop direnv from automatically loading the environment, you can simply delete the .envrc/ file in the project directory or run direnv deny in the project directory. To uninstall direnv, use the package manager you installed it with (e.g., brew uninstall direnv for macOS).
- Nix: If you want to remove the Nix package manager and all packages installed through it, you can run sudo rm -rf /nix to delete the Nix store. To uninstall Nix completely, follow the [official Nix documentation for uninstallation](https://nix.dev/manual/nix/2.22/installation/uninstall) instructions.

[Miro board](https://miro.com/app/board/uXjVNNaWVeA=/?share_link_id=753160038592)

## How to Contribute

- We welcome product ideas and code contribution.
- Collaborate over:
  - [GitHub Discussions](https://github.com/nerds-odd-e/doughnut/discussions) for product
    ideas/features,
  - [GitHub Issues](https://github.com/nerds-odd-e/doughnut/issues) for reporting issues or bugs, OR
  - [Odd-e-doughnut gitter](https://gitter.im/Odd-e-doughnut/community)
- FOSS style; Fork and submit GitHub PR.
  - Please keep the PR small and on only one topic
  - The code need to come with tests.
  
