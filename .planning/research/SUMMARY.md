# Scheduling Policy Research Summary

**Domain:** Spaced-repetition scheduling — ADR 0003 vs current Doughnut scheduler  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## Executive Summary

ADR 0003 defines safety properties for how graded evidence updates memory strength and the due-time projection (`nextRecallAt`). Most outcome-specific routing already exists (spelling accidental match, overlap no-op, commissioned Tutor 0–5 scores, frequent-failure warning), but the **core success path still conflates queue compliance with memory evidence**: `MemoryTracker.recalledSuccessfully` passes `gradedAt − nextRecallAt` into `ForgettingCurve.succeeded`. That penalizes early correct answers and creates the correct-but-overdue trap (SEED-004). Industry norm (Anki, FSRS) and ADR 0003 require retention to be judged from **elapsed time since last graded recall**, with `nextRecallAt` as operational queue metadata only.

**Policy-first retrofit, not library migration.** No new runtime libraries. Keep Java / Spring Boot / MySQL / Vue unchanged. Fix how success math consumes time evidence; verify with **observable schedule tests** (`nextRecallAt` strictly after grade, interval monotonicity, outcome-specific non-movement) — not `forgettingCurveIndex` magic numbers. FSRS, event-sourced history replay, and bulk `nextRecallAt` migration are deferred anti-features per ADR.

## Key Discoveries

### Primary gap

- `MemoryTracker.recalledSuccessfully` computes `delayInHours` from `calculateNextRecallAt()` (planned due), not from `lastRecalledAt` (prior graded recall).
- `ForgettingCurve.succeeded` reduces the success increment when `delayInHours < 0` (early relative to due). That encodes schedule compliance into strength.
- Overdue correct answers therefore do not get retention credit for the longer observed interval; busy learners can trap in immediate/daily loops.

### Already aligned (built — verify)

| Area | Finding |
|------|---------|
| Accidental match | `partialFail()` + normal interval path (not 12h `recallFailed`); verify weaker than incorrect |
| Overlap | Skips tracker mutation; excluded from frequent-failure count |
| Commissioned 0–5 | ADR quantified table in `CommissionedLearningSessionFeedbackPolicy`; post-grade strictly-future helper exists |
| Incorrect | Strength cut + explicit 12h retry; timing-neutral in failure path |
| Frequent-failure warning | Informational API (`wrongCount`, `threshold`, `periodDays`); no schedule side effect by design |
| Effort | Thinking-time adjustment clamped; null = neutral |
| Spacing table | `SpacedRepetitionAlgorithm` + user space intervals unchanged |
| Schema | Existing `forgetting_curve_index`, `next_recall_at`, `last_recalled_at` sufficient — no migration expected for evidence fix |

### Stack

No new dependencies. Scheduling stays pure Java domain code. Verification: JUnit + Hamcrest on schedule observables; targeted Cypress for overdue-correct / overlap / commissioned. Avoid FSRS libraries, Redis/job queues for per-tracker scheduling, frontend scheduling logic, bulk history replay.

### Architecture implications (discoveries, not a plan)

- Evidence should be outcome + elapsed since `lastRecalledAt` (+ optional effort); never `gradedAt − nextRecallAt` as negative evidence.
- Scheduling mutations are scattered (spelling, MCQ, commissioned, manual mark-as-recalled) — divergent semantics are a real risk if only one path is fixed.
- Commissioned path already has `ensureNextRecallStrictlyAfterNow`; recall success paths lack an equivalent strictly-future guarantee after every grade.
- Policy tests that lock index floats will miss schedule traps and break under representation changes.

### Critical pitfalls

1. Conflating schedule deviation with memory evidence  
2. Immediate/daily trap after correct answers (`nextRecallAt <= gradedAt` or zero interval)  
3. Parallel entry points with divergent semantics  
4. Accidental match treated as incorrect (12h override / threshold counting)  
5. Policy tests on internal index instead of observable schedule  
6. Unsafe bulk `nextRecallAt` rebuild from incomplete history  

Details: [PITFALLS.md](./PITFALLS.md). Architecture: [ARCHITECTURE.md](./ARCHITECTURE.md). Features: [FEATURES.md](./FEATURES.md). Stack: [STACK.md](./STACK.md).

## Open questions

- **Accidental match vs frequent-failure:** Current wrong-count query treats `correct=false` (includes accidental match); ADR says incorrect-only. Product decision needed.
- **ADR 0003 status:** Still Proposed — human approval or explicit drift tracking before shipping policy changes.
- **Bounded lateness bonus:** ADR allows; not required for removing the late-success penalty.
- **Manual `markAsRecalled` / `updateForgettingCurve`:** Clarify whether admin/bypass paths must follow the same evidence rules.

## Confidence

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Grounded in build + live modules; no new deps |
| Features | HIGH | ADR + SEED-004 + codebase audit; Anki/FSRS corroborate table stakes |
| Architecture | HIGH | File-level anchors for gap and seams |
| Pitfalls | HIGH (code); MEDIUM (rollout) | Interval drift monitoring not established |

## Sources

- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md`
- `backend/.../ForgettingCurve.java`, `MemoryTracker.java`
- `backend/.../CommissionedLearningSessionFeedbackScheduling.java`
- `backend/.../SpellingRecallGrading.java`
- [Anki — Falling Behind](https://docs.ankiweb.net/studying.html#falling-behind)
- [FSRS Algorithm wiki](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm)

---
*Research completed: 2026-08-12*
