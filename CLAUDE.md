# CLAUDE.md

Index for Claude Code and other AI coding agents. Skill contracts: `.agents/skills/`; rules: `.cursor/rules/`.

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
- **Execution wrap-up (required):** Jidoka → post-change-refactor → API generation when needed → fresh format-changed agent → update plan → commit → push (**execute-plan**; also `/gsd-execute-phase`). Skills emit completion markers for handoff.
- **Story shaping:** use **story-decomposition** for broad or unclear requirements; one non-executable decomposition seed contains ordered candidate stories.
- **GSD** for milestones (`/gsd-onboard`, `/gsd-plan-phase`, `/gsd-execute-phase`, …); **slice-planning** + **execute-plan** for one selected story under `.planning/quick/`.
- **Test optimization:** `test-optimization` skill — plans under `.planning/phases/` or `quick/`, run via execute-plan.
- **Non-compatible local overlays** (must keep): documented in `.cursor/rules/gsd-coexistence.mdc`.
