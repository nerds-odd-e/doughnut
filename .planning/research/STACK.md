# Stack Research

**Domain:** Spaced-repetition scheduling policy alignment (ADR 0003) — evidence vs due-time separation  
**Project:** Doughnut v1.4  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## Executive recommendation

**No new runtime libraries are required.** v1.4 is a policy correction inside the existing in-house scheduler (`SpacedRepetitionAlgorithm` → `ForgettingCurve` → `MemoryTracker`), with verification through existing JUnit/Hamcrest backend tests and targeted Cypress E2E. ADR 0003 explicitly defers FSRS and defers rebuildable due-time projection from history; the milestone ships by refactoring how `ForgettingCurve.succeeded()` consumes time evidence and by tightening policy tests — not by adopting an external SRS engine.

The one material stack decision is **negative**: keep scheduling as plain Java domain code on the current Spring Boot / MySQL stack.

## Recommended Stack

### Core Technologies (unchanged — scheduling runs here)

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Java | 25 | Scheduling policy implementation | Already the backend language; `ForgettingCurve`, `MemoryTracker`, and `CommissionedLearningSessionFeedbackPolicy` are pure Java with no external SRS dependency. Policy math is small enough that a library would add coupling without reducing risk. |
| Spring Boot | 4.1.0 | Service orchestration, transactions | `MemoryTrackerService`, `SpellingRecallGrading`, and `LearningSessionService` already own all grade→schedule paths inside `@Transactional` controller tests. No new integration tier needed. |
| Spring Data JPA + MySQL | via `spring-boot-starter-data-jpa`; connector `mysql-connector-j:26.7.0` | Persist `forgetting_curve_index`, `next_recall_at`, `last_recalled_at` | ADR 0003 preserves the materialized `nextRecallAt` projection. Existing columns are sufficient; no event-store or replay schema. |
| Flyway | via `spring-boot-starter-flyway` + `flyway-mysql` | Schema versioning | **No migration expected** for v1.4 unless a future phase adds rebuildable scheduler state (explicitly deferred). Policy change is behavioral, not structural. |
| Vue 3 + TypeScript | Node ≥26.5; Vite frontend | Recall UX, overlap retry, threshold warnings | Frontend already sends `thinkingTimeMs`, handles `OVERLAP` retry without rescheduling, and fetches `ThresholdExceededResult`. No scheduling logic lives in the browser. |

### In-house scheduling modules (extend, do not replace)

| Module | Location | Role in v1.4 | Integration note |
|--------|----------|--------------|------------------|
| `SpacedRepetitionAlgorithm` | `backend/.../algorithms/` | User-configured Fibonacci spacing table → hours | **Keep as-is.** Maps `forgetting_curve_index` to interval hours; not the source of the late-success bug. |
| `ForgettingCurve` | `backend/.../entities/` | Strength adjustments for success/fail/partial + effort | **Primary change site.** Today `succeeded(delayInHours, …)` penalizes overdue success because `delayInHours` is computed vs `nextRecallAt` (see `MemoryTracker.recalledSuccessfully`). Refactor to use **observed elapsed time since `lastRecalledAt`**, with early-recall discount bounded by weaker evidence — not queue compliance. |
| `MemoryTracker` | `backend/.../entities/` | Applies grades, sets `nextRecallAt` | Pass retention interval into `ForgettingCurve`; enforce post-grade **strictly positive** interval (commissioned path already does this via `CommissionedLearningSessionFeedbackScheduling.ensureNextRecallStrictlyAfterNow`). |
| `CommissionedLearningSessionFeedbackPolicy` | `backend/.../algorithms/` | Score 0–5 → strength delta | **Already aligned** with ADR 0003 quantified table. Audit only; no new dependency. |
| `CommissionedLearningSessionFeedbackScheduling` | `backend/.../algorithms/` | Record feedback → schedule | Reuse `firstPositiveSpacingHours` pattern for any shared “no zero interval after grade” guard. |
| `SpellingRecallGrading` | `backend/.../services/` | Outcome routing: correct / incorrect / accidental / overlap | **Overlap:** already skips tracker mutation (ADR-compliant). **Accidental:** uses `partialFail()` + normal interval path — verify penalty ordering vs incorrect. |
| `MemoryTrackerService` | `backend/.../services/` | Entry points for recall + threshold | Threshold constants (5 wrong / 14 days) and `RecallPromptRepository` query already match ADR; no API stack change. |
| `TimestampOperations` | `backend/.../utils/` | Hour deltas for scheduling | Use `getDiffInHours(answerTime, lastRecalledAt)` for retention evidence; stop using `getDiffInHours(answerTime, nextRecallAt)` as strength input. |

### Supporting Libraries (existing — use for policy verification)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit Jupiter | via `spring-boot-starter-test` (Boot 4.1.0 BOM) | Unit + integration tests | Algorithm policy tests (`ForgettingCurve`, commissioned policy) and controller tests driving real DB. |
| Hamcrest | via `spring-boot-starter-test` | Observable schedule assertions | Prefer `greaterThan`, `greaterThanOrEqualTo`, `lessThan` on `nextRecallAt` and interval hours — ADR requires asserting **observable schedule behavior**, not internal index values. |
| Spring Boot Test + `@Transactional` | Boot 4.1.0 | Controller-level policy tests | Grade through `RecallPrompt` / `LearningSession` controllers with `makeMe` fixtures; assert `nextRecallAt` and due-queue eligibility. |
| Cypress | repo `e2e_test/` | End-to-end recall flows | Targeted specs for overdue-correct trap regression, overlap retry, commissioned record — not full suite unless CI requires. |
| Vitest + `@vue/test-utils` | frontend `package.json` | Recall UI behavior | `useRecallAnswerHandling`, overlap retry, threshold popup — already covered; extend only if API shape changes. |
| OpenAPI → TypeScript client | `@hey-api/openapi-ts` (root `generateTypeScript`) | Frontend API types | Regenerate only if threshold or answer DTOs change (unlikely for v1.4). |

### Development Tools (unchanged)

| Tool | Purpose | Notes |
|------|---------|-------|
| Nix dev shell | `CURSOR_DEV=true nix develop -c …` | Required for `pnpm backend:test_only`, E2E, lint. |
| Gradle + Spotless 8.9.0 | Backend build/format | `pnpm backend:verify` before phase close. |
| Biome 2.5.7 | Frontend lint/format | No frontend package changes expected. |
| `makeMe` test builders | Concise scheduling fixtures | Extend builders if policy tests need overdue/early recall timestamps (`MemoryTrackerBuilder.afterNthStrictRecall`, timestamp helpers). |

## Installation

**No new packages to install.** Work proceeds on the existing toolchain:

```bash
# Backend policy tests (primary verification)
CURSOR_DEV=true nix develop -c pnpm backend:test_only

# Targeted E2E after behavior phases
CURSOR_DEV=true nix develop -c pnpm sut   # if not already running
CURSOR_DEV=true nix develop -c npx cypress run --config-file e2e_test/config/ci.ts --spec e2e_test/features/learning_session/commissioned_learning_session.feature
```

## Integration Points by Graded Outcome

| Outcome | Stack touchpoints | New dependency? |
|---------|-------------------|-----------------|
| **Correct** | `MemoryTracker.recalledSuccessfully` → `ForgettingCurve.succeeded` | No — fix time input |
| **Incorrect** | `MemoryTracker.recallFailed` (12h forced retry + strength cut) | No — penalty is outcome-based already |
| **Accidental match** | `SpellingRecallGrading` → `markAsAccidentalMatch` → `partialFail` | No — tune increment if policy tests fail ordering |
| **Overlap** | `SpellingRecallGrading` (no `markAsRecalled`); frontend `useRecallAnswerHandling` retry | No |
| **Commissioned 0–5** | `CommissionedLearningSessionFeedbackScheduling.recordFeedback` | No — already shipped v1.3 |
| **Effort** | `Answer.thinkingTimeMs` → `ForgettingCurve.calculateThinkingTimeAdjustment` | No — bound per ADR; null = neutral |
| **Frequent-failure warning** | `RecallPromptRepository.countWrongAnswersSinceForMemoryTracker` (excludes `OVERLAP`) + `ThresholdExceededResult` API | No |

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Refactor in-house `ForgettingCurve` | **FSRS** (`open-spaced-repetition/java-fsrs`, `fsrs4j`, etc.) | Future milestone **after** ADR 0003 policy is proven in production and rebuild/migration strategy exists. Brings new state vector, parameters, retention targets, and fitting — unnecessary for removing the late-success penalty. |
| Policy tests on `nextRecallAt` | Golden-file simulation framework | Only if tuning many constants interactively; overkill for bounded increment changes. |
| Keep `delayInHours` vs due time | Full event-sourced recall history | ADR defers rebuild-from-history; incomplete legacy history makes this unsafe now. |
| No DB migration | Add `scheduler_version` / event table | Defer until a deliberate “rebuildable projection” phase; not required for evidence-vs-due separation. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **FSRS or Anki scheduler libraries** | ADR 0003 Option “Adopt FSRS immediately” is **deferred**; policy can ship on existing forgetting-curve model. Adds dependency, migration risk, and a second scheduling vocabulary. | Extend `ForgettingCurve` + policy tests |
| **Apache Commons Math / numerical libs** | Adjustments are linear increments, clamps, and `sqrt` on thinking time — already in `ForgettingCurve`. | Plain Java `float` math |
| **Redis / job queues for `nextRecallAt`** | Due work is already queried from MySQL `memory_tracker.next_recall_at`. Introduces dual writes and consistency risk. | Existing JPA entity update in same transaction as grade |
| **Quartz / ShedLock for per-tracker scheduling** | ShedLock (7.7.0) is used for batch jobs, not per-recall scheduling. Per-answer schedule is synchronous on grade. | `MemoryTracker.setNextRecallAt` at grade time |
| **Frontend scheduling libraries** | Scheduling policy must be server-authoritative; client only displays due state. | Backend-only policy |
| **Property-based test libs (e.g. jqwik)** | Not in repo; policy cases are finite and ADR-specified. | JUnit `@ParameterizedTest` + Hamcrest |
| **New `npm` / `gradle` packages for v1.4** | No new capability requires external code. | Internal refactor |

## Stack Patterns by Variant

**If correcting the overdue-success trap:**
- Change `ForgettingCurve.succeeded` to accept **retention hours** (`lastRecalledAt` → answer), not **due deviation hours** (`nextRecallAt` → answer).
- Remove the branch that applies negative adjustment when `delayInHours < 0` on success.
- Optionally add a **bounded** lateness bonus (ADR allows; not required for minimal fix).
- Because `SpacedRepetitionEarlyRecallAdjustmentTest` encodes pre-policy early-recall semantics, replace assertions that expect late success to weaken strength.

**If hardening post-grade interval safety (all outcomes):**
- Extract shared helper from `CommissionedLearningSessionFeedbackScheduling.ensureNextRecallStrictlyAfterNow` for recall grades — ADR: zero persisted interval not allowed after any graded answer.
- Apply after correct, accidental, and commissioned paths; incorrect path already sets explicit 12h retry.

**If accidental-match penalty needs tuning:**
- Adjust `ForgettingCurve.partialFail()` increment only — weaker than `failed()`, still schedules via `calculateNextRecallAt()` (normal path, not 12h relearning override).

**If effort bounds need tightening:**
- Keep adjustment inside `ForgettingCurve` constants (`BASE_THINKING_TIME_MS` 25000, `MAX_THINKING_TIME_MS` 60000); ensure slow correct answers cannot flip to failure or zero interval.

## Version Compatibility

| Package / runtime | Version | Compatible with | Notes |
|-------------------|---------|-----------------|-------|
| Spring Boot | 4.1.0 | Java 25 | BOM manages JUnit 5, Hamcrest, Jackson |
| `mysql-connector-j` | 26.7.0 | MySQL 8.x (Nix dev) | No schema change for v1.4 |
| Flyway | Boot-managed + `flyway-mysql` | Current migration chain | Skip new migrations unless projection rebuild phase starts |
| Vue / Vite | frontend `package.json` | OpenAPI-generated SDK | Recall answer DTO already includes `outcome`, `thinkingTimeMs` |
| Cypress | repo lockfile | `pnpm sut` stack | Use `--spec` for scheduling-related features only |

## Data the stack already provides (no new persistence layer)

| Field / artifact | Table / type | Policy use |
|------------------|--------------|------------|
| `forgetting_curve_index` | `memory_tracker` | Internal strength representation (implementation detail per ADR) |
| `next_recall_at` | `memory_tracker` | Due-work projection — stays authoritative for queries |
| `last_recalled_at` | `memory_tracker` | **Retention evidence** anchor for elapsed time |
| `quiz_answer.outcome` | `quiz_answer` | `CORRECT` / `ACCIDENTAL_MATCH` / `OVERLAP` discrimination |
| `quiz_answer.thinking_time_ms` | `quiz_answer` | Effort evidence |
| `quiz_answer.correct` | `quiz_answer` | Threshold wrong-count (overlap excluded via outcome filter) |
| User space intervals | `user` → `SpacedRepetitionAlgorithm` | Configurable spacing table input |

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — authoritative policy; defers FSRS and history replay (**HIGH**)
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java` — late-success penalty in `succeeded(delayInHours < 0)` (**HIGH**, codebase)
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` — `delayInHours` computed vs `nextRecallAt` (**HIGH**, codebase)
- `backend/build.gradle` — Spring Boot 4.1.0, Java 25, no SRS dependencies (**HIGH**, codebase)
- `backend/src/main/java/com/odde/doughnut/algorithms/CommissionedLearningSessionFeedbackScheduling.java` — post-grade positive interval guard (**HIGH**, codebase)
- `.planning/PROJECT.md` — v1.4 milestone scope (**HIGH**)

---
*Stack research for: Doughnut v1.4 SRS scheduling policy (ADR 0003)*  
*Researched: 2026-08-12*
