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

Keep the existing overflow-to-menu interaction and correct its layout: the
controls stay on one row and the menu stays inside the screen.

The owner's suspected cause, a constrained toolbar container, was checked on
2026-10-12 and is not the cause; the toolbar already spans the note column.
The owner then decided, on 2026-10-12, that the toolbar keeps the page's side
margins and stays as wide as the note text below it. Stretching it to both
screen edges was considered and not chosen: it gains 32px, needs a change to
the layout shared by all notebook pages, and does not address the cause.

## Story Decomposition

<a id="story-1"></a>

### Keep the mobile note toolbar on one full-width row with a visible options menu

**Identity:** SEED-075#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-mobile-note-toolbar-one-row/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"c60c7f325ef89ab78d7ea84f4e07bbc5880856fcc7e049562484a0d5dd5388ac","plan":"2aedf2e8a64c098c50d4a57df51cc7087baacac9cb9bf5c936fb6a3132c751d8"}}
```

- **Goal:** A note author on a phone, or in any narrow note column, sees
  every toolbar control on one row and can read and select each action that
  moved into More options. This restores access to existing note actions; it
  adds no toolbar capability.
- **Scope:**
  - Required: the toolbar shows only as many actions as fit on one row
    together with More options, counting the space the buttons can really use;
    the rest are in the More options menu. This holds at every toolbar width,
    not only at phone sizes.
  - Required: the opened More options menu lies fully inside the screen's
    left and right edges, wherever the More options button sits, with every
    listed action readable and selectable.
  - Unchanged: which actions exist, the order in which they move into the
    menu, the actions that stay on the bar while active (voice input in use,
    assimilation open), and the toolbar when every action fits.
  - Unchanged, by owner decision: the toolbar keeps the page's side margins
    and stays as wide as the note text.
  - Boundary assumption: keeping a menu inside the screen is a property of
    the shared menu placement, so other menus that use it gain the same
    behavior. This story commits to verifying it only for More options.
  - Deferred: a menu taller than the screen. At the widths where actions
    overflow on a phone held upright the menu is far shorter than the screen
    (252px at 320px wide); this delivery does not commit to a scrolling menu.
- **Key examples:**
  - 390px-wide screen, sidebar closed, editable note → the note page opens →
    the toolbar is one row ending in More options; no control sits on a
    second row. Today New note, Wiki link, Conversation, Edit, Export, and
    Questions fill the row and More options drops to a second row at the
    left.
  - Same screen → the author opens More options → the menu's left and right
    edges are inside the screen and its actions can be read; selecting Trash
    note starts the same trash action as the toolbar button. Today the menu's
    left edge is 206px left of the screen.
  - 320px-wide screen, where the row already holds on one line → the author
    opens More options → the whole menu is inside the screen. Today its left
    edge is 5px outside.
  - The screen widens from 390px to 414px → Questions for the note joins the
    bar and leaves the menu; narrowing again moves it back. The row stays
    single at every width in between.
  - 640px-wide screen → all ten actions are on the bar and there is no More
    options button, as today.
- **Evaluation:** At 390px and at 320px, the toolbar controls share one row
  and the opened More options menu is inside the screen with a selectable
  action. Resize between 320px and 640px and confirm the row stays single and
  no action becomes unreachable. Observe with the real page styles and real
  measured sizes; the existing toolbar tests replace the measured sizes with
  fixed numbers and so cannot show wrapping or menu position.
- **Evidence and gaps:**
  - Owner report, 2026-10-12; no device model, viewport, or screenshot
    supplied.
  - Reproduced on 2026-10-12 at `c2e8ffaff2` with a temporary browser test
    (Chromium, the frontend test runner, real styles, real `ResizeObserver`):
    the toolbar mounted inside the notebook page's main column
    (`px-4 container mx-auto`), sidebar closed, at viewport widths 320, 360,
    375, 390, 414, 480, and 640px. The temporary test was removed afterwards.
  - Result at 390px: the toolbar spans 16px to 374px; its inner width is
    358px including 8px padding on each side, so the buttons have 342px. Seven
    controls of 50px each need about 351px. More options is on a second row
    at left 24px to 74px and the toolbar is 80px tall instead of 64px. The
    opened menu is 280px wide at left −206px.
  - Result at 320, 360, 375, 414, 480, and 640px: one row. At 320px the
    opened menu is at left −5px.
  - Cause, read in `NoteToolbarMoreOptions.vue`: the available width passed
    to the fit calculation is the toolbar's inner width including its side
    padding, so the buttons are allowed 16px more than they have. The row
    wraps whenever the shown controls need more than the real space but not
    more than that inflated figure: a band of up to 16px in every 50px of
    width, for example about 383px to 398px. Phones of 390px and 393px width
    fall inside it.
  - The button row has no layout rule of its own (`daisy-btn-group` carries
    no styles in the installed daisyUI 5), so buttons that do not fit wrap as
    ordinary inline content.
  - The menu is placed with its right edge at the More options button's
    right edge and nothing keeps it on screen
    (`dropdownPortalPlacement.ts`). A wrapped button at the left therefore
    pushes the 280px menu off the left edge, and at 320px even a button on
    the first row does.
  - Not the cause: the page column. Below 640px it has no maximum width,
    only 16px side padding.
  - Limits: observed in the component test browser, not on a phone and not
    through the full application; the sidebar-open case, where New note is
    absent from the bar, was not observed, and the same calculation applies.
    The E2E suite runs at 1200px wide and has no narrow-screen scenario.
- **Architecture:** No Accepted ADR in `docs/adrs/` governs frontend layout;
  nothing consequential is added or moved. The fit calculation and the shared
  menu placement each remain the single owner of their rule.
- **Value / learning:** Restores access to existing mobile note actions. The
  investigation established the cause: a miscounted width, not a container.
- **Effort hypothesis:** S (30–60 minutes including delivery), medium
  confidence now that the cause is reproduced and located. The main
  uncertainty is proving one-row layout and menu position with real measured
  sizes.
- **Depends on:** No blocking story dependency established.
- **Safe stopping point:** The toolbar and its overflow actions are usable
  independently of the voice-control and spelling stories.

## Ordering and Scope Reduction

The owner selected this as the second of the three new top backlog stories,
after the shared voice-input control and before spelling-cloze repair.
This is non-executable story input.

## Open Decisions

None. No additional toolbar capability is requested.

## When to Surface

When selecting the queued mobile toolbar repair.

## Breadcrumbs

- Owner's mobile toolbar report and explicit top-of-backlog instruction,
  2026-10-12; authorized capture directly on main and sync origin.
- `frontend/src/components/notes/core/NoteToolbar.vue`
- `frontend/src/components/notes/widgets/NoteToolbarMoreOptions.vue`
- `frontend/src/composables/noteToolbarOverflow.ts`
