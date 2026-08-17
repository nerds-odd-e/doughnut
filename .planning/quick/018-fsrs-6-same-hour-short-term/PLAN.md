# Plan: Same-hour success uses FSRS-6 short-term Stability

**Status:** in progress (slices 1–3 done)  
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
Status: done

Same-hour Easy at S=24 → **43**; Hard stays **24**. Shared formula remains `Fsrs.hoursAfterShortTermRecall`; per-grade elapsed-0 routing stayed in Easy/Hard modules. Again unchanged.

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
