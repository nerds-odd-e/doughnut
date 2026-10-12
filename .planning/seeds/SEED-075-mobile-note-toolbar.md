---
id: SEED-075
status: dormant
planted: 2026-10-12
planted_during: owner-directed capture of three note authoring and spelling stories
trigger_when: selecting the reported mobile note toolbar layout repair
scope: small
---

# SEED-075: Keep note actions usable on narrow mobile screens

## Why This Matters

A note author on a phone needs to reach every toolbar action. On 2026-10-12
the owner reported that the More options button wraps onto a second line and
its menu extends beyond the left edge of the screen, making the actions
unreadable or unreachable. The owner suspects a constrained toolbar container
and wants the toolbar to use the full available width without wrapping.

## Alternatives and Decision

Keep the existing overflow-to-menu interaction and correct its layout. Use the
full available page width on mobile, with the controls on one row and the menu
inside the viewport. The suspected container cause needs reproduction; it is
not an established diagnosis.

## Story Decomposition

<a id="story-1"></a>

### Keep the mobile note toolbar on one full-width row with a visible options menu

**Identity:** SEED-075#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A mobile note author can use the toolbar and read and select
  its overflow actions on a narrow screen.
- **Scope:** Confirm the reported narrow-screen layout and repair a confirmed
  discrepancy. Give the mobile toolbar the full available page width, removing
  the constraining container layout. Keep its controls, including More options,
  on one row; move actions into the existing overflow menu as needed. Keep the
  opened menu within the visible viewport with its actions readable and
  operable. Preserve the existing wider-screen toolbar and action behavior.
- **Evaluation:** At a narrow phone width where actions overflow, the toolbar
  occupies the available width and its controls share one row. Open More
  options, read the menu, and select an overflow action. Resize across the
  overflow threshold and verify that actions remain accessible and the menu
  stays within the screen. Record the actual reproduction viewport.
- **Evidence and gaps:** Owner report, 2026-10-12; no device model, exact
  viewport, screenshot, or runtime reproduction supplied. Main at
  `5d6307f63d` already has width-based overflow selection in
  `NoteToolbarMoreOptions.vue` and `noteToolbarOverflow.ts`, read on 2026-10-12.
  This establishes an existing solution to retain, not the layout defect's
  cause.
- **Value / learning:** Restore access to existing mobile note actions and
  establish which layout constraint causes the reported failure.
- **Effort hypothesis:** S (30–60 minutes including delivery), low confidence
  until the narrow-screen failure is reproduced. The owner considers this a
  possible quick fix; execution sizing remains to be confirmed.
- **Depends on:** No blocking story dependency established.
- **Safe stopping point:** The toolbar and its overflow actions are usable
  independently of the voice-control and spelling stories.

## Ordering and Scope Reduction

The owner selected this as the second of the three new top backlog stories,
after the shared voice-input control and before spelling-cloze repair.
This is non-executable story input.

## Open Decisions

Reproduce the failing phone width and confirm the cause during refinement.
No additional toolbar capability is requested.

## When to Surface

When selecting the queued mobile toolbar repair.

## Breadcrumbs

- Owner's mobile toolbar report and explicit top-of-backlog instruction,
  2026-10-12; authorized capture directly on main and sync origin.
- `frontend/src/components/notes/core/NoteToolbar.vue`
- `frontend/src/components/notes/widgets/NoteToolbarMoreOptions.vue`
- `frontend/src/composables/noteToolbarOverflow.ts`
