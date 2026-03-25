# CLI interactive UI — Ink migration (remaining work)

Informal plan; update as work proceeds. **Testing:** observable behavior first — `runInteractive` / E2E; see `.cursor/rules/planning.mdc` and `.cursor/rules/cli.mdc`.

---

## North star

Interactive TTY = **one Ink `render()`** root (`InteractiveShellDisplay`: **`Static`** history + live panel). **End state:** while the shell is active, **one stdin / keyboard owner** (Ink `useInput` and, for lists and confirms, **`@inkjs/ui`** where it matches behavior — see gate 5). **No parallel paths:** **readline** / **`ttyAdapter` keypress** must not still handle typing, confirm, or list keys for modes the tree already owns (**phase 5** closes this).

**Today (until phase 3 ships):** confirm / session y/n still use readline `keypress`; phases **3 → 4** move those into Ink; **phase 5** removes leftover adapter/readline duplicates.

**Non-interactive** (`-c`, piped): **`processInput` + `pipedAdapter`** unless you explicitly unify.

**Adapter rule:** `ttyAdapter` must **not** branch on product concepts (*recall*, *MCQ*, …). Only **mechanism** `TTYDeps` from `interactive.ts` (`buildTTYDeps()`).

**Layout bridge:** `cli/src/renderer.ts` — grapheme-aware width/wrap for **piped** `writeFullRedraw` and for structures that must stay column-exact (input box via `renderBox` / `truncateToWidth`). TTY default live **current guidance** + optional **current prompt** use Ink wrap (phase 1).

**Raw stdout:** Interactive glue lives in **`cli/src/adapters/interactiveTtyStdout.ts`** (OSC, cursor, clear, exit farewell). Ink still renders the live tree on its own stream.

---

## Layering

| Layer | Role |
|-------|------|
| **Business** | `interactive.ts`, `recall.ts`, … — orchestration; exposes data/callbacks to UI. |
| **Interactive UI** | React/Ink components + state; no domain rules beyond dispatching props. |
| **TTY adapter** | Composes shell + `interactiveTtyStdout`; **after migration** does not duplicate key handling for modes owned by the Ink tree (see phase 5). |
| **Ink shell** | `render` / rerender, `Static` + live column. |

---

## Ink vocabulary (use in code/comments)

| Avoid | Prefer |
|-------|--------|
| Modal stack | Conditional subtree / stacked UI state at root |
| Adapter “paints” | Props → `<Box>` / `<Text>` / `Static` |
| History scrollback | `Static` items **if** append-only semantics match product (**decision gate 2**) |

---

## Decision gates (pause and get sign-off)

1. Single Ink root vs islands / hybrid — **stdin ownership**
2. **`Static`** vs rewriting old history lines
3. **`useFocus`** / Tab vs ↑↓ in guidance / selection mode — **default:** preserve current TTY behavior (same ↑↓ meaning for draft history vs selection, same Tab); introduce Tab/focus only if deliberately chosen.
4. Ink `Text` wrap vs `renderer.ts` grapheme wrap (**CJK/emoji**) — **resolved:** TTY default live column uses Ink `Text` `wrap` inside `Box width={terminalWidth}` for current prompt + guidance; piped path unchanged (`buildLiveRegionLines`, `buildSuggestionLines`). Subtle wrap differences vs grapheme-aware wrap accepted (gate 6).
5. **`@inkjs/ui`** vs hand-rolled `useInput` — **resolved:** **complete replacement** toward Ink ecosystem — use **`Select`**, **`ConfirmInput`**, **`TextInput`** from **`@inkjs/ui`** when behavior maps **1:1** (or close enough with thin wrappers). If a primitive does not fit, use Ink **`useInput` inside the live subtree** only — **not** a second handler in **`ttyAdapter`**. Pure policy helpers (e.g. submit-line derivation in **`selectListInteraction`**) may stay **called from** Ink handlers; they are not a duplicate stdin path.
6. Visual parity (stage band, borders) — **declined** for this migration; slimmer Ink look OK
7. **`patchConsole`** / `console.log` vs layout corruption

---

## Completed (no separate phase doc needed)

Ink shell, neutral `TTYDeps`, confirm/MCQ/token/fetch-wait display components, **J1** empty-Enter path **`shellInstance.clear()` before `unmount()`** (avoids stacked boxes — **E2E** `cli_interactive_mode` is the strong check), reverse-video caret in **`CommandLineLivePanel`**, one layout snapshot per `drawBox`, resize → rerender without full-screen clear, **`interactiveTtyStdout`** as single owner of adapter `stdout` writes.

### Phase 1 (done) — default live column: Ink `Box` / `Text` wrap

- **`CommandLineLivePanel`** builds the default live block in Ink (stage band, separators, optional grey current prompt, input box, guidance). No **`buildLiveRegionLinesWithCaret`** on this path; removed unused **`LiveRegionLines.tsx`**.
- **`buildSuggestionLinesForInk`** in **`renderer.ts`**: same rows as **`buildSuggestionLines`** but no per-line **`truncateToWidth`**; **`ttyAdapter`** uses it for **`CommandLineLivePanel`** props. Piped **`writeFullRedraw`** still uses **`buildLiveRegionLines`** + **`buildSuggestionLines`** (grapheme-aware).
- **Ink note:** this Ink version’s **`Text`** has no `width` prop — wrap width comes from a parent **`Box width={terminalWidth}`** per wrappable block. Do **not** put **`width={terminalWidth}`** on the **root** live column **`Box`** (breaks resize: box border stayed at old columns when **`stdout.columns`** changed in Vitest).
- **Verify:** `pnpm cli:test`; **`renderer.test.ts`** covers **`buildSuggestionLinesForInk`**.

---

## Remaining phases (numbered)

**Order:** **2 → 3 → 4 → 5 → 6 (optional) → 7** (phases 1–2 done)

### Phase 2 (done) — `useInput` for main command line (gates 1 & 3)

- **`CommandLineLivePanel`:** `useInput` + `onCommandKey` / `onInterrupt` (Ctrl+C). Main line behavior lives in **`handleCommandLineInkInput`** in **`ttyAdapter`** (same semantics as before: draft, caret, Enter, arrows, Tab, multiline, `/clear`, slash suggestions, draft history, **`INTERACTIVE_INPUT_READY_OSC`** via existing paint path).
- **`stdin.on('keypress')`:** returns early when **`!isAlternateLivePanel()`** so readline does not handle command-line keys; alternate panels (fetch wait, stop confirm, session y/n, MCQ, token list) unchanged.
- **Vitest:** default command line uses **`stdin.push`** sequences (**`pushTTYCommandBytes`**, **`pushTTYCommandEnter`**, **`pushTTYCommandKey`**, **`pushTTYCommandEscape`**) so Ink’s stdin parser sees input; alternate flows keep synthetic **`keypress`** where needed (e.g. MCQ **`/contest`**).
- **Ink `delete`:** treated like backspace in **`handleCommandLineInkInput`** (TTY DEL).

### Phase 3 — Confirm / session y/n on Ink input

Drive **`ConfirmDisplay`** via **`@inkjs/ui` `ConfirmInput`** when 1:1, else Ink **`useInput`** in the subtree — **not** adapter keypress.

- **Complete replacement (this phase):** **no** confirm / y/n **char / backspace / enter** branches left in **`ttyAdapter`** for TTY paths where the shell is showing that confirm UI.
- **Verify:** interactive Vitest + E2E for confirm flows.

### Phase 4 — MCQ and token list: Ink-owned list input (gate 5)

**Primary:** **`@inkjs/ui` `Select`** (or equivalent ink-ui primitive) for **↑↓ / Enter / Esc** and visible highlight, wired to existing deps / **`processInput`** via callbacks. **Fallback** if API cannot match layout or policy: a **single** shared **`useInput`** list primitive **inside** the live subtree (still **not** `ttyAdapter`).

- **Complete replacement (this phase):** remove **all** parallel list-selection key handling from **`ttyAdapter`** when the shell is active (including MCQ, token list, slash-selection mode). Keep **`cli/src/interactions/selectListInteraction.ts`** only as **pure policy** (e.g. submit-line derivation) invoked **from** Ink/`Select` handlers if still useful — not a second stdin listener.
- **Verify:** `cli/tests/interactive/*`, mutation/tests touching **`selectListInteraction`** as appropriate, `e2e_test/features/cli/` for recall / list flows.

### Phase 5 — Complete replacement gate (mandatory)

**Audit and delete** any remaining duplicate stdin/readline/keypress paths for modes already migrated in **2–4**. **Definition of done:** with Ink shell running, keyboard routing has **one** owner per mode; **`ttyAdapter`** contains **no** dead branches that used to handle the same keys. Document **briefly** any intentional residue (e.g. bootstrap before `render`, non-TTY).

- **Verify:** `pnpm cli:test` full interactive suite; spot-check **`ttyAdapter`** (grep for `keypress`, `readline`, `up`/`down` on confirm/list/command paths).

### Phase 6 (optional) — ink-ui polish

**`Spinner`**, extra **`TextInput`** polish, etc., only where API maps **directly** to current behavior — does not block phase 5.

### Phase 7 — Ink-idiomatic stdout (shrink `interactiveTtyStdout`)

After **2–5** (and with phase 1 done for default live wrap), reduce raw **`process.stdout.write`** using Ink-supported patterns: e.g. **`render(..., { stdout })`** via a thin **`Writable`**, **`Static`** for append-only exit tails if PTY rules allow, cursor/show-hide via the unified input path, revisit **gate 7** if it removes duplicate prompt logging **without** corrupting layout.

- **Expect residue:** Private OSC (**`INTERACTIVE_INPUT_READY_OSC`**) may stay documented in a tiny layer.
- **Verify:** Interactive Vitest + E2E for history and OSC ordering.
- **Stop if:** tests fail — keep **`interactiveTtyStdout`** as escape hatch.

---

## Replacement bar (summary)

| Mode | Owner after migration | `ttyAdapter` / readline |
|------|------------------------|-------------------------|
| Main command line | Ink **`useInput`** (phase 2) | No duplicate key handling |
| Confirm / y/n | **`ConfirmInput`** or **`useInput`** (phase 3) | No duplicate |
| Lists (MCQ, tokens, selection) | **`Select`** or subtree **`useInput`** (phase 4) | No duplicate |
| Residue check | — | Phase **5** audit |

---

## What the UI layer is not

Not **business rules**, not a second **`processInput`**, not **domain branching** in **`ttyAdapter`**. Piped mode unchanged unless you extend scope.

---

## References (Context7)

- **Ink** — `vadimdemedes/ink`: `render`, `Box`, `Text`, `Static`, `useInput`, `useApp`, `useFocus`, instance `clear` / `unmount`, `render` options.
- **Ink UI** — `vadimdemedes/ink-ui`: `TextInput`, `ConfirmInput`, `Select`, `Spinner`, `ProgressBar`.
