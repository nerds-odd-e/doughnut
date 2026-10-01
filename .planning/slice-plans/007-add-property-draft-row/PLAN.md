# Add a property through a draft row with a visible Add button that says what is missing

**Identity:** SEED-064#story-6
**Source:** [story](../../seeds/SEED-064-note-properties-fixes.md#story-6), refined 2026-09-30 with the owner's answers:
story 6 does not wait for SEED-063; only Add or Enter adds; the draft row has a Cancel control. Planned on 2026-09-30
at the owner's request; its premises were first observed on story 5's finished branch `exec/seed-064-story-5` at
`16a7c73f9f`.

## Start condition

Met on 2026-10-01: SEED-064#story-5 is on trunk (`16a7c73f9f` is an ancestor of `02464698a4`,
`RichFrontmatterPropertyRow.vue` has the `readOnly` mode, `RichFrontmatterReadOnlyList.vue` is gone). Since
`16a7c73f9f`, trunk changed only the property panel (reify), one `property-value` prop on the row's panel, a page
object `reifyRichNoteProperty` method and two new specs; the add form, `PropertyValueField.vue`,
`useRichFrontmatterPropertyEditing.ts` and the image value are unchanged, so the premises below still hold.

## Goal and scope

A note author on an iPad adds a property with a deliberate step and is told why nothing was added; the add form stops
being a third place that decides how a key and value are shown.

Included: tapping Add property opens a draft at the end of the property list with the key focused; Add, or Enter in the
value field, adds the property once; leaving the value field adds nothing; Add with an empty key, value or both shows a
message next to the draft naming what is missing and keeps what was typed; Cancel removes the draft and saves nothing;
Add and Cancel are at least 44 px on a touch device; the draft is built from the shared property row and the separate
add form component is deleted; key presets, wiki-link values, `url`, `image` (Choose image) and `wikidata_id` (Set…)
keep working in the draft; Wikidata dialog Save and a finished image upload still add the property as today.

Excluded (story decisions): more than one draft at a time (Add property again focuses the open draft, as today); keeping
a draft across leaving the note; any change to what adding an existing key means, and any new test that fixes that rule
(SEED-063 boundary); changes to editing, removing or reordering stored rows, or to the list dialog; a confirmation before
Cancel. A draft is local state until added, so stored Markdown (ADR 0004) gains nothing new. No Accepted ADR or North
Star topic governs this component layout; this is an ordinary plan with no new direction.

## Existing solutions (PFE)

Nothing new is invented. The draft reuses:

- `RichFrontmatterPropertyRow.vue` (story 5): key field with presets, the value branches for text / `image` /
  `wikidata_id` / relation, and the `readOnly` mode that shows a second mode can live in the same template.
- The add rule `tryCommitInsert` in `useRichFrontmatterPropertyEditing.ts` (append for a list-capable existing key,
  "Duplicate property keys are not allowed." otherwise, then `validatePropertyRowsForRichEdit`). It stays the one add
  path; the story only changes what triggers it and adds the missing-field messages in front of it.
- `RichFrontmatterPropertyValidationMessage.vue` and `setValidationMessage` for the message.
- The section class `pointer-coarse:[&_.daisy-btn-sm]:min-h-11` in `RichFrontmatterProperties.vue` for 44 px touch
  targets; the e2e step family `the controls of property … should be at least 44 px high` in
  `e2e_test/step_definitions/note_property.ts` for its proof.
- Test support: `createRichMarkdownEditorTestHarness` (`openAddProperty`, `commitInsertProperty`,
  `setPropertyValueField`, `lastEmittedMarkdown`), e2e page object `noteRichPropertyMethods.ts`.

## Decisive premises

Observed on `exec/seed-064-story-5` at `16a7c73f9f` by reading and searching; re-checked on trunk at
`02464698a4` (2026-10-01).

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| Today a property is added only when the value field loses focus | slice 1 | `RichFrontmatterInsertForm.vue`: `PropertyValueField @blur="emit('value-blur')"`, `RichFrontmatterProperties.vue`: `@value-blur="tryCommitInsert"`; no other caller of `tryCommitInsert` | confirmed |
| The image value's URL input also adds on blur | slice 1 | `RichFrontmatterImagePropertyValue.vue` input `@blur="emit('commit')"`; the add form binds `@commit="emit('value-blur')"` | confirmed on `02464698a4`: slice 1 removes this binding too |
| Enter in the value field adds today only because it blurs the field | slice 1 (Enter needs its own event once blur stops adding) | `PropertyValueField.vue`: `@keydown.enter.prevent="onEnter"`, `onEnter() { root.value?.blur() }`, `onBlur` emits `blur` | confirmed: Enter must emit a distinct event before blurring |
| `tryCommitInsert` returns silently when key or value is blank | slice 2 | `if (!key \|\| !value) return` after `trim()` | confirmed; the red test is the missing message |
| Enter in the key field moves to the value field and does not add | slices 1, 5 | `RichFrontmatterPropertyKeyField.vue` `@keydown.enter.prevent="emit('enter')"` → insert form `focusValueInput` | confirmed |
| A finished image upload adds the property without the add path | scope ("as today"), slice 5 | `RichFrontmatterImagePropertyValue.vue` `onImageFileSelected` → `noteStore.uploadNoteImage(noteId, file)`; the new content arrives through the `parsed` watch in `RichFrontmatterProperties.vue`, which resets `insertOpen`, `draftKey`, `draftValue` | confirmed; typing an image URL does go through the add path, so it needs Add after slice 1 |
| Wikidata dialog Save adds without the add path | scope ("as today"), slice 5 | `useWikidataPropertyDialog.ts` `applyWikidataIdAndClose` → `commitInsertWithWikidataValue` (its own duplicate check, then `onPropertiesChanged`) | confirmed; unaffected by slice 1 |
| Add property while a draft is open focuses the draft and opens no second one | excluded-case note, slice 5 | `openPropertyInsert` sets `insertOpen = true` and focuses `insertKeyInputId`; one `draftKey`/`draftValue` | confirmed |
| Every `daisy-btn-sm` in the section is 44 px on a coarse pointer | slice 4 | section class in `RichFrontmatterProperties.vue`; `note_property_layout.feature` scenario "A touch device gets property controls of at least 44 px" passes for row controls | confirmed by reading; slice 4 observes the draft's buttons |
| Every caller of the add form's blur-to-add | slices 1, 5 | `rg "commitInsertProperty\|openAddProperty\|rich-note-property-key\b\|rich-note-property-value\b\|rich-note-image-insert\|wikidata-property-insert"` over `frontend/tests` and `e2e_test` | frontend: harness `commitInsertProperty` (used by `propertyEntry`, `propertyRowEditing`, `listProperties`, `changesOnlyTheEdit` specs), direct blur in `propertyEntry.spec.ts` "emits composed frontmatter" and `propertyRowEditing.spec.ts` "adds an image property from a typed URL"; e2e page object `noteRichPropertyMethods.ts`: `addRichNoteProperty` (`.blur()`; reached by `note_edit.feature`, `folder_page_readme.feature` via "I add a rich note property…", and `edit_when_assimilating.feature` via "I set the level of…" → `setLevel`), `setRichNoteImagePropertyUrl` (`cli_notebook_lfs.feature` via "I set rich note image property URL…"), `startAddingRichNoteProperty` (`note_property_layout.feature`), `uploadRichNoteImagePropertyFromFixture` (`rich-note-image-insert-file-input`; `note_frontmatter_image.feature`), `associateWikidataDialog` (`rich-note-wikidata-property-insert-edit`; `associate_wikidata.feature`); step `expectNewRichNotePropertyControlNotCovered('rich-note-image-insert-choose')` |
| The shared row's value dialog opener emits `update:propertyValue` then `commit` on save | slice 5 decision | `RichFrontmatterScalarPropertyValue.vue` `onValueDialogSave`; the add form never had that opener | confirmed; the draft does not show it (current decision) |
| No key-only or value-only add test exists | slice 2 | `rg -n "key only\|value only\|missing" frontend/tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts` and the seed's note | none |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Add adds the property once; the draft closes | slice 1 | `RichMarkdownEditor.propertyEntry.spec.ts`: one emitted Markdown with `status: draft` after tapping Add, red first |
| Enter in the value field adds, like Add | slice 1 | same spec |
| Leaving the value field adds nothing and keeps the draft | slice 1 | same spec: blur → no emitted change, draft still shown, red first |
| The add journey still works end to end | slice 1 | `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_edit.feature` after the page object taps Add |
| Missing key, value or both → message next to the draft naming what is missing; typed text kept; nothing saved | slice 2 | same spec, three cases, red first |
| Cancel removes the draft and saves nothing | slice 3 | same spec, red first |
| Add and Cancel are at least 44 px on a touch device | slice 4 | new scenario in `e2e_test/features/note_topology/note_property_layout.feature` |
| The draft is a property row at the end of the list, also on a note without properties; one draft at a time | slice 5 | same spec (draft located by the row's `data-testid` and a draft marker), all slice 1–3 cases green with locators only changed |
| Presets, wiki links, `url`, `image`, `wikidata_id` still work in the draft; Wikidata Save and image upload still add | slice 5 | existing `propertyEntry.spec.ts` preset and Wikidata cases, `propertyRowEditing.spec.ts` image case; e2e `note_property_layout.feature` "key presets of a new property" scenarios, `note_frontmatter_image.feature`, `associate_wikidata.feature` |
| Existing-key behavior is whatever the current rule does; no new test fixes it | all | the existing "appends to exact list-capable keys" case keeps its assertion (only its helper changes); no new existing-key case is added |
| Stored rows are unchanged | slices 1–5 | `propertyRowEditing.spec.ts`, `listProperties.spec.ts`, `readOnlyProperties.spec.ts`, `propertyLocation.spec.ts` green with no assertion changed |
| Real iPad check | owner, optional, after slice 5 | draft row, focus with the real keyboard, tap Add with a missing value, tap Cancel |

Component specs run with
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form/<spec>`.

## Ordered slices

Size target is about 10 minutes including proof, as in the previous plans of this seed.

### 1. Only Add or Enter adds a property
Type: Behavior
Status: done
Proof: `propertyEntry.spec.ts` new cases red then green; the specs listed in the premise "Every caller" green after their
helper change; `note_edit.feature` once.

Behavior: add form with key `status` and value `draft` → tap Add → one save with `status: draft`, the form closes;
same → press Enter in the value field → same result; same → the value field loses focus → nothing is saved, the form
stays with what was typed.

Change: an Add button (`daisy-btn-sm`) in `RichFrontmatterInsertForm.vue` calls `tryCommitInsert`; the value field's
blur no longer does. `PropertyValueField.vue` emits `enter` before it blurs; the add form turns it into the same add.
Existing rows keep committing on blur (they listen to `blur`, not `enter`). Update the harness `commitInsertProperty`
and the two direct blur calls to tap Add, and the e2e page object `addRichNoteProperty` to click Add instead of
`.blur()`. The add form's image value `@commit` (its URL input blur) stops adding too, so the image URL case (typed URL) now
needs Add as well; image upload and Wikidata Save are untouched.

### 2. Add says what is missing
Type: Behavior
Status: done
Proof: `propertyEntry.spec.ts` three new cases red then green.

Behavior: key `topic`, value empty → Add → the message "Enter a property value." next to the add form, nothing saved,
`topic` still typed; key empty, value `training` → "Enter a property key."; both empty → "Enter a property key and
value.". Change: replace the silent return in `tryCommitInsert` with `setValidationMessage`. The message clears on the
next successful add, as validation messages do today.

### 3. Cancel drops the draft
Type: Behavior
Status: done
Proof: `propertyEntry.spec.ts` new case red then green.

Behavior: add form with key `topic` and value `training` → tap Cancel (× icon, `aria-label="Cancel adding property"`,
`daisy-btn-sm`) → the form is gone, nothing is saved; Add property opens an empty form again. Change: Cancel sets
`insertOpen` false and clears `draftKey`, `draftValue` and the validation message.

### 4. Add and Cancel are touch sized
Type: Behavior
Status: planned
Proof: new scenario "A touch device gets new property controls of at least 44 px" in `note_property_layout.feature`
(`I use a touch device`, window 820 × 1000, `I start adding a property with key "topic"`, then a new step that the new
property's Add and Cancel controls are at least 44 px high), run with
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/note_property_layout.feature`.

Behavior: touch device → start adding a property → Add and Cancel are at least 44 px high. Expected green without a
product change (the section class covers every `daisy-btn-sm`); if it is red, the fix belongs in this slice. Reuse
`expectRichNotePropertyControlHeights`' measurement for the new-property controls rather than writing a second one.

### 5. The draft is a property row and the add form is deleted
Type: Structure
Status: planned
Proof: all slice 1–3 cases and the existing `propertyEntry`, `propertyRowEditing`, `listProperties`,
`changesOnlyTheEdit`, `readOnlyProperties`, `propertyLocation` specs green with only locators changed; one new case: a
note without properties → Add property → one draft row inside the list; Add property again → still one draft row. Then
e2e once: `note_property_layout.feature`, `note_frontmatter_image.feature`, `associate_wikidata.feature`,
`note_edit.feature`.

Change: `RichFrontmatterPropertyRow.vue` gains a draft mode: key field (presets, no excluded row) and the same value
branches, Add and Cancel in place of the chevron and panel, no value-dialog opener; its key and value blur do nothing in
draft mode (the list does not bind `commit` for the draft) and value Enter emits the add. `RichFrontmatterPropertyList`
renders the draft row after the stored rows with its message beneath it, and is shown whenever there are rows or a
draft. `RichFrontmatterProperties.vue` drops the add form; `RichFrontmatterInsertForm.vue` is deleted. Test ids: the
draft uses the row's own ids inside a row marked `data-property-draft="true"`; the harness, `propertiesTestDom.ts`,
the page object methods and the `expectNewRichNotePropertyControlNotCovered` step scope to that row in the same commit
(the add-form ids `rich-note-property-key`, `rich-note-property-value`, `rich-note-image-insert-*`,
`rich-note-wikidata-property-insert-edit` go away). The add rule, the Wikidata insert context and the draft refs stay
as they are.

If this runs past about 10 minutes, split at "the row's draft mode, exercised by a component test with the add form
still in place" and "the list renders the draft row and the add form is deleted".

## Current decisions

- One add path: `tryCommitInsert` stays the single place that turns a draft into a stored property; the Wikidata dialog
  keeps its own existing insert commit. Neither changes its existing-key rule.
- Behavior first on today's add form (slices 1–4), structure last (slice 5), so the visible fixes stand even if the row
  rebuild is cancelled.
- Enter is a distinct `enter` event from `PropertyValueField`; blur never adds.
- Message wording: "Enter a property key.", "Enter a property value.", "Enter a property key and value.".
- The draft row shows no value-dialog opener; the add form never had one and the dialog's save commits a stored row.
- The draft is located in tests by the row `data-testid` plus `data-property-draft="true"`, not by separate test ids.
- Tests do not call `console.log`.

## Execution context

- Mode: story-branch; workspace: `/Users/terryyin/git/doughnut/.worktrees/adding-a-property-uses-a-row-with-a-visible-add`.
- Branch: `codex/adding-a-property-uses-a-row-with-a-visible-add`; publisher: `dashboard-mac.lan-doughnut`; agent: Maria-chan.
- Authorized remote: origin; trunk: main; increments target the execution branch.
- Established claim: `94de559d13059fe233e5fb85f960133f1a751c9d`; starting revision: `e2d621e989fc00cc011bd1eed254af967d6e22ff`.
- Setup: `./scripts/run.sh bash scripts/worktree_setup.sh`, then `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit`, both passed in this checkout.
- Replanning: existing planned authority retained; slice 5's explicit split remains available.
- CI source: GitHub Actions, verified push workflow `ci.yml` (donut CI). Claim on trunk is unobserved.

## Accepted proof

### Slice 1

- Red: `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts` failed for absent Add and saving on text/image blur.
- Green: `./scripts/run.sh pnpm frontend:test` — 309 files, 1981 tests passed. New insertion cases drive RichMarkdownEditor with a local draft and observe one Markdown emission on Add/Enter (text and image), closure on parent model echo, and no emission with retained draft on blur.
- Refactor replacement: `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts tests/components/form/RichMarkdownEditor.propertyFocusAndPresets.spec.ts tests/components/form/RichMarkdownEditor.propertyRowEditing.spec.ts tests/components/form/RichMarkdownEditor.relationPropertyEditing.spec.ts` — 34 tests passed after responsibility-based extraction; production boundary unchanged.
- `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` passed after refactor.
- `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_edit.feature` — 12 scenarios passed, including saved rich properties after reload and Markdown source reflecting the Add action.

### Slice 2

- `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts`: four missing-field/clear-on-success cases red before change, 13 tests green after implementation and refactor. Mounted editor assertions observe exact messages, no emission, retained key/value, then cleared alert on successful Add.
- `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` passed on final refactor content. Earlier full-suite and E2E proof unchanged.
- Slice 1 accepted revision: `c05b83ea2b8a5ed3b0bd1534d720fcd08f8c9539` on the execution branch. Managed delivery reports `pendingCi: unobserved` because the Codex yielded-cell bridge is unavailable; no observer was armed.

### Slice 3

- `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts`: Cancel case red for absent control then green. Assertions observe draft closure, no save, cleared alert and empty reopened fields.
- Refactor replacement: `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts tests/components/form/RichMarkdownEditor.propertyRenameGuard.spec.ts` — 14 tests passed after sharing draft reset with the parsed-property watcher and extracting unrelated rename-guard coverage.
- `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` passed. Include `propertyRenameGuard.spec.ts` in slice 5 regression proof.
- Slice 2 accepted revision: `454bb48eaa208dde9d3a759d59344a41fd585f65` on execution branch; CI remains unobserved.

## Learnings

- Current existing-key behavior is per-value append from SEED-063; preserved unchanged.
- Newly landed `NoteEditableContent.trackerFollowsValue.spec.ts` also used direct blur-to-add; its helper now clicks Add, with assertions unchanged.
- Image URL Enter requires its own explicit event as text Enter does.
- Required file-size refactoring extracted `propertyFocusAndPresets.spec.ts` and `relationPropertyEditing.spec.ts`; typed-image insertion now lives in `propertyEntry.spec.ts`. Include these new specs in slice 5 regression proof.

