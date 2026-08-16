# CONTEXT: Close remaining FSRS scheduling gap

**Scope:** Remaining leftover Tutor scores, confusion off the spacing ladder, then lock the rest of Proposed ADR 0003 (implement or explicitly defer). Do not accept the ADR.

## Locked this session (Tutor 3 / 2 / 1 / 0)

| Score | Decision |
|-------|----------|
| **3** | FSRS-6 **Hard**. New: D=5, S=24h (same as 4/5, not FSRS first-rating). S>0: Hard SInc (`Good increment × w15`), Hard next-D, overdue extra. Next S ≥ current S and **strictly shorter** than the same state under score 4. Due from Stability. No 12h retry. Effort neutral. |
| **2** | **Doughnut extension** (choice A). Not Hard. No growth; accumulated Stability × 0.8 (initial assimilate = 0, so 80% of current S), whole hours. **D unchanged.** Ignore elapsed time. No overdue extra. Due from new S. |
| **1** | FSRS-6 **Again** memory (post-lapse S + Again next-D). Due = `lastRecalledAt + stability`, **not** +12h. |
| **0** | **Same schedule as 1.** Product text still differs. Do not reset to S=0 on a graded tracker. |

Shared commissioned rules (already intended): recorded score is a grade (count, `lastRecalledAt`, reschedule); no Feedback → unchanged; effort neutral; late session does not weaken; never ordinary 12h retry.

Keep Tutor **0–5** at the product surface (ADR 0005). Publish a compatibility map in ADR 0003; do not replace the rubric with Anki buttons.

## Remaining gap-doc items (this plan closes them)

**Implement (user-visible schedule):** leftover Tutor 3/2/1/0; confusion off `DEFAULT_SPACES`.

**Lock in ADR 0003, no new product surface:**

| ID | Close-as |
|----|----------|
| C1 | Keep Doughnut outcomes; compatibility map in Decision |
| C3 | Thinking time stays bounded within **correct** only; cannot invert outcome |
| D2 | Elapsed 0 whole hours: no extra success increment; no calendar same-day rule |
| E1 | `mark-as-recalled` is a grade; `remove` / `revive` are not |
| E2 | Non-positive interval → **24h** (today `hoursFromSpacingIndex(1)` already 24) |
| Relearning steps | Ordinary fail due stays **+12h** metadata; no step list |

**Explicitly defer (Decision one-liners, no columns/UI):** B2 Settings `r ≠ 0.9`; B4 lapses; C4 just-review Hard/Easy buttons; E3 fuzz / max interval; E4 fitting / per-user weights; E6 RecallLog. Success due stays `last + S` because implicit `r = 0.9`.

## Confusion (recommended lock at slice 5)

Not a grade. Not FSRS. Must stay strictly weaker than ordinary incorrect.

When S > 0: next S = whole-hour **midpoint** of current S and FSRS-6 Again S for the same D (null → 5), elapsed whole hours vs `lastRecalledAt`, and current S. Floor 1h. Must be `<` current S and `>` Again S when rounding still distinguishes them. D, `lastRecalledAt`, `recallCount` unchanged. Due never later (`min(existing due, last + new S)`). S = 0 stays 0.

**Jidoka** before coding slice 5 if this formula is rejected.

## Cleanup limits

- **No Flyway.** D/S already persist. Do not replay history.
- **Do not delete** `DEFAULT_SPACES` / `hoursFromLegacyIndex` / `StabilityIndexToHoursBackfill`: committed `V300000260` still replays them on fresh DBs. Stop **live** scheduling from walking the ladder (`applyScore` leftover, `hoursAfterSpacingDelta` for confusion).
- Thinking-time scale still uses `LEGACY_INDEX_STEP`; leave it unless a live caller of the ladder dies in the same slice.
- After the last slice: shrink ADR Working draft; fold spent gap-doc discussion into Decision; seed/STATE match final policy. Do not keep a planning diary.

## Tests

Stable boundary: `LearningSessionRecordTests` / `commissioned_learning_session.feature` for Tutor scores; existing accidental-match controller tests for confusion. Pin unique FSRS hours like 102 / 169 — do not invent percentages. Score 0 asserts **same as 1**, not a second full Again shape.

## Out of scope

Accept/reject ADR 0003. Settings retention knob. RecallLog / fitting. Relearning step list. Just-review extra buttons. Lapse column. Fuzz / max interval. Score-5 cohesion plan 013. Confusion as a commissioned score.
