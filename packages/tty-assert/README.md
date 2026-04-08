# `tty-assert`

Node-only helpers for asserting on **PTY (pseudo-terminal) transcripts**: ANSI stripping, **xterm.js** replay, and **explicit search surfaces** so tests do not rely on “search the whole history and hope labels disambiguate.”

This package is **Cypress-neutral** and **Doughnut-neutral**; the monorepo wires it from `e2e_test` and other callers.

**Prior art:** [microsoft/tui-test](https://github.com/microsoft/tui-test) uses xterm.js and a Playwright-style locator API. Doughnut does **not** adopt that runner; `tty-assert` borrows **ideas** (buffer slices, row-major search, strict matching). Design notes for Phase 5 live in [`ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md`](../../ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md).

**Research copy:** A temporary local `tt/` tree (upstream tui-test snapshot) was used only for reading while designing locators. It is **not** a product dependency; remove it from the workspace when no longer needed.

---

## Strip vs replay vs locators

| Concept | What it is | Typical use |
|--------|------------|-------------|
| **Stripped transcript** | Cumulative PTY bytes with **ANSI/OSC escapes removed** (`stripAnsiCliPty`). One long string — **not** an emulator layout. | “Did this text ever appear in the session?” (plugin startup wait, many cumulative assertions). |
| **Viewport replay** | Feed raw bytes through **headless xterm**, then read the **visible viewport** as plain text: one row per screen line, rows joined with **`\n`** (`ptyTranscriptToViewportPlaintext` / `ptyTranscriptToVisiblePlaintextViaXterm`). | **Current screen** heuristics (e.g. Ink “current guidance” extraction in Doughnut adapters). |
| **Locator surfaces** | After the same xterm replay, search a **named slice** of the buffer (`waitForTextInSurface`), or search the **stripped transcript** as a single haystack. | “Is this text visible in the **viewable** region?” vs “anywhere in **scrollback + viewport**?” vs “in the **stripped** log?” |

Stripped text and replayed viewport text **differ**: layout, wrapping, and scrollback mean a substring can appear in one model and not the other. Pick the surface that matches **what the user sees** for that assertion.

---

## Flattening contract (locators)

**Viewport replay** (`ptyTranscriptToViewportPlaintext`): newline-**joined** rows (see implementation in `ptyTranscriptToVisiblePlaintextViaXterm.ts`).

**`waitForTextInSurface`** for xterm-backed surfaces (`viewableBuffer`, `fullBuffer`):

- **Search haystack:** each row is `cols` cells; empty cells become a **space**; rows are concatenated **with no `\n` between rows** (row-major flat block). This matches tui-test’s locator search shape.
- **Failure snapshots:** **newline-separated** rows, each row `trimEnd`’d — readable and **not** identical to the flat search string.

For **`strippedTranscript`**, the haystack and snapshot are the **same**: the full ANSI-stripped string (no xterm geometry).

---

## Subpath exports (`package.json` `"exports"`)

| Import | Role |
|--------|------|
| `tty-assert/geometry` | Default CLI PTY cols/rows |
| `tty-assert/ptyTranscriptToViewportPlaintext` | **Canonical** xterm viewport plaintext (async) |
| `tty-assert/ptyTranscriptToVisiblePlaintextViaXterm` | Same function as `ptyTranscriptToViewportPlaintext` |
| `tty-assert/errorSnapshotFormatting` | Truncated / safe previews for errors |
| `tty-assert/ptySession` | Buffered PTY session helpers |
| `tty-assert/facade` | `startProgram` / `TtyAssertTerminalHandle` — `getReplayedScreenPlaintext` uses xterm; `expect(…).toBeVisible` searches **stripped** cumulative text |
| `tty-assert/waitForTextInSurface` | `waitForTextInSurface`, `stripAnsiCliPty`, `TtySearchSurface`, `TtyAssertStrictModeViolationError` |

---

## `waitForTextInSurface`

```ts
import { waitForTextInSurface } from 'tty-assert/waitForTextInSurface'

await waitForTextInSurface({
  raw: ptyBytes,
  needle: 'Expected',
  surface: 'viewableBuffer', // or 'fullBuffer' | 'strippedTranscript'
  timeoutMs: 3000,           // default 0 = single attempt (good for Vitest)
  retryMs: 50,
  strict: true,              // default; throws if multiple non-overlapping matches
})
```

After the needle matches, optional **`cellExpectations`** assert on xterm cells (string needle, `viewableBuffer` / `fullBuffer` only). Example — bold on the **first** match and Ink-style gray block on the **last** (palette 8):

```ts
await waitForTextInSurface({
  raw: ptyBytes,
  needle: 'User paste',
  surface: 'viewableBuffer',
  strict: false,
  cellExpectations: [
    { match: 'first', expectations: [{ kind: 'allBold' }] },
    {
      match: 'last',
      expectations: [
        { kind: 'noFgPaletteUnlessBgPalette', fgPalette: 8, unlessBgPalette: 8 },
        { kind: 'allBgPalette', index: 8 },
      ],
    },
  ],
})
```

- **`viewableBuffer`:** lines from xterm `baseY` through `length - 1` (tui-test “viewable” slice).
- **`fullBuffer`:** lines `0` through `length - 1` (scrollback + active).
- **`strippedTranscript`:** no replay; search `stripAnsiCliPty(raw)`.
- **`raw`:** string or a **getter** `() => string` so each poll can see updated buffer data.

---

## Scripts

From repo root (with Nix dev shell as usual):

```bash
pnpm tty-assert:test
pnpm tty-assert:lint
```

---

## Roadmap

Extraction and phases: [`ongoing/cli-terminal-test-library-extraction.md`](../../ongoing/cli-terminal-test-library-extraction.md).
