# Plan: Same-hour success uses FSRS-6 short-term Stability

**Status:** in progress (slices 1–2 done)  
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
Status: done

**Learning:** Same-hour Good increment is 1 hour, so 10s vs 25s thinking time both round to 25; the elapsed-0 thinking-time pin uses 0ms so fast still > base. `Fsrs.hoursAfterShortTermRecall` is the shared helper for slice 3.

Ordinary correct at elapsed 0 and S=24 → Stability **25** (unit + E2E). 72h elapsed-0 still 72. Easy/Hard/Again unchanged.

---

### 3. Same-hour Easy grows Stability more than Good

Type: Behavior  
Status: planned

**Pre-condition:** Tracker with Stability **24**, Difficulty **5**, elapsed whole hours **0**.  
**Trigger:** Easy recall (Tutor score **5** / `recalledEasily`).  
**Post-condition:** Persisted Stability is **43**. Strictly greater than the same state under Good (25).

**Commit bound:** Easy path; extract a shared short-term helper if Good and Easy would duplicate. One unit or HTTP pin of 43 (do not re-assert Good 25). Also pin Hard same-hour at S=24 stays **24** (clamp on the shared helper) so Hard matches the ADR without a fourth slice. Do not change Again. No E2E (float path already shown for Good; Tutor 5 on-time E2E already exists).

- HTTP: `LearningSessionRecordTutorFeedbackTests` — second score **5** in the same hour, or `MemoryTracker.recalledEasily` at elapsed 0. Canonical Easy claim is 43; Hard claim is “does not shrink.”
- Production: `afterEasyRecall` / `afterHardRecall` at elapsed 0 call existing `Fsrs.hoursAfterShortTermRecall` with `G = EASY` / `HARD`. New (S = 0) still inits 24, not this formula.

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
