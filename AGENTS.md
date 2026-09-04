# AGENTS.md

Index for Codex and other AI coding agents. Skill contracts: `.agents/skills/`; rules: `.cursor/rules/`.

Donut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Start with `.cursor/agent-map.md` for repo navigation, generated API guidance, focused commands, service assumptions, and default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.cursor/rules/`. Cursor injects `alwaysApply: true` rules automatically. **Codex / Claude Code:** read these always-applied rules before coding — `general.mdc`, `unit-testing.mdc`, `problem-decomposition.mdc`, `planning.mdc`, `gsd-coexistence.mdc`, `architecture-decisions.mdc` — then the stack file for the area you touch: frontend → `frontend.mdc`; backend → `backend.mdc`; E2E → `e2e-authoring.mdc`; lint → `linting_formating.mdc`; migrations → `db-migration.mdc`; MCP → `mcp-server.mdc`; CLI → `cli.mdc`. Do not search for other names first. Cursor auto-attaches globbed detail files when matching files are in context.

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning lives under `.planning/` (GSD + local). Canonical coexistence:
`.cursor/rules/gsd-coexistence.mdc`. Decomposition and slice quality:
`.cursor/rules/problem-decomposition.mdc`; planning artifacts and lifecycle:
`.cursor/rules/planning.mdc`.
Do not put new plans under `ongoing/`. Test-optimization candidates live in `.planning/test-optimization-blacklist.md`.

## Principles

Portable digest (details live in the cited always-applied rules — keep `AGENTS.md` and `CLAUDE.md` in sync):

1. High cohesion — one concept, one place (`general.mdc`)
2. Keep it simple — minimum code; no defensive programming (`general.mdc`)
3. Capability naming — no GSD phase numbers in product artifacts (`general.mdc`, `planning.mdc`)
4. Test observables via high-level entry points (`unit-testing.mdc`)
5. Failure handling — fail loudly is legitimate; catch for a business outcome or a clearer message (ADR 0006)
6. Prefer committing all changes and leaving none local; partial commits are deliberate exceptions, not forbidden

## Planning and slice delivery

- **Layout (GSD-aligned):** non-executable story decompositions under `.planning/seeds/`; executable work under `.planning/phases/NN-slug/` or `.planning/quick/NNN-slug/`, plus GSD `PROJECT` / `ROADMAP` / `STATE` / `codebase/`. See `planning.mdc` and `gsd-coexistence.mdc`.
- **Hard decomposition grammar:** problem → 3V story → Behavior/Structure execution leaf; stop-safe, one evaluable outcome at the current resolution (`problem-decomposition.mdc`) — applies to GSD PLANs too.
- **Time budget (self-enforced):** story hypotheses are roughly 30 minutes to a few hours; execution leaves target ~5 min including tests; >5 min → scrutinize; >10 min → hard finer-decompose unless a stated good reason (`problem-decomposition.mdc`).
- **History:** keep resume-useful planning artifacts while a plan is in progress; **clean up** spent history when the plan is fully executed into code/permanent docs.
- **Execution wrap-up (required):** Jidoka → fresh post-change-refactor agent → API generation when needed → coordinator runs `./scripts/run.sh pnpm format:changed` once → update plan without a second routine formatting pass → commit (independent check-only lint hook) → push (**execute-plan**; also `/gsd-execute-phase`). `format-changed` remains on-demand; implementers/refactorers run neither it nor standalone `lint:changed`.
- **Story shaping:** use **story-decomposition** for broad or unclear requirements; one non-executable decomposition seed contains ordered candidate stories.
- **Plan refinement:** use **slice-plan-refinement** in place when an existing PLAN is complex, sizing confidence is low, or execution overruns. Skip the extra pass when slice-planning already produced clear commit-sized leaves.
- **Execution retrospective:** reconstruct a completed plan and its commits with **execution-retrospective**; audit the aggregate result and process, and stop after generating any follow-up PLAN without executing it.
- **GSD** for milestones (`/gsd-onboard`, `/gsd-plan-phase`, `/gsd-execute-phase`, …); for one selected ad-hoc story use **slice-planning** → optional **slice-plan-refinement** → **execute-plan** under `.planning/quick/`.
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
