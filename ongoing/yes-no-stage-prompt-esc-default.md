# Plan: YesNoStagePrompt — ESC cancel, Enter = default, configurable default

Temporary plan for `cli/src/YesNoStagePrompt.tsx`. Aligns with **Escape-based cancellation** and **stage-local input** in `ongoing/cli-architecture-roadmap.md` (§3.2, §11.3). **Not executed yet** — implementation follows TDD in `.cursor/rules/planning.mdc` when this work starts.

---

## Current behavior (baseline)

- Typed `y` / `n` (case-insensitive) committed with Enter (including PTY `\r` chunk handling) calls `onAnswer(true | false)`.
- **Empty buffer + Enter:** `tryCommit` does nothing (no `onAnswer`). Covered by `cli/tests/recallJustReviewInteractive.test.tsx` (“empty Enter … do not recall”).
- Prompt footer: `(y/n)` only.
- Used from `cli/src/commands/recall/RecallJustReviewStage.tsx` (card + load-more prompts).

---

## Target behavior

1. **Configurable default answer**  
   When the caller opts in, **Enter on an empty (or whitespace-only) buffer** after normalization should commit the **default** and invoke `onAnswer` with that boolean — without requiring the user to type `y` or `n`.

2. **ESC to cancel**  
   **Escape** should invoke a **cancellation** path so the parent stage can leave the prompt (e.g. dismiss, treat as “no”, or return to a safe state). Cancellation is **not** the same as answering “no” unless the parent wires it that way.

3. **Backward compatibility**  
   Existing call sites that must keep “empty Enter does nothing” should do so by **not** passing a default (or by an explicit “no default” representation — see API sketch below).

4. **Unit tests**  
   Cover the new behavior through **Ink + observable frames** (see `.cursor/rules/cli.mdc`: `StageKeyRoot`, `renderInkWhenCommandLineReady` / probe pattern, `waitForFrames` / `waitForLastFrame`, no wall-clock sleeps).

---

## API sketch (to finalize in implementation)

- **`defaultAnswer?: boolean`**  
  - If **omitted** (or `undefined`): keep today’s behavior — empty Enter does not call `onAnswer`.  
  - If **`true` | `false`**: normalized empty line + Enter → `onAnswer(defaultAnswer)`.

- **`onCancel?: () => void | Promise<void>`**  
  - If **omitted**: ESC is a no-op (safe default for stages that are not ready to handle cancel).  
  - If **set**: ESC calls it once per cancel (same `runOnAnswer`-style async tolerance as `onAnswer` if needed).

- **Prompt copy (optional polish in same phase)**  
  Reflect default in the hint, e.g. `(Y/n)`, `(y/N)`, or `(y/n)` when no default — match common CLI conventions and keep width sensible.

**Alternative considered:** `onAnswer: (result: boolean | 'cancel')` — rejected for this plan to avoid forcing every caller to handle a third union member; a dedicated `onCancel` keeps recall handlers typed as `(boolean) => …` and matches roadmap language on cancellation as its own outcome.

---

## Phases (scenario-first, small slices)

### Phase 1 — Component contract + regression in isolation

**User-visible slice:** Stages that adopt the new props get Enter-default and ESC-cancel; stages that do not adopt them behave as today.

**Work:**

- Implement `defaultAnswer` + `onCancel` in `YesNoStagePrompt` (Ink `Key`: use **`key.escape`** for ESC; confirm against `ink` types / existing patterns in the repo).
- **`tryCommit` rules (intended):**
  - Normalized line `y`/`Y` → yes; `n`/`N` → no (unchanged).
  - Normalized line empty **and** `defaultAnswer` is set → `onAnswer(defaultAnswer)`.
  - Normalized line empty **and** `defaultAnswer` unset → no-op (unchanged).
  - Non-empty line that is not exactly y/n → no-op (unchanged), unless you later decide otherwise (out of scope unless product asks).
- Add **`cli/tests/YesNoStagePrompt.test.tsx`** (or one focused file): render prompt inside **`StageKeyRoot`** from `cli/tests/inkTestHelpers.ts`, drive stdin, assert **`onAnswer` / `onCancel` mocks** and optionally visible hint text. Cases: empty+Enter with default yes/no; empty+Enter without default does not call `onAnswer`; ESC with/without `onCancel`; existing y/n paths still fire.

**Phase-complete:** Component + tests green; **no requirement** to change recall yet if defaults are unset everywhere.

### Phase 2 — Recall just-review prompts adopt defaults and cancel policy

**User-visible slice:** During `/recall` just-review, Enter on empty can mean an explicit default per prompt; ESC cancels or backs out per product choice.

**Work:**

- In `RecallJustReviewStage`, pass **`defaultAnswer`** and **`onCancel`** for each `YesNoStagePrompt` instance.
- **Product decisions to lock before coding:**
  - **“Yes, I remember?”** — default yes vs no vs no default (affects safety vs speed).
  - **“Load more…”** — default yes/no/none.
  - **ESC** — e.g. same as “no” / end session summary / stay on prompt (roadmap: parent decides).
- Update **`cli/tests/recallJustReviewInteractive.test.tsx`** (and any E2E under `e2e_test/features/cli/` if assertions mention empty Enter or new hint text) so expectations match the chosen defaults and cancel behavior.

**Phase-complete:** Recall flow tested end-to-end for the new keyboard contract on real prompts.

---

## Testing notes (planning.mdc + cli.mdc)

- Prefer **one focused test file** for pure `YesNoStagePrompt` behavior; extend **recall** tests only when recall wiring changes observable behavior.
- **No `sleep` / fixed ms** — use `setImmediate` polling with caps (`waitForFrames`).
- After implementation, run **`CURSOR_DEV=true nix develop -c pnpm cli:test`**; if Phase 2 touches recall E2E, run the relevant **`pnpm cypress run --spec …`** per `.cursor/rules/e2e_test.mdc`.

---

## Cleanup when done

- Remove or archive this file when the behavior is shipped and nothing here is still actionable.
- If prompt vocabulary changes (e.g. `(Y/n)`), consider a one-line touch to **Domain terminology** in `.cursor/rules/cli.mdc` for “Recall y/n confirmations” — only if the doc still matches reality.
