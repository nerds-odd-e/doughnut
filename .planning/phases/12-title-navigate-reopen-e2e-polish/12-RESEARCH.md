# Phase 12: Title navigate, reopen, E2E polish - Research

**Researched:** 2026-08-05
**Domain:** Vue KeepAlive recall session + accidental-match resolve reopen / Cypress E2E
**Confidence:** HIGH (in-repo remount/`matchedNotes` fidelity); MEDIUM (Vue KeepAlive `__name` matching via Context7)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
#### Reopen affordance (Pitfall 6)
- **D-01:** Allow title navigation to leave recall (existing `NoteTitleWithLink` → `noteShowLocation`). Do **not** `preventDefault` or otherwise block title clicks to keep the dialog mounted. Modal closing on route change is expected. — **Reversibility:** reversible
- **D-02:** Minimum bar is **manual reopen**: after return to the accidental-match result, the same **Resolve accidental match** CTA must remain available; user opens the dialog again and sees the same match titles (and paths once hydrated). Do **not** auto-reopen the dialog via global store/query unless a later polish phase explicitly specs it + E2E. — **Reversibility:** reversible

#### Return path and match-list persistence
- **D-03:** Canonical return for product + E2E is **history back** (browser back / equivalent) to the accidental-match result under recall — not a new “Back to result” chrome on note show, and not requiring Recently Recalled as the only path. — **Reversibility:** reversible
- **D-04:** Prefer restoring matches from the existing answered-question payload (`answeredQuestion.matchedNotes` on the in-session `previousAnsweredQuestions` list and/or `RecallsController.previouslyAnswered`). Do **not** add OpenAPI/backend match-list enrichment in this phase unless plan-time research proves the remount path drops matches and no client-side fix restores them. — **Reversibility:** costly — API enrichment would widen Answer DTOs / history without a proven need
- **D-05:** If remount clears the live answered cursor, restore enough recall state so the accidental-match result (alert + Resolve CTA + `matchedNotes`) is visible again after return — researcher/planner choose the smallest seam (e.g. keep-alive, cursor restore, or previouslyAnswered fidelity). Do not invent a dedicated “resolve session” store for auto-open state. — **Reversibility:** reversible

#### E2E polish and coverage shape
- **D-06:** Add a capability-named E2E scenario for reopen-after-title-navigate: open Resolve → click matched title → leave → return → Resolve CTA → dialog lists the same match(es). Prefer page-object helpers over rewriting existing Gherkin step text where possible. — **Reversibility:** reversible
- **D-07:** Treat open/dismiss and multi-match path identity as **must stay green** (extend asserts/page objects if gaps remain); do not rewrite the whole accidental-match feature file. Keep `overlap_try_again` uncoupled and green. — **Reversibility:** reversible
- **D-08:** Wave 1 — Vitest at the recall / answered-spelling boundary for remount-or-return seams that prove CTA + same `matchedNotes` after simulated leave/return (only if a client fix is needed). Wave 2 — targeted E2E round-trip. Skip Vitest-only wave if research shows pure E2E + tiny client fix is enough — planner decides after research. — **Reversibility:** reversible

### Claude's Discretion
- Exact E2E return helper (`cy.go('back')` vs navigate to `/recall` then show last answered) as long as D-03’s history-back intent holds.
- Whether multi-match reopen uses a new fixture notebook pair or reuses Phase 8 two-match fixtures.
- Whether any keep-alive / cursor restore lives on `RecallPage` vs a thinner composable — prefer smallest change that makes AMR-05 true.

### Deferred Ideas (OUT OF SCOPE)
- AMR-10..13 resolve polish (quiet already-linked/overlapped, keyboard, etc.) — v2
- SEED-001 MCQ / fuzzy / `Notebook:Title` — parked seed
- Auto-reopen dialog via session/query — only if a later phase specs it + E2E
- OpenAPI enrichment of match path/history — only if client remount cannot restore matches
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AMR-05 | After navigating away via a matched title and returning to the accidental-match result, user can open the resolve dialog again and see the same matches | KeepAlive already caches `RecallPage` with live `previousAnsweredQuestions` (full `matchedNotes`). Dialog closes on route change; manual Resolve CTA reopen is the product bar. `previouslyAnswered` does **not** return `matchedNotes` — do not rely on remount/history reload unless a client KeepAlive path fails E2E. Targeted E2E + page objects prove the round-trip; keep `overlap_try_again` green. |
</phase_requirements>

## Summary

Phase 12 is primarily an **E2E-proven reopen** story, not an OpenAPI enrichment story. On the live answer path, `matchedNotes` already live on the in-session `previousAnsweredQuestions` entry pushed by `useRecallAnswerHandling.onAnswered`. `DoughnutApp` wraps the main `router-view` in `<KeepAlive :include="['RecallPage']">`, and Vue script-setup SFCs infer `__name` from the filename (`RecallPage.vue` → `RecallPage`), so history-back to `/recall` should **reactivate** the same instance with cursor + full match list intact. The resolve Modal already closes on `route.fullPath` change; `PopButton` local `show` resets — matching locked **manual** reopen (D-01/D-02).

The history-reload / remount path is **lossy**: `RecallsController.previouslyAnswered` maps via single-arg `AnsweredQuestion.from(RecallPrompt)`, which never sets `matchedNotes`, and `Answer.matchedNoteId` is `@Transient` (not persisted). Per D-04, do **not** enrich OpenAPI unless E2E proves KeepAlive cannot restore CTA + matches. Smallest seam if keep-alive fails: harden KeepAlive naming / cursor restore on the client — not a dedicated resolve store and not backend enrichment.

**Primary recommendation:** Plan E2E-first (page-object reopen helpers + capability-named scenario using in-app title click + `cy.go('back')`). Expect **zero or tiny** product code. Skip a Vitest-only wave unless a client fix is required (D-08). Contingency order if E2E red: verify KeepAlive include match → cursor still set after back → only then consider `previouslyAnswered` fidelity / OpenAPI.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Title navigate to matched note | Browser / Client | — | `NoteTitleWithLink` → vue-router `noteShow` |
| Modal close on leave | Browser / Client | — | `Modal` watches `route.fullPath` → `close_request` |
| Preserve accidental-match result + `matchedNotes` across leave/return | Browser / Client | — | KeepAlive-cached `RecallPage` refs (`previousAnsweredQuestions`, cursor) |
| Manual Resolve CTA reopen | Browser / Client | — | `AnsweredSpellingQuestion` PopButton; no auto-open |
| Grade-time `matchedNotes` payload | API / Backend | — | Already on answerSpelling response; out of Phase 12 change scope |
| `previouslyAnswered` history | API / Backend | — | Remount fallback only; currently omits `matchedNotes` — enrichment deferred unless KeepAlive fails |
| E2E reopen-after-title-navigate | Browser / Client (Cypress) | — | Page objects + feature scenario; not product chrome |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| vue | 3.5.40 | Recall UI + KeepAlive | In-repo frontend dependency `[VERIFIED: frontend/package.json]` |
| vue-router | 5.2.0 | Title navigate + history back | In-repo; `NoteTitleWithLink` / `noteShowLocation` `[VERIFIED: frontend/package.json]` |
| Vitest | 4.1.10 | Optional client remount harness | Frontend unit tests `[VERIFIED: frontend/package.json]` |
| Cypress + Cucumber | in-repo e2e_test | Targeted E2E for AMR-05 | Existing accidental-match feature + page objects |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `@generated/doughnut-backend-api` | generated | `AnsweredQuestion.matchedNotes?: NoteTopology[]` | Assert/fixture shapes only — do not change OpenAPI this phase unless contingency fires |
| `doughnut-test-fixtures/makeMe` | workspace | Accidental-match fixtures | Vitest only if Wave 1 client-fix needed |
| existing `PopButton` / `Modal` | in-repo | Resolve dialog host | Already wired; no new modal library |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| KeepAlive-preserved live answer | Enrich `previouslyAnswered` with `matchedNotes` | Costly OpenAPI/DTO work; only if remount is the real path and KeepAlive fails (D-04) |
| History back | Main-menu Resume / `resumeRecall` | Clears answered cursor by design — wrong for AMR-05 (D-03) |
| Auto-reopen dialog | Manual Resolve CTA | Deferred (D-02) |

**Installation:**
```bash
# No new packages — reuse vue / vue-router / Vitest / Cypress already in the monorepo
```

**Version verification:** Core versions read from `frontend/package.json` this session. No new registry packages.

## Package Legitimacy Audit

> No external packages are installed for this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A — no installs |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
[User on ACCIDENTAL_MATCH result]
        │
        ▼
[Resolve CTA] ──open──► [PopButton show=true]
        │                      │
        │                      ▼
        │              [AccidentalMatchResolveDialog]
        │                      │
        │                      ▼
        │              [NoteTitleWithLink → noteShow]
        │                      │
        │                      ▼
        │         route.fullPath changes
        │                      │
        │         ┌────────────┴────────────┐
        │         ▼                         ▼
        │   [Modal close_request]    [KeepAlive deactivate RecallPage]
        │   PopButton show=false     (refs kept: previousAnsweredQuestions,
        │                             previousAnsweredQuestionCursor,
        │                             matchedNotes on live entry)
        │
        ▼
[Inspect note show]
        │
        ▼
[history back] ──► KeepAlive activate RecallPage
        │
        ▼
[Same accidental-match result: alert + Resolve CTA + matchedNotes]
        │
        ▼
[User clicks Resolve again] ──► dialog lists same matches (paths hydrate)

── Remount / cold reload (NOT the D-03 happy path) ──
onMounted → previouslyAnswered → AnsweredQuestion.from(prompt)
  → matchedNotes ABSENT → Resolve CTA HIDDEN
```

### Recommended Project Structure
```
frontend/src/
├── DoughnutApp.vue                 # KeepAlive include=['RecallPage'] — leave as-is unless include miss
├── pages/RecallPage.vue            # previousAnsweredQuestions + cursor; only touch if remount/cursor broken
├── components/recall/
│   ├── AnsweredSpellingQuestion.vue
│   ├── AccidentalMatchResolveDialog.vue
│   └── AccidentalMatchResolveRow.vue  # NoteTitleWithLink already navigable
e2e_test/
├── features/recall/accidental_match_reveal.feature  # add reopen scenario
├── features/recall/overlap_try_again.feature        # must stay green, uncoupled
└── start/pageObjects/AnsweredQuestionPage.ts        # navigate + reopen helpers
```

### Pattern 1: Rely on KeepAlive for live-session reopen
**What:** After title navigate, history back reactivates cached `RecallPage`; live answered entry still has `matchedNotes`; user clicks Resolve again.
**When to use:** Canonical AMR-05 product path (D-03).
**Example:**
```vue
<!-- Source: frontend/src/DoughnutApp.vue:88-90 -->
<KeepAlive :include="['RecallPage']">
  <component :is="Component" />
</KeepAlive>
```

### Pattern 2: Manual CTA reopen (no auto-open)
**What:** Modal closes on route change; `PopButton` `show` stays false until user clicks Resolve.
**When to use:** Always for this phase (D-02).
**Example:**
```ts
// Source: frontend/src/components/commons/Modal.vue:72-78
watch(
  () => route.fullPath,
  () => {
    emit("close_request")
  }
)
```

### Pattern 3: E2E via in-app navigation + history back
**What:** Click match title inside dialog (real `router-link`), then `cy.go('back')` (or equivalent history back). Do **not** use Main-menu Resume (`resumeRecall` clears cursor). Prefer page-object helpers over Gherkin churn.
**When to use:** AMR-05 E2E (D-06).

### Anti-Patterns to Avoid
- **`preventDefault` on title clicks to keep dialog mounted:** Blocks inspect-matched-note (D-01 / Pitfall 6).
- **Auto-reopen dialog from global store/query:** Out of scope (D-02).
- **OpenAPI enrichment of `matchedNotes` on history:** Costly; only if KeepAlive + client cursor fail (D-04).
- **Using Resume menu as “return”:** `shouldResumeRecall` clears `previousAnsweredQuestionCursor` — hides accidental-match result.
- **`cy.visit('/recall')` for return:** Full document load remounts app; loses KeepAlive cache — wrong for D-03 fidelity.
- **Rewriting whole accidental-match feature / coupling `overlap_try_again`:** Violates D-07.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Persist resolve-open across routes | Custom “resolve session” store | Manual Resolve CTA + KeepAlive result state | D-02/D-05; modal state is disposable |
| Restore matches after remount | New answer-history table / OpenAPI enrichment | Live `previousAnsweredQuestions` via KeepAlive | D-04; enrichment only if proven necessary |
| E2E leave/return | Ad-hoc `cy.visit` loops | In-dialog title click + history back + page objects | Matches product navigation; preserves SPA KeepAlive |
| Match path after reopen | Duplicate path DTOs | Existing realm hydrate in resolve rows | Already shipped Phases 8–11 |

**Key insight:** AMR-05 fails only if the **answered result** (CTA + `matchedNotes`) is gone after return — not if the dialog itself unmounts. KeepAlive already targets that seam.

## Common Pitfalls

### Pitfall 1: Assuming `previouslyAnswered` restores matches
**What goes wrong:** Remount shows ACCIDENTAL_MATCH alert without Resolve CTA (empty `matchedNotes`).
**Why it happens:** Single-arg `AnsweredQuestion.from` never sets `matchedNotes`; `matchedNoteId` is transient.
**How to avoid:** Design for KeepAlive live session; treat history reload as lossy until proven otherwise.
**Warning signs:** E2E uses full remount/`cy.visit` return; unit tests mock `previouslyAnswered` with accidental-match but omit `matchedNotes`.

### Pitfall 2: Using Resume instead of history back
**What goes wrong:** User/E2E lands on current quiz, not accidental-match result.
**Why it happens:** `resumeRecall` → `shouldResumeRecall` clears cursor.
**How to avoid:** Canonical return = browser/history back (D-03).
**Warning signs:** Steps that “click resume recall from the menu” (existing browse-notes scenario — different capability).

### Pitfall 3: KeepAlive include name miss
**What goes wrong:** Leaving recall destroys instance; remount + `previouslyAnswered` loses matches.
**Why it happens:** `include` matches `name` / `__name`; script-setup usually gets `__name: 'RecallPage'` from filename, but an explicit rename or bundler edge case can break matching.
**How to avoid:** If E2E fails with remount symptoms, first harden `defineOptions({ name: 'RecallPage' })` on `RecallPage.vue` — still no API change.
**Warning signs:** `onMounted` / `previouslyAnswered` fires again on every return.

### Pitfall 4: Breaking OVERLAP / open-dismiss coverage while polishing
**What goes wrong:** Accidental-match E2E churn breaks `overlap_try_again` or deletes open/dismiss asserts.
**Why it happens:** Shared page objects / feature rewrite.
**How to avoid:** Extend `AnsweredQuestionPage` helpers; keep overlap feature uncoupled (D-07).
**Warning signs:** Edits under `overlap_try_again.feature` without a regression reason.

## Code Examples

### Live answer keeps full matchedNotes (grade path)
```java
// Source: backend/.../RecallPromptController.java (answer spelling)
// AnsweredQuestion.from(recallPrompt, matchedNotes) — two-arg overload
// Source: backend/.../dto/AnsweredQuestion.java:51-56
public static AnsweredQuestion from(RecallPrompt recallPrompt, List<Note> matches) {
  AnsweredQuestion answeredQuestion = from(recallPrompt);
  if (matches != null && !matches.isEmpty()) {
    answeredQuestion.setMatchedNotes(matches.stream().map(Note::getNoteTopology).toList());
  }
  return answeredQuestion;
}
```

### previouslyAnswered omits matchedNotes
```java
// Source: backend/.../RecallsController.java:54-66
@GetMapping(value = {"/previously-answered"})
@Transactional
public List<AnsweredQuestion> previouslyAnswered(
    @RequestParam(value = "timezone") String timezone) {
  // ...
  return recallService
      .getPreviouslyAnsweredRecallPrompts(/* ... */)
      .stream()
      .map(AnsweredQuestion::from)  // single-arg — no matchedNotes
      .toList();
}
```

```java
// Source: backend/.../dto/AnsweredQuestion.java:36-48 — single-arg from()
public static AnsweredQuestion from(RecallPrompt recallPrompt) {
  // sets id, questionType, memoryTrackerId, recalledNote, answer, optional MCQ
  // does NOT set matchedNotes
  return answeredQuestion;
}
```

```java
// Source: backend/.../entities/Answer.java:37
@Transient @Getter @Setter private Long matchedNoteId;
```

### Resolve CTA requires matchedNotes length
```ts
// Source: frontend/src/components/recall/AnsweredSpellingQuestion.vue:69-73
const showResolveAccidentalMatchCta = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)
```

### Resume clears answered cursor (anti-pattern for AMR-05 return)
```ts
// Source: frontend/src/pages/RecallPage.vue:196-204
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

### Recommended E2E page-object shape (discretion)
```ts
// Prefer adding to AnsweredQuestionPage — not rewriting Gherkin text
openResolveDialog() { /* click resolve-accidental-match; wait busy */ }
clickMatchedNoteTitle(title: string) { /* within dialog, click <a> title */ }
returnToRecallViaHistoryBack() { cy.go('back'); waitUntilAppIsNotBusy() }
expectResolveCtaWithMatches(answer: string, titles: string[]) { /* alert + CTA + reopen list */ }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Stacked matched NoteShows | Resolve dialog + CTA | v1.2 Phases 7–8 | Titles navigate; dialog local state disposable |
| Fear of remount → API enrich | Prefer KeepAlive live session | This research | Avoids costly OpenAPI unless E2E proves need |
| Auto-reopen dialog | Manual CTA reopen | Pitfall 6 / D-02 | Simpler; testable |

**Deprecated/outdated:**
- Treating “reopen” as “dialog stays open across routes”
- Using `Answer.matchedNoteId` as a full multi-match restore source (`@Transient`, first-id only even if set)

## Project Constraints (from .cursor/rules/)

| Directive | Source | Planner implication |
|-----------|--------|---------------------|
| Behavior vs Structure; one observable behavior; stop-safe | `planning.mdc` | Single Behavior phase AMR-05; no speculative Structure beyond immediate need |
| ~5 min slice; >10 min finer-decompose | `planning.mdc` | Prefer small E2E + tiny fix over large remount redesign |
| Nix tooling: `CURSOR_DEV=true nix develop -c …` | `general.mdc` / agent-map | All test commands via Nix |
| Assume `pnpm sut` running | agent-map | Do not restart services |
| Small-test style: stable boundary, data over mocks, `makeMe` | `unit-testing.mdc` | If Vitest needed, drive RecallPage/AnsweredSpelling boundary |
| Frontend: Vitest browser mode; `mockSdkService`; no getByRole | `frontend-testing.mdc` | Optional Wave 1 only |
| E2E: targeted `--spec`; capability-named; page objects; `waitUntilAppIsNotBusy` | `e2e-authoring.mdc` | Spec `accidental_match_reveal.feature` (+ `overlap_try_again` regression if touched) |
| No phase numbers in product tests | `planning.mdc` | Scenario names by capability |
| After phase: Jidoka, post-change-refactor, commit+push | `planning.mdc` / gsd-coexistence | Wrap-up on execute |
| ADR 0003: no ACCIDENTAL_MATCH / OVERLAP / SRS change | architecture-decisions + CONTEXT | UI/session only |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Production KeepAlive include reliably matches `RecallPage` via filename `__name` (no explicit `defineOptions` name today) | Architecture Patterns | If false, remount loses matches → need name harden or richer restore |
| A2 | History-back SPA navigation preserves KeepAlive cache in Cypress the same as real browsers when using in-app title click + `cy.go('back')` | E2E / Validation | Wrong return helper could false-fail or false-pass |
| A3 | No product code change required if KeepAlive path works | Summary / Waves | Planner should still budget a tiny contingency client fix |

**If this table is empty:** N/A — three assumptions logged above.

## Open Questions

1. **Does E2E KeepAlive survive title → `cy.go('back')` in this Cypress harness?**
   - What we know: In-app `router.push` preserves SPA; full `cy.visit` remounts.
   - What's unclear: Only E2E execution confirms.
   - Recommendation: Implement scenario; if red, inspect whether remount or cursor clear happened before any API work.

2. **Multi-match fixture in E2E?**
   - What we know: Unit fixture `accidentalMatchWithTwoMatchedNotes` exists; current accidental-match E2E uses single match (sedition/sedation).
   - What's unclear: Whether Phase 12 E2E must assert multi-match identity on reopen or single-match suffices for AMR-05.
   - Recommendation: Discretion — single-match reopen satisfies AMR-05; extend multi-match only if open/dismiss gaps remain (D-07).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | Vitest / tooling | ✓ | v24.5.0 | — |
| pnpm | scripts | ✓ | 11.20.0 | — |
| Nix develop | repo commands | ✓ (assumed local) | — | Cloud VM skill if missing |
| `pnpm sut` (backend+frontend) | E2E | Assume running | — | healthcheck; do not restart |
| Cypress | AMR-05 E2E | via Nix/pnpm | in-repo | — |

**Missing dependencies with no fallback:** none identified for this phase

**Missing dependencies with fallback:** none

Step 2.6: External deps are existing monorepo tooling only — no new services.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.10 (frontend) + Cypress/Cucumber (e2e_test) |
| Config file | `frontend/vitest.config.ts`; `e2e_test/config/` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.spec.ts` (only if Wave 1) |
| Full suite command (targeted) | `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature,e2e_test/features/recall/overlap_try_again.feature` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AMR-05 | Reopen resolve after title navigate + history back; same matches | e2e | `pnpm cypress run --spec e2e_test/features/recall/accidental_match_reveal.feature` | ❌ Wave 0 — scenario missing |
| AMR-05 guard | CTA + matchedNotes survive KeepAlive deactivate/activate | unit (optional) | `pnpm frontend:test tests/pages/RecallPage.spec.ts` (new case) | ❌ only if client fix needed (D-08) |
| D-07 | open/dismiss + multi-match identity stay green | e2e (existing asserts in page object) | same accidental_match spec | ✅ partial — `expectAccidentalMatchReveal` already open/dismiss |
| D-07 | overlap try-again uncoupled | e2e | `pnpm cypress run --spec e2e_test/features/recall/overlap_try_again.feature` | ✅ |

### Sampling Rate
- **Per task commit:** targeted Vitest file if touched; else accidental_match E2E scenario under `@wip` until green
- **Per wave merge:** accidental_match + overlap_try_again specs
- **Phase gate:** both specs green; remove `@wip` from reopen scenario

### Wave 0 Gaps
- [ ] Capability-named E2E scenario for reopen-after-title-navigate in `accidental_match_reveal.feature` (or adjacent capability-named feature — no phase numbers)
- [ ] `AnsweredQuestionPage` helpers: open resolve → click matched title → history back → reopen + assert same match titles
- [ ] Optional: KeepAlive remount Vitest **only if** a client fix is introduced

*(Existing open/dismiss coverage lives inside `expectAccidentalMatchReveal`; extend rather than rewrite.)*

### Recommended plan waves (for planner)

| Wave | Type | Work | When |
|------|------|------|------|
| **1 (preferred)** | Behavior / E2E | Page-object helpers + `@wip` then green reopen scenario; assert Resolve CTA + same match title(s) after history back; keep overlap green | Default — research shows no required product code |
| **1b (contingency)** | Tiny client fix | If E2E shows remount: harden `defineOptions({ name: 'RecallPage' })` and/or restore cursor after activate — **still no OpenAPI** | Only if Wave 1 red for remount/cursor reasons |
| **Vitest (optional)** | Skip unless 1b | Small KeepAlive harness proving CTA + `matchedNotes` after deactivate/activate | D-08: skip if pure E2E enough |
| **OpenAPI enrichment** | Out of scope | Enrich `previouslyAnswered` `matchedNotes` | Only if 1b insufficient (D-04 escalation) |

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Existing session; no new auth |
| V3 Session Management | no | No new session tokens |
| V4 Access Control | yes (unchanged) | Existing note show / recall auth; titles use same routes |
| V5 Input Validation | no new inputs | No new user-authored fields |
| V6 Cryptography | no | — |

### Known Threat Patterns for recall navigate/reopen

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Privilege escalation via remount restore | Elevation | Do not invent client-only match lists from untrusted IDs; prefer live graded payload already returned by authenticated answer API |
| Leaking other users’ matches via history API | Information disclosure | Any future `previouslyAnswered` enrichment must stay user-scoped (existing RecallService) — not this phase |
| XSS via match titles in dialog | Tampering | Existing title rendering paths; no new HTML sinks |

`security_enforcement: true` in `.planning/config.json` — phase is UI/E2E; no new attack surface beyond existing router navigation.

## Sources

### Primary (HIGH confidence)
- `frontend/src/DoughnutApp.vue` — KeepAlive include RecallPage
- `frontend/src/pages/RecallPage.vue` — previousAnsweredQuestions, cursor, resume clear, previouslyAnswered on mount
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — Resolve CTA gated on matchedNotes
- `frontend/src/components/commons/Modal.vue` — close on route.fullPath
- `backend/.../RecallsController.java` — previouslyAnswered → `AnsweredQuestion::from`
- `backend/.../dto/AnsweredQuestion.java` — single-arg vs two-arg `from`
- `backend/.../entities/Answer.java` — `@Transient matchedNoteId`
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — open/dismiss helpers
- `.planning/research/PITFALLS.md` — Pitfall 6
- `.planning/phases/12-.../12-CONTEXT.md` — locked decisions

### Secondary (MEDIUM confidence)
- Context7 `/vuejs/vue` — script-setup `__name` filename inference; KeepAlive include matching `name || __name`
- `.planning/research/ARCHITECTURE.md` / `SUMMARY.md` — avoid API enrichment unless required

### Tertiary (LOW confidence)
- Assumption that Cypress `cy.go('back')` after in-dialog title click mirrors production KeepAlive — validate in E2E execution

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions from `frontend/package.json`; no new packages
- Architecture: HIGH — remount/`matchedNotes` fidelity verified in source; KeepAlive matching MEDIUM via Context7
- Pitfalls: HIGH — Pitfall 6 + verified Resume vs history-back divergence

**Research date:** 2026-08-05
**Valid until:** 2026-09-04 (stable in-repo behavior; re-check if KeepAlive/router shell changes)
