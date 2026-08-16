# Incorrect recall: FSRS-6 Again (D1)

Approved next gap vs open FSRS. Proposed [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy.md) stays **Proposed** (do not Accept). Seed: [SEED-004](../../seeds/SEED-004-close-spaced-repetition-scheduling-policy-gap.md). Tracker: [FSRS-COMPATIBILITY-GAP.md](../../research/FSRS-COMPATIBILITY-GAP.md).

Do not mix with in-progress [quick/006](../006-difficulty-card-and-test-cleanup/PLAN.md) (Information-card layout). This plan owns Again scheduling + D1 ADR text, not Vue layout.

## Locked for this plan

- Ordinary **incorrect** (MCQ / just review / spelling fail) maps to FSRS **Again**. Doughnut outcomes stay; no Hard/Easy buttons.
- **Memory update** (S > 0): FSRS-6 post-lapse Stability from D, S, and **Retrievability R** (elapsed whole hours vs Stability), plus Again next-Difficulty. Frozen default `w` as in `FsrsGoodRecall`. Queue lateness vs `nextRecallAt` is not an input.
- **Due time** stays **now + 12h** (schedule metadata, not the new Stability). 12h is the current default, not a sacred constant.
- After a graded fail, persisted Stability is **at least 1 hour** (ADR already forbids 0 after a grade).
- Unset D on S > 0 fail is treated as **5**, same as success.
- **New (S = 0) fail** stays S = 0 + 12h.
- Confusion, commissioned, Tutor 2, requested retention, RecallLog, relearning steps: **out of this plan**.
- No schema. Do not rewrite historical fails.
- Thinking time stays correct-only.

## Doughnut today

`MemoryTracker.recallFailed` sets Stability via `hoursAfterSpacingDelta(S, −2)` (Fibonacci `DEFAULT_SPACES`) and `nextRecallAt = now + 12h`. It does not use elapsed time or update Difficulty. Accidental-match **primary** is this path (`S = 200` in that fixture). Existing E2E 12h checks (`recall_quiz_ai_question`, `accidental_match_reveal`) are mostly **first-grade fail on New** — they do not cover S > 0 Again.

## Discoveries

- FSRS Again from D=5 typically **clamps Difficulty to 10** (weak mean reversion). That is the Memory Tracker signal once Difficulty is persisted.
- Post-lapse S from first success (S=24h, D=5, on-time) is on the order of **hours, not 0**. Today that fail stores S=0.
- Accidental-match “confusion is strictly weaker” may not hold numerically vs post-lapse S at low S until a later confusion slice. Leave confusion on −1 ladder.
- `hoursAfterSpacingDelta` must remain for confusion (−1) and commissioned (+1) until those slices.
- Do **not** extract shared FSRS helpers before the first Again behavior. Add Again next to the existing FSRS helper (or a sibling). Dedupe `W` / R only in post-change-refactor after a second consumer exists.

## Tests

- **Unit:** `SpacedRepetitionRecallSchedulingTest` via `markAsRecalled(..., false, ...)` — same grain as Good. One new assertion focus per slice.
- **Accidental-match edge:** keep 12h / recallCount / lastRecalledAt; stop pinning `ForgettingCurve.failed()` once canonical S is pinned.
- **E2E:** `spaced_repetition.feature` (`@mockBrowserTime`, no OpenAI). Success then fail, then Memory Tracker. Existing New+fail 12h E2E must stay green. Tag new scenarios `@wip` until that slice’s post-condition passes.
