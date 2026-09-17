---
id: SEED-023
status: dormant
planted: 2026-09-17
planted_during: Reported trash-sidebar expansion bug
trigger_when: immediately from the product backlog
scope: S
---

# SEED-023: Keep the trash folder collapsed when trashing a note

## Why This Matters

When a note owner trashes a note, the sidebar unexpectedly expands the trash
folder. Trashing is not a journey into the trash folder, so it should not
navigate to or reveal that folder's contents.

## Alternatives and Decision

Preserve the user's current sidebar context while trashing a note. Do not
automatically expand or visit the trash folder as a side effect of the action.

## Story Decomposition

<a id="story-1"></a>

### 1. Keep the trash folder collapsed when trashing a note

- **For / why:** A note owner can trash a note without being taken into the
  trash folder's contents or having that folder unexpectedly expanded.
- **Evaluation:** Starting with the trash folder collapsed, trashing a note
  leaves it collapsed and does not visit its contents.
- **Value / learning:** The trash action preserves the user's current sidebar
  context instead of implying that they navigated to the trashed note.
- **Effort hypothesis:** S — the report describes one bounded visible behavior;
  implementation scope remains uninvestigated.
- **Depends on:** none.
- **Safe stopping point:** The trash action succeeds while the trash folder
  remains collapsed and unvisited.

## Ordering and Scope Reduction

This single reported behavior is the complete story and is first in the product
backlog by owner direction.

## Open Decisions

None recorded; implementation details remain uninvestigated.

## When to Surface

Immediately, from the product backlog.

## Breadcrumbs

- Reporter observation: trashing a note expands the trash folder in the sidebar.
- Expected behavior: trashing does not visit or reveal the trash folder's contents.
