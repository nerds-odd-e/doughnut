---
name: cloud-vm-setup
description: >-
  Set up and run the doughnut dev environment on Cursor Cloud VM (no Nix).
  Use when working on Cloud VM, running tests on Cloud VM, or encountering
  command errors related to missing nix. Triggers on: cloud VM, cloud agent,
  no nix available, /workspace path.
---

# Cloud VM Development Environment

**On Cloud VM: do NOT use `CURSOR_DEV=true nix develop -c` prefix. Run commands directly.**

## Pre-configured

- Node.js v22.21.1, pnpm 10.22.0 — no nix needed.

## One-Time Setup (backend + E2E)

```bash
source /workspace/scripts/cloud_agent_setup.sh
```

Installs: Java 25, MySQL 8.4 (port 3309), Redis (port 6380), xvfb, and initializes test databases. Idempotent — safe to re-run.

## Running Tests

### Frontend

```bash
pnpm frontend:test
```

Single file: `pnpm -C frontend test tests/path/to/TestFile.spec.ts` (no `--` before the path).

### Backend

```bash
source /workspace/scripts/cloud_agent_setup.sh
./backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test
./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel
```

### E2E

```bash
source /workspace/scripts/cloud_agent_setup.sh
pnpm bundle:all
pnpm exec run-p -clnr backend:sut:ci start:mb local:lb &
NO_PROXY=127.0.0.1,localhost pnpm exec wait-on http://127.0.0.1:5173/__lb__/ready --timeout 120000
xvfb-run pnpm cypress run --spec e2e_test/features/<feature>.feature --config-file e2e_test/config/ci.ts
```

`CI=1` is exported by setup script (CLI PTY tests use pipe fallback, matching GitHub Actions).

## SUT Health / Restart

- `pnpm sut:healthcheck` — check if stack is up
- `pnpm sut:restart` — SIGTERM listeners on 5173/5174/9081, then restart (requires `lsof`)

## Ports

| Service | Port |
|---------|------|
| Spring Boot | 9081 |
| Local LB | 5173 |
| Vite dev | 5174 |
| MySQL | 3309 |
| Redis | 6380 |

## Common Commands

| Task | Command |
|------|---------|
| Frontend tests | `pnpm frontend:test` |
| Backend tests | `pnpm backend:verify` |
| Linting | `pnpm lint:all` |
| Formatting | `pnpm format:all` |
