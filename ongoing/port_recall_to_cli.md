# Port Recall Feature to CLI - Phased Plan

## Context

- **Recall (web)**: Spaced-repetition feature. User sees notes due for recall, answers MCQ/spelling questions or "Just Review" (yes/no). APIs: `GET /api/recalls/recalling`, `GET /api/memory-trackers/{id}/question`, `POST /api/recall-prompts/{id}/answer`, `POST /api/recall-prompts/{id}/answer-spelling`, `PATCH /api/memory-trackers/{id}/mark-as-recalled`.
- **CLI**: Uses `doughnut-api` + `@generated/doughnut-backend-api`; auth via `access-tokens.json` and `withBackendClient(token, fn)`. Needs default token for recall.
- **E2E**: `@withCliConfig` creates temp config dir; `runCliDirectWithArgs` runs bundle; steps use `I have a valid Doughnut Access Token` and `runCliWithConfig`.

---

## Phase 1: Recall status (count) ✅ DONE

**User behavior**: User runs `/recall-status` and sees how many notes to recall today (e.g. "3 notes to recall today" or "0 notes to recall today").

**APIs**: `RecallsController.recalling({ query: { timezone, dueindays: 0 } })` → `DueMemoryTrackers.toRepeat.length`.

**CLI changes**:

- Add `cli/src/recall.ts` with `recallStatus()` calling `RecallsController.recalling()` via default token.
- Add `runWithDefaultBackendClient(fn)` (or equivalent) in `accessToken.ts` for commands needing default token.
- Wire `/recall-status` in `interactive.ts`, register in `help.ts`.

**Tests**:

- **E2E**: User with token + notes due for recall runs `-c "/recall-status"`, sees "N notes to recall today"; user with 0 due sees "0 notes to recall today".
- **UT**: Mock `RecallsController.recalling`; test timezone handling (use `Intl.DateTimeFormat().resolvedOptions().timeZone` or env); test "no default token" error; test empty vs non-empty toRepeat.

**Cleanup**: No dead code; only what E2E and UT use.

---

## Phase 2.1: Recall one Just Review note (title only) ✅ DONE

**User behavior**: User runs `/recall next`; if next note is Just Review (no question), CLI shows note title and prompts "Yes, I remember? (y/n)"; user answers and note is marked as recalled.

**APIs**: `recalling()` → pick first `toRepeat`; `askAQuestion(memoryTrackerId)` → if null/empty, it's Just Review; `markAsRecalled(memoryTrackerId, successful)`.

**CLI changes**:

- In `recall.ts`: `recallNext()` — fetch due list, fetch question for first tracker; if no question, show `note.noteTopology.title`, prompt y/n, call `markAsRecalled`.
- Wire `/recall next` in `interactive.ts`.

**Tests**:

- **E2E**: Background with 1 note due (Just Review). Run `/recall next`, answer y, verify success; run again to see "0 notes to recall" or appropriate message.
- **UT**: Mock question returns null → Just Review path; mock markAsRecalled; test invalid y/n input; test no notes due; test no default token.

**Cleanup**: Remove any unused branches; only code exercised by E2E/UT.

---

## Phase 2.2: Just Review with markdown note content ✅ DONE

**User behavior**: User runs `/recall next`; for Just Review, CLI shows note title + note details (markdown rendered to terminal) before prompting "Yes, I remember? (y/n)". Matches web behavior where full note is visible.

**APIs**: Same as 2.1. Note details come from `MemoryTracker.showMemoryTracker` → `note.details` (or from question/recall prompt when available).

**CLI changes**:

- Add `markdansi` (or equivalent) dependency to CLI.
- Extend Just Review display: after title, render `note.details` as markdown (ANSI) when present.
- Introduce `renderMarkdownToTerminal(md)` helper; use for note details.

**Tests**:

- **E2E**: Background with 1 note due (Just Review) whose details contain markdown (e.g. `**bold**`, list). Run `/recall next`, verify rendered output (title + formatted details), answer y, verify success.
- **UT**: `renderMarkdownToTerminal` outputs ANSI for headings, bold, lists; empty/null details renders nothing; wrapping respects terminal width (or fixed width in non-TTY).

**Cleanup**: No dead code; markdown helper only used where E2E/UT exercise it.

---

## Phase 3: Recall one MCQ note ✅ DONE

**User behavior**: User runs `/recall next`; if next note has an MCQ, CLI shows question and choices (1, 2, 3...), user enters choice number; answer submitted.

**APIs**: `askAQuestion()` → `RecallPrompt` with MCQ; `RecallPromptController.answerQuiz(recallPromptId, { body: { choiceIndex } })`.

**CLI changes**:

- Extend `recallNext()`: when `askAQuestion` returns MCQ, display stem + choices, read choice index, call `answerQuiz`.

**Tests**:

- **E2E**: 1 note due with AI-generated MCQ. Run `/recall next`, choose correct answer, see success.
- **UT**: MCQ display formatting; valid/invalid choice index; answerQuiz success/error; mixed scenario (first Just Review, second MCQ) if needed.

**Cleanup**: No dead code.

---

## Phase 3.1: MCQ with up/down/Enter selection ✅ DONE

**User behavior**: When recalling an MCQ, user can use up/down arrow keys to highlight a choice and Enter to select it (in addition to typing the number). Matches the UX pattern used for access token list selection.

**CLI changes**:

- Extend interactive mode: when `pendingRecallAnswer` is MCQ, enter a "choice selection" state similar to `tokenListItems`; show highlighted choice list; up/down move highlight; Enter submits the highlighted choice.
- Keep typing a number as alternative input (1–N) for piped/non-TTY mode and user preference.

**Tests**:

- **E2E**: Run `/recall next` with MCQ, use down arrow and Enter to select; verify success.
- **UT**: Up/down navigation; Enter submits correct choiceIndex; typed number still works.

**Cleanup**: No dead code.

---

## Phase 4: Recall one spelling note ✅ DONE

**User behavior**: User runs `/recall next`; if next note has spelling, CLI shows prompt (e.g. "Spell: ..."), user types answer; submitted via `answer-spelling`.

**APIs**: `askAQuestion()` → `RecallPrompt` with spelling; `RecallPromptController.answerSpelling(recallPromptId, { body: { spellingAnswer, thinkingTimeMs } })`.

**CLI changes**:

- Extend `recallNext()` for spelling: detect spelling type, prompt for input, call `answerSpelling`.

**Tests**:

- **E2E**: 1 note due with spelling question. Run `/recall next`, type correct spelling, see success.
- **UT**: Spelling prompt format; answerSpelling call; thinkingTimeMs handling (optional).

**Cleanup**: No dead code.

---

## Phase 5: Full recall session (all due) ✅ DONE

**User behavior**: User runs `/recall` (no args); CLI processes all due notes one by one (Just Review, MCQ, spelling) until none left, then shows summary.

**CLI changes**:

- Add `recallSessionMode` in interactive: loop by calling `recallNext` after each answer until none left; show "Recalled N notes" at end.
- Add `/recall` command to `recallCommandDocs`.

**Tests**:

- **E2E**: 2 notes due (both Just Review). Run `/recall`, complete all; verify "Recalled 2 notes".
- **UT**: Loop termination; empty list on start; error mid-session clears mode and logs.

**Cleanup**: Kept `/recall next` for single-note recall.

---

## Phase 6: Load more from future days ✅ DONE

**User behavior**: User runs `/recall`; when no notes due today, CLI offers "Load more from next 3 days? (y/n)"; if yes, fetches with `dueindays: 3` and continues.

**APIs**: `recalling({ query: { timezone, dueindays: 3 } })` (or 7, 14).

**CLI changes**:

- When `toRepeat` is empty, prompt to load more with `dueindays: 3`; if yes, refetch and continue session.

**Tests**:

- **E2E**: 0 due today, 1 due in 3 days. Run `/recall`, answer y to load more, complete that note.
- **UT**: Prompt display; dueindays param; empty response after load more.

**Cleanup**: Remove interim behavior if later replaced (e.g. `/recall --days 7` flag).

---

## Phase 7: Recall substate and `/stop` ✅ DONE

**User behavior**: When recalling (Phases 5/6 session), CLI enters a substate with a different command set. User can type `/stop` to leave recall mode and return to normal input. While in recall state, a "Recalling" indicator is shown below the input box.

**CLI changes**:

- Introduce recall substate: when in `recallSessionMode` (or equivalent), interpret input as recall-specific commands.
- Commands in recall state: `/stop` — exit recall mode, return to normal prompt.
- When in recall state: render a "Recalling" indicator below the input box (status bar / footer area).

**Tests**:

- **E2E**: Run `/recall`, start session; verify "Recalling" indicator visible below input; type `/stop`, verify recall ends and indicator disappears.
- **UT**: Substate routing; `/stop` clears recall mode; indicator visibility tied to recall state.

**Cleanup**: No dead code.

---

## Phase 8: Contest / Regenerate (lower priority)

**User behavior**: After answering MCQ, CLI asks "Contest this question? (y/n)"; if yes, call contest + regenerate; show new question (or skip).

**APIs**: `RecallPromptController.contest`, `RecallPromptController.regenerate`.

**Tests**:

- **E2E**: Answer MCQ, contest, get new question.
- **UT**: Contest/regenerate error handling; reject invalid input.

**Cleanup**: Only add if user value justifies it; else defer.

---

## Interim Behaviors (remove when superseded)

- **Phase 2.1**: Just Review with title only is interim; Phase 2.2 replaces with full note (title + markdown details).
- Any "quick access" command (e.g. `/recall-now` as alias) that gets replaced by `/recall` should be removed.

---

## Technical Notes

1. **Default token**: Add `runWithDefaultBackendClient(fn)` or `getDefaultToken()` in `cli/src/accessToken.ts` and use for all recall commands.
2. **Timezone**: Use `Intl.DateTimeFormat().resolvedOptions().timeZone` for `recalling` (matches web).
3. **SDK**: Use `RecallsController.recalling`, `MemoryTrackerController.askAQuestion`, `MemoryTrackerController.markAsRecalled`, `RecallPromptController.answerQuiz`, `RecallPromptController.answerSpelling` from `packages/generated/doughnut-backend-api/sdk.gen.ts`.
4. **E2E setup**: Reuse `I have a valid Doughnut Access Token`, `runCliWithConfig`; add steps for "notes due for recall" (assimilation + time travel) similar to `e2e_test/features/recall/`.
5. **CLI E2E**: Follow `e2e_test/features/cli/cli_access_token.feature` pattern; new `cli_recall.feature` with `@withCliConfig`.
6. **Terminal markdown (Phase 2.2)**: Use `markdansi` for rendering note details to ANSI; `render(md, { width })` fits CLI ESM/Node setup.

---

## Phase Order Summary

| Phase | Command(s)                   | User Value             | Dependencies                |
| ----- | ---------------------------- | ---------------------- | --------------------------- |
| 1     | `/recall-status`             | See count quickly       | runWithDefaultBackendClient  |
| 2.1   | `/recall next` (Just Review) | Recall one note        | Phase 1                     |
| 2.2   | Just Review + markdown        | Full note visible      | Phase 2.1                   |
| 3     | `/recall next` (MCQ)         | Answer one MCQ         | Phase 2.1                   |
| 3.1   | MCQ up/down/Enter selection  | Better MCQ UX          | Phase 3                     |
| 4     | `/recall next` (spelling)     | Answer one spelling    | Phase 3                     |
| 5     | `/recall`                    | Full session           | Phases 2.2, 3, 4           |
| 6     | `/recall` load more          | Notes from future days | Phase 5                     |
| 7     | Recall substate, `/stop`     | Exit recall mid-session| Phase 5                     |
| 8     | Contest/Regenerate           | Edge case, low priority| Phase 3                     |

Each phase: implement → E2E + UT → remove dead code → commit → push → CD deploy → next phase.
