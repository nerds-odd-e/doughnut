# Scheduling Policy Research Summary

**Domain:** Spaced-repetition scheduling — ADR 0003 vs current Doughnut scheduler  
**Researched:** 2026-08-12  
**Confidence:** HIGH

## Executive Summary

ADR 0003 defines safety properties for how a graded recall transitions persisted memory state and the due-time projection (`nextRecallAt`). Most outcome-specific routing already exists (spelling accidental match, overlap no-op, commissioned Tutor 0–5 scores, frequent-failure warning).

**Late-success penalty is shipped** (2026-08-05, `735b96623a`): overdue correct no longer shrinks the success increment and must not get a shorter interval than on-time (`lateCorrectAnswerDoesNotShortenTheNextInterval`). Busy-learner immediate/daily traps from *that* penalty are gone.

**Remaining primary gap is C1:** `MemoryTracker.recalledSuccessfully` passes a `delayInHours` measured from a recomputed expected recall time (`lastRecalledAt + current interval`) into `ForgettingCurve.succeeded`. For early success, the formula is algebraically equivalent to scaling by `elapsed / current interval`; it does not read persisted `nextRecallAt`. The remaining problems are the misleading due-relative contract and the fact that `recallFailed` does not advance `lastRecalledAt`, so a later recall can span across a failure. Overdue answers get the on-time increment only — **not** the FSRS overdue reward (longer elapsed → lower R → larger bounded stability).

**Policy-first retrofit, not library migration.** No new runtime libraries. Keep Java / Spring Boot / MySQL / Vue unchanged. Make elapsed time and recall-anchor updates explicit; verify with **observable schedule tests** (`nextRecallAt` strictly after grade, interval monotonicity, outcome-specific non-movement) — not `forgettingCurveIndex` magic numbers. FSRS, `RecallLog` persistence/replay, and bulk `nextRecallAt` migration are deferred per ADR.

## Key Discoveries

### Primary remaining gap (C1)

- `MemoryTracker.recalledSuccessfully` computes `delayInHours` from `calculateNextRecallAt()`, which recomputes `lastRecalledAt + current interval`; it does not read the persisted due projection.
- For early success, `ForgettingCurve.succeeded` is algebraically `standardIncrement × elapsed / currentInterval`. Elapsed time is therefore already implicit, but hidden behind a due-relative interface.
- `recallFailed` does not update `lastRecalledAt`; the anchor is not reliably the previous state-changing recall.
- Overdue correct (`delayInHours ≥ 0`) gets the **same** increment as on-time — the late-success penalty was removed 2026-08-05. Doughnut does **not** yet grant FSRS-style extra credit for longer observed elapsed time.

### Already aligned (built — verify)

| Area | Finding |
|------|---------|
| No late-success penalty | Shipped: overdue ≥ on-time increment/interval |
| Accidental match | `partialFail()` + normal interval path (not 12h `recallFailed`); verify weaker than incorrect |
| Overlap | Skips tracker mutation; excluded from frequent-failure count |
| Commissioned 0–5 | ADR quantified table in `CommissionedLearningSessionFeedbackPolicy`; post-grade strictly-future helper exists |
| Incorrect | Strength cut + explicit 12h retry; does not advance `lastRecalledAt` (C1) |
| Frequent-failure warning | Informational API (`wrongCount`, `threshold`, `periodDays`); no schedule side effect by design |
| Effort | Thinking-time adjustment clamped; null = neutral |
| Spacing table | `SpacedRepetitionAlgorithm` + user space intervals unchanged |
| Schema | Existing `forgetting_curve_index`, `next_recall_at`, `last_recalled_at` sufficient for C1; `RecallLog` is deferred |

### Stack

No new dependencies. Scheduling stays pure Java domain code. Verification: JUnit + Hamcrest on schedule observables; targeted Cypress for overdue-correct / overlap / commissioned. Avoid FSRS libraries, Redis/job queues for per-tracker scheduling, frontend scheduling logic, bulk history replay.

### Architecture implications (discoveries, not a plan)

- `MemoryTracker` should transition its persisted pre-recall state from outcome + elapsed since `lastRecalledAt` (+ optional effort); queue deviation is not an input.
- Scheduling mutations are scattered (spelling, MCQ, commissioned, manual mark-as-recalled) — divergent semantics are a real risk if only one path is fixed.
- Commissioned path already has `ensureNextRecallStrictlyAfterNow`; recall success paths lack an equivalent strictly-future guarantee after every grade.
- Policy tests that lock index floats will miss schedule traps and break under representation changes.

### Critical pitfalls

1. Hiding elapsed time behind a due-relative API and failing to advance `lastRecalledAt` on incorrect recall (C1)
2. Immediate/daily trap after correct answers (`nextRecallAt <= gradedAt` or zero interval) — **not** the shipped late-success penalty; leftover floor/zero-interval / missing strictly-future guard  
3. Parallel entry points with divergent semantics  
4. Accidental match treated as incorrect (12h override / threshold counting)  
5. Policy tests on internal index instead of observable schedule  
6. Unsafe bulk `nextRecallAt` rebuild from incomplete history  

Details: [PITFALLS.md](./PITFALLS.md). Architecture: [ARCHITECTURE.md](./ARCHITECTURE.md). Features: [FEATURES.md](./FEATURES.md). Stack: [STACK.md](./STACK.md).

## Open questions

Tracked and expanded (including Doughnut ↔ open FSRS compatibility) in
**[FSRS-COMPATIBILITY-GAP.md](./FSRS-COMPATIBILITY-GAP.md)** — settle those issues
before finalizing ADR 0003.

Prior short list (still open there as O5 / O8 / O9 / O10):

- **Accidental match vs frequent-failure:** Current wrong-count query treats `correct=false` (includes accidental match); ADR says incorrect-only.
- **ADR 0003 status / FSRS stance:** Still Proposed — decide semantic vs vocabulary vs migration commitment.
- **Bounded lateness bonus:** Minimum bar (no overdue penalty) is shipped. FSRS-style overdue reward is not. ADR allows a bounded bonus; requires C1 first.
- **Manual `markAsRecalled` / `updateForgettingCurve`:** Same recall-transition rules vs explicit escape hatch.

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
*Research completed: 2026-08-12; corrected 2026-08-13 (late-success penalty shipped)*
