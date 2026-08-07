---
phase: 03-potential-learning-sessions
verified: 2026-08-07T23:58:00Z
status: passed
score: 6/6 must-haves verified
behavior_unverified: 0
overrides_applied: 0
---

# Phase 3: Potential learning sessions — Verification Report

**Phase Goal:** Due commissioned trackers surface as potential learning sessions by notebook on the recall page, not as ordinary recall.

**Verified:** 2026-08-07T23:58:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | With only due commissioned trackers, ordinary recall count is 0 (TRK-03 / SC1) | ✓ VERIFIED | `byUserIdFrom` keeps `type <> 'COMMISSIONED'`; sibling `byUserIdCommissionedFrom` feeds `dueCommissioned` only. Controller: `shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall` — `toRepeat` size 0, `dueCommissioned` size 1 (PASS). Nav badge uses `toRepeatCount` only (`useNavigationItems`). E2E scenarios assert `0 notes to recall` |
| 2 | Recall progress bar offers potential learning session(s) grouped by notebook (POT-01 / SC2) | ✓ VERIFIED | `useRecallData.potentialLearningSessions` groups `dueCommissioned` by `notebookId`. `RecallProgressBar` renders `data-test="potential-learning-session"` with glossary copy + notebook title. Vitest: `renders glossary copy for one notebook session` + `groups dueCommissioned by notebookId` PASS |
| 3 | Two notebooks with due commissioned trackers yield two potential sessions (POT-02 / SC3) | ✓ VERIFIED | Group-by produces one row per `notebookId`. Vitest: `renders one row per notebook with distinct titles` PASS (Spanish conversation + Kanji). E2E scenario graduated without `@wip` |
| 4 | `dueCommissioned` arrives on the existing recalling / DueMemoryTrackers round-trip (D-01) | ✓ VERIFIED | `DueMemoryTrackers.dueCommissioned`; `RecallService.getDueMemoryTrackers` maps both lists; OpenAPI `types.gen.ts` includes field; `useRecallPageLoading` / `MainMenu` call `setDueCommissioned` from same payloads as `toRepeat` |
| 5 | Potential session is FE-derived by notebook — no PLS persistence (D-02) | ✓ VERIFIED | `PotentialLearningSession` type + computed only in `useRecallData.ts`; no Flyway / entity / table for potential sessions |
| 6 | Phase 3 rows are display-only — no commission dialog / Learning Session create (D-04) | ✓ VERIFIED | Potential-session strip has no `@click` / dialog wiring; commission deferred to Phase 4–5 per ROADMAP |

**Score:** 6/6 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ----------- | ------ | ------- |
| `DueCommissionedMemoryTrackerLite.java` | Lite with memoryTrackerId, notebookId, notebookName | ✓ VERIFIED | Substantive DTO; mapped in `RecallService` from note notebook |
| `DueMemoryTrackers.java` | Additive `dueCommissioned` list | ✓ VERIFIED | Field present; generated client mirrors it |
| `MemoryTrackerRepository.java` | Sibling `byUserIdCommissionedFrom` + due stream | ✓ VERIFIED | `type = 'COMMISSIONED'`; ordinary `byUserIdFrom` unchanged |
| `useRecallData.ts` | `setDueCommissioned` + `potentialLearningSessions` group-by | ✓ VERIFIED | Map by notebookId; `toRepeatCount` independent |
| `RecallProgressBar.vue` | Display-only strip under ProgressBar | ✓ VERIFIED | `v-if` / `v-for` rows; glossary copy; `break-words`; empty → nothing |
| `RecallPage.vue` | Passes `potentialLearningSessions` into bar | ✓ VERIFIED | Prop wired from `useRecallData` |
| `commissioned_learning_session.feature` | Both Phase 3 E2E scenarios graduated | ✓ VERIFIED | Tutor-await + multi-notebook scenarios; no `@wip` |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| `GET /api/recalls/recalling` (+ menu `recallStatus`) | `DueMemoryTrackers.dueCommissioned` | `RecallService.getDueMemoryTrackers` → `byUserIdCommissionedFrom` | ✓ WIRED | Both controllers call same service method; commissioned list set alongside `toRepeat` |
| `useRecallPageLoading` / `MainMenu` | `useRecallData.setDueCommissioned` | Response `dueCommissioned`; never shuffled into quiz queue | ✓ WIRED | `setDueCommissioned` before `setToRepeat(shuffle(toRepeat))`; quiz uses `toRepeat` only |
| `potentialLearningSessions` computed | `RecallProgressBar` rows | Prop from `RecallPage`; `data-test="potential-learning-session"` | ✓ WIRED | Group-by notebookId → one row per notebook |
| E2E Given assimilate-as-commissioned | Then potential-session assertions | `recallPage.expectPotentialLearningSession` | ✓ WIRED | Step defs + page object `cy.contains` on row selector |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| Recalling / menu DTO | `dueCommissioned` | DB `memory_tracker` via `findAllCommissionedByUserAndNextRecallAtLessThanEqualOrderByNextRecallAt` | Yes — controller test persists COMMISSIONED + asserts notebook identity | ✓ FLOWING |
| `useRecallData` | `potentialLearningSessions` | Grouped from `dueCommissioned` set by load/menu | Derived from API list; empty when unset | ✓ FLOWING |
| `RecallProgressBar` | `potentialLearningSessions` prop | `RecallPage` from composable | Renders notebook names from real grouping | ✓ FLOWING |
| Nav recall badge | `toRepeatCount` | `toRepeat` length − index only | Ordinary-only; commissioned never inflate badge | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Ordinary empty + dueCommissioned populated (controller) | `pnpm backend:test_only --tests '*shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall'` | BUILD SUCCESSFUL | ✓ PASS |
| FE group-by leaves toRepeatCount 0 | `pnpm -C frontend test -t "groups dueCommissioned by notebookId without affecting toRepeatCount"` | 1 passed | ✓ PASS |
| Progress-bar one/many/empty/long-title | `pnpm -C frontend test tests/components/recall/RecallProgressBar.spec.ts` | 4 tests passed | ✓ PASS |
| E2E learning_session Phase 3 scenarios | Cypress (needs running `pnpm sut`) | Not run in verifier (no server start) | ? SKIP |

### Probe Execution

| Probe | Command | Result | Status |
| ----- | ------- | ------ | ------ |
| — | — | No probes declared for this phase | SKIPPED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| TRK-03 | 03-01, 03-02 | Due commissioned do not appear as ordinary recall | ✓ SATISFIED | Controller separation + E2E `0 notes to recall` + badge from `toRepeatCount` |
| POT-01 | 03-01 | Potential sessions from recall progress bar by notebook | ✓ SATISFIED | FE group-by + progress-bar strip + one-notebook E2E |
| POT-02 | 03-02 | Different notebooks → separate potential sessions | ✓ SATISFIED | Multi-row Vitest + multi-notebook E2E scenario |

No orphaned Phase 3 requirements in REQUIREMENTS.md.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | No `TBD` / `FIXME` / `XXX` in phase key files | — | — |
| `RecallProgressBar.vue` | — | Potential-session rows have no click/commission stub | ℹ️ Info | Correct for D-04 / Phase 4–5 deferral |

### Human Verification Required

None — success criteria are covered by controller/unit evidence; E2E scenarios exist and are graduated (verifier skipped live Cypress per no-server rule, same as Phase 2).

### Gaps Summary

None. Phase goal achieved: due COMMISSIONED trackers surface as notebook-grouped potential learning sessions on the recall progress bar while ordinary recall stays empty of that work.

Commission dialog / Learning Session create intentionally deferred to Phases 4–5 (not gaps).

---

_Verified: 2026-08-07T23:58:00Z_
_Verifier: Claude (gsd-verifier)_
