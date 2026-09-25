# CLAUDE.md

Repository instructions for Claude Code and other AI coding agents.

Donut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Consult `.agents/agent-map.md` when you need repository entry points, generated API guidance, focused commands, service assumptions, or default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.agents/skills/` as Agent Skills. Always-on skills for every coding session: `unit-testing`. Then the stack skill for the area you touch: frontend → `frontend`; backend → `backend`; E2E → `e2e-authoring`; lint → `linting_formating`; migrations → `db-migration`; MCP → `mcp-server`; CLI → `cli`. Do not search for other names first. Path-scoped skills (`frontend-component`, `backend-testing`, and so on) attach when matching files are in context.

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning lives under `.planning/`. Decomposition and slice quality:
`.agents/skills/dough-story-decomposition/references/problem-decomposition.md`; planning artifacts and lifecycle:
`.agents/skills/dough-story-refinement/references/planning.md`.
Do not put new plans under `ongoing/`. Test-optimization candidates live in `.planning/test-optimization-candidates.md`.

## Principles

Portable digest (details live in the cited skills — keep `AGENTS.md` and `CLAUDE.md` in sync):

1. High cohesion — one concept, one place (`dough-post-change-refactor` skill)
2. Keep it simple — minimum code; no defensive programming (`dough-post-change-refactor` skill)
3. Capability naming — no planning sequence numbers in product artifacts (`dough-story-refinement` planning reference)
4. Test observables via high-level entry points (`unit-testing` skill)
5. Failure handling — fail loudly is legitimate; catch for a business outcome or a clearer message (ADR 0006)
6. Prefer committing all changes and leaving none local; partial commits are deliberate exceptions, not forbidden

## Planning and slice delivery

- **Plan numbering:** The owner reset `.planning/quick/` numbering to `001` on 2026-09-20. Use three-digit sequential numbers from this reset (`001`, `002`, …); ignore pre-reset allocations in Git history. Story/seed identities are unchanged.

- **Layout:** non-executable story decompositions under `.planning/seeds/`; executable plans under `.planning/quick/NNN-slug/`; the ordered story queue in `.planning/PRODUCT-BACKLOG.md`; short-term architectural direction in `.planning/NORTH-STAR.md`. See `.agents/skills/dough-story-refinement/references/planning.md`.
- **Hard decomposition grammar:** problem → 3V story → Behavior/Structure execution leaf; stop-safe, one evaluable outcome at the current resolution (`.agents/skills/dough-story-decomposition/references/problem-decomposition.md`).
- **Time budget (self-enforced):** story hypotheses are roughly 30 minutes to a few hours; execution leaves target ~5 min including tests; >5 min → scrutinize; >10 min → hard finer-decompose unless a stated good reason (`.agents/skills/dough-story-decomposition/references/problem-decomposition.md`).
- **History:** keep resume-useful planning artifacts while a plan is in progress; **clean up** spent history when the plan is fully executed into code/permanent docs.
- **Execution wrap-up (required):** Jidoka → fresh dough-post-change-refactor agent → API generation when needed → coordinator runs `./scripts/run.sh pnpm format:changed` once → update plan without a second routine formatting pass → commit (independent check-only lint hook) → push (**dough-execute-plan**). `format-changed` remains on-demand; implementers/refactorers run neither it nor standalone `lint:changed`.
- **Story shaping:** use **dough-story-decomposition** for broad or unclear requirements; one non-executable decomposition seed contains ordered candidate stories. Queue and reprioritize unfinished stories with **dough-product-backlog**; details stay in the home seed.
- **Story refinement:** use `.agents/skills/dough-story-refinement/SKILL.md` to clarify selected stories' goal, scope, and key examples in their home seeds before slice planning. Apply conservative scope and post-implementation cleanup from `.agents/skills/dough-story-refinement/references/planning.md`.
- **Plan refinement:** use **dough-slice-plan-refinement** in place when an existing PLAN is complex, sizing confidence is low, or execution overruns. Skip the extra pass when dough-slice-planning already produced clear commit-sized leaves.
- **Execution retrospective:** review a completed or in-progress plan with **dough-execution-retrospective**; it may plan bounded corrections but never executes them. Close completed story history afterward with **dough-story-wrap-up**.
- **Selected story delivery:** use **dough-slice-planning** → optional **dough-slice-plan-refinement** → **dough-execute-plan** under `.planning/quick/`.
- **Test optimization:** `dough-test-optimization` — use the public Open Dough workflow; plans live under `.planning/quick/` and run via dough-execute-plan. Donut-specific commands and profile exclusions live in `.agents/agent-map.md` and the applicable stack testing skills.

## Cursor Cloud specific instructions

On the Cloud VM there is **no Nix** — run commands directly (drop the `CURSOR_DEV=true nix develop -c` prefix). Full details: `.agents/skills/cloud-vm-setup/SKILL.md`.

- **Dependency refresh is automatic; system services are not.** The startup update script only runs `pnpm --frozen-lockfile recursive install`. It does **not** install Java/MySQL/Redis or start any service. Before running backend tests, E2E tests, or the app, run the idempotent setup script:
  ```bash
  source /workspace/scripts/cloud_agent_setup.sh
  ```
  It installs Java 25, MySQL 8.4 (port 3309), Redis (port 6380), and xvfb, initializes the `doughnut_test` / `doughnut_e2e_test` databases, and runs the test-DB migration.
- **`source` it, don't execute it.** The script exports env vars (`JAVA_HOME`, `MYSQL_HOME`, `PATH`, `SPRING_DATASOURCE_URL`, `INPUT_DB_URL`, `CI=1`) into the current shell only. Any new shell that runs Gradle/backend/E2E must `source` it first, or those commands won't find Java/MySQL.
- **Node engine warning is benign.** `package.json` wants Node `>=26.7` but the VM ships Node v22; there is no `engine-strict`, so install, lint, unit tests, build, and the app all work on the preinstalled Node. Do not spend time upgrading Node unless a task truly needs a v26 runtime feature.
- **Run E2E:** after sourcing the setup script, `SUT_TIMEOUT_MS=360000 pnpm cy:run --spec <feature>` (first boot can exceed the 120s default; the wrapper owns its stack). App (local LB) → http://localhost:5173/, Vite → 5174, backend → 9081, Mountebank → 2525.
- **Local login for manual testing:** open http://localhost:5173/, click Login (or go to `/users/identify`), then use a seeded account — `old_learner` / `password`, `another_old_learner` / `password`, or `admin` / `password` (form fields `id=username`, `id=password`, `id=login-button`).
- **Common commands:** frontend unit tests `pnpm frontend:test`; backend unit tests `./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --parallel`; lint everything `pnpm lint:all`.
