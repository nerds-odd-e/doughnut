# AGENTS.md

Index for Codex and other AI coding agents. Skill contracts: `.agents/skills/`; rules: `.cursor/rules/`.

Donut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Start with `.cursor/agent-map.md` for repo navigation, generated API guidance, focused commands, service assumptions, and default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.cursor/rules/`. Cursor injects `alwaysApply: true` rules automatically. **Codex / Claude Code:** read these always-applied rules before coding — `general.mdc`, `error-handling.mdc`, `unit-testing.mdc`, `planning.mdc`, `gsd-coexistence.mdc`, `architecture-decisions.mdc` — then the stack file for the area you touch: frontend → `frontend.mdc`; backend → `backend.mdc`; E2E → `e2e-authoring.mdc`; lint → `linting_formating.mdc`; migrations → `db-migration.mdc`; MCP → `mcp-server.mdc`; CLI → `cli.mdc`. Do not search for other names first. Cursor auto-attaches globbed detail files when matching files are in context.

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning lives under `.planning/` (GSD + local). Canonical coexistence:
`.cursor/rules/gsd-coexistence.mdc`. Slice quality: `.cursor/rules/planning.mdc`.
Do not put new plans under `ongoing/`. Test-optimization candidates live in `.planning/test-optimization-blacklist.md`.

## Principles

Portable digest (details live in the cited always-applied rules — keep `AGENTS.md` and `CLAUDE.md` in sync):

1. High cohesion — one concept, one place (`general.mdc`)
2. Keep it simple — minimum code; no defensive programming (`general.mdc`)
3. Capability naming — no GSD phase numbers in product artifacts (`general.mdc`, `planning.mdc`)
4. Test observables via high-level entry points (`unit-testing.mdc`)
5. Never silently swallow failures — prevent → propagate → enrich → deliberate catch (`error-handling.mdc`)

## Planning and slice delivery

- **Layout (GSD-aligned):** `.planning/phases/NN-slug/`, `.planning/quick/NNN-slug/`, plus GSD `PROJECT` / `ROADMAP` / `STATE` / `codebase/`. See `planning.mdc` and `gsd-coexistence.mdc`.
- **Hard plan grammar:** Behavior vs Structure, stop-safe, one observable behavior per slice (`planning.mdc`) — applies to GSD PLANs too.
- **Time budget (self-enforced):** ~5 min fuzzy goal per problem slice (incl. tests); >5 min → scrutinize finer decompose; >10 min → hard finer-decompose + revert/retry unless good reason (`planning.mdc`).
- **History:** keep resume-useful planning artifacts while a plan is in progress; **clean up** spent history when the plan is fully executed into code/permanent docs.
- **Execution wrap-up (required):** Jidoka → post-change-refactor → update plan → commit → push (**execute-plan**; also `/gsd-execute-phase`). Skills emit completion markers for handoff.
- **GSD** for milestones (`/gsd-onboard`, `/gsd-plan-phase`, `/gsd-execute-phase`, …); **slice-planning** + **execute-plan** for ad-hoc slices under `.planning/quick/`.
- **Test optimization:** `test-optimization` skill — plans under `.planning/phases/` or `quick/`, run via execute-plan.
- **Non-compatible local overlays** (must keep): documented in `.cursor/rules/gsd-coexistence.mdc`.

## Cursor Cloud specific instructions

On the Cloud VM there is **no Nix** — run commands directly (drop the `CURSOR_DEV=true nix develop -c` prefix). Full details: `.agents/skills/cloud-vm-setup/SKILL.md`.

- **Dependency refresh is automatic; system services are not.** The startup update script only runs `pnpm --frozen-lockfile recursive install`. It does **not** install Java/MySQL/Redis or start any service. Before running backend tests, E2E tests, or the app, run the idempotent setup script:
  ```bash
  source /workspace/scripts/cloud_agent_setup.sh
  ```
  It installs Java 25, MySQL 8.4 (port 3309), Redis (port 6380), and xvfb, initializes the `doughnut_test` / `doughnut_e2e_test` databases, and runs the test-DB migration.
- **`source` it, don't execute it.** The script exports env vars (`JAVA_HOME`, `MYSQL_HOME`, `PATH`, `SPRING_DATASOURCE_URL`, `INPUT_DB_URL`, `CI=1`) into the current shell only. Any new shell that runs Gradle/backend/E2E must `source` it first, or those commands won't find Java/MySQL.
- **Node engine warning is benign.** `package.json` wants Node `>=26.7` but the VM ships Node v22; there is no `engine-strict`, so install, lint, unit tests, build, and the app all work on the preinstalled Node. Do not spend time upgrading Node unless a task truly needs a v26 runtime feature.
- **Run the app (full dev stack):** after sourcing the setup script, `SUT_TIMEOUT_MS=360000 pnpm sut` (first boot can exceed the 120s default). App (local LB) → http://localhost:5173/, Vite → 5174, backend → 9081, Mountebank → 2525. Health: `pnpm sut:healthcheck`; restart: `pnpm sut:restart`. `pnpm sut` spawns services detached and returns once healthy.
- **Local login for manual testing:** open http://localhost:5173/, click Login (or go to `/users/identify`), then use a seeded account — `old_learner` / `password`, `another_old_learner` / `password`, or `admin` / `password` (form fields `id=username`, `id=password`, `id=login-button`).
- **Common commands:** frontend unit tests `pnpm frontend:test`; backend unit tests `./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel`; lint everything `pnpm lint:all`.
