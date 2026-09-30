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
  - a collapsed "show all" mode for long property lists (iPad I2); the row wrapping already keeps every value visible;
  - an empty-state message for a note without properties, and hiding the `type: Note` row (iPad I5); its answer
    depends on whether the row is stored (ADR 0004);
  - a message when a value is appended to a list key such as `url` (iPad I7);
  - a confirmation or undo for remove (iPad I8); 44 px targets on touch devices reduce mistaken taps;
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

<a id="story-2"></a>

### Typing a property key narrows the presets, and the list stays on screen

**Identity:** SEED-064#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-note-properties-key-presets/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6bd805231755afb8dc39996234a50c49a4b5c81f24918596da2bc9683dc9e952","plan":"4518b0ea61d267e3c40b339c7d65403ef03a4c4059cf874be4ad493793285d4e"}}
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
    left to planning; the list must not depend on a breakpoint other than the row's own.
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
- **Depends on:** none. This story's layout scenario extends `note_property_layout.feature`, which already has the
  window step `I am on a window {int} * {int}`.
- **Boundary with SEED-063:** the numbered preset entry that appears when a key is taken (`url 2`) is left as it is and
  is not a target of this story's filtering tests; SEED-063 removes it with the numbered-key convention. This story
  filters and positions the list only.
- **Safe stopping point:** the add journey works better even if later stories are cancelled.

<a id="story-3"></a>

### A rejected property value is explained next to the row that was rejected

**Identity:** SEED-064#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/003-note-property-row-rejection-message/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"cf4526e1610ae1667b681fa01ea0dd2c1bdffaa40f651a2d75df17d52c817d35","plan":"8c254fd55c91e343b4a2cb9c4a0dc1d555991014581702aca18809d45213b783"}}
```

- **Goal:** an iPad user (primary device) or a phone user whose change to an existing property row is rejected sees the
  reason directly under that row, without scrolling. Today the reason is one message below the last row, about 600 px
  from the edited row on a 19-row note and off screen on an iPad in landscape, so the value goes back to its old text
  with no visible reason (iPad D2). Nothing is lost when this happens (the old value returns and a retry works); the
  story removes the confusion and the search for the reason.
- **Scope:**
  - **Message under its row.** When a change to an existing row is rejected (a value that breaks a rule such as
    `note_level` outside 1 to 6 or a scalar `aliases`, or a key renamed to an existing key), the message appears
    immediately below that row, inside the list. It is not also shown at the bottom. The value or key goes back to its
    old text, as today.
  - **One message at a time.** A rejection on another row moves the message to that row. A change that is accepted, a
    removal, or a reload clears it, as today. It never stays under a row it does not describe.
  - **Everything else stays as it is.** The add form's message stays directly above the form (already next to what was
    typed), and the Wikidata and value-dialog messages stay where they are. The message keeps its role, live-region
    behavior, size and colour.
  - **Relation type.** A rejected relation type change carries its row in the same way, at no extra cost. It is not a
    promise and has no test of its own: no valid table can make it fail, because the backend refuses invalid stored
    content.
  - **Proof is placement, not layout.** The edited row was just tapped, so it is on screen; a message that is that
    row's neighbour is on screen with it. A frontend component test (which runs in Chromium) proves the neighbour
    relation; no Cypress viewport or touch step is needed.
  - **Real iPad check by the owner** (optional): on the long note change `note_level` to 7 in both orientations and
    confirm the message shows beside the row.
- **Excluded (story decisions):**
  - the add form, Wikidata and value-dialog messages (already adjacent or in their own dialog);
  - the message's size, colour or wording, and a scroll-into-view variant (it moves the edited row off screen and
    contradicts this story's evaluation);
  - keeping the rejected text in the field so the user can correct it instead of retyping: the invalid text would have
    to live in the table while the checks run;
  - any change to which values are valid, and keyboard-aware positioning.
- **Key examples:**
  - 19-row note, `note_level: 3`; set it to `7` and leave the field → the message "note_level must be an integer from 1
    to 6." is directly under that row, and the field shows `3` again.
  - Then set `4` → the change is saved and the message is gone.
  - Two rows `alpha` and `beta`; rename `beta` to `alpha` → "Duplicate property keys are not allowed." under the second
    row, which shows `beta` again.
  - A rejected value on row 1, then a rejected value on row 2 → one message, under row 2.
  - Row with a message is removed → no message remains.
  - Add form, key `note_level`, value `7` → the message is still directly above the form, not under a row.
- **Value / learning:** the message belongs to the row; adds about 15 to 20 lines (estimate) and one small shared
  message component that the add form's message also uses. Story 6's draft row and SEED-063's rows per value can reuse
  it.
- **Effort hypothesis:** S, high confidence: rows already have client ids and both call sites know their row.
- **Depends on:** none. Story 2 (planned) also edits `RichFrontmatterEditablePropertyRow.vue`; the two touch different
  parts (the key field versus the row's neighbour), so only a routine merge is expected.
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
- **Depends on:** none. Protecting tests first: read-only component tests for a
  single wiki-link value, an image value and a Wikidata value (only spec names were checked, not assertions), and
  the read-only scenario in `note_property_layout.feature`.
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
- Stories 2, 3 and 4 are independent of each other; story 6 needs stories 2 and 5 and
  SEED-063#story-1, because adding a property must follow the new rule for repeated keys.
- Story 3 anchors its message to a row, not to a key, so it holds if SEED-063 makes rows per value.
- First to drop: story 6 (largest risk, the defects it touches are already softened by story 2).

## Open Decisions

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
