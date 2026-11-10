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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/091-run-repository-commands-without-shell-setup/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"2bea8292a7a8481bdbb4a7ad99ced2592c996a63283c8706d81a79861e6cc883","plan":"92cf26fc66c1b57b0fdbdae5bab0beaaea18d934842781063eaf24ec1e4e2938"}}
```

**Goal.** Contributors and the coding agents working for them can enter
Donut's repository development environment through `./scripts/run.sh` and run
its mocked-OpenAI E2E features from a non-interactive shell, without exporting
`NIX_CONFIG` or `OPENAI_API_TOKEN` by hand. Routine setup and verification
commands should work without repeating environment workarounds in agent briefs.

**Problem and evidence.** [DD-439](../../DonutRetrospectiveFindings.md#dd-439)
records SEED-113#assimilate-notes-with-a-clear-default-and-result
(2026-11-09, Open Dough 0.3.92): `./scripts/run.sh bash scripts/worktree_setup.sh`
refused to start because flakes were disabled, and two mocked-OpenAI E2E
features failed with `This request requires apiKey` until a placeholder was
exported. Every later brief had to carry both exports, and later Dough Intent
Coverage commands for SEED-113 and SEED-114 still prefixed the flakes
setting by hand.

**Scope.** `./scripts/run.sh` enables the Nix features required by both its
initial environment preparation and subsequent command launches, even when
the machine's Nix configuration leaves them disabled. It preserves command
arguments, working directory, exit status, and execution inside an existing
Nix shell or on a machine without Nix.

The E2E application has a non-secret placeholder key when `OPENAI_API_TOKEN`
is unset, allowing the existing OpenAI mock to serve refinement and question
generation. A supplied real token retains its value wherever the selected
launch already permits it. `pnpm e2e:hold` continues to withhold the caller's
real token by default; `--paid-openai` keeps its existing explicit opt-in.
This restriction prevents unintended paid calls during a held observation.
The placeholder supplies the mock client's credential precondition; existing
scenario tags and service configuration still choose the OpenAI endpoint.

Scope limits: no changes to Production or persistent Development credential
policy, no automatic real-service access, no service-isolation redesign, and
no repair of other shell settings without a demonstrated failure. Nix and the
documented local infrastructure remain installation prerequisites. Entering
the repository environment does not promise to start the persistent
Development application or infrastructure services.

**Key examples.**

- Given a shell that has not loaded `~/.zshrc` and a Nix configuration without
  `nix-command` or `flakes`, invoke `./scripts/run.sh true` before and after
  its environment is cached → both invocations exit successfully without a
  caller-provided feature-enabling export.
- Given the documented MySQL/Redis prerequisites and an unset
  `OPENAI_API_TOKEN`, invoke `./scripts/run.sh pnpm cy:run --spec` with
  `assimilation/note_refinement.feature` and
  `ai_generated_recall_questions/question_contest.feature` → refinement and
  question replacement complete against the runner-owned mock, and the
  invocation shuts down its owned services. Inspect its `sut.log` for the
  credential failure as supporting diagnosis, alongside the passing journeys.
- Given a supplied real token and a launch that permits it → the E2E
  application resolves that exact value. Given a held stack without
  `--paid-openai` → its application resolves the non-secret fallback; with
  the flag and a supplied token → it resolves the supplied value. Credential
  precedence can be demonstrated with a synthetic value without paid calls.

**Evaluation.** The repository-entry and mocked-OpenAI reproductions pass
without the manual exports. Record the delivered response and release at
[DD-439](../../DonutRetrospectiveFindings.md#dd-439), retaining its dated
observations. A later agent execution can supply recurrence evidence; waiting
for that unrelated execution is not part of this story's delivery.
