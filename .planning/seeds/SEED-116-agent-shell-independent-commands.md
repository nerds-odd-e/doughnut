---
id: SEED-116
status: active
planted: 2026-11-10
scope: story
---

# Run Donut's repository commands from a non-interactive shell

<a id="run-repository-commands-without-interactive-shell-setup"></a>

### Run Donut's repository commands without interactive-shell setup

**Identity:** SEED-116#run-repository-commands-without-interactive-shell-setup
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary and goal.** Contributors and the coding agents working for them
can start Donut's development environment and run its mocked-OpenAI E2E
features from a shell that has not loaded `~/.zshrc`, without first exporting
`NIX_CONFIG` or `OPENAI_API_TOKEN` by hand.

**Problem and evidence.** [DD-439](../../DonutRetrospectiveFindings.md#dd-439)
records SEED-113#assimilate-notes-with-a-clear-default-and-result
(2026-11-09, Open Dough 0.3.92): `./scripts/run.sh bash scripts/worktree_setup.sh`
refused to start because flakes were disabled, and two mocked-OpenAI E2E
features failed with `This request requires apiKey` until a placeholder was
exported. Every later brief had to carry both exports, and later Dough Intent
Coverage commands for SEED-113 and SEED-114 still prefixed the flakes
setting by hand.

**Scope.** `./scripts/run.sh` enables the Nix features it needs itself.
Stacks that serve mocked OpenAI start with a non-secret placeholder key when no
key is set. A real `OPENAI_API_TOKEN` in the environment, and the existing
`--paid-openai` choice, keep their current meaning. Out of scope: other
interactive-shell settings that no failed command has shown to be needed.

**Key examples.**

- In `env -i HOME="$HOME" PATH="$PATH" zsh -c ...`, `./scripts/run.sh true`
  succeeds on a machine whose Nix configuration does not enable flakes.
- From the same kind of shell, a mocked-OpenAI E2E feature that exercises
  refinement or question generation passes, and `sut.log` shows no
  `requires apiKey` error.
- With a real `OPENAI_API_TOKEN` exported, the stack receives that value, not
  the placeholder.

**Evaluation.** The two reproductions above pass without exports, and a
retrospective of the next agent execution needs no environment exports in its
briefs. Record the delivered response and release at DD-439.
