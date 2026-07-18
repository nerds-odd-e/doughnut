# AGENTS.md

Index for Codex and other AI coding agents. Skill contracts: `.cursor/skills/`; rules: `.cursor/rules/`.

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Start with `.cursor/agent-map.md` for repo navigation, generated API guidance, focused commands, service assumptions, and default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.cursor/rules/`; use the relevant rule for backend, frontend, E2E, linting, migrations, MCP, or shell-script work.

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning lives under `.planning/` (GSD + local). Canonical coexistence:
`.cursor/rules/gsd-coexistence.mdc`. Phase quality: `.cursor/rules/planning.mdc`.
Legacy notes may remain under `ongoing/` — do not migrate unless asked.

## Planning and phased delivery

- **Layout (GSD-aligned):** `.planning/phases/NN-slug/`, `.planning/quick/NNN-slug/`, plus GSD `PROJECT` / `ROADMAP` / `STATE` / `codebase/`. See `planning.mdc` and `gsd-coexistence.mdc`.
- **Hard plan grammar:** Behavior vs Structure, stop-safe, one observable behavior per phase (`planning.mdc`) — applies to GSD PLANs too.
- **History:** keep resume-useful planning artifacts while a plan is in progress; **clean up** spent history when the plan is fully executed into code/permanent docs.
- **Execution wrap-up (required):** Jidoka → post-change-refactor → update plan → commit → push (**execute-plan**; also `/gsd-execute-phase`). Skills emit completion markers for handoff.
- **GSD** for milestones (`/gsd-onboard`, `/gsd-plan-phase`, `/gsd-execute-phase`, …); **phased-planning** + **execute-plan** for timer/ad-hoc slices under `.planning/quick/`.
- **Timers:** 5 min → `codebase-retrospective`; 10 min → stop, stash, write/update plan under `.planning/quick/` (or `phases/`).
- **Test optimization:** `test-optimization` skill — plans under `.planning/phases/` or `quick/`, run via execute-plan.
- **Non-compatible local overlays** (must keep): documented in `.cursor/rules/gsd-coexistence.mdc`.
