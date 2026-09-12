# Publish large notebook commits under one minute

Status: execution started.
Source: [SEED-018 story 3](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-3).
Target accepted by the user on 2026-09-12. Execution authorized by
`/dough-execute-plan 106` on 2026-09-12.

## Planned execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main`
- Execution checkout: `/Users/terryyin/git/d106-publish-large-notebooks` on `execute/106-publish-large-notebooks`
- Integration target: `main`
- Claim commit: `0f4e718df0`
- CI observation: unavailable — `.github/workflows/ci.yml` (`donut CI`) is push-triggered only for `main`; this execution branch has no push-triggered CI workflow.

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

Result: slice 1 pre-change small HTTP capture on this worktree
(`doughnut_e2e_wt_55773df2d6ac449796e9d4c0abac2671`, JDK 25.0.3,
`-XX:TieredStopAtLevel=1`, Hibernate 7.4.5.Final, MySQL 8.4.11).
`pnpm cy:run` does not forward `--expose`/`--config`; tags were applied
for this run only by a temporary `e2e_test/config/ci.ts` override that
was reverted afterward.

- Valid HTTP (`2026-09-12T07-59-47.043Z`): elapsedMs **205.722**, HTTP 200,
  accepted head `62940858301bcc5c0f4bc7ab32e16a979bf311bd`, 20 authored
  documents verified. Request-thread allocation weight **50,417,416**
  bytes. 8 execution samples in ~206 ms (too few to re-rank the large-run
  flush bottleneck); 5/8 in `org.hibernate`, including
  `AbstractFlushingEventListener` / `NotePropertyIndexService` /
  `NoteAliasIndexService`.
- Late rejection (`2026-09-12T07-59-53.252Z`): elapsedMs **147.006**, HTTP
  400 invalid aliases at `group-19/Added-00019.md`.

No new 10,000-note baseline run. This is before-evidence only, not
optimization.

## Slice 1 engine observations (2026-09-12)

Temporary request-scoped Hibernate `SessionEventListener` on the unchanged
writer, then removed. Versions actually resolved in this worktree: Hibernate
**7.4.5.Final**, isolated MySQL **8.4.11**. Controller reads after
`REQUIRES_NEW` commit show property wiki values and inbound aliases.

Per added or replaced note, after setup queries:

1. AUTO partial flush in `NotePropertyIndexService.refreshForNote` with 0
   entities (existing index lookup under `FlushModeType.COMMIT`).
2. EXPLICIT `entityManager.flush()` in `NotePropertyIndexService.refreshForNote`
   after unlinking/removing old index rows. Replacement flushed 12 entities /
   21 collections; addition with no prior rows still flushed the whole context
   (9 entities / 14 collections) so `replaceContent`'s transient
   `authored_note_reference` children are visible to the following JPQL.
3. AUTO partial flush in `ownRowsBySourceLocalKey` after restoring AUTO
   (addition 9/9, replacement 10/10). This is a second traversal of already
   flushed state.
4. AUTO partial flush in `NoteAliasIndexService.refreshForNote` from bulk
   `DELETE`, then EXPLICIT flush after the bulk delete.
5. EXPLICIT `EntityPersister.flush()` after the concept loop in
   `NotebookGitProposalDocumentApplication.apply` (additions), then an
   additional explicit flush on accept.

FK / uniqueness / visibility that must be preserved:

- `note_property_index.uq_note_property_index_note_key_item` and FK
  `authored_note_reference_id` (ON DELETE SET NULL). Old index rows must be
  unlinked and flushed before inserting replacements; otherwise uniqueness
  fails and `replaceContent` children stay transient for the JPQL map.
- `note_alias_index.uq_note_alias_index_note_lookup` requires the bulk delete
  to be flushed before inserting the new lookup key.
- After that required explicit property flush, pending authored references are
  already visible; the AUTO query flush is not required for visibility.
- Rollback remains the existing serializable `REQUIRES_NEW` transaction;
  slice 2 still owns atomic late-rejection proof.

**Candidate for slice 2:** keep `FlushModeType.COMMIT` through
`ownRowsBySourceLocalKey` (restore the prior flush mode only after the query
and new index-row persist). That removes the redundant query-triggered
whole-context auto-flush without changing the required explicit unlink flush,
alias bulk-delete ordering, or the final apply flush. Do not skip the
explicit property flush on this evidence: additions still need it for
transient authored-reference visibility, and replacement needs it for uniqueness
and FK ordering.

Production writer flush semantics are unchanged in this slice.

Small HTTP capture (20 existing / 20 additions / 20 folders) recorded
elapsedMs **205.722** (accepted) and **147.006** (late invalid alias).
JFR at this size is too sparse to rank flushes; engine-safety ordering
comes from the controller probe above. Slice 2 can proceed with the
COMMIT-through-`ownRowsBySourceLocalKey` candidate.

## Slice 2 engine observations (2026-09-12)

Candidate applied in `NotePropertyIndexService.refreshForNote`:
`FlushModeType.COMMIT` now covers unlink, explicit flush,
`ownRowsBySourceLocalKey`, and new index-row persist; prior mode is restored
after persist. Explicit property unlink flush and alias bulk-delete flush
are unchanged. `pnpm cy:run` now forwards `--expose` and `--config`.
Instrumentation was request-scoped then removed; no production flush
counter. Versions re-resolved: Hibernate **7.4.5.Final**, isolated MySQL
**8.4.11**.

Flush-sequence delta vs slice 1 (same controller publication path):

1. AUTO 0/0 existing-index lookup under COMMIT — unchanged.
2. EXPLICIT property unlink flush remains (replacement still 12/21).
3. AUTO `ownRowsBySourceLocalKey` is now **0/0** (slice 1: addition 9/9,
   replacement 10/10). The redundant whole-context traversal is gone.
4. AUTO alias bulk-delete plus EXPLICIT alias flush remain.

Small HTTP after-capture (20/20/20), same worktree
`doughnut_e2e_wt_55773df2d6ac449796e9d4c0abac2671`:

- Valid HTTP (`2026-09-12T08-17-41.867Z`): elapsedMs **186.894** (slice 1
  **205.722**), HTTP 200, accepted head
  `62940858301bcc5c0f4bc7ab32e16a979bf311bd`, 20 authored documents.
- Late rejection (`2026-09-12T08-17-48.019Z`): elapsedMs **153.214**
  (slice 1 **147.006**), HTTP 400 invalid aliases at
  `group-19/Added-00019.md`; 19 preceding additions processed; accepted
  head, stored rows, and learning state unchanged.

Wall-clock at this size is noisy around ~200 ms; the required proof is
less flush work, which held. Installed-CLI `@publicationProfile` passed
with default 20/20. Slice 3 may proceed with the large confirmation; the
unresolved risk remains that this bounded change may not achieve the
roughly 63-fold large-run target.

## Ordered slices

### 1. Make required refresh ordering observable on a small fixture
Type: Structure
Status: done
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
Status: done
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
Sizing: 5–8 minutes active work. Slice 1 identified one local change in the
existing `NotePropertyIndexService.refreshForNote` lifecycle: keep
`FlushModeType.COMMIT` through `ownRowsBySourceLocalKey`. No extra mutation
phase or new owner. Required backend/build/browser waits may exceed the limit;
do not use the wait exception for implementation.
Isolated tagged captures: `pnpm cy:run` forwards `--expose`/`--config`.
Do not mutate committed `e2e_test/config/ci.ts`.

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
PUBLICATION_PROFILE_EXISTING=1000 PUBLICATION_PROFILE_ADDITIONS=10000 PUBLICATION_PROFILE_FOLDERS=20 PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS=60000 CURSOR_DEV=true nix develop -c pnpm cy:run --browser chrome --spec e2e_test/features/cli/cli_notebook_web_created_note.feature --config taskTimeout=66000 --expose tags=@publicationProfileHttp
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

Slice 1 is done: Hibernate 7.4.5.Final / MySQL 8.4.11; required explicit
property unlink flush and alias bulk-delete flush retained; redundant AUTO
query flush after restoring AUTO is the candidate. Characterization tests and
small HTTP before-capture (elapsedMs 205.722 / 147.006) are recorded.
Slice 2 is done: COMMIT through `ownRowsBySourceLocalKey` persist; AUTO
query flush 9/9→0/0; explicit property and alias flushes remain; after-capture
elapsedMs 186.894 / 153.214; `pnpm cy:run` forwards `--expose`/`--config`.
Slice 3 is the remaining scaling confirmation, with the unresolved risk that
the bounded change will not achieve the required roughly 63-fold improvement.
Required suite/build/fixture/runtime waits are the only sizing exceptions.
