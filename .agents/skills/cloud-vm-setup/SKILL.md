---
name: cloud-vm-setup
description: >-
  Set up and run the doughnut dev environment on Cursor Cloud VM (no Nix).
  Use when working on Cloud VM, running tests on Cloud VM, or encountering
  command errors related to missing nix. Triggers on: cloud VM, cloud agent,
  no nix available, /workspace path.
---

<objective>
Set up and run the doughnut dev environment on Cursor Cloud VM without Nix.

Purpose: Cloud-agent development and testing when `nix develop` is unavailable.

Output: Environment ready for tests + summary ending with `## CLOUD VM READY`.
</objective>

<context>
**On Cloud VM: do NOT use `CURSOR_DEV=true nix develop -c` prefix. Run commands directly.**

**Pre-configured:** Node.js v24.16.0, pnpm 11.3.0 — no nix needed.

**Ports:**

| Service | Port |
|---------|------|
| Spring Boot | 9081 |
| Local LB | 5173 |
| Vite dev | 5174 |
| MySQL | 3309 |
| Redis | 6380 |

`CI=1` is exported by the setup script (CLI PTY tests use pipe fallback, matching
GitHub Actions).
</context>

<process>

<step name="one_time_setup">
Backend + E2E one-time setup (idempotent — safe to re-run):

```bash
source /workspace/scripts/cloud_agent_setup.sh
```

Installs: Java 25, MySQL 8.4 (port 3309), Redis (port 6380), xvfb, and initializes
test databases.
</step>

<step name="frontend_tests">
```bash
pnpm frontend:test
```

Single file: `pnpm -C frontend test tests/path/to/TestFile.spec.ts` (no `--` before the path).
</step>

<step name="backend_tests">
```bash
source /workspace/scripts/cloud_agent_setup.sh
./backend/gradlew -p backend migrateTestDB -Dspring.profiles.active=test
./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel
```
</step>

<step name="e2e_tests">
```bash
source /workspace/scripts/cloud_agent_setup.sh
pnpm bundle:all
pnpm exec run-p -clnr backend:sut:ci start:mb local:lb &
NO_PROXY=127.0.0.1,localhost pnpm exec wait-on http://127.0.0.1:5173/__lb__/ready --timeout 120000
xvfb-run pnpm cypress run --spec e2e_test/features/<feature>.feature --config-file e2e_test/config/ci.ts
```
</step>

<step name="sut_health">
- `pnpm sut:healthcheck` — check if stack is up
- `pnpm sut:restart` — SIGTERM listeners on 5173/5174/9081, then restart (requires `lsof`)
</step>

</process>

<success_criteria>
- Setup script run when backend/E2E needed
- Commands run **without** nix prefix
- Correct ports and healthcheck commands available
- Final output includes `## CLOUD VM READY`
</success_criteria>

<output>
Confirm environment state and which test commands to use.

**Common commands:**

| Task | Command |
|------|---------|
| Frontend tests | `pnpm frontend:test` |
| Backend tests | `pnpm backend:verify` |
| Linting | `pnpm lint:all` |
| Formatting | `pnpm format:all` |

```
## CLOUD VM READY
```
</output>

<out_of_scope>
- Do not use `CURSOR_DEV=true nix develop -c` on Cloud VM.
- Do not assume local Nix shell ports or paths (`/Users/...` vs `/workspace/...`).
</out_of_scope>
