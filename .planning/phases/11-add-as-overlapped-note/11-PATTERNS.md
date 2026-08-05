# Phase 11: Add as overlapped note - Pattern Map

**Mapped:** 2026-08-05
**Files analyzed:** 8 (modify/extend) + 4 reuse-only seams
**Analogs found:** 8 / 8

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` | component | request-response + CRUD | itself (Phase 9 gate + list host) + `MatchedNoteLinkOffer.vue` compose→`updateTextField` | exact (gate) / role-match (persist; stay on list) |
| `frontend/src/components/recall/AccidentalMatchResolveRow.vue` | component | request-response | itself — Build a link CTA (gated `daisy-btn-sm` + emit) | exact |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | test | request-response | itself Phase 9 Build-a-link / AMR-07 cases + `MatchedNoteLinkOffer.spec.ts` wiki-link spy | exact |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | test (page object) | request-response | itself — `openLinkToMatchedNote` + `expectStillOnAccidentalMatchResult` | exact |
| `e2e_test/features/recall/accidental_match_reveal.feature` | test (e2e) | request-response | itself — wiki-property stay-on-result scenario | exact |
| `e2e_test/step_definitions/recall.ts` | test (steps) | request-response | itself — link-as-wiki-property When → page object | exact |
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | component | request-response | itself — **do not modify**; negative chrome asserts only | exact (leave alone) |
| `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` | utility | transform | itself — **reuse as-is** (Phase 10) | exact |

**Reuse unchanged (do not edit this phase):**

| File | Role | Why listed |
|------|------|------------|
| `frontend/src/components/recall/MatchedNoteLinkOffer.vue` | component | Compose → null/undefined guard → `updateTextField` shape |
| `frontend/src/store/StoredApiCollection.ts` | store / service | `updateTextField(noteId, "edit content", …)` persist seam |
| `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` | test utility | `mountAnsweredSpellingQuestion` / `seedRealms` / `openResolveAccidentalMatch` |
| `e2e_test/features/recall/overlap_try_again.feature` | test (e2e) | Must stay green / uncoupled — no declare wiring |

## Pattern Assignments

### `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` (component, request-response + CRUD)

**Analog (gate + list host):** itself today (lines 1–70).  
**Analog (compose + persist):** `MatchedNoteLinkOffer.vue` lines 83–96.  
**Analog (util call):** `appendOverlapWikiLinkToNoteContent.ts` lines 4–15.

**Imports pattern** — add util; keep existing host imports:
```typescript
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { computed, inject, ref, type PropType, type Ref } from "vue"
import type { NoteTopology, User } from "@generated/doughnut-backend-api"
import AccidentalMatchResolveRow from "@/components/recall/AccidentalMatchResolveRow.vue"
import MatchedNoteLinkOffer from "@/components/recall/MatchedNoteLinkOffer.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
```

**Shared mutating-action gate** — rename/generalize `canOfferBuildLink` (D-09); same body (lines 55–62):
```typescript
// Source: AccidentalMatchResolveDialog.vue:55-62 — generalize name, keep logic
function canOfferMutatingAction(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}
```

**Pass gate to both CTAs** (extend current template lines 7–13):
```vue
<AccidentalMatchResolveRow
  v-for="matched in matchedNotes"
  :key="matched.id"
  :matched="matched"
  :can-mutate="canOfferMutatingAction(matched.id)"
  @build-link="openLinkOffer(matched.id)"
  @add-as-overlapped="addAsOverlappedNote(matched.id)"
/>
```
*(Prop name discretion: `canMutate` / keep dual props both fed from same helper — prefer one prop so gates cannot drift.)*

**Core declare handler** — host-owned; **stay on list** (no `step` change, no `closeDialogThen`):
```typescript
// Shape from RESEARCH + MatchedNoteLinkOffer persist; differencing: null check + no dismiss
async function addAsOverlappedNote(matchedNoteId: number) {
  const reviewed = reviewedRealm.value
  const matched = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  if (!reviewed?.note || !matched) return

  const composed = appendOverlapWikiLinkToNoteContent(
    reviewed.note.content ?? "",
    {
      noteTopology: matched.note.noteTopology,
      notebookId: matched.notebookRealm.notebook.id,
      notebookName: matched.notebookRealm.notebook.name,
    },
    { notebookId: reviewed.notebookRealm.notebook.id }
  )
  if (composed === null) return // D-05 — silent; no updateTextField

  await storageAccessor.value
    .storedApi()
    .updateTextField(props.reviewedNoteId, "edit content", composed)
  // stay on list — do not set step; do not emit retry
}
```

**Persist seam reference** (`MatchedNoteLinkOffer.vue` 83–96) — copy async write; **omit** `closeDialogThen`:
```typescript
// Source: MatchedNoteLinkOffer.vue:83-96 — property path analog
const composed = appendWikiLinkPropertyRow(source.content ?? "", linkText)
if (composed === undefined) return
await closeDialogThen(() =>
  storageAccessor.value
    .storedApi()
    .updateTextField(source.id, "edit content", composed)
)
// Overlap: null-check is `=== null`; do NOT call closeDialogThen / change step
```

**Critical anti-patterns:**
- Do **not** call `appendAliasToNoteContent` with a bare title (Pitfall 5 / D-02).
- Do **not** emit `retry` or mutate `answer.outcome` (Pitfall 4 / D-06).
- Do **not** nest Modal/PopButton for declare (D-01) — list-row click only.
- Build-a-link still uses `openLinkOffer` → step swap; overlap does **not**.

---

### `frontend/src/components/recall/AccidentalMatchResolveRow.vue` (component, request-response)

**Analog:** itself — Build a link button (lines 16–26, 42–50).

**CTA chrome to copy** (locked copy; discretionary classes match Build a link):
```vue
<!-- Source pattern: AccidentalMatchResolveRow.vue:16-26 -->
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

**Add sibling CTA** (recommended testid from RESEARCH):
```vue
<button
  v-if="canMutate"
  type="button"
  class="daisy-btn daisy-btn-secondary daisy-btn-sm"
  :data-testid="`add-as-overlapped-note-${matched.id}`"
  title="Add as overlapped note"
  aria-label="Add as overlapped note"
  @click="$emit('addAsOverlapped')"
>
  Add as overlapped note
</button>
```

**Emits / props pattern** (extend lines 42–50):
```typescript
defineProps({
  matched: { type: Object as PropType<NoteTopology>, required: true },
  canMutate: { type: Boolean, default: false }, // shared AMR-07 gate from host
})

defineEmits<{
  (e: "buildLink"): void
  (e: "addAsOverlapped"): void
}>()
```

**Keep** title + path hydrate unchanged (`NoteTitleWithLink` / `BreadcrumbWithCircle` / `matchRealmRef`) — gates hide CTAs only (D-08).

---

### `frontend/src/components/recall/AnsweredSpellingQuestion.vue` (component — leave alone)

**Analog:** itself. Outcome-discriminated chrome must remain exclusive.

**Do not wire declare here** (D-03 / Pitfall 8). Host stays PopButton → dialog only (lines 9–20).

**Negative chrome gates** (assert after declare; do not change):
```typescript
// Source: AnsweredSpellingQuestion.vue:65-73, 31-41
const isOverlap = computed(
  () => props.answeredQuestion.answer.outcome === "OVERLAP"
)
const showResolveAccidentalMatchCta = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)
// try-again: v-if="isOverlap" data-testid="overlap-try-again" @click="emit('retry')"
```

---

### `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` (utility — reuse)

**Analog:** itself (Phase 10). Call site must match signature:

```typescript
// Source: appendOverlapWikiLinkToNoteContent.ts:4-15
export function appendOverlapWikiLinkToNoteContent(
  contentMarkdown: string,
  target: {
    noteTopology: { title: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number }
): string | null {
  const token = buildWikiLinkText(target, { notebookId: source.notebookId })
  return appendAliasToNoteContent(contentMarkdown, token)
}
```

Do **not** re-test util exhaustively at dialog boundary — assert wiring + wiki-link token in `updateNoteContent` payload.

---

### `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` (test, request-response)

**Analog (boundary + gates):** itself Phase 9 cases (lines 135–232).  
**Analog (wiki-link write spy):** `MatchedNoteLinkOffer.spec.ts` lines 125–155.  
**Analog (no try-again / retry):** `AnsweredSpellingQuestionOverlap.spec.ts` (positive OVERLAP); invert for ACCIDENTAL_MATCH after declare.  
**Support:** `answeredSpellingQuestionTestSupport.ts` — `mountAnsweredSpellingQuestion` + `seedRealms` + `openResolveAccidentalMatch`.

**Imports / setup pattern** (existing file lines 1–17, 22–28):
```typescript
import {
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  accidentalMatchWithTwoMatchedNotes,
  mountAnsweredSpellingQuestion,
  openResolveAccidentalMatch,
} from "./answeredSpellingQuestionTestSupport"
```

**Canonical success case** — mirror Build-a-link open + MatchedNoteLinkOffer spy; assert stay ACCIDENTAL_MATCH:
```typescript
it("adds as overlapped note via wiki-link content update without try-again", async () => {
  const { answeredQuestion, reviewedRealm, matchedA, matchedB } =
    accidentalMatchWithTwoMatchedNotes()
  const updateSpy = mockSdkService(
    TextContentController,
    "updateNoteContent",
    reviewedRealm
  )

  wrapper = mountAnsweredSpellingQuestion(answeredQuestion, {
    currentUser: makeMe.aUser.please(),
    seedRealms: [reviewedRealm, matchedA, matchedB],
    withRouter: true,
  })
  await flushPromises()
  await openResolveAccidentalMatch(wrapper)

  ;(
    document.body.querySelector(
      '[data-testid="add-as-overlapped-note-10"]'
    ) as HTMLElement
  ).click()
  await flushPromises()

  expect(updateSpy).toHaveBeenCalledTimes(1)
  const callArgs = updateSpy.mock.calls[0]![0] as {
    path: { note: number }
    body: { content?: string }
  }
  expect(callArgs.path.note).toBe(reviewedRealm.id)
  expect(callArgs.body.content).toContain("[[") // wiki-link token, not plain alias
  // still list + accidental-match chrome
  expect(
    document.body.querySelector(
      '[data-testid="accidental-match-resolve-dialog"]'
    )
  ).toBeTruthy()
  expect(
    wrapper.find('[data-testid="accidental-match-alert"]').exists()
  ).toBe(true)
  expect(
    wrapper.find('[data-testid="overlap-try-again"]').exists()
  ).toBe(false)
  expect(
    wrapper.find('[data-testid="overlap-try-again-alert"]').exists()
  ).toBe(false)
  expect(wrapper.emitted("retry")).toBeUndefined()
})
```

**Delta-only gate cases** — copy Phase 9 readonly / unloaded (lines 193–232); swap selector to `add-as-overlapped-note-` (and keep Build-a-link asserts if consolidating under shared gate):
```typescript
// Source: AnsweredSpellingQuestionAccidentalMatch.spec.ts:193-208
it("omits Build a link when reviewed notebook is readonly", async () => {
  // … reviewedRealm.notebookRealm.readonly = true …
  expect(
    document.body.querySelectorAll('[data-testid^="link-to-matched-note-"]')
  ).toHaveLength(0)
})
// Parallel: querySelectorAll('[data-testid^="add-as-overlapped-note-"]') length 0
```

**Null-append delta:** seed content that already has the overlap wiki-link alias → click → `updateSpy` not called; stay on list (D-05). Prefer fixture via `makeMe` content rather than mocking the util.

**Focused assertions:** one behavior per test; canonical success owns full shape; readonly/unloaded/null-append assert only their delta (`unit-testing.mdc`).

---

### `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` (page object)

**Analog:** `openLinkToMatchedNote` (lines 90–114) + `expectStillOnAccidentalMatchResult` (135–148) + overlap negative helpers (15–24, 26–29).

**Open Resolve → Add as overlapped** (mirror Build a link; no step swap after click):
```typescript
// Analog: AnsweredQuestionPage.ts:90-114
openAddAsOverlappedNote(matchedNoteTitle: string) {
  cy.findByTestId('resolve-accidental-match')
    .scrollIntoView()
    .should('be.visible')
    .click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('be.visible')
    .and('contain.text', matchedNoteTitle)
    .within(() => {
      cy.get('[data-testid^="add-as-overlapped-note-"]')
        .should('be.visible')
        .and('contain.text', 'Add as overlapped note')
        .click()
    })
  waitUntilAppIsNotBusy()
  return self
},
```

**Post-success assert** — reuse `expectStillOnAccidentalMatchResult`; add no try-again:
```typescript
expectStillOnAccidentalMatchResult(answer, matchedNoteTitle)
cy.findByTestId('overlap-try-again').should('not.exist')
cy.findByTestId('overlap-try-again-alert').should('not.exist')
```

---

### `e2e_test/features/recall/accidental_match_reveal.feature` + `recall.ts` (e2e)

**Analog:** wiki-property stay-on-result scenario (feature lines 24–32) + When step (recall.ts 173–179).

**Recommended Gherkin** (extend same feature; capability-named; no phase numbers):
```gherkin
Scenario: Add as overlapped note stays on accidental match without try-again
  Given It's day 1
  And the note "sedition" was assimilated on day 1
  When I visit recall for a due quiz question on day 2
  Then I should be asked spelling question "means incite violence" from notebook "English practice"
  When I type my answer "sedation"
  Then I should see an accidental match reveal for spelling answer "sedation" with reviewed note "sedition" and matched note "sedation"
  When I add the matched note "sedation" as overlapped from the accidental match result
  Then I should still be on the accidental match result for spelling answer "sedation" with matched note "sedation"
  And I should not see overlap try-again on the accidental match result
```

**Step wiring analog** (recall.ts 173–179):
```typescript
When(
  'I add the matched note {string} as overlapped from the accidental match result',
  (matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .openAddAsOverlappedNote(matchedNoteTitle)
  }
)
```

**Keep** `overlap_try_again.feature` untouched; run it targeted after Wave 2 to prove uncoupled.

---

## Shared Patterns

### AMR-07 mutating-action gate (hide, not disable)
**Source:** `AccidentalMatchResolveDialog.vue:55-62`  
**Apply to:** Both **Build a link** and **Add as overlapped note**  
```typescript
function canOfferMutatingAction(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}
```
Hydrate reviewed realm once at dialog host (`reviewedRealm` computed lines 48–53).

### Content compose → conditional `updateTextField`
**Source:** `MatchedNoteLinkOffer.vue:83-96` + Phase 10 util  
**Apply to:** Host `addAsOverlappedNote` only  
- Compose with `appendOverlapWikiLinkToNoteContent`  
- Guard: `if (composed === null) return`  
- Persist: `updateTextField(reviewedNoteId, "edit content", composed)`  
- Field union: `"edit title" | "edit content"` (`StoredApiCollection.ts:98-105`)

### Outcome-discriminated chrome (do not couple)
**Source:** `AnsweredSpellingQuestion.vue:65-73, 31-41`  
**Apply to:** All Phase 11 tests (negative asserts)  
- Resolve UX only for `ACCIDENTAL_MATCH`  
- Try-again only for graded `OVERLAP`  
- Declare must not emit `retry` or flip outcome

### Vitest answered-spelling boundary
**Source:** `answeredSpellingQuestionTestSupport.ts` + Phase 9 accidental-match spec  
**Apply to:** Wave 1  
- Drive `AnsweredSpellingQuestion` via `mountAnsweredSpellingQuestion`  
- `seedRealms` + `currentUser` for writable gates  
- `mockSdkService(TextContentController, "updateNoteContent", …)` for HTTP  
- Query `data-testid` on `document.body` (Modal teleports)  
- Avoid role queries (`frontend-testing.mdc`)

### E2E page-object over Gherkin rewrite
**Source:** Phase 9 `AnsweredQuestionPage` + `accidental_match_reveal.feature`  
**Apply to:** Wave 2  
- Prefer new page-object method + one When step  
- Reuse `expectStillOnAccidentalMatchResult`  
- Extend reveal feature rather than inventing a phase-numbered feature

### DaisyUI CTA density
**Source:** `AccidentalMatchResolveRow.vue:16-26`  
**Apply to:** New Add as overlapped button  
`class="daisy-btn daisy-btn-secondary daisy-btn-sm"`

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 11 files have exact or role-match analogs from Phases 7–10 |

## Metadata

**Analog search scope:**  
`frontend/src/components/recall/`, `frontend/src/utils/`, `frontend/src/store/StoredApiCollection.ts`, `frontend/tests/components/recall/`, `e2e_test/start/pageObjects/`, `e2e_test/features/recall/`, `e2e_test/step_definitions/`, prior `.planning/phases/{07,08,09,10}-*/` PATTERNS

**Files scanned:** ~20 primary seams (dialog/row/offer/answered-spelling/util/store + Vitest/E2E/page-object/Gherkin)

**Pattern extraction date:** 2026-08-05

**Key planner notes:**
1. Behavior wiring only — util already shipped; zero new libraries; no backend/SRS/`AnswerOutcome` change.
2. Highest-risk lock: Pitfall 4 — Vitest + E2E must assert **no** `overlap-try-again*` and **no** `retry` emit after declare.
3. Differencing vs Build a link: no step swap / no `MatchedNoteLinkOffer` / stay on list after click.
4. Differencing vs MatchedNoteLinkOffer persist: use `appendOverlapWikiLinkToNoteContent` + `=== null` guard; skip `closeDialogThen`.
