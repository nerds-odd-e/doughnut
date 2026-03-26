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

**Do not** land “half TextInput” (mounted but disabled, or second component) — same rule as phase 15: **no dead UI**, **no duplicate stdin handlers**.

Slash completion styling (inverse segments, grapheme-aware width) must either map onto the chosen component, live in **non-editable** rows (guidance only), or get an explicit **new gate** if product requires pixel-parity with today’s paint.

## Phases (order by risk → user value)

### Phase 1 — Gate closure + spike

- Confirm chosen path from the table above; smallest spike (e.g. Vitest or local TTY) proving **one** stdin owner can edit the line with **caret not** duplicated by the hardware cursor.
- **Verify:** short note in this file; no user-facing change required if spike-only.

### Phase 2 — Single-owner main line (behavior parity)

- Implement the chosen input primitive so **typing, caret, delete, home/end** and **session state** (`InteractiveCommandInput`) stay correct; **↑↓** draft history and **Tab** focus still match **live column focus** rules (`cli.mdc`).
- Preserve **Enter / Esc / Tab** semantics and integration with `handleCommandLineInkInput` (submit, slash picker dismiss, focus cycle) — likely by **narrow** `useInput` at root **only** for keys the TextInput does not consume, **without** duplicating character insertion (same contract as `mainCommandLineInkTyping.ts` today, or superseded by TextInput callbacks only).
- **Verify:** `pnpm cli:test` interactive + `mainCommandLineInkTyping` tests **updated or replaced** so they still assert **observable** transcripts/behavior, not internal split details unless that API is the deliberate contract.

### Phase 3 — Slash / placeholder / width

- Restore or re-spec **slash-aware** and **placeholder** presentation under the new editor (may be TextInput-only, or helper rows + plain TextInput — **one** coherent design).
- **Verify:** `pnpm cli:test` (slash completion, `renderer`/`terminalLayout` consumers); targeted E2E `cli_interactive_mode.feature` if live strip shape changes.

### Phase 4 — Remove hardware cursor from default live path

- After the main line no longer relies on hiding the HW cursor for Ink-drawn caret, remove or narrow **`hideCursor` / `finalizeDefaultLiveAfterInk`** so default command-line paint does not emit **`HIDE_CURSOR`**.
- Update **`ttyEntry.ts`** checklist and **`.cursor/rules/cli.mdc`** “approved non-Ink” table — **no silent** new `process.stdout.write` paths.
- **Verify:** `pnpm cli:test`; real TTY smoke (no double caret, no flash); E2E if PTY-visible.

### Phase 5 — Cleanup

- Delete dead helpers only when **no** test or product path needs them; **no** duplicate editor code paths.
- **Verify:** `pnpm cli:lint`; `rg` for orphaned `formatInteractiveCommandLineInkRows` / caret-only paths if fully superseded.

## Phase discipline

Before closing each phase: tests green for that slice, **no dead code**, update this doc’s snapshot (what’s done / what’s next). Deploy gate per team habit (`planning.mdc`).

## References

- Current live column: `cli/src/ui/liveColumnInk.tsx`, `cli/src/interactions/mainCommandLineInkTyping.ts`
- Cursor + OSC hooks: `cli/src/adapters/interactiveTtyStdout.ts`, `cli/src/adapters/interactiveTtySession.ts` (`handleShellRendered`)
- Ink UI: `@inkjs/ui` `TextInput` (package version in `cli/package.json`)
