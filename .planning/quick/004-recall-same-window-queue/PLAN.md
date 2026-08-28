# Keep the recall queue across the same half-day

**Status:** in progress — slices 1–4 done. **Stopped at slice 5 Jidoka** (shuffle before first paint?).
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
- **Window identity is the half-day boundary**, not leftover sub-seconds from `now`. Backend `alignByHalfADay` / `startOfHalfADay` truncate to the hour (slice 3). Frontend KeepAlive still treats ISO strings as the same window when they agree at second precision (slice 1), including mocked millis-different pairs and `Z` vs `+00:00`.
- **Shuffle is session-start randomization**, not a KeepAlive side effect. `loadMore` may still shuffle when it *replaces* the queue. Tests keep skipping shuffle (`getEnvironment() !== "testing"`).
- **Do not shuffle on every activation** to “get randomization.” That is the bug. First visit after slices 1–4 uses the menu-loaded (DB) order unless slice 5 is done.
- **Prefetch coalescing.** `fetchRecallPrompts` queues another pass if a loop is already in flight so a mid-prefetch `toRepeat` replace still fetches the new index-0 (slice 4).

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

### 1. Same half-day activation does not remount the due queue — Behavior `[x]`

**Shipped:** `sameHalfDayWindow` in `useRecallPageLoading` (ISO parse, truncate to seconds) skips `loadCurrentDueRecalls()` when KeepAlive fetches the same half-day. Tests: first activation + reactivation with `…00:00.123+00:00` vs `…00:00.456Z`; rollover still remounts.

**Learning:** Frontend second-precision compare is the KeepAlive defense for mocked/serialized format differences. Slice 3 made production menu vs Recall strings equal as well.

### 2. Returning from a note in the same half-day keeps the unanswered prompt — Behavior `[x]`

**Shipped:** New scenario in `browse_answer_and_notes_while_recalling.feature` — two assimilated notes, two generated stems; same stem after visiting `medical` and `I return to recalling` (Recall nav + `waitUntilAppIsNotBusy`). No extra production code (slice 1 was enough).

**Learning:** Resume is not on the unanswered first card; the return path that hits KeepAlive is Recall in the nav. Cypress `localhost` skips shuffle (`getEnvironment() === "testing"`); the two-stem assertion is still the user-path net. Mocked OpenAI E2E needs `OPENAI_API_TOKEN` set on `backend:sut:ci` even with Mountebank.

### 3. Due-window timestamp is a stable half-day identity — Behavior `[x]`

**Shipped:** `alignByHalfADay` and `startOfHalfADay` truncate to the hour (`truncatedTo(ChronoUnit.HOURS)`), so leftover nanos cannot differ between menu and Recall. Residue test: two Asia/Shanghai morning instants with different sub-second residue yield equal windows.

**Learning:** Main had dropped `.withNano(0)` in `b5662122c1`; this restores stable identity. Slice 1’s frontend second-precision compare remains defense for mocked/serialized format differences.

### 4. Replacing the due list mid-prefetch still shows the current prompt — Behavior `[x]`

**Shipped:** `fetchRecallPrompts` queues another pass when a prefetch is already in flight. Quiz test gates tracker 1, replaces the list with ids 6–10, asserts `getRecallPrompt` for 6 and the contestable prompt is visible.

**Learning:** KeepAlive same-window no longer remounts (slices 1–3), but rollover / nonce / load-more still replace `toRepeat` while Quiz is mounted; the queue is still needed.

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
