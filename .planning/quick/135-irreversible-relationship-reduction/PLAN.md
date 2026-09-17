# Reduce a relationship note into its source irreversibly

Status: planned
Source: [SEED-024 story 1](../../seeds/SEED-024-atomic-relationship-note-reduction.md#story-1),
refined 2026-09-17 with owner decisions on learning, body text, attachments,
and cross-notebook sources. The owner authorized planning and plan refinement,
not execution.

## Goal and scope

A note owner who reduces a relationship note ends with one representation of
that relationship, the source property, in active notes, trash, undo, and the
Git-backed Portable tree. Learners keep their learning on that property.

Included:

- a dedicated backend reduction operation, separate from trash, that interprets
  the relationship note itself (source, target, property key);
- refusal when the relationship note has non-blank body text after its leading
  frontmatter;
- in one accepted web change: add the source property (existing collision
  suffixing), move every learner's note-level understanding tracker (active or
  removed from recall) to the resolved property key with its schedule and
  recall history, then permanently delete the relationship note (attachments,
  spelling and commissioned trackers, and other dependents go with it);
- the web client calls the new operation, records no undo, drops the removed
  note from its cache, lands on the source note, and labels the choice as
  permanent;
- removal of the reduce-via-trash contract: `REDUCE_TO_SOURCE_PROPERTY`,
  `sourcePropertyKey`, the client `sourceNoteId` option, and their callers.

Excluded: undo or recovery of reduction; the undo gap of "Remove from
properties of references"; cleanup of relationship notes trashed by earlier
reductions; Git-inferred or bulk reduction; SEED-020 renames; hiding the
reduce choice for notes with body text; any change enabling or constraining a
source note in another notebook
([SEED-025](../../seeds/SEED-025-cross-notebook-relationship-reduction.md#story-1));
database schema changes.

Key examples (from the seed):

1. "Moon a part of Earth" (empty body) → "Moon" has `a part of: '[[Earth]]'`,
   the relationship note is gone from notes and trash, no undo, owner lands on
   "Moon".
2. Same in a Git-backed notebook → one accepted commit modifies `Moon.md` and
   removes the relationship file; nothing under `_trash/`.
3. "Moon" already has `a part of` → new key `a part of 2`.
4. Two learners' understanding trackers (one removed from recall) → each
   becomes a property tracker for the resolved key on "Moon", same schedule,
   recall logs, and removed state.
5. Body "Observations from orbit." → refused; nothing changes.
6. Source link no longer resolves → refused; nothing changes.
7. Choosing Trash for a relationship note still trashes it with undo.

## Existing-solution and architecture assessment

| Responsibility | Existing solution and decision |
| --- | --- |
| Relationship interpretation and source mutation | `NoteReferenceHandling.reduceRelationNoteToSourceProperty` (reached through `NoteService`) already parses relationship frontmatter, resolves the source, authorizes, and adds the property with collision suffixing. Change this owner: derive the key from `relation` itself, check body text, move trackers for all learners, and permanently remove. Do not add a second relationship parser. |
| Property key from `relation` | The only kebab→label mapping is frontend `relationTypeFromKebab`. Every known relation label equals its kebab with hyphens replaced by spaces, so the backend uses that one rule, then `PropertyKeyNaming.canonicalExampleOfFamilyKey` as today. Unknown kebabs keep their literal words instead of the frontend's `related to` fallback (decision below). No backend relation-type catalogue. |
| Permanent deletion | `NoteService.permanentlyRemove(note, LEAVE_DEAD_LINKS, viewer)` is the Git-publication owner; DB `ON DELETE CASCADE` (`V300000325`) removes dependents. Reuse it after trackers are moved. |
| One accepted web change | `AcceptedWebChangeService.apply` locks the notebook, runs the complete operation, and snapshots live notes, so a removed note becomes a removed file. Reuse it (NORTH-STAR "One complete accepted web change"). `WebNoteEditService.edit` returns the edited note, which no longer exists after reduction, so the reduction service calls `apply` directly and returns the source note. |
| HTTP boundary | `RelationController` (`/api/relations`) owns relationship-shaped note operations. Add `POST /api/relations/{relationNote}/reduce-to-source-property` returning the source `NoteRealm`. |
| Client flow | `useNoteTrashFlow` offers the choice; `StoredApiCollection` owns API calls, cache (`storage.removeNoteRealm`, used by `undoCreateNote`), navigation, and sidebar refresh. Add a reduce method there beside `trashNote`; simplify `qualifyRelationNoteForReduceOnDelete` to decide only whether to offer the choice. |

ADR 0001 defines permanent deletion as ending identity and dependent data;
moving understanding trackers before deletion is the owner's decided exception
for learning. ADR 0004's relationship representation is unchanged. No North
Star change is warranted.

## Outside-in proof ownership

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Reduction adds the property and permanently deletes the relationship note, not trashing it | 1 | New `RelationControllerReduceToSourcePropertyTests`: examples 1 and 3 through `RelationController`; relationship note absent from `noteRepository`; returned realm is the source. |
| Git-backed reduction is one accepted commit without `_trash/` | 2 | Replace `reduceToSourcePropertyTrash…` in `NotebookGitWebTrashLinkedReferrerControllerTest` with a reduction test (move to a relation-named test class if it fits better): downloaded head has reduced `Moon.md`, lacks the relationship path, and has no `_trash/` path. |
| All learners' understanding learning moves; spelling/commissioned trackers go | 3 | Example 4 in the new controller test: two users' trackers, recall logs, next recall time, removed-from-recall state; a spelling tracker is gone. |
| Web reduction is permanent in the UI | 4 | `NoteMoreOptionsForm.deleteNote.relationship.spec.ts` and a store spec: reduce endpoint called, no undo entry, removed realm, navigation to source; `relationship_edit_and_remove.feature` reduce scenarios green against a body-less relationship. |
| Body text and unresolvable source refuse with no change | 5 | Examples 5 and 6 in the new controller test: error raised, relationship note and source content unchanged. |
| Reduce-via-trash contract is gone; trash is unchanged | 6 | Generated client lacks `REDUCE_TO_SOURCE_PROPERTY`/`sourcePropertyKey`; `NoteControllerTrashTests`, trash store/flow specs, and example 7 (existing "Deleting a relationship" scenario) green. |

## Current decisions

- Property key = `relation` value with hyphens as spaces, trimmed, then the
  existing example-of canonicalization. For all known relation types this
  equals today's key.
- Tracker move rule: every user's note-level `UNDERSTANDING` tracker (empty
  property key), regardless of `removedFromTracking`, is re-pointed to the
  source note with the resolved key. Spelling and commissioned trackers are
  left to cascade deletion (seed assumption). Recall logs follow the tracker.
- "Body text" = non-blank content after the leading frontmatter; extra
  frontmatter properties do not refuse and are discarded (seed assumption).
- Refusals use the existing `ResponseStatusException(BAD_REQUEST, …)` style
  and happen before any mutation; the surrounding transaction covers failures
  after mutation starts.
- The E2E Background relationship keeps its body for the edit scenario; reduce
  scenarios use a relationship without body.

## Ordered slices

### 1. Reduce through a dedicated operation that permanently deletes

Type: Behavior
Status: done. Added `POST /api/relations/{relationNote}/reduce-to-source-property`
(`RelationController`) delegating to new `RelationReduceService`, which runs
`AcceptedWebChangeService.apply`, calls `NoteService.reduceRelationNoteToSourceProperty`
(new), then `NoteService.permanentlyRemove(..., LEAVE_DEAD_LINKS, viewer)`, and
returns the source `NoteRealm`. `NoteReferenceHandling.reduceRelationNoteToSourceProperty`
now returns the source `Note` and derives the property key from the relationship
note's own `relation` frontmatter scalar (hyphens → spaces, trimmed) when no
explicit key is supplied; the trash path's explicit-key behavior is unchanged.
Extracted `WebNoteEditService.resolveNoteWithinLockedStateOrRepository` (package-private)
so the new service reuses the same Git-locked-state note lookup as `edit()`.
Ran `pnpm generateTypeScript`; generated client includes `reduceToSourceProperty`.
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*RelationControllerReduceToSourceProperty*'`
green (`RelationControllerReduceToSourcePropertyTests`, examples 1 and 3);
regression-checked `*RelationController*`, `*NoteControllerTrash*`,
`*NotebookGitWebTrashLinkedReferrerControllerTest*`, and full `pnpm backend:test_only`
green. Post-change refactor: no candidates, already clean.
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*RelationControllerReduceToSourceProperty*'` green.

Given a same-notebook Moon→Earth "a part of" relationship with empty body (and,
separately, Moon already holding `a part of`), when the owner calls the new
reduce endpoint, then Moon holds `a part of: '[[Earth]]'` (or `a part of 2`),
the relationship note no longer exists, and the source realm is returned. Add
the endpoint and a reduction service using `AcceptedWebChangeService.apply`
that runs the existing reduce step and then `permanentlyRemove`. Make
`NoteReferenceHandling` derive the key from `relation` when no key is supplied.
Interim: the trash path still supplies the client key and still trashes; slice
6 removes both. Keep the existing viewer-only tracker move for now. Run
`pnpm generateTypeScript` so generated code stays in sync.

### 2. Git-backed reduction records one commit without trash

Type: Behavior
Status: done, test-only. Replaced
`reduceToSourcePropertyTrashIncludesReducedSourceAndTrashedRelationshipInAcceptedTree`
(and its now-unused fixture/record) in `NotebookGitWebTrashLinkedReferrerControllerTest`
with a new standalone `NotebookGitWebRelationReduceControllerTest`, whose
`reduceToSourcePropertyRecordsReducedSourceWithNoRelationshipOrTrashInAcceptedTree`
exercises the slice 1 endpoint directly and asserts the reduced `Moon.md`, no
relationship file, and no `_trash/` entry anywhere in the downloaded tree.
Passed against slice 1's code unchanged, as predicted; no production code
touched. Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*NotebookGitWebRelationReduceControllerTest*' --tests '*NotebookGitWebTrashLinkedReferrerControllerTest*'`
green. Post-change refactor: no candidates, already clean.

### 3. Every learner's understanding learning moves to the property

Type: Behavior
Status: done. Rewrote `rehomeNoteLevelMemoryTrackerToSourceProperty` in
`NoteReferenceHandling` to move every tracker matching `isUnderstanding() &&
isNoteLevelTracker()` regardless of `removedFromTracking` or owning user
(dropped the old viewer-only/`findFirst()` filter); non-matching trackers
(spelling/commissioned/property-level) are explicitly detached so Hibernate's
persistence context doesn't hold a managed reference into the relationship
note's later cascade delete (added `EntityPersister.detach`, mirroring its
existing thin wrappers) — this fixes a previously-latent `TransientPropertyValueException`
gap, untested until this slice combined `permanentlyRemove` with a
`MemoryTracker` fixture. Fixed a stale javadoc on `NoteService.reduceRelationNoteToSourceProperty`
during refactor. New test `movesEveryLearnersUnderstandingTrackerAndDropsTheSpellingTracker`
in `RelationControllerReduceToSourcePropertyTests` covers two learners (one
removed from recall) plus a spelling tracker.
Proof: focused Gradle run of `*RelationControllerReduceToSourceProperty*` green
with the new learning case; existing `NoteControllerTrashTests` still green.

Given two learners with note-level understanding trackers on the relationship
(one removed from recall, both with recall logs) and a spelling tracker, when
reduced, then each understanding tracker is on Moon with key `a part of`,
unchanged schedule, logs, and removed state, and the spelling tracker is gone.
Replace the viewer/active-only filter in
`rehomeNoteLevelMemoryTrackerToSourceProperty` with the decided rule.

### 4. The web client reduces permanently and lands on the source

Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts tests/store/storedApi.trashNote.spec.ts tests/pages/NoteShowPage.autosaveDelete.spec.ts tests/utils/relationNoteReduceOnDelete.spec.ts`
green, then `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature` green.

Given a relationship note shown in the web app, when the owner chooses
"Reduce to a property of the source" (label also stating it permanently
deletes the relationship note and cannot be undone), then the client calls the
reduce endpoint, records no undo, removes the note realm, refreshes the
sidebar, and shows Moon. Move the reduce scenarios onto a body-less
relationship (keep the page-object label match working). The trash choice
keeps its current call. The E2E run includes stack boot time; that external
wait is an accepted focused-test exception to the slice time target.

### 5. Refuse reduction that would lose body text or cannot resolve its source

Type: Behavior
Status: planned
Proof: focused Gradle run of `*RelationControllerReduceToSourceProperty*` green
with both refusal cases.

Given a relationship with body "Observations from orbit.", or whose source link
no longer resolves, when reduced, then a bad-request error is raised and the
relationship note and Moon content are unchanged. Add the body check to the
interpretation step before any mutation; the unresolved-source case pins
existing behavior. This comes after slice 4 because the shared step still
serves the trash path, whose E2E scenarios used a relationship with body text
until slice 4 moved them.

### 6. Remove the reduce-via-trash contract

Type: Behavior (API contract)
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, then focused
Gradle run of `*NoteControllerTrash*` and `*NotebookGitWebTrash*`, and
`CURSOR_DEV=true nix develop -c pnpm frontend:test tests/store/storedApi.trashNote.spec.ts tests/pages/NoteShowPage.autosaveDelete.spec.ts` green;
`git grep -n "REDUCE_TO_SOURCE_PROPERTY\|sourcePropertyKey"` returns nothing
outside historical planning text.

Given the trash endpoint, when the generated client is rebuilt, then its
request offers only `LEAVE_DEAD_LINKS` and `REMOVE_FROM_PROPERTIES` with no
`sourcePropertyKey`. Delete the enum value, DTO field, trash-service and
`NoteService` reduce branches, `reduceToSourceProperty` test helper, client
`sourceNoteId`/`sourcePropertyKey` options and navigation branch, and tests
that only covered reduce-via-trash. Trash behavior stays unchanged.

## Learnings

- Fetching a note's `MemoryTracker` rows via `memoryTrackerRepository.findByNote_IdIn`
  makes every returned row a managed JPA entity for the rest of the transaction,
  even ones the caller doesn't otherwise touch. If that note is later permanently
  removed in the same transaction (`NoteService.permanentlyRemove` →
  `EntityPersister.remove`), any still-managed tracker referencing it trips
  Hibernate's pre-flush transient-dependency check (`TransientPropertyValueException`),
  because Hibernate doesn't know about the DB-level `ON DELETE CASCADE`. This
  was latent and untested before slice 3 (no prior test combined `permanentlyRemove`
  with a `MemoryTracker` fixture on the same note); fixed locally in slice 3 by
  detaching every non-moved tracker before the removal flush. Other
  `permanentlyRemove` call sites that load related entities without detaching
  untouched ones were not audited and may share this latent gap — out of this
  plan's scope, flagged for awareness only.
