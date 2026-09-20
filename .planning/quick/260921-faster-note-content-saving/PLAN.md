# Faster note-content saving

Status: slice 1 published; slice 2 verified for delivery; slice 3 next.
Source: [SEED-034 story 1](../../seeds/SEED-034-faster-note-content-saving.md#story-1).
Identity: SEED-034#story-1.
Research revision: `4d06fc052f4b90406677a769e6a998473ea4c2ef`, 2026-09-20.

## Execution identity

- Mode: Trunk Mode (`--truck` interpreted as `--trunk`).
- Origin/integration checkout: `/Users/terryyin/git/doughnut`, branch `main`.
- Execution checkout: `/Users/terryyin/.codex/worktrees/faster-note-content-saving/doughnut`.
- Execution branch: `codex/faster-note-content-saving`.
- Authorized destination: `origin/main`, `nerds-odd-e/doughnut`.
- Published revisions: `f2dddcb9fa31a70286385a4847beb252a7202110` (claim), `11b76124f07c23f24adac79200cd6a1ebf8794b1` (slice 1).
- Product baseline revision: `b5cad203d1d8915b03cbb2353866134979519349`.
- Replanning: retain existing plan refinement authority within selected story scope.
- CI source: GitHub Actions, `ci.yml`, display name `donut CI`.
- CI observer: coordinator `260921-resume2`, yielded cell `94`, session `90384`,
  process `61971`, directory `/tmp/dough-ci-501/watch-WZsYXb`; runtime is this
  execution checkout's `.agents/skills/dough-execute-plan/scripts/ci-mailbox.mjs`.
- Claim backend CI failed: run `35478855867`, job `105992832735` has five
  failures caused by MySQL `Too many connections`/consequent context-load
  failures. Earlier product revision `4d06fc052f` run `35476646641`, job
  `105986915501` has the same five failures and causes. Claim diff changes only
  this backlog entry; this is an existing test-environment defect, not a
  passing CI result. Logs: `/tmp/donut-save-claim-backend.log` and
  `/tmp/donut-save-prior-backend.log`. No retry or unrelated repair performed.

## Goal and scope

Authors save changed content in a large notebook, especially wiki-linked
content, with greater than 4× lower median save-completion latency. Preserve
content, live link meaning, editing races, note/learning identity, and complete
accepted Git changes. The source owns the measurement and design contracts.

The complete handwritten production diff must be net smaller and easier to
understand. Fewer lines are evidence, not a substitute for cohesive ownership.
No cache/invalidation layer, async acknowledgement, duplicated link resolver,
endpoint-specific snapshot algorithm, or new persistence infrastructure is
selected. Keep existing editor timing and API shape unless evidence requires
revisiting the plan. Title/Readme performance and bulk publication performance
are deferred, while affected shared behavior must remain correct.

## Research and limits

Read-only Development notebook-1 shape and full original research remain in
this plan at `b5cad203d1` and `/tmp/donut-note-save-research/`. No Production
measurement or mutation; research database retired. Controller median 737.1 ms,
59 statements and 15,269 loaded entities are diagnostic only (different JVM).
Content saving serializes debounced editor requests through the content
controller, `WebNoteEditService`, accepted change and realm response owners.
It hydrates all stored notes/folders and repeats snapshot construction/accepted
blob decoding. Preserve the 1,000 ms typing debounce, immediate wiki/blur flush,
content-derived state, complete accepted history and live response resolution.

## Existing solutions and chosen design

PFE selected existing `NotebookExportRows`, `ExportNoteRow`, `ExportFolderRow`
and `PortableTreeSnapshot` as the shared complete persisted tree owner. Query
these flat shapes directly, preserving trash, instead of hydrating aggregates.
`AcceptedWebChangeService` remains transaction/lock/acceptance owner: lock
bindings in notebook-ID order, resolve targets inside that transaction, apply
the whole operation, flush, then read the final projection. Remove redundant
whole-notebook target lookup; ORM already owns managed-entity identity.

Publication retains identity-bearing `LockedNotebookState`; inspect its callers.
Read accepted entries once per opened binding and reuse one final snapshot for
comparison/persistence. These are operation-local inputs, not a persistent cache.
Native Git tree comparison is a possible later alternative, not selected work.
Keep authored persistence, reference replacement/indexing and live resolution
with existing owners. No speculative batching, memoization or new indexes.

Follow Accepted ADRs 0001 (domain), 0002 and
`docs/notebook-git-synchronization.md` (atomic projection/head, original history),
0004 (Portable bytes/references), 0005 (destinations), 0006 (visible failures),
0007 (environment isolation), plus `.planning/NORTH-STAR.md` section
“One complete accepted web change”. No architectural exception is selected.

## Baseline and retained evidence

Before product edits, the owned rich-editor browser baseline passed in 3m02s:
three workloads × (first save + three warm-ups + 20 changed samples), with
HTTP 200, dirty-state clearing, exact refreshed link destinations and final
reload/content/link assertions. No watchdog diagnostics perturbed samples.
Setup supplies only the starting fixture; real editor typing initiates saves.
Source boundary is request initiation through saved/refreshed visible state.

| Workload | Request median | Visible median | Visible p95 |
| --- | ---: | ---: | ---: |
| Existing links | 1434 ms | 1454 ms | 1598 ms |
| Added links | 1454.5 ms | 1476 ms | 1608 ms |
| Plain control | 1394.5 ms | 1413.5 ms | 1929 ms |

Literal command: `CURSOR_DEV=true nix develop -c node scripts/profiling/run-note-save-baseline.mjs`.
Evidence: `/tmp/donut-note-save-baseline/before/` contains samples, summary,
JFR, versions, environment, numeric shape, generator/SQL and exact initial
bundle. `/tmp/donut-note-save-baseline/README.md` owns restoration/invocation
instructions for the archived temporary harness. Repository scaffolding was
removed after capture; do not commit unsanitized research `history.git`.

The sanitized fixture preserves the folder graph (depths 1–12), note/reference
counts and approximate per-note content lengths. It has 19 changed-history
commits, head `1204b33d3f48da94ad495e1a0c9beb6054b68aa2`, tree
`cf2c333f8041a2f08e2aa197cb30796765917bb7`. Synthetic bundle is 4,855,865 bytes
versus 3,288,529 original: a disclosed representativeness limit. Reuse EXACT
before bundle bytes, same SQL/sequence/runtime and real timers for comparison.
Keep setup outside timing and first-save observations separate.

Existing/plain last-type-completion-to-saved medians are 2412.5/2370.5 ms,
including roughly 956–959 ms debounce. `cy.type` returns about 35–45 ms after
final input, so immediate added-link flush has a negative apparent debounce;
this timestamp limitation does not affect request-to-visible duration.

Baseline setup used a bounded 30-active-minute fixture/timing exception.
An initial MySQL ENOSPC stall is retained in `disk-full-attempt/`; user freed
disk and resumed before accepted capture. No shared database/binlogs deleted.
Old CI process 18938 is absent; ENOSPC prevented its terminal receipt
(`/tmp/dough-ci-501/watch-L5kmu0`). Its two failures were accounted for;
pending CI in that gap is unobserved. Current observer identity is above.

## Evidence-led plan reassessment

Deep JFR: 2676 of 5362 request CPU samples have MySQL/TLS crypto leaves;
1366 involve collection reads, 452 single-entity reads and 854 entity writes.
Overlapping stack categories: Hibernate 4180, accepted-web-change 3145, JGit
898, state-loader 724, realm 17, wiki resolver 10. These are sampled CPU
observations, not elapsed-time fractions; frame truncation limits exact-query
attribution. The prior controller median is not comparable acceptance evidence.

Persistence remains the supported target. Transport/encryption weakens the
hypothesis that selected removals alone exceed 4×, so profile residual cost at
slice 2/final acceptance and do not infer speedup from entity counts. Keep the
same cohesive simplification. Link caching and changing debounce do not address
this evidence. No TLS weakening, runtime-only tuning or new persistence
architecture is selected. Failure to reach 4× requires evidence-led refinement,
not a relaxed target or speculative machinery. Source outcome is unchanged.

## Ordered slices

Target approximately five minutes of implementation, verification, and cleanup
per leaf. Scrutinize work exceeding five; at ten minutes stop and finer-decompose
unless the extra time is the required full backend suite or the bounded large
fixture/profile run. Record such external verification time separately; it does
not excuse an oversized implementation. Every delivered boundary stays green.

### 1. Read Portable snapshots through their existing flat representation

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed 2,535 tests;
export controller/service, Portable snapshot and cutover assertions inspected.
Log: `/tmp/donut-flat-export-backend.log`. Independent refactor: no changes.
Size hypothesis: about five minutes of edits; required full-suite time separate.

Have the existing export-row owner obtain folder/note scalar projections
directly from the repositories, preserving order, null/root placement, display
names, complete stored content including trash, and README/empty-folder bytes.
Remove entity-to-row mapping where no entity is needed. Do not change ordinary
note query meanings or introduce another export shape. This enables slice 2 to
compare complete notebooks without managing every note/folder as an entity.

Preserve `DisplayName` conversion and null root IDs in projections, and retain
`notes(List<Note>)` for publication's identity-bearing callers.

Proof: existing `NotebookExportControllerTest`, `NotebookExportServiceTest`,
`PortableTreeSnapshotTest`, and Git cutover/controller coverage, through the
full backend suite. Add only a missing stable-boundary case if inspection finds
a projection semantic gap. Export bytes must remain unchanged.

### 2. Save a changed note without hydrating the entire notebook as entities

Type: Behavior
Status: done
Proof: full backend suite passed 2,536 tests (1m03s); controller load regression
loads fewer than 30 Notes for a notebook with 30 unrelated notes. No-op,
readback, learning identity, downloaded ancestry, queued saves and cross-notebook
reduction assertions inspected. Log `/tmp/donut-save-without-hydration-final-backend.log`.
Active edits ~7 minutes; independent refactor found no changes.
Identical 72-save browser run passed (2m49s), evidence `after-slice2/` under the
baseline directory: visible median/p95 existing 1184.5/1272 ms, added 1281/1423,
plain 1222/1642; speedups 1.228×/1.152×/1.157×. Whole-notebook loader frames
disappeared; 2284/4056 request CPU samples remain JDBC/TLS crypto leaves.
Size hypothesis: five to ten minutes; cross-caller adaptation is the sizing risk.

Behavior: given a large synchronized notebook, changing one note's content
persists the complete accepted change while managed-entity loading is confined
to actual mutation/read needs rather than every stored note and folder.

Use binding locks plus the shared flat projection in `AcceptedWebChangeService`.
Resolve mutation targets through the repository inside its transaction. Remove
`LockedNotebooks` and snapshot-instance lookup if the inspected callers confirm
they have no remaining responsibility. Adapt callbacks in web note editing,
creation, folder creation/relocation, and relationship reduction in the same
green boundary. Keep multi-notebook locking, under-lock touched-set validation,
post-mutation flush, fresh final rows, and the existing drift policy.
Publication still loads entities when needed for identity/application.

Proof: controller save/readback and downloaded bundle show one correct accepted
commit, retained learning identity, unchanged no-op behavior, and no lost
concurrent accepted work. Reuse `NotebookGitWebContentSaveControllerTest`,
`NotebookGitWebContentHistoryControllerTest`, folder/new-note/move and
`NotebookGitWebRelationReduceControllerTest` coverage. Run all backend tests.
Re-profile the same workload: report latency and entity counts separately;
ensure the whole-notebook entity population is gone without counting removal
of correctness work as an improvement. This slice owns shared-caller preservation.

### 3. Accept one final Portable snapshot with less repeated work

Type: Behavior
Status: planned
Size hypothesis: about five minutes of edits; final benchmark runtime separate.

Behavior: saving changed wiki-linked content completes sooner while comparison
and the appended accepted commit describe the same complete final tree.

Slice 2 already reuses the final snapshot for comparison and persistence.
Within the existing owner, reuse accepted entries across comparisons. Delete
redundant construction/decoding, keeping canonical ordering at one boundary.
Keep no-op, drift, folder/readme/empty-folder representation, and exact authored
content behavior. Do not introduce changed-note-only Git patching, which could
miss other notes touched by a complete operation.

Proof: full backend suite plus focused note-edit and wiki-link E2E. This slice
owns the final browser comparison and aggregate design acceptance: greater than
4× for both selected wiki-link workloads, plain-content/p95 regression check,
no edit-race regression, and net fewer handwritten production lines with fewer
representations. Inspect the complete diff and explain removed responsibilities.
If 4× is missed, this story remains incomplete even if this slice's local
simplification is correct. Capture the remaining cost and refine the same plan
before selecting another change; do not append speculative optimizations.

## Verification and delivery

All slices require `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Final focused E2E command:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_creation_and_update/note_edit.feature,e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature'`.

Existing frontend preservation includes `TextContentWrapper.spec.ts`,
`NoteShowPage.autosaveTrash.spec.ts` and content-undo coverage. If frontend
changes, run `pnpm frontend:test` and `pnpm -C frontend exec vue-tsc --noEmit`
through Nix. API/schema changes are not selected; revise and apply generation/
migration guidance if needed. Never run tests in Development.

Coordinator delivery: Jidoka → fresh dough-post-change-refactor agent → API
regeneration if triggered → `./scripts/run.sh pnpm format:changed` once → plan
update → commit with check-only hook → Trunk publication/CI registration.
Retain plan/evidence for automatic retrospective and later story wrap-up.

Final 4× efficacy is unproved until the final comparison passes.
