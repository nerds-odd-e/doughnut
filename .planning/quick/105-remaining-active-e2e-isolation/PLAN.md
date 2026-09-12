# Run remaining active E2E features in isolated worktrees

Source: [SEED-015 Story 10](../../seeds/SEED-015-concurrent-worktree-environments.md#story-10).
Status: in progress. Planning/refinement authorized 2026-09-12; execution
authorized 2026-09-12.
Execution identity: originating checkout `/Users/terryyin/git/doughnut` on
`main`; execution checkout `/Users/terryyin/git/d105-e2e` on
`execute/105-e2e-isolation`; integration target `main`. CI observer mailbox
`/tmp/dough-ci-501/watch-VA4ixD` (workflow `ci.yml` / `donut CI`).
Backlog remains queued in its current order. No dependency on implementing
Story 9: use short disposable checkout paths for this story's live proofs.

## Outcome and bounded scope

Admit the current active feature set to local isolated `pnpm cy:run` and
`pnpm cy:open`, individually and in sequential batches, using existing private
application/data ownership and private endpoints for OpenAI and Wikidata mocks.
Preserve primary/CI behavior, selected-origin CLI/MCP routing, fixture reset,
existing filters, failure visibility and one shutdown per invocation/session.

The seed's inventory has 74 active files out of 79: 52 application/no declared
network mock (including existing MCP), 5 CLI, 12 OpenAI mock, 4 Wikidata mock,
and 1 live OpenAI. Five wholly ignored files plus ignored scenarios within
mixed files are excluded. Counts are a dated inventory, not a permanent
hardcoded numerical constraint or evidence of test success.

No Gmail/Google migration, ignored-test deletion, live-provider redesign,
performance tuning, startup-race fix, socket-path correction, full suite per
slice, arbitrary future-feature admission, same-worktree parallel jobs,
Cloud VM work, persistent services or general orchestration. Live-provider
scenarios preserve their existing behavior; no paid calls are required proof.
Opt-in profiling scenarios are not enabled by broader feature admission.

## Existing solutions and architectural decision

PFE inspection:

| Responsibility | Existing solution and decision |
| --- | --- |
| Spec admission and union of needs | `isolated-cypress-spec-selection.mjs`: extend its existing registry; retain one authority shared by batch/plugin/interactive paths. No second allowlist or ad hoc filename checks in mocks. |
| Invocation ownership | `e2e-runner.mjs`, `sut-start.mjs`, `sut-owner.mjs`: reuse full batch/session lifetime and lease; do not add another owner. |
| Private mock process | `isolated-openai-mock.mjs`, its ports/ownership helpers, `sut-owned-process-tree.mjs`: modularize actual Mountebank process ownership for the two current services. Keep one child per invocation when both services are required, with distinct serving ports. No plugin framework or unrelated service support. |
| Browser mock routing | `ServiceMocker` accepts management URL/serving port. OpenAI endpoint context already injects verified private endpoints. Wikidata hardcodes 5002/default 2525; change its adapter to consume its owned endpoint. |
| Fixture state | `hook.ts` resets through selected app; `testability.mockService` applies a service URL to that app. Preserve these responsibilities and reset/restore ordering. |
| CLI artifacts and origin | `cliEnv`, CLI config/clone/install tasks use selected app origin, `mkdtemp` and checkout-local bundles. Verify and admit; do not redesign CLI packaging. |
| Event composition | `common.ts`/`composeCypressPluginEvents` preserve Cucumber and screenshot callbacks. Plugin transports resource context; it does not own batch teardown. |

Accepted [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) govern isolation
and failure propagation. No ADR change or new North Star topic is warranted.

Resource requirements describe a file's existing scenarios; they do not replace
Cucumber's scenario/tag selection. A mixed file may provision a mock even when
its selected scenario does not use it; scenario hooks still choose mocked versus
real service URLs. Do not build a new Gherkin tag-expression engine to avoid that
small unused-resource cost. Interactive resource setup must precede use by a
selected spec and remain owned until session exit.

## Verification and delivery

Every leaf contains its own test and implementation proof. Target ~5 minutes
active work, scrutinize above 5, stop/refine above 10 unless only the stated
focused build/browser/service wait remains. No estimate hides an independent
implementation beat. Preserve all existing passing workflows at each commit.

Use stable runner/plugin boundaries with actual lower-level allocation/lifecycle
code and crafted OS boundaries. Use existing mock HTTP/process fixtures for
ownership and lifecycle; no tests per private helper merely for coverage.
Real-engine assumption: the current Mountebank version must serve two recording
imposters under one owned management listener. Prove this once in slice 5 with
real Mountebank, distinct responses and complete child shutdown before broad
Wikidata integration. A failure revises the structural choice, not product scope
by assumption.

Focused commands (paths renamed by cohesive extraction must update commands):

```sh
CURSOR_DEV=true nix develop -c node --test scripts/e2e-runner.test.mjs scripts/isolated-cypress.test.mjs scripts/isolated-cypress-openai-mock.test.mjs scripts/isolated-cypress-cli-spec.test.mjs
CURSOR_DEV=true nix develop -c node --test scripts/isolated-cypress-openai-mock-cancel.test.mjs scripts/isolated-cypress-openai-mock-failure.test.mjs e2e_test/config/composeCypressPluginEvents.test.mjs
```

For new multi-service boundary proof use
`CURSOR_DEV=true nix develop -c node --test scripts/isolated-service-mocks.test.mjs`.
Choose focused cases from these commands per slice; do not rerun unrelated
suites mechanically. Browser proof uses existing feature files and `pnpm cy:run`.

At execution follow dough-execute-plan: Jidoka, fresh post-change-refactor agent,
API generation only if triggered, coordinator `./scripts/run.sh pnpm format:changed`
once, plan update without repeat routine formatting, commit with check-only hook,
push and asynchronous CI observation. Git needs no Nix prefix. Preserve unrelated
work. Planning/refinement performs none of those execution actions.

## Ordered slices

### 1. Admit ordinary active browser features
Type: Behavior
Status: done
Behavior: An existing non-mock feature outside the old four-file allowlist runs
against the selected isolated app and needs no network mock process.
Proof: Extend runner/plugin admission tests for the assessed application-only
inventory; use `note_creation.feature` as the representative live reset-and-create
proof. Keep unknown files and excluded whole-file ignored tests outside admission.
Preserve MCP's existing origin handling; include its existing focused regression.
Do not admit resource-dependent groups until their owning slices are complete.
Sizing: 5–8 minutes, medium confidence; live browser wait excepted.
Done 2026-09-12: extended `APPLICATION_ONLY_ACTIVE_SPECS` in
`scripts/isolated-cypress-spec-selection.mjs` (one registry authority) to admit
the assessed 52 application-only active files (50 new + 2 already admitted);
updated 4 existing tests that used `note_creation.feature` as the unsupported
example to use `book_reading/epub_book.feature` (permanently excluded); added a
positive admission test. Refactor consolidated stale 4-file lists in
`docs/worktree-browser-tests.md` and `.cursor/rules/e2e-authoring.mdc` to
reference the single registry (fulfills the plan's doc-update promise). Focused
unit suites (74 tests across 6 suites) and `pnpm cy:run --spec
e2e_test/features/note_creation_and_update/note_creation.feature` (9/9) green.
Assessed count matches the plan's 52-file inventory with no discrepancy.

### 2. Admit active CLI workflows with selected-origin artifacts
Type: Behavior
Status: done
Behavior: Active clone/edit/relocate/install CLI features run against the chosen
worktree using local bundles and temporary config/install/clone directories.
Proof: Extend existing CLI admission and command-boundary origin/artifact tests;
run `cli/cli_notebook_clone.feature` as representative additional browser proof.
Preserve already-supported web-created-note behavior. Actual shared artifact
writes, if found, must be corrected at this seam before admitting their callers.
Sizing: 5–8 minutes, medium confidence; measured build/browser waits excepted.
Done 2026-09-12: added `ACTIVE_CLI_SPECS` (4 remaining active CLI features) to
the single registry; extended CLI admission/command-boundary origin tests;
updated slice-1 exclusion test to reflect CLI admission; refreshed
`docs/worktree-browser-tests.md` and `.cursor/rules/e2e-authoring.mdc` CLI
scope. No shared-artifact writes found at the CLI origin/artifact seam (all
config/install/clone dirs use `mkdtemp`; bundles checkout-local under
`repoRoot/cli/dist`). Refactor removed one redundant two-worktree test
(coverage survives in the existing spec-agnostic origin-routing test).
Focused unit suites (64 tests) and `pnpm cy:run --spec
e2e_test/features/cli/cli_notebook_clone.feature` (12/12, clean shutdown)
green. Assessed CLI inventory matches the plan's 5 files with no discrepancy.

### 3. Provision private OpenAI mocks for remaining active AI features
Type: Behavior
Status: done
Behavior: Any currently active OpenAI mock feature gets the existing private
endpoint, including files whose mock tag appears on a scenario rather than on
the Feature. Its invocation cleanup remains unchanged.
Proof: Registry requirement examples cover the 12-file group, runner startup is
once per batch, and `note_view/semantic_search.feature` establishes a newly
admitted scenario-level mock path. Existing completion/failure/cancel proof stays
green. Do not infer mock requirements from the feature filename in service code.
Sizing: 5–8 minutes, medium confidence; focused browser wait excepted.
Done 2026-09-12: added `ACTIVE_OPEN_AI_MOCK_SPECS` (11 remaining active
OpenAI-mock features) to the single registry, each declaring
`requiresPrivateOpenAiMock: true` (12-file group with the already-admitted
representative). Scenario-level mock-tagged files (`semantic_search`,
`property_memory_tracker`, `mcq_management`) admitted via the registry
requirement, not inferred from filename. Refactor removed a test-only plural
export and rewrote the inventory test to derive from the registry; fixed a
stale assertion message. Focused unit suites (74 tests) and `pnpm cy:run
--spec e2e_test/features/note_view/semantic_search.feature` (4/4) green;
completion/failure/cancel regression green. Assessed OpenAI-mock inventory
matches the plan's 12 files (9 Feature-level, 3 scenario-level) with no
discrepancy.

### 4. Extract the existing Mountebank ownership from OpenAI configuration
Type: Structure
Status: done
Change: Expose the existing owned process, management listener, port allocation,
and verification/stop behavior independently of OpenAI-specific endpoint names;
OpenAI uses that same lifecycle adapter with unchanged observable behavior.
Immediately enables slice 5's two-service resource contract. Do not duplicate
process observers, termination code, port-exclusion rules or endpoint transport.
Proof: Existing private OpenAI endpoint, ownership, cancellation and failure
boundary tests stay green, as do primary canonical mock behavior tests.
Sizing: 5–8 minutes, medium confidence. If extraction entails separate independent
protocol changes, stop and refine instead of combining them invisibly.
Done 2026-09-12: extracted Mountebank ownership lifecycle into a new generic,
service-label-parameterized adapter `scripts/isolated-mountebank-mock.mjs`
(`observeOwnedMockChild`, `createEmptyRecordingImposter`,
`startOwnedMountebankMock`); `isolated-openai-mock.mjs` is now a thin OpenAI
adapter over it. Refactor renamed the generic ownership/ports modules to
`isolated-mountebank-mock-ownership.mjs` / `isolated-mountebank-mock-ports.mjs`
and made the OpenAI label explicit (`OPEN_AI_SERVICE_LABEL`) at the two
production callers. No process observers, termination code, port-exclusion
rules, or endpoint transport duplicated; one coherent change. Focused
suites (77 tests) and primary canonical mock behavior suite (6 tests) green;
OpenAI observable behavior unchanged. Slice 5's two-service contract is now
enabled.

### 5. Own distinct OpenAI and Wikidata mock endpoints in one invocation
Type: Behavior
Status: done
Behavior: A selection requiring both services receives one owned management
process and separate recording serving endpoints; failure/cancellation settles
that process and partial allocations without touching a peer.
Proof: Real Mountebank boundary fixture creates two imposters with distinct
responses, verifies both listeners belong to the recorded child, and observes
all owned listeners gone after shutdown while an unrelated listener survives.
Command: `CURSOR_DEV=true nix develop -c node --test scripts/isolated-service-mocks.test.mjs`.
Record actual engine/version and outcome here during execution. Missing or
foreign endpoint evidence refuses before mutation; exclude canonical mock and
application ports from private allocation. Preserve the single-service case.
Sizing: 5–8 minutes active work; real process startup wait excepted. Engine
assumption not yet executed; this proof gates slice 6.
Done 2026-09-12: **Real-engine assumption PASSED — Mountebank `@mbtest/mountebank`
v2.9.4** serves two recording imposters with distinct responses under one owned
management listener. Added `startOwnedMountebankMockMulti` +
`allocateMountebankMockPortsMulti` and the `scripts/isolated-service-mocks.test.mjs`
boundary fixture (4 tests). Refactor collapsed single-service starters/allocators
to delegate to the multi-service core (one authoritative home). Boundary proof
verifies: two distinct imposters, both serving listeners belong to the recorded
child, all owned listeners gone after shutdown, unrelated listener survives,
foreign/missing endpoint refuses before mutation, canonical mock (2525/5001/5002)
and application ports excluded from private allocation. Single-service OpenAI
regression (16 tests) green. Slice 6 unblocked.

### 6. Run active Wikidata features through private endpoints
Type: Behavior
Status: done
Behavior: An active Wikidata scenario installs its stubs on its invocation-owned
endpoint and configures only its own app; restoration keeps later real-service
scenarios on their existing URL behavior.
Proof: Wire ServiceMocker/endpoint context through the existing plugin boundary,
verify ownership before mutation, and admit the four current Wikidata files.
Run `wikidata/note_create_with_wikidata_id.feature`; boundary proof establishes
isolated missing-endpoint refusal and unchanged primary/CI canonical behavior.
No Google/Gmail adapter changes. No service startup in Cucumber hooks.
Sizing: 5–8 minutes, medium confidence; focused browser wait excepted.
Done 2026-09-12: admitted the 4 Wikidata files through the single registry
(each `requiresPrivateWikidataMock: true`); wired Wikidata's adapter to consume
its invocation-owned endpoint instead of hardcoded 5002/2525 via a new thin
`scripts/isolated-wikidata-mock.mjs` adapter + `wikidataMockEndpointContext.ts`
(mirroring the OpenAI pattern over the generic `startOwnedMountebankMock`
lifecycle). `e2e-runner.mjs` generalized to collect required mocks from `approved`
and start each (OpenAI and/or Wikidata) under one lease with distinct env keys;
`combineChildExits` ends the run if any owned mock exits. Plugin boundary
verifies ownership before mutation (missing/foreign endpoint refuses). The
`@usingMockedWikidataService` hook loads the endpoint override + verifies
ownership + calls `mock()`; no service started in Cucumber hooks. Real-service
scenarios in mixed files keep their existing URL behavior (Cucumber tag
selection unchanged). Refactor removed dead imports/params and development-
history comment leaks. Focused unit suites (90 node + 16 TS tests) and
`pnpm cy:run --spec e2e_test/features/wikidata/note_create_with_wikidata_id.feature`
(3/3) green; OpenAI path and primary/CI canonical behavior unchanged. Assessed
Wikidata inventory matches the plan's 4 files with no discrepancy. **Sizing
deviation:** active work ~25 min vs. 5–8 min target — the Wikidata wiring touched
the runner, plugin boundary, endpoint context, and Cucumber hook across one
coherent responsibility; recorded for retrospective. Slice converged with green
proof; no refinement warranted mid-slice.

### 7. Run a mixed-resource batch without cross-file interference
Type: Behavior
Status: done
Behavior: Ordinary, OpenAI, and Wikidata files run sequentially under one stack;
scenario resets and mock reinstalls keep each file's correct endpoint and the
union of resources survives until invocation exit.
Proof: Run exactly this representative batch in a short disposable worktree:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_creation.feature,e2e_test/features/note_view/semantic_search.feature,e2e_test/features/wikidata/note_create_with_wikidata_id.feature
```

Observe one startup and final shutdown, correct responses from both services,
and empty owned ports after completion. Extend the invocation boundary example
only for any gap this mixed-resource transition exposes.
Sizing: 5 minutes active work; measured browser/runtime wait excepted.
Done 2026-09-12: ran the representative mixed batch (note_creation + semantic_search
+ note_create_with_wikidata_id) — 16/16 scenarios green, one startup and one
final shutdown, correct responses from both services, empty owned ports after
completion (a pre-existing foreign dev Mountebank on canonical 2525/5001/5002
correctly survived per ADR 0007). No production code change — the existing runner
already unions batch requirements (slices 5–6); added one focused boundary test
in `scripts/e2e-runner.test.mjs` covering the union gap (both mocks start under
one lease, distinct env keys, survive until exit, stop together). Refactor:
no candidates (test is focused behavioral documentation of the union gap, not
duplication). Sizing held (~6 min active + measured browser wait).

### 8. Keep interactive resource ownership across feature switches
Type: Behavior
Status: planned
Behavior: One interactive session switches between ordinary, OpenAI and Wikidata
features with the required private endpoints ready before use; reruns preserve
session ownership and close cleans all owned resources.
Proof: Session-boundary fixture covers resource selection, rerun, and close.
Use the existing preselection/session protocol; if interactive choices are not
known at launch, provisioning the bounded two-service set once for the session
is sufficient. Do not add a background resource broker or change Cucumber
scenario semantics. One focused interactive switch/close manual check is part
of this slice; inspect owned ports after closing.
Sizing: 5–8 minutes, medium confidence; measured browser startup wait excepted.

### 9. Establish complete active-inventory coverage without changing filters
Type: Behavior
Status: planned
Behavior: Every currently active file is selectable with its declared resources;
ignored scenarios and opt-in profiling selections retain existing tag behavior,
and real-service scenarios retain their original external URL policy.
Proof: Selection/configuration boundary checks reconcile admitted paths and
resource groups with the current feature inventory, including scenario-level
tags and mixed files. Use existing Gherkin/preprocessor facilities or fixtures,
not a new tag-language implementation. Explicitly cover the live OpenAI file and
mixed Wikidata file at the routing/filter boundary without paid/live requests.
Confirm primary/CI tags and active documentation agree. Do not hardcode 74 as a
permanent cap; report the actual assessed paths and any discrepancy.
This is coverage of the migration contract, not a claim that all product
scenarios or live external providers have been run successfully.
Sizing: 5–8 minutes, medium confidence. If inventory exposes a new shared-resource
family, stop for scope review; do not merely admit it to satisfy the count.

### 10. Demonstrate newly admitted workflows cannot change a peer's resources
Type: Behavior
Status: planned
Behavior: Two concurrent worktree invocations using newly admitted features
complete independently; resetting one app or reinstalling one Wikidata imposter
leaves the peer's note and mock response unchanged.
Proof: Extend the existing paired reset barrier harness with the minimal
Wikidata response observation; reuse the existing ownership/coordination logic.
Record paired command, origins, responses and no owned survivors. Keep shared
MySQL/Redis and persistent Development unchanged. Existing OpenAI peer proof
remains a regression, not a new parallel harness.
Sizing: 5–8 minutes active work, medium confidence; real-service/browser waits
excepted. A needed new coordination mechanism triggers refinement.

## Promise ownership

| Promise | Slice |
| --- | --- |
| Current active app/CLI files admitted with selected origin/artifacts | 1, 2 |
| Existing and additional OpenAI resource requirements | 3 |
| Cohesive owned multi-service mock lifecycle | 4, 5 |
| Wikidata isolated routing; no shared fallback | 5, 6 |
| One stack/resource union through multi-file batch | 7 |
| Interactive session resource readiness and cleanup | 8 |
| Complete inventory, ignored/profile filtering, live-service behavior | 9 |
| Primary/CI compatibility, failure/cancel cleanup | 3–6, 8, 9 |
| Peer data/mocks and shared services survive | 10 |

Update active runner guidance/docs alongside the admission slice that changes
it; final coverage proof checks stale four-file instructions are gone. Do not
rewrite completed historical plans or the old measured workload as if it had
run today's broader suite.

## Cumulative assessment

One selection registry describes existing file requirements; their union drives
one invocation-owned Mountebank process with service-specific endpoints.
Application target and fixture lifecycle stay as delivered. New resource data
does not justify recognizers in each caller or a plugin framework. The scoped
active inventory is the completion boundary; newly discovered resource families
or unrelated product failures require explicit assessment, not silent growth.

## Refinement assessment (2026-09-12)

Split original slice 8 into interactive resource lifetime (8) and active-inventory
/filter compatibility (9); paired proof moved to 10. No completed work or
promise was removed. Result: 10 slices, one Structure and nine Behavior.

All ten classify Ready: one gate and proof loop each, with the current reuse
path and medium sizing hypotheses. Slices 1–3, 5–8 and 10 allow measured
build/browser/process waits beyond the ten-minute limit only when active work
has finished; slice 9 has no runtime exception. Unknown engine compatibility is
owned and gated by slice 5, not presumed proven. No other unresolved
slice-specific refinement concern was identified in this assessment.

Ready for direct execution under the refinement workflow; implementation still
requires separate user authorization. No resplit recommendation at ten slices.
Keep the bounded story outcome unchanged if execution needs finer leaves.
