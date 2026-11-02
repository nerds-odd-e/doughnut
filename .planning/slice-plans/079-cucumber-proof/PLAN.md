# Publish ordinary Cucumber observations beside Gherkin intent

**Identity:** SEED-096#cucumber-proof
**Source:** [authorized story](../../seeds/SEED-096-cucumber-proof.md#cucumber-proof)
**Source review:** [independent source-only commitments](source-review.md)

## Goal and scope

Ordinary focused, tagged, retried, and full Cypress/Cucumber invocations produce
Open Dough version-1 proof for executed scenarios under declaring Gherkin
locations. Commit real observations from meaningful ordinary Donut tests with
the integration. Preserve existing output, diagnostics, exit meaning, retries,
concurrency, composed callbacks, and runner/resource ownership.

Project-owned code and configuration implement the
[installed release contract](../../../.agents/skills/dough-test-optimization/references/cucumber-proof.md).
Managed Open Dough files, product behavior, and existing scenario coverage are
outside the change. No whole-product suite runs solely to collect proof.

Failure policy: [Open Dough's failing and flaky test policy](/Users/terryyin/git/open-dough/AGENTS.md#failing-and-flaky-tests)
applies as written to execution, every delegation, and CI repair; Donut guidance
also applies. Known failures remain repair obligations under that policy.

## Execution context

- Owned, newly created checkout: `/Users/terryyin/git/doughnut/.worktrees/codex-cucumber-proof`;
  branch `codex/cucumber-proof`; Story Branch Mode; publisher
  `codex-cucumber-proof-20261102`; agent Honoka-chan.
- Starting revision: `2d4d57fabf931193f17754b0d22f59a764bf072a`.
  Admission accepted on origin/main as `e90f3032a97b409128f3eadfb4c963fbdf54652c`;
  execution branch published at that claim. Originating/default integration
  checkout: `/Users/terryyin/git/doughnut`. Authorized landing target:
  `origin` `refs/heads/main` (`nerds-odd-e/doughnut`).
- Recorder increment accepted on `origin/codex/cucumber-proof` as
  `d648487d39f67a47f05072ca21947b9926ef2c4d`; formatter and independent hook
  passed. Its CI remains owned by the live execution observer. Adding the
  Intent proof declaration changes no CI source, workflow, or completion setting.
- `./scripts/run.sh bash scripts/worktree_setup.sh` passed, preserving locked
  dependencies. `./scripts/run.sh node --test e2e_test/config/composeCypressPluginEvents.test.mjs`
  passed both tests from this checkout. CI: push-triggered `ci.yml`, verified
  GitHub workflow 5208694, display name `donut CI`.
- Slice target ~5 minutes; >10 minutes requires reassessment and finer
  decomposition. Browser/service startup and hosted CI waits are stated external
  wait exceptions; they do not excuse unrelated implementation expansion.
  Existing planning authority permits bounded replanning within this outcome.

## Existing solution and architecture

PFE search found no version-1 observation recorder. Reuse the current common
Cypress setup, its composed completion handlers, and the installed preprocessor's
Node task/message transport rather than adding a browser reporter or replacing
runner output. Current ordinary configuration enables no message tracking.
The dependency's supported message overrides and omitted reporting handlers can
enable a message-only transport; its pinned transport names require a real-run
probe before adoption. Preserve already-enabled reporter handlers and targets.

ADRs [0006](../../../docs/adrs/0006-failure-handling-accepted.md) and
[0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) apply:
recording catches failures for its explicitly required silent best-effort
outcome; existing runner failures propagate; disposable E2E data/ports remain
runner-owned. North Star topics impose no additional choice on this tooling.

## Outside-in proof

| Promise | Observing boundary |
| --- | --- |
| Stable original scenario/outline identity, parsed cells and tested bytes | Version-1 files after real preprocessor messages; original AST name/row cells and Git blob comparison |
| Final attempt only, correct attempts/outcome/time/duration including hooks | Retried/failing real Cypress fixture; intermediate retry produces no final observation |
| Executed-only, declared-location scope, dry run | Fixture tag/full/focused selections and explicit resolved dry-run; outside locations produce no observations |
| Run-start commit and source version remain truthful after edits | Temporary Git checkout changed after start/source receipt, then compare observed commit/blob |
| Unique atomic per-run files; completed observations survive interruption | Two live concurrent Node recorder processes expose finished files, then are terminated with another scenario unfinished; real Cypress transport proves the same immediate final-event path |
| Silent best effort, ordinary output/diagnostics/exit/reports preserved | Real runner positive and negative controls with/without adapter, unwritable/no-Git proof paths, existing report and composed cleanup assertions |
| Ordinary Donut route emits useful committed proof | Existing scoped application feature(s) via `pnpm cy:run --spec …`; inspect feature identities/hashes and commit generated proof |

Raw logs and scratch fixture observations stay outside committed proof directories.
Only the declaring locations' real ordinary observations are committed.

## Ordered slices

### 1. Final scenario observations survive ordinary runner events
Type: Behavior
Status: done
Proof: focused Node boundary tests and a real lightweight Cypress fixture
exercise the table's identity, retry, scope, dry-run, provenance, atomicity,
interruption, silent-failure and output/exit promises. Run the changed tooling's
complete focused suite, including existing event-composer tests. No default
product feature configuration is enabled until slice 2.

Behavior: source and runtime task messages from an executed final scenario →
project-owned Node adapter → one compact observation in this run's atomically
updated proof file, with preserved pre-existing runner behavior. A temporary
fixture is a tool consumer, not committed product proof.

Accepted proof (owned checkout, before independent refactor):

- `./scripts/run.sh node --test e2e_test/config/cucumberProofRecorder.test.mjs e2e_test/config/cucumberProofProcess.test.mjs e2e_test/config/composeCypressPluginEvents.test.mjs`
  passed 11 tests. Recorder assertions observe source/commit provenance after
  edits, parsed outline/Rule identities, final retries/duration, latest identity,
  scope/executed-only selection, silent write failure and separate run files.
  The process test reads both live writers' files, then kills those writers
  with unfinished cases and reads the surviving observations. Composer tests
  observe each existing start/completion callback once.
- `./scripts/run.sh node --test e2e_test/config/cucumberProofRunner.test.mjs`
  passed its six-mode real Cypress fixture in 34.94s. The baseline must execute
  passing scenarios and the intentional failure paths; adapted output/exit
  matches those stable reporter/diagnostic signals without new report warnings.
  Assertions inspect five scoped records, final retry/hook outcomes, escaped
  outline cells, commit/blob, dry zero/focused five/tagged two, and retained
  upstream message output. Fixture setup is in `cucumberProofRunnerFixture.mjs`;
  ordinary application configuration remains a slice-2 obligation.
- `./scripts/run.sh node --test scripts/isolated-cypress-openai-mock.test.mjs`
  passed 8 tests; `./scripts/run.sh node --test scripts/isolated-cypress.test.mjs`
  passed 14. Existing run-start private-mock, admission and reset owners remain
  covered. `./scripts/run.sh bash scripts/check_diff_whitespace.sh` passed.
- Consumer trace at base `1581250a75251575f30a32f7f3afcdbb9f39264d` completed
  across 4,070 files with public effects `addCucumberPreprocessorWithProof`,
  `createCucumberProofRecorder`, `composeCypressPluginEvents`. `common.ts` and
  transitive `ci.ts` retain the composer; callback/fixture/guard proof above
  observes that effect. `package.json` retains its existing composer command;
  the released contract is checked against format assertions. Other matches
  are lexical references to unexported generic helper/fixture terms, not
  consumers. New ordinary adapter wiring/test registration is owned by slice 2.

Independent refactor accepted: source/AST indexing now lives in
`cucumberProofCases.mjs`, computing each feature hash/index once; recorder API
and lexical URI behavior remain unchanged. The same 11-test focused command
and real six-mode runner command passed again (35.90s), with evidence at
`/tmp/nix-shell.SgFOfi/donut-cucumber-proof-runner-8J56v2`. Unchanged guard proofs
remain applicable. Full trace and 144 dispositions are retained outside Git at
`/private/tmp/donut-cucumber-refactor-consumer-trace.md`; no uncovered consumer
remains. Source/core modules are 78/112 lines before coordinator formatting.

Coordinator preparation repaired five lint findings without changing the
recorder contract; test output normalization now uses Node's native ANSI-strip
utility. `./scripts/run.sh node --test e2e_test/config/cucumberProofRecorder.test.mjs e2e_test/config/cucumberProofProcess.test.mjs e2e_test/config/composeCypressPluginEvents.test.mjs e2e_test/config/cucumberProofRunner.test.mjs`
then passed all 12 tests, including the six-mode real runner (42.24s). The failed
formatter attempt committed/published nothing; its intervening repair requires
the guarded formatter to run again before delivery.

### 2. Ordinary Donut runs expose proof beside declared Gherkin
Type: Behavior
Status: done
Proof: shared common setup configures the adapter and declaring Gherkin proof
directory; focused and full fixture runs cover that shared integration. Run
meaningful existing Donut feature(s) through the owning `pnpm cy:run` wrapper,
inspect their real version-1 proof, run relevant E2E/tooling checks, and commit
the observations. Hosted CI validates the actual published revision; explicitly
invoke Dough Land after execution/review settlement and await applicable trunk CI.

Behavior: ordinary Donut Cucumber invocation → shared setup → executed scoped
scenarios update published proof without changing the invocation's own result.

Accepted proof before final independent refactor:

- `./scripts/run.sh pnpm test:cucumber-proof` passed 15 tests: all six new
  mixed-status outcomes, prior recorder/process promises, and six actual
  Cypress modes. An initial failure exposed nondeterministic inter-spec
  discovery order; comparison now asserts the exact spec set and each spec's
  ordered reporter/error rows, without changing scheduling or warning checks.
- `./scripts/run.sh env -u SPRING_DATASOURCE_URL -u DB_URL -u SPRING_FLYWAY_URL -u INPUT_DB_URL -u SERVER_PORT -u LOCAL_LB_BACKEND -u LOCAL_LB_VITE_UPSTREAM -u LOCAL_LB_LISTEN_PORT -u FRONTEND_DEV_PORT -u FRONTEND_BACKEND_ORIGIN -u SUT_RUNTIME_TARGET pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_creation.feature`
  passed all 9 scenarios, zero skipped/failed (20s). The owning isolated wrapper
  provisioned its recorded database/ports and completed cleanup.
- `./scripts/run.sh node --test e2e_test/config/composeCypressPluginEvents.test.mjs`
  passed all 3. `common.ts` registers the adapter once; `ci.ts` and the default
  `cypress.config.ts` inherit it; `lint:all` invokes the tested new command.
- Reading `e2e_test/proof/20261102T030305438Z-d4182692-ae90-4ae0-981d-a993fec40c59.json`
  confirms valid version 1, nine exact feature scenario names, passed/attempts 1,
  positive durations, commit `d648487d39f67a47f05072ca21947b9926ef2c4d`, and
  blob `f6bd4975da729f087a51a5817fa8575aa1b9dd70`; it is not ignored. These are
  real ordinary observations, not fixture proof. Full trace and 67 dispositions
  plus transitive ordinary/CI consumers are retained at
  `/private/tmp/donut-cucumber-slice2-consumer-trace.md` and
  `/private/tmp/donut-cucumber-slice2-consumer-dispositions.md`.

Independent refactor: already clean, no edits; 67 dispositions plus three
transitive consumers checked, accepted proofs reused. Project whitespace check passed.

## Current decisions

- Slice 1 sizing reached about 10 active minutes, excluding external waits.
  The bounded exception finished the existing real-run harness and tracing;
  it exposed lexical/physical URI coordinates missing from the first fixtures.
  No expanded capability was authorized by that exception.
- Interruption proof uses the actual Node writer processes: their parent
  reads both completed files before terminating the live children, then
  confirms both survive and the unfinished case contributes nothing. Combined
  with real Cypress final-event transport proof, this observes the requested
  durable-run behavior directly. It replaces the plan's unnecessary choice
  of killing a whole Cypress/browser tree; the source outcome is unchanged.
- Initial real-run transport probe: baseline/enabled exit 2 with matching
  3 passed, 2 intentionally failed, 1 skipped; source bytes/parsed cells and
  retry flags matched. Dry-run exit 0 emitted synthetic passed attempts.
  Literal commands, payloads and logs: `/private/tmp/donut-cucumber-proof-probe-YXYlus`.
  Later slice evidence above proves recorder and run-start composition.
- Enable message transport only for ordinary terminal runs with an applicable
  proof declaration. Keep existing active reporting intact. Explicit resolved
  dry-run suppresses observations even if upstream emits synthetic passed steps.
- Capture HEAD at run start and hash the compiled source envelope, never the
  post-run feature file. Keep runner IDs in memory only.
- One Node process owns each run's unique file; flush after final scenarios,
  with temporary files in the same directory ending in a non-JSON suffix.
- Local proof host: macOS, initial load 3.72/4.46/4.58, reclaimable inactive
  memory ~20 GB, no competing Cypress/Gradle/E2E processes observed. Sequence
  one lightweight Electron probe/test process and one scoped application batch;
  command owner rechecks if load, contention or failures change this basis.

## Learnings

- The preprocessor gates Node tasks on report tracking; simply enabling messages
  also activates reporting diagnostics. Use the supported omitted reporting
  lifecycle handlers for the otherwise-dormant fallback and leave existing
  reporting configurations unchanged.
- Dry runs can report synthetic passed steps, so statuses alone do not prove
  execution.
- A raw Mocha beforeEach failure emits no Cucumber scenario start/finish, as
  does its subsequent skipped scenario. These unexecuted Cucumber scenarios
  retain earlier observations; their raw runner diagnostics and exit remain
  intact. Cucumber case hooks are included in observed step results.
- Cypress URIs are relative to its original lexical project root while the
  compiler can use physical paths. Resolve in those original coordinates
  before mapping a path beneath that root to its canonical root. The unit
  fixture now generates the real compiler's URI shape.
- A matching failure count can hide fixture compilation failures. The real
  runner test now requires actual passing scenario signals and each intended
  error before interpreting its exit status or comparing recorder output.

## Story obligations

### G1. Ordinary product integration remains for its receiving slice
Reported: slice 1 — "Ordinary Donut configuration, proof declaration, meaningful product proof, and CI test-script integration remain for slice 2."
Story clause: "Ordinary focused and full runs record only executed scenarios within the declaring locations, in version-1 proof files. Dry runs record nothing."
Disposition: proved by slice 2: common.ts single registration, open-dough.json declaration, registered 15-test suite, ordinary nine-scenario wrapper pass and the inspected real proof file.

### G2. Remaining outcome-rank observations
Reported: slice 1 — "ambiguous, undefined, pending, and unknown outcome variants use the generic rank map but have no dedicated observing test yet."
Story clause: "A retried outline records its original Gherkin name, parsed example cells, final attempt outcome, all attempts' duration, and final completion time."
Disposition: proved by slice 2: cucumberProofRecorder.test.mjs data-driven mixed-status cases observe ambiguous, undefined, pending, skipped, passed and unknown; prior failed/hook and all-skipped observations remain green in the 15-test suite.


### G3. Post-implementation delivery belongs to the coordinator continuation
Reported: slice 2 — "Source-only review includes landing, publication, final CI and reporting duties whose trigger follows delivered implementation; these are not yet fulfilled observations."
Story clause: "Readers of Donut's published Gherkin can see the latest observed scenario outcome, when it ran, the run-start commit, and the tested feature version through Open Dough Intent."
Disposition: no user cost "Readers of Donut's published Gherkin can see the latest observed scenario outcome, when it ran, the run-start commit, and the tested feature version through Open Dough Intent.": these rows specify the subsequent coordinator delivery phase, not omitted product behavior. They remain mandatory task duties: normal-hook proof commit, retrospective/completion, explicit Dough Land to origin/main, actual final main CI, containment and final report. Implementation acceptance asserts none of those future results; the coordinator retains the checkout and live observer through fulfillment.
Commitment: C03, C22, C31, C32, C33, C34, C35, C36, C37, C38, C39, C40, C48, C50, R18, R61, X26, X27
