# AGENTS.md

Index for Codex and other AI coding agents. Skill contracts: `.agents/skills/`; rules: `.cursor/rules/`.

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Start with `.cursor/agent-map.md` for repo navigation, generated API guidance, focused commands, service assumptions, and default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.cursor/rules/`. Cursor injects `alwaysApply: true` rules automatically. **Codex / Claude Code:** read these always-applied rules before coding — `general.mdc`, `error-handling.mdc`, `unit-testing.mdc`, `planning.mdc`, `gsd-coexistence.mdc`, `architecture-decisions.mdc` — then the stack rule for the area you touch (backend, frontend, E2E, linting, migrations, MCP, CLI).

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning lives under `.planning/` (GSD + local). Canonical coexistence:
`.cursor/rules/gsd-coexistence.mdc`. Phase quality: `.cursor/rules/planning.mdc`.
Legacy notes may remain under `ongoing/` — do not migrate unless asked.

## Principles

Portable digest (details live in the cited always-applied rules — keep `AGENTS.md` and `CLAUDE.md` in sync):

1. High cohesion — one concept, one place (`general.mdc`)
2. Keep it simple — minimum code; no defensive programming (`general.mdc`)
3. Capability naming — no phase numbers in product artifacts (`general.mdc`, `planning.mdc`)
4. Test observables via high-level entry points (`unit-testing.mdc`)
5. Never silently swallow failures — prevent → propagate → enrich → deliberate catch (`error-handling.mdc`)

## Planning and phased delivery

- **Layout (GSD-aligned):** `.planning/phases/NN-slug/`, `.planning/quick/NNN-slug/`, plus GSD `PROJECT` / `ROADMAP` / `STATE` / `codebase/`. See `planning.mdc` and `gsd-coexistence.mdc`.
- **Hard plan grammar:** Behavior vs Structure, stop-safe, one observable behavior per phase (`planning.mdc`) — applies to GSD PLANs too.
- **Time budget (self-enforced):** ~5 min fuzzy goal per problem slice (incl. tests); >5 min → scrutinize finer decompose; >10 min → hard finer-decompose + revert/retry unless good reason (`planning.mdc`).
- **History:** keep resume-useful planning artifacts while a plan is in progress; **clean up** spent history when the plan is fully executed into code/permanent docs.
- **Execution wrap-up (required):** Jidoka → post-change-refactor → update plan → commit → push (**execute-plan**; also `/gsd-execute-phase`). Skills emit completion markers for handoff.
- **GSD** for milestones (`/gsd-onboard`, `/gsd-plan-phase`, `/gsd-execute-phase`, …); **phased-planning** + **execute-plan** for ad-hoc slices under `.planning/quick/`.
- **Test optimization:** `test-optimization` skill — plans under `.planning/phases/` or `quick/`, run via execute-plan.
- **Non-compatible local overlays** (must keep): documented in `.cursor/rules/gsd-coexistence.mdc`.
