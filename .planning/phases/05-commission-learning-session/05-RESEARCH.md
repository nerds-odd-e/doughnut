# Phase 05: Commission Learning Session - Research

**Researched:** 2026-08-08
**Domain:** Recall progress-bar commission UI + copyable Learning Session Request (ADR 0005) wired to existing commission API
**Confidence:** HIGH

## Summary

Phase 5 is a **Behavior** slice: the learner commissions a **Learning Session** from the recall progress bar, receives a **copyable Learning Session Request** (markdown per ADR 0005), and the persisted session is **awaiting the tutor's report**. Backend commission, Request builder, abandon-on-recommission, and learning-status aggregation are **already implemented** in Phase 4 — `POST /api/learning-sessions/commission` returns `{ learningSessionId, requestMarkdown, status }` with `status` `"AWAITING_REPORT"` or `"RECORDED"` `[VERIFIED: packages/generated/doughnut-backend-api/types.gen.ts:619-627]`.

Phase 5 work is **frontend + E2E**: promote the display-only potential-session strip (Phase 3) into an interactive commission affordance, open a dialog from the recall progress bar (milestone CONTEXT), call `LearningSessionController.commission` via `apiCallWithLoading` + `timezoneParam()`, show the returned markdown in a readonly textarea with existing `CopyButton`, surface awaiting-report state for COM-03, and graduate the draft E2E scenario `"Commissioning a learning session produces a request for the tutor"` from `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` into `e2e_test/features/learning_session/commissioned_learning_session.feature` (remove `@wip` when green).

**Primary recommendation:** Add `CommissionLearningSessionDialog.vue` (Modal + textarea + CopyButton pattern from `AiRequestExportDialog.vue`), wire `RecallProgressBar` row to open it per notebook, commission with `{ blockUi: true, message: "Commissioning learning session…" }`, refresh recalling data via `requestDueRecallsRefresh()`, exclude trackers already in an `AWAITING_REPORT` session from `dueCommissioned` (backend filter in `RecallService` — planner discretion but strongly recommended), Vitest at mounted-component boundary + Cypress commission scenario with new step definitions/page-object methods.

<user_constraints>
## User Constraints (from CONTEXT.md)

**No Phase 5 `*-CONTEXT.md` exists yet.** Constraints below are locked from ROADMAP Phase 5, REQUIREMENTS.md (COM-*), Phase 1 milestone CONTEXT, Phase 3 UI-SPEC / CONTEXT, ADR 0005, and the draft `commissioned_learning_session.feature` — planner should treat these as binding until `/gsd-discuss-phase 5` adds a CONTEXT file.

### Locked Decisions

| Source | Decision |
|--------|----------|
| ROADMAP Phase 5 | **Behavior** — one observable: commission → Request + awaiting report |
| ROADMAP Phase 5 | Requirements **COM-01, COM-02, COM-03** |
| ROADMAP Phase 5 SC1 | User opens dialog from recall progress bar and commissions a notebook's potential session |
| ROADMAP Phase 5 SC2 | Request lists due Session Items with content, status, and scoring instruction |
| ROADMAP Phase 5 SC3 | Session in awaiting-report state after commission |
| ROADMAP Phase 5 SC4 | Commissioning abandons prior unfinished sessions / items without Feedback for that notebook |
| Phase 1 CONTEXT | UI surface: **dialog opened from a button on the recall page's top progress bar** |
| Phase 1 CONTEXT | Session lifecycle: potential session derived in FE; Learning Session exists only once commissioned; **old unfinished sessions and Session Items without Feedback are abandoned (deleted)** on new commission for the same notebook |
| Phase 1 CONTEXT | Protocol: ADR 0005 markdown; score→schedule ADR 0003 applies in Phase 6 only |
| Phase 3 D-02 | Potential learning session remains **frontend-derived** from `dueCommissioned`; no PLS persistence |
| Phase 3 D-03/D-04 | Potential sessions on/near progress bar; Phase 3 was display-only — **Phase 5 adds commission interaction** |
| Phase 3 D-05 | Ordinary recall counts / nav badge stay ordinary-only |
| Phase 3 UI-SPEC | DaisyUI + existing recall chrome; **primary accent (`daisy-btn-primary`) reserved for Phase 5 commission CTA** |
| Phase 3 UI-SPEC | Row marker `data-test="potential-learning-session"`; glossary copy: `1 potential learning session to commission for notebook "{name}"` |
| ADR 0005 | Request = notebook + inline 0–5 rubric + `### {note title}` sections with expected learning content + learning status |
| ADR 0005 | No session id in protocol documents; learner pastes Report into session later (Phase 6) |
| Phase 4 (executed) | `LearningSessionService.commission` + `LearningSessionController` POST `/api/learning-sessions/commission` — **no new commission API unless UX requires GET** |
| E2E draft | Scenario `"Commissioning a learning session produces a request for the tutor"` with steps for session items, learning status, content, rubric, awaiting report |

### Claude's Discretion

- Exact dialog markup (Modal vs daisy-modal), row affordance (whole row click vs explicit Commission button on row)
- Whether to exclude trackers in `AWAITING_REPORT` sessions from `dueCommissioned` (recommended — see Open Questions)
- Dialog copy strings beyond glossary terms (title, CTA label, awaiting banner)
- `data-test` / `data-testid` names for dialog controls (must be page-object friendly)
- Optional composable (`useCommissionLearningSession`) vs inline dialog logic
- Tracer vs expansion plan split (single tracer plan is viable if backend exclusion is one small task)

### Deferred Ideas (OUT OF SCOPE)

- Record report, parse Report, schedule trackers — Phase 6 (REC-*)
- Amend recorded session — Phase 7 (AMD-01)
- GET learning session by notebook / open-sessions list UI — not required for COM-01–03 MVP (commission response carries markdown)
- Descriptive feedback, smart request, MCP transport — v2 (PROT-*)
- Growing E2E for report-parse edge cases — unit tests only
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| COM-01 | User can commission a Learning Session for a notebook from a dialog opened on the recall page progress bar | Promote `RecallProgressBar` potential-session row to open `CommissionLearningSessionDialog`; call `LearningSessionController.commission({ body: { notebookId }, query: { timezone } })` |
| COM-02 | Commissioning produces a Learning Session Request (markdown per ADR 0005) listing Session Items with expected learning content, learning status, and 0–5 rubric | Display `response.requestMarkdown` in readonly textarea; backend builder already ADR-verbatim `[VERIFIED: LearningSessionControllerTests.java:48-70]` |
| COM-03 | After commissioning, the Learning Session awaits the Tutor's report | Show `response.status === 'AWAITING_REPORT'` in dialog; E2E step `the learning session should be awaiting the tutor's report` |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Commission mutation + Request markdown | API / Backend | — | **Already Phase 4** — `LearningSessionService.commission` |
| Abandon prior awaiting-report sessions | API / Backend | — | **Already Phase 4** — `abandonUnfinishedSessions` before insert |
| Open commission dialog from progress bar | Browser / Client | — | COM-01 user trigger on recall page |
| Call commission API + loading/error toasts | Browser / Client | API / Backend | `apiCallWithLoading` + generated SDK |
| Copyable Request display | Browser / Client | — | Textarea + `CopyButton` — no new clipboard lib |
| Awaiting-report affordance | Browser / Client | — | COM-03 visible status from commission response |
| `dueCommissioned` feed after commission | API / Backend | Browser / Client | Optional exclusion filter + `requestDueRecallsRefresh()` |
| Report recording / scheduling | API / Backend | — | **Phase 6** — out of scope |

## Standard Stack

### Core

| Library / layer | Version | Purpose | Why Standard |
|-----------------|---------|---------|--------------|
| Vue 3 + TypeScript | repo frontend | Dialog, progress-bar wiring | Existing recall page stack |
| `@generated/doughnut-backend-api/sdk.gen` | generated | `LearningSessionController.commission` | `[VERIFIED: sdk.gen.ts:901-911]` — never hand-edit |
| `apiCallWithLoading` | `@/managedApi/clientSetup` | Commission mutation + blockUi + error toast | `frontend-api.mdc` |
| `timezoneParam()` | `@/managedApi/window/timezoneParam` | Required query param on commission | Same as recalling path `[VERIFIED: types.gen.ts:2320-2326]` |
| `Modal` + `CopyButton` | existing commons | Request preview + copy | `AiRequestExportDialog.vue`, `ConversationExportDialog.vue` |
| DaisyUI | `daisy-` classes | Primary CTA, textarea, layout | Phase 3 UI-SPEC; `RecallProgressBar` already uses `text-base-content` |
| Vitest browser mode | repo | Component tests | `frontend-testing.mdc` |
| Cypress + Cucumber | repo | Commission E2E scenario | `e2e_test/features/learning_session/` |

### Supporting

| Asset | When to Use |
|-------|-------------|
| `useRecallData.requestDueRecallsRefresh` | After successful commission — reload `dueCommissioned` |
| `waitUntilAppIsNotBusy()` | E2E after commission click | `frontend-api.mdc` / `e2e-authoring.mdc` |
| `mockSdkService(LearningSessionController, "commission", …)` | Vitest commission flow |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| New commission API | Existing POST commission | Violates Phase 4 structure; response already has markdown |
| Custom markdown viewer | Readonly textarea | Matches export dialogs; E2E can assert textarea value |
| `navigator.clipboard` inline | `CopyButton` | Duplicates tested clipboard UX |
| GET session before show | Commission-on-open | Extra API; not in OpenAPI today |

**Installation:** None — no new npm/Maven packages.

**Version verification:** N/A (in-repo stack only).

## Package Legitimacy Audit

> No external packages installed this phase.

| Package | Verdict | Disposition |
|---------|---------|-------------|
| — | N/A | N/A |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
Recall page (RecallPage.vue)
  └── RecallProgressBar
        ├── potential-session row (per notebook)  [click / Commission CTA]
        │         └── opens CommissionLearningSessionDialog(notebookId, notebookName)
        └── ProgressBar (unchanged ordinary recall)

CommissionLearningSessionDialog
  ├── [pre-commission] Primary CTA "Commission learning session"
  │       └── apiCallWithLoading(() =>
  │             LearningSessionController.commission({
  │               body: { notebookId },
  │               query: { timezone: timezoneParam() },
  │             }),
  │           { blockUi: true, message: "Commissioning learning session…" })
  ├── [post-commission] readonly textarea ← response.requestMarkdown
  ├── CopyButton(text=requestMarkdown)
  ├── status banner ← response.status === "AWAITING_REPORT"
  └── on success → emit commissioned → parent requestDueRecallsRefresh()

POST /api/learning-sessions/commission  [EXISTING — Phase 4]
  └── LearningSessionService.commission
        ├── abandonUnfinishedSessions (AWAITING_REPORT for notebook)
        ├── persist session + session items
        └── LearningSessionRequestMarkdownBuilder.build → requestMarkdown

GET /api/recalls/recalling  [EXISTING]
  └── dueCommissioned → potentialLearningSessions (group by notebook)
        └── [RECOMMENDED Phase 5] exclude trackers in AWAITING_REPORT session items
```

### Recommended Project Structure

```text
frontend/src/components/recall/
  CommissionLearningSessionDialog.vue    # new — Modal + commission + request display
  RecallProgressBar.vue                  # promote row to open dialog; primary CTA

frontend/src/composables/
  useRecallData.ts                       # already has requestDueRecallsRefresh

frontend/tests/components/recall/
  CommissionLearningSessionDialog.spec.ts
  RecallProgressBar.spec.ts              # extend — click opens dialog / CTA visible

e2e_test/features/learning_session/
  commissioned_learning_session.feature  # add commission scenario (@wip until green)

e2e_test/step_definitions/
  learning_session.ts                    # new — commission + request assertions

e2e_test/start/pageObjects/
  recallPage.ts                          # commissionLearningSession(notebookTitle)

backend/src/main/java/com/odde/doughnut/services/
  RecallService.java                     # [recommended] filter dueCommissioned

backend/src/test/java/com/odde/doughnut/controllers/
  RecallsControllerTests.java            # assert awaiting-report exclusion if added
```

### Pattern 1: Export-style Request dialog

**What:** Reuse `Modal` + readonly `daisy-textarea` + `CopyButton` — same as `AiRequestExportDialog.vue` but content comes from commission response instead of fetch-on-mount.

**When to use:** Any copy-paste protocol document (ADR 0005 Request).

**Example:**

```vue
<!-- Pattern source: frontend/src/components/commons/AiRequestExportDialog.vue -->
<textarea
  class="daisy-textarea w-full h-96 bg-base-100 font-mono text-xs"
  readonly
  :value="requestMarkdown"
  data-test="learning-session-request"
/>
<CopyButton :text="requestMarkdown" test-id="copy-learning-session-request" />
```

### Pattern 2: Commission API call

**What:** Wrapped SDK call with global blocker for mutation.

```typescript
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"

const { data, error } = await apiCallWithLoading(
  () =>
    LearningSessionController.commission({
      body: { notebookId },
      query: { timezone: timezoneParam() },
    }),
  { blockUi: true, message: "Commissioning learning session…" }
)
if (!error) {
  requestMarkdown.value = data.requestMarkdown
  status.value = data.status
}
```

`[VERIFIED: packages/generated/doughnut-backend-api/types.gen.ts:619-627]` — `CommissionLearningSessionRequest` = `{ notebookId: number }`; `LearningSessionCommissionResponse` = `{ learningSessionId, requestMarkdown, status: 'AWAITING_REPORT' | 'RECORDED' }`.

### Pattern 3: Progress-bar interaction promotion (Phase 3 → 5)

**What:** Phase 3 row was `role="status"` display-only `[VERIFIED: RecallProgressBar.vue:64-72]`. Phase 5 promotes to control:

- Add `daisy-btn daisy-btn-primary` Commission affordance (UI-SPEC accent reservation)
- Keep glossary row copy for discoverability OR shorten to notebook title + button
- Preserve `data-test="potential-learning-session"` for existing E2E scenarios

**Anti-pattern:** Injecting commission into `ProgressBar` fill or ordinary `#buttons` — violates D-05.

### Pattern 4: E2E commission flow (draft feature)

**What:** Graduate scenario with steps:

```gherkin
When I commission a learning session for notebook "Spanish conversation"
Then the learning session request should list session items for notes "Hola, Gracias"
And the learning session request should include the learning status of "Hola"
And the learning session request should include the expected learning content "Hello"
And the learning session request should instruct the tutor to report one score per session item
And the learning session should be awaiting the tutor's report
```

`[VERIFIED: .planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature:28-36]`

**Page-object guidance:** Assert on `data-test="learning-session-request"` textarea `.val()` or visible text includes `### Hola`, `Expected learning content: Hello`, rubric line `score from 0 to 5 per item`, and awaiting marker `data-test="learning-session-awaiting-report"`.

### Anti-Patterns to Avoid

- **Re-implementing Request markdown in FE:** Backend builder is source of truth; display `requestMarkdown` only.
- **Hand-editing `packages/generated/**`:** Regenerate only if backend changes (not expected Phase 5).
- **Silent commission failure:** Use `apiCallWithLoading`; do not catch-and-ignore.
- **Graduating record/amend E2E:** Phase 6/7 scenarios stay out of CI until those phases.
- **Phase numbers in component names:** Use capability names (`CommissionLearningSessionDialog`, not `Phase5Dialog`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Clipboard copy | Custom copy util | `CopyButton.vue` | Tested; `navigator.clipboard.writeText` |
| Modal / ESC stack | New overlay | `Modal.vue` + `modalStack` | Consistent recall-page dialogs |
| Commission HTTP | Raw fetch | `LearningSessionController.commission` | OpenAPI types + auth client |
| Loading / errors | Local toast logic | `apiCallWithLoading` | Global thin bar + toast contract |
| Request markdown format | FE template | Backend `requestMarkdown` | ADR 0005 single source |
| Timezone query param | Ad-hoc string | `timezoneParam()` | Matches recalling API |

**Key insight:** Phase 5 is **wiring and observability** — the protocol and persistence already exist.

## Common Pitfalls

### Pitfall 1: Re-commission abandons awaiting session silently

**What goes wrong:** Learner still sees "potential session" after commission, clicks again, prior `AWAITING_REPORT` session deleted.  
**Why:** `dueCommissioned` still lists due trackers; commission does not reschedule until Phase 6.  
**How to avoid:** Exclude trackers in `AWAITING_REPORT` `session_item` rows from `dueCommissioned` **or** replace row with awaiting state that reopens last request without re-commissioning (needs GET — heavier).  
**Warning signs:** Double commission in one session without E2E intending it.

### Pitfall 2: Forgetting `waitUntilAppIsNotBusy` in E2E

**What goes wrong:** Assertions run while `data-app-busy` modal still open.  
**How to avoid:** After commission CTA click, `waitUntilAppIsNotBusy()` before textarea assertions.  
**Warning signs:** Flaky Cypress on `learning-session-request` visibility.

### Pitfall 3: Rubric substring drift

**What goes wrong:** E2E step `instruct the tutor to report one score per session item` fails if FE paraphrases rubric.  
**How to avoid:** Assert substrings from backend markdown (`score from 0 to 5 per item`, `## How to report`).  
**Warning signs:** Commission passes manually but E2E rubric step fails.

### Pitfall 4: Ordinary recall regression

**What goes wrong:** Commission wiring mutates `toRepeat` or nav badge.  
**How to avoid:** Do not touch `ProgressBar` math; keep `toRepeatCount` ordinary-only; run existing `learning_session` + `RecallPage` specs.  
**Warning signs:** `0 notes to recall` scenarios fail when only commissioned due.

### Pitfall 5: Missing timezone on commission

**What goes wrong:** 400/validation error; learning status dates wrong.  
**How to avoid:** Always pass `query: { timezone: timezoneParam() }` — required in OpenAPI `[VERIFIED: open_api_docs.yaml:1317-1321]`.

### Pitfall 6: Blocking classification drift

**What goes wrong:** Commission mutation added without `frontend-api.mdc` inventory entry.  
**How to avoid:** Add row to **Intentionally noncancelable** table: `Commission learning session` / `Commissioning learning session…` / dialog call site.

## Code Examples

### Backend commission response shape (already implemented)

```java
// [VERIFIED: LearningSessionControllerTests.java:45-46]
assertThat(response.getStatus(), equalTo(LearningSessionStatus.AWAITING_REPORT));
String markdown = response.getRequestMarkdown();
assertThat(markdown, containsString("# Learning Session Request"));
assertThat(markdown, containsString("### Hola"));
assertThat(markdown, containsString("Expected learning content: Hello"));
assertThat(markdown, containsString("not yet tutored"));
```

Enum literals: `[VERIFIED: backend/src/main/java/com/odde/doughnut/entities/LearningSessionStatus.java:3-6]` — `AWAITING_REPORT`, `RECORDED`.

### Recommended `dueCommissioned` exclusion (sketch)

```java
// Filter in RecallService after getCommissionedMemoryTrackersNeedToRepeat stream
// Exclude memory_tracker ids present in session_item for user's AWAITING_REPORT sessions
Set<Integer> awaitingTrackerIds =
    sessionItemRepository.findMemoryTrackerIdsInAwaitingReportSessions(user.getId());
// .filter(mt -> !awaitingTrackerIds.contains(mt.getId()))
```

New repository method is cleaner than in-memory filter for large sets — planner chooses JPQL vs stream filter.

### Vitest commission dialog (sketch)

```typescript
mockSdkService(LearningSessionController, "commission", {
  learningSessionId: 42,
  requestMarkdown: "# Learning Session Request\n\n### Hola\n",
  status: "AWAITING_REPORT",
})
// click Commission → expect textarea data-test learning-session-request
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Display-only potential rows | Clickable commission + dialog | Phase 5 | COM-01 |
| Commission API tests only | User-facing Request copy | Phase 5 | COM-02 |
| Session status in DB only | Visible awaiting-report in UI | Phase 5 | COM-03 |
| `dueCommissioned` = all due COMMISSIONED | Optional exclude awaiting-report items | Phase 5 (recommended) | Safer recommission UX |

**Deprecated/outdated:**
- `@wip` on commission E2E — remove when scenario green (Phase 5 exit criterion)

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Commission E2E does not require potential-session row to disappear after commission | Open Q1 | If product wants row hidden, need exclusion + refresh |
| A2 | Dialog can commission on first open (no separate confirm beyond CTA) | Pattern 3 | Extra confirm step may be desired in discuss-phase |
| A3 | `AWAITING_REPORT` status string shown in UI matches OpenAPI enum literal | Pattern 2 | Display copy may use human phrase instead |
| A4 | No GET endpoint needed for COM-03 if dialog stays open after commission | Architecture | Re-open flow after navigate away needs GET or re-commission |
| A5 | Generated SDK already includes `LearningSessionController` — no `pnpm generateTypeScript` required | Standard Stack | Stale client if branch diverged |

## Open Questions (RESOLVED)

1. **Exclude awaiting-report trackers from `dueCommissioned`?** — **RESOLVED: Yes**
   - **Decision:** `05-02-PLAN.md` Task 1 adds `SessionItemRepository.findMemoryTrackerIdsInAwaitingReportSessions` + `RecallService` filter; `RecallsControllerTests` exclusion case.
   - **UX:** `RecallProgressBar` calls `requestDueRecallsRefresh()` after commission (`05-01-PLAN.md`).

2. **Row UX: whole-row click vs explicit button?** — **RESOLVED: Explicit button**
   - **Decision:** `daisy-btn-primary` `Commission` button per row (`05-UI-SPEC.md` Interaction Contract); `data-test="commission-learning-session"`.

3. **UI-SPEC (`workflow.ui_phase: true`)?** — **RESOLVED: Generated**
   - **Decision:** `05-UI-SPEC.md` created via `gsd-ui-phase --auto`; extends Phase 3 spacing/typography/accent rules for commission dialog.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix shell | `pnpm frontend:test`, Cypress | ✓ | local | `cloud-vm-setup` skill |
| `pnpm sut` | E2E commission scenario | assume running | — | `pnpm sut:healthcheck` |
| Generated API client | `LearningSessionController` | ✓ | in repo | `pnpm generateTypeScript` if missing |
| Clipboard API | `CopyButton` | ✓ | browser | E2E uses Cypress clipboard permissions |

**Missing dependencies with no fallback:** none  
**Step 2.6:** Existing doughnut dev stack — no new CLIs.

## Project Constraints (from .cursor/rules/)

- Tooling via `CURSOR_DEV=true nix develop -c …`; git without Nix prefix `[VERIFIED: general.mdc]`
- Behavior phase: one observable behavior; stop-safe; targeted E2E not full suite `[VERIFIED: planning.mdc]`
- Capability naming in product code — no phase numbers `[VERIFIED: general.mdc, planning.mdc]`
- Frontend API: import from `@generated/doughnut-backend-api/sdk.gen`; `apiCallWithLoading`; `blockUi` for mutations; `waitUntilAppIsNotBusy` after busy actions in E2E `[VERIFIED: frontend-api.mdc]`
- Frontend tests: Vitest browser mode; `mockSdkService`; `data-testid`; avoid role queries `[VERIFIED: frontend-testing.mdc]`
- Small-test style at stable boundary; concise `makeMe` `[VERIFIED: unit-testing.mdc]`
- Never silently swallow failures `[VERIFIED: error-handling.mdc]`
- E2E: graduate commission scenario with `@wip` until green; `pnpm cypress run --spec` for touched feature `[VERIFIED: e2e-authoring.mdc, planning.mdc]`
- No hand-edit `packages/generated/**` or `open_api_docs.yaml` `[VERIFIED: agent-map.md]`
- ADR 0005 protocol shape is binding; ADR 0003 scheduling not in this phase `[VERIFIED: architecture-decisions.mdc]`

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest (frontend) + JUnit (backend regression) + Cypress/Cucumber (E2E) |
| Config file | `frontend/vitest.config.ts`; `e2e_test/config/ci.ts` |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/CommissionLearningSessionDialog.spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm frontend:test` + `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| COM-01 | Open dialog from progress bar; commission notebook | unit + E2E | Vitest `RecallProgressBar` / dialog spec; Cypress commission scenario | ❌ Wave 0 |
| COM-02 | Request lists items, content, status, rubric | unit + E2E | Dialog spec asserts markdown substrings; E2E draft steps | ❌ Wave 0 |
| COM-03 | Session awaiting report visible | unit + E2E | Dialog `data-test="learning-session-awaiting-report"`; E2E awaiting step | ❌ Wave 0 |
| Regression | Phase 3 potential-session E2E stay green | E2E | `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature` | ✅ feature exists |
| Regression | Backend commission unchanged | unit | `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '*LearningSessionControllerTests*'` | ✅ |
| Optional | `dueCommissioned` exclusion | unit | `RecallsControllerTests` new case | ❌ if exclusion added |

### Sampling Rate

- **Per task commit:** targeted Vitest file(s) for touched components
- **Per wave merge:** `learning_session` Cypress spec + `pnpm frontend:test` for recall components
- **Phase gate:** commission scenario without `@wip`; existing scenarios in same feature still green

### Wave 0 Gaps

- [ ] `frontend/src/components/recall/CommissionLearningSessionDialog.vue`
- [ ] `frontend/tests/components/recall/CommissionLearningSessionDialog.spec.ts`
- [ ] `e2e_test/step_definitions/learning_session.ts` — steps for commission + request assertions + awaiting
- [ ] `e2e_test/start/pageObjects/recallPage.ts` — `commissionLearningSession(notebookTitle)` helper
- [ ] Add commission scenario to `e2e_test/features/learning_session/commissioned_learning_session.feature` with `@wip` until green
- [ ] (Recommended) `RecallService` exclusion + `RecallsControllerTests` assertion
- [ ] Update `frontend-api.mdc` blocking inventory for commission mutation

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | yes | `authorizationService.assertLoggedIn()` on commission `[VERIFIED: LearningSessionController.java:53]` |
| V4 Access Control | yes | `assertAuthorization(notebook)` before commission `[VERIFIED: LearningSessionController.java:59]` |
| V5 Input Validation | yes | `notebookId` body; empty due → 400 without creating session `[VERIFIED: LearningSessionControllerTests#emptyDueCommissionedTrackers]` |
| V6 Cryptography | no | — |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Commission notebook without access | Elevation | `AuthorizationService.assertAuthorization(notebook)` |
| XSS via notebook name in dialog | Tampering | Vue text interpolation only — never `v-html` on `notebookName` `[CITED: 03-UI-SPEC.md Security/XSS]` |
| IDOR on commission | Spoofing | Session scoped to logged-in user in service layer |

`security_enforcement: true`, ASVS level 1 `[VERIFIED: .planning/config.json workflow.security_enforcement]`.

## Sources

### Primary (HIGH confidence)

- `backend/src/main/java/com/odde/doughnut/controllers/LearningSessionController.java` — commission endpoint
- `backend/src/main/java/com/odde/doughnut/services/LearningSessionService.java` — commission + abandon lifecycle
- `backend/src/test/java/com/odde/doughnut/controllers/LearningSessionControllerTests.java` — ADR markdown assertions
- `packages/generated/doughnut-backend-api/sdk.gen.ts` + `types.gen.ts` — FE API contract
- `frontend/src/components/recall/RecallProgressBar.vue` — current potential-session strip
- `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature` — commission E2E steps

### Secondary (MEDIUM confidence)

- `docs/adrs/0005-commissioned-learning-session-protocol.md` — Request shape
- `.planning/phases/03-potential-learning-sessions/03-UI-SPEC.md` — DaisyUI / accent / testability
- `.planning/phases/01-commissioned-tracker-model/CONTEXT.md` — dialog + lifecycle locks

### Tertiary (LOW confidence)

- Post-commission strip behavior — deferred product choice (Assumption A1)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — existing SDK, Modal, CopyButton, apiCallWithLoading all verified in repo
- Architecture: HIGH — Phase 4 backend complete; FE wiring pattern matches export dialogs
- Pitfalls: MEDIUM — `dueCommissioned` exclusion is recommended but not locked in CONTEXT

**Research date:** 2026-08-08  
**Valid until:** 2026-09-08 (stable in-repo patterns)

## RESEARCH COMPLETE
