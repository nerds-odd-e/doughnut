# Deduplicating `interactive.ts` vs `InteractiveApp` (Ink TTY)

Informal notes from removing **`listAccessTokens` / `/list-access-token`** handling that lived in both **`cli/src/interactive.ts`** (`processInput`) and **`cli/src/ui/interactiveApp.tsx`**. More of the same class of change is expected.

---

## What caused the duplicate

The interactive shell has **two execution surfaces** for slash commands:

1. **`processInput`** — shared engine, module globals for recall, `OutputAdapter` (`log` / `writeCurrentPrompt`). Used when `InteractiveApp` calls `runProcessInputTurn` and in Vitest that imports `processInput` directly with a **default console** adapter.
2. **`InteractiveApp`** — on TTY submit, **intercepts** some commands first (e.g. `TOKEN_LIST_COMMANDS`: `/list-access-token`, bare `/remove-access-token`, …) to drive **Ink state** (transcript, token picker, history), then returns **without** calling `processInput` for that line.

So the **same user-visible command** could be implemented twice: once for “plain log output” in `processInput`, once for “picker + past messages” in `InteractiveApp`. That is real **behavior** duplication, not only dependency wiring.

---

## How to decide “which side keeps the truth?”

Ask **which path actually runs in production TTY**:

- Trace **Enter** in `InteractiveApp` (`handleCommandLineInkInput`): if `TOKEN_LIST_COMMANDS[trimmedInput]` (or similar) matches and **`return`s before `runProcessInputTurn`**, then **`processInput` never sees that exact line** on the real shell.
- Grep **`processInput(`** under `cli/src`: today only **`InteractiveApp`** calls it from production code.

**Rule of thumb:** If TTY never reaches `processInput` for a command, the **`processInput` branch is dead for product behavior** and can be removed unless you still want a **separate contract** for direct `processInput(...)` callers.

---

## Tests: migrate observable behavior, not the dead path

- **`cli/tests/interactive/processInput.test.ts`** — contract for **`processInput` + default console** (no TTY bytes). Assertions here are **not** substitutes for Ink behavior when Ink intercepts first.
- **`cli/tests/interactive/interactiveTty*.test.ts`** — **`runInteractive`** + mock TTY; this is the right place to assert **user-visible** behavior for commands handled in **`InteractiveApp`** (token list, stage band, ★ / space prefixes, empty list message, etc.).

When removing a duplicate from `processInput`:

1. Confirm TTY path via **`InteractiveApp`** (or `ShellSessionRoot` / `liveColumnInk`).
2. **Delete** the redundant `processInput` branch and any imports only used there.
3. **Move** or **add** assertions under the appropriate **`interactiveTty*`** test (or extend an existing case) so **observable** behavior stays covered.
4. **Remove** `processInput` tests that only asserted the removed branch (they would otherwise start failing or testing the wrong surface).

---

## Example: `/list-access-token`

- **Removed** from `processInput`: list + `formatTokenLines` loop (unreachable on TTY after Ink intercept).
- **Kept** in `InteractiveApp`: `listAccessTokens()`, empty → transcript message; non-empty → `beginTokenSelection` (picker uses same ★ / two-space convention as `formatTokenLines` in `liveColumnInk`).
- **Migrated** test: explicit TTY check for `★ Alpha`, `  Beta`, `  Gamma` in **`interactiveTtyTokenList.test.ts`** (parity with former `formatTokenLines` output).

**After removal:** `processInput('/list-access-token')` falls through to **“Not supported”** — acceptable if the **only** supported interactive shell is TTY + Ink and docs/tests do not promise the old console-only listing.

---

## Checklist for the next similar dedup

1. Grep command string and handler in **`interactiveApp.tsx`** and **`interactive.ts`**.
2. Confirm submit order: intercept vs `runProcessInputTurn` → `processInput`.
3. List **all** tests touching the command (`processInput.test`, `interactiveTty*`, `interactiveFetchWait`, E2E).
4. Remove dead **`processInput`** code; trim imports.
5. Ensure **TTY** (or E2E) tests still assert outcomes that matter to users.
6. Run **`pnpm -C cli test`** for touched specs (and a broader **`pnpm cli:test`** if the change is non-trivial).

---

## Related

- **`ongoing/interactive-shell-deps-phased-review.md`** — per-member review of `InteractiveShellDeps` and moving concrete imports into UI/command modules; some phase notes may predate the **`/list-access-token`** `processInput` removal — reconcile when editing that file.
- **`.cursor/rules/cli.mdc`** — TTY vs `processInput` testing guidance.
