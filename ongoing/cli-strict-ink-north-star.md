# CLI — strict Ink north star (remaining)

Informal plan; update as work proceeds. **Testing:** observable behavior first — `runInteractive` / Vitest TTY harness / targeted E2E; see `.cursor/rules/planning.mdc` and `.cursor/rules/cli.mdc`.

## Invariant (unchanged)

**No `-c`**, **no piped-stdin interactive shell**, **no** revived `pipedAdapter`. **`processInput` + `defaultOutput`** stays a **test / direct-call harness** for the same command engine as TTY — not a second product UI.

## Strict goal (what the shipped migration deliberately did not finish)

1. **Editable main command line** uses **`@inkjs/ui` `TextInput`** (or an Ink-ecosystem equivalent) as the **only** stdin consumer for that strip — **not** a parallel `useInput` line editor plus `chalk.inverse` paint that *mimics* TextInput (`formatInteractiveCommandLineInkRows` as the primary editor surface).
2. **Hardware cursor coordination** is **dropped** from the default live path once Ink/TextInput owns the caret — **no** pairing `HIDE_CURSOR` with the main line for “fake caret” UX. Keep **only** what is still honestly required (e.g. exit farewell, or a documented exception added to the checklist with JSDoc in the same change).

**Today (baseline):** `LiveColumnInkPanel` uses **`useInput`** + `tryApplyMainCommandLineInkTyping` + `formatInteractiveCommandLineInkRows` (`cli/src/ui/liveColumnInk.tsx`). `interactiveTtyStdout.finalizeDefaultLiveAfterInk` still emits **`HIDE_CURSOR`** (`cli/src/adapters/interactiveTtyStdout.ts`, called from `handleShellRendered` in `interactiveTtySession.ts`). `@inkjs/ui` **`TextInput`** is **uncontrolled** (`defaultValue`-only; cursor at end on mount), so mounting it **enabled** next to the current `useInput` would **double-handle** stdin — the previous plan correctly avoided that.

## Design gate (do first — blocks honest TextInput adoption)

Pick **one** path and record the outcome here (issue link, fork name, or “wait upstream”):

| Option | Notes |
|--------|--------|
| **Upstream** | `ink-ui` gains **controlled** value + **caret index** (or documented recipe for single-owner input). |
| **Fork / patch** | Vendor a minimal `TextInput` fork in-repo or patch package until upstream merges. |
| **Replace primitive** | Use another Ink input component that supports controlled + caret **and** allows one `useInput` owner for the tree. |

**Phase 1 gate decision (closed): `Fork / patch`.**

- Current upstream docs explicitly state `@inkjs/ui` `TextInput` is **uncontrolled** (`defaultValue`, no controlled `value`/caret API): [ink-ui TextInput docs](https://github.com/vadimdemedes/ink-ui/blob/main/docs/text-input.md).
- For Doughnut command-line parity (shared `InteractiveCommandInput` state + explicit caret/history coordination), we need a controlled surface; waiting upstream blocks phase 2, so we proceed with a minimal local patch/fork path.

**Do not** land “half TextInput” (mounted but disabled, or second component) — same rule as phase 15: **no dead UI**, **no duplicate stdin handlers**.

Slash completion styling (inverse segments, grapheme-aware width) must either map onto the chosen component, live in **non-editable** rows (guidance only), or get an explicit **new gate** if product requires pixel-parity with today’s paint.

## Phases (order by risk → user value)

### Phase 1 — Gate closure + spike

- Confirm chosen path from the table above; smallest spike (e.g. Vitest or local TTY) proving **one** stdin owner can edit the line with **caret not** duplicated by the hardware cursor.
- **Verify:** short note in this file; no user-facing change required if spike-only.

**Status:** done.

- Added `cli/tests/textInputSingleOwnerSpike.test.ts` and verified with:
  - `CURSOR_DEV=true nix develop -c pnpm -C /Users/lia/doughnut/cli test tests/textInputSingleOwnerSpike.test.ts`
- Spike result: one mounted `TextInput` receives stdin and supports edit-at-caret behavior (`a`, left, `b` => `ba`) while rendering inverse-caret SGR output from the component itself (no parallel Doughnut line-editor path in this test).
- Spike test removed after phase-2 parity wiring landed (no long-term duplicate test path).

### Phase 2 — Single-owner main line (behavior parity)

- Implement the chosen input primitive so **typing, caret, delete, home/end** and **session state** (`InteractiveCommandInput`) stay correct; **↑↓** draft history and **Tab** focus still match **live column focus** rules (`cli.mdc`).
- Preserve **Enter / Esc / Tab** semantics and integration with `handleCommandLineInkInput` (submit, slash picker dismiss, focus cycle) — likely by **narrow** `useInput` at root **only** for keys the TextInput does not consume, **without** duplicating character insertion (same contract as `mainCommandLineInkTyping.ts` today, or superseded by TextInput callbacks only).
- **Verify:** `pnpm cli:test` interactive + `mainCommandLineInkTyping` tests **updated or replaced** so they still assert **observable** transcripts/behavior, not internal split details unless that API is the deliberate contract.

**Status:** done.

- Main command line now uses `PatchedTextInput` (`cli/src/ui/PatchedTextInput.tsx`) as the single stdin owner for typing/caret/editing in the default command-line panel.
- `LiveColumnInkPanel` no longer runs command-line character editing through its own `useInput` branch; root `useInput` remains for list-selection panels and command-line special-key routing only.
- `InteractiveCommandInput` remains the source of truth; text changes flow via `onCommandLineTyping(next)` and slash picker reset is derived from draft change.
- Verify run:
  - `CURSOR_DEV=true nix develop -c pnpm -C /Users/lia/doughnut/cli test tests/interactive/interactiveTtySession.test.ts tests/interactive/interactiveTtySuggestionScroll.test.ts tests/mainCommandLineInkTyping.test.ts`

### Phase 3 — Slash / placeholder / width

- Restore or re-spec **slash-aware** and **placeholder** presentation under the new editor (may be TextInput-only, or helper rows + plain TextInput — **one** coherent design).
- **Verify:** `pnpm cli:test` (slash completion, `renderer`/`terminalLayout` consumers); targeted E2E `cli_interactive_mode.feature` if live strip shape changes.

**Status:** done.

- `PatchedTextInput` now builds its visible row from `renderer` command-line helpers (`buildCommandInputDraftLines`), restoring slash-command prefix highlighting and grapheme-aware caret rendering in the single-owner TextInput path.
- The TextInput row now applies renderer width fitting (`truncateToWidth`) with prompt-aware available width, so placeholder/draft rendering stays coherent at narrow terminal widths.
- Verify run:
  - `CURSOR_DEV=true nix develop -c pnpm -C /Users/lia/doughnut/cli test tests/slashCompletion.test.ts tests/renderer.test.ts tests/interactive/interactiveTtySession.test.ts`

### Phase 4 — Remove hardware cursor from default live path

- After the main line no longer relies on hiding the HW cursor for Ink-drawn caret, remove or narrow **`hideCursor` / `finalizeDefaultLiveAfterInk`** so default command-line paint does not emit **`HIDE_CURSOR`**.
- Update **`ttyEntry.ts`** checklist and **`.cursor/rules/cli.mdc`** “approved non-Ink” table — **no silent** new `process.stdout.write` paths.
- **Verify:** `pnpm cli:test`; real TTY smoke (no double caret, no flash); E2E if PTY-visible.

**Status:** done.

- `interactiveTtyStdout.finalizeDefaultLiveAfterInk` no longer emits `HIDE_CURSOR`; default command-line live paint now emits only the input-ready OSC suffix.
- `hideCursor()` remains for explicit exception paths (fetch-wait); `showCursor()` remains on shutdown.
- Updated the non-Ink write checklist in `cli/src/adapters/ttyEntry.ts` and added the same policy table to `.cursor/rules/cli.mdc`.
- Verify run:
  - `CURSOR_DEV=true nix develop -c pnpm -C /Users/lia/doughnut/cli test tests/interactive/interactiveTtySession.test.ts tests/interactive/interactiveTtyTokenList.test.ts tests/interactive/interactiveTtyMcq.test.ts`

### Phase 5 — Cleanup

- Delete dead helpers only when **no** test or product path needs them; **no** duplicate editor code paths.
- **Verify:** `pnpm cli:lint`; `rg` for orphaned `formatInteractiveCommandLineInkRows` / caret-only paths if fully superseded.

### Phase 6 — Readline `keypress` bridges (successor to old “phase 16”)

**Goal:** Shrink `stdin.on('keypress')` in `interactiveTtySession.ts` wherever Ink can own the key **without** TTY stdin ordering or raw-mode lifecycle regressions.

**Today (baseline):** The handler does **Ctrl+C** (exit before other routing) and **fetch-wait Esc** → `cancelInteractiveFetchWaitFor` + `drawBox`. **`FetchWaitDisplay`** deliberately has **no** active Ink `useInput`: it must match the old **disabled** `@inkjs/ui` `TextInput` footprint (`{ isActive: false }`). Mounting **active** `useInput` on that strip broke the fetch-wait → recall / MCQ transition on a real PTY (`cli_recall.feature` saw no stdout growth after raw keypresses).

**Re-open this phase** only with a design that satisfies at least one of:

- Fetch-wait cancel without an **extra** active Ink stdin owner on the loading strip (e.g. different primitive than `useInput`, or Ink/raw-mode fixes that preserve the disabled-TextInput lifecycle), **or**
- Explicit product + harness contract: e.g. guaranteed readiness OSC (or equivalent) after single-key handling so E2E can wait without `len > lenBeforeSend`.

**Vitest:** Esc that must hit the readline bridge uses **`pressKey(stdin, 'escape')`** (`interactiveTestHelpers.ts`); **`pushTTYCommandEscape`** pushes bytes for Ink-only paths — do not assume `Readable.push('\x1b')` emits readline `keypress` on the mock TTY.

**Verify:** `pnpm cli:test` (`interactiveTtyFetchWaitEsc`, etc.); targeted E2E `e2e_test/features/cli/cli_recall.feature` (and any feature that hits fetch-wait then recall/MCQ).

## Phase discipline

Before closing each phase: tests green for that slice, **no dead code**, update this doc’s snapshot (what’s done / what’s next). Deploy gate per team habit (`planning.mdc`).

## References

- Fetch-wait UI + readline bridge: `cli/src/ui/FetchWaitDisplay.tsx`, `cli/src/adapters/interactiveTtySession.ts` (`stdin.on('keypress')`)
- Current live column: `cli/src/ui/liveColumnInk.tsx`, `cli/src/ui/PatchedTextInput.tsx`
- Cursor + OSC hooks: `cli/src/adapters/interactiveTtyStdout.ts`, `cli/src/adapters/interactiveTtySession.ts` (`handleShellRendered`)
- Ink UI: `@inkjs/ui` `TextInput` (package version in `cli/package.json`)
