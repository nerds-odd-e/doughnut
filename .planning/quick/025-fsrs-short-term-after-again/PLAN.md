# Plan: FSRS-6 short-term After-Again

**Status:** complete  
**Goal:** When Stability > 0 and elapsed whole hours are **< 24**, Again uses the same published FSRS-6 short-term `S'(S,G)` as Hard/Good/Easy. Elapsed **≥ 24** stays post-lapse. Existing S/D/due change going forward only.

## Decisions (accepted 2026-08-19)

- Close the ADR exception “Again is post-lapse at every elapsed.” One time gate for all four G: elapsed whole hours **< 24** → short-term; **≥ 24** → long-term (Again = post-lapse). Not ts-fsrs `t === 0`.
- Short-term SInc may be **< 1** for Again (G=1). Clamp SInc ≥ 1 only for **G ≥ 2**. Keep the **1 hour** whole-hour floor after Again.
- No Flyway. Do not replay RecallLog. Do not invert post-lapse to recover previous S.
- Confusion keeps the midpoint of current S and Again S; it inherits the new Again S. No separate confusion rule.
- ADR 0003 stays **Proposed**. Do not accept it. Do not start **E4** fitting. Do not add the long-term After-Again cap `min(S'_f, S / e^{w_{17} w_{18}})`.

## Out of scope

- Per-user / fitted weights (E4)
- Accepting ADR 0003
- Flyway squash / deleting `V300000260` replay
- Long-term After-Again upper bound
- FSRS `S_MIN` = 0.1 day (keep 1 hour)

## Observable pins (frozen `Fsrs.W`)

| Pre | Trigger | Post |
|-----|---------|------|
| New → first Good **55h**, Difficulty `D0(Good)` | same-hour just-review No | Stability **18**, due **18h**, Difficulty **7.3945026** (Again next-D unchanged) |
| Graded tracker S=**72h**, D=5 | same-hour Again (elapsed 0 or 23) | Stability **24** (not on-time post-lapse **17**) |
| Same 72h tracker | Again at elapsed **≥ 24** (on-time = 72) | post-lapse **17** (existing pin) |
| New | first Again | **5h** / `D0(1)` (unchanged) |
| First Good **55h** | on-time Again (elapsed 55) | post-lapse **15** (existing pin) |

Short-term `S'(S,G)` ignores elapsed inside the window: Again at 0h and at 23h get the same next S.

Existing E2E `Memory Tracker shows Stability and Again Difficulty after incorrect just-review` (day 3 / 15h after first Good) is long-term After-Again; keep **15**.

## Slices

### 1. Lock short-term After-Again in ADR 0003

**Type:** Structure  
**Status:** done

Proposed ADR 0003 Decision uses one `< 24` / `S > 0` gate for all four G. Status stays Proposed; Working draft empty.

**Learning:** **Lapses** now points at **Incorrect recall (Again)** instead of equating After-Again with always post-lapse. Do not split the 400+ line ADR.

### 2. Same-hour Again after first Good uses short-term Stability 18

**Type:** Behavior  
**Status:** done

Same-hour just-review No after first Good is Stability **18** / **18h** due (not post-lapse **15**). Unit pins: 72h same-hour and elapsed 23 → **24**; long-term on-time 72h **17** and after first Good **15** unchanged. `FsrsAgainRecall` uses `hoursAfterShortTermRecall` when elapsed **< 24**; SInc ≥ 1 only for G ≥ Hard; 1h floor.

**Learning:** TDD red was same-hour post-lapse **13**, not on-time **15**.

### 3. Short-term After-Again leftover cohesion

**Type:** Structure  
**Status:** done

One elapsed `< 24` gate: `Fsrs.hoursAfterShortTermOrLongTerm`. Hard/Good/Easy continue with Stability increase; Again continues with post-lapse. Trackers match the shipped rule. Remaining deferred is still **E4** plus **accept ADR 0003**.

## Not this plan

- Long-term After-Again cap `min(S'_f, S / e^{w_{17} w_{18}})` — later slice if we close that formula gap
- Full RecallLog replay of S/D/due
- Accept ADR 0003
