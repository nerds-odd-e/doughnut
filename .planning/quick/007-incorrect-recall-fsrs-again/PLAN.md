# Plan: Ordinary incorrect recall uses FSRS-6 Again

**Status:** planned  
**Goal:** S > 0 incorrect recall persists FSRS-6 post-lapse Stability (from D, S, R) and Again Difficulty; due time stays +12h. Lock that D1 slice in ADR 0003 Decision. No Accept.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. One observable schedule behavior per slice. Do not Accept ADR 0003. Do not delete `DEFAULT_SPACES`.

---

### 1. On-time incorrect (S > 0) persists post-lapse Stability; due in 12h

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker, Stability > 0, Difficulty set (or treated as 5).  
**Trigger:** Ordinary incorrect recall at elapsed whole hours equal to current Stability.  
**Post-condition:** Persisted Stability is FSRS-6 post-lapse S (not −2 ladder steps), at least 1 hour. `nextRecallAt` is still grade time + 12 hours. `lastRecalledAt` / `recallCount` still advance. New (S = 0) fail is unchanged (S = 0 + 12h).

- Lock ADR 0003 Decision **Incorrect recall (Again)**: Doughnut incorrect = FSRS Again; post-lapse S from D, S, R; 12h retry is due-time metadata; floor 1h; New+fail unchanged; confusion/commissioned stay. Move matching Working draft incorrect bullets into Decision. Do not Accept.
- `recallFailed` takes grade time and elapsed whole hours like success. On-time test uses elapsed = S.
- Pin exact next Stability in `SpacedRepetitionRecallSchedulingTest` (same style as `onTimeCorrectRecallUsesFsrsGoodStabilityIncrement`). Also: New fail still 0+12h; fail from small positive S persists ≥ 1h.
- Accidental-match edge: still 12h / count / lastRecalledAt; drop `new ForgettingCurve(200f).failed()`.
- E2E in `spaced_repetition.feature` (`@mockBrowserTime`, no OpenAI): assimilate, just-review **Yes** (first success), next due just-review **No, I need more recall**, Memory Tracker: Stability is not 0, Last-to-Next is 12h. Existing New+fail 12h scenarios stay green.
- Do **not** persist next Difficulty yet. Leave `DEFAULT_SPACES` for confusion/commissioned.

**Done when:** on-time unit pin green; New+fail guard green; targeted E2E green; ADR D1 S+12h text in Decision.

---

### 2. Overdue incorrect leaves more remaining Stability than on-time

Type: Behavior  
Status: planned

**Pre-condition:** Two graded siblings, same S and D.  
**Trigger:** One incorrect at elapsed = S; the other at elapsed = 2S (same thinking-time: none).  
**Post-condition:** Overdue remaining Stability is **strictly greater** than on-time. Both still due in 12h. Extra comes from elapsed vs Stability (low R), not from `nextRecallAt`.

- Delta only in `SpacedRepetitionRecallSchedulingTest` (mirror `overdueCorrectRecallLengthensStabilityMoreThanOnTime`). Do not re-pin on-time hours or 12h.
- No E2E (unit delta; due time unchanged).

**Done when:** overdue > on-time unit test green.

---

### 3. Incorrect recall persists Again Difficulty

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker, Stability > 0.  
**Trigger:** Ordinary incorrect recall.  
**Post-condition:** Persisted Difficulty is FSRS-6 Again next-D (harder; typically 10 from 5). Unset D on S > 0 matches a D=5 sibling. Stability/12h rules from slices 1–2 still hold.

- Canonical D pin (like `onTimeCorrectRecallUpdatesDifficultyTowardEasyInit`). Unset-D sibling only asserts D (and S if that is the unique fill-in delta vs D=5).
- Extend the slice 1 E2E: Memory Tracker Difficulty after that fail is the Again number (from 5 → 10 with frozen `w`).
- Wrap-up: `failed()` / Again helpers must not call `hoursAfterSpacingDelta`. Mark D1 resolved in the gap doc; shrink ADR Working draft; update SEED-004 remaining list (fail S/D done; leftover ladder on confusion/commissioned). Do not Accept ADR 0003.

**Done when:** D unit tests + E2E Difficulty green; fail path off the ladder; D1 recorded as closed in ADR/gap/seed.
