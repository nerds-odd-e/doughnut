# Phase 9: Build a link from resolve dialog - Pattern Map

**Mapped:** 2026-08-05
**Files analyzed:** 9
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | component | request-response | itself + pre–Phase 7 parent of `MatchedNoteLinkOffer` (git `375a5d2589^`) | exact (prop pass only) |
| `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` | component | request-response | itself (list host) + `MatchedNoteLinkOffer.vue` (offer contract) + `BookReadingPage.vue` (kind `v-if` swap) | role-match (step host is new) |
| `frontend/src/components/recall/AccidentalMatchResolveRow.vue` | component | request-response | itself (Phase 8 row) + Resolve/try-again CTA chrome in `AnsweredSpellingQuestion.vue` | exact |
| `frontend/src/components/recall/MatchedNoteLinkOffer.vue` | component | CRUD | itself — **reuse as-is** | exact |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | test | request-response | current Phase 8 dialog tests + git `2ce5b39380` gate/link cases | exact (adapt selectors) |
| `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` | test utility | transform | itself — already has `currentUser` / `seedRealms` | exact |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | test (page object) | request-response | itself — `expectAccidentalMatchReveal` Resolve→dialog path; rewrite stale `openLinkToMatchedNote` | exact |
| `e2e_test/features/recall/accidental_match_reveal.feature` | test (e2e) | request-response | itself — untag `@wip` only | exact |
| `e2e_test/step_definitions/recall.ts` | test (steps) | request-response | itself — **prefer no Gherkin/step rewrite** | exact |

## Pattern Assignments

### `frontend/src/components/recall/AnsweredSpellingQuestion.vue` (component, request-response)

**Analog:** Current host (lines 9–18) + pre–Phase 7 `reviewedNoteId` derived from recalled topology.

**Keep single PopButton; pass reviewed id only** (lines 9–18):
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
      :reviewed-note-id="answeredQuestion.recalledNote.noteTopology.id"
    />
  </PopButton>
```

**Do not** reintroduce nested `PopButton` + `@close-dialog="closer"` around the offer (anti-pattern from git `375a5d2589^`):
```vue
<!-- FORBIDDEN in Phase 9 — nested Modal / closer dismisses resolve -->
<PopButton v-if="canOfferLinkToMatched(matched.id)" …>
  <template #default="{ closer }">
    <MatchedNoteLinkOffer … @close-dialog="closer" />
  </template>
</PopButton>
```

**Outcome gate stays here** (lines 68–72) — Resolve CTA only for `ACCIDENTAL_MATCH` + non-empty matches; leave OVERLAP try-again alone (lines 30–40).

---

### `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` (component, request-response)

**Analog (list shell):** itself today (lines 1–24).  
**Analog (offer mount):** `MatchedNoteLinkOffer.vue` (lines 1–18, 31–38).  
**Analog (kind-based view swap):** `frontend/src/pages/BookReadingPage.vue` (lines 26–39) — `v-else-if="bootstrap?.kind === 'epub'|'pdf'"`.  
**Analog (AMR-07 gate):** pre–Phase 7 `canOfferLinkToMatched` (git `375a5d2589^`).

**Current list-only host** (copy structure, then wrap with step):
```1:24:frontend/src/components/recall/AccidentalMatchResolveDialog.vue
<template>
  <ul
    class="flex flex-col gap-2"
    data-testid="accidental-match-resolve-dialog"
  >
    <AccidentalMatchResolveRow
      v-for="matched in matchedNotes"
      :key="matched.id"
      :matched="matched"
    />
  </ul>
</template>
```

**Step state shape** (from RESEARCH / UI-SPEC — discretionary enum):
```typescript
type ResolveStep =
  | { kind: "list" }
  | { kind: "link"; matchedNoteId: number }

const step = ref<ResolveStep>({ kind: "list" })

function openLinkOffer(matchedNoteId: number) {
  step.value = { kind: "link", matchedNoteId }
}

function returnToList() {
  step.value = { kind: "list" }
}
```

**Template swap** (never nest PopButton):
```vue
<ul
  v-if="step.kind === 'list'"
  class="flex flex-col gap-2"
  data-testid="accidental-match-resolve-dialog"
>
  <AccidentalMatchResolveRow
    v-for="matched in matchedNotes"
    :key="matched.id"
    :matched="matched"
    :can-build-link="canOfferBuildLink(matched.id)"
    @build-link="openLinkOffer(matched.id)"
  />
</ul>
<MatchedNoteLinkOffer
  v-else
  :reviewed-note-id="reviewedNoteId"
  :matched-note-id="step.matchedNoteId"
  @close-dialog="returnToList"
/>
```

**Gate to port** (host-level reviewed hydrate preferred):
```typescript
import { computed, inject, type Ref } from "vue"
import type { User } from "@generated/doughnut-backend-api"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const currentUser = inject<Ref<User | undefined>>("currentUser")
const storageAccessor = useStorageAccessor()

const reviewedRealm = computed(
  () =>
    storageAccessor.value
      .storedApi()
      .getNoteRealmRefAndLoadWhenNeeded(props.reviewedNoteId).value
)

function canOfferBuildLink(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}
```

**Critical wiring:** `@close-dialog="returnToList"` — **not** PopButton `closer`. Offer already emits `closeDialog` on go-back / property success / relationship success (`MatchedNoteLinkOffer.vue` lines 9, 16, 77–78).

---

### `frontend/src/components/recall/AccidentalMatchResolveRow.vue` (component, request-response)

**Analog:** itself (Phase 8 title + path) + button chrome from Resolve CTA / try-again.

**Existing row stack** (append CTA after path):
```1:17:frontend/src/components/recall/AccidentalMatchResolveRow.vue
<template>
  <li
    class="flex flex-col gap-2"
    :data-testid="`resolve-match-row-${matched.id}`"
  >
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
</template>
```

**Build a link CTA** (hide when gated; preserve testid family):
```vue
<button
  v-if="canBuildLink"
  type="button"
  class="daisy-btn daisy-btn-secondary daisy-btn-sm"
  :data-testid="`link-to-matched-note-${matched.id}`"
  title="Build a link"
  aria-label="Build a link"
  @click="$emit('buildLink')"
>
  Build a link
</button>
```

**Props/emits pattern** — mirror `matched` PropType style already in the row (lines 26–31); add `canBuildLink: Boolean` and `defineEmits<{ buildLink: [] }>()`. Keep per-row match hydrate via `getNoteRealmRefAndLoadWhenNeeded` (lines 33–36) — do not move match hydrate solely for the CTA.

**Button chrome analog** (Resolve CTA classes, without `mt-2` on the row button):
```9:14:frontend/src/components/recall/AnsweredSpellingQuestion.vue
  <PopButton
    v-if="showResolveAccidentalMatchCta"
    title="Resolve accidental match"
    aria-label="Resolve accidental match"
    btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
    data-testid="resolve-accidental-match"
```

---

### `frontend/src/components/recall/MatchedNoteLinkOffer.vue` (component, CRUD) — REUSE

**Analog:** itself. Do not rewrite the offer pipeline.

**Stay-on-result + closeDialog contract** (lines 1–18):
```1:18:frontend/src/components/recall/MatchedNoteLinkOffer.vue
  <LinkInsertionChoice
    v-if="selectedSearchResult && sourceNote && !targetSearchResult"
    :target-note-topology="selectedSearchResult.noteTopology"
    :bare-wiki-link-available="false"
    :wiki-property-option-available="wikiPropertyOptionAvailable"
    @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
    @choose-add-relationship="chooseAddRelationship"
    @go-back="$emit('closeDialog')"
  />
  <AddRelationshipFinalize
    v-if="targetSearchResult && sourceNote"
    :note="sourceNote"
    :target-search-result="targetSearchResult"
    :navigate-on-success="false"
    @success="$emit('closeDialog')"
    @go-back="targetSearchResult = undefined"
  />
```

**Props** (lines 31–34): `reviewedNoteId` + `matchedNoteId`. Host supplies both after step swap.

**Realm hydrate inside offer** (lines 42–47) already loads reviewed + matched — host gate still needs early hydrate for CTA visibility before the offer mounts.

---

### `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` (test)

**Analog (dialog open / body query):** current Phase 8 tests (lines 49–90).  
**Analog (gates + link):** git `2ce5b39380` — adapt so CTAs live inside opened resolve dialog (`document.body` after Resolve click).

**Open dialog then query body** (current pattern):
```60:68:frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts
    await wrapper
      .find('[data-testid="resolve-accidental-match"]')
      .trigger("click")
    await flushPromises()

    const dialog = document.body.querySelector(
      '[data-testid="accidental-match-resolve-dialog"]'
    )
```

**Gate / CTA cases to restore** (adapt from `2ce5b39380`; open Resolve first; query `document.body`):

```typescript
// Writable + seeded → two Build a link CTAs
const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
  accidentalMatchWithTwoMatchedNotes()
wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
  currentUser: makeMe.aUser.please(),
  seedRealms: [reviewedRealm, matchedA, matchedB],
})
await flushPromises()
await wrapper.find('[data-testid="resolve-accidental-match"]').trigger("click")
await flushPromises()
expect(
  document.body.querySelectorAll('[data-testid^="link-to-matched-note-"]')
).toHaveLength(2)

// Readonly → zero CTAs (titles/path may still show)
reviewedRealm.notebookRealm.readonly = true
// …mount with seedRealms + currentUser, open Resolve…
expect(
  document.body.querySelectorAll('[data-testid^="link-to-matched-note-"]')
).toHaveLength(0)

// Never-settling showNote + no seedRealms → zero CTAs
mockSdkServiceWithImplementation(NoteController, "showNote", () => new Promise(() => {}))
```

**Step-in-same-Modal / stay-on-result deltas** (new for Phase 9):
- Click `link-to-matched-note-10` → body contains `Link to:` + matched title; still one resolve Modal; list testid may be absent while on offer step; no nested link PopButton.
- After property success (mock `TextContentController.updateNoteContent`) → list/`accidental-match-resolve-dialog` visible again; wrapper still has `accidental-match-alert`.

**Mount helpers** — reuse as-is:
```29:61:frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts
export function mountAnsweredSpellingQuestion(
  answeredQuestion: AnsweredQuestion,
  options: {
    currentUser?: User
    seedRealms?: NoteRealm[]
    withRouter?: boolean
  } = {}
) {
  // … withCurrentUser + refreshNoteRealm(seedRealms) …
}
```

**Offer pipeline unit coverage** — keep `MatchedNoteLinkOffer.spec.ts` (property write + `closeDialog` emit, lines 125–156); drive host-level stay-in-dialog from the accidental-match boundary spec.

---

### `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` (page object)

**Analog (good path):** `expectAccidentalMatchReveal` Resolve → dialog (lines 84–92).  
**Stale path to replace:** `openLinkToMatchedNote` / `expectStillOnAccidentalMatchResult` still use `matched-notes-section` (lines 32–42, 102–146).

**Copy Resolve→dialog open from** (lines 84–92):
```typescript
cy.findByTestId('resolve-accidental-match').click()
waitUntilAppIsNotBusy()
cy.findByTestId('accidental-match-resolve-dialog')
  .should('be.visible')
  .and('contain.text', matchedNoteTitle)
```

**Rewrite `openLinkToMatchedNote`** (page-object only; keep Gherkin):
```typescript
openLinkToMatchedNote(matchedNoteTitle: string) {
  cy.findByTestId('resolve-accidental-match').click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('contain.text', matchedNoteTitle)
  cy.findByTestId(/^link-to-matched-note-/)
    .should('be.visible')
    .and('contain.text', 'Build a link')
    .click()
  cy.contains('Link to:')
    .should('be.visible')
    .parent()
    .should('contain.text', matchedNoteTitle)
  cy.findByPlaceholderText('Search').should('not.exist')
  // …
  return self
}
```

**Rewrite `expectStillOnAccidentalMatchResult`** — assert alert + recall URL (+ Resolve CTA / optional reopen dialog for match title); **do not** call `expectMatchedNoteInSection`.

**Steps stay thin** (`e2e_test/step_definitions/recall.ts` lines 173–197) — they already delegate to page object methods.

---

### `e2e_test/features/recall/accidental_match_reveal.feature` (e2e)

**Analog:** itself. Untag `@wip` on the two link scenarios (lines 24–44) only after page object + product are green. Do not rewrite scenario text (D-09).

## Shared Patterns

### Authentication / currentUser inject
**Source:** pre–Phase 7 gate + `NoteShow.vue` lines 130–132  
**Apply to:** `AccidentalMatchResolveDialog` AMR-07 gate
```typescript
const currentUser = inject<Ref<User | undefined>>("currentUser")
// Gate fails if !currentUser?.value
```

### Readonly hide (prefer omit, not disabled)
**Source:** `NoteShow.vue` lines 130–132; historical `canOfferLinkToMatched`  
**Apply to:** Build a link CTA visibility
```typescript
!currentUser?.value || noteRealm.notebookRealm.readonly === true
```

### Single Modal via PopButton (do not nest)
**Source:** `PopButton.vue` lines 16–24 — one `Modal` with `#default="{ closer }"`  
**Apply to:** Resolve host only. Phase 9 must **not** pass `closer` into offer `@closeDialog`.
```vue
<Modal v-if="show" @close_request="closeDialog">
  <template #body>
    <slot name="default" :closer="closeDialog" />
  </template>
</Modal>
```

### Realm hydrate
**Source:** `AccidentalMatchResolveRow.vue` lines 33–36; `MatchedNoteLinkOffer.vue` lines 42–47  
**Apply to:** Host-level reviewed hydrate once; keep per-row match hydrate; gate uses `.value` presence
```typescript
storageAccessor.value
  .storedApi()
  .getNoteRealmRefAndLoadWhenNeeded(noteId)
```

### Stay-on-result after link write
**Source:** `MatchedNoteLinkOffer.vue` `:navigate-on-success="false"` + `closeDialog` emit  
**Apply to:** Dialog host maps emit → `returnToList`; result chrome (`accidental-match-alert`) remains mounted under `AnsweredSpellingQuestion`

### Vitest boundary + makeMe
**Source:** `answeredSpellingQuestionTestSupport.ts` + `unit-testing.mdc` / `frontend-testing.mdc`  
**Apply to:** Extend accidental-match spec; use `mockSdkService` / `mockSdkServiceWithImplementation` for HTTP only; query `data-testid` on `document.body` for Modal content

## Anti-Patterns (explicit)

| Anti-pattern | Wrong analog | Correct pattern |
|--------------|--------------|-----------------|
| Nested `PopButton` around offer | git `375a5d2589^` stacked section | Step swap in `AccidentalMatchResolveDialog` |
| `@close-dialog="closer"` on offer | `NoteToolbar.vue` / `GlobalBar.vue` SearchForm host | `@close-dialog="returnToList"` |
| Revive `matched-notes-section` for E2E stay | stale `AnsweredQuestionPage` helpers | Alert + Resolve CTA (+ optional dialog reopen) |
| Disabled-looking Build a link | — | `v-if` omit (D-06) |
| New OpenAPI / capability API for gates | — | Client realm + `readonly` only (D-07) |

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | No gaps. Step-state host is new composition but composed from existing Modal list + `MatchedNoteLinkOffer` + kind `v-if` swap analogs. |

## Metadata

**Analog search scope:** `frontend/src/components/recall/`, `frontend/src/components/commons/Popups/`, `frontend/src/components/notes/`, `frontend/src/pages/BookReadingPage.vue`, `frontend/tests/components/recall/`, `e2e_test/start/pageObjects/`, `e2e_test/features/recall/`, `e2e_test/step_definitions/`, git history `375a5d2589^` / `2ce5b39380`  
**Files scanned:** ~25 primary + historical snapshots  
**Pattern extraction date:** 2026-08-05
