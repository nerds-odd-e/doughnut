# CLI — SessionScrollback (replace in-tree past messages)

**Status:** Phases 1–2 **done**. Phase 3 below is **not started**.

**Intent:** One Ink `<Static>` region for append-only session history above the live column (stage + `MainInteractivePrompt`). **`SessionScrollback`** stays **content-agnostic** (generic `items` + render child). Shell transcript factories (`TranscriptItem`) live in **`interactiveCliTranscript.tsx`**; recall “answered” lines use **`recallAnsweredScrollback.tsx`** (`RecallAnsweredItem` + `RecallAnsweredRow`); **`interactiveSessionScrollback.tsx`** merges both kinds into one `<SessionScrollback>` (still a single Static). Aligns with Ink’s single static node and with `ongoing/cli-recall-answered-questions.md`.

**Out of scope for this document:** Full rich recall answered UI — that stays in the recall plan; Phase 2 is a **minimal** recall-driven append to prove the append API.

**Constraints**

- **Exactly one** `<Static>` under the interactive Ink root (Ink 6: singular static output).
- **SessionScrollback** does not import domain transcript types; callers pass `T extends { id: string }` and a `children` render function.
- **Stable keys:** every item has a unique `id` (currently `crypto.randomUUID()` per append) for Static child keys — no index keys.
- **Terminal layout:** reuse existing width / wrapping helpers for new transcript shapes; no raw string `.length` for columns.

---

## Phase 1 — SessionScrollback + interactive shell parity (**shipped**)

**User-visible outcome:** Same as before migration: version line, committed user blocks (gray + spacing), assistant lines, stage + command line; `/exit` ordering unchanged (`useEffect` after assistant line).

**Implemented**

- **`cli/src/sessionScrollback/SessionScrollback.tsx`** — Generic `<SessionScrollback<T>>` wrapping `<Static items={…}>`, `SessionScrollbackItem = { id: string }`, render prop `(item, index) => ReactNode`.
- **`cli/src/sessionScrollback/interactiveCliTranscript.tsx`** — `TranscriptItem` (`user_line` | `assistant_text`), `transcriptUserLine` / `transcriptAssistantText` only (no recall types).
- **`cli/src/sessionScrollback/interactiveSessionScrollback.tsx`** — `InteractiveScrollbackItem` union + `InteractiveSessionScrollback` (one `SessionScrollback`, gap rule between user lines and following assistant or recall lines).
- **`cli/src/InteractiveCliApp.tsx`** — `scrollbackItems: InteractiveScrollbackItem[]`; renders `<InteractiveSessionScrollback />`.
- **`TranscriptMessage`** removed from `interactiveSlashCommand.ts` (folded into transcript model).

**Tests**

- Vitest: `InteractiveCliApp` and related suites green.
- **Ink testing note:** `<Static>` repeats scrollback in **every** captured frame; assertions that count substrings across `frames.join('\n')` can double-count. Prefer **`lastFrame()`** (or equivalent) when asserting “appears once on screen” or row budgets in the final layout.

**Phase-complete when:** (met) Shell-only flows match prior behavior under Static.

---

## Phase 2 — Append API for stages (context); recall answered via own item type (**shipped**)

**User-visible outcome:** Stages append to the shared `scrollbackItems` list via context. Recall outcomes that E2E labels **answered questions** (`Correct!`, `Incorrect.`, `Spell correct: …`, `Reviewed: …`, contest notices) are **`RecallAnsweredItem`** rows from **`recallAnsweredLine()`**, not `transcriptAssistantText`, and are **not** duplicated via `onSettled` for those strings (`onSettled('')` dismisses the stage only).

**Implemented**

- **`cli/src/commands/recall/recallAnsweredScrollback.tsx`** — `RecallAnsweredItem`, `recallAnsweredLine`, `RecallAnsweredRow`.
- **`cli/src/sessionScrollback/interactiveSessionScrollback.tsx`** — merges `TranscriptItem | RecallAnsweredItem` in one `SessionScrollback`.
- **`sessionScrollbackAppendContext`** — append API typed as `InteractiveScrollbackItem`.
- **`InteractiveCliApp` `handleAsyncSlashSettled`** — skips appending when `assistantText === ''`.
- **`RecallSessionStage` / `RecallMcqStage` / `SpellingRecallStage`** — append recall answered lines as above; no in-stage `answeredRecallLines` chrome; no second `<Static>`.

**Tests**

- Vitest: `recallJustReviewInteractive.test.tsx` — `Reviewed: Alpha` after first `y` with two due cards (`lastFrame`).
- E2E: `cli_recall.feature` — answered-question assertions unchanged in intent; two-card step uses **answered questions** for `Reviewed: sedition`.

**Phase-complete when:** (met) Recall answered copy uses recall item type + shared scrollback; one Static; tests green.

---

## Phase 3 — Rich recall answered UI (optional slice; overlaps recall doc)

**User-visible outcome:** Breadcrumb, markdown, styling per `ongoing/cli-recall-answered-questions.md` — build on `RecallAnsweredItem` / `RecallAnsweredRow` (or new kinds) without folding recall into `interactiveCliTranscript.tsx`.

**Work**

- Enrich **`RecallAnsweredRow`** (or split item kinds) and keep **`SessionScrollback`** generic.
- Coordinate copy and E2E with `ongoing/cli-recall-answered-questions.md`.

**Tests:** Follow the recall plan.

**Note:** Phase 2 already routes E2E “answered questions” strings through **`recallAnsweredLine`** + root scrollback; Phase 3 is presentation and persistence polish.

---

## Design notes (carry through)

| Topic | Choice |
|--------|--------|
| Entry identity | `crypto.randomUUID()` per append on transcript items; `id` required for `SessionScrollbackItem` |
| Content vs chrome | **`SessionScrollback`:** generic. **`interactiveCliTranscript`:** shell `TranscriptItem` factories only. **`recallAnsweredScrollback`:** recall answered items + row UI. **`interactiveSessionScrollback`:** composes one list. |
| Shell vs recall (Phase 2+) | `SessionScrollbackAppendProvider` + `useSessionScrollbackAppend`; recall appends `RecallAnsweredItem` |
| `onSettled` | Session summaries, errors, stop lines → parent `assistant_text`; recall answered outcomes → `recallAnsweredLine` + `onSettled('')` where the stage should end without extra assistant line |
| Future stages / recall | Extend **`RecallAnsweredItem`** / row component or add new non-transcript kinds beside `interactiveCliTranscript` |

---

## References

- `cli/src/sessionScrollback/SessionScrollback.tsx` — generic Static wrapper
- `cli/src/sessionScrollback/interactiveCliTranscript.tsx` — shell transcript factories
- `cli/src/sessionScrollback/interactiveSessionScrollback.tsx` — merged scrollback UI
- `cli/src/sessionScrollback/sessionScrollbackAppendContext.tsx` — append API for stages
- `cli/src/commands/recall/recallAnsweredScrollback.tsx` — recall answered scrollback rows
- `cli/src/InteractiveCliApp.tsx` — `scrollbackItems`, stage + prompt layout
- `cli/src/commands/interactiveSlashCommand.ts` — stage props (`InteractiveSlashCommandStageProps`)
- `cli/src/commands/recall/RecallSessionStage.tsx` — recall session orchestration
- `ongoing/cli-recall-answered-questions.md` — answered blocks persistence
- Ink 6: single static output; `<Static items={…}>` append-only semantics
