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

<a id="story-4"></a>

### Saving a typed Wikidata ID shows the title choice even when the search found nothing

**Identity:** SEED-064#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-wikidata-typed-id-title-choice/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"28ae3509c41d88fe5cfcc9e62134d32ed772e2faf6189bd451682410a070cf43","plan":"4f04ce63e96af3dded0e4bf0c35b36c469aae147d917e9bc5930155b69f1f342"}}
```

- **Goal:** a user (iPad, phone or desktop; the cause is not device-specific) who types or pastes a Wikidata ID into a
  property's Wikidata dialog, when the title search found nothing, sees what Save is asking. Typing an ID is an
  intended way to fill the property (owner decision, 2026-09-30): it is the natural fallback exactly when the search
  finds nothing. Today the first Save appears to do nothing (reproduced twice, iPad portrait and phone). Reading the
  code, Save checks the ID and, when that entry's English label differs from the note title, is meant to show the
  "Replace title" / "Add as alias" choice; the dialog hides that choice behind "No Wikidata entries found" whenever the
  search list is empty. Nothing is lost and a second Save appears to apply the ID; the owner accepts a second Save as
  long as the meaning is clear.
- **Scope:**
  - **Choice is visible.** In the property Wikidata dialog, Save on a typed ID whose English label differs from the note
    title shows the "Replace title" / "Add as alias" choice and keeps the dialog open, whether or not the search list is
    empty. The choice takes the place of the results area, as it already does when the search has results. The
    "No Wikidata entries found" text is not shown beside it.
  - **Second Save keeps the title.** With the choice showing, Save without choosing applies the ID and closes the
    dialog, leaving the title as it is. This is today's behavior and is what the owner accepts as the second step; it is
    proven, not changed.
  - **Confirm the cause first.** The cause above is read from the code and not yet run. The first slice reproduces the
    report with an empty search list; if the cause differs, that is reported to the owner before the fix.
  - **Everything else stays as it is.** Label equal to the title, or an empty label, saves and closes at once; an
    invalid ID or an unavailable service shows its existing message; choosing a search result works as before.
  - **Real iPad check by the owner** (optional): a note whose title finds no Wikidata entries, type `Q42`, tap Save, and
    confirm the choice is visible.
- **Excluded (story decisions):**
  - a message about why an ID cannot be used: there is no such case; real rejections already show their own messages;
  - whether saving a property should offer to rename the note at all (existing behavior, a product question);
  - the "No Wikidata entries found" wording, other dialog layout, and keyboard or viewport behavior;
  - a Cypress scenario: a frontend component test with an empty search stub is the proof;
  - a stale-ID hole found while reading: once the choice is showing, editing the ID and pressing Save applies the new ID
    without checking it. It is reachable today after choosing a search result and is not changed here (finding for the
    owner).
- **Key examples:**
  - Note "Snake", search list empty; type `Q42` (English label "Douglas Adams"), Save → the choice is shown and the
    dialog stays open. Choose "Add as alias" → the ID is saved and the alias is added.
  - Same, then Save again without choosing → the ID is saved, the dialog closes, the title is unchanged.
  - Same setup, an ID whose label equals the title (any case) → saved and closed at once.
  - Same setup, `Q12345R` → the existing invalid-ID message.
  - Search list not empty, a result chosen whose label differs → the choice shows, as today.
- **Value / learning:** removes a silent dead end on a route the owner keeps; settles that a typed ID is a supported
  path.
- **Effort hypothesis:** S, medium-high confidence: the likely cause is one template branch order; the first slice
  confirms it.
- **Depends on:** none.
- **Safe stopping point:** independent.

<a id="story-5"></a>

### A read-only property's wiki-link value is a link, and both views share one row

**Identity:** SEED-064#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/005-note-property-read-only-row/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6493571101843d930cf0fb0e5075d253f33f3586c49ea8174789cd46d32079c4","plan":"58c5c945b2e1ef580c5d0bb649e8a5419e87a71dac247016a08dde76261acea4"}}
```

- **Goal:** a reader of a shared or subscribed note (read-only view) can follow a property whose whole value is a
  wiki link, such as `related: "[[Other]]"`. Today that value shows as plain `[[Other]]` text, while the same value
  is a link in the editable view. The story then makes the read-only view a mode of the editable row, so the layout
  of a property row lives in one place for SEED-062, SEED-063 and story 6 to extend. Nothing is lost or blocked
  today: the reader can still read the text and find the note another way. The reader-visible gain is small; the
  structural gain is what queued work depends on.
- **Scope:**
  - **Link.** In the read-only view, a scalar property value that is a single whole wiki link is a link to the
    note it names, in the way the editable view links it. Any other scalar value, and every list value, shows as
    it does today.
  - **One row.** The read-only view is rendered by the same row component as the editable view, in a read-only
    mode: the key is text, the value is the compact display used today (relation label, Wikidata ID with its link,
    URL with its link, list value, plain text). The separate read-only list component is deleted, and the row
    and list names stop saying "Editable".
  - **Read-only stays read-only.** No chevron, no property panel, no Assimilate, no Skip, no remove control, no key
    or value input, no key presets (owner decision, 2026-09-30). A property visit still highlights the row and
    scrolls it into view (existing behavior, `RichMarkdownEditor.propertyLocation.spec.ts`).
  - **Semantics kept.** The read-only list stays a description list (`dl`, `dt`, `dd`); the specs that read it
    keep working.
  - **Evidence about the reported layout.** A Chromium measurement (2026-09-30, five rows, one width) found the
    read-only value column already starts at the same position on every row, and so does the editable one. The
    seed's "ragged positions" finding is therefore not reproduced within a view; the two views only differ from each
    other (read-only has no chevron column). Cross-view alignment is not a promise here, and the long-key defect
    (iPad D1) was already fixed by commit `da80025b09`.
- **Excluded (story decisions):**
  - showing an image for an `image` value, in either mode: the editable row does not show one either (it shows the
    URL in an input with Replace / Choose image), so this is a new capability and would be its own story;
  - a dead wiki link in the read-only view doing anything when tapped (not wired today, as for list values);
  - linking text inside a longer value such as `see [[A]] and [[B]]`: the read-only view keeps plain text for it;
  - pixel-identical layout between the two views, or adding a blank chevron column to the read-only view;
  - any rule that a key is unique or appears once (SEED-063 boundary);
  - changes to what the editable view shows or does.
- **Key examples:**
  - Read-only note with `related: "[[Other]]"` where `Other` is a note the reader can see → the value is a link to
    that note (today: the text `[[Other]]`).
  - Same note, editable view → unchanged, the value is a link inside the value field.
  - Read-only `topic: training` → plain text `training`, as today.
  - Read-only `wikidata_id: Q42` → `Q42` and its external link, as today; `url: https://a.io` → text and link.
  - Read-only `tags: [alpha, beta]` → compact list, as today.
  - Read-only, property visit for `topic` → that row is highlighted and scrolled into view; other rows are not.
  - Read-only note → no toggle, remove control, key input, value input, Assimilate or Skip appears anywhere in the list.
  - 820 px and 375 px, long-key note (existing `note_property_layout.feature` read-only scenario) → every value stays
    visible and the note does not scroll sideways.
- **Value / learning:** the wiki-link value becomes a link for readers, and one row serves both modes. The code
  reduction is smaller than first estimated: the read-only value display moves into the row rather than
  disappearing, so expect roughly -20 to -35 lines and one component fewer (estimate, not a promise). The larger
  saving belongs to story 6.
- **Effort hypothesis:** M, medium confidence.
- **Boundary with SEED-063:** keeps today's unit of a row (one entry per key as stored) and adds no rule that a key
  is unique; SEED-063 decides whether a row becomes one value. The single shared row is what SEED-063 extends.
- **Depends on:** none for the link. The row merge waits until story 2 (Taken) is on trunk, because both edit the row.
- **Safe stopping point:** the wiki-link value is a link for readers even if the row merge is cancelled.

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
- First to drop: story 6 (largest risk, the defects it touches are already softened by story 2).

## Open Decisions

- Story 6 waits for SEED-063's representation; if SEED-063 is dropped or reordered later, story 6 keeps today's
  duplicate-key rule and its dependency on SEED-063 is removed.
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
