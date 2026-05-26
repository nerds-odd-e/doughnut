# CLAUDE.md

Guidance for Claude Code and other AI coding agents working in this repository.

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

Start with `.cursor/agent-map.md` for repo navigation, generated API guidance, focused commands, service assumptions, and default indexing notes.

Run repo tooling with `CURSOR_DEV=true nix develop -c …` unless documented otherwise (e.g. Cloud VM). **Git commands do not need the Nix prefix** — run `git` directly.

Repo conventions live in `.cursor/rules/`; use the relevant rule for backend, frontend, E2E, linting, migrations, MCP, or shell-script work.

For local MySQL or Redis failures, inspect `mysql/mysql.log` or `redis/redis.log`; the Nix shell setup is defined by `process-compose.yaml` and `scripts/shell_setup.sh`.

Planning notes belong in `ongoing/`, which is excluded from default indexing. Read it explicitly only for active planning or history tasks.
