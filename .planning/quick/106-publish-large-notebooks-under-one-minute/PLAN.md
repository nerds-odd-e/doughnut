# Publish large notebook commits under one minute

Status: planned; no execution started.
Source: [SEED-018 story 3](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-3).
Target accepted by the user on 2026-09-12. This request authorizes planning and
plan refinement only. Backlog placement remains unchanged.

## Outcome and boundaries

A notebook owner publishes one valid commit containing 10,000 added concepts
into the representative notebook with 1,000 existing concepts and 20 folders.
The successful HTTP response completes in **less than 60,000 ms**, and the
accepted head and authored contents are available in Donut. Measurement starts
with the publication request and ends with its complete response body, including
transport and server commit; fixture creation, bundle preparation and receiver
verification are outside that interval. Counts are a fixture, not a product cap.

One bounded optimization of repeated property/alias-index flush work is included.
Preserve authorization, validation, authored names/content/reference semantics,
note identity, learning history, and atomic acceptance/rejection. Keep the
existing synchronous publication contract. No timeout-only solution, background
jobs, progress UI, resumable upload, general synchronization, broad persistence
rewrite, or optimization of unrelated operations. Large rejection latency and a
production-wide SLA are not promised.

## Existing solution and architectural constraints

PFE responsibility: persist authored Markdown and its derived indexes coherently
within the existing publication transaction while avoiding repeated whole-context
flush traversal. Change the existing solution; do not add a second import writer.

- `NotebookGitProposalPublisher.publish` owns the serializable, REQUIRES_NEW
  transaction and accepted-head authorization/concurrency checks.
  `NotebookGitProposalDocumentApplication.apply` already has a final flush after
  the concept loop. Reuse that lifecycle where the engine proof supports it.
- `AuthoredNoteDocumentPersistence.persist` is shared by publication and web
  content saves. It delegates derived state to
  `NoteReferenceService.refreshDerivedIndexesForNote`, then the existing property,
  alias and level services. Preserve one definition of derived-index semantics.
- `NotePropertyIndexService.refreshForNote` suppresses query auto-flush while
  unlinking/removing old index rows, explicitly flushes, restores the prior mode,
  and queries authored-reference rows. Its comment identifies transient children
  and FK ordering as a real requirement to investigate.
  `NoteAliasIndexService.refreshForNote` uses bulk deletion followed by an explicit
  flush. Both explicit and implicit flushes matter.
- Other callers include note construction/extraction, content edits and title
  reference rewrites. They need their current immediately usable derived state.
  Existing `saveAll` usage for recall logs and AI requests does not own authored
  reference replacement and is not a substitute. `EntityPersister.flushAndClear`
  is not a ready solution: publication retains managed notes and binding state.
- Reuse the profile feature, HTTP helper, JFR analyzer and bulk receiver checker
  from [retained profiling guidance](../../../docs/notebook-publication-profiling.md).
  CLI/MCP/frontend are callers, not alternate owners of server index persistence.

Relevant Accepted decisions: [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
requires authored Markdown as authority and source-owned derived references;
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) preserves useful
business rejection and visible unexpected failure;
[ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) requires
owned disposable Unit Test/E2E data. Index and record statuses agree. ADR 0002 is
Proposed, not a new synchronization commitment. No new North Star topic is needed:
existing ownership supports this local optimization.

## Evidence and engine assumption

The awake baseline is 3,772,320.997 ms on the documented local JVM/MySQL setup.
Do not average it with the sleep-affected capture. Two captures put about 98.2%
of publication-thread execution samples in flush traversal; that does not prove
the roughly 63-fold elapsed-time reduction needed here.

Unproven assumption: the existing refresh lifecycle can avoid repeated traversal
without losing pending reference visibility, FK/uniqueness ordering, or rollback.
Use the repository-resolved Hibernate version and isolated MySQL 8.4; record the
actual versions. Probe additions with property wiki references and aliases, plus
replacement of an existing reference/alias and removal of stale derived entries.
Observe state through controller reads in a fresh committed transaction, not only
the writer's persistence context. Record explicit/auto-flush observations and the
minimum required ordering before selecting the production change.

Proof command for controller coverage (all backend tests, per backend rule):

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Small publication capture on the owned isolated E2E stack:

```sh
PUBLICATION_PROFILE_EXISTING=20 PUBLICATION_PROFILE_ADDITIONS=20 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose 'tags=@publicationProfileHttp or @publicationProfileHttpRejection'
```

Result: **not run**; planning supplies no engine-safety or performance evidence.
Reuse current small rejection evidence for fixture suitability, not proof of a
future changed implementation. No new 10,000-note baseline run is needed.

## Ordered slices

### 1. Make required refresh ordering observable on a small fixture
Type: Structure
Status: planned
Change: Use the existing controller/profile fixture to expose the current
refresh lifecycle with temporary, request-scoped flush observations. Add only
missing stable-boundary characterization of existing derived-state visibility;
do not create a profiler framework, production flush counter or new index owner.
This proof support immediately enables the safe optimization in slice 2.
Proof: Execute the engine probe described above against the unchanged writer.
Record the explicit/query-triggered flush sequence, the concrete FK/visibility
requirements and one candidate change in this plan. Existing successful save,
replacement and rollback behavior remains green. Run all backend tests if test
code changes and use the small capture command for the pre-change measurement.
Remove temporary instrumentation after retaining the observations. Do not commit
a deliberately failing future-performance test or claim this is optimization.
Sizing: about 5 minutes active work; measured required test/capture waits excepted.
If one short probe cannot establish a safe candidate, stop and record the exact
unresolved assumption. Reassess slice 2 in place before starting it; investigation
does not expand to larger fixtures merely to repeat the bottleneck ranking.

### 2. Publish small additions with less repeated flush work
Type: Behavior
Status: planned
Behavior: Given a valid small authored addition batch, when its owner publishes,
the accepted contents and derived references are usable with less measured flush
work and reduced publication time, preserving existing note and learning state.
Proof: Apply the one candidate supported by slice 1 in the existing refresh
lifecycle. Run the full backend suite and the small after-capture under comparable
conditions. Reuse `NotebookGitPublicationControllerTest` for accepted content,
identity and learning state; extend controller fixtures only for missing derived
state observations. Keep shared web-save and alias/property refresh regressions.
The same slice must pass `NotebookGitPublicationAtomicControllerTest`,
`NotebookGitProposalPropertyValidationControllerTest` and the small final-invalid-
alias capture: useful path error, preceding processing, unchanged accepted head,
stored rows and learning state. Do not ship the optimization before rollback proof.
Preserve the ordinary installed-CLI success path with the existing small
`@publicationProfile` scenario, using the same feature command and small default
counts. Reuse valid regression evidence; add only assertions for exposed gaps.
Sizing: 5–8 minutes active work, conditional on slice 1 identifying one local
change. Required backend/build/browser waits may exceed the limit. If the
candidate needs several mutation phases or uncertain new ownership, refine this
slice before editing code; do not use the wait exception for implementation.

### 3. Confirm the representative commit publishes under one minute
Type: Behavior
Status: planned
Behavior: Given 1,000 existing concepts and a valid 10,000-concept addition across
20 folders, when the owner publishes that one commit, its successful response
arrives in less than 60,000 ms and its accepted head and authored bytes are visible.
Proof: Only after the small candidate is correct and measurably better, run one
large confirmation using the command below. Require successful response, recorded
`elapsedMs < 60000`, matching accepted head/tree, and every received file unchanged
through the existing bulk checker. Record revision, fixture fingerprints, JVM
flags/version, DB version, host awake interval, elapsed time and request CPU/flush
and allocation measurements in the existing profiling document. Compare with the
awake baseline and state material environment differences. A runner timeout or
backend-only success is incomplete evidence, not a passed publication.
Sizing: about 5 minutes active work plus measured fixture/build/capture/verification
waits; this is one scaling proof, not another optimization slice. If the target
fails, preserve the result and return for story scope review. Do not repeatedly
run large captures or optimize a second area automatically.

```sh
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=10000 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cypress run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose tags=@publicationProfileHttp
```

The profiling helper already records request-through-response time. Assess its
recorded value explicitly; timer configuration alone is not the acceptance proof.
Slice 2 owns ordinary installed-CLI compatibility; this large HTTP capture owns
the agreed request-time target. Do not time fixture creation or CLI packaging as
if it were the retained HTTP baseline.

## Promise ownership

| Promise | Owning slice and observation |
| --- | --- |
| Reduced repeated flush work on the selected path | 2: after-capture compared with slice 1 observations |
| Accepted authored content and derived reference/alias semantics | 2: publication/controller reads and existing shared-save regressions |
| Existing identity and learning history | 2: retained controller proof and fresh-transaction reads |
| Authorization, validation and atomic late rejection | 2: existing controller suite plus small late-invalid-alias capture |
| Large success below 60,000 ms, accepted head and exact content | 3: successful timed HTTP capture and bulk receiver verification |
| Ordinary installed-CLI success | 2: existing small CLI profile scenario |
| Comparable before/after evidence | 1–2 small captures; 3 qualified awake large-baseline comparison |

## Delivery, sizing and stop conditions

Execution defaults to an owned Git worktree under dough-execute-plan; record its
identity here when execution is authorized. Do not mutate Development or the
reported user's jap3 notebook. On timeout, inspect the owned backend before any
reset/retry; a disconnected client does not establish server cancellation.

Leaves target about 5 minutes including focused verification; scrutinize longer
work and stop/refine above 10 minutes active work. The only stated exceptions
are measured required backend-suite/build and E2E fixture/runtime waits, which
splitting cannot reduce. Record actual elapsed time and evidence. An unresolved
flush requirement stops dependent optimization; an insufficient bounded gain
returns for scope review without weakening the agreed target.

At every implementation delivery: Jidoka, fresh dough-post-change-refactor agent,
API generation only if its trigger changes, one coordinator
`./scripts/run.sh pnpm format:changed`, plan update, commit with check-only hook,
push and asynchronous CI handling. This design anticipates no API/schema change.
Keep the plan and evidence for retrospective and story wrap-up; do not mark the
story complete solely because the small optimization is faster.

## Cumulative assessment

One shared content-derived-state model remains authoritative; there is no
size-specific writer, special case for the measurement counts, or separate
representation for large notebooks. The large example measures the same behavior.
No second implementation path is prescribed by the slice split. Correctness
tests for the changed behavior stay in slice 2; slice 1 establishes only the
existing engine requirement and pre-change evidence.

## Refinement assessment (2026-09-12)

Replaced original slice 1 with a bounded Structure prerequisite (1) and its
immediately enabled Behavior (2). Original large confirmation is now slice 3;
ordinary CLI compatibility moved into slice 2's correctness proof. No completed
work or promise was removed. Result: three slices; no story resplit indicated.

Slice 1 is bounded to one small engine proof, with a stop on unresolved ordering.
Slice 2 remains conditional on that evidence: its exact safe mutation and sizing
cannot be certified by static inspection. The prerequisite completion must
record the selected change and reassess this same plan before implementation.
Slice 3 is a single scaling confirmation, with the unresolved risk that the
bounded change will not achieve the required roughly 63-fold improvement.
Required suite/build/fixture/runtime waits are the only sizing exceptions.

There is no open product decision, but this assessment does not claim all
slices are ready for direct execution or that the target is feasible. No product
tests or captures were run during planning/refinement; all slices remain planned.
