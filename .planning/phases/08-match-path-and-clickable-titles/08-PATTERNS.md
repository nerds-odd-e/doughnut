# Phase 8: Match path and clickable titles - Pattern Map

**Mapped:** 2026-08-05
**Files analyzed:** 6
**Analogs found:** 6 / 6

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/AccidentalMatchResolveRow.vue` | component | request-response | `frontend/src/components/recall/MatchedNoteLinkOffer.vue` (hydrate) + `NoteTitleWithLink` + `BreadcrumbWithCircle` | exact (compose) |
| `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` | component | request-response | itself (Phase 7 list host) → row extract like list→item split | exact |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | test | request-response | same file (extend dialog-open case) + `MatchedNoteLinkOffer.spec.ts` notebook mutate | exact |
| `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` | utility | transform | itself (`seedRealms`) + `sidebarTestSupport.mockShowNoteForRealms` | role-match |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | test | request-response | itself `expectAccidentalMatchReveal` dialog block | exact |
| `e2e_test/features/recall/accidental_match_reveal.feature` | test | request-response | itself (optional wording only) | exact |

## Pattern Assignments

### `frontend/src/components/recall/AccidentalMatchResolveRow.vue` (component, request-response)

**Analog (hydrate):** `frontend/src/components/recall/MatchedNoteLinkOffer.vue`  
**Analog (title link):** `frontend/src/components/notes/NoteTitleWithLink.vue`  
**Analog (breadcrumb props):** `frontend/src/components/toolbars/BreadcrumbWithCircle.vue` + `NoteShow.vue` binding  
**Analog (title-first + secondary identity layout):** `frontend/src/components/search/SearchResultListItem.vue` (title then meta under — but use `BreadcrumbWithCircle`, not plain notebook string)

**Imports / hydrate pattern** (`MatchedNoteLinkOffer.vue` lines 26–47):
```typescript
import { useStorageAccessor } from "@/composables/useStorageAccessor"
// ...
const storageAccessor = useStorageAccessor()

const reviewedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.reviewedNoteId)
const matchedRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matchedNoteId)
```

For the row: one call with `props.matched.id` (same API; lines verified in `StoredApiCollection.ts` 230–234):
```typescript
getNoteRealmRefAndLoadWhenNeeded(noteId: Doughnut.ID) {
  const result = this.storage.refOfNoteRealm(noteId)
  if (!result.value) this.loadNote(noteId)
  return result
}
```

**Title navigation pattern** (`NoteTitleWithLink.vue` lines 1–18):
```vue
<template>
  <router-link
    :to="noteShowLocation(noteTopology.id)"
    class="no-underline"
  >
    <NoteTitleComponent v-bind="{ noteTopology }" />
  </router-link>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import NoteTitleComponent from "./core/NoteTitleComponent.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
})
</script>
```

Row should pass `:note-topology="matched"` — do **not** reimplement `router-link` / `noteShowLocation`.

**Breadcrumb props pattern** (`BreadcrumbWithCircle.vue` lines 45–54):
```typescript
defineProps({
  ancestorFolders: {
    type: Array as PropType<Folder[]>,
    default: () => [],
  },
  notebookRealm: {
    type: Object as PropType<NotebookRealm>,
    required: true,
  },
})
```

**Binding when realm present** (`NoteShow.vue` lines 8–14 — copy props only; do **not** mount `NoteShow`):
```vue
<BreadcrumbWithCircle
  v-bind="{
    ancestorFolders,
    notebookRealm: noteRealm.notebookRealm,
  }"
/>
```

Row equivalent:
```vue
<BreadcrumbWithCircle
  :ancestor-folders="matchRealmRef.ancestorFolders ?? []"
  :notebook-realm="matchRealmRef.notebookRealm"
/>
```

**Title-first layout sketch** (`SearchResultListItem.vue` lines 16–55 — structure only):
```vue
<div class="min-w-0 flex-1">
  <div class="search-result-item-title">
    <NoteTitleWithLink :note-topology="..." />
  </div>
  <div v-if="..." class="search-hit-meta ...">
    <!-- Phase 8: BreadcrumbWithCircle here, not plain notebook-name-label -->
  </div>
</div>
```

**Progressive path (D-08/D-09):** always render `NoteTitleWithLink` from `NoteTopology`; wrap breadcrumb in `v-if="matchRealmRef"` (ref may be `undefined` until hydrate). No list-level Suspense / spinner.

**Hydrate timing:** no explicit `onMounted` prefetch — dialog body mounts only when `PopButton` opens (`PopButton.vue` lines 16–24: `Modal` is `v-if="show"`).

**Recommended shape** (from RESEARCH; testids discretionary):
```vue
<li :data-testid="`resolve-match-row-${matched.id}`">
  <NoteTitleWithLink :note-topology="matched" />
  <div
    v-if="matchRealmRef"
    :data-testid="`resolve-match-path-${matched.id}`"
  >
    <BreadcrumbWithCircle
      :ancestor-folders="matchRealmRef.ancestorFolders ?? []"
      :notebook-realm="matchRealmRef.notebookRealm"
    />
  </div>
</li>
```

**Anti-patterns:** do not import `NoteShow` / `NoteTextContent`; do not add nested `PopButton` (Phase 9 actions).

---

### `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` (component, request-response)

**Analog:** current file (Phase 7 titles-only list host)

**Current list host** (lines 1–26):
```vue
<template>
  <ul
    class="flex flex-col gap-2"
    data-testid="accidental-match-resolve-dialog"
  >
    <li
      v-for="matched in matchedNotes"
      :key="matched.id"
      :data-testid="`resolve-match-row-${matched.id}`"
    >
      {{ matched.title }}
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"

defineProps({
  matchedNotes: {
    type: Array as PropType<NoteTopology[]>,
    required: true,
  },
})
</script>
```

**Core change pattern:** keep `ul` + `data-testid="accidental-match-resolve-dialog"` + `matchedNotes` prop; replace inline `<li>{{ matched.title }}</li>` with:
```vue
<AccidentalMatchResolveRow
  v-for="matched in matchedNotes"
  :key="matched.id"
  :matched="matched"
/>
```
Move `resolve-match-row-*` testid onto the row component’s root `<li>` (already recommended above). Dialog stays a thin list host — no hydrate logic here.

**Parent unchanged** (`AnsweredSpellingQuestion.vue` lines 9–18):
```vue
<PopButton
  v-if="showResolveAccidentalMatchCta"
  title="Resolve accidental match"
  aria-label="Resolve accidental match"
  btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
  data-testid="resolve-accidental-match"
>
  <AccidentalMatchResolveDialog
    :matched-notes="answeredQuestion.matchedNotes ?? []"
  />
</PopButton>
```

---

### `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` (test, request-response)

**Analog:** same file — extend “opens resolve dialog…” case; keep driving `AnsweredSpellingQuestion` boundary (small-test style).  
**Fixture notebook mutate analog:** `MatchedNoteLinkOffer.spec.ts` lines 28–45.

**Existing dialog-open case** (lines 49–73) — evolve into path + link asserts:
```typescript
it("opens resolve dialog listing matched note titles only", async () => {
  const { answeredQuestion } = accidentalMatchWithTwoMatchedNotes()
  wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
    withRouter: true,
  })
  await flushPromises()

  await wrapper
    .find('[data-testid="resolve-accidental-match"]')
    .trigger("click")
  await flushPromises()

  const dialog = document.body.querySelector(
    '[data-testid="accidental-match-resolve-dialog"]'
  )
  expect(dialog).toBeTruthy()
  expect(
    document.body.querySelector('[data-testid="resolve-match-row-10"]')
      ?.textContent
  ).toContain("Matched A")
  // ...
})
```

**Recommended Phase 8 delta** (capability-named; focused asserts):
```typescript
// After open with seedRealms that have distinct notebook names:
const row = document.body.querySelector('[data-testid="resolve-match-row-10"]')
expect(row?.querySelector("a")?.getAttribute("href")).toMatch(/10/)
expect(
  document.body.querySelector('[data-testid="resolve-match-path-10"]')
    ?.textContent
).toContain("Notebook Alpha")
// Still no peek in dialog — reviewed NoteShow stub may exist outside dialog
```

**Notebook name mutation** (`MatchedNoteLinkOffer.spec.ts` lines 41–44):
```typescript
matchedRealm.notebookRealm.notebook.id =
  reviewedRealm.notebookRealm.notebook.id
matchedRealm.notebookRealm.notebook.name =
  reviewedRealm.notebookRealm.notebook.name
```

For Phase 8 path disambiguation, set **distinct** names after `please()` (builder defaults notebook name to note title):
```typescript
matchedA.notebookRealm.notebook.name = "Notebook Alpha"
matchedB.notebookRealm.notebook.name = "Notebook Beta"
```

**Mount with seeded realms** (test support already supports this):
```typescript
wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
  withRouter: true,
  seedRealms: [matchedA, matchedB],
})
```

Keep canonical “title + path + link” in **one** test; siblings (empty matchedNotes / dismiss / compact CTA) only assert deltas. Prefer not adding a dedicated `AccidentalMatchResolveRow.spec.ts` unless mounting the row alone is clearer.

**Generic `showNote` mock pitfall:** `beforeEach` currently uses `mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())`. Prefer `seedRealms` so hydrate hits cache; if async load still fires, use keyed mock (see Shared Patterns).

---

### `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` (utility, transform)

**Analog:** itself — `seedRealms` + `accidentalMatchWithTwoMatchedNotes`  
**Optional keyed-mock analog:** `frontend/tests/notes/sidebar/sidebarTestSupport.ts` `mockShowNoteForRealms`

**Existing seed pattern** (lines 47–50):
```typescript
if (options.seedRealms) {
  for (const realm of options.seedRealms) {
    useStorageAccessor().value.refreshNoteRealm(realm)
  }
}
```

**Existing fixture factory** (lines 64–76):
```typescript
export function accidentalMatchWithTwoMatchedNotes() {
  const reviewedRealm = makeMe.aNoteRealm.title("Reviewed Note").please()
  const matchedA = makeMe.aNoteRealm.id(10).title("Matched A").please()
  const matchedB = makeMe.aNoteRealm.id(20).title("Matched B").please()
  const answeredQuestion = makeMe.anAnsweredQuestion
    .withNote(reviewedRealm.note)
    .accidentalMatch("matched a", [
      matchedA.note.noteTopology,
      matchedB.note.noteTopology,
    ])
    .please()
  return { answeredQuestion, reviewedRealm, matchedA, matchedB }
}
```

**Recommended extension:** helper that returns realms with distinct `notebookRealm.notebook.name` (mutate after `please()`), or document that callers mutate before `seedRealms`. Prefer concise helper in this file over extending `NoteRealmBuilder` unless mutation repeats awkwardly (RESEARCH Open Question 1).

**Router:** keep `withRouter: true` for title-link href asserts (already optional on mount).

---

### `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` (test, request-response)

**Analog:** same file — `expectAccidentalMatchReveal` dialog block (lines 84–88)

**Current dialog assert:**
```typescript
cy.findByTestId('resolve-accidental-match').click()
cy.findByTestId('accidental-match-resolve-dialog')
  .should('be.visible')
  .and('contain.text', matchedNoteTitle)
cy.get('.close-button').filter(':visible').first().click()
cy.findByTestId('accidental-match-resolve-dialog').should('not.exist')
```

**Phase 8 extension** (inside dialog, before close — no navigate-and-reopen):
```typescript
cy.findByTestId('accidental-match-resolve-dialog')
  .should('be.visible')
  .and('contain.text', matchedNoteTitle)
  .and('contain.text', 'English practice') // notebook path identity
  .within(() => {
    cy.contains('a', matchedNoteTitle).should('be.visible')
    // assert href toward noteShow if stable; do not click through (Phase 12)
  })
```

Fixture uses one notebook `"English practice"` (`accidental_match_reveal.feature` Background) — assert that name; do **not** require a second notebook for Phase 8.

Keep link-offer helpers (`openLinkToMatchedNote`, etc.) untouched — they still target legacy `matched-notes-section` / `@wip` Phase 9 scenarios.

---

### `e2e_test/features/recall/accidental_match_reveal.feature` (test, request-response)

**Analog:** itself — prefer page-object change over feature rewrite.

**Current success scenario** (lines 16–22) stays; optional Feature description update from “listing matched note titles” → titles + path. Do **not** add navigate-away-and-reopen scenario (Phase 12). Leave `@wip` link-offer scenarios alone.

Step definition already delegates to page object (`e2e_test/step_definitions/recall.ts` ~165–169) — no new step required if asserts live in `expectAccidentalMatchReveal`.

## Shared Patterns

### Realm hydrate via StoredApi
**Source:** `MatchedNoteLinkOffer.vue` + `StoredApiCollection.getNoteRealmRefAndLoadWhenNeeded`  
**Apply to:** `AccidentalMatchResolveRow.vue`  
```typescript
const matchRealmRef = storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(props.matched.id)
```

### Clickable title
**Source:** `NoteTitleWithLink.vue`  
**Apply to:** every resolve row  
Reuse component; Modal already closes on `route.fullPath` change — do not prevent navigation.

### Breadcrumb path chrome
**Source:** `BreadcrumbWithCircle.vue`  
**Apply to:** path under title when realm present  
Pass `notebookRealm` + `ancestorFolders`; accept default clickable folder/notebook segments.

### Modal open = hydrate start
**Source:** `PopButton.vue` lines 16–24 (`v-if="show"` on Modal)  
**Apply to:** timing decisions — do not prefetch realms on accidental-match result mount.

### Vitest seedRealms / keyed showNote
**Source:** `answeredSpellingQuestionTestSupport.ts` `seedRealms`; fallback `sidebarTestSupport.mockShowNoteForRealms`  
**Apply to:** path-asserting unit tests  
```typescript
// sidebarTestSupport.ts lines 99–112
export function mockShowNoteForRealms(realms: NoteRealm[]) {
  const byId = Object.fromEntries(realms.map((r) => [r.id, r])) as Record<
    number,
    NoteRealm
  >
  mockSdkServiceWithImplementation(NoteController, "showNote", (options) => {
    const id = (options as Options<ShowNoteData>).path.note
    const realm = byId[id]
    expect(
      realm,
      `sidebar tests: unmocked showNote for note id ${id}`
    ).toBeDefined()
    return realm!
  })
}
```

### Small-test boundary
**Source:** `unit-testing.mdc` + existing accidental-match spec  
**Apply to:** Vitest — drive `AnsweredSpellingQuestion` / dialog; fixture via `makeMe`; focused asserts; mock only SDK.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 8 files have in-repo analogs |

## Metadata

**Analog search scope:** `frontend/src/components/recall/`, `frontend/src/components/notes/`, `frontend/src/components/toolbars/`, `frontend/src/components/search/`, `frontend/src/store/StoredApiCollection.ts`, `frontend/tests/components/recall/`, `frontend/tests/notes/sidebar/`, `frontend/tests/toolbars/`, `e2e_test/start/pageObjects/`, `e2e_test/features/recall/`  
**Files scanned:** ~25 (targeted reads + greps)  
**Pattern extraction date:** 2026-08-05
