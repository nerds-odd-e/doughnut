# 007 leftover: weak Again pins and ADR contradiction

Inspected 007 commits `7e5556251e`…`1a01482ca2`. Proposed ADR 0003 stays Proposed. Do not Accept.

## Not bugs

- Ordinary fail uses `FsrsAgainRecall` (post-lapse S + Again D). `hoursAfterSpacingDelta` remains only for confusion (`-1`) and commissioned (`+1`). Keep `DEFAULT_SPACES`.
- New fail still S=0 + 12h and skips `setDifficulty` (`getStability() > 0` in `recallFailed`). Production matches Decision. The skip is **untested**.
- 12h due vs persisted S is intentional (Again Decision: 12h is schedule metadata).
- Controller `lessThan(oldStability)` / `shouldRepeatInTwelveHours` (MCQ and spelling) are HTTP-boundary smokes, not identity-controller tautologies. Do not delete.
- Unit 17h (S=72, D=5) and E2E Difficulty 10 are different boundaries (formula vs page). Keep both.
- Unset-D sibling vs 5→10 pin: unique claim is “null D treated as 5”. Keep.
- Overdue extra at first-success S=24h both round to **8h** (on-time 7.60, 2× 8.31). The overdue unit pin at S=72 (17 vs 18) is the unique claim. Do not add an S=24 overdue test.

## Meaningful leftover

### 1. E2E remaining-Stability `> 0` does not prove post-lapse

After first-success Yes, Stability is already **24**. Fail should persist **8h** (FSRS-6 post-lapse, D=5, on-time, frozen `w`). `expectRemainingStability() > 0` still passes if fail **does not change S**.

12h Last-to-Next and Difficulty 10 prove a fail happened. They do **not** prove remaining S is post-lapse (old ladder stored S=0 + 12h; a no-op would keep 24). Pin **8**, same style as the 17h unit pin. Then delete `expectRemainingStability`.

### 2. New-fail Difficulty unset is untested

`recallFailed` only sets D when S>0. `newTrackerIncorrectRecallKeepsZeroStabilityAndTwelveHourDue` does not assert D stays null. A “always `setDifficulty` like success” refactor would set New fail to Again D=10. Add `nullValue()` on the existing New-fail test (delta only).

### 3. 1-hour floor test is `≥ 1`, not the fixture pin

S=1h, D=5, on-time raw ≈ 0.41h → round 0 → floor **1h**. `greaterThanOrEqualTo(1f)` would still pass if the floor vanished and the formula later returned 5h. Pin `equalTo(1f)` like 17h.

### 4. ADR general Stability bullets contradict Again

Decision still says after a grade `nextRecallAt = lastRecalledAt + stability` and persisted S=0 is not allowed. Again Decision says due is **grade+12h** and New fail stays S=0. Qualify the general bullets. Do not Accept.

## Rejected (do not plan)

- Do not extract shared hours↔days / `elapsedWholeHours` helpers (no following behavior slice).
- Do not collapse `difficultyAfterAgainRecall` one-liners as their own slice.
- Do not make `ForgettingCurve.failed` package-private as its own slice (fold into wrap-up only if that slice already touches the class).
- Do not delete `DEFAULT_SPACES` or the −1/+1 ladder.
- Do not add Difficulty E2E beyond tightening the existing scenario’s Stability pin.
- Do not restore ObjectMapper / JSON show tests.
