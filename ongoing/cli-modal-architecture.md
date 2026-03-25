# CLI interactive UI — Ink migration (plan)

## North star (end state for interactive TTY)

Interactive TTY is an **Ink** app: `render()`, `Box` / `Text`, **`Static`** for append-only committed history where that matches product rules, and (target) **`useInput`** for typing and list/confirm flows. **Business** stays in `interactive.ts` / `recall.ts`; **`ttyAdapter`** only wires stdin/stdout, **`TTYDeps`** (mechanism-named), and the shell tree — **no** branching on recall/MCQ/notebook semantics.

**Non-interactive** (`-c`, piped): `processInput` + `pipedAdapter` unless explicitly unified later.

**Layout:** `cli/src/renderer.ts` stays the grapheme-aware bridge where Ink `Text` wrap is not enough (especially **piped**); see **Decision gate 4**.

**Raw stdout:** `cli/src/adapters/interactiveTtyStdout.ts` is the single owner for adapter-emitted bytes (OSC, cursor hide/show, clear, exit farewell, etc.); Ink still draws the live tree on its own path.

---

## Layering

| Layer | Role |
|-------|------|
| **Business** | Orchestration; exposes callbacks / data to the shell. |
| **Interactive UI** | React components + state; dispatch intents, no domain rules. |
| **TTY adapter** | Readline + key routing + `render()` / `rerender`; consumes **`TTYDeps`** only. |
| **Ink shell** | `InteractiveShellDisplay`, `buildLivePanel`, instance lifecycle. |
| **renderer.ts** | Wrapping, ANSI strings for default column today, piped boxes. |

---

## Vocabulary (Ink-aligned)

| Avoid | Prefer |
|-------|--------|
| Modal stack | Conditional subtree / stacked UI state at root |
| Adapter “paints” | Props → `<Box>` / `<Text>` / `Static` |
| History scrollback | `Static` items if append-only semantics fit |

---

## Decision gates (pause and get a product/tech choice)

**Who decides:** You (maintainer / product). Anyone implementing a phase should **stop and ask** when a gate blocks a clear path—not pick silently.

1. Single Ink root vs islands / hybrid raw ANSI  
2. **`Static`** vs rewriting past history lines  
3. **`useFocus`** / Tab vs ↑↓ in guidance  
4. Ink `Text` wrap vs `renderer.ts` grapheme wrap (CJK/emoji)  
5. **`@inkjs/ui`** vs hand-rolled `useInput`  
6. Visual parity vs slimmer Ink-native look (**declined** — slimmer OK)  
7. **`patchConsole`** / `console.log` vs layout corruption (**declined for now**; may revisit in phase 6)

---

## Discipline (each phase)

- Keep **user-visible** behavior covered: **Vitest** `runInteractive` / TTY tests + **E2E** `e2e_test/features/cli/*.feature` where applicable.  
- **Observable behavior first** (`.cursor/rules/planning.mdc`): assert stdout/PTY/DOM, not internal branches.  
- After a phase: delete **dead code** and tests that only mirrored removed internals.

---

## Completed baseline (no per-phase detail)

Already shipped: single **`render()`** root, **`Static`** history + live column (`InteractiveShellDisplay`, `buildLivePanel`), Ink branches for confirm / MCQ / token list / fetch-wait / default line (**`CommandLineLivePanel`** with reverse-video caret, **no** adapter caret CSI), **one layout snapshot per `drawBox`**, resize via **`rerender`** (no full clear), **empty Enter → `clear()` then `unmount()`** before remount (E2E is the oracle for stacked boxes), **`interactiveTtyStdout`** (no raw `process.stdout.write` in `ttyAdapter`). **Select-list** keys: `selectListInteraction.ts`; **session y/n**: `sessionYesNoInteraction.ts`.

*Legacy letter labels (git history / old discussions): A–I, F1–F3, G, H, J1–J4, K.*

---

## Next phases (numbered)

**Order:** 1 → 2 → 3 → 4 → 5 (optional) → 6.

### Phase 1 — Default live column: Ink `Box` / `Text` wrap (gate 4)

Replace default **`buildLiveRegionLines` + `LiveRegionLines`** with Ink layout and **`width`** = terminal columns. Keep **`renderer.ts`** for piped **`writeFullRedraw`** until parity is proven.

**Verify:** Interactive Vitest (incl. CJK/emoji if touched); E2E as needed. **Stop if** Ink wrap is insufficient — document; keep a minimal bridge.

---

### Phase 2 — Main command line: `useInput` (gates 1 & 3)

Move typing, caret, Enter, arrows, Tab, multiline draft from **readline `keypress`** into Ink. Preserve scrollback, **`INTERACTIVE_INPUT_READY_OSC`**, slash suggestions, draft history, `/clear`, Ctrl+C.

**Verify:** `cli/tests/interactive/*`, `interactiveCommandInput.test.ts`, CLI E2E.

---

### Phase 3 — Confirm / session y/n: `useInput`

Drive **`ConfirmDisplay`** (or ink-ui **`ConfirmInput`** if 1:1) from Ink input; drop duplicate char/backspace handling in **`ttyAdapter`** for those modes.

**Verify:** Session y/n + stop-confirm coverage + E2E.

---

### Phase 4 — MCQ and token list: `useInput` in live subtree

Handle ↑↓ / number / Enter / Esc inside **`McqDisplay` / `TokenListDisplay`** (or shared hook) with deps callbacks; remove parallel list key branches from **`ttyAdapter`** when the shell is active.

**Optional:** `@inkjs/ui` **`Select`** only if it **net-deletes** code.

**Verify:** `interactiveTtyMcq`, `interactiveTtyTokenList`, recall E2E.

---

### Phase 5 (optional) — ink-ui polish

**`Spinner`**, **`TextInput`**, etc., only where the API maps **directly** to current behavior.

---

### Phase 6 — Ink-conventional stdout (shrink **`interactiveTtyStdout`**)

Reduce raw **`process.stdout.write`** using Ink-supported patterns: e.g. one **`stdout`** into **`render({ stdout })`** (wrapper `Writable`), **`Static`** for append-only exit tails if PTY rules allow, cursor via Ink/input stack when possible, revisit **gate 7** only if it **removes** duplicate prompt paths without corrupting layout.

**Depends on:** phase 1 width model; **phases 2–4** recommended first (single stdin + shell lifecycle).

**Expect residue:** Private shell-integration OSC may remain documented and tiny.

**Verify:** `interactiveTtyStdout` call sites trend down; E2E for history + input-ready OSC.

**Stop if** tests or shell-integration break — keep **`interactiveTtyStdout`** as the escape hatch.

---

## What the interactive UI is not

- Not **recall/quiz business rules**.  
- Not a second **`processInput`**.  
- Not **domain branching** inside **`ttyAdapter`**.

---

## References (Context7)

- **Ink** — `vadimdemedes/ink`: `render`, `Static`, `useInput`, instance `clear` / `unmount`, `render` options.  
- **Ink UI** — `vadimdemedes/ink-ui`: `TextInput`, `ConfirmInput`, `Select`, `Spinner`.
