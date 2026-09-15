# Receive web-created folders and their notes locally

Status: planned; shared-projection concern requires resolution before slice 1

## Source and authority

[SEED-009 story 35](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-35)
and the owner's 2026-09-15 clarifications govern this plan. The
[product direction](../../PRODUCT-BACKLOG.md#near-future-direction) retains one
append-only history after the planned production baseline reset.

This request authorizes story refinement, slice planning, and necessary plan
refinement. It does not authorize implementation, production changes, or release.

## Goal, scope, and decisions

An owner creates a folder and ordinary notes on the web, receives the hierarchy
through clone/clean fast-forward pull, and continues with an ordinary local
note edit and publish. Empty folders survive before their first note exists.

- Generate a zero-byte `.keep` only for an otherwise empty non-root folder:
  no notes, non-blank Readme, or child folders. A nested empty leaf represents
  its ancestors. Blank Readmes produce no README file.
- Remove a generated marker when a note, README, or tracked descendant represents
  the folder. This is a canonical snapshot rule, not a cleanup job or persisted
  marker state. Test README presence in snapshot data; new Readme editing remains
  story 24's responsibility.
- Cover first snapshots of existing notebook contents, web root/nested folder
  creation, first ordinary note creation, receipt with or without intervening
  pulls, and subsequent local editing/publication with unchanged markers elsewhere.
- Preserve content, authorization, note/folder identity, learning associations,
  transaction integrity, and post-reset accepted/local commit identities.
- The owner will reset initial bundles for all production notebooks. Old-bundle
  backfill and compatibility are excluded. Reset execution is not performed by
  this plan. Whether reset preparation belongs here is pending clarification;
  until answered, retain it as the supplied external rollout prerequisite.
- Exclude new local empty-folder authoring, arbitrary non-Markdown-file support,
  intentional local marker editing, folder move/rename/dissolve expansion, trash
  compatibility, divergence recovery, performance targets, and new creation UI.
  Exclusions do not authorize breaking already-supported behavior or discarding
  authored bytes. An unsupported authored `.keep` is not disposable scaffolding.

## Existing solutions and architecture

PFE inspection on 2026-09-15 selected these existing owners:

| Responsibility | Evidence and decision |
| --- | --- |
| Canonical Portable files | Change `services/notebookExport/PortableTreeSnapshot`; `NotebookZipBuilder`, `NotebookGitCutoverService`, `WebNoteCreationService`, `WebNoteEditService`, and `NotebookGitProjection` already consume it. Do not add a Git-only snapshot variant. |
| Create folder with permissions and placement | Reuse `NotebookController.createFolder` and `FolderConstructionService`. Add the web history coordination at the web-authoring boundary; shared construction also serves trash and Git materialization, so it must not independently append commits. |
| Append accepted history | Reuse `NotebookGitStateLoader` locking and `AcceptedSnapshotPersistence`; follow `WebNoteCreationService` transaction and matching-projection pattern. Folder creation must include its new folder row in the resulting snapshot. |
| Import/publish | Reuse `NotebookGitProposalImporter`, safe regular-file inspection, current document application and final projection verification. Unchanged marker blobs are tree context, not note documents. Do not add a publication mode for this story. |
| Lint | Current notebook lint reads domain rows through `HealthRuleRunner`, not arbitrary files. Markdown proposal validation skips non-Markdown paths. Preserve these purposes; do not create a new marker lint subsystem. Existing empty-folder advisories need not disappear. |
| Empty-folder detection | Health's `FolderSubtreeLiveNotes` finds note-empty subtrees for advisories/purge. That is different from a leaf needing tracked representation; do not reuse purge semantics to delete folders or generate ancestor markers. |
| First baseline | `NotebookGitCutoverService` builds the initial snapshot. New notebooks are bound at creation; download/clone does not lazily create a missing binding. Its resnapshot method is testability-only, not a production reset API. |

Source paths above are relative to
`backend/src/main/java/com/odde/donut/`.

[ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
requires tracked representation of empty folders, omission of blank Readmes,
and one lossless export/import/lint contract. `.keep` is structural, not a concept.
[ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)
permits loud failure; transactional preservation is the tested business outcome,
not a new error-recovery framework. The relevant
[North Star topic](../../NORTH-STAR.md#one-final-publication-result) keeps one final
publication application path and existing identity owners. No new architecture
topic, ADR amendment, storage table, or UI is justified.

## Proof ownership

All references below are planned evidence; no product tests ran during planning.
Unit tests use real controller boundaries, committed transactions when needed,
and existing `makeMe`/bundle helpers. Snapshot policy tests may use the existing
stable `PortableTreeSnapshot` contract. Do not mock the folder/history behavior
being promised or use testability resnapshot after a web action under test.

| Promise | Owning slice | Observation |
| --- | --- | --- |
| Only otherwise empty folders get markers; nested ancestors and README/note folders do not | 1 | Exact snapshot/ZIP entries across compact data cases; blank Readme stays omitted |
| First baseline retains existing empty folders without changing content/identities | 1 | Real cutover result downloaded through controller; exact tree and retained database identities |
| New web folder arrives locally as an empty folder | 2 | Installed CLI pulls web-created folder; `.keep` and old-head ancestry are observed |
| Failed web folder creation cannot leave folder and history inconsistent | 3 | Late binding-write failure leaves both committed folder rows and accepted bundle unchanged |
| First note replaces marker without manual sync/repair | 4 | Folder-only head followed by note head; note bytes, absence of marker, same folder identity |
| A child folder replaces its parent marker with a leaf marker | 5 | Controller-downloaded tree contains only the leaf marker |
| Nested creation and note creation can be received together | 6 | One clean pull gets full nested path without redundant markers and preserves old head |
| Received note remains editable/publishable; other empty folders survive | 7 | Local proposal accepted unchanged; same note/tracker, downloaded unchanged sibling marker |
| Existing authorization, placement, note-history, and publication behavior | Each affected slice | Existing controller/full backend suite plus named relevant CLI proof; investigate new regressions rather than relabel them unsupported |

## Verification and delivery contract

Commands are literal from repository root in the eventual execution checkout:

- Backend unit proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend rule requires the whole backend unit suite, not selected classes.
- Existing clone boundary: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_clone.feature` when slice 1 changes clone-visible output.
- Web folder/note receipt: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature` for slices 2, 4, 6, and 7 when their new scenarios are added there.
- CLI unit proof, only if CLI production changes: `CURSOR_DEV=true nix develop -c pnpm cli:test`.

Inspect/reuse steps in `e2e_test/step_definitions/cli.ts` and page objects under
`e2e_test/start/pageObjects/cli/`; use real web folder/note actions for the
trigger and installed CLI for receipt. Existing feature scenario “Publishing a
local refinement of received web-created note text updates Donut” is the
continuation pattern. Its current root-note setup does not prove new folders.
No manual-browser testing or all-E2E run is prescribed.

Every slice targets approximately 5 minutes including focused proof and local
cleanup; inspect any >5-minute hypothesis and stop/reassess above 10 minutes.
The required full backend suite and focused Cypress stack startup are explicit
external-wait exceptions: record actual time separately; these exceptions do
not excuse >10 minutes of implementation or repeated unproductive attempts.

At each eventual delivery boundary follow dough-execute-plan: Jidoka, fresh
independent dough-post-change-refactor agent, API generation if signatures or
DTOs changed, coordinator runs `./scripts/run.sh pnpm format:changed` once,
update this plan, commit with check-only lint hook, push, and asynchronous CI
handling. Implementers/refactorers do not run format:changed or lint:changed.
Keep this plan and proof through retrospective; story wrap-up owns cleanup.
The current pre-existing backlog/story-38 edits are not implementation-owned.

## Ordered slices

### 1. First snapshots retain only otherwise empty folders

Type: Behavior
Status: planned; held on concern C1 below
Proof: Backend command; extend `PortableTreeSnapshotTest`, the existing cutover
proof, and `NotebookExportControllerTest`/bundle download assertions only for
these distinct consuming boundaries. Reuse existing content-preservation cases.

Behavior: Given notebook content with an empty leaf, a blank-Readme leaf, a
README-only folder, and a note-bearing folder, when its Portable snapshot and
first binding are generated, only the empty leaves contain `.keep`. Downloaded
content and original database identities are preserved.

Implementation direction: One empty-directory rule in the existing snapshot
traversal, with zero-byte markers and no root marker. Inspect changed snapshots'
callers before activation. Do not deliver a globally changed builder while
known existing publication journeys fail. First-snapshot fixtures may use real
cutover; later web-action proofs must not repair their own history via fixtures.
Sizing: 5–8 minutes active work, medium confidence plus suite wait. The small
policy is cohesive; C1, not data-case count, is the material uncertainty.

### 2. Receive a web-created empty folder

Type: Behavior
Status: planned
Proof: Backend command and web-created-note feature command. Add a folder
creation controller case and one real web-create/CLI-pull scenario.

Behavior: Given a synchronized notebook and clean clone, when the owner creates
`Biology` on the web then pulls, the checkout contains `Biology/.keep` at an
accepted descendant of its original head.

Implementation direction: Reuse folder placement and existing accepted-snapshot
coordination. Lock before evaluating the starting projection and keep folder
creation and accepted history in one transaction. Preserve current authorization
and duplicate-name behavior. Do not append history from generic folder helpers.
Sizing: 5–10 minutes active work, medium confidence, plus suite/stack wait.
Stop above the hard limit; do not treat the coordinator wiring as a second
implementation phase outside this estimate.

### 3. Failed folder creation leaves accepted notebook state intact

Type: Behavior
Status: planned
Proof: Backend command; follow
`NotebookGitNoteCreationAtomicControllerTest`'s real committed-transaction and
late binding-save failure seam, asserting folder rows and accepted binding.

Behavior: Given a synchronized notebook, when a web folder creation cannot
persist its accepted bundle, neither the new folder nor a partial history
advance is committed.

This tests transaction integrity, not the fact that an exception is thrown.
Keep the failure loud and reuse existing test failure injection. No retry UI,
new compensating transaction, or generic failure-injection framework.
Sizing: approximately 5 minutes active work, medium confidence plus suite wait.

### 4. Receive the first note in a previously empty web folder

Type: Behavior
Status: planned
Proof: Backend command and web-created-note feature command. Extend
`NotebookGitNoteCreationFolderControllerTest` through actual controller creation,
not a fabricated accepted folder; reuse bundle ancestry assertions.

Behavior: Given `Biology/.keep` was received from web folder creation, when the
owner creates ordinary note `Cells` in that folder and pulls, the checkout has
`Biology/Cells.md` and no `Biology/.keep`, retaining the same folder and all prior
commits. A title-only ordinary note is sufficient; authored-body preservation
already has existing note-save coverage and the continuation proof below.

Use `WebNoteCreationService` and the canonical snapshot. A marker represents
the destination without becoming a note or requiring a Readme.
Sizing: approximately 5 minutes active work, medium confidence plus suite/stack wait.

### 5. A nested empty folder replaces its ancestor marker

Type: Behavior
Status: planned
Proof: Backend command; a controller-created parent and child followed by a
bundle download. Use existing path/ancestry observations.

Behavior: Given accepted `Science/.keep`, create child folder `Biology` on the
web. The next accepted tree contains `Science/Biology/.keep` and no
`Science/.keep`, retaining the same parent identity and prior commits.

Use the same folder/history path and snapshot recursion. Do not add a separate
nested-folder handler or trigger a cleanup traversal outside snapshot building.
Sizing: approximately 5 minutes active work, medium confidence plus suite wait.

### 6. Receive nested web authoring without intermediate pulls

Type: Behavior
Status: planned
Proof: Web-created-note feature command; backend command only if production
changes are needed. Extend the real web-authoring/installed-CLI scenario.

Behavior: Given a clean receiving checkout, create `Science`, child `Biology`,
and ordinary `Cells` on the web before the next pull. One pull receives
`Science/Biology/Cells.md`, with no `.keep` in either folder and the original
head retained as ancestor.

This exercises already-built behavior across several appended commits. Do not
add count/depth gates or a special batch path. If green without production edits,
the acceptance evidence is the slice's deliverable.
Sizing: approximately 5 minutes active work, medium confidence plus focused stack wait.

### 7. Publish a local edit while retaining received empty folders

Type: Behavior
Status: planned
Proof: Backend command and web-created-note feature command. Extend
`NotebookGitWebCreatedNotePublicationControllerTest`'s real web-created note,
tracker, proposed-head equality, and downloaded-tree observations.

Behavior: Given received `Biology/Cells.md` and an empty sibling `Chemistry/.keep`,
edit `Cells.md` locally, commit, and publish. The same Donut note and tracker
carry the new content, the exact local commit becomes accepted, and the sibling
marker remains unchanged and never becomes a note/Readme concept.

Change admission only if the observed ordinary edit requires it. Unchanged
regular files already remain context in `NotebookGitProposalTreeInspection`;
do not blanket-ignore arbitrary `.keep` edits or add local empty-folder creation.
Preserve Markdown validation and current health-lint meaning.
Sizing: 5–8 minutes active work, medium confidence plus suite/stack wait.

## Concerns and refinement assessment

### C1 — A supported local operation can create a newly empty folder

This is unrelated to legacy bundles. Inspection found
`NotebookGitProposalOrdinaryNoteApplication.applyDeletions` deletes a note,
`NoteService.permanentlyRemove` leaves its folder intact, and publication's
final `NotebookGitProjection.requireMatchingAcceptedTree` compares the complete
snapshot to the owner's commit. Deleting the last local Markdown file (or
moving it out) can therefore leave a live empty folder whose new snapshot
contains `.keep` while the proposed commit does not.

Before changing the shared builder, use the existing publication controller
boundary to establish the current last-note-deletion outcome on the execution
baseline. No test has been run here; this is a concrete source-derived risk.
Do not hide it by weakening exact tree validation, deleting a folder identity,
rewriting the owner's commit, or requiring a new manual step without an owner
decision. Resolve the resulting empty-folder contract with the owner if existing
semantics and the empty-only marker rule cannot both be preserved by the same
understood rule. Slice 1 and dependent activation remain held; finer slicing
alone cannot settle a product decision.

### Other limits

- Production reset preparation ownership remains pending the clarification
  asked in this conversation; no reset tooling or production run is included
  under the current external-prerequisite assumption.
- Full-backend and Cypress wall times were not measured in this planning turn.
  Sizing is a hypothesis, not an execution-time guarantee.
- No generic infrastructure uncertainty requires a separate experiment here:
  existing code already writes and reads arbitrary regular Git blobs. C1 is a
  product-semantic boundary, not an unknown Git storage capability.

## Learnings and retained evidence

- Planning inspected current source and named proof setups, but ran no product
  tests and changed no product code.
- Empty `.planning/quick/074-*` and `098-*` directories are remnants; Git history
  allocates through 125. This plan uses the next three-digit entry, 126.

## Slice-plan refinement — 2026-09-15

Replaced the original nested-authoring slice with slices 5 and 6: leaf-marker
replacement and combined receipt had separate observations. Renumbered local
continuation to 7 and retained its proof ownership. Seven slices result; no
story resplit recommendation. Existing PFE and North Star choices remain valid.

| Slices | Assessment | Reason |
| --- | --- | --- |
| 1 | Escalate on C1 | Shared snapshot activation meets a concrete unresolved future-operation contract; smaller implementation steps cannot choose folder/commit semantics. |
| 2–4 | Ready in shape, dependent on 1 | One web-authoring result each; atomic failure isolated from successful receipt. |
| 5–6 | Refined; ready in shape, dependent on earlier slices | Leaf-marker replacement separated from one-pull receipt. |
| 7 | Ready in shape, dependent on earlier slices | One existing edit/publish journey with unchanged marker context; no new marker editing policy. |

Cumulative model: snapshot traversal alone decides generated marker placement;
web operations append that full snapshot; publication retains its existing final
application and identity owners. No story-specific marker store or recognizer
is planned. C1 is the limit of this model, explicitly unresolved rather than
covered by a compatibility layer. No execution-readiness claim is made until it
is settled. No execution was started and no overrun work needs parking.
Sizing exceptions are the required backend suite and focused Cypress startup/
run time already stated above; no active-work exception is granted.
