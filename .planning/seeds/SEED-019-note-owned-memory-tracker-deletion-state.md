---
id: SEED-019
status: dormant
planted: 2026-09-13
planted_during: portable trash discussion; isolated as independent work
trigger_when: next product backlog item is selected
scope: small
---

# SEED-019: Use note deletion state for memory trackers

## Why This Matters

Learners should retain all existing learning behavior while maintainers remove
duplicated deletion state. Current application writes set memory tracker
`deleted_at` when deleting its note; removing a tracker from repeating instead
uses the independent `removed_from_tracking` flag.

## Alternatives and Decision

The owner selected removal of memory tracker `deleted_at`, relying solely on
the note's `deleted_at`. Keeping the duplicate state avoids query changes but
retains synchronization and timestamp-matching restoration logic. This work is
independent of portable trash and does not change the note deletion model.

## Story Decomposition

<a id="story-1"></a>

### 1. Remove memory tracker deleted_at and rely on note deletion state

- **For / why:** Learners retain existing features and learning history while
  maintainers have one authoritative deletion state on the note.
- **Scope:** Remove the memory tracker field and its persistence responsibility;
  adapt queries, activity checks, deletion/restoration, and affected uniqueness
  constraints to use note deletion state. Preserve tracker identities, history,
  and independent `removed_from_tracking` preferences. All existing features
  must behave exactly as before. No portable trash implementation is included.
- **Evaluation:** Deleting a note excludes its trackers from existing learning
  features; restoring it restores availability without re-enabling trackers
  removed from repeating. Active-note recall, assimilation, counts, history,
  and property tracking retain their existing results and constraints. The
  memory tracker deletion column is absent after migration, with retained data
  and identities intact.
- **Value / learning:** Removes redundant state and establishes whether direct
  note filtering provides comparable query performance.
- **Effort hypothesis:** M, low confidence until existing data and affected
  queries are assessed.
- **Depends on:** No new story; independently deliverable before portable trash.
- **Safe stopping point:** Existing note deletion and restoration remain usable
  if portable trash is never implemented.

## Ordering and Scope Reduction

The owner requested this as the highest-priority backlog item, above Portable
trash. Keep it independently scoped; the backlog owns global ordering.

## Refinement Checks

- Assess existing tracker data for deletion-state mismatches and duplicate keys
  before changing uniqueness; do not silently discard records to fit the schema.
- The existing restoration test constructs trackers with different deletion
  dates although no current independent soft-delete writer was found. Reconcile
  that test and any real legacy states with the unchanged-feature requirement.
- Compare representative recall, aggregate, and recent-history queries before
  and after the change. Comparable performance is a hypothesis, not measured
  evidence; resolve material regression before delivery.

## When to Surface

At the next backlog selection, as explicitly prioritized by the owner.

## Breadcrumbs

- Owner's 2026-09-13 request to isolate this change from the portable trash
  discussion, preserve existing behavior, and give it highest priority.
- Related independent direction: [Portable trash](SEED-009-git-backed-local-notebook-workflow.md#story-26).

This seed is non-executable planning input; implementation has not started.
