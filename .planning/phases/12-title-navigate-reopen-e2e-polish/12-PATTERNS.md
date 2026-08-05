# Phase 12: Title navigate, reopen, E2E polish - Pattern Map

**Mapped:** 2026-08-05
**Files analyzed:** 6 (3 primary E2E + 1 contingency product + 1 optional Vitest + 1 regression guard)
**Analogs found:** 6 / 6

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `e2e_test/features/recall/accidental_match_reveal.feature` | test (Cucumber feature) | request-response (user journey) | Same file — Phase 9–11 “stay on result” scenarios | exact |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | test (page object) | request-response | Same file — `openResolveAndClickMatchedNoteCta` + `expectStillOnAccidentalMatchResult` | exact |
| `e2e_test/step_definitions/accidental_match.ts` | test (step defs) | request-response | Same file — thin `start.assumeAnsweredQuestionPage()` wrappers | exact |
| `frontend/src/pages/RecallPage.vue` *(contingency only)* | page / component | request-response + in-session state | `DoughnutApp.vue` KeepAlive include + existing `RecallPage` refs/cursor | role-match |
| `frontend/tests/pages/RecallPage.spec.ts` *(optional Wave 1b)* | test (Vitest) | transform (activate/deactivate) | `frontend/tests/composables/useThinkingTimeTracker.spec.ts` KeepAlive harness | role-match |
| `e2e_test/features/recall/overlap_try_again.feature` | test (regression guard) | request-response | Leave uncoupled — run only; do not rewrite | exact (no-edit) |

**Product leave-as-is (reference only unless E2E proves remount):**

| File | Why referenced |
|------|----------------|
| `frontend/src/DoughnutApp.vue` | KeepAlive `:include="['RecallPage']"` — canonical persistence seam |
| `frontend/src/components/notes/NoteTitleWithLink.vue` | Title → `noteShowLocation` (D-01: keep navigable) |
| `frontend/src/components/recall/AccidentalMatchResolveRow.vue` | `<NoteTitleWithLink>` inside resolve dialog |
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | Resolve CTA gated on `matchedNotes` |
| `frontend/src/components/commons/Modal.vue` | Closes on `route.fullPath` — dialog disposable |

**No OpenAPI / backend files** — research locks enrichment out unless KeepAlive fails (D-04).

---

## Pattern Assignments

### `e2e_test/features/recall/accidental_match_reveal.feature` (test, request-response)

**Analog:** Same feature — extend with one capability-named scenario; do not rewrite Background or existing scenarios (D-06/D-07).

**Feature header + Background pattern** (lines 1–14):
```gherkin
@mockBrowserTime
@disableOpenAiService
Feature: Accidental match reveal
  As a learner doing spelling recall
  I want an optional resolve dialog listing matched note titles and notebook path when my answer names another note
  So that I can see the conflict without losing focus on the reviewed note

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        | Skip Memory Tracking | Remember Spelling |
      | English  |                                | true                 |                   |
      | sedition | Sedition means incite violence |                      | true              |
      | sedation | Put to sleep is sedation       |                      |                   |
```

**Stay-on-result scenario shape to mirror** (lines 24–32 — link stays on result; reopen scenario adds leave → history back → reopen):
```gherkin
  Scenario: Offer links the matched note as a wiki property without leaving the result
    Given It's day 1
    And the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "sedation"
    Then I should see an accidental match reveal for spelling answer "sedation" with reviewed note "sedition" and matched note "sedation"
    When I link the matched note "sedation" as a wiki property from the accidental match result
    Then I should still be on the accidental match result for spelling answer "sedation" with matched note "sedation"
```

**Recommended new scenario shape** (planner — capability-named; no phase numbers):
```gherkin
  Scenario: Reopen resolve after navigating matched title and returning
    # same Given/When answer setup as other scenarios
    When I open resolve and navigate to matched note "sedation"
    And I return to recall via history back
    Then I should see resolve available again for spelling answer "sedation" with matched note "sedation"
```

**Anti-pattern (do not copy for AMR-05 return):** `browse_answer_and_notes_while_recalling.feature` lines 25–33 uses Resume menu — clears cursor (D-03 / Pitfall 2).

---

### `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` (page object, request-response)

**Analog:** Same page object — extend helpers; prefer page-object over Gherkin churn (D-06).

**Imports + busy-wait pattern** (lines 1–3):
```typescript
import { waitUntilAppIsNotBusy } from '../pageBase'
import { form } from '../forms'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'
```

**Open resolve + act inside dialog** (lines 32–51) — copy structure for title click instead of CTA buttons:
```typescript
function openResolveAndClickMatchedNoteCta(
  matchedNoteTitle: string,
  testIdPrefix: string,
  buttonLabel: string
) {
  cy.findByTestId('resolve-accidental-match')
    .scrollIntoView()
    .should('be.visible')
    .click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('be.visible')
    .and('contain.text', matchedNoteTitle)
    .within(() => {
      cy.get(`[data-testid^="${testIdPrefix}"]`)
        .should('be.visible')
        .and('contain.text', buttonLabel)
        .click()
    })
}
```

**Title link already asserted in open/dismiss path** (lines 93–101) — reuse `cy.contains('a', matchedNoteTitle)` for navigate:
```typescript
      cy.findByTestId('accidental-match-resolve-dialog')
        .should('be.visible')
        .and('contain.text', matchedNoteTitle)
        .and('contain.text', 'English practice')
        .within(() => {
          cy.contains('a', matchedNoteTitle).should('be.visible')
        })
```

**Stay-on-result assert after mutate** (lines 161–173) — for reopen-after-back, assert CTA + reopen list, **not** that dialog stayed open:
```typescript
    expectStillOnAccidentalMatchResult(
      answer: string,
      matchedNoteTitle: string
    ) {
      cy.url().should('include', '/recall')
      expectAccidentalMatchAlert(answer)
      cy.findByTestId('resolve-accidental-match')
        .scrollIntoView()
        .should('be.visible')
      cy.findByTestId('accidental-match-resolve-dialog')
        .should('be.visible')
        .and('contain.text', matchedNoteTitle)
      return self
    },
```

**Suggested new helpers** (from RESEARCH — no in-repo `cy.go('back')` yet):
```typescript
openResolveDialog() {
  cy.findByTestId('resolve-accidental-match').scrollIntoView().should('be.visible').click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog').should('be.visible')
  return self
},
clickMatchedNoteTitle(title: string) {
  cy.findByTestId('accidental-match-resolve-dialog').within(() => {
    cy.contains('a', title).click()
  })
  waitUntilAppIsNotBusy()
  // optionally assert note show URL like notePage wikiLink follow
  return self
},
returnToRecallViaHistoryBack() {
  cy.go('back')
  waitUntilAppIsNotBusy()
  cy.url().should('include', '/recall')
  return self
},
expectResolveCtaWithMatches(answer: string, matchedNoteTitle: string) {
  expectAccidentalMatchAlert(answer)
  cy.findByTestId('resolve-accidental-match').scrollIntoView().should('be.visible')
  // reopen + assert same title(s) in dialog
  return self
},
```

**Secondary analog for following a title link → note show:** `e2e_test/start/pageObjects/notePage.ts` lines 54–58:
```typescript
    followAndAssumeNote(noteTitle: string) {
      locator().click()
      cy.url({ timeout: 15000 }).should('match', noteShowPathInUrl)
      return assumeNotePage(noteTitle)
    },
```

**Anti-pattern for return:** `leaveEpubReadingViewAndReturn` uses `cy.visit(readingPath)` (full remount) — wrong for KeepAlive fidelity (RESEARCH Pitfall 1 / D-03).

**Cursor restore fallback (only if history back lands on quiz):** `goToLastAnsweredQuestion` lines 197–213 — not the primary D-03 path, but available if needed after back.

---

### `e2e_test/step_definitions/accidental_match.ts` (step defs, request-response)

**Analog:** Same file — one When/Then per page-object method; no logic in steps.

**Thin wrapper pattern** (lines 17–48):
```typescript
When(
  'I link the matched note {string} as a wiki property from the accidental match result',
  (matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .linkMatchedNoteAsProperty(matchedNoteTitle)
  }
)

Then(
  'I should still be on the accidental match result for spelling answer {string} with matched note {string}',
  (answer: string, matchedNoteTitle: string) => {
    start
      .assumeAnsweredQuestionPage()
      .expectStillOnAccidentalMatchResult(answer, matchedNoteTitle)
  }
)
```

**Imports pattern** (lines 1–6):
```typescript
/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
```

Add new When/Then steps that call the new page-object helpers only — prefer not changing existing step text used by Phases 7–11 scenarios.

---

### `frontend/src/pages/RecallPage.vue` *(contingency — KeepAlive name / cursor)* (page, request-response + state)

**Primary analog:** `frontend/src/DoughnutApp.vue` lines 87–90 — KeepAlive include must match component name:
```vue
        <router-view v-slot="{ Component }">
          <KeepAlive :include="['RecallPage']">
            <component :is="Component" />
          </KeepAlive>
        </router-view>
```

**Live session state to preserve** (RecallPage lines 122–124) — do not replace with a resolve store (D-05):
```typescript
const previousAnsweredQuestions = ref<(AnsweredQuestion | undefined)[]>([])
const previousAnsweredQuestionCursor = ref<number | undefined>(undefined)
```

**Anti-pattern for AMR-05 return — Resume clears cursor** (lines 196–204):
```typescript
watch(
  () => shouldResumeRecall.value,
  (shouldResume) => {
    if (shouldResume) {
      previousAnsweredQuestionCursor.value = undefined
      clearShouldResumeRecall()
    }
  }
)
```

**Contingency name harden** — no page currently uses `defineOptions({ name })`. Closest `defineOptions` usage is inheritAttrs-only, e.g. `PopButton.vue` line 34:
```typescript
defineOptions({ inheritAttrs: false })
```

If E2E shows remount/`previouslyAnswered` path, add on `RecallPage.vue` script setup:
```typescript
defineOptions({ name: "RecallPage" })
```
(Match string already in KeepAlive include — still no OpenAPI.)

**CTA gate that remount must satisfy** — `AnsweredSpellingQuestion.vue` lines 69–73:
```typescript
const showResolveAccidentalMatchCta = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)
```

**Title navigation (leave alone)** — `NoteTitleWithLink.vue` lines 1–8:
```vue
  <router-link
    :to="noteShowLocation(noteTopology.id)"
    class="no-underline"
  >
    <NoteTitleComponent v-bind="{ noteTopology }" />
  </router-link>
```

**Dialog closes on route change (leave alone)** — `Modal.vue` lines 72–78:
```typescript
watch(
  () => route.fullPath,
  () => {
    emit("close_request")
  }
)
```

**Resolve row title is already a link** — `AccidentalMatchResolveRow.vue` lines 1–6:
```vue
  <li
    class="flex flex-col gap-2"
    :data-testid="`resolve-match-row-${matched.id}`"
  >
    <NoteTitleWithLink :note-topology="matched" />
```

---

### `frontend/tests/pages/RecallPage.spec.ts` *(optional — only if client fix)* (test, transform)

**Analog:** KeepAlive activate/deactivate harness in `useThinkingTimeTracker.spec.ts` lines 152–194:
```typescript
  describe("KeepAlive lifecycle", () => {
    const InnerComponent = defineComponent({
      setup() {
        // ...
        onActivated(() => { start(); resume() })
        onDeactivated(() => pause())
        // ...
      },
    })

    const WrapperComponent = defineComponent({
      components: { InnerComponent, KeepAlive },
      setup() {
        const show = ref(true)
        return { show }
      },
      template: `
        <div>
          <button data-testid="toggle" @click="show = !show">Toggle</button>
          <KeepAlive>
            <InnerComponent v-if="show" key="test" />
          </KeepAlive>
        </div>
      `,
    })
```

**Accidental-match fixture / CTA asserts** — prefer `answeredSpellingQuestionTestSupport.ts` + `AnsweredSpellingQuestionAccidentalMatch.spec.ts` if proving CTA + `matchedNotes` at the spelling boundary rather than full page:
```typescript
import {
  accidentalMatchWithTwoMatchedNotes,
  mountAnsweredSpellingQuestion,
  openResolveAccidentalMatch,
} from "./answeredSpellingQuestionTestSupport"
```

**Skip Vitest wave** if pure E2E + KeepAlive path is green (D-08 / RESEARCH preferred wave).

---

### `e2e_test/features/recall/overlap_try_again.feature` (regression guard)

**Analog:** Leave file untouched. Run targeted Cypress alongside accidental_match (D-07). Shared page object methods (`expectNoMatchedNotesOrAccidentalMatchOnOverlap`, `clickOverlapTryAgain`) must keep working when extending `AnsweredQuestionPage`.

---

## Shared Patterns

### Page-object over Gherkin churn
**Source:** `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` + `e2e_test/step_definitions/accidental_match.ts`
**Apply to:** All Phase 12 E2E work
- Put navigate / history-back / reopen asserts on the page object
- Steps only call `start.assumeAnsweredQuestionPage().…`
- Reuse Background + notebook fixture from accidental_match_reveal

### Manual Resolve reopen (dialog state disposable)
**Source:** `Modal.vue` route watcher + `PopButton.vue` local `show` ref + `AnsweredSpellingQuestion.vue` PopButton host
**Apply to:** Product expectations and E2E asserts
- After title navigate, dialog **must** be closed; assert Resolve CTA, then click again
- Do **not** assert dialog still open across routes; do **not** auto-open

### KeepAlive live-session for match list
**Source:** `DoughnutApp.vue` KeepAlive + `RecallPage` `previousAnsweredQuestions`
**Apply to:** Product contingency + E2E return helper
- Prefer in-app title click + `cy.go('back')` (preserves SPA + KeepAlive)
- Avoid `cy.visit('/recall')` and Main-menu Resume for AMR-05 return

### waitUntilAppIsNotBusy after navigation
**Source:** Existing accidental-match page object (`waitUntilAppIsNotBusy` after resolve open / mutate)
**Apply to:** Title click, history back, reopen CTA clicks

### No OpenAPI enrichment this phase
**Source:** RESEARCH / D-04
**Apply to:** All plans — backend `previouslyAnswered` / `AnsweredQuestion.from` remain read-only references until KeepAlive + client cursor fail

---

## No Analog Found

| File / concern | Role | Data Flow | Reason |
|----------------|------|-----------|--------|
| `cy.go('back')` history-back helper | E2E navigation | request-response | **No in-repo Cypress `cy.go`/`history.back` usage.** Closest leave/return (`leaveEpubReadingViewAndReturn`) uses `cy.visit` — **anti-pattern** for KeepAlive. Introduce `cy.go('back')` as new pattern per RESEARCH / D-03. |
| `defineOptions({ name: 'RecallPage' })` | page name for KeepAlive | config | No page SFC uses explicit KeepAlive `name` today; only `inheritAttrs: false` on PopButton/AutoCollapseDropdown. Contingency introduces the pattern. |

---

## Metadata

**Analog search scope:** `e2e_test/features/recall/`, `e2e_test/start/pageObjects/`, `e2e_test/step_definitions/`, `frontend/src/pages/`, `frontend/src/DoughnutApp.vue`, `frontend/src/components/recall/`, `frontend/src/components/notes/NoteTitleWithLink.vue`, `frontend/src/components/commons/`, `frontend/tests/composables/`, `frontend/tests/components/recall/`
**Files scanned:** ~25 primary hits (grep + targeted reads)
**Pattern extraction date:** 2026-08-05
**Planner note:** Default plan = E2E-only (feature + page object + steps). Budget contingency task for `defineOptions({ name: 'RecallPage' })` only if Wave 1 E2E shows remount symptoms. Skip Vitest unless that contingency fires.
