# Keep the recall queue across the same half-day

**Status:** planned — not started.
**Type:** ad-hoc plan (`.planning/quick/`)
**Origin:** slice 14.6 of [001-morning-cognitive-index](../001-morning-cognitive-index/PLAN.md) (`48763b341d`). That slice meant “remount only when the due window rolls over.” The check uses exact `currentRecallWindowEndAt` string equality, which almost never holds.

Do **not** append more slices to 001. Remaining 001 work (21+, 17.1) is the cognitive index, not this queue bug.

## Goal

Opening Recall, or returning to it in the **same half-day**, keeps the already-shown unanswered prompt and the in-progress queue. Shuffle and remount stay for a **new** due-list load only (real window rollover, “load more from next N days,” `dueRecallsRefreshNonce`).

## Value ordering

1. Stop replacing the visible prompt (the flash / reshuffle).
2. Pin that on a two-item E2E path (one due item cannot show a shuffle).
3. Make the due-window timestamp a real half-day identity (what 14.6 assumed).
4. Current prompt still appears if the list is replaced mid-prefetch (spinner).
5. Optional: shuffle a **new** session before first paint, so randomization remains without a flash.

## Key design decisions

- **Same window ⇒ keep `toRepeat` and `currentIndex`.** `onActivated` may still refresh session strips (`dueCommissioned`); it must not call `loadCurrentDueRecalls()` unless the half-day actually changed.
- **Window identity is the half-day boundary**, not leftover sub-seconds from `now`. `alignByHalfADay` today zeros minutes and seconds but not nanos, so menu `getMenuData` and Recall’s `recalling` return different ISO strings in the same half-day. Frontend comparison must treat those as the same window even if the backend is unchanged.
- **Shuffle is session-start randomization**, not a KeepAlive side effect. `loadMore` may still shuffle when it *replaces* the queue. Tests keep skipping shuffle (`getEnvironment() !== "testing"`).
- **Do not shuffle on every activation** to “get randomization.” That is the bug. First visit after slices 1–4 uses the menu-loaded (DB) order unless slice 5 is done.
- **Prefetch single-flight is the spinner.** `fetchRecallPrompts` drops a second call while a loop is in flight; a mid-loop `toRepeat` replace leaves the new index-0 unfetched and `ContentLoader` up with `data-app-busy`.

## Discoveries

- Main menu already sets `toRepeat` (unshuffled) from `getMenuData`. Quiz paints that first card and prefetches 5. Then KeepAlive `onActivated` refetches `recalling`, the window strings differ, `loadCurrentDueRecalls` clears the list, shuffles, resets `currentIndex` to 0 → different first card and another 5 prefetches.
- `RecallPage.activation.spec.ts` only compares **identical** window strings, so 14.6 shipped green while production never matches.
- `recall_timing.feature` detour and “browse notes while recalling” use **one** due tracker; shuffle cannot change the stem. Need two due prompts to see the bug in E2E.
- Viewing last answered stays on Recall (`v-show`); Resume while still on `/recall` does not deactivate. The return path that hits `onActivated` is navigating away (note, notebooks, etc.) and coming back.

## Jidoka checkpoints — stop for developer judgement

**Before slice 5 — still want shuffle?** After slices 1–4, a new half-day visit shows menu/DB order; shuffle runs only on remount (`loadMore` / nonce / real rollover). Slice 5 is the improvement: randomize once **before** the first prompt is shown. Skip 5 if DB order is acceptable.

---

## Slices

Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

### 1. Same half-day activation does not remount the due queue — Behavior `[ ]`

**Pre:** `toRepeat` and `currentRecallWindowEndAt` are already set (menu or an earlier load). The half-day has not rolled over.
**Trigger:** RecallPage KeepAlive activates (first mount inside KeepAlive, or return from a detour).
**Post:** `toRepeat` is unchanged; the first unanswered prompt stays the same.

- Extend `frontend/tests/pages/RecallPage.activation.spec.ts` (do not add a second file):
  - First activation with a menu-loaded queue and a fetched window that is the **same half-day at different millisecond precision** must not call `setToRepeat`.
  - Existing “unchanged window” reactivation case must use that millis-different pair (the identical-string case is what let 14.6 ship).
  - Existing rollover case still remounts (`00:00` vs `12:00`).
- Production: `onActivated` treats two window timestamps as the same half-day when they agree at second precision (parse ISO; ignore sub-second residue and `Z` vs `+00:00`). Only then skip `loadCurrentDueRecalls()`.
- No E2E in this slice — one-item features cannot fail for the right reason. Slice 2 is the user-path net.

### 2. Returning from a note in the same half-day keeps the unanswered prompt — Behavior `[ ]`

**Pre:** At least two due recall prompts this half-day; the learner is on the first unanswered one.
**Trigger:** Open another note (KeepAlive deactivate), then return to Recall.
**Post:** The same question stem is still showing (not a shuffled sibling).

- Extend `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` with a **new** scenario (leave the existing one-item spelling browse as-is). Two assimilated notes, two generated stems; `Then I should be asked "…"` before the detour and again after return.
- Tag and wait with the same conventions as the sibling scenarios (`waitUntilAppIsNotBusy` on return). `@wip` until green.
- Targeted `cypress run --spec` that feature only.

### 3. Due-window timestamp is a stable half-day identity — Behavior `[ ]`

**Pre:** Current time is inside a half-day (e.g. morning in `Asia/Shanghai`).
**Trigger:** `recalling` (or menu `recallStatus`) twice, milliseconds apart, same timezone and `dueindays`.
**Post:** `currentRecallWindowEndAt` is equal.

- `alignByHalfADay` must `.withNano(0)` like `startOfHalfADay` already does.
- Test through `TimestampOperations` (two instants in the same half-day) and keep `RecallsControllerTests` half-day alignment green. Frozen-time tests already compare equal windows; the new test is the residue case they never covered.
- Stop-safe even without slice 1: production menu vs Recall strings start matching. Slice 1 still required for mocked millis-different responses and as defense if serialization format differs.

### 4. Replacing the due list mid-prefetch still shows the current prompt — Behavior `[ ]`

**Pre:** Quiz is prefetching (`eagerFetchCount` 5); the first `getRecallPrompt` has not returned.
**Trigger:** `memoryTrackers` is replaced so index 0 is a different tracker.
**Post:** That new current prompt is fetched and shown; `ContentLoader` does not stick after the in-flight calls finish.

- Extend `frontend/tests/recall/Quiz.spec.ts` (gated first fetch, then `setProps` to a new id list). Assert `getRecallPrompt` for the new current id and the contestable prompt visible.
- Production: do not drop a prefetch because one loop is in flight — queue another pass so the current index is fetched after the list change.
- Still needed after slices 1–3: real remounts (rollover, nonce refresh, load more) can replace the list while Quiz is mounted.

### 5. A new recall session is shuffled before the first prompt — Behavior `[ ]`

**Blocked on Jidoka above.** Skip if first-visit DB order is fine.

**Pre:** Menu (or first load) is applying a due list for a window that has no in-progress queue yet.
**Trigger:** Learner opens Recall for that window.
**Post:** The first prompt is already in session order (shuffled once). No later jump to a different first card.

- Shuffle when the due list is **first** applied for a window (likely `MainMenu` `fetchMenuData` / the same place that today writes unshuffled `toRepeat`), not on KeepAlive activation.
- `loadMore` must not shuffle again for that same window if the queue is already the session order.
- Unit: menu (or RecallPage) applies a multi-item `recallStatus` and the stored `toRepeat` is a permutation of the payload, not a second remount. Tests stay deterministic (`testing` env still skips shuffle) — assert “not remounted on activation” plus a non-testing helper only if there is already a seam; do not weaken `getEnvironment() !== "testing"` into flaky order assertions.
- No E2E for permutation (non-deterministic). Slice 2’s “same stem after return” remains the stability net.

## Permanent artifacts (capability-named)

| Artifact | Slices |
|----------|--------|
| `frontend/tests/pages/RecallPage.activation.spec.ts` | 1 |
| `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` | 2 |
| `backend/src/test/java/com/odde/donut/utils/TimestampOperationsTest.java` | 3 |
| `frontend/tests/recall/Quiz.spec.ts` | 4 |

## Out of scope

- Cognitive-index / pace / accuracy / thinking-time work in 001.
- CLI `fetchShuffledDueMemoryTrackerIds` (already shuffles once per fetch; no KeepAlive).
- Removing shuffle entirely (Jidoka on slice 5).
