# `tty-assert`

Node-only helpers for asserting on **PTY (pseudo-terminal) transcripts**: ANSI stripping, **xterm.js** replay, and **explicit search surfaces** so tests do not rely on “search the whole history and hope labels disambiguate.”

This package is **Cypress-neutral** and **Doughnut-neutral**; the monorepo wires it from `e2e_test` and other callers.

**Prior art:** [microsoft/tui-test](https://github.com/microsoft/tui-test) uses xterm.js and a Playwright-style locator API. Doughnut does **not** adopt that runner; `tty-assert` borrows **ideas** (buffer slices, row-major search, strict matching). Design notes for Phase 5 live in [`ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md`](../../ongoing/cli-phase5-tty-assert-api-xterm-finish-subphases.md).

**Research copy:** A temporary local `tt/` tree (upstream tui-test snapshot) was used only for reading while designing locators. It is **not** a product dependency; remove it from the workspace when no longer needed.

**Phase 6:** Managed session + single Cypress assertion task — design and sub-phases in [`ongoing/cli-phase6-tty-assert-managed-session-subphases.md`](../../ongoing/cli-phase6-tty-assert-managed-session-subphases.md).

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

## Assertion failure messages (Phase 7)

When `waitForTextInSurface` or `ManagedTtySession.assert` fails, the message body includes:

1. **Detail** and **`Search surface: "…"`** plus a short note (transcript vs row-major matching).
2. A **`---`** block with the snapshot as **numbered lines** (`  1 | …`, split on `\n`). The block is truncated after numbering (about **8000** characters).
3. A **raw PTY appendix** with ANSI stripped and safe-visible escaping, capped at about **12_000** visible characters.

---

## Subpath exports (`package.json` `"exports"`)

| Import | Role |
|--------|------|
| `tty-assert/geometry` | Default CLI PTY cols/rows |
| `tty-assert/ptyTranscriptToViewportPlaintext` | **Canonical** xterm viewport plaintext (async) |
| `tty-assert/ptyTranscriptToVisiblePlaintextViaXterm` | Same function as `ptyTranscriptToViewportPlaintext` |
| `tty-assert/ptySession` | Buffered PTY session helpers |
| `tty-assert/facade` | `startProgram` / `TtyAssertTerminalHandle` — `getReplayedScreenPlaintext` uses xterm; `expect(…).toBeVisible` searches **stripped** cumulative text |
| `tty-assert/managedTtySession` | Long-lived PTY + incremental xterm replay + polling `assert` — see below |
| `tty-assert/waitForTextInSurface` | `waitForTextInSurface`, `stripAnsiCliPty`, `TtySearchSurface`, `TtyAssertStrictModeViolationError` |

---

## Managed interactive session (`managedTtySession`)

Use this when one process owns **the same** PTY buffer, xterm headless instance, and assertion loop (retry + sync). That matches interactive CLI E2E: start once, write many times, assert many times, dispose once.

**Lifecycle**

1. **`startManagedTtySession(opts)`** — spawns the PTY (`ptySession`), returns **`ManagedTtySession`**.
2. **`write` / `submit`** — send bytes or a line (`\r` appended for `submit`).
3. **`assert(opts)`** — polls until timeout: syncs new raw bytes into xterm, then runs the same surface logic as `waitForTextInSurface` (or stripped-transcript path without replay). No need to pass an updated `raw` string from outside; it always reads `session.buf.text`.
4. **`dumpFrames()`** — async diagnostic snapshot (previews of viewport, stripped tail, etc.).
5. **`dispose()`** — idempotent: tears down xterm, disposes the buffered PTY session. Safe if the child already exited.

**`ManagedTtyAssertOptions`** (same assertion knobs as `waitForTextInSurface` except **`raw`** is omitted): `needle` (string or `RegExp`), `surface`, `timeoutMs`, `retryMs`, `strict`, `messagePrefix`, `startAfterAnchor`, `fallbackRowCount`, `cellExpectations`.

**Cypress:** Doughnut maps **`cy.task('cliInteractiveAssert', payload)`** to `managed.assert(...)`. The task body must stay **JSON-serializable**: use `{ source, flags? }` instead of `RegExp` objects for needles and anchors (see `e2e_test/config/cliE2ePluginTasks.ts`).

**Node tests without Cypress:** Prefer `managedTtySession` or `facade` directly; do not round-trip PTY text through a browser.

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
    { match: 'last', expectations: [{ kind: 'allBgPalette', index: 8 }] },
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
