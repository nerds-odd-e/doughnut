# Stack Research

**Domain:** Spaced-repetition scheduling policy alignment (ADR 0003)  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## Recommendation

**No new runtime libraries.** Align policy inside the existing in-house scheduler (`SpacedRepetitionAlgorithm` → `ForgettingCurve` → `MemoryTracker`), verified with existing JUnit/Hamcrest and targeted Cypress. ADR 0003 defers FSRS and rebuildable due-time projection from history.

## Current stack (unchanged)

| Technology | Role for scheduling |
|------------|---------------------|
| Java 25 | Domain policy math (`ForgettingCurve`, commissioned feedback policy) |
| Spring Boot 4.1 | Orchestration, transactions (`MemoryTrackerService`, grading, learning sessions) |
| Spring Data JPA + MySQL | `forgetting_curve_index`, `next_recall_at`, `last_recalled_at` — sufficient; no event store |
| Flyway | Present; **no migration expected** for evidence-vs-due fix |
| Vue 3 + TypeScript | Displays due state; sends `thinkingTimeMs`; overlap retry UX — no client-side scheduling |

## In-house modules

| Module | Discovery |
|--------|-----------|
| `SpacedRepetitionAlgorithm` | Spacing table → hours; keep |
| `ForgettingCurve` | Late-success penalty gone; `succeeded(delayInHours)` still due-relative (early shrink only) |
| `MemoryTracker` | Computes delay vs `nextRecallAt` on success (**C1**) |
| `CommissionedLearningSessionFeedbackPolicy` | Already ADR 0–5 table |
| `CommissionedLearningSessionFeedbackScheduling` | Has post-grade strictly-future helper |
| `SpellingRecallGrading` | Outcome routing; overlap skips mutation |
| `MemoryTrackerService` | Threshold constants already ADR-shaped |
| `TimestampOperations` | Use vs `lastRecalledAt` for retention evidence |

## Verification tools (existing)

- JUnit Jupiter + Hamcrest — assert schedule observables (`nextRecallAt`, interval hours)
- Spring Boot `@Transactional` controller tests + `makeMe`
- Cypress targeted specs — overdue-correct, overlap, commissioned
- Vitest for recall UI handling only if API shape changes

## Outcome → stack touchpoints

| Outcome | Touchpoints | New dependency? |
|---------|-------------|-----------------|
| Correct | `recalledSuccessfully` → `succeeded` | No — fix time input |
| Incorrect | `recallFailed` | No |
| Accidental | `markAsAccidentalMatch` → `partialFail` | No |
| Overlap | Skip `markAsRecalled` | No |
| Commissioned 0–5 | Feedback scheduling | No |
| Effort | `thinkingTimeMs` adjustment | No |
| Frequent-failure | Wrong-count query + threshold API | No |

## Alternatives considered

| Choice | Alternative | When alternative applies |
|--------|-------------|--------------------------|
| In-house `ForgettingCurve` fix | FSRS libraries | After policy proven + migration strategy |
| Assert `nextRecallAt` | Golden-file simulation frameworks | Only if tuning many constants |
| Keep transactional projection | Event-sourced history replay | When complete versioned history exists |
| No DB migration | `scheduler_version` / event table | Rebuildable-projection work later |

## What not to use

| Avoid | Why |
|-------|-----|
| FSRS / Anki scheduler libs | ADR deferred; second vocabulary + migration risk |
| Numerical libs | Adjustments are simple float math |
| Redis / queues for `nextRecallAt` | Dual-write risk; grade already transactional |
| Quartz per-tracker scheduling | Schedule is synchronous on grade |
| Frontend scheduling libs | Server must own policy |
| New npm/gradle packages | No capability needs external code |

## Data already available

| Field | Policy use |
|-------|------------|
| `forgetting_curve_index` | Internal strength (implementation detail) |
| `next_recall_at` | Due-work projection |
| `last_recalled_at` | Retention evidence anchor |
| `quiz_answer.outcome` | Correct / accidental / overlap |
| `quiz_answer.thinking_time_ms` | Effort |
| User space intervals | Spacing table input |

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- `ForgettingCurve.java`, `MemoryTracker.java`
- `backend/build.gradle`
- `CommissionedLearningSessionFeedbackScheduling.java`

---
*Researched: 2026-08-12*
