# Phase 7: Compact result + Resolve dialog shell - Pattern Map

**Mapped:** 2026-08-05
**Files analyzed:** 8
**Analogs found:** 8 / 8

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | component | request-response (UI chrome) | self + `PopButton`/`Modal` host pattern in `FolderNewButton.vue` / `TestMenu.vue` | exact |
| `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` | component | transform (presentational list) | `NotebookHealthFindings.vue` title `<ul>` + `TestMenu.vue` PopButton body without `closer` | role-match |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` | test | request-response | self (rewrite asserts) + `answeredSpellingQuestionTestSupport.ts` | exact |
| `frontend/tests/components/recall/AnsweredSpellingQuestionOverlap.spec.ts` | test | request-response | self (leak assert → no resolve CTA) | exact |
| `e2e_test/features/recall/accidental_match_reveal.feature` | test | request-response | self (reveal → CTA+dialog; `@wip` link scenarios) | exact |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | utility | request-response | self (selector/helper rewrite) | exact |
| `e2e_test/step_definitions/recall.ts` | test | request-response | self (thin wrappers; change only if Gherkin wording changes) | exact |
| `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` | test utility | CRUD (fixtures) | self — **reuse**, likely no API change | exact |

**Keep unused (do not delete):** `MatchedNoteLinkOffer.vue` — Phase 9 reuse. **Anti-pattern for Phase 7 dialog:** nested `PopButton` + `#default="{ closer }"` around link offer (current stacked UI).

## Pattern Assignments

### `frontend/src/components/recall/AnsweredSpellingQuestion.vue` (component, request-response)

**Analog:** self (outcome gates + chrome) + `PopButton.vue` host + `TestMenu.vue` (body without closer) + current link `PopButton` (what to remove / not nest)

**Imports pattern** (lines 64–73) — keep `PopButton`; swap `MatchedNoteLinkOffer` for `AccidentalMatchResolveDialog`; drop realm/link helpers when CTAs leave:
```typescript
import { computed, inject, type PropType, type Ref } from "vue"
import type { AnsweredQuestion, User } from "@generated/doughnut-backend-api"
import NoteShow from "@/components/notes/NoteShow.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import MatchedNoteLinkOffer from "@/components/recall/MatchedNoteLinkOffer.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
```
Phase 7 target imports: drop `MatchedNoteLinkOffer`, `inject`/`User`/`useStorageAccessor` if only used by `canOfferLinkToMatched`; add `AccidentalMatchResolveDialog`.

**Outcome gate** (lines 100–108) — reuse shape for Resolve CTA; never gate on `matchedNotes.length` alone:
```typescript
const isOverlap = computed(
  () => props.answeredQuestion.answer.outcome === "OVERLAP"
)

const showMatchedNotesSection = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)
```
Rename conceptually to `showResolveAccidentalMatchCta` (or keep name and bind CTA).

**Core chrome order today** (lines 1–18) — insert Resolve `PopButton` **immediately after** alert, **before** `NoteUnderQuestion` (D-02):
```vue
  <div
    class="daisy-alert"
    :class="alertClass"
    :data-testid="alertTestId"
  >
    <strong>{{ alertMessage }}</strong>
  </div>
  <NoteUnderQuestion
    v-bind="recalledNoteUnderQuestionProps(answeredQuestion.recalledNote)"
  />
```

**OVERLAP try-again stays outcome-gated** (lines 19–29) — do not share Resolve gate:
```vue
  <button
    v-if="isOverlap"
    type="button"
    class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-6"
    data-testid="overlap-try-again"
    …
  >
    Try again
  </button>
```

**Remove this stacked section + nested PopButton** (lines 30–61) — anti-pattern for Phase 7 / Phase 9 (never nest `PopButton` inside resolve Modal):
```vue
  <section
    v-if="showMatchedNotesSection"
    class="mt-6"
    data-testid="matched-notes-section"
  >
    …
        <PopButton
          v-if="canOfferLinkToMatched(matched.id)"
          :title="'Link to this note'"
          …
          :show-close-button="false"
        >
          <template #default="{ closer }">
            <MatchedNoteLinkOffer
              …
              @close-dialog="closer"
            />
          </template>
        </PopButton>
```

**Recommended Resolve CTA host** — copy attrs style from removed link CTA + UI-SPEC; **no** `#default="{ closer }"` in Phase 7 (see `TestMenu.vue`):
```vue
<PopButton
  v-if="showResolveAccidentalMatchCta"
  title="Resolve accidental match"
  aria-label="Resolve accidental match"
  btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
  data-testid="resolve-accidental-match"
>
  <template #default>
    <AccidentalMatchResolveDialog
      :matched-notes="answeredQuestion.matchedNotes ?? []"
    />
  </template>
</PopButton>
```

**PopButton host contract** (`PopButton.vue` lines 1–25):
```vue
  <Modal
    v-if="show"
    :sidebar="sidebar"
    :show-close-button="showCloseButton"
    @close_request="closeDialog"
  >
    <template #body>
      <slot name="default" :closer="closeDialog" />
    </template>
  </Modal>
```
Default `showCloseButton: true` — leave default for AMR-03.

**Presentational PopButton body without closer** (`TestMenu.vue` lines 2–28) — Phase 7 analog:
```vue
    <PopButton title="Testability" :btn-class="'testability-button'">
      <template #button_face>
        <span class="button-text">T</span>
      </template>
      <h1>Testability</h1>
      <CheckInput … />
      …
    </PopButton>
```

**Contrast — form that needs closer** (`FolderNewButton.vue` lines 1–14) — reserve for Phase 9, not Phase 7:
```vue
  <PopButton :title="buttonTitle" :aria-label="ariaLabel ?? buttonTitle">
    <template #default="{ closer }">
      <FolderNewForm
        …
        @close-dialog="closer"
      />
    </template>
  </PopButton>
```

---

### `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` (component, transform)

**Analog:** `NotebookHealthFindings.vue` vertical title list; presentational props like RESEARCH sketch; **not** `MatchedNoteLinkOffer.vue` (actions + closer).

**Presentational list pattern** (`NotebookHealthFindings.vue` lines 16–22):
```vue
        <ul
          v-if="(group.items?.length ?? 0) > 0"
          class="flex flex-col gap-2"
        >
          <li v-for="(item, index) in group.items" :key="itemKey(item, index)">
            {{ item.label }}
          </li>
        </ul>
```

**Phase 7 target shape** (from RESEARCH / UI-SPEC — implement against this):
```vue
<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
defineProps<{ matchedNotes: NoteTopology[] }>()
</script>

<template>
  <div data-testid="accidental-match-resolve-dialog">
    <ul class="flex flex-col gap-2">
      <li
        v-for="matched in matchedNotes"
        :key="matched.id"
        :data-testid="`resolve-match-row-${matched.id}`"
      >
        {{ matched.title }}
      </li>
    </ul>
  </div>
</template>
```

**Anti-pattern — do not copy into Phase 7 dialog** (`MatchedNoteLinkOffer.vue` lines 1–18, 31–38): multi-step link UI + `closeDialog` emit. Keep file for Phase 9 inside the **same** Modal (step swap), never as nested `PopButton`.

**NoteTopology source** — titles only from grade payload; no realm hydrate:
```typescript
// packages/generated/doughnut-backend-api/types.gen.ts (NoteTopology)
export type NoteTopology = {
    id: number;
    title: string;
    …
};
```

**XSS:** text interpolation only (`{{ matched.title }}`), never `v-html` (UI-SPEC / RESEARCH Security).

---

### `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` (test)

**Analog:** self + `answeredSpellingQuestionTestSupport.ts` mount helpers + `modalTestSupport.ts` / `popButtonTestSupport.ts` for dialog DOM.

**Stable boundary + fixtures** (`answeredSpellingQuestionTestSupport.ts` lines 29–76):
```typescript
export function mountAnsweredSpellingQuestion(
  answeredQuestion: AnsweredQuestion,
  options: { currentUser?: User; seedRealms?: NoteRealm[]; withRouter?: boolean } = {}
) {
  …
  return chain.mount({
    attachTo: document.body,
    global: {
      stubs: {
        NoteShow: noteShowStub,
        NoteUnderQuestion: true,
        ViewMemoryTrackerLink: true,
      },
    },
  })
}

export function accidentalMatchWithTwoMatchedNotes() { … }
```
Keep `attachTo: document.body` so Modal Teleport is queryable. Prefer boundary tests on `AnsweredSpellingQuestion` over a thin dialog-only spec (`unit-testing.mdc`).

**Current asserts to rewrite** (lines 19–41) — replace stacked `matched-notes-section` / matched `NoteShow` ids with: no section; CTA; open → titles in `document.body` / `dialog`:
```typescript
    const matchedSection = wrapper.find('[data-testid="matched-notes-section"]')
    expect(matchedSection.exists()).toBe(true)
    const matchedShows = matchedSection.findAllComponents({ name: "NoteShow" })
    expect(matchedShows.map((show) => show.props("noteId"))).toEqual([10, 20])
```

**Empty matchedNotes omit** (lines 44–55) — keep delta; assert no `resolve-accidental-match` instead of (or in addition to) no section:
```typescript
  it("omits matched notes section when matchedNotes is empty", async () => {
    …
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
  })
```

**Drop / pause link CTA cases** (lines 57–124) — link CTAs removed this phase; delete or park for Phase 9 (do not leave failing). Opening dialog pattern already used for link offer:
```typescript
    await wrapper
      .find('[data-testid="link-to-matched-note-10"]')
      .trigger("click")
    await flushPromises()

    expect(document.body.textContent).toContain("Link to:")
```
Rewrite to click `resolve-accidental-match` and assert `"Matched A"` / `"Matched B"`.

**Dialog dismiss helpers** (`modalTestSupport.ts` lines 14–24, 32–38):
```typescript
export function dialogEl() {
  return document.body.querySelector("dialog")
}
export function closeButtonEl() {
  return document.body.querySelector(".close-button") as HTMLElement | null
}
export async function waitForDialog(attempts = 20) { … }
```
Optional light open→`.close-button`→dialog gone assert (AMR-03); full Modal suite already in `Modal.spec.ts` lines 57–77.

**PopButton open helper** (`popButtonTestSupport.ts` lines 29–39):
```typescript
export async function openPopButtonDialog(wrapper: VueWrapper) {
  await wrapper.find("button").trigger("click")
  await flushPromises()
}
export function modalCloseButtonEl() {
  return document.body.querySelector(".close-button") as HTMLElement | null
}
```

**Focused assertions** (`unit-testing.mdc`): canonical accidental-match case owns alert + CTA + titles; empty / OVERLAP siblings assert only deltas (no CTA / no dialog entry).

---

### `frontend/tests/components/recall/AnsweredSpellingQuestionOverlap.spec.ts` (test)

**Analog:** self — update leak test selectors.

**Leak assert today** (lines 31–45):
```typescript
  it("hides matched-notes section even when matchedNotes leak on OVERLAP", async () => {
    …
    expect(wrapper.find('[data-testid="matched-notes-section"]').exists()).toBe(
      false
    )
    expect(wrapper.text()).not.toContain("Link to this note")
  })
```
Phase 7: also assert no `[data-testid="resolve-accidental-match"]` (and no resolve dialog copy). Keep try-again emit test unchanged (lines 13–28).

---

### `e2e_test/features/recall/accidental_match_reveal.feature` (test)

**Analog:** self.

**Scenario 1** (lines 16–22) — keep structure; change reveal meaning via page object (CTA + dialog titles), not stacked bodies. Update Feature blurb (lines 3–6) to match compact + optional resolve.

**Scenarios 2–3** (lines 24–42) — tag `@wip` until Phase 9 restores Build a link. CI skips `@wip` (`e2e_test/config/ci.ts`: `not @wip`). Do **not** `@wip` the whole feature. Current repo `@wip` count is 0; cap is 5.

```gherkin
  @wip
  Scenario: Offer links the matched note as a wiki property without leaving the result
  …
  @wip
  Scenario: Offer links the matched note as a relationship without leaving the result
```

Capability naming only — no phase numbers in feature/scenario names.

---

### `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` (utility)

**Analog:** self — rewrite reveal helpers; extend overlap “no accidental” helper.

**Alert helper keep** (lines 5–13):
```typescript
function expectAccidentalMatchAlert(answer: string) {
  cy.findByTestId('accidental-match-alert')
    .scrollIntoView()
    .should('be.visible')
    …
}
```

**Replace stacked reveal** (lines 31–41, 64–78):
```typescript
function expectMatchedNoteInSection(matchedNoteTitle: string) {
  cy.findByTestId('matched-notes-section')
    …
}
```
Direction: assert `resolve-accidental-match` → click → dialog / body contains matched title → optional close (`.close-button`) → still on result. Reviewed note via existing `note-title` filters stays.

**Overlap no-bleed** (lines 26–28) — extend:
```typescript
function expectNoMatchedNotesOrAccidentalMatch() {
  cy.findByTestId('matched-notes-section').should('not.exist')
  cy.findByTestId('accidental-match-alert').should('not.exist')
}
```
Add `resolve-accidental-match` / resolve dialog absent.

**Link helpers** (lines 80–116) — leave callable for `@wip` scenarios / Phase 9; they will fail until link returns inside dialog. Prefer keeping implementations and tagging scenarios `@wip` rather than deleting helpers prematurely.

**Fluent page object** — return `self` for chaining (existing pattern).

---

### `e2e_test/step_definitions/recall.ts` (test)

**Analog:** self — thin wrappers around page object (lines 164–197).

```typescript
Then(
  'I should see an accidental match reveal for spelling answer {string} with reviewed note {string} and matched note {string}',
  (answer, reviewedNoteTitle, matchedNoteTitle) => {
    start
      .assumeAnsweredQuestionPage()
      .expectAccidentalMatchReveal(answer, reviewedNoteTitle, matchedNoteTitle)
  }
)
```
Prefer updating page object only so step text can stay; change Gherkin wording only if product language shifts to “resolve dialog”.

---

### `frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts` (test utility)

**Analog:** self — **reuse** `mountAnsweredSpellingQuestion` / `accidentalMatchWithTwoMatchedNotes`. No new fixture API required for Phase 7 titles (topology already on accidentalMatch builder). Seed realms optional for Phase 7 (no link gates); keep helper for Phase 9.

## Shared Patterns

### PopButton → Modal host
**Source:** `frontend/src/components/commons/Popups/PopButton.vue`, `frontend/src/components/commons/Modal.vue`
**Apply to:** Resolve CTA in `AnsweredSpellingQuestion`
- One Modal via `PopButton`; default close button; Teleport to `body`; ESC via `modalStack`; route `fullPath` → `close_request`
- Phase 7: slot without `closer`; Phase 9 may use `#default="{ closer }"` for in-dialog steps
- Forbidden: DaisyUI stock modal markup fork; nested `PopButton` inside resolve Modal

```vue
<!-- Modal.vue dismiss surfaces -->
<button v-if="showCloseButton" class="close-button" @click="$emit('close_request')">
…
<div class="modal-panel-wrapper" @mousedown.self="$emit('close_request')">
…
watch(() => route.fullPath, () => { emit("close_request") })
onMounted(() => { dialogRef.value?.showModal(); unregister = registerModal(() => emit("close_request")) })
```

### Outcome-discriminated chrome
**Source:** `AnsweredSpellingQuestion.vue` (`isOverlap` vs `ACCIDENTAL_MATCH` gates)
**Apply to:** Resolve CTA visibility; Overlap unit + E2E leak asserts
- Resolve only when `outcome === "ACCIDENTAL_MATCH"` **and** `matchedNotes.length > 0`
- OVERLAP keeps try-again; never shows Resolve CTA even if `matchedNotes` leaks

### Small-test Vitest boundary
**Source:** `answeredSpellingQuestionTestSupport.ts` + AccidentalMatch / Overlap specs
**Apply to:** all Phase 7 unit updates
- Drive `AnsweredSpellingQuestion`; `makeMe.anAnsweredQuestion.accidentalMatch(...)` / `.overlap(...).withMatchedNotes(...)`
- `data-testid` selectors; query Modal content on `document.body`
- Focused assertions; stub only NoteShow / NoteUnderQuestion / ViewMemoryTrackerLink as today

### E2E page object + `@wip`
**Source:** `AnsweredQuestionPage.ts`, `accidental_match_reveal.feature`, `e2e_test/config/ci.ts`
**Apply to:** reveal + link pause
- Capability-named features; fluent page objects
- Tag only link scenarios `@wip` (cap 5); keep reveal + `overlap_try_again` green
- Targeted Cypress: `accidental_match_reveal.feature` + `overlap_try_again.feature`

### DaisyUI / spacing (UI-SPEC)
**Apply to:** CTA + dialog list only
- CTA: `daisy-btn daisy-btn-secondary daisy-btn-sm mt-2` (not `mt-6`)
- List: `flex flex-col gap-2`
- Do not restyle Modal panel padding / close hit area

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 7 files have in-repo analogs |

**Closest novel piece:** `AccidentalMatchResolveDialog` is new but is a thin presentational list — copy `NotebookHealthFindings` `<ul>` + RESEARCH SFC sketch; no new library.

## Anti-Patterns Checklist (planner / executor)

| Do not | Why | Instead |
|--------|-----|---------|
| Keep stacked `NoteShow`s until dialog “ready” | AMR-01 / Pitfall 1 | Remove stacks + ship CTA+title list same phase |
| Nest `PopButton` / `MatchedNoteLinkOffer` inside resolve Modal | Nested modal / Phase 9 | Single Modal; Phase 9 steps into offer |
| Auto-open Modal on submit | AMR-03 / Pitfall 2 | Click-only open |
| Gate CTA on length alone | OVERLAP leak (Pitfall 8) | Require `ACCIDENTAL_MATCH` |
| `v-html` titles | XSS | `{{ matched.title }}` |
| Phase numbers in product/test names | `planning.mdc` | Capability names / testids above |
| Leave link E2E red without `@wip` | CI fail | `@wip` scenarios 2–3 only |
| Delete `MatchedNoteLinkOffer.vue` | Phase 9 needs it | Keep file; unused in Phase 7 UI |

## Metadata

**Analog search scope:** `frontend/src/components/recall/`, `frontend/src/components/commons/`, `frontend/src/components/notes/`, `frontend/src/components/notebook/`, `frontend/src/pages/`, `frontend/tests/components/recall/`, `frontend/tests/commons/`, `e2e_test/features/recall/`, `e2e_test/start/pageObjects/`, `e2e_test/step_definitions/`
**Files scanned:** ~25 primary + grep hits across PopButton usages
**Pattern extraction date:** 2026-08-05
**UI contract:** `07-UI-SPEC.md` (testids, CTA copy, spacing, dismiss)
