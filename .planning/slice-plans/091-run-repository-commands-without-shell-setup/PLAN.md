# Run repository commands without interactive-shell setup

**Source:** [selected story](../../seeds/SEED-116-agent-shell-independent-commands.md#run-repository-commands-without-interactive-shell-setup)
**Identity:** SEED-116#run-repository-commands-without-interactive-shell-setup

## Goal and scope

Contributors and agents can enter the repository environment and run the
reported mocked-OpenAI workflows from a non-interactive shell without manual
Nix-feature or OpenAI-token exports. The evaluator observes successful command
execution and the real refinement/question-replacement journeys.

Include both Nix environment population and reuse, an E2E-only non-secret
credential default, supplied-token precedence, and the existing held-stack
paid-call choice. Preserve command forwarding and service ownership. Installed
Nix and the documented local MySQL/Redis prerequisites remain assumptions.
Exclude Production/Development credential changes, starting persistent
Development services, new real-service calls, and unrelated shell setup.
Preparation probes below are reverted observations, not delivered slices.

## Existing solutions and decisions

- Change `scripts/run.sh`, the existing per-worktree Nix profile owner. Both
  `nix develop` calls need explicit additive `nix-command flakes` enablement;
  use Nix's `--extra-experimental-features` option rather than a shell-wide
  configuration export. Keep profile caching and direct execution paths.
- Change the existing `e2e` document in
  `backend/src/main/resources/application.yml`: its
  `spring.openai.token` already resolves `OPENAI_API_TOKEN` with a default.
  Supply a non-secret default there. This is the configuration owner of the
  client's credential precondition; no new runner environment policy or
  credential resolver is needed. Default application and `dev` configuration
  retain their existing credential behavior.
- Reuse scenario-selected mock routing and owned E2E lifetime. The separate
  `scripts/sut-start-spawn.mjs` owner withholds the inherited token for an
  ordinary hold and passes it for `--paid-openai`; retain that choice.
  CI's secret injection is a caller input, not a local setup solution.
- PFE considered the runner, Nix hook, all profile defaults, CI injection and
  held-stack filtering. Repeating exports in briefs or changing user Nix
  configuration leaves the reported prerequisite with every caller; the two
  existing owners can satisfy it locally without a new abstraction.
- Follow [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  probes and acceptance use only this checkout's disposable E2E allocation;
  keep persistent Development and peer data untouched. Follow
  [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage):
  prevent the known missing mock credential and preserve ordinary loud
  failures. The [North Star](../../NORTH-STAR.md) requires no new direction;
  this work changes no product domain or environment boundary.

## Outside-in proof ownership

| Promise | Owner and observation |
| --- | --- |
| Commands launch with host Nix features disabled, for a fresh and reused profile | Slice 1: extend the real-runner Bach fixture so the external Nix stand-in requires feature enablement on every invocation; retain population/reuse assertions. Observe real Nix in a clean shell with features explicitly disabled |
| Arguments, working directory, child exit status, existing-shell and no-Nix paths retain their meaning | Slice 1: existing six `scripts/test/run.sh.test` boundary cases, with expectations aligned to the explicit options |
| Unset token permits real refinement and question replacement through the owned mock | Slice 2: run both named Cypress features through the real wrapper, with no token or feature-enabling export. Observe 7 passing scenarios and owned cleanup; inspect only that invocation's log segment for credential diagnosis |
| Supplied token takes precedence and the default is E2E-specific | Slice 2: focused configuration-contract JUnit cases load the production YAML through Spring ConfigData with the actual `e2e` profile, once unset and once with a synthetic supplied token; also preserve the `dev` default. Do not let test `application.properties` supply its own OpenAI key |
| Held stack withholds real credentials by default and permits them with the existing paid choice | Slice 2: retain both token-filtering cases in `scripts/e2e-runner-hold.cases.mjs`; combine their observed child environment with the E2E configuration-contract cases. Use synthetic values, with no paid calls |
| Reported workaround has a recorded delivery response | Slice 2: update DD-439 with accepted implementation/proof evidence and the known release/revision, preserving original dates and observations; do not claim a later execution has already demonstrated non-recurrence |

The positive application journeys are the main credential proof. The log
inspection is supporting diagnosis, not a permanent absence test. Existing
backend tests inject `test_token_for_openai`, so their passing alone cannot
prove the E2E default.

## Decisive premises and preparation observations

Observed on 2026-11-10 in this owned workspace at
`c2824d14fc428ed43da5f3a2a24a9d0985bf91c7`, approximately 11:19–11:27 UTC.
All scratch source/test edits and temporary probe files were reverted before
writing this plan. Results do not establish delivery of the future change.

| Premise / consuming operation | Form, result, and remaining boundary |
| --- | --- |
| Disabled host features reproduce the runner failure; additive command options are accepted by installed Nix | `env -i HOME="$HOME" PATH="$PATH" NIX_CONFIG='experimental-features =' zsh -c './scripts/run.sh true'` failed with flakes disabled. The same command exited 0 with a scratch addition of `--extra-experimental-features "nix-command flakes"` to both runner Nix calls. Plain `env -i` passed on this host because its Nix configuration already enables features; that alone is not the required reproduction |
| Cold population and cached reuse can be covered at the existing runner boundary | `CURSOR_DEV=true nix develop -c bash scripts/test/run.sh.test` passed 6/6 before changes. A disposable version of its external Nix fixture requiring the explicit options failed 4/6 cases against the original runner and passed 6/6 with the scratch runner change. The two direct-execution cases passed in both. This settles fixture discrimination; slice 1 must retain the new requirement as a real regression |
| The reported mocked journeys fail at the credential precondition and are repaired by the profile default | `env -i HOME="$HOME" PATH="$PATH" NIX_CONFIG='extra-experimental-features = nix-command flakes' CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/assimilation/note_refinement.feature,e2e_test/features/ai_generated_recall_questions/question_contest.feature` ran against `doughnut_e2e_wt_73e50aae670241ba812a3167726033cc`, origin `http://127.0.0.1:62681`. Original YAML: 0/7 passing, exit 7, log contained `This request requires apiKey or workloadIdentity`. Only a scratch E2E default `donut-e2e-placeholder`: 7/7 passing, exit 0, test phase 45s, no credential error after that invocation's startup. Both invocations returned through owned cleanup. Nix enabling in this diagnostic command isolates the second failure; final acceptance removes that workaround after slice 1 |
| A profile default preserves supplied-token precedence without changing Development | A disposable Java main on Gradle's `sourceSets.main.runtimeClasspath` ran `ConfigDataEnvironmentPostProcessor.applyTo` against the source `application.yml`, explicit `e2e`/`dev` profiles, and isolated property sources. Launched by `CURSOR_DEV=true nix develop -c python3 -`, it observed original E2E unset → empty; scratch E2E unset → `donut-e2e-placeholder`; scratch E2E supplied `synthetic-supplied-token` → that exact value; scratch dev unset → empty. It loaded real ConfigData, not a second YAML/placeholder parser. Slice 2 turns this contract into durable proof |
| The held-stack credential gate is independently observable without network credentials | `CURSOR_DEV=true nix develop -c node --test --test-name-pattern='shell.s OpenAI token' scripts/e2e-runner-hold.cases.mjs` passed both cases: ordinary hold child environment omitted the supplied synthetic token; paid hold child environment retained it. The stand-in observes launch filtering, while ConfigData above observes the application's consuming default |

Preparation host choice: local macOS, 16 CPUs, 48 GiB RAM, load about 10,
roughly 2 GiB free and 12.5 GiB inactive memory, with other agent/browser work
visible. Run one two-feature E2E batch at a time; no broad backend run was
launched concurrently. Both batches finished without a resource failure.
Execution must inspect current headroom before its full backend/E2E proof;
these measurements do not describe a remote runner.

## Ordered slices

### 1. Launch repository commands with the required Nix features

Type: Behavior
Status: planned
Proof: The feature-enforcing real-runner Bach cases pass for population and
reuse; the real wrapper exits 0 with disabled host features and no enabling
export. Existing forwarding/direct-execution cases stay green.

Behavior: Given a non-interactive shell and Nix without enabled experimental
features → run `./scripts/run.sh <command>` → prepare or reuse this worktree's
environment and execute that exact command with its normal result.

Add the required additive options to both Nix calls, extending the existing
fixture rather than testing an extracted argument builder. Keep errors and
command output visible. Inspect documentation/agent entry recipes that reach
this wrapper; align any affected recipe without rewriting unrelated direct
`nix develop` installation instructions.

Sizing: about 5 active minutes including fixture updates and cleanup; the
focused script test takes seconds. External Nix population time is an explicit
wait exception. At more than 5 active minutes scrutinize the remaining work;
at 10 stop and finer-decompose unless only that external wait remains.

Safe stopping point: ordinary repository entry works independently; mocked
E2E still needs its credential default until slice 2.

### 2. Run mocked OpenAI journeys with a local E2E credential default

Type: Behavior
Status: planned
Proof: The configuration contract, existing held-stack token cases, full
backend suite, and both real Cypress features establish the one E2E credential
rule and its preserved caller choices.

Behavior: Given the documented infrastructure and an unset token in a
non-interactive shell → invoke the two mocked features through `run.sh` →
refinement and question replacement complete and the runner cleans up its
owned services. A permitted supplied token takes precedence, and an ordinary
hold continues to withhold the caller's paid credential.

Change the existing E2E profile default and add concise tests using Spring's
actual configuration loader without booting an extra application context.
Reuse the synthetic-token held-stack tests; do not change their withholding
contract. Align relevant E2E setup guidance, and record the delivered response
at [DD-439](../../../DonutRetrospectiveFindings.md#dd-439) after proof is accepted.

Sizing: about 5–8 active minutes for one configuration/precedence rule and
its proof, plus the mandatory full-backend and focused E2E runtime wait
exception. Keeping unset/supplied/profile cases together proves one default
resolution contract. Scrutinize after 5 active minutes and stop/finer-decompose
at 10 active minutes rather than extending an implementation attempt.

Safe stopping point: all selected story examples are observable without paid
calls or changed persistent-environment policy.

## Verification and delivery

- Slice 1: `./scripts/run.sh bash scripts/test/run.sh.test`. The external Nix
  fixture must exercise both initial population and reuse with disabled
  features. Repeat the real disabled-host clean-shell command from the
  premise table after the change. Use a scratch repository/cache if needed
  to observe real cold population without deleting another command's state.
- Slice 2: `./scripts/run.sh pnpm backend:test_only`, all backend tests as
  required by the backend skill; `./scripts/run.sh node --test scripts/e2e-runner-hold.cases.mjs`;
  `env -i HOME="$HOME" PATH="$PATH" zsh -c './scripts/run.sh pnpm cy:run --spec e2e_test/features/assimilation/note_refinement.feature,e2e_test/features/ai_generated_recall_questions/question_contest.feature'`.
  Run backend and E2E sequentially because they share checkout build output.
- Trace changed effects with the installed consumer-trace script during
  execution: `scripts/run.sh`, Nix options, `spring.openai.token`, and the
  unset-token precondition. Inspect resulting callers/scenarios and assign
  proof dispositions. Known consumers include setup/hooks, the Bach runner
  fixture, `OpenAiApiConfig`, `OpenAiApiHandler`, question-batch token config,
  Cypress mock routing, and held-stack filtering. Keep scope to the changed
  credential/entry contract, not a full E2E or production-service run.
- Follow Execute Plan's required Jidoka, fresh independent
  dough-post-change-refactor agent, coordinator's single
  `./scripts/run.sh pnpm format:changed`, plan update, agent commit with the
  independent check-only hook, and push. No API/schema change is expected;
  API regeneration applies only if execution actually changes such a contract.
  Preserve failures under the workflow's repair policy.

## Concern review

Slice-plan refinement was not needed: two independent entry/credential proof
loops each change an existing owner, with no preparatory Structure or special
case accumulation. The credential variations share Spring's existing default
resolution rule. No remaining scope, proof-route, decisive-premise, ADR or
slice-specific concern was found in this review. No pre-Take human decision
or early probe is required.
