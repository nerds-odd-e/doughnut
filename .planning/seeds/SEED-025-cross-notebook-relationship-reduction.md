---
id: SEED-025
status: dormant
planted: 2026-09-17
planted_during: SEED-024 story refinement
trigger_when: immediately from the product backlog
scope: M
---

# SEED-025: Reduce a relationship whose source lives in another notebook

## Why This Matters

A relationship note's `source` is resolved through a wiki link and may belong
to a different notebook than the relationship note. Reduction changes that
source note, but the web accepted-change boundary locks and commits only the
relationship note's notebook. When the source notebook is Git-backed, its
Portable tree and accepted history can miss the source edit, and an owner
editing that notebook in Obsidian or an AI IDE does not see the reduced
property. [SEED-024](SEED-024-atomic-relationship-note-reduction.md#story-1)
leaves this case at its natural behavior.

## Alternatives and Decision

Refusing cross-notebook reduction is the smallest guard, but it removes a
capability instead of making it correct. The desired direction is one complete
reduction whose effects are recorded in every affected Git-backed notebook,
without controllers coordinating Git history themselves (NORTH-STAR "One
complete accepted web change"). How several notebooks share one consistency
owner is unresolved and needs refinement before planning.

## Story Decomposition

<a id="story-1"></a>

### 1. Reduce a relationship into a source note in another notebook

- **For / why:** A note owner whose relationship note and source note live in
  different notebooks can reduce the relationship and see the same result in
  Donut and in each Git-backed notebook's Portable tree.
- **Evaluation:** Given a relationship note in notebook A whose source note is
  in Git-backed notebook B, reduction adds the property to the source note,
  permanently deletes the relationship note, and records the source edit in
  notebook B's accepted history and the deletion in notebook A's (when
  Git-backed). A failure in either notebook leaves both unchanged.
- **Value / learning:** Establishes how one web action covers more than one
  notebook's accepted history.
- **Effort hypothesis:** M, low confidence; multi-notebook locking and commit
  ordering are not yet designed.
- **Depends on:** [SEED-024 story 1](SEED-024-atomic-relationship-note-reduction.md#story-1).
- **Safe stopping point:** Cross-notebook reduction is complete and atomic
  across both notebooks; same-notebook reduction is unchanged.

## Ordering and Scope Reduction

Follows SEED-024 story 1, which delivers the backend reduction operation this
story extends.

## Open Decisions

- Whether one web action may append accepted commits to two notebooks, and in
  what order, or whether cross-notebook reduction should instead be refused.

## When to Surface

Immediately after SEED-024 story 1, from the product backlog.

## Breadcrumbs

- Owner decision (2026-09-17): keep cross-notebook reduction out of SEED-024
  and queue it as its own story.
- `WebNoteEditService.edit` locks and commits only the edited note's notebook.
