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
