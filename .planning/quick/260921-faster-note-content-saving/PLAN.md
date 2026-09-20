# Faster note-content saving

Status: planned; no implementation authorized or started.
Source: [SEED-034 story 1](../../seeds/SEED-034-faster-note-content-saving.md#story-1).
Identity: SEED-034#story-1.
Research revision: `4d06fc052f4b90406677a769e6a998473ea4c2ef`, 2026-09-20.
Allocation: next number after the highest previously allocated quick directory,
`260920-frontend-proof-type-checking`; the active quick directory was empty.

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

Read-only inspection of Development notebook 1 found 11,184 notes, 4,046
folders, 2,887,527 content characters, 17,739 authored-reference rows on 9,809
notes, and a Git binding containing a 3,288,529-byte bundle. A string search
found `[[` in 9,815 notes; that is not the parsed-reference count.

The code path is `TextContentWrapper` → `useDebouncedTextAutosave` →
`StoredApiCollection.updateTextContentWithoutUndo` →
`TextContentController.updateNoteContent` → `WebNoteEditService` →
`AcceptedWebChangeService`, followed by `NoteRealmService.build`.

- Ordinary typing has a 1,000 ms debounce; new wiki-link text and blur can
  flush it immediately. Save requests are serialized. This deliberate delay
  is distinct from persistence latency.
- `NotebookGitStateLoader` loads every stored Note and Folder for a bound
  notebook. `LockedNotebooks.storedNote` searches that whole loaded collection
  to find the one edited note.
- `AcceptedWebChangeService` builds a before projection and compares it to
  decoded accepted blobs. After mutation it loads the rows again, builds and
  compares the final projection, then builds that final projection again for
  persistence. Accepted blobs are decoded again for the second comparison.
- Content persistence already owns reference children and derived indexes.
  The response live-resolves outgoing and incoming references. Do not remove
  either responsibility to make the request look faster.

Two local diagnostic experiments used temporary tooling outside the repository:

| Boundary | Observation | Limit |
| --- | --- | --- |
| Current Git importer, two tree reads, changed append, bundle writer on a local bundle copy | Five warmed totals 255.7–305.1 ms; first run 877.9 ms; 11,222 tree entries | Excludes SQL, entities, authorization, browser; no timing improvement claimed |
| Actual content controller plus response serialization against an isolated research database | After compiling current classes: five warmed controller totals 709.3–896.5 ms, median 737.1 ms; 59 prepared statements, 15,269 loaded entities, two flushes; serialization 0.2–0.5 ms | Direct controller, no HTTP/browser; copied notebook/reference data, unrelated tables empty; not acceptance workload |
| Separately timed equivalent preparation/persistence/realm journey | Five warmed persistence totals 590.5–694.8 ms; realm 54.6–61.3 ms; preparation 0.6–1.0 ms; 15,252 entities already loaded by persistence | Persistence accounts for about 91% of these totals; phase run adds commits to its isolated history |

Reproduction commands used:

```sh
CURSOR_DEV=true nix develop -c python3 /tmp/donut-note-save-research/profile.py
CURSOR_DEV=true nix develop -c backend/gradlew -p backend classes --quiet
CURSOR_DEV=true nix develop -c python3 /tmp/donut-note-save-research/run_journey.py
```

Local evidence: `/tmp/donut-note-save-research/profile.txt`,
`controller-profile.log`, `journey.log`, and `journey.jfr`. The standalone probe
and its isolated database are research resources, not permanent product tooling
or a repeatable final benchmark. The temporary database is retired after
research. JFR had too few application samples to establish precise CPU shares.
SQL-only transfer probes did not establish an expensive trash-view bottleneck;
the sampled title lookup returned no match and is not useful resolution proof.
Do not turn those probes into unsupported optimization claims.

No Production requests or mutations were performed. Development was read only.
The several-second production report and full browser save wait remain unmeasured.

## Existing solutions and chosen design

PFE assessment: `NotebookExportRows`, `ExportNoteRow`, `ExportFolderRow`, and
`PortableTreeSnapshot` already own a complete persisted Portable-tree projection
including trash. Read those existing row shapes directly for snapshot purposes;
do not materialize every note as an editable aggregate and then discard it into
the same row shape. Keep one projection owner shared with export/cutover.

Keep `AcceptedWebChangeService` as the transaction/lock/acceptance owner. Lock
bindings in notebook-id order, load the actual mutation targets within that
transaction, apply the whole operation, flush, and read the final projection.
The ORM transaction already supplies identity for managed entities; a separate
whole-notebook search to return the same entity should disappear.

Keep publication's identity-bearing `LockedNotebookState` where publication
actually needs it. `NotebookGitBundleDownloadService` already demonstrates
locking the binding without loading all notebook entities. Inspect all callers
before changing the state loader's contract; do not replace publication's
identity evidence with export rows.

Read the accepted tree once per opened binding and reuse its canonical ordered
entries during that operation. Build the final Portable snapshot once and use
those exact entries for comparison and commit. This is operation-local input,
not a persistent cache. Prefer deleting duplicate assembly and adapters over
adding a new snapshot framework. Native Git tree-identity comparison is a
possible later alternative, not a preselected extra layer or required slice.

Preserve `AuthoredNoteDocumentPersistence`, `Note.replaceContent`,
`NoteReferenceService`, and `WikiLinkResolver` as the existing content/reference
owners. Measured response work is secondary. No speculative link batching,
memoization, new derived index, or rewritten resolution policy is planned.

Constraints: Accepted ADR [0001](../../../docs/adrs/0001-ubiquitous-language.md)
(domain names/minimum representations), [0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and its [architecture](../../../docs/notebook-git-synchronization.md)
(atomic projection/head and append-only original history),
[0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(authored references and Portable bytes),
[0005](../../../docs/adrs/0005-web-routes-accepted.md) (destinations),
[0006](../../../docs/adrs/0006-failure-handling-accepted.md) (visible failures),
[0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(environment ownership). Follow [One complete accepted web change](../../NORTH-STAR.md#one-complete-accepted-web-change).
No ADR exception or new North Star topic is required by the selected design.

## Baseline prerequisite and outside-in proof

Before any product change, capture the source's 20-sample/three-warm-up browser
baseline on an owned disposable E2E stack with notebook-1-like size, folder
depth, content/link distribution, and accepted history. Reuse the existing
E2E runner and publication profile's ownership/JFR practices; its bulk publish
timings do not measure this endpoint. Keep setup outside timed intervals and
retain a reproducible sanitized fixture and exact invocation with the evidence.
Use the same initial fixture/history for before and after; repeated saves must
actually change bytes. Record first-save timing separately from warmed samples.

Measure edit/flush, request start/end, and visible completion separately.
Observe the dirty state clear and refreshed links after successful save, then
reload to prove persistence. Do not use disappearance of the busy bar alone.
Cover prose edits with existing links and edits that add/change a link; compare
plain content as a control. Use normal rich-editor interaction for the timing
case and existing Markdown-editor proof for preservation. Keep timers real.

Missing baseline blocks dependent optimization, not unrelated fixture setup.
If the representative request has a different dominant cost, revise this plan
before implementing the proposed simplifications. The diagnostic controller
median above cannot substitute for the browser baseline or prove 4×.

## Ordered slices

Target approximately five minutes of implementation, verification, and cleanup
per leaf. Scrutinize work exceeding five; at ten minutes stop and finer-decompose
unless the extra time is the required full backend suite or the bounded large
fixture/profile run. Record such external verification time separately; it does
not excuse an oversized implementation. Every delivered boundary stays green.

### 1. Read Portable snapshots through their existing flat representation

Type: Structure
Status: planned
Size hypothesis: about five minutes of edits; required full-suite time separate.

Have the existing export-row owner obtain folder/note scalar projections
directly from the repositories, preserving order, null/root placement, display
names, complete stored content including trash, and README/empty-folder bytes.
Remove entity-to-row mapping where no entity is needed. Do not change ordinary
note query meanings or introduce another export shape. This enables slice 2 to
compare complete notebooks without managing every note/folder as an entity.

Proof: existing `NotebookExportControllerTest`, `NotebookExportServiceTest`,
`PortableTreeSnapshotTest`, and Git cutover/controller coverage, through the
full backend suite. Add only a missing stable-boundary case if inspection finds
a projection semantic gap. Export bytes must remain unchanged.

### 2. Save a changed note without hydrating the entire notebook as entities

Type: Behavior
Status: planned
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

Within the existing owner, reuse the accepted entries across before/after
comparisons and reuse the final snapshot for comparison and persistence. Delete
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

Literal established commands (record actual results during execution):

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/note_creation_and_update/note_edit.feature,e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature'
```

Frontend preservation evidence includes `TextContentWrapper.spec.ts`,
`NoteShowPage.autosaveTrash.spec.ts`, and content-undo coverage. If frontend code
changes, run `pnpm frontend:test` and `pnpm -C frontend exec vue-tsc --noEmit`
through Nix. No API/schema change is selected; if one becomes necessary, revise
the plan and apply generation/migration guidance. Do not run tests in Development.

Once implementation is authorized, use dough-execute-plan: Jidoka, fresh
dough-post-change-refactor agent, API generation when needed, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update, commit with check-only
hook, and push/CI ownership. Planning does not move the story to Taken and does
not authorize commit/push or implementation. Keep this plan for retrospective
and story wrap-up after execution.

## Refinement assessment and remaining concerns

Three slices; no completed implementation or accepted performance proof.
The cumulative rule is one complete persisted projection per state and one
transactional acceptance owner, not successive endpoint special cases.

The initial broad idea of optimizing wiki-link resolution was not selected:
measured persistence dominates. The potentially large loader change is split
into flat-projection Structure immediately followed by its consuming Behavior.
No separate profiling-only delivery or test-only slice is used.

Slice 2 remains sensitive to callback adaptation and must be further split if
it exceeds the editing budget; do not split off broken caller changes. Slice 3
has a material efficacy risk: current evidence does not establish that these
removals alone can exceed 4× browser-visible improvement. A smaller improvement
does not authorize relaxing the target or adding complexity. The browser
baseline prerequisite and this final acceptance gap prevent certifying the
whole story ready for unconditional execution.
