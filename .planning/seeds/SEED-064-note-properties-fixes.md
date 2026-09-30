---
id: SEED-064
status: dormant
planted: 2026-09-30
planted_during: owner selection of note properties fixes and improvements found by the one-hour manual UAT (SEED-061#story-1)
trigger_when: making note properties comfortable on an iPad, operable on a phone, and simpler to maintain
scope: medium
---

# SEED-064: Make note properties comfortable on an iPad, operable on a phone, and smaller in code

## Why This Matters

A manual UAT of the note properties area (2026-09-30, iPad first, then phone and desktop, commit
`1986473b79`) found that every journey can be completed, but a long key hides its value in the read-only view,
the key preset list gets in the way, a rejected value gives no message near the row, and touch controls are
small. The code review found that the read-only list, the editable row and the add form each decide separately how
to show a key, so the same fix has to be made in three places.

The owner kept every defect and kept an improvement only where its value justified the effort, then grouped what
remained by severity and value. Every story below carries the user interface change that belongs to it; there is
no separate polish story. An iPad is the primary device; a phone only has to be operable.

The full report (measurements, reproduction steps, screenshot file names, line-count method) is recoverable at
`1986473b79:.planning/seeds/SEED-061-note-properties-ux-uat.md`, section `## UAT Findings`. "iPad D1" means iPad
defect 1, "iPad I4" means iPad improvement 4, "phone D2" means phone operability defect 2, and "C3" means design
candidate 3 in that report. Line effects are that report's estimates.

## Context

- **Owner decisions (2026-09-30):**
  - A read-only view must not show the row panel controls Assimilate and Skip. Today the read-only view has no
    chevron and no panel; that stays true.
  - Kept: all defects. Kept improvements: those folded into the stories below.
- **What already works and must keep working:** adding, changing, removing and saving a property on iPad, phone and
  desktop; the list dialog with reorder; image add and replace; relation type; Wikidata Save with a chosen search
  result; the duplicate-key message.
- **Findings left out by the owner** (low value for the effort, or not reproduced):
  - a collapsed "show all" mode for long property lists (iPad I2); the row compaction in story 1 is tried first;
  - an empty-state message for a note without properties, and hiding the `type: Note` row (iPad I5); its answer
    depends on whether the row is stored (ADR 0004);
  - a message when a value is appended to a list key such as `url` (iPad I7);
  - a confirmation or undo for remove (iPad I8); larger targets in story 1 reduce mistaken taps;
  - the upload error disappearing after 2.5 seconds (iPad I10);
  - a Replace tap that timed out once and a tap blocked once after an image upload: not reproduced;
  - the panel's Assimilate moving to another note and the Skip wording (phone finding, iPad I8 wording): these are
    assimilation behavior, not properties layout; the owner has not decided whether they are wanted;
  - merging the `noteContent*` utilities, one YAML parser for both languages, replacing the value field with a plain
    input, changing the value dialog, and a new confirmation dialog for remove (report, "Not recommended").
- **Direction shared with SEED-063 (multiple values of one property, no numbered keys):** SEED-063 will replace the
  numbered-key convention (`url 2`, `example of two`) with several values under one key, each with its own tracker;
  its storage and editing representation is still undecided. The code that produces numbered keys today is
  `propertyKeyBaseAndSuffix` and `nextAvailablePropertyKeyForBase` / `ForPreset` in
  `frontend/src/utils/noteContentPropertyKeys.ts`, plus the duplicate-key rule that shows "Duplicate property keys are
  not allowed." This seed therefore (a) does not add, keep or test numbered-key behavior, (b) does not decide what
  adding an existing key means, and (c) does not assume that a key appears once. SEED-062 (reify a property) acts on
  one property row and needs no change here. Expected deletions by SEED-063 and by stories 5 and 6 may overlap, so the
  line estimates here are not additive with SEED-063's.
- **Real device:** device mode cannot show the real iPad keyboard, visual viewport or touch. Each story lists a check
  on a real iPad in its evaluation.

## Alternatives and Decision

Fixing each defect where it appears would repeat the fix in the read-only list, the editable row and the add form.
The recommended direction is to fix the visible defects first (stories 1 to 4), then merge the paths that make them
recur (stories 5 and 6), which also removes about 180 to 225 lines (estimate). The strongest simpler alternative,
fixing only the defects and leaving the structure, was rejected because the same layout rule would still live in
three places and each later change (for example SEED-062 and SEED-063) would touch all three.

Stories are ordered by user impact on an iPad first, then by code reduction, then by dependency. A story that
changes the shared layout comes before the stories that reuse it.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.

<a id="story-1"></a>

### A note's properties keep every value visible and are easy to tap on an iPad and a phone

**Identity:** SEED-064#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-note-properties-touch-layout/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"463baf37f47acba5999efb0b1375c5b15d9fd504d2dbdac5aea696a6ba2ac6e7","plan":"6307ab8e03312f8e9717d10e2ec35d1a0d53aa4a6df601fc167e09479b953730"}}
```

- **Goal:** an iPad user (primary device) and a phone user can read every value of a note's properties, in the
  read-only and the editable view, even when a key or value is long, and can tap a row control without hitting its
  neighbour. This removes the most severe defect of the note properties area (iPad D1, phone D1: the value is 0 px
  wide when the key is long) and the small-target risk beside an unconfirmed Remove (no undo, iPad I8 left out).
- **Scope:**
  - **Value stays visible.** At 820 px and 375 px, in both views, a note with a 76-character key and a 200-character
    value shows the key and the value, and the note has no sideways scroll. A read-only key and value wrap. In the
    editable row the value field wraps; the single-line key field shows an ellipsis when its text is cut, and its full
    text is reached by tapping into the field. A cut with no cue is not left anywhere in the row (iPad D4, phone D3).
  - **44 px touch targets.** On touch devices (`pointer: coarse`, so an iPad in both orientations as well as a phone,
    not a width rule) the row chevron, edit, remove and external-link controls and the row key and value fields are at
    least 44 px high; one rule for the properties section, not per-component edits (iPad I1).
  - **Density is a reported number, not a gate (owner decision).** The result reports how far the note body starts from
    the top of the screen for a 19-row note at 820 px, before and after, editable and read-only. A worse number does
    not fail the story and is not corrected inside it; it goes to the owner as a trade-off. A hard gate was rejected
    because meeting it may force a different structure and an ADR change.
  - **Read-only keeps its own component here.** The read-only list gets the same wrapping fix now, even though story 5
    deletes that component: it is cheap and keeps the defect fixed if story 5 is delayed. Its Cypress scenario carries
    over to the merged row.
  - **Real iPad check by the owner** (device mode cannot show real touch): open the long-key note read-only and
    editable in both orientations; tap chevron, edit, remove; report the density number.
- **Key examples:**
  - Read-only, 820 px: key `a_rather_long_...` (76 characters, no spaces) with value `short` → key wraps inside its
    column, `short` is visible, `scrollWidth` does not exceed the note width.
  - Read-only, 375 px: a 76-character key with spaces and a 200-character value → both wrap; the value is fully
    readable by scrolling down, not sideways.
  - Editable, 375 px: the same note → the value field shows the whole 200 characters wrapped; the key field shows an
    ellipsis; chevron, edit and remove do not overlap.
  - Editable on a touch device: every row control measures at least 44 px high; on a non-touch desktop the row is as
    tall as today.
  - 19-row note at 820 px: body start reported before (UAT baseline: 992 px editable, 1120 px read-only, from a commit
    whose properties components are unchanged) and after.
- **Excluded (considered, not done):**
  - key and value visual hierarchy (iPad I3): no failure behind it; it may follow from the layout rule at no cost, but
    is not a promise or a test;
  - dialog close buttons (iPad D5, 26 px): a different component and concern from the row;
  - add form and preset list sizing: story 2;
  - a "show all" collapse (iPad I2), a tap-to-expand cue for cut text, and a width-based touch rule.
- **Value / learning:** removes the most severe defect and answers, with a number, how much 44 px targets cost in
  density.
- **Effort hypothesis:** M, medium confidence. The layout part is small; one Cypress viewport scenario and finding a
  way to prove `pointer: coarse` in Cypress carry the effort.
- **Depends on:** none.
- **Safe stopping point:** the value-visible increment stands alone; the touch-target increment can be dropped or
  reverted without losing it.

<a id="story-2"></a>

### Typing a property key narrows the presets, and the list stays on screen

**Identity:** SEED-064#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-note-properties-key-presets/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"a75e92592c3093789e8f7154bf1d7210b70a43380c03645e1253741e89fe2af5","plan":"8940e8fae6150d2c46859f1c0ebbc539fffab743878df9e8f640f424c42f089d"}}
```

- **Goal:** an iPad user (primary device) or a phone user who types a property key sees only the presets that match
  what was typed, and can always tap the value field, or Choose image for the key `image`, with one tap while the
  list is showing. Nothing is lost today and typing a custom key already works; the story removes the one silent
  blocker (a tap on the value field or Choose image lands on a preset option, phone D2) and makes the list usable
  (iPad D3).
- **Scope:**
  - **Narrowing.** The list shows the presets whose displayed name contains the text typed since the key field took
    focus (case-insensitive) and disappears when none matches, because a custom key is then being typed. Focusing a
    key field without typing shows every available preset, as today (an existing row's key can be swapped for a
    preset). Choosing a preset, or leaving the field, closes the list and clears the filter.
  - **Width.** No option extends beyond its key panel: long names such as `question_generation_instruction` wrap
    inside it (iPad D3, width part).
  - **Value stays reachable.** At 375 px, with a key typed that still matches a preset (`url`, or `image`), the value
    field and the Choose image button are not covered by the list; the element at their centre is the field or the
    button itself (phone D2). At 820 px the list also does not extend over the value column. How this is achieved is
    left to planning; the list must follow the same breakpoint as story 1's stacked row.
  - **Both entry points.** The add form's key field and an existing row's key field behave the same, because they are
    one field; one key field replaces the two copies (C3, about -25 lines, estimate). This is how the fix is made
    once and survives story 6's draft row; it is not a separate outcome.
  - **Real iPad check by the owner** with the software keyboard, in both orientations: type `ur` in the add form's key
    field and confirm the list and the value field are visible. If the list still runs off the screen, that is a new
    finding for the owner, not a failure of this story.
- **Excluded (story decisions):**
  - keyboard-aware positioning (visual viewport, opening the list upward); the automated proof is layout at fixed
    widths, not a keyboard;
  - Tab order from the key field into the first preset instead of the value field (phone D2 observation); deferred, the
    owner has not rated it;
  - the numbered `url 2` entry stays as it is and is not tested (SEED-063 boundary);
  - changes to which presets exist, their order, or the key-family rules.
- **Key examples:**
  - Add property, type `ur` → only `url` is listed; clear the text → all available presets are listed again.
  - Type `of` → `example of`; type `UR` → `url`; type `mo` → no list.
  - Focus an existing row's key `custom` without typing → every available preset is listed; type `ur` → only `url`;
    choose it → the key is `url`, the list closes, and the value field of that row has focus.
  - 375 px, add form, type `url` → the value field is not covered by the list. Type `image` → Choose image is not
    covered.
  - 820 px and 375 px, type `question` → the list stays within the key panel and its option wraps inside it; at 820 px
    the value column is not covered.
- **Effort hypothesis:** M, medium-high confidence: one structural slice (shared key field), one filtering slice, one
  layout slice. The keyboard part that made it uncertain is excluded.
- **Depends on:** story 1 for execution order: story 1 changes the row's grid and creates the viewport step and the
  layout feature file that this story's layout scenario extends. The narrowing has no dependency.
- **Boundary with SEED-063:** the numbered preset entry that appears when a key is taken (`url 2`) is left as it is and
  is not a target of this story's filtering tests; SEED-063 removes it with the numbered-key convention. This story
  filters and positions the list only.
- **Safe stopping point:** the add journey works better even if later stories are cancelled.

<a id="story-3"></a>

### A rejected property value is explained next to the row that was rejected

**Identity:** SEED-064#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** the validation message appears below the last row, about 600 px from the edited row on a 19-row
  note and off screen on an iPad in landscape, so the value jumps back with no visible reason (iPad D2).
- **Evaluation:** on a 19-row note enter an invalid `note_level` on an iPad in both orientations: the message shows
  next to that row without scrolling, and disappears when the value is corrected.
- **Value / learning:** the message belongs to its row; adds about 17 lines (estimate) or 4 for a scroll-into-view
  variant, chosen at refinement.
- **Effort hypothesis:** S, high confidence.
- **Depends on:** none.
- **Safe stopping point:** independent.

<a id="story-4"></a>

### Saving a typed Wikidata ID that cannot be used tells the user why

**Identity:** SEED-064#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** with a typed ID such as `Q42` and an empty search list, Save closes nothing and shows nothing
  (reproduced twice: iPad portrait and phone). With a chosen search result Save works.
- **Evaluation:** in the Wikidata dialog type an ID with no matching search results and tap Save: the dialog either
  saves the ID or shows a message that says why not. Which of the two is intended is decided at refinement, against
  the Wikidata scenarios under `e2e_test/features/wikidata/`.
- **Value / learning:** removes a silent dead end; settles whether a typed ID is a supported path.
- **Effort hypothesis:** S, medium confidence: the cause is not yet found.
- **Depends on:** none.
- **Safe stopping point:** independent.

<a id="story-5"></a>

### Read-only properties look and link like the editable ones, without the row panel

**Identity:** SEED-064#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** the read-only list is a separate component with its own grid: values start at ragged positions,
  a single wiki-link value shows as plain `[[...]]` text while list values are links, and an image value shows only a
  file name (iPad I4, C2). Its key column has no maximum width, the likely cause of iPad D1.
- **Evaluation:** open the same note read-only and editable at 820 px: rows line up, single wiki-link values are
  links, image values show the image, and the read-only view still has no chevron, no remove control, no Assimilate
  and no Skip (owner decision). The read-only list component is deleted.
- **Value / learning:** one row for both modes puts the layout in one place, about -60 to -75 lines and one file
  fewer (estimate).
- **Effort hypothesis:** M to L, medium confidence.
- **Boundary with SEED-063:** keeps today's unit of a row (one entry per key as stored) and adds no rule that a key
  is unique; SEED-063 decides whether a row becomes one value. The single shared row is what SEED-063 extends.
- **Depends on:** story 1 (the row layout rule to reuse). Protecting tests first: read-only component tests for a
  single wiki-link value, an image value and a Wikidata value (only spec names were checked, not assertions), and
  story 1's scenario.
- **Safe stopping point:** the read-only view is consistent even if story 6 is cancelled.

<a id="story-6"></a>

### Adding a property uses a row with a visible Add button that says why nothing was added

**Identity:** SEED-064#story-6
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** the add form is a separate structure with its own grid; a row is added only when the value field
  loses focus, so a key without a value, or a value without a key, does nothing and shows no message (iPad I6).
- **Evaluation:** on an iPad tap Add property: a draft row appears in the list with an Add button of at least 44 px;
  entering only a key, then tapping Add, shows what is missing; a complete row is added and saved once; a draft row
  never reaches the stored Markdown (ADR 0004).
- **Value / learning:** the largest code reduction, about -120 to -150 lines and one file fewer (estimate); changes
  the add journey, so the test ids `rich-note-property-key` and `rich-note-property-value` are kept or the page
  object `noteRichPropertyMethods.ts` changes in the same commit.
- **Effort hypothesis:** L, medium confidence: the highest risk of the seed.
- **Boundary with SEED-063:** what happens when the typed key already exists (today: list keys append, other keys are
  refused as duplicates) is SEED-063's decision. This story is refined after SEED-063 and uses its rule; it must not
  freeze the current duplicate-key behavior or the numbered-key suggestions.
- **Depends on:** stories 2 and 5 (one key field and one row to build the draft on) and
  [SEED-063](SEED-063-track-property-values-separately.md#story-1) (how an existing key is added). Protecting tests
  first:
  key-only and value-only add tests (none exist).
- **Safe stopping point:** stories 1 to 5 stand without it.

## Ordering and Scope Reduction

- **Highest priority** (data hidden or a dead end on the primary device): stories 1 to 4, in the order listed.
- **Then** (structure that removes code and prevents the same defects): story 5, then SEED-062 and SEED-063 (already
  queued, they act on the row story 5 unifies), then story 6.
- Stories 2, 3 and 4 are independent of each other; story 5 needs story 1; story 6 needs stories 2 and 5 and
  SEED-063#story-1, because adding a property must follow the new rule for repeated keys.
- Story 3 anchors its message to a row, not to a key, so it holds if SEED-063 makes rows per value.
- First to drop: story 6 (largest risk, the defects it touches are already softened by story 2).

## Open Decisions

- Story 3: the message next to the row (about +17 lines) or the cheaper scroll-into-view (about +4)?
- Story 6 waits for SEED-063's representation; if SEED-063 is dropped or reordered later, story 6 keeps today's
  duplicate-key rule and its dependency on SEED-063 is removed.
- Story 4: is a typed Wikidata ID without search results meant to be supported?
- Not queued, owner has not decided: whether the row panel's Assimilate should move to another note after saving
  a property understanding item, and the wording of its Skip confirmation.

## When to Surface

When the owner selects note properties, iPad or phone comfort, or simplification work from the product backlog.

## Breadcrumbs

- UAT report: `1986473b79:.planning/seeds/SEED-061-note-properties-ux-uat.md`, section `## UAT Findings`
  (screenshots were not committed).
- Related queued work in the same area: [SEED-062](SEED-062-reify-property.md#story-1),
  [SEED-063](SEED-063-track-property-values-separately.md#story-1). Both add to the properties rows; story 5 gives
  them one row to extend, and story 6 follows SEED-063's rule for repeated keys.
- Code: `frontend/src/components/form/RichFrontmatter*.vue`,
  `frontend/src/composables/useRichFrontmatterPropertyEditing.ts`, `frontend/src/utils/noteContentFrontmatter*.ts`.
- Protecting tests today: `e2e_test/features/note_view/note_frontmatter_image.feature`,
  `frontend/tests/components/form/RichMarkdownEditor.frontmatter.spec.ts`.
