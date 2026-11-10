# Source-only review

Launch: collaboration.spawn_agent, task `/root/source_review`, fork_turns `none`. Input: original selected story Goal, Scope, key examples and Evaluation copied in the launch, with explicit instructions to use only those inputs, read no files and change nothing. Returned numbered commitments retained below verbatim. No human amendment.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| C01 | Contributors and their coding agents can enter the repository development environment through `./scripts/run.sh`. | slice 1 | observed: execution-evidence.md slice 1: real cold/cached entry and public setup pass |
| C02 | Repository entry works from a non-interactive shell. | slice 1 | observed: execution-evidence.md slice 1 real Nix probe: env-limited zsh -c cold/cached exits 0 |
| C03 | Repository entry requires no hand-exported `NIX_CONFIG`. | slice 1 | observed: execution-evidence.md slice 1 real Nix probe: host experimental features explicitly disabled, wrapper enables its own |
| C04 | Mocked-OpenAI E2E features run without hand-exporting `OPENAI_API_TOKEN`. | slice 2 | |
| C05 | Routine setup commands work without repeating environment workarounds in agent briefs. | slice 1 | observed: execution-evidence.md slice 1 public ./scripts/run.sh bash scripts/worktree_setup.sh pass |
| C06 | Routine verification commands work without repeating those workarounds. | slice 2 | |
| C07 | `run.sh` enables required Nix features during initial environment preparation, even when machine configuration disables them. | slice 1 | observed: scripts/test/run.sh.test test-populates-recorded-environment-once-then-reuses-it and execution-evidence.md real cold exit 0 |
| C08 | `run.sh` enables required Nix features during subsequent command launches under that same condition. | slice 1 | observed: scripts/test/run.sh.test test-populates-recorded-environment-once-then-reuses-it and execution-evidence.md real cached exit 0 |
| C09 | `run.sh` preserves command arguments. | slice 1 | observed: scripts/test/run.sh.test test-populates-recorded-environment-once-then-reuses-it spaced and glob arguments |
| C10 | `run.sh` preserves working directory. | slice 1 | observed: scripts/test/run.sh.test same population/reuse test observes sub dir working directory |
| C11 | `run.sh` preserves exit status. | slice 1 | observed: scripts/test/run.sh.test test-propagates-child-exit-status observes 7 |
| C12 | `run.sh` preserves execution inside an existing Nix shell. | slice 1 | observed: scripts/test/run.sh.test test-runs-directly-inside-nix observes child output and direct execution |
| C13 | `run.sh` preserves execution on a machine without Nix. | slice 1 | observed: scripts/test/run.sh.test test-runs-directly-without-nix observes child output |
| C14 | The E2E application receives a non-secret placeholder key when `OPENAI_API_TOKEN` is unset. | slice 2 | |
| C15 | That fallback permits refinement through the existing OpenAI mock. | slice 2 | |
| C16 | That fallback permits question generation through the existing OpenAI mock. | slice 2 | |
| C17 | A supplied real token retains its exact value wherever the selected launch permits it. | slice 2 | |
| C18 | `pnpm e2e:hold` withholds the caller’s real token by default. | slice 2 | |
| C19 | `--paid-openai` retains its existing explicit opt-in behavior. | slice 2 | |
| C20 | Existing scenario tags continue to participate in choosing the OpenAI endpoint. | slice 2 | |
| C21 | Existing service configuration continues to participate in choosing the OpenAI endpoint. | slice 2 | |
| C22 | Repository-entry reproductions pass without manual exports. | slice 1 | observed: execution-evidence.md slice 1 real cold/cached Nix command exits 0 |
| C23 | Mocked-OpenAI reproductions pass without manual exports. | slice 2 | |
| C24 | The delivered response is recorded at DD-439. | slice 2 | |
| C25 | The release is recorded at DD-439. | slice 2 | |
| C26 | Dated observations are retained. | slice 2 | |
| Q01 | The audience is contributors and coding agents working for them. | slice 1 | observed: docs/development-setup.md non-interactive contributor/agent wrapper recipe |
| Q02 | The placeholder applies to the E2E application when the token is unset. | slice 2 | |
| Q03 | Preservation of a supplied real token is conditional on the selected launch already permitting it. | slice 2 | |
| Q04 | The placeholder satisfies the mock client’s credential precondition; it does not select the endpoint. | slice 2 | |
| Q05 | Default withholding during a held observation prevents unintended paid calls. | slice 2 | |
| Q06 | Nix remains an installation prerequisite for the Nix-backed repository environment. | slice 1 | observed: scripts/run.sh preserves installed-Nix detection and direct fallback |
| Q07 | Documented local infrastructure remains an installation prerequisite. | slice 2 | |
| Q08 | A later agent execution may provide recurrence evidence. | slice 2 | |
| E01 | Start with a shell that has not loaded `~/.zshrc`. | slice 1 | observed: execution-evidence.md real Nix probe zsh -c with env limited to HOME/PATH/NIX_CONFIG |
| E02 | Start with Nix configuration that enables neither `nix-command` nor `flakes`. | slice 1 | observed: execution-evidence.md real Nix probe sets experimental-features empty |
| E03 | Before the environment is cached, `./scripts/run.sh true` exits successfully without a caller-provided feature-enabling export. | slice 1 | observed: execution-evidence.md real Nix cold exit 0 in owned temporary repository |
| E04 | After the environment is cached, the same invocation exits successfully under the same constraint. | slice 1 | observed: execution-evidence.md real Nix cached exit 0 with one profile symlink |
| E05 | With documented MySQL/Redis prerequisites and an unset token, launch `assimilation/note_refinement.feature` through `./scripts/run.sh pnpm cy:run --spec`; refinement completes against the runner-owned mock. | slice 2 | |
| E06 | Under those prerequisites, launch `ai_generated_recall_questions/question_contest.feature` through the same entry point; question replacement completes against the runner-owned mock. | slice 2 | |
| E07 | The E2E invocation shuts down services it owns. | slice 2 | |
| E08 | Inspect `sut.log` for credential failure as supporting diagnosis alongside the passing journeys. | slice 2 | |
| E09 | Given a supplied token and a launch that permits it, the E2E application resolves that exact value. | slice 2 | |
| E10 | A held stack without `--paid-openai` resolves the non-secret fallback. | slice 2 | |
| E11 | A held stack with `--paid-openai` and a supplied token resolves the supplied value. | slice 2 | |
| E12 | Credential precedence may be demonstrated using a synthetic token without paid calls. | slice 2 | |
| X01 | No change to Production credential policy. | slice 2 | |
| X02 | No change to persistent Development credential policy. | slice 2 | |
| X03 | No automatic real-service access. | slice 2 | |
| X04 | No service-isolation redesign. | slice 2 | |
| X05 | No repair of other shell settings without a demonstrated failure. | slice 1 | observed: scripts/run.sh diff only additive feature options; docs only wrapper recipe |
| X06 | Entering the repository environment does not promise to start persistent Development application services. | slice 1 | observed: scripts/run.sh and public worktree setup preserve CURSOR_DEV hook service policy |
| X07 | Entering the repository environment does not promise to start persistent infrastructure services. | slice 1 | observed: scripts/run.sh and public worktree setup preserve CURSOR_DEV hook service policy |
| X08 | Waiting for an unrelated later agent execution is outside story delivery. | slice 2 | |
