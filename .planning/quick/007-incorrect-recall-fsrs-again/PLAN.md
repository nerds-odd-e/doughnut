# Plan: Ordinary incorrect recall uses FSRS-6 Again

**Status:** planned  
**Goal:** S > 0 incorrect recall persists FSRS-6 post-lapse Stability (D, S, R) and Again Difficulty; due time stays +12h. Lock D1 in ADR 0003 Decision. No Accept.

**Context:** [CONTEXT.md](./CONTEXT.md)

Sequential. Each slice is one Behavior, stop-safe, ~one commit. Do not Accept ADR 0003. Do not delete `DEFAULT_SPACES`.

Canonical post-conditions (12h due, lastRecalledAt / recallCount) are asserted **once** in slice 1. Later slices assert only their delta.

---

### 1. On-time incorrect (S > 0) persists post-lapse Stability

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker, Stability > 0, Difficulty 5 (or unset, treated as 5).  
**Trigger:** Ordinary incorrect recall at elapsed whole hours = current Stability.  
**Post-condition:** Persisted Stability is FSRS-6 post-lapse S (not −2 ladder steps). `nextRecallAt` is still grade time + 12 hours. New (S = 0) fail is unchanged (S = 0 + 12h).

- Lock ADR 0003 Decision **Incorrect recall (Again)** for this outcome only: Doughnut incorrect = FSRS Again; post-lapse S from D, S, R; 12h is due-time metadata; New+fail unchanged; confusion/commissioned stay. Floor-1h and Again next-D wait for later slices. Do not Accept.
- One on-time Stability pin in `SpacedRepetitionRecallSchedulingTest` (Good SInc style) plus a New-fail guard. Accidental-match edge: keep 12h / count / lastRecalledAt; drop `new ForgettingCurve(200f).failed()`.
- `recallFailed` passes elapsed whole hours. S ≤ 0 keeps today’s 0. Do not persist next Difficulty. Do not add E2E yet.

**Done when:** on-time unit pin green; New-fail guard green; targeted backend tests green.

---

### 2. Memory Tracker shows post-lapse Stability after incorrect

Type: Behavior  
Status: planned

**Pre-condition:** Learner assimilated, then just-review **Yes** (first success, S = 24h).  
**Trigger:** Next due just-review **No, I need more recall**.  
**Post-condition:** Memory Tracker Stability is not 0; Last-to-Next is 12 hours.

- `spaced_repetition.feature` (`@mockBrowserTime`, no OpenAI). `@wip` until green, then remove `@wip`.
- Page object: read Stability; reuse 12h Last/Next. Do not assert Difficulty yet.
- Existing New+fail 12h scenarios stay green (no product change expected).

**Done when:** that E2E scenario green without `@wip`.

---

### 3. Harder Difficulty leaves less remaining Stability on incorrect

Type: Behavior  
Status: planned

**Pre-condition:** Two graded siblings, same S, on-time elapsed; Difficulties 3 and 8.  
**Trigger:** Ordinary incorrect recall.  
**Post-condition:** The harder tracker’s remaining Stability is **strictly less**. Due time still +12h (do not re-assert).

- Delta only in `SpacedRepetitionRecallSchedulingTest`. No E2E.

**Done when:** harder < easier remaining-S unit test green.

---

### 4. Overdue incorrect leaves more remaining Stability than on-time

Type: Behavior  
Status: planned

**Pre-condition:** Two graded siblings, same S and D.  
**Trigger:** One incorrect at elapsed = S; the other at elapsed = 2S.  
**Post-condition:** Overdue remaining Stability is **strictly greater**. Extra from elapsed vs Stability (low R), not `nextRecallAt`.

- Delta only (mirror `overdueCorrectRecallLengthensStabilityMoreThanOnTime`). No E2E.

**Done when:** overdue > on-time unit test green.

---

### 5. Incorrect from 1-hour Stability persists at least 1 hour

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker, Stability = 1 hour.  
**Trigger:** Ordinary incorrect recall.  
**Post-condition:** Persisted Stability is **≥ 1**. Still due in 12h (do not re-assert).

- One unit test. This is the floor; slice 1’s 72h/24h pin need not hit it.

**Done when:** 1-hour-S fail unit test green.

---

### 6. Incorrect recall persists Again Difficulty

Type: Behavior  
Status: planned

**Pre-condition:** Graded tracker, Stability > 0.  
**Trigger:** Ordinary incorrect recall.  
**Post-condition:** Persisted Difficulty is FSRS-6 Again next-D (harder; from 5 → 10 with frozen `w`). Unset D matches a D=5 sibling (assert D only).

- Canonical D pin plus unset sibling in `SpacedRepetitionRecallSchedulingTest`. Lock Again next-D in ADR 0003 Decision. Do not Accept.
- No E2E in this slice.

**Done when:** both D unit tests green.

---

### 7. Memory Tracker shows Again Difficulty after incorrect

Type: Behavior  
Status: planned

**Pre-condition:** Same success-then-fail path as slice 2.  
**Trigger:** Open Memory Tracker after that incorrect.  
**Post-condition:** Difficulty is the Again number (5 → 10). Stability/12h already covered — do not re-assert.

- Extend the slice 2 scenario (one extra Then).
- Wrap-up: fail path must not call `hoursAfterSpacingDelta`; D1 resolved in the gap doc; shrink ADR Working draft; SEED-004 remaining = confusion/commissioned leftover ladder. Do not Accept ADR 0003.

**Done when:** E2E Difficulty green; fail off the ladder; D1 closed in ADR/gap/seed.
