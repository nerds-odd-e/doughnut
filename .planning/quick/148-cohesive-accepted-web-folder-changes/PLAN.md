# Cohesive accepted web folder changes

Status: planned
Source: [SEED-035 story 1](../../seeds/SEED-035-web-folder-rename-dissolve-accepted-tree.md#story-1).
Authority: 2026-09-18 owner approval of the refined scope and instruction to
write a slice plan, refining it if needed. Planning only; no execution authority.
Inspected product baseline: `2169b08f15` on `main`.

## Goal and scope

For a notebook whose current Portable projection matches accepted Git history,
web folder rename and dissolve retain that agreement. Each complete operation
adds one accepted child commit when its tree changes, and none for a no-op.
Database changes and accepted-history persistence share the transaction.

The same story owns the evidenced structural correction: accepted-web-change
coordination is duplicated in `WebFolderCreationService` and
`WebNoteCreationService`. Remove those competing representations while retaining
their existing observable admission and construction behavior. This is current
domain repair, not preparation for hypothetical operations.

Included: final descendant paths, existing merge/collision behavior, reference
rewrites in the notebook, README and `.keep` representation, surviving note IDs
and learning history, locked current-state validation, no-op/drift/unbound
behavior, and preserved existing accepted-change callers.

Excluded: repair of prior drift; expanding Git participation of Wikidata or
non-ordinary creation; accepted-history coordination of cross-notebook
referrers or cross-notebook moves; new dissolve/merge/README/trash policies;
Git publication or rename-inference changes; new UI; unrelated service cleanup.
Existing cross-notebook reference rewrites continue, without a new promise that
those other notebooks receive accepted commits.

## Existing solutions and domain boundaries

PFE inspection covered web controller callers, all `AcceptedWebChangeService`
callers, accepted snapshot persistence callers, construction, export and
publication consumers, and controller proof. Current ownership:

| Responsibility | Choice and evidence |
| --- | --- |
| Complete accepted web change | Reuse `AcceptedWebChangeService.apply`: lock bindings in order, validate bundle/head, compare initial projection, run mutation once, flush/read final persisted tree, conditionally persist one child per eligible changed notebook. |
| Folder operation and current-state validation | Reuse `FolderRelocationService.applyLiveFolderChange`; rename/dissolve keep their domain recipes and delegate once around the complete recipe. |
| Subtree structure and collision/merge | Keep `FolderSubtree`, `FolderConstructionService`, and sibling validation; no new folder operation model. |
| Reference meaning and mutation | Keep `WikiLinkRewriteService` capture and `WikiLinkRelocationRewrite`; neither becomes Git coordination. |
| Creation | Keep construction in existing construction owners; web creation adapters retain real variant selection and delegate coordination. |
| Portable representation and commit persistence | Reuse `PortableTreeSnapshot`, `NotebookGitProjection`, `AcceptedSnapshotPersistence`; callers must not maintain alternative proposed-tree lists. |
| Publication, cutover, export | Distinct purposes; sharing codecs/persistence does not make them web changes. Leave their orchestration intact. |

Both requirements are hard constraints. There must be one authoritative
accepted-web-change policy, and that owner must not know whether its caller is
rename, dissolve, creation, trash, or reduction. No operation flags, registry,
generic workflow framework, second snapshot coordinator, or new identity model.
Distinct domain recipes and small delegating adapters are legitimate. Assess
semantic ownership, not the number of classes or matching lines of code.

### Creation admission: preserve meaning without inventing extension points

`WebNoteCreationService` currently bypasses accepted-history coordination for
Wikidata-assisted or non-ordinary content. Preserve those branches in the web
creation owner. The ordinary path also checks `isRepresentedFolder` after the
initial projection matches. Current evidence supports removing that redundant
check without adding a generic eligibility callback:

1. `NotebookGitStateLoader` loads the notebook's folders and all stored notes.
2. `NotebookGitProjection.matchesAcceptedTree` compares the full canonical
   `PortableTreeSnapshot` against accepted entries.
3. `PortableTreeSnapshot.collectDirectory` emits a README, note, descendant
   entry, or `.keep` for every valid non-root folder. Root is always represented.
4. A valid destination folder in a matching tree therefore has an accepted
   entry under its path. An absent/foreign destination is independently refused
   by `NoteConstructionService`, before creating a note. A legitimate extra
   folder absent from accepted history is already projection drift.

This is a code-supported equivalence, not executed proof. Slice 5 must obtain
baseline characterization before removing the check. Preserve ordinary creation
under `.keep`, nested folders and README-only folders, legacy/drifted empty
folders without an accepted marker, and invalid/foreign destinations. If an
actual supported counterexample invalidates the implication, stop that removal
and refine the design; do not silently change admission or add a policy framework.
Remove `isRepresentedFolder` only if it has no remaining caller; other represented
folder logic used by publication has a different purpose and stays.

The construction method declares IO/interruption exceptions because it can fetch
Wikidata. The accepted ordinary path passes no Wikidata service. Expose that
existing synchronous construction capability within `NoteConstructionService`,
sharing its create/content/finalization code with the enriched path. Preserve
the enriched path's fetch-before-final-refresh ordering, image cleanup, reference
indexing and response construction. Do not spread IO exception contracts through
all accepted-change callers, catch impossible IO failures, or duplicate creation.

### Transaction boundary

`NotebookController.renameFolder` and `dissolveFolder` currently start default
transactions, while the shared owner requests SERIALIZABLE and rollback for
exceptions. A REQUIRED inner transaction does not upgrade its caller. Align
these two endpoint transaction declarations with the established accepted-change
endpoints so the real request uses the intended boundary. Keep one transaction;
do not use REQUIRES_NEW, commit after the controller returns, or catch-and-continue.
Re-resolve the folder and revalidate membership/authorization under the binding
lock; capture references only after that resolution and before mutation.

Direction: [NORTH-STAR — One complete accepted web change](../../NORTH-STAR.md#one-complete-accepted-web-change).
Accepted ADR constraints: [0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
for Portable representation; [0005](../../../docs/adrs/0005-web-routes-accepted.md)
for note-ID routes; [0006](../../../docs/adrs/0006-failure-handling-accepted.md)
for propagation. Proposed ADR 0002 remains non-binding. No new direction topic
or ADR is required by this plan.

## Proof ownership

All checks below are planned. No tests were run during planning. Existing test
inspection identifies reusable coverage, not a claim that the current suite passed.

| Promise | Owner | Observation |
| --- | --- | --- |
| Complete rename result, including same-notebook references, README, descendant paths, IDs and learning | 2 | Controller rename from synchronized fixture; read accepted blobs/tree and preserved note/tracker; parent is precisely previous head. |
| Rename no-op, conflict, wrong notebook/authorization, drift and no binding | 2 | Controller cases; inspect database and copied head/bundle values, no new binding; no-op includes whitespace-normalized name. |
| Rename does not strand subsequent edits | 2 | Rename followed by ordinary content save; second head is child of rename head and contains edited bytes. |
| Complete dissolve result, with promotion and requested recursive merge | 3 | Controller dissolve; final accepted tree includes promoted and merged result, references, preserved identities; dissolved README disappears and retained destination README remains. |
| Dissolve refusal, wrong notebook/authorization, drift and no binding | 3 | Controller cases; compare database placement and copied accepted head/bundle; preserve existing refusal semantics. |
| Empty-folder representation after either operation | 2, 3 | `.keep` moves on rename; dissolving last empty child leaves parent `.keep`, or no marker at notebook root. |
| Atomic accepted update through actual endpoint transaction | 2, 3 | Existing late-binding-save injection pattern, invoke controller outside a wrapping test transaction, reread committed database/binding after failure. |
| Existing folder creation behavior and rollback | 1 | Existing creation/atomic controller tests plus missing unbound/drift characterization. |
| Ordinary/enriched construction, normalization, references and response | 4 | Existing ordinary creation and Wikidata enrichment controller observations, with baseline before construction extraction. |
| Ordinary and excluded creation admission and rollback | 5 | Existing note creation/folder/atomic and Wikidata controller tests plus missing destination characterization. |
| Existing move/trash/delete/edit/reduction behavior | 1–5 | Full backend suite; inspect known external observations listed below, preserve multi-notebook reduction and per-notebook drift. |
| One representation of coordination without false generalization | 1, 5; final review | Inspect all production callers/persistence references: creation adapters have no duplicated lock/head/drift/snapshot/commit sequence; distinct publication and domain recipes retain ownership. |

### Inspected reusable proof

Paths below are under `backend/src/test/java/com/odde/donut/controllers/`.

- `NotebookGitFolderCreationControllerTest`: web creation of root/nested empty
  folders, `.keep`, and parent chain. `NotebookGitFolderCreationAtomicControllerTest`:
  injected late binding-save failure, folder count and binding reread in a fresh
  committed transaction.
- `NotebookGitNoteCreationControllerTest`: canonical content, existing blobs,
  nonbinding, drift, relationship bypass and validation. Its support helper
  compares a binding entity; for preservation proof capture scalar/head and
  cloned bundle values before mutation rather than assuming an entity cannot
  be updated in place.
- `NotebookGitNoteCreationFolderControllerTest`: web-created `.keep` becomes
  first note, nested folder placement, unchanged README bytes, linear parents.
- `NotebookGitNoteCreationAtomicControllerTest`: ordinary-note metadata/content
  and late-save rollback of note, creator, references and binding in fresh
  transactions. `NotebookRootNoteCreationWithWikidataTests`:
  `wikidataAssistedRootCreateDoesNotAdvanceAcceptedHead` plus enrichment behavior;
  external HTTP is the mock boundary. Capture independent before-values here too.
- `NotebookFolderRenameControllerTest` and `NotebookFolderDissolveControllerTest`:
  current domain mutation/refusal. Their wiki-link rewrite sibling classes prove
  database rewrites, not accepted blobs. Add the missing Git observation in slices
  2/3 rather than counting those fixtures as synchronization proof.
- `NotebookGitWebFolderMoveControllerTest`: final subtree, in-notebook rewrites,
  learning identity, collision, unbound and drift. `NotebookGitWebFolderTrashAtomicControllerTest`:
  reusable failure injection and fresh-transaction observation pattern.
- `NotebookGitWebRelationReduceControllerTest`: two-notebook commits and mixed
  drift. Existing `NotebookGitWeb*Trash*`, `NotebookGitWeb*PermanentDelete*`,
  `NotebookGitWebContent*` and `NotebookGitWebNoteMove*` protect the other callers.

Use real controller entry points, database, projection and Git persistence. Do
not mock the shared owner or assert helper invocation counts. Establish accepted
preconditions before the trigger; never construct the expected accepted result
through a fixture after the operation. Add observations only where missing.

## Ordered slices

### 1. Folder creation shares accepted-web-change coordination
Type: Structure
Status: done

Structure: remove the documented duplicate coordination from
`WebFolderCreationService`, using the shared owner and existing folder
construction. This directly owns the approved retrospective structural
correction and establishes the same folder consistency rule used by slice 2.

Keep root/parent/context-note selection, authorization, returned Folder, commit
message and history semantics. Obtain missing pre-change characterization for
unbound and already-drifted notebooks before editing; then delegate the complete
construction using the locked notebook when available. No mutation-specific
branch or additional extension point in the shared owner is needed.

Proof: existing root/nested `.keep` and late-save rollback tests; new drift/unbound
cases inspect actual folder creation and unchanged or absent binding. Use the
full backend command below. Final code inspection shows no direct bundle import,
projection comparison, tree build or accepted persistence in this adapter.

Sizing: approximately 5 minutes active work plus required backend-suite runtime;
one adapter replacement with established atomic proof. Safe stop: creation is
cohesive and behavior is preserved; rename/dissolve remain explicitly unfinished.

Result: `WebFolderCreationService.createFolder` now delegates to
`AcceptedWebChangeService.apply` using the same
`locked.state(id).map(...notebook).orElse(notebook)` fallback as
`FolderRelocationService`; no mutation-specific branch was needed in the shared
owner. `NotebookController.createFolder` dropped its now-unused `throws
IOException` (nothing in the call chain throws it; consistent with sibling
endpoints that only declare `IOException` where they actually do IO). Added
`nonGitNotebookFolderCreationCreatesNoBinding` and
`driftedNotebookFolderCreationKeepsMutationAndAcceptedHistoryUnchanged` to
`NotebookGitFolderCreationControllerTest` for the missing unbound/drift
characterization. Refactor pass consolidated a newly-duplicated
`countFoldersForNotebook` native-query helper (already duplicated in two
sibling atomic test classes) into `NotebookGitBundleControllerTestBase` as the
one authoritative home. Full backend suite: `CURSOR_DEV=true nix develop -c
pnpm backend:test_only` — `BUILD SUCCESSFUL`, no failures. Learning: this push
`NotebookGitBundleControllerTestBase.java` to 257 lines, just past this
project's 250-line file-size check; flagged but not split, since splitting a
62-caller shared fixture base is unrelated restructuring outside this slice's
concept.

### 2. Folder rename records the complete accepted result
Type: Behavior
Status: planned

Behavior: a synchronized notebook contains a folder, a nested learned note,
folder README and an in-notebook referrer → rename through the web endpoint →
one accepted child contains the final paths and rewritten bytes, while note and
learning identities survive. A subsequent ordinary content edit extends that head.

Wrap the existing rename recipe in `applyLiveFolderChange`, using the reloaded
folder/notebook and shared timestamp. Keep name validation, no-op and reference
rules with their existing owners. Align the controller transaction as above.
Do not implement name-segment rewriting or snapshot construction again.

Proof: add controller-level accepted-result coverage beside the existing Git
folder move tests, with focused variants for empty `.keep`, no-op, conflict,
unauthorized/wrong-notebook, unbound and drift; reuse existing domain assertions.
Use a late binding-save failure to observe rollback of folder name, rewritten
referrer and accepted binding outside a wrapping test transaction. Atomicity is
an explicit business promise, not a test of every propagated exception.

Sizing: target 5 minutes; scrutinized estimate 5–10 minutes active work because
the boundary has missing Git observations. All cases exercise the one endpoint
change; do not split off a later tests-only slice or deliver without its guards.
Backend-suite runtime exception applies. Safe stop: rename is complete; existing
dissolve limitation remains visible, without a temporary mode in production.

### 3. Folder dissolve records promotion and merge as one accepted result
Type: Behavior
Status: planned

Behavior: a synchronized folder contains direct notes and nested subfolders →
dissolve, optionally requesting existing merge behavior → one accepted child
contains the final promoted/merged structure and rewritten references, preserves
surviving note/learning identities, and omits the dissolved folder and its README.

Wrap the complete existing dissolve recipe once in `applyLiveFolderChange`.
Keep collection, recursive merging, name conflicts and removal in their current
domain owners. Return adaptation for the void endpoint is internal, not a new
API response. Align the controller transaction. Do not commit low-level reparent
or merge calls separately or invent a README transfer policy.

Proof: controller scenarios for ordinary promotion and merge requested; include
a same-notebook referrer and copied pre-operation learning identity. Refusal
without merge retains state/head. Focused empty-folder, drift, unbound and access
cases own the table's boundary promises. Late-save rollback rereads the removed
folder, promoted/merged placement, referrer text and binding in fresh transactions.
Reuse existing dissolve tests for domain variants rather than duplicating all of
them in a Git test class.

Sizing: target 5 minutes; scrutinized 5–10 minutes active work with existing
subtree/merge and atomic fixtures; suite-runtime exception applies. Proof setup
is the risk, not extra production recipes. Safe stop: both requested folder
behaviors work; remaining structural duplication in note creation is still an
explicit story obligation.

### 4. Local note construction and optional enrichment retain distinct responsibilities
Type: Structure
Status: planned

Structure: expose the existing synchronous construction capability in
`NoteConstructionService` while keeping optional external enrichment distinct.
This is the checked-exception part of the approved creation-coordination
correction, needed by the concrete ordinary caller in slice 5. No standalone
framework or unused future extension is introduced.

Separate the synchronous construction capability within `NoteConstructionService`
from optional Wikidata enrichment without copying the common persistence and
finalization steps. Preserve create, optional enrichment, final refresh, image
cleanup, reference indexing and response ordering. Make the ordinary branch of
`WebNoteCreationService` use this synchronous entry now, leaving its current Git
coordination in place until slice 5. The enriched branch keeps the existing
IO/interruption contract. No catch for an impossible network call, wider throws
contract on the shared owner, or duplicate core constructor.

Proof: inspect and run ordinary creation/content/reference and Wikidata
enrichment controller coverage before extraction; obtain any missing observation
needed for a changed finalization boundary. After extraction the same full
backend suite observes unchanged content, references, response and Git outcomes.
Reuse actual production callers; no test solely of a newly extracted helper.

Sizing: approximately 5 minutes active work plus mandatory suite runtime. The
current method is small; this slice is one behavior-preserving extraction with
one preservation proof loop. If preserving ordering requires a larger redesign,
stop and refine rather than invent a general construction pipeline.
Safe stop: ordinary and enriched construction keep their behavior, and the
synchronous entry already has its real caller; Git consolidation remains pending.

### 5. Ordinary-note creation shares the same accepted-change owner
Type: Structure
Status: planned

Structure: remove the remaining documented duplicate accepted-change sequence
from `WebNoteCreationService`. This directly completes the approved structural
correction, using the synchronous construction capability from slice 4.

Keep enriched/non-ordinary bypass selection in the web creation owner; route only
the already-supported ordinary path through `AcceptedWebChangeService`. Remove
caller-owned proposed note lists and snapshots. Keep commit message, timestamp,
response and existing admission outcomes.

Before changing admission, characterize `.keep`, README/nested destinations,
unrepresented drifted folder, and absent/foreign destination. Apply the
represented-folder equivalence above only if those observations and the
production path support it. No new predicate/plugin API is justified by a
redundant condition. Preserve response, commit metadata, content normalization,
image cleanup, derived references and existing checked exception contracts at
the external-enrichment boundary.

Proof: reuse note creation, folder creation, Wikidata and atomic suites and add
only missing pre-change observations. Preserve independent before-values when
the existing assertions use live binding entities. After consolidation, read all shared owner
callers and accepted persistence references to establish structural completion;
publication/export remain separate and multi-notebook reduction still uses the
existing set owner. Run the full backend suite.

Sizing: target 5 minutes; scrutinized estimate 5–10 minutes active work for the
adapter replacement and missing admission observations, plus mandatory suite
runtime. Existing `.keep`, README, nested-folder, drift, relationship, Wikidata
and rollback observations reduce new proof work. The implication in the design
section bounds the admission question; an observed counterexample triggers
reassessment rather than another special-case branch.
Safe stop: all functional and structural obligations are covered, subject to
their execution evidence, delivery and retrospective. This is not a completion
claim for work that has not run.

## Verification and delivery contract

Run from the eventual execution checkout, using its isolated database as the
repository worktree tooling documents. No migration is planned:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

Backend skill requires all backend unit tests, not filtered `--tests` runs.
For Structure, obtain needed baseline observations before altering behavior;
for new Behavior, demonstrate missing accepted-result proof before fixing it.
After edits, run the full suite and retain literal command/result and inspected
observations in this plan. Do not label current inspection as a passing run.

Execution leaves target about 5 minutes including verification; scrutinize >5
and finer-decompose >10 absent a stated reason. Required full backend-suite
runtime is a stated exception because splitting tests cannot shorten that gate;
record active-work and test-wait time separately. It is not an exception for
unbounded coding or debugging. Stop an oversized attempt and refine safely.

Per-slice authorized execution delivery: Jidoka; fresh
`dough-post-change-refactor` agent; API regeneration only if controller/DTO
OpenAPI shape changes; coordinator `./scripts/run.sh pnpm format:changed` once;
update plan; check-only lint hook on commit; push and asynchronous CI handling
through `dough-execute-plan`. Implementers/refactorers run neither routine
formatting nor standalone `lint:changed`. Retain plan/seed for retrospective and
story wrap-up after execution. Planning does not take the backlog item.

## Current decisions and concerns

- Owner approved both functional and structural scope; neither side of the
  cohesion/domain-boundary requirement is optional.
- No change to creation admission outcomes, cross-notebook scope, or drift policy.
- Snapshot final persisted state; no special representation per endpoint.
- Refined assessment: slices 2/3 have tightly related endpoint cases but use
  established proof patterns. The original combined note-creation Structure
  slice was replaced by slices 4/5; no production checks have run.
- Execution identity, proof results and delivery status remain unset until
  implementation is separately authorized.

## Slice-plan refinement assessment, 2026-09-18

The owner separately authorized refinement if needed. Reassessed the smallest
approved outcome against the alternatives: adding only two delegation calls
leaves documented duplicate coordination; a universal mutation framework adds
no demonstrated value. Retain the selected domain ownership and current scope.

Replaced original slice 4 with current slices 4/5 because checked-exception
separation and admission-preserving Git consolidation had independent structural
changes and could exceed the active-work limit together. Result: five slices,
all planned, no obsolete slices included in the count, no story resplit needed.
Structure slices directly own the evidenced correction named in this story;
they are not additional feature stories or speculative preparation.

| Slice | Assessment | Remaining concern and consequence |
| --- | --- | --- |
| 1 | Ready | Missing creation drift/unbound observations must precede removal of the old path. |
| 2 | Ready with stated runtime exception | Several boundary examples, one complete rename rule; fresh transaction rollback setup must use the existing pattern. Split only if active work exceeds the limit, preserving full endpoint proof. |
| 3 | Ready with stated runtime exception | Merge/removal rollback must observe real committed state; merging and snapshotting remain one complete operation, not separate commits. |
| 4 | Ready | Preserve enrichment/finalization order. Sharing construction must not make Wikidata an accepted operation. |
| 5 | Ready with stated runtime exception | Obtain missing admission characterization; counterevidence to represented-folder equivalence stops removal and causes reassessment, not policy expansion. |

The sole timing exception is the mandatory complete backend-suite run, applicable
to every slice. No measured duration is claimed. Each slice retains a single
behavior or structural preservation loop; no tests-only slice or caller-specific
commit mode was added. The cumulative result has one web consistency owner,
distinct folder/construction/reference owners, and unchanged publication ownership.
No unresolved domain-scope or ADR conflict was identified by this assessment.
The sequence is ready for direct execution under a future execution instruction;
the current instruction authorizes only this plan and its refinement.
