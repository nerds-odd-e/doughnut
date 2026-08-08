# v1.3 post-milestone refactor

**Status:** in-progress (Phases 1–5 done)

**Type:** ad-hoc quick plan (Structure only)

**Goal:** Apply post-change-refactor and unit-testing.mdc hygiene to all
production and test code introduced or materially touched by milestone
**v1.3 Commissioned Learning Session MVP** (`bce2433f92^..d12e191f63`), without
changing externally observable learning-session behavior.

## Scope

**In:** Backend CLS services/entities/DTOs/tests; frontend recall progress bar /
commission dialog / recall composables + Vitest; E2E page objects and step
helpers for `commissioned_learning_session.feature`.

**Out:** New CLS features; OpenAPI glossary rename (STATE tech_debt); quick
007 inline titles; splitting pre-existing oversized files not aggravated by
v1.3 (e.g. whole `testability.ts`); Behavior changes to scheduling or ADR
0005 protocol.

**Constraint:** Every phase is **Structure**. Existing CLS E2E and focused
unit suites must stay green. Prefer one subsystem per phase; when OpenAPI
type names change, follow with a frontend-only consume phase (no simultaneous
backend+frontend production edit in one phase without Jidoka).

## Audit summary (evidence)

| Check | Must-fix | Should-fix |
|-------|-----------|-------------|
| File size >250 | `LearningSessionControllerTests` 758; `RecallsControllerTests` 521; `LearningSessionService` 256; `RecallProgressBar.vue` 267; `CommissionLearningSessionDialog.spec` 288 | `MemoryTracker` 250 (at limit) |
| Duplication | Identical `AwaitingReportLearningSessionLite` / `RecordedLearningSessionLite`; triplicated session strips + 3× dialog mounts in `RecallProgressBar` | Rejected-entry mapping; controller notebook-auth boilerplate; duplicate `commissionLearningSession` on recall page object |
| Dead code | Unused `zoneId` on `LearningSessionService.record`; dead `LearningSessionReportParser.parse(String)`; unreachable `matched == null` after title-filtered parse; unused UI field `trackerIds` | — |
| Unit-testing.mdc | PolicyTest SpringBoot scheduling overlaps controller; sibling tests re-assert canonical record/amend shape; oversized multi-behavior controller test class | Dialog/ProgressBar specs duplicate E2E happy-path copy; CSS-class pin in AssimilationPanel.commissioned |
| Naming | No phase numbers in product artifacts | Service throws `ResponseStatusException` (HTTP in domain layer) — optional polish |

**Keep as pure contracts:** `LearningSessionReportParserTest` (markdown matrix).
**Keep thin:** `CommissionedLearningSessionFeedbackPolicyTest` → pure `applyScore` only.

## Design decisions

1. **Structure-only, stop-safe.** Stopping after any phase leaves CLS behavior
   unchanged and the tree cleaner than before.
2. **Tests own capability, not phase history.** Split oversized test classes by
   domain seam (commission / record / amend / recall CLS feed), never by
   milestone phase number.
3. **Boundary tests win.** Controller / mounted-component / E2E keep schedule
   and happy-path coverage; algorithm tests keep only pure math.
4. **One lite session type.** Awaiting vs recorded is which list the session
   is in, not two DTO shapes.
5. **One strip + one dialog** in the progress bar; strip vs dialog get distinct
   `data-test` ids.
6. **Cross-subsystem DTO rename** is two sequential phases (backend+regen, then
   frontend consume) so each phase stays single-subsystem.

## Phases

### Phase 1 — Dead code on the record path
**Type:** Structure | **Status:** done | **Subsystem:** backend

- Remove unused `ZoneId zoneId` parameter from `LearningSessionService.record`
  and update the controller call site.
- Delete unused `LearningSessionReportParser.parse(String)` overload.
- Remove unreachable `matched == null` rejection branch in `record` (parser
  already rejects unknown titles when `sessionItemTitles` is non-empty).

**Verify:** focused `LearningSessionControllerTests` + `LearningSessionReportParserTest`.

### Phase 2 — Trim overlapping CLS unit tests (unit-testing.mdc)
**Type:** Structure | **Status:** done | **Subsystem:** backend (tests)

- Shrink `CommissionedLearningSessionFeedbackPolicyTest` to pure `applyScore`
  (prefer parameterized 0–5); drop SpringBoot scheduling / snapshot cases
  already locked at the controller.
- In `LearningSessionControllerTests`: one canonical record shape; amend
  siblings assert **deltas only**; split multi-behavior
  `amendSpanishNotebookPartialReport` into one-behavior tests.
- In `RecallsControllerTests`: drop redundant “dueCommissioned empty” assert
  where awaiting-session presence already implies it.
- Prefer builder/`makeMe` over raw `new SessionItem()` + setters where tests
  remain.

**Verify:** affected controller + policy test classes.

### Phase 3 — Split `LearningSessionControllerTests`
**Type:** Structure | **Status:** done | **Subsystem:** backend (tests)

Split 758-line class along cohesive seams sharing
`LearningSessionControllerTestBase` (and hoist Spanish notebook /
`recordRequest` helpers there):

- `LearningSessionCommissionTests`
- `LearningSessionRecordTests`
- `LearningSessionAmendTests`

Each file under 250 lines. No production changes.

**Verify:** the three new test classes.

### Phase 4 — Split CLS feed out of `RecallsControllerTests`
**Type:** Structure | **Status:** done | **Subsystem:** backend (tests)

Extract commissioned learning-session feed coverage (~awaiting / recorded /
day-three due) into e.g. `RecallsCommissionedLearningSessionTests`. Leave
ordinary Repeat / PreviouslyAnswered in `RecallsControllerTests`. Both under
250 lines (or as close as cohesive seams allow without mixing concerns).

**Verify:** both Recalls test classes.

### Phase 5 — Collapse identical session feed lite DTOs
**Type:** Structure | **Status:** done | **Subsystem:** backend (+ generated API)

- Replace `AwaitingReportLearningSessionLite` and `RecordedLearningSessionLite`
  with one type (e.g. `LearningSessionLite`).
- One mapper path in `RecallService`.
- Regenerate TypeScript client (`pnpm generateTypeScript`).

Wire JSON field names and list membership stay the same; only Java/TS type
identity changes.

**Verify:** Recalls CLS tests + compile after regen.

### Phase 6 — Frontend session types after lite DTO unify
**Type:** Structure | **Status:** planned | **Subsystem:** frontend

- Align `useRecallData` types with regenerated SDK (single strip session shape
  or thin aliases).
- Drop unused `trackerIds` from `PotentialLearningSession` and update Vitest
  fixtures.
- Extract `applySessionStrips(response)` shared by initial load and `loadMore`
  in `useRecallPageLoading`.

**Verify:** `useRecallData.spec.ts`, `RecallProgressBar.spec.ts`,
`RecallPage.spec.ts`.

### Phase 7 — Extract record/amend session resolution
**Type:** Structure | **Status:** planned | **Subsystem:** backend

Pull awaiting-vs-id-vs-latest-recorded targeting (and rejected-entry factory
dedupe) out of `LearningSessionService` into a cohesive helper/type so the
service stays under 250 lines and protocol targeting has one edit site.
Keep HTTP mapping at the controller edge if easy in the same pass; otherwise
leave `ResponseStatusException` for optional polish.

**Verify:** LearningSession record/amend controller tests.

### Phase 8 — Extract commissioned feedback scheduling from `MemoryTracker`
**Type:** Structure | **Status:** planned | **Subsystem:** backend

Move `recordCommissionedFeedback`, `restorePreSessionSnapshot`, and closely
related spacing helpers into a dedicated type (e.g. policy/collaborator used
by the entity or service) so `MemoryTracker` stays under 250 lines before the
next touch. Preserve `recordCommissionedFeedback` call sites’ behavior.

**Verify:** LearningSession record/amend tests + remaining policy `applyScore`
tests.

### Phase 9 — One strip component + one dialog in `RecallProgressBar`
**Type:** Structure | **Status:** planned | **Subsystem:** frontend

- Extract a single session-strip presentational component (message + CTA
  `data-test` + click).
- Mount **one** `CommissionLearningSessionDialog` driven by `{ mode, session }`.
- Unify refresh handlers (`onCommissioned` / `onRecorded`).
- Target `RecallProgressBar.vue` under 250 lines.

**Verify:** `RecallProgressBar.spec.ts` (+ dialog mount wiring).

### Phase 10 — Trim overlapping frontend unit coverage
**Type:** Structure | **Status:** planned | **Subsystem:** frontend (tests)

- Split or trim `CommissionLearningSessionDialog.spec.ts` under 250: keep API
  body, failure retention, rejection UI, amend `learningSessionId`; drop
  markdown/rubric/awaiting assertions owned by E2E; share
  `commissionToAwaiting()` setup; siblings assert deltas.
- Collapse triplicated `mountBar` helpers in `RecallProgressBar.spec.ts`; keep
  open-dialog wiring, drop glossary-copy cases E2E already owns.
- Drop `RecallPage.spec` potential-session copy assertion if ProgressBar/E2E
  cover it.
- In `AssimilationPanel.commissioned.spec.ts`, assert observable caret/click
  behavior instead of `pointer-events-none` class pin.
- **Do not** merge `.commissioned` into `AssimilationPanel.spec.ts` (would
  breach 250 and break the existing sibling-file pattern).

**Verify:** affected Vitest files.

### Phase 11 — E2E page-object / test-id cohesion
**Type:** Structure | **Status:** planned | **Subsystem:** frontend + e2e helpers

- Distinct `data-test` for strip CTA vs dialog submit (today both
  `record-learning-session-report`).
- Deduplicate `commissionLearningSession` on `recallPage.ts` (one
  implementation).
- Simplify amend branch in `learning_session.ts` after unique ids.
- Update `commissioned_learning_session.feature` steps only as needed; no
  scenario behavior change.

**Verify:** `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`.

### Optional polish (defer unless a phase above lands on them)
- Private `authorizedNotebook` helper on `LearningSessionController`.
- Domain exceptions instead of `ResponseStatusException` in
  `LearningSessionService` (same HTTP outcomes).
- Shared Spanish-notebook `makeMe` helper across LearningSession/Recalls tests.

## Execution notes

- After each phase: Jidoka → post-change-refactor on that phase’s diff → update
  this plan → commit (+ push per local deploy gate when executing).
- Do not run the full E2E suite; use the CLS feature spec when UI/e2e helpers
  change.
- Capability names only in product/test artifacts; phase numbers stay under
  `.planning/`.

## Discoveries affecting remaining work

- `parse(String)` and `record(... zoneId)` deadness removed in Phase 1; `LearningSessionService` now 245 lines.
- `trackerIds` is written and asserted in unit tests but never read by UI or
  commission API.
- Frontend strip/dialog duplication is the main frontend maintainability debt;
  backend test file size is the main backend debt.
- AssimilationPanel `.commissioned` sibling file is intentional and should stay.
