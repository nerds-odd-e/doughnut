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

- **Goal:** Give maintainers one authoritative deletion state on the note,
  while learners retain exactly the same external behavior and learning history.
  The owner explicitly selected this as an internal structure change; it adds
  no new user-facing behavior.
- **Scope:** Remove the memory tracker field and its persistence responsibility;
  adapt queries, activity checks, deletion/restoration, and affected uniqueness
  constraints to use note deletion state. Preserve tracker identities, history,
  and independent `removed_from_tracking` preferences. All existing features
  must behave exactly as before, including existing UI and API behavior.
- **Migration:** Apply the schema and any necessary data transformation directly
  through a new Flyway SQL migration. No placeholder gate, feature flag, staged
  opt-in, or separate migration approval is required. This is the owner's
  explicit 2026-09-13 direction and takes precedence over the default gated-DML
  guidance in `.cursor/rules/db-migration.mdc` for this change. It does not
  authorize discarding tracker identities or learning history.
- **Deferred promises:** Portable trash and changes to note deletion semantics,
  scheduling policy, or learning features are outside this delivery.
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

#### Key examples

| Pre-condition | Trigger | Required result |
| --- | --- | --- |
| An active note has existing trackers and recall history. | Apply the SQL migration and use existing learning features. | Tracker identities, history, scheduling state, and existing recall, assimilation, count, and property-tracking results are preserved; the tracker deletion column is gone. |
| A note has trackers available for learning. | Delete the note through an existing supported workflow. | Its trackers become unavailable wherever deletion currently excludes them; learning history is retained as before. |
| A deleted note has trackers, including one previously removed from repeating. | Restore the note. | Existing availability returns without re-enabling the tracker removed from repeating; identities and learning history remain intact. |
| A note is already deleted when the migration runs. | Apply the SQL migration, then use existing learning and restoration workflows. | The note remains deleted, its trackers remain excluded as before, and restoration preserves existing behavior. |

#### Architectural constraints

Keep the memory tracker and recall terminology from
[ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md).
Preserve scheduling state and durable RecallLog history under
[ADR 0003 — Spaced-repetition scheduling policy](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
Neither decision requires a migration gate or a new product behavior.

## Ordering and Scope Reduction

The owner requested this as the highest-priority backlog item, above Portable
trash. Keep it independently scoped; the backlog owns global ordering.

## Implementation Evidence to Resolve

Scope is settled. The following are implementation verification concerns, not
additional migration gates or requests for product approval:

- Assess existing tracker data for deletion-state mismatches and duplicate keys
  before changing uniqueness; do not silently discard records to fit the schema.
- The existing restoration test constructs trackers with different deletion
  dates although no current independent soft-delete writer was found. Reconcile
  that test and any real legacy states with the unchanged-feature requirement.
  Do not interpret this internal timestamp fixture as a new independent tracker
  deletion feature, or silently change observable behavior if such a legacy
  state is found.
- Compare representative recall, aggregate, and recent-history queries before
  and after the change. Comparable performance is a hypothesis, not measured
  evidence; resolve material regression before delivery.

## When to Surface

At the next backlog selection, as explicitly prioritized by the owner.

## Breadcrumbs

- Owner's 2026-09-13 request to isolate this change from the portable trash
  discussion, preserve existing behavior, and give it highest priority.
- Owner's 2026-09-13 refinement: internal structure change with exactly unchanged
  external behavior and a direct SQL migration requiring no gate.
- Related independent direction: [Portable trash](SEED-009-git-backed-local-notebook-workflow.md#story-26).

This seed is non-executable planning input; implementation has not started.
