# Phase 03: Potential learning sessions - Research

**Researched:** 2026-08-08
**Domain:** Due COMMISSIONED memory trackers → recall-page potential learning sessions (API DTO + frontend grouping + E2E)
**Confidence:** HIGH

## Summary

Phase 3 is a **Behavior** slice: due commissioned memory trackers must surface as **potential learning sessions** (grouped by notebook) on the recall progress-bar area, while ordinary recall counts stay COMMISSIONED-free. Ordinary exclusion already exists via `byUserIdFrom` (`type <> 'COMMISSIONED'`) from Phase 1 — this phase adds a **positive** due-COMMISSIONED feed on the same recalling/`DueMemoryTrackers` round-trip, then derives sessions in the frontend (no PLS persistence).

**Primary recommendation:** Add `dueCommissioned: List<DueCommissionedMemoryTrackerLite>` (flat lites with `memoryTrackerId`, `notebookId`, `notebookName`) to `DueMemoryTrackers`; query via a sibling repository fragment `type = 'COMMISSIONED'` + same `next_recall_at` cutoff as ordinary due; group by `notebookId` in `useRecallData` / `RecallProgressBar` as display-only UI; graduate the two draft E2E scenarios under `@wip` until green.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Due-commissioned data feed
- **D-01:** Ordinary `toRepeat` remains COMMISSIONED-free (Phase 1). Expose
  **due commissioned trackers** (with enough notebook identity for grouping and
  E2E notebook-title assertions) on the existing recalling / `DueMemoryTrackers`
  load path so the recall page gets them in one round-trip. — **Reversibility:**
  reversible — additive DTO field; no persistence of Potential Learning Session
  entities
- **D-02:** A **potential learning session** is **derived in the frontend** by
  grouping those due commissioned trackers **by notebook** (Phase 1 lifecycle
  lock). Do **not** persist Potential Learning Session rows in this phase. A
  Learning Session entity exists only once commissioned (Phase 4–5)

#### Progress-bar presentation
- **D-03:** On the recall page **top progress bar** area, show potential
  learning session(s) **by notebook name** so E2E can assert
  “1 potential learning session to commission for notebook {title}”. Use
  glossary wording (**potential learning session**). — **Reversibility:**
  reversible — UI presentation only
- **D-04:** Phase 3 is **display-only**. Do **not** open the commission dialog
  or create a Learning Session when the learner interacts with a potential
  session. Commission UI + Request belong to Phases 4–5 (milestone CONTEXT:
  dialog from progress bar)

#### Ordinary recall separation
- **D-05:** Ordinary recall count, progress finished/`toRepeatCount`, and nav
  recall badge stay **ordinary-only**. Potential sessions are a **separate**
  affordance and must not inflate ordinary recall numbers (TRK-03 /
  success criterion 1)

#### E2E scope
- **D-06:** Graduate into `e2e_test/features/learning_session/` (tag `@wip`
  until green) only the two Phase 3 scenarios from the draft feature:
  “Due commissioned trackers await a Tutor rather than ordinary recall” and
  “Notes from different notebooks are commissioned as separate learning
  sessions”. Do not graduate commission / record / amend scenarios in this phase

### Claude's Discretion
- Exact DTO field shape (flat due-commissioned lites vs pre-grouped notebook
  summaries) as long as D-01/D-02 hold and notebook title is assertable
- Exact DaisyUI / progress-bar slot markup and `data-testid` / `data-test` ids
  (page-object friendly; match existing recall progress patterns)
- Whether due-commissioned query reuses a sibling of `byUserIdFrom` that
  **selects only** COMMISSIONED vs a dedicated repository method — smallest
  coherent extension of `RecallService.getDueMemoryTrackers`

### Deferred Ideas (OUT OF SCOPE)
- Commission dialog from progress bar + Learning Session create — Phase 4–5
  (COM-*)
- Learning Session Request markdown — Phase 5 (ADR 0005)
- Record / amend report — Phases 6–7 (REC-*, AMD-01)
- Feedback score on tracker — REC-03 (Phase 6)

None beyond roadmap — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TRK-03 | Due commissioned memory trackers do not appear as ordinary recall work | Keep Phase 1 `byUserIdFrom` exclusion; assert ordinary `toRepeat` / badge stay empty when only COMMISSIONED due; E2E “0 notes to recall” |
| POT-01 | User sees potential learning sessions (due commissioned trackers grouped by notebook) from the recall page progress bar | Additive `dueCommissioned` on recalling path; frontend group-by-notebook; progress-bar display with glossary copy |
| POT-02 | Due commissioned trackers from different notebooks form separate potential learning sessions | Flat lites carry `notebookId`/`notebookName`; group key = notebook; multi-notebook E2E scenario |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Due COMMISSIONED selection | API / Backend | Database / Storage | Same ownership as ordinary due-recall; user-scoped native query |
| Ordinary due exclusion (unchanged) | API / Backend | Database / Storage | Phase 1 `byUserIdFrom` must remain; do not “fix” by removing filter |
| Wire shape for due commissioned + notebook identity | API / Backend | — | Additive field on `DueMemoryTrackers`; OpenAPI → generated client |
| Potential learning session derivation (group by notebook) | Browser / Client | — | Locked D-02 — frontend-only concept this phase |
| Progress-bar presentation (display-only) | Browser / Client | — | D-03/D-04 — recall GlobalBar / `RecallProgressBar` |
| Nav badge / `toRepeatCount` ordinary-only | Browser / Client | API / Backend | D-05 — badge from `toRepeat` length only |
| E2E observability | Browser / Client | — | Cucumber + page objects; `@wip` until green |

## Standard Stack

### Core
| Library / layer | Version | Purpose | Why Standard |
|-----------------|---------|---------|--------------|
| Spring Boot + Spring Data JPA | existing backend | Controller → `RecallService` → `MemoryTrackerRepository` | Existing recalling path `[VERIFIED: backend/.../RecallsController.java:39-51]` |
| OpenAPI → `pnpm generateTypeScript` | repo skill | Sync `DueMemoryTrackers` TS types / SDK | Never hand-edit generated client `[VERIFIED: .cursor/agent-map.md:24-28]` |
| Vue 3 Composition API | existing frontend | `computed` grouping + progress-bar UI | Repo pattern; Vue `computed` for derived lists `[CITED: Context7 /vuejs/vue TodoMVC filteredTodos]` |
| Vitest 4.1.10 | frontend | Component/page unit tests | `[VERIFIED: frontend/package.json vitest]` |
| Cypress 15.20.0 + Cucumber | e2e_test | Graduate draft scenarios | `[VERIFIED: package.json cypress 15.20.0]` |

### Supporting
| Library / asset | Version | Purpose | When to Use |
|-----------------|---------|---------|-------------|
| `doughnut-test-fixtures` `DueMemoryTrackersBuilder` | workspace | Frontend fixture payloads | Extend for `dueCommissioned` |
| Backend `makeMe.aMemoryTrackerFor(...).commissioned()` | test | Controller fixtures | Already used in `RecallsControllerTests` |
| DaisyUI classes (`daisy-btn`, `daisy-alert`, …) | existing | Progress-bar adjacent markup | Match recall UI |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Flat `dueCommissioned` lites + FE group | Backend pre-grouped notebook summaries | Violates spirit of D-02 (derive in FE); harder Phase 4 reuse of tracker ids |
| Enrich `MemoryTrackerLite` with notebook fields | Companion lite type | Pollutes ordinary quiz queue type with unused notebook fields |
| New `/api/recalls/potential-sessions` endpoint | Extend `DueMemoryTrackers` | Extra round-trip; conflicts with D-01 one-load-path |

**Installation:** None — no new npm/PyPI packages.

**Version verification:** N/A (no new packages). Existing Vitest/Cypress versions read from package manifests this session.

## Package Legitimacy Audit

> No external packages installed this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
Learner (recall page / menu)
        │
        ▼
GET /api/recalls/recalling  (also menu: UserController → getDueMemoryTrackers)
        │
        ▼
RecallService.getDueMemoryTrackers
        ├── ordinary stream → byUserIdFrom (type <> 'COMMISSIONED') → toRepeat
        └── commissioned stream → byUserIdCommissionedFrom (type = 'COMMISSIONED')
                                  + next_recall_at cutoff → dueCommissioned
                                        │
                                        ▼ map MemoryTracker → DueCommissionedMemoryTrackerLite
                                           (id, notebookId, notebookName via note.notebook)
        │
        ▼
DueMemoryTrackers { toRepeat, dueCommissioned, totalAssimilatedCount, ... }
        │
        ▼
useRecallPageLoading / MainMenu → useRecallData
        ├── toRepeat → toRepeatCount → nav badge + ProgressBar (ordinary-only)
        └── dueCommissioned → computed groupBy notebookId
                │
                ▼
RecallProgressBar (display-only potential learning session rows)
```

### Recommended Project Structure (touch points)

```
backend/src/main/java/com/odde/doughnut/
├── controllers/dto/
│   ├── DueMemoryTrackers.java          # add dueCommissioned
│   └── DueCommissionedMemoryTrackerLite.java  # new companion DTO
├── entities/repositories/MemoryTrackerRepository.java  # COMMISSIONED sibling query
└── services/RecallService.java         # fill dueCommissioned alongside toRepeat

frontend/src/
├── composables/useRecallData.ts        # store + group computed
├── composables/useRecallPageLoading.ts # set from recalling response
├── components/toolbars/MainMenu.vue    # set dueCommissioned from recallStatus (badge unchanged)
├── components/recall/RecallProgressBar.vue  # render potential sessions
└── pages/RecallPage.vue                # pass props

packages/doughnut-test-fixtures/src/DueMemoryTrackersBuilder.ts
e2e_test/features/learning_session/commissioned_learning_session.feature
e2e_test/step_definitions/ + start/pageObjects/recallPage.ts
```

### Pattern 1: Additive field on existing recalling DTO (D-01)
**What:** Extend `DueMemoryTrackers` with a list of due COMMISSIONED lites; leave `toRepeat` semantics unchanged.
**When to use:** Always for this phase — locked one round-trip.
**Example (recommended shape):**

```java
// Source: research recommendation aligned to existing DueMemoryTrackers
// [VERIFIED: backend/.../DueMemoryTrackers.java:9-14]
// existing fields: totalAssimilatedCount, currentRecallWindowEndAt, toRepeat, dueInDays
@Getter @Setter private List<DueCommissionedMemoryTrackerLite> dueCommissioned;
```

```java
// Companion DTO — do not overload MemoryTrackerLite
// [VERIFIED: backend/.../MemoryTrackerLite.java:9-16]
// MemoryTrackerLite today: memoryTrackerId, spelling, propertyKey
public class DueCommissionedMemoryTrackerLite {
  private int memoryTrackerId;
  private int notebookId;
  private String notebookName; // Notebook.getName() — E2E assertable title
}
```

### Pattern 2: Sibling native-query fragment (Claude discretion)
**What:** Mirror `byUserIdFrom` but **select only** COMMISSIONED; do not edit the ordinary fragment.
**When to use:** Smallest coherent extension of `RecallService.getDueMemoryTrackers`.

```java
// Source: sibling of [VERIFIED: MemoryTrackerRepository.java:64-69]
// byUserIdFrom ends with: AND rp.type <> 'COMMISSIONED'
String byUserIdCommissionedFrom =
    " FROM memory_tracker rp "
        + " WHERE rp.user_id = :userId "
        + "   AND rp.removed_from_tracking IS FALSE "
        + "   AND rp.deleted_at IS NULL "
        + "   AND rp.type = 'COMMISSIONED' ";
```

Use the **same** `next_recall_at <= :nextRecallAt` cutoff as ordinary due (including `dueInDays` expansion via existing `TimestampOperations.addHoursToTimestamp` in `RecallService.getMemoryTrackersNeedToRepeat`).

### Pattern 3: Frontend-derived potential sessions (D-02)
**What:** `computed` that groups `dueCommissioned` by `notebookId` into one potential session per notebook.
**When to use:** Always — locked lifecycle.

```typescript
// Source: Vue Composition API computed derived lists
// [CITED: Context7 /vuejs/vue — filteredTodos / remaining computed]
const potentialLearningSessions = computed(() => {
  const byNotebook = new Map<number, { notebookId: number; notebookName: string; trackerIds: number[] }>()
  for (const t of dueCommissioned.value ?? []) {
    const existing = byNotebook.get(t.notebookId)
    if (existing) existing.trackerIds.push(t.memoryTrackerId)
    else
      byNotebook.set(t.notebookId, {
        notebookId: t.notebookId,
        notebookName: t.notebookName,
        trackerIds: [t.memoryTrackerId],
      })
  }
  return [...byNotebook.values()]
})
```

### Pattern 4: Display-only progress-bar affordance (D-03/D-04)
**What:** Render one row/chip per potential session in/near `RecallProgressBar` `#buttons` or a sibling under `GlobalBar`, using glossary copy. No dialog / no Learning Session create.
**Recommended testid:** `data-test="potential-learning-session"` with notebook name visible in text (page-object friendly). Exact DaisyUI markup is discretionary; prefer existing `daisy-btn` / text patterns over new card chrome.

### Anti-Patterns to Avoid
- **Removing `type <> 'COMMISSIONED'` from `byUserIdFrom`:** Breaks Phase 1 contract; TRK-03 needs exclusion + positive feed, not filter deletion.
- **Persisting Potential Learning Session entities:** Forbidden by D-02 / Phase 1 lifecycle lock.
- **Click → commission dialog:** Phase 4–5 (D-04).
- **Inflating `toRepeatCount` / nav badge with commissioned:** Violates D-05.
- **Pre-grouping only on the backend and calling that the “potential session” API:** Conflicts with D-02; keep API as due trackers + notebook identity.
- **Hand-editing `packages/generated/doughnut-backend-api/**`:** Regenerate via `generate-api-client` / `pnpm generateTypeScript`.
- **Phase numbers in product artifact names:** Capability names only (`potential-learning-session`, `dueCommissioned`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| API TypeScript client | Manual `types.gen.ts` edits | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` | Generated from OpenAPI; hand edits drift |
| Auth on recalling | New auth middleware | Existing `authorizationService.assertLoggedIn()` | Already on `RecallsController.recalling` |
| Ordinary due selection | Parallel custom due engine | Existing `getMemoryTrackersNeedToRepeat` / `byUserIdFrom` | Proven Phase 1 SC3 |
| E2E commissioned fixtures | Raw SQL inject | `AssimilationController.assimilate` with `assimilateAsCommissioned: true` (+ time travel) | Create path already exists Phase 2 |
| Frontend grouping library | lodash groupBy dependency | Small `computed` Map by `notebookId` | No new packages; D-02 is trivial |

**Key insight:** The hard part is keeping **two parallel due feeds** coherent on one DTO without leaking COMMISSIONED into ordinary counts — not inventing a new session store.

## Common Pitfalls

### Pitfall 1: Touching `byUserIdFrom` while adding COMMISSIONED feed
**What goes wrong:** Ordinary assimilation counts / due lists regress.
**Why it happens:** Temptation to “generalize” the fragment.
**How to avoid:** Add `byUserIdCommissionedFrom` sibling; leave ordinary fragment literal intact.
**Warning signs:** `shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists` fails; `totalAssimilatedCount` includes COMMISSIONED.

### Pitfall 2: Badge / progress bar count inflated by potential sessions
**What goes wrong:** Sidebar shows N recalls when only commissioned work is due (fails TRK-03 E2E).
**Why it happens:** Deriving badge from combined lists or mutating `toRepeat`.
**How to avoid:** Keep `toRepeatCount` computed solely from `toRepeat` `[VERIFIED: useRecallData.ts:17-21]`; store `dueCommissioned` separately.
**Warning signs:** `I should see that I have 0 notes to recall` fails after commissioned assimilate.

### Pitfall 3: Missing notebook identity on lite
**What goes wrong:** FE cannot group or E2E cannot assert notebook title.
**Why it happens:** Reusing bare `MemoryTrackerLite` (`memoryTrackerId`, `spelling`, `propertyKey` only) `[VERIFIED: MemoryTrackerLite.java:9-16]`.
**How to avoid:** Companion DTO with `notebookId` + `notebookName` from `mt.getNote().getNotebook()` (both associations default EAGER `@ManyToOne` without LAZY on note→notebook path) `[VERIFIED: MemoryTracker.java:40-45]`, `[VERIFIED: Note.java:32-36]`, `[VERIFIED: Notebook.java:86-91]`.

### Pitfall 4: Lazy/session issues when mapping notebook name
**What goes wrong:** LazyInitializationException outside transaction.
**Why it happens:** Mapping after transaction ends.
**How to avoid:** Map inside `RecallService.getDueMemoryTrackers` while controller `@Transactional` is active `[VERIFIED: RecallsController.java:39-40]`.

### Pitfall 5: E2E asserts potential sessions without visiting recall UI
**What goes wrong:** Step looks at sidebar only; progress-bar copy never appears.
**Why it happens:** Existing `I should see that I have {int} notes to recall` uses `cy.reload()` + sidebar badge `[VERIFIED: e2e_test/step_definitions/recall.ts:52-57]`, `[VERIFIED: recallPage.ts:144-148]`.
**How to avoid:** Potential-session step navigates to recall page (or assumes progress bar visible) and asserts `data-test` / visible glossary text.

### Pitfall 6: Graduating too many draft scenarios / `@wip` cap
**What goes wrong:** CI `@wip` cap (5) exceeded or commission scenarios fail early.
**Why it happens:** Copying whole draft feature.
**How to avoid:** D-06 — only two scenarios; scenario-level `@wip` until green. Current `@wip` count is 0 `[VERIFIED: scripts/check_wip_tags.sh run this session]`.

### Pitfall 7: Forgetting OpenAPI regenerate
**What goes wrong:** Frontend types miss `dueCommissioned`; compile/test fail.
**How to avoid:** After Java DTO change, run generate-api-client skill / `pnpm generateTypeScript`; extend `DueMemoryTrackersBuilder`.

### Pitfall 8: Shuffle / loadMore dropping commissioned payload
**What goes wrong:** `useRecallPageLoading` shuffles `toRepeat` and never sets `dueCommissioned` `[VERIFIED: useRecallPageLoading.ts:51-64]`.
**How to avoid:** Always `setDueCommissioned(response.dueCommissioned)` from the same response; do not shuffle commissioned list into quiz queue.

## Code Examples

### Backend: extend getDueMemoryTrackers (sketch)

```java
// Source: extend [VERIFIED: RecallService.java:47-71]
public DueMemoryTrackers getDueMemoryTrackers(...) {
  // existing toRepeat mapping unchanged...
  List<DueCommissionedMemoryTrackerLite> dueCommissioned =
      getCommissionedMemoryTrackersNeedToRepeat(user, currentUTCTimestamp, timeZone, dueInDays)
          .map(mt -> {
            DueCommissionedMemoryTrackerLite lite = new DueCommissionedMemoryTrackerLite();
            lite.setMemoryTrackerId(mt.getId());
            Notebook nb = mt.getNote().getNotebook();
            lite.setNotebookId(nb.getId());
            lite.setNotebookName(nb.getName());
            return lite;
          })
          .toList();
  dueMemoryTrackers.setDueCommissioned(dueCommissioned);
  return dueMemoryTrackers;
}
```

### Controller test focus (small-test style)

```java
// Drive RecallsController.recalling — assert deltas only
// Canonical ordinary exclusion already in shouldExcludeCommissionedMemoryTrackersFromOrdinaryRecallLists
// New test: only commissioned due → toRepeat empty AND dueCommissioned has notebookName
@Test
void shouldExposeDueCommissionedTrackersGroupedByNotebookIdentity() {
  // makeMe commissioned trackers due; assert getToRepeat() empty;
  // assert dueCommissioned notebookName equals notebook.getName()
}
```

### Frontend progress-bar copy (E2E-aligned)

```vue
<!-- Display-only; glossary wording from draft feature -->
<div
  v-for="session in potentialLearningSessions"
  :key="session.notebookId"
  data-test="potential-learning-session"
>
  1 potential learning session to commission for notebook "{{ session.notebookName }}"
</div>
```

Draft step wording to support `[VERIFIED: .planning/phases/01-.../commissioned_learning_session.feature:22-26,38-46]`:

```gherkin
Then I should see 1 potential learning session to commission for notebook "Spanish conversation"
```

### E2E fixture helper

```typescript
// Extend testability — AssimilationRequestDTO already has assimilateAsCommissioned
// [VERIFIED: AssimilationRequestDTO.java:7]
assimilateNoteAsCommissioned(noteTitle: string) {
  return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
    cy.wrap(
      AssimilationController.assimilate({
        body: { noteId, skipMemoryTracking: false, assimilateAsCommissioned: true },
      }),
      { log: false }
    )
  )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| COMMISSIONED excluded only (invisible) | Exclude + positive `dueCommissioned` feed | Phase 3 | Enables potential sessions UI |
| Potential session = future entity | Frontend-derived grouping | Phase 1 CONTEXT lock | No PLS table this phase |
| Draft feature only under `.planning/` | Graduate 2 scenarios to `e2e_test/features/learning_session/` | Phase 3 | E2E proves TRK-03/POT-* |

**Deprecated/outdated:**
- Treating TRK-03 as “already done” solely because of Phase 1 exclusion without user-visible potential sessions — Phase 3 E2E is the product proof for the recall page.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Field name `dueCommissioned` / type name `DueCommissionedMemoryTrackerLite` / `notebookName` are the preferred identifiers (discretion) | Architecture Patterns | Planner/executor may rename; keep OpenAPI + FE + fixtures in sync |
| A2 | Same `dueInDays` cutoff should apply to commissioned as ordinary when using Load more | Pattern 2 | If product wants only current-window commissioned, loadMore would over-include — confirm only if UX disagrees |
| A3 | E2E copy uses singular “session” even when count is parameterized (`{n} potential learning session`) matching draft feature | Code Examples / D-03 | Step regex must match exact UI string |

**If renaming A1:** still flat lites + FE group; only identifiers change.

## Open Questions

1. **Exact UI placement inside vs beside ProgressBar**
   - What we know: D-03 says top progress bar area; `RecallProgressBar` has `#buttons` slot inside `ProgressBar` `[VERIFIED: RecallProgressBar.vue:1-56]`.
   - What's unclear: Whether rows sit in `#buttons`, above the bar, or as a sibling in `GlobalBar`.
   - Recommendation: Sibling strip under/near the bar (or `#buttons` column) with `data-test="potential-learning-session"` — keep ordinary progress math untouched.

2. **Click affordance styling**
   - What we know: D-04 display-only — no dialog.
   - What's unclear: `<div>` vs disabled-looking `<button>` for Phase 5 reuse.
   - Recommendation: Non-submitting element with stable test id; Phase 5 can promote to open-dialog control without changing copy.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix shell tooling | All repo commands | ✓ | local | Cloud VM skill if absent |
| `pnpm` | generateTypeScript / tests | ✓ | 11.20.0 | — |
| `pnpm sut` (MySQL/backend/frontend) | E2E / backend tests | assume running per agent-map | — | `pnpm sut:healthcheck` |
| New npm packages | — | N/A | — | — |

**Missing dependencies with no fallback:** none identified for this phase.

**Step 2.6:** External tools are the existing doughnut stack only — no new CLIs.

## Validation Architecture

> `workflow.nyquist_validation` is true in `.planning/config.json` `[VERIFIED: .planning/config.json:24]`.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit (backend) + Vitest 4.1.10 (frontend) + Cypress 15.20.0 / Cucumber (E2E) |
| Config file | backend Gradle tests; `frontend/vitest.config.ts`; `e2e_test/config/ci.ts` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` and `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.spec.ts` (plus new progress-bar spec) |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm backend:verify`; targeted E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| TRK-03 | Only commissioned due → ordinary `toRepeat` empty / badge 0 | unit (controller) + E2E | `pnpm backend:test_only` (RecallsControllerTests); E2E scenario 1 | ✅ extend existing controller test; ❌ E2E scenario not graduated |
| POT-01 | Progress bar shows potential session by notebook name | unit (RecallProgressBar / RecallPage) + E2E | `pnpm frontend:test tests/...`; E2E | ❌ Wave 0 UI tests; ❌ E2E step |
| POT-02 | Two notebooks → two potential sessions | unit + E2E | same | ❌ Wave 0; ❌ E2E scenario 2 |

### Sampling Rate
- **Per task commit:** targeted backend controller test and/or frontend component test for touched surface
- **Per wave merge:** `pnpm backend:test_only` + relevant `pnpm frontend:test …` + E2E `--spec` for learning_session feature when scenarios exist
- **Phase gate:** Those greens; remove `@wip` when both Phase 3 scenarios pass; do not run full E2E suite unless required

### Wave 0 Gaps
- [ ] Backend controller assertion for **positive** `dueCommissioned` payload (ordinary exclusion test exists; positive feed does not)
- [ ] `DueMemoryTrackersBuilder` + frontend fixture support for `dueCommissioned`
- [ ] Frontend unit test: mount recall progress UI with due commissioned lites → assert notebook-titled potential session rows; assert `toRepeatCount` unchanged
- [ ] E2E: graduate two scenarios; add Given steps for bulk “assimilated as commissioned on day N”; add Then for potential learning session; page-object method
- [ ] Extend `testability.assimilateNote*` for `assimilateAsCommissioned: true`

*(Framework install: none — infrastructure exists)*

## Security Domain

> `security_enforcement` enabled (`true`) `[VERIFIED: .planning/config.json:48]`.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Existing session login; `assertLoggedIn()` on recalling |
| V3 Session Management | yes (unchanged) | Existing Spring session scope controller |
| V4 Access Control | yes | Native queries filter `user_id = :userId` — never return other users’ trackers |
| V5 Input Validation | yes (light) | Existing timezone / dueindays params; new field is response-only |
| V6 Cryptography | no | No new crypto |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| IDOR on memory trackers | Elevation of Privilege | User-scoped repository queries only |
| Inflating/leaking other notebooks’ titles | Information Disclosure | Only notebooks reachable via the user’s due COMMISSIONED trackers |
| XSS via notebook name in progress bar | Tampering | Vue text interpolation (default escape); do not `v-html` notebook names |

## Project Constraints (from .cursor/rules/)

| Directive | Implication for this phase |
|-----------|----------------------------|
| `planning.mdc` — Behavior vs Structure; one observable behavior; stop-safe | Single Behavior phase delivering potential sessions visible + ordinary empty of commissioned |
| `planning.mdc` — `@wip` until E2E green; CI `@wip` cap 5 | Tag only the two graduated scenarios |
| `planning.mdc` — targeted E2E `--spec`, not full suite | Spec learning_session feature |
| `unit-testing.mdc` — small tests via controller/page boundary | `RecallsController.recalling` + mounted recall progress UI; data over mocks |
| `backend-testing.mdc` — prefer controller tests + real DB | Extend `RecallsControllerTests` |
| `frontend-testing.mdc` — `data-testid` / browser Vitest; mock only SDK | Drive `RecallProgressBar`/`RecallPage` with makeMe fixtures |
| `e2e-authoring.mdc` — page objects; capability-named features; `waitUntilAppIsNotBusy` | Steps thin; assert via recall page object |
| `frontend-api.mdc` — wrapped `{ data, error }`; no hand-edit generated client | Keep `useRecallPageLoading` pattern |
| `backend-code.mdc` — DTO when wire shape differs | Companion lite justified |
| `general.mdc` — Nix prefix for tooling; git without Nix; no phase numbers in product | Capability names |
| `gsd-coexistence.mdc` — local wrap-up on execute | Jidoka → refactor → plan update → commit+push when executing |
| `error-handling.mdc` — never swallow | Propagate API errors; don’t hide missing dueCommissioned as empty without distinguishing load failure |
| `architecture-decisions.mdc` / adr-awareness | Glossary terms from ADR 0001 §3; ADR 0005 Proposed — guide protocol but no Request this phase |

## Sources

### Primary (HIGH confidence)
- In-repo reads: `03-CONTEXT.md`, `DueMemoryTrackers.java`, `MemoryTrackerLite.java`, `RecallService.java`, `MemoryTrackerRepository.java`, `RecallsController.java`, `RecallsControllerTests.java`, `useRecallData.ts`, `useRecallPageLoading.ts`, `RecallProgressBar.vue`, `ProgressBar.vue`, `useNavigationItems.ts`, `MainMenu.vue`, draft + graduated feature files, ADR 0001 §3 / ADR 0005, Phase 1 VERIFICATION
- `.planning/config.json` — nyquist + security flags
- `scripts/check_wip_tags.sh` — current `@wip` count 0

### Secondary (MEDIUM confidence)
- Context7 `/vuejs/vue` — Composition API `computed` for derived filtered lists

### Tertiary (LOW confidence)
- None material; discretionary naming (A1) flagged in Assumptions Log

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — reuse existing doughnut recalling stack; no new packages
- Architecture: HIGH — seams and locked decisions verified in source this session
- Pitfalls: HIGH — exclusion/badge/OpenAPI/E2E traps grounded in current code

**Research date:** 2026-08-08
**Valid until:** 2026-09-07 (stable domain; re-check if recalling DTO or progress bar refactors land)
