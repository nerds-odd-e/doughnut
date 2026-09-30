# Explain a rejected property change next to its row

**Identity:** SEED-064#story-3
**Source:** [story](../../seeds/SEED-064-note-properties-fixes.md#story-3), refined 2026-09-30 from the owner's review
answers: message directly under the rejected row, revert kept, relation-type path carried at no extra cost, scroll-into-view
and "keep the rejected text" excluded.

## Goal and scope

An iPad or phone user whose change to an existing property row is rejected sees the reason directly under that row,
without scrolling.

Included: a rejected row commit (a value that breaks a rule such as `note_level` outside 1 to 6, or a key renamed to an
existing key) shows its message immediately below that row and not at the bottom; a rejection on another row moves the
message; an accepted change, a removal or a reload clears it, as today.

Excluded (story decisions): the add form's message (stays directly above the form), the Wikidata and value-dialog messages,
the message's size, colour and wording, scroll-into-view, keeping the rejected text in the field, any change to which
values are valid. No stored-Markdown behavior changes, so ADR 0004 is unaffected. No Accepted ADR or North Star topic
governs where a validation message sits, so this is an ordinary plan with no new direction.

Assumption: the relation-type path (`onRelationTypeSelected`) also passes its row, because it is the same call. It is
not a promise and has no test: no valid table makes it fail (the backend refuses invalid stored content).

## Existing solutions (PFE)

Nothing new is built. Reused: the single `validationMessage` state and its `role="alert"` paragraph in
`RichFrontmatterProperties.vue`, `setValidationMessage` / `clearValidation` in `useRichFrontmatterPropertyEditing.ts`,
the row list's `v-for` in `RichFrontmatterEditablePropertyList.vue`, and the test helpers `attemptRenamePropertyKey`,
`propertyRowSelector`, `propertyValidationText` (`propertiesTestDom.ts`) and `setPropertyValueField` (test harness).
One small component is extracted so the bottom message and the row message share their markup; no other file is
merged or created for this.

## Decisive premises

| Premise | Operation that consumes it | Observation | Result |
| --- | --- | --- | --- |
| Among the six writers of the message, only `commitRow` and `onRelationTypeSelected` know a row index | choosing which call sites pass a row | read `useRichFrontmatterPropertyEditing.ts` (`tryCommitInsert`, `addWikiLinkAsProperty`) and `useWikidataPropertyDialog.ts` | confirmed: insert, wiki-link and Wikidata paths have no row; they stay at the bottom |
| An invalid `note_level` typed into a row is rejected on value blur and the field returns to its old text | slice 2's note_level example | throwaway component spec (mount `note_level: 3`, set `7`, blur), deleted afterwards | message `note_level must be an integer from 1 to 6.` shown, field text `3`, message not adjacent to the row today |
| Renaming a key to an existing key is rejected and reverts | slice 2's rename example | existing spec `rejects duplicate keys before emitting valid renamed keys and values` in `RichMarkdownEditor.propertyRowEditing.spec.ts` | passes today (run with the two row specs below) |
| Component tests run in a real browser and the named specs are green now | every slice's proof | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run tests/components/form/RichMarkdownEditor.propertyRowEditing.spec.ts tests/components/form/RichMarkdownEditor.listProperties.spec.ts` | 18 passed in Chromium; note `console.log` fails a test, so probes must not log |
| The message is read through a whole-tree query, so moving it keeps the existing assertions valid | slice 1 and 2's "no assertion changed" proof | grep `propertyValidationText\|rich-note-property-validation` in `frontend/tests` | only `propertiesTestDom.ts` queries it; two callers (rename duplicate, list-property row commit) read it via the helper |
| The row list container does not currently contain the message | slice 2's neighbour proof | same throwaway spec: `row.nextElementSibling === message` | false today, so the neighbour assertion is red before the change |
| The message is not needed in the read-only view | scope | read `RichFrontmatterProperties.vue` | the read-only list renders no editing and the message can only come from editing paths |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| The message, its role and its text are unchanged where it stays | slice 1 | `RichMarkdownEditor.propertyRowEditing.spec.ts`, `RichMarkdownEditor.listProperties.spec.ts`, `RichMarkdownEditor.propertyEntry.spec.ts` green with no assertion changed |
| A rejected row change shows its message directly under that row, and the value returns | slice 2 | component tests: `note_level` `7` on row 1 of a two-row note → message is row 1's next element, field shows `3`; rename `beta` (row 2) to `alpha` → message is row 2's next element |
| One message, moving with the rejection; cleared by an accepted change and by removal | slice 2 | same spec: rejection on row 1 then on row 2 → exactly one message, under row 2; a valid edit then leaves none; removing the row with a message leaves none |
| The add form's message stays above the form, not under a row | slice 2 | component test: add form key `note_level`, value `7` → one message that is not inside or beside a row |
| Real iPad check | owner, optional, after slice 2 | manual: long note, `note_level` to `7`, both orientations |

The component tests run in Chromium, so the neighbour relation is a real DOM relation. No Cypress scenario is planned:
the edited row was just tapped and is on screen, so its neighbour is too.

## Ordered slices

Size target is about 10 minutes each including proof.

### 1. The message has one component for both places
Type: Structure
Status: done
Proof: the three frontend specs named above are green with no assertion changed
(`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run <the three spec files>`).

Internal change: extract the `role="alert"` paragraph (classes `text-error text-xs mt-1`, test id
`rich-note-property-validation`) from `RichFrontmatterProperties.vue` into
`RichFrontmatterPropertyValidationMessage.vue` and render it there. Enables slice 2 to show the same message under a
row without copying the markup.

### 2. A rejected row change is explained directly under that row
Type: Behavior
Status: done
Proof: the new component tests in `RichMarkdownEditor.propertyRowEditing.spec.ts` (with a small helper in
`propertiesTestDom.ts` that returns the message element that follows a row), red before the change and green after; then
the three specs of slice 1 unchanged, run once.

Behavior: a note with rows `note_level: 3` and `beta` → the user sets `note_level` to `7` and leaves the field → the
message is directly under that row and the field shows `3`; a rename of `beta` to `alpha` puts the duplicate-key message
under that row; a rejection on the other row moves the single message; an accepted edit or a removal leaves none; the add
form's message is still directly above the form.

Change: `setValidationMessage(message, rowIndex?)` also records the row and `clearValidation` and the reload watch clear
it; `commitRow` and `onRelationTypeSelected` pass their `idx`. `RichFrontmatterEditablePropertyList.vue` receives the
message and row index and renders the shared component after the matching row; the bottom paragraph in
`RichFrontmatterProperties.vue` renders only a message without a row. If the first red run shows the message already
adjacent, stop and re-read the layout before changing anything.

## Current decisions

- The row message sits after the row inside the list (a sibling of the row), so it is outside the focus highlight and
  needs no change to the row component that story 2 edits.
- The row is identified by its index; every path that changes rows emits and clears the message, so the index cannot
  outlive the row it describes (observed in `emitProperties`, which calls `clearValidation`).
- Tests do not call `console.log`: it fails the run in this project's test setup.

## Learnings

- Slice 1 (2026-09-30): proof green — the three specs, 27 tests, no assertion changed. `frontend/components.d.ts`
  (tracked, auto-generated) gains the new component's declarations; commit it with the component.
- Slice 2 (2026-09-30): red run showed 3 of 4 new tests failing (message not adjacent; add-form test passes by design),
  then green: the three specs 31 passed, `vue-tsc --noEmit` clean. Relation-type path passes `idx`, untested as planned.
  The real iPad check stays with the owner.

## Real iPad check (owner, optional)

After slice 2, on the long note change `note_level` to `7` in both orientations and confirm the message shows beside
the row.
