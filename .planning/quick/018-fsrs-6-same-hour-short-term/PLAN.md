# Plan: Same-hour success uses FSRS-6 short-term Stability

**Status:** in progress (slice 1 done)  
**Index:** ad-hoc under `.planning/quick/` — on last-slice wrap-up, update `.planning/STATE.md` remaining FSRS gap (D2 short-term done). Do not Accept ADR 0003.

**Goal:** At elapsed whole hours 0 and S > 0, a success grade updates Stability with published FSRS-6 short-term `S'(S, G)` (clamp SInc ≥ 1). Again stays post-lapse. No migration.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Each slice is one Behavior or Structure, stop-safe, **one commit**. Commit bound is part of the slice: do not include the next slice’s files or assertions.

---

### 1. ADR: same-hour success uses FSRS-6 short-term

Type: Structure  
Status: done

**Learning:** Phrase **SInc ≥ 1** as a no-shrink multiplier (Hard 24 and Good 72 stay put), not a 1-hour additive floor.

Structure change: Proposed ADR 0003 **Whole-hour elapsed-time precision** now locks elapsed 0 and S > 0 success grades to published FSRS-6 short-term next Stability. Status still Proposed. Immediate next: slice 2 implements same-hour Good.

---

### 2. Same-hour Good on a first-interval tracker grows Stability to 25

Type: Behavior  
Status: planned

**Pre-condition:** Tracker with Stability **24**, Difficulty **5**, elapsed whole hours **0** (including a sub-hour remainder). Not New (S = 0).  
**Trigger:** Ordinary correct recall (no thinking time).  
**Post-condition:** Persisted Stability is **25**. Due is 25 hours after the grade. A sibling at Stability **72** and elapsed 0 stays **72** (clamp).

**Commit bound:** Good path only (`afterGoodRecall` / `recalledSuccessfully`). One unit pin of 25; keep the existing 72h elapsed-0 “does not grow” pin. One E2E in `e2e_test/features/recall/spaced_repetition.feature`: first yes (New → 24), then same hour **do more recall** / yes, Memory Tracker shows Stability **25** and 25 hours between last and next. Do not change Easy, Hard, or Again. Do not edit gap/seed docs.

- Unit: `SpacedRepetitionCorrectRecallSchedulingTest` or extend `SpacedRepetitionEarlyRecallAdjustmentTest` — unique claim is 24→25 at elapsed 0. Do not re-assert on-time 266 or overdue extra.
- Production: when elapsed is 0 and S > 0, Good next S from FSRS-6 short-term with `G = GOOD`, S in days, round to whole hours, clamp SInc ≥ 1. Elapsed ≥ 1 hour unchanged.
- Thinking time still applies after. Existing `ForgettingCurveThinkingTimeTest` elapsed-0 comparisons should stay green (on-time still > same-hour).
- E2E: `@mockBrowserTime`; first assimilate+yes is New init (24), not this formula.

**Done when:** unit pin is 25; E2E shows 25; 72h elapsed-0 still 72; Easy/Hard/Again unchanged; `pnpm backend:test_only` and `pnpm cypress run --spec e2e_test/features/recall/spaced_repetition.feature` green.

---

### 3. Same-hour Easy grows Stability more than Good

Type: Behavior  
Status: planned

**Pre-condition:** Tracker with Stability **24**, Difficulty **5**, elapsed whole hours **0**.  
**Trigger:** Easy recall (Tutor score **5** / `recalledEasily`).  
**Post-condition:** Persisted Stability is **43**. Strictly greater than the same state under Good (25).

**Commit bound:** Easy path; extract a shared short-term helper if Good and Easy would duplicate. One unit or HTTP pin of 43 (do not re-assert Good 25). Also pin Hard same-hour at S=24 stays **24** (clamp on the shared helper) so Hard matches the ADR without a fourth slice. Do not change Again. No E2E (float path already shown for Good; Tutor 5 on-time E2E already exists).

- HTTP: `LearningSessionRecordTutorFeedbackTests` — second score **5** in the same hour, or `MemoryTracker.recalledEasily` at elapsed 0. Canonical Easy claim is 43; Hard claim is “does not shrink.”
- Production: `afterEasyRecall` / `afterHardRecall` at elapsed 0 use short-term with `G = EASY` / `HARD`. New (S = 0) still inits 24, not this formula.

**Done when:** same-hour Easy is 43; Hard at 24 stays 24; Again at elapsed 0 still post-lapse.

---

### 4. Tracker and leftover short-term placeholder docs

Type: Structure  
Status: planned

Structure change: research/seed/STATE match shipped short-term success. Immediate next: none in this plan (deferred knobs unchanged).

**Commit bound:** `.planning/research/FSRS-COMPATIBILITY-GAP.md`, `.planning/seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md`, `.planning/STATE.md`. Do not Accept ADR 0003. Do not add an unused lapse column. Keep `DEFAULT_SPACES` for `V300000260` replay.

- Gap doc: same-hour success short-term is in Decision/code; D2 is not a remaining deferred knob. Leave B4 / C4 / E3 / E4 / E6.
- Seed: remaining trigger is accept ADR 0003 plus those deferred IDs.
- STATE: remaining FSRS gap no longer lists short-term as open.

**Done when:** no “future short-term” / D2-open wording remains outside this plan directory; deferred list unchanged.
