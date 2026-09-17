---
id: SEED-024
status: dormant
planted: 2026-09-17
planted_during: Reported relationship-note reduction semantics
trigger_when: immediately from the product backlog
scope: M
---

# SEED-024: Reduce a relationship note into its source atomically

## Why This Matters

When a note owner reduces a relationship note to a property of its source,
Donut currently treats the relationship note as trashed and reconstructs the
relationship on the source note. That models one irreversible transformation as
a recoverable removal: the original relationship note remains in trash and the
action can appear undoable even though reduction cannot be recovered correctly.

The owner needs reduction to be one atomic action whose successful result is a
source property and no original relationship-note identity. The backend must own
the complete transformation so clients cannot observe or create a partially
reduced state.

## Alternatives and Decision

Hiding undo while continuing to trash the relationship note is the smallest UI
change, but it leaves the original entity recoverable and preserves the wrong
domain semantics. Instead, the backend atomically reduces the relationship into
the source property and permanently deletes the original relationship note and
its dependent data. The action is not added to recoverable note-editing history.

Before changing the API, inspect every production caller of the current
trash/reduce contract. Remove the contract completely if no live behavior still
uses it. If other trash behavior shares part of it, retain only that live part
and remove the obsolete reduce-via-trash operation, request fields, enum branch,
generated-client surface, and callers.

## Story Decomposition

<a id="story-1"></a>

### 1. Reduce a relationship note into its source in one irreversible action

- **Goal / beneficiary:** A note owner who reduces a relationship note ends
  with exactly one representation of that relationship, the source property,
  wherever they look: active notes, trash, undo, and, for a Git-backed
  notebook, the Portable tree they open in Obsidian or an AI IDE.
- **Current defect (verified in code, 2026-09-17):** reduction runs inside the
  trash operation. The relationship note moves beneath `_trash/` and the client
  records an ordinary trash undo entry. Undo restores the relationship note but
  leaves the added source property and the rehomed memory tracker in place, so
  the same relationship then exists twice. In a Git-backed notebook the trashed
  relationship file keeps `source`/`target` links under `_trash/`. External
  editors such as Obsidian index that folder by default.
- **Why now:** The near-future direction is external editing of Git-backed
  notebooks. Today every reduction leaves a relationship file in the repository
  that contradicts the source property. This story comes before SEED-020, which
  renames the trash request contract. This story removes the reduce fields from
  that contract, so SEED-020 renames less and does not rename a surface that is
  about to be deleted. Honest limit: this does not block synchronization, and
  how often owners reduce relationships is unknown. It is ranked first because
  of owner direction and data correctness, not because other work depends on it.

#### Scope — promised

- Reduction is its own backend operation, separate from trash. The backend
  reads the relationship note and derives the source note, target, and property
  key from its content. It does not trust a client-computed key or source ID.
- The backend refuses reduction when the relationship note has body text
  (any non-blank content after the leading frontmatter), with a message and no
  change. Donut-created relationship notes have an empty body unless details
  were preserved.
- In one transaction the backend adds the property to the source note
  (suffixing the key on collision, as today), moves learning to that property,
  and permanently deletes the relationship note through the existing
  permanent-deletion owner, the same one Git publication uses. Inbound wiki
  links to the relationship note are left as dead links, matching Git-published
  deletion. Attachments are lost with the note.
- Learning is kept: every learner's note-level understanding memory tracker on
  the relationship note, active or removed from recall, becomes that learner's
  property memory tracker for the resolved key on the source note, keeping its
  schedule, recall history, and state. Today only the reducing viewer's active
  tracker moves. Spelling and commissioned trackers have no property form. They
  test or tutor the relationship note itself, so they are permanently deleted
  with it (assumption, 2026-09-17).
- For a Git-backed notebook containing both notes, the change produces one accepted commit containing
  the source-note edit and the removed relationship file, with nothing under
  `_trash/` (NORTH-STAR "One complete accepted web change").
- Any failure (unresolvable source, missing relation, authorization, persistence)
  leaves both notes, trackers, and Git history unchanged.
- The client submits the reduction intent, records no undo entry, navigates to
  the source note, and refreshes the sidebar. The choice label says the action
  permanently deletes the relationship note and cannot be undone.
- Remove `REDUCE_TO_SOURCE_PROPERTY`, `sourcePropertyKey`, the client-side
  `sourceNoteId` option, and the reduce branch from the trash request, trash
  service, and generated client. Choosing Trash for a relationship note, and the
  existing `LEAVE_DEAD_LINKS` / `REMOVE_FROM_PROPERTIES` handling, stay as they
  are. Git publication's `permanentlyRemove(LEAVE_DEAD_LINKS)` stays.

#### Scope — deferred (not built or verified here)

- Any undo or recovery for reduction.
- The same irreversibility smell in the trash choice "Remove from properties of
  references", whose undo does not restore removed properties.
- Cleaning up relationship notes already trashed by earlier reductions. They
  stay as ordinary trash; no migration.
- Inferring a reduction from Git-published changes, bulk reduction, and
  reducing relationship notes from anywhere other than the existing
  note-options trash flow.
- SEED-020 vocabulary renames beyond the removed reduce surface.
- A source note in another notebook:
  [SEED-025](SEED-025-cross-notebook-relationship-reduction.md#story-1). This
  story neither enables nor constrains that case; whatever the natural behavior
  is stays unchanged.
- Hiding the reduce choice for relationship notes with body text; the backend
  refusal message is the promised signal.
- Authored frontmatter properties beyond the structural relationship keys are
  discarded with the note, like attachments (assumption, 2026-09-17).

#### Key examples

1. **Same-notebook reduction.** Given "Moon a part of Earth" in notebook "Space
   topics", when the owner chooses "Reduce to a property of the source", then
   "Moon" contains `a part of: '[[Earth]]'`, the relationship note is absent from
   active notes and from trash, no undo is offered, and the owner lands on
   "Moon". An attachment on the relationship note is gone.
2. **Git-backed notebook.** Given the same notebook is Git-backed, reduction
   appends one accepted commit that modifies `Moon.md` and deletes the
   relationship file. No file appears under `_trash/`.
3. **Key collision (existing).** When "Moon" already has `a part of: "[[Mars]]"`,
   the new property is `a part of 2: '[[Earth]]'`.
4. **Learning moves.** Two learners each assimilated "Moon a part of Earth" and
   recalled it; one later removed it from recall. After reduction, each learner
   has a property memory tracker for "a part of" on "Moon" with the same next
   recall time, recall history, and removed-from-recall state as before.
5. **Body text refuses reduction.** Given the relationship note's body is
   "Observations from orbit.", reduction is refused with a message; the
   relationship note, "Moon", and Git history are unchanged.
6. **Failure is atomic.** If the relationship's `source` link no longer resolves
   to a note the owner can edit, reduction is refused with a message, and both
   notes, the tracker, and Git history are unchanged.
7. **Trash remains recoverable.** Choosing "Trash" for the same relationship
   note still moves it beneath `_trash/` with undo available.

- **Effort hypothesis:** M, moderate confidence. Backend-owned interpretation
  and permanent deletion reuse existing owners. Risk is the new endpoint,
  generated client propagation, and removing the frontend qualification's
  source-ID role.
- **Depends on:** none.
- **Safe stopping point:** Reduction is atomic, backend-owned, and
  irreversible, proven at the product boundary. The relationship note is absent
  from active notes, trash, and the Portable tree, and no reduce-via-trash
  surface remains.

## Ordering and Scope Reduction

This single end-to-end story is first in the product backlog by owner
direction and precedes SEED-020 so the vocabulary correction does not rename a
contract this story removes. Atomic source mutation and permanent deletion
cannot be delivered separately without recreating the invalid intermediate
state. The strongest cheaper alternative, removing the reduction feature
entirely and letting owners edit the source property themselves, was
considered. It is not proposed because the owner kept reduction as a product
capability, but it remains a fallback.

## Open Decisions

None. Owner decisions, 2026-09-17: keep this story first; learning moves to
the source property; body text refuses reduction; losing attachments is
acceptable; cross-notebook sources move to SEED-025.

## When to Surface

Immediately, from the product backlog.

## Breadcrumbs

- Reporter observation: relationship-note reduction currently trashes the
  original note before reconstructing the relationship on its source.
- Owner decision: reduction is one backend-owned atomic action, is not
  recoverable, and permanently deletes the original relationship note.
- Owner decision: audit the supporting API and completely remove every portion
  that has no remaining production use.
- Caller audit (refinement, 2026-09-17): `REDUCE_TO_SOURCE_PROPERTY` and
  `sourcePropertyKey` are used only by the note trash flow
  (`useNoteTrashFlow`, `StoredApiCollection.trashNote`, `NoteTrashService`);
  `LEAVE_DEAD_LINKS` and `REMOVE_FROM_PROPERTIES` remain live for trash and Git
  publication.
- [SEED-020](SEED-020-trash-vocabulary.md) renames the same trash request
  contract.
- [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
  (trash is recoverable; permanent deletion ends identity and dependent data)
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  (relationship-note representation and portable trash semantics)

