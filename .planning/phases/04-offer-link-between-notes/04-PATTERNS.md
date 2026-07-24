# Phase 4: Offer link between notes - Pattern Map

**Mapped:** 2026-07-24
**Files analyzed:** 9
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/MatchedNoteLinkOffer.vue` (NEW) | component | request-response | `frontend/src/components/links/SearchForm.vue` | exact (minus search step) |
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | component | request-response | `frontend/src/components/notes/core/NoteToolbar.vue` (PopButton + gate) + self matched-notes section | role-match |
| `frontend/src/components/links/LinkInsertionChoice.vue` | component | request-response | self — additive `bareWikiLinkAvailable` prop | exact |
| `frontend/src/utils/noteContentPropertyRows.ts` | utility | transform | `NoteEditableContent.vue` `registerWikiPropertyInserter.insert` | exact |
| `frontend/src/components/notes/core/NoteEditableContent.vue` | component | transform | same file — call extracted helper | exact |
| `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` (NEW) | test | request-response | `frontend/tests/links/AddRelationship.spec.ts` | role-match |
| `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` | test | request-response | self — extend ACCIDENTAL_MATCH cases | exact |
| `e2e_test/features/recall/accidental_match_reveal.feature` | test | request-response | self + `noteTargetSearchDialog.ts` button labels | role-match |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | test | request-response | self `expectAccidentalMatchReveal` + `noteTargetSearchDialog.createRelationshipToTargetAs` | role-match |

## Pattern Assignments

### `frontend/src/components/recall/MatchedNoteLinkOffer.vue` (component, request-response)

**Analog:** `frontend/src/components/links/SearchForm.vue`

**Imports pattern** (lines 35–51):
```typescript
import { ref, computed, nextTick } from "vue"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import AddRelationshipFinalize from "./AddRelationshipFinalize.vue"
import LinkInsertionChoice from "./LinkInsertionChoice.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
```

**Core two-stage state machine** (lines 15–31, 72–73) — copy this, skip search:
```vue
<LinkInsertionChoice
  v-if="selectedSearchResult && !targetSearchResult && note"
  :target-note-topology="selectedSearchResult.noteTopology"
  :wiki-property-option-available="wikiPropertyOptionAvailable"
  @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
  @choose-add-relationship="targetSearchResult = selectedSearchResult!"
  @go-back="selectedSearchResult = undefined"
/>
<AddRelationshipFinalize
  v-if="targetSearchResult && note"
  v-bind="{ targetSearchResult, note }"
  @success="$emit('closeDialog')"
  @go-back="targetSearchResult = undefined"
/>
```
```typescript
const selectedSearchResult = ref<NoteSearchResult | undefined>(undefined)
const targetSearchResult = ref<NoteSearchResult | undefined>(undefined)
```

**Recall-specific differences from SearchForm:**
- Seed `selectedSearchResult` from props (preselected matched note) — never show `SearchForNoteAndFolder`.
- Pass `:bare-wiki-link-available="false"` on `LinkInsertionChoice`.
- Do **not** import `useContentCursorInserter`; compute `wikiPropertyOptionAvailable` from `parseNoteContentMarkdown(sourceNote.content ?? "").ok`.
- Property write uses dead-link-style `updateTextField` (below), not `insertWikiLinkAsProperty`.

**Dead-link direct-write analog** (SearchForm lines 102–117) — property path for recall:
```typescript
async function onLinkDeadLinkToNote() {
  if (!selectedSearchResult.value || !note || !deadLinkPayload) return
  const newLinkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
    displayText: deadLinkPayload.displayText,
  })
  // ...
  await closeDialogThen(() =>
    storageAccessor.value
      .storedApi()
      .updateTextField(note.id, "edit content", newContent)
  )
}
```

**closeDialogThen pattern** (SearchForm lines 80–84) — reuse for property success (D-07: close dialog, stay on page):
```typescript
async function closeDialogThen(run: () => void | Promise<void>) {
  emit("closeDialog")
  await nextTick()
  await run()
}
```

**Note:** `AddRelationshipFinalize` emits `success` then navigates via router internally. For D-07 (remain on recall result), the wrapper should still `@success` → close PopButton/Modal; if finalize navigates away, planner must decide whether to suppress navigation for this entry point (existing finalize always navigates after create — verify before planning).

**Build NoteSearchResult from NoteStorage** (RESEARCH Pattern 1 / StoredApiCollection lines 229–233):
```typescript
getNoteRealmRefAndLoadWhenNeeded(noteId: Doughnut.ID) {
  const result = this.storage.refOfNoteRealm(noteId)
  if (!result.value) this.loadNote(noteId)
  return result
}
```
```typescript
const matchedSearchResult = computed<NoteSearchResult | undefined>(() => {
  const realm = matchedRealmRef.value
  if (!realm) return undefined
  return {
    noteTopology: realm.note.noteTopology,
    notebookId: realm.notebookRealm.notebook.id,
    notebookName: realm.notebookRealm.notebook.name,
  }
})
```

---

### `frontend/src/components/recall/AnsweredSpellingQuestion.vue` (component, request-response)

**Analog (CTA + dialog host):** `frontend/src/components/notes/core/NoteToolbar.vue`  
**Analog (placement):** self — `matched-notes-section` (lines 26–41)

**Matched-notes row insertion point** (AnsweredSpellingQuestion lines 33–39) — CTA inside existing wrapper, below NoteShow:
```vue
<div
  v-for="matched in answeredQuestion.matchedNotes"
  :key="matched.id"
  :data-testid="`matched-note-${matched.id}`"
>
  <NoteShow :note-id="matched.id" :expand-children="false" />
  <!-- NEW: PopButton CTA here when canOfferLink -->
</div>
```

**PopButton + SearchForm open pattern** (NoteToolbar lines 14–31):
```vue
<PopButton
  v-if="!readonly"
  ref="linkPopButtonRef"
  aria-label="Link"
  title="Link (Ctrl+Shift+F / Cmd+Shift+F)"
  :show-close-button="false"
>
  <template #button_face>
    <SvgSearchForLink />
  </template>
  <template #default="{ closer }">
    <SearchForm
      v-bind="{ note }"
      :modal-closer="closer"
      @close-dialog="closer"
    />
  </template>
</PopButton>
```

**UI-SPEC CTA styling** — replace icon face with text button:
- Label: `"Link to this note"`
- Classes: `daisy-btn daisy-btn-secondary daisy-btn-sm` via PopButton `btn-class`
- `data-testid="link-to-matched-note-{id}"`
- Spacing: `mt-2` under NoteShow

**PopButton slot API** (`PopButton.vue` lines 15–24, 55–64):
```vue
<template #default="{ closer }">
  <MatchedNoteLinkOffer ... @close-dialog="closer" />
</template>
```

**Readonly / write-permission gate** (NoteShow.vue lines 130–132):
```typescript
const currentUser = inject<Ref<User | undefined>>("currentUser")
const readonly = (noteRealm: NoteRealm) =>
  !currentUser?.value || noteRealm.notebookRealm.readonly === true
```

**Combined gate for CTA** (RESEARCH / NoteShow + Storage):
```typescript
const canOfferLink = computed(
  () =>
    !!currentUser?.value &&
    !!reviewedRealmRef.value &&
    reviewedRealmRef.value.notebookRealm.readonly !== true &&
    !!matchedRealmRef.value
)
```
Omit CTA when false (silent hide, same as toolbar — not disabled-with-tooltip).

**Source note id:** `answeredQuestion.recalledNote.noteTopology.id`  
**Target note id:** `matched.id` from `v-for`

---

### `frontend/src/components/links/LinkInsertionChoice.vue` (component, request-response)

**Analog:** self — mirror existing optional-button `v-if` pattern

**Current props** (lines 40–44):
```typescript
const props = defineProps<{
  targetNoteTopology: NoteTopology
  wikiPropertyOptionAvailable?: boolean
  deadLinkDisplayText?: string
}>()
```

**Primary button today** (lines 10–12) — always shown; for recall, hide bare wiki insert:
```vue
<button class="daisy-btn daisy-btn-primary" @click="onPrimaryClick">
  {{ primaryLabel }}
</button>
```

**Additive prop pattern** (RESEARCH Pattern 3):
```typescript
bareWikiLinkAvailable?: boolean // default true — recall passes false
```
```vue
<button
  v-if="bareWikiLinkAvailable !== false || deadLinkDisplayText"
  class="daisy-btn daisy-btn-primary"
  @click="onPrimaryClick"
>
  {{ primaryLabel }}
</button>
```

Keep accent property + secondary relationship buttons unchanged when `!deadLinkDisplayText` (lines 13–26).

**Existing choice button labels** (do not change):
- `"Add wiki link as a new property"` (`daisy-btn-accent`)
- `"Add a new relationship note"` (`daisy-btn-secondary`)

---

### `frontend/src/utils/noteContentPropertyRows.ts` + `NoteEditableContent.vue` (utility / transform)

**Analog:** `NoteEditableContent.vue` `registerWikiPropertyInserter.insert` (lines 158–168)

**Logic to extract as pure helper:**
```typescript
const parsed = parseNoteContentMarkdown(props.noteContent ?? "")
if (!parsed.ok) return
const rows = [
  ...sortedPropertyRowsFromNoteProperties(parsed.properties),
  propertyRowWithScalar("", text),
]
if (!validatePropertyRowsForRichEdit(rows).ok) return
const composed = composeNoteContentFromPropertyRows(rows, parsed.body)
```

**Recommended shared signature** (RESEARCH Pattern 2):
```typescript
function appendWikiLinkPropertyRow(
  content: string,
  linkText: string
): string | undefined
```

Place in `noteContentPropertyRows.ts` (already owns `propertyRowWithScalar` / `composeNoteContentFromPropertyRows`).  
`NoteEditableContent` keeps caret / rich-editor side effects after calling the helper.  
Recall wrapper: `buildWikiLinkText` → `appendWikiLinkPropertyRow` → `storedApi().updateTextField(id, "edit content", composed)`.

**Wiki link text builder** (`buildWikiLinkText.ts` lines 1–28) — do not hand-roll:
```typescript
buildWikiLinkText(target, { notebookId: sourceNotebookId })
```

---

### `frontend/src/components/links/AddRelationshipFinalize.vue` (reuse unchanged)

**Props contract** (lines 56–62):
```typescript
note: Note          // source = reviewed note
targetSearchResult: NoteSearchResult  // preselected matched
```

**Success emit** (line 158): `emit("success")` after `createRootNoteAtNotebook`.  
Wire `@success` → PopButton `closer()` / `@closeDialog`. Do not fork finalize logic.

---

### Tests

#### `MatchedNoteLinkOffer.spec.ts` (NEW)

**Analog:** `frontend/tests/links/AddRelationship.spec.ts`

**Mount + seed storage pattern** (lines 73–79):
```typescript
const renderer = helper.component(Host).withCleanStorage()
if (seedRealm) {
  useStorageAccessor().value.refreshNoteRealm(seedRealm)
}
return renderer
  .withProps({ note, targetSearchResult })
  .mount({ attachTo: document.body })
```

**Mock creation** (lines 89–110): `mockSdkService(NotebookController, "createNoteAtNotebookRoot", …)`  
**Property write mock:** `mockSdkService(TextContentController, "updateNoteContent", …)`  
Assert API not called until user clicks choice / selects relation type (never-auto-write).

**Fixture:** `makeMe.aNoteSearchResult` for preselected target.

#### `AnsweredSpellingQuestion.spec.ts` (extend)

**Analog:** self (lines 18–38, 41–89)

- Keep `NoteShow` stub for section-structure tests; for CTA/gate tests, seed realms via `refreshNoteRealm` and provide `currentUser`.
- Assert `data-testid="link-to-matched-note-{id}"` count === `matchedNotes.length` when writable.
- Assert CTA absent when `notebookRealm.readonly === true`.

#### E2E

**Analog page object:** `AnsweredQuestionPage.expectAccidentalMatchReveal` (lines 25–52) — extend return with link helpers.  
**Analog dialog clicks:** `noteTargetSearchDialog.createRelationshipToTargetAs` (lines 152–156):
```typescript
cy.findByRole('button', { name: 'Add a new relationship note' }).click()
form.getField('Relation Type').clickOption(relationType)
```

**Feature file:** extend `e2e_test/features/recall/accidental_match_reveal.feature` (capability-named; `@wip` until green).  
**CTA selector:** `cy.findByTestId('link-to-matched-note-…')` then button names from `LinkInsertionChoice`.

---

## Shared Patterns

### Authentication / write gate (UX only)
**Source:** `frontend/src/components/notes/NoteShow.vue` lines 130–132; NoteToolbar `v-if="!readonly"`  
**Apply to:** Per-row CTA in `AnsweredSpellingQuestion`  
```typescript
!currentUser?.value || noteRealm.notebookRealm.readonly === true
```
Server still enforces on `updateNoteContent` / `createNoteAtNotebookRoot`.

### NoteStorage load-when-needed
**Source:** `frontend/src/store/StoredApiCollection.ts` lines 229–233  
**Apply to:** Source `Note` + target `NoteSearchResult` construction in wrapper / CTA gate  
```typescript
storageAccessor.value.storedApi().getNoteRealmRefAndLoadWhenNeeded(noteId)
```
Do not expand `AnsweredQuestion.matchedNotes` wire contract for `notebookId`.

### Direct content write (no cursor)
**Source:** `SearchForm.vue` `onLinkDeadLinkToNote` → `updateTextField(…, "edit content", …)`  
**Apply to:** Recall "Add wiki link as a new property" path only  
**Anti-pattern:** `useContentCursorInserter` / `insertWikiLinkAsProperty` on recall page (module singleton; multiple NoteShows).

### Dialog host
**Source:** `PopButton.vue` + NoteToolbar usage  
**Apply to:** Each matched-note CTA  
```vue
<PopButton btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm" …>
  <template #default="{ closer }">
    <MatchedNoteLinkOffer @close-dialog="closer" />
  </template>
</PopButton>
```

### Link-type choice + relationship finalize
**Source:** `LinkInsertionChoice.vue` + `AddRelationshipFinalize.vue`  
**Apply to:** Wrapper interior; hide bare wiki via new prop; never auto-submit.

### Wiki markdown + property rows
**Source:** `buildWikiLinkText.ts` + `noteContentFrontmatter` / `noteContentPropertyRows.ts`  
**Apply to:** Property-write composition; extract shared `appendWikiLinkPropertyRow`.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 4 files map to existing analogs; no greenfield persistence or new API surface. |

**Planner note:** Closest gap is D-07 vs `AddRelationshipFinalize`'s post-create `router` navigation — not "no analog," but verify whether staying on the recall page requires a prop/callback on finalize or accepting navigate-away after relationship create. Property path (direct `updateTextField`) already stays on page via `closeDialog` only.

## Metadata

**Analog search scope:** `frontend/src/components/{links,recall,notes,commons}`, `frontend/src/store`, `frontend/src/utils`, `frontend/tests/{links,components/recall}`, `e2e_test/{features/recall,start/pageObjects}`  
**Files scanned:** ~25 primary + test support  
**Pattern extraction date:** 2026-07-24

## PATTERNS COMPLETE
