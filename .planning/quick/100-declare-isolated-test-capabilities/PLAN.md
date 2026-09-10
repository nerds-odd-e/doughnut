# Declare isolated test capabilities in one place

Source: [SEED-017 Story 3](../../seeds/SEED-017-cohesive-design-corrections.md#story-3),
including the seed's F3 audit finding and historical plans 061/070/088/089.
Preserved lifecycle evidence: plans 073/093 and the current runner tests.
Status: planned. Planning only; no product changes or verification runs yet.

## Correction contract

Maintainers can understand and change an approved isolated spec's resource
requirements in one place. Developers retain the same four safe isolated
workflows. This corrects a policy dependency, not the admission scope.

Current evidence: `scripts/isolated-openai-mock.mjs` owns the completion feature
filename; `isolated-cypress-spec-selection.mjs` imports it to build admission;
`isolated-cypress.mjs` compares it to the selected spec to start the mock.
The runner performs selection both during node setup and in `before:run`.
Harnesses and fixtures also import the filename from the resource module.

One spec-policy owner must declare admission and resource requirements.
Selection resolves a normalized approved spec to that policy. Orchestration
uses its requirements; existing lifecycle modules consume allocation and
ownership configuration without knowing feature paths. Derived lists and
diagnostics must come from that same source. No production policy is copied
into test fixtures, per-story handlers, or a second resource pipeline.

Preserve note-editing, CLI web-created-note, MCP search/graph, and OpenAI
completion admission. Every run still uses the owning SUT and runner lease;
only completion currently requires the private OpenAI mock. Preserve explicit
setup-time endpoint exposure, deferred selection in `before:run`, path/argument
normalization, single-spec refusal, health/ownership checks, endpoint privacy,
runner-observed failures, cancellation, and composed completion cleanup.
Unknown specs must not default to no-mock requirements.

Exclude new admissions, multiple-spec support, new providers, plugin frameworks,
environment allocation changes, process termination redesign (Story 4), and
notebook publication. Do not change eligibility or resource timing merely to
simplify implementation. No disputed product decision remains in the source.

## Final design and architectural constraints

Keep the implementation small: a registry of the current approved workflows
with their existing requirements is sufficient. A private-mock requirement
does not require a generic resource graph or plugin API. Common SUT/lease
ownership stays common rather than being repeated in each workflow handler.
Choose the smallest descriptor representation during implementation; its
shape is not a new externally configurable API.

The policy owns feature identities; selection owns normalization/admission;
the runner owns when resources are acquired and exposed; the existing mock
and runner lifecycle own cleanup/failure observation. These responsibilities
do not mandate separate new files. Re-exported names, if useful to current
consumers, must reference the policy owner without a second table. Remove the
feature export from the mock lifecycle module and migrate its consumers.

Accepted [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
requires owned disposable test resources and no shared-default fallback.
Accepted [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
requires visible failures and purposeful handling. Their index/in-file statuses
agree; no conflicting decision or new ADR approval is needed.

## Ordered slices

### 1. Give approved-spec policy one owner
Type: Structure
Status: planned
Sizing: about 5 minutes active implementation, focused tests and cleanup;
medium confidence because filename imports span fixtures and harnesses.

Correction owned: remove the resource-to-spec dependency and independently
maintained admission policy. Create or evolve the small authoritative registry
for all four approved specs and current resource requirements. Derive the
allowlist and admission diagnostics from it. Move completion filename ownership
out of `isolated-openai-mock.mjs` and migrate every consumer, including the idle
mock runner and reset-isolation harness tests. Keep normalization/discovery and
all run behavior unchanged.

This directly owns a bounded part of the evidenced architectural correction;
it is not speculative preparation. The runner's existing filename comparison
may temporarily consume the canonical policy constant; slice 2 removes that
remaining policy interpretation. Do not leave a compatibility export in the
resource lifecycle that reverses the dependency again.

Proof: exercise existing `guardCypressNodeSetup` admission and mock/no-mock
tests and harness selection tests, using their public boundaries. Confirm the
same four paths and rejection diagnostics are derived from the registry. A
source/import review verifies that the resource lifecycle no longer owns or
imports feature identities. Reuse data cases; no test class per internal helper.

```bash
CURSOR_DEV=true nix develop -c node --test \
  scripts/isolated-cypress.test.mjs \
  scripts/isolated-cypress-cli-spec.test.mjs \
  scripts/isolated-cypress-openai-mock.test.mjs \
  scripts/worktree-reset-isolation-harness.test.mjs \
  scripts/worktree-reset-isolation-harness-openai-mock.test.mjs
```

Safe stop: all existing workflows still behave identically, feature policy
has one owner, and no lifecycle module depends on a feature filename.
The correction remains incomplete until slice 2 consumes resource requirements.

### 2. Acquire isolated resources from approved requirements
Type: Structure
Status: planned
Depends on: slice 1
Sizing: about 5 minutes active work; medium confidence. Focused process tests
and the live Cypress run have an explicit external-wait exception below.

Correction owned: remove filename-based resource dispatch from the runner.
Resolve admitted requirements for both setup-known selection and `before:run`
selection through one rule. Acquire the existing private mock from those
requirements after the lease, expose its endpoint at the existing time, and
retain existing failure observation and cleanup registration. Do not defer an
already-known completion requirement until `before:run`: setup-time exposure
is needed. Do not create a second mock when `before:run` confirms the selection.

Proof: extend existing runner-boundary data only for missing observations:
the three approved no-mock workflows acquire no private mock; completion
acquires one and exposes its endpoint during known-spec setup; `before:run`
resolves the same policy and does not acquire again for a confirmed selection.
Keep the existing deferred-selection/refusal behavior and normalization paths.
Unknown or multiple specs refuse before reset/execution, with cleanup of any
already-acquired resources. Do not invent new mismatch or switching semantics.

Reuse existing success, ownership, asynchronous mock-failure and SIGINT/SIGTERM
proofs. They must establish owner-observed failure and release with peers still
serving, not merely a rejected startup promise. Preserve the composed
`after:spec`/`after:run` handler tests. No lifecycle algorithm rewrite is planned.

```bash
CURSOR_DEV=true nix develop -c node --test \
  scripts/isolated-cypress.test.mjs \
  scripts/isolated-cypress-cli-spec.test.mjs \
  scripts/isolated-cypress-openai-mock.test.mjs \
  scripts/isolated-cypress-owning-health.test.mjs \
  scripts/isolated-cypress-openai-mock-failure.test.mjs \
  scripts/isolated-cypress-openai-mock-cancel.test.mjs \
  scripts/isolated-openai-mock.test.mjs \
  e2e_test/config/composeCypressPluginEvents.test.mjs
```

Representative live proof, from this execution's own configured worktree:

```bash
CURSOR_DEV=true nix develop -c pnpm sut:healthcheck
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/ai_generated_content/note_content_completion.feature
```

Use the documented owning `pnpm sut` setup if that worktree has no running SUT;
never borrow another task's resources. Reuse plan 093's completion observation:
record the run-owned mock process/ports, confirm they are gone after completion,
and confirm the owner remains healthy and its runner lease is reusable. Use
the existing `acquireSutRunnerLease`/`releaseSutRunnerLease` API with `try/finally`
for the reuse observation; do not start another full Cypress run just to prove
lease acquisition. Record exact commands/observations in this PLAN during
execution. A green Cypress exit without resource-release evidence is incomplete.

Safe stop: one policy drives every existing selection path and resource
decision; all currently supported runs retain their lifecycle guarantees.

## Proof ownership

| Promise | Owner and observation |
| --- | --- |
| One admission/resource policy; no feature ownership in lifecycle | 1 registry/import review; 2 removal of runner filename dispatch |
| Same four admissions and single-spec safety | 1 existing admission/harness tests; 2 same resolver at both lifecycle stages |
| No mock for note editing, CLI or MCP | 2 runner-boundary data using approved specs |
| Completion endpoint available at setup; no duplicate startup | 2 setup then `before:run` observation |
| Normalization/deferred selection remain intact | 1 unchanged selection functions; 2 deferred-selection boundary proof |
| Owning SUT/lease, foreign refusal, no shared defaults | 1–2 existing admission/ownership tests, 2 owning-health proof |
| Completion/failure/cancellation release and preserve peers | 2 existing process-boundary tests, composed handlers and live completion |
| Cohesion rather than story-shaped implementation | 1–2 cumulative source review and independent post-change refactor |

## Sizing assessment and execution boundaries

The sequence was split around two independently reviewable architectural
corrections: policy ownership, then its resource consumption. No per-workflow
slices or generic resource framework are justified. Both are Structure slices
owning evidenced correction work with preserved external behavior. No separate
Behavior slice or test-only slice is needed.

Each slice targets about five minutes including focused proof and cleanup.
If active work exceeds five minutes, scrutinize the remaining work; approaching
ten minutes requires finer decomposition unless only the named focused-test or
external wait remains. Slice 2's process-test/Cypress/service-start wall time
is the explicit exception, measured separately from implementation effort.
It does not excuse missing endpoint or cleanup evidence. No novel storage or
infrastructure mechanism requires a separate experiment; the representative
run verifies integration with the existing Cypress runtime.

No remaining multi-outcome or speculative-design concern was identified after
this split. Setup-time resource exposure remains the concrete integration risk,
owned by slice 2's boundary and live proof. If preserving that timing demands
new lifecycle semantics, stop and reassess the correction rather than broadening
it silently. Park or revert only attempt-owned work on an overrun.

## Delivery and parallel execution

Execution is not authorized by this planning request. When authorized, use
`dough-execute-plan`, its backlog take protocol, and this PLAN as the single
execution/resume record. Each delivered change follows Jidoka → fresh
`dough-post-change-refactor` agent → API generation if actually required →
one coordinator `./scripts/run.sh pnpm format:changed` → PLAN update without
a second routine formatting pass → commit/check-only hook → push and
asynchronous CI handling. Implementers/refactorers do not format or run
standalone `lint:changed`. No API, schema or application behavior changes are
expected; backend tests are not part of this infrastructure-only scope.

Can run alongside plan 099 in a separate worktree with independently owned
test resources. Coordinate shared seed/backlog edits; this task owns plan 100,
and publication owns plan 099. Do not concurrently refactor Story 4's process
termination mechanism. Retain source/PLAN through retrospective and wrap-up.

## Learnings

Planning inspection confirmed existing public-boundary lifecycle proofs and
the resource-to-spec dependency. No tests were run during planning, and no
product behavior is claimed delivered.
