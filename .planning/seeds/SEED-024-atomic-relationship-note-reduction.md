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

- **For / why:** A note owner can reduce a relationship note without leaving a
  recoverable copy whose identity and undo behavior contradict the completed
  transformation.
- **Evaluation:** Choosing “Reduce to a property of the source” adds the
  relationship as a property of the resolved source note and permanently
  deletes the original relationship note. The note does not appear in trash and
  the reduction is not offered through undo.
- **Scope:** The backend owns validation, relationship interpretation, source
  and target resolution, source-note mutation, dependent-data handling, and
  permanent deletion in one transaction. A failure leaves both the source note
  and relationship note unchanged. The frontend submits the reduction intent
  and follows the returned result; it does not reconstruct or coordinate the
  transformation. Audit all production callers of the superseded API surface
  and remove every wholly or partially obsolete reduce-via-trash contract as
  described above.
- **Key example:** Given a relationship note from “Moon” to “Earth” with
  relation “a part of”, reducing it adds `a part of: [[Earth]]` to “Moon”,
  permanently removes the relationship note and its remaining dependent data,
  creates no trash entry, and creates no undo action. If the source update or
  deletion fails, neither change is committed.
- **Value / learning:** The stored model, visible trash contents, and undo
  behavior all agree that reduction replaces the relationship-note identity
  rather than temporarily relocating it. The caller audit establishes whether
  the current API should be narrowed or removed instead of leaving an unused
  compatibility surface.
- **Effort hypothesis:** M — moderate confidence; the product behavior is
  bounded, but atomic hard deletion, dependent-data coverage, generated API
  propagation, and caller removal cross backend and frontend boundaries.
- **Depends on:** none.
- **Safe stopping point:** Reduction is atomic, backend-owned, irreversible,
  and proven at the product boundary; the original relationship note is absent
  from both active notes and trash, and no obsolete production API surface or
  caller remains.

## Ordering and Scope Reduction

This single end-to-end story is second in the product backlog by owner
direction. Atomic source mutation and permanent deletion cannot be split into
separately delivered behavior without recreating the invalid intermediate
state. API cleanup stays in the same story because retaining an unused
reduce-via-trash contract would preserve the superseded behavior as a supported
path.

## Open Decisions

None.

## When to Surface

Immediately, from the product backlog.

## Breadcrumbs

- Reporter observation: relationship-note reduction currently trashes the
  original note before reconstructing the relationship on its source.
- Owner decision: reduction is one backend-owned atomic action, is not
  recoverable, and permanently deletes the original relationship note.
- Owner decision: audit the supporting API and completely remove every portion
  that has no remaining production use.
- [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
  (trash is recoverable; permanent deletion ends identity and dependent data)
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  (relationship-note representation and portable trash semantics)
