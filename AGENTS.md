# AGENTS.md

Guidance for Codex and other AI coding agents working in this repository.

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Start with `.cursor/agent-map.md` for repo navigation, generated API guidance, focused commands, service assumptions, and default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.cursor/rules/`; use the relevant rule for backend, frontend, E2E, linting, migrations, MCP, or shell-script work.

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning notes belong in `ongoing/`, which is excluded from default indexing. Read it explicitly only for active planning or history tasks.

## Planning and phased delivery

- Informal plans for active work: `ongoing/<short-name>.md`
- **Phase principles, testing strategy, TDD workflow, deploy gate:** `.cursor/rules/planning.mdc` and the **phased-planning** skill (`.cursor/skills/phased-planning/SKILL.md`)
- **Executing plans:** **execute-plan** skill (`.cursor/skills/execute-plan/SKILL.md`). Delegates each phase to a **fresh sub-agent** (Task tool) so context does not accumulate. Each sub-agent commits and pushes before the next starts. Trigger: "do @ongoing/…", "execute plan", "run @ongoing/…".
- **Codebase friction retrospective:** **codebase-retrospective** skill (`.cursor/skills/codebase-retrospective/SKILL.md`); auto-triggered by timer hook after 5 minutes
- **Task decomposition into phases:** **phased-planning** skill; auto-triggered by timer hook after 10 minutes
