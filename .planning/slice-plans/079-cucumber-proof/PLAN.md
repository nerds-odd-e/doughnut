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
| Unique atomic per-run files; completed observations survive interruption | Concurrent recorder processes and a real Cypress run interrupted after a finished scenario |
| Silent best effort, ordinary output/diagnostics/exit/reports preserved | Real runner positive and negative controls with/without adapter, unwritable/no-Git proof paths, existing report and composed cleanup assertions |
| Ordinary Donut route emits useful committed proof | Existing scoped application feature(s) via `pnpm cy:run --spec …`; inspect feature identities/hashes and commit generated proof |

Raw logs and scratch fixture observations stay outside committed proof directories.
Only the declaring locations' real ordinary observations are committed.

## Ordered slices

### 1. Final scenario observations survive ordinary runner events
Type: Behavior
Status: planned
Proof: focused Node boundary tests and a real lightweight Cypress fixture
exercise the table's identity, retry, scope, dry-run, provenance, atomicity,
interruption, silent-failure and output/exit promises. Run the changed tooling's
complete focused suite, including existing event-composer tests. No default
product feature configuration is enabled until slice 2.

Behavior: source and runtime task messages from an executed final scenario →
project-owned Node adapter → one compact observation in this run's atomically
updated proof file, with preserved pre-existing runner behavior. A temporary
fixture is a tool consumer, not committed product proof.

### 2. Ordinary Donut runs expose proof beside declared Gherkin
Type: Behavior
Status: planned
Proof: shared common setup configures the adapter and declaring Gherkin proof
directory; focused and full fixture runs cover that shared integration. Run
meaningful existing Donut feature(s) through the owning `pnpm cy:run` wrapper,
inspect their real version-1 proof, run relevant E2E/tooling checks, and commit
the observations. Hosted CI validates the actual published revision; explicitly
invoke Dough Land after execution/review settlement and await applicable trunk CI.

Behavior: ordinary Donut Cucumber invocation → shared setup → executed scoped
scenarios update published proof without changing the invocation's own result.

## Current decisions

- The real-run transport probe passed in this owned checkout: ordinary
  `PROBE_MODE=baseline|enabled|dry pnpm exec cypress run --project <checkout>
  --config-file <scratch>/cypress.config.cjs --browser electron`, through
  `./scripts/run.sh node`, exercised two tiny features. Baseline and enabled
  runs both exited 2 with the same 3 passed, 2 intentionally failed, 1 skipped
  scenarios and matching reporter/error text after duration/port normalization.
  Received source bytes matched consumed files, outline cells decoded pipes
  and backslashes, retries emitted attempts 0/1 and final flags true/false.
  Dry-run exited 0 while emitting synthetic passed attempts. Inspectable
  literal commands, payloads, scripts and logs are retained outside Git at
  `/private/tmp/donut-cucumber-proof-probe-YXYlus`; all checkout scratch was
  removed. This settles transport, not the unwritten recorder or run-start
  composition: slices own their discriminating proof.
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

## Plan review

Refinement is not needed: two cohesive tooling outcomes, one recorder boundary
followed by its ordinary product consumer. The decisive transport premise was
settled by the real-run probe; no remaining slice-specific concern was identified.

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
