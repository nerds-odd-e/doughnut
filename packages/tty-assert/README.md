# `tty-assert`

Node-only helpers for asserting on **PTY (pseudo-terminal) transcripts**: ANSI stripping, **xterm.js** replay, and **explicit search surfaces** so tests do not rely on “search the whole history and hope labels disambiguate.”

This package is **Cypress-neutral** and **Doughnut-neutral**; the monorepo wires it from `e2e_test` and other callers.

**Imports:** **`package.json` exposes only the package root** (`tty-assert` → `src/index.ts`). Everything else lives under `src/` for use inside this package (relative imports in tests and implementation). A published build would point the root at compiled `dist/` instead of `.ts`.

**Phase 6:** Managed session + single Cypress assertion task — design and sub-phases in [`ongoing/cli-phase6-tty-assert-managed-session-subphases.md`](../../ongoing/cli-phase6-tty-assert-managed-session-subphases.md).

---

## Strip vs replay vs locators

| Concept | What it is | Typical use |
|--------|------------|-------------|
| **Stripped transcript** | Cumulative PTY bytes with **ANSI/OSC escapes removed** (`stripAnsiCliPty`). One long string — **not** an emulator layout. | “Did this text ever appear in the session?” (plugin startup wait, many cumulative assertions). |
| **Viewport replay** | Feed raw bytes through **headless xterm**, then read the **visible viewport** as plain text: one row per screen line, rows joined with **`\n`** (`ptyTranscriptToViewportPlaintext` / `ptyTranscriptToVisiblePlaintextViaXterm`). | **Current screen** plain text; product-specific parsing (e.g. “current guidance”) stays in adapters. |
| **Locator surfaces** | After the same xterm replay, search a **named slice** of the buffer (`waitForTextInSurface`), or search the **stripped transcript** as a single haystack. | “Is this text visible in the **viewable** region?” vs “anywhere in **scrollback + viewport**?” vs “in the **stripped** log?” |

Stripped text and replayed viewport text **differ**: layout, wrapping, and scrollback mean a substring can appear in one model and not the other. Pick the surface that matches **what the user sees** for that assertion.

---

## Flattening contract (locators)

**Viewport replay** (`ptyTranscriptToViewportPlaintext`): newline-**joined** rows (see implementation in `ptyTranscriptToVisiblePlaintextViaXterm.ts`).

**`waitForTextInSurface`** for xterm-backed surfaces (`viewableBuffer`, `fullBuffer`):

- **Search haystack:** each row is `cols` cells; empty cells become a **space**; rows are concatenated **with no `\n` between rows** (row-major flat block).
- **Failure snapshots:** **newline-separated** rows, each row `trimEnd`’d — readable and **not** identical to the flat search string.

For **`strippedTranscript`**, the haystack and snapshot are the **same**: the full ANSI-stripped string (no xterm geometry).

---

## Assertion failure messages

When `waitForTextInSurface` or `ManagedTtySession.assert` fails, the message body includes:

1. **Detail** and **`Search surface: "…"`** plus a short note (transcript vs row-major matching).
2. A **`---`** block with the snapshot as **numbered lines** (`  1 | …`, split on `\n`). The block is truncated after numbering (about **8000** characters).
3. **`ManagedTtySession.assert` only:** **`--- Final visible screen (viewport) ---`** plus the same **numbered-line** form of the xterm **viewport** (current visible rows), then a closing **`---`**. This is the plain-text “screenshot” of what is on screen at failure time, before the cumulative transcript dump.
4. A **raw PTY appendix** with ANSI stripped and safe-visible escaping, capped at about **12_000** visible characters.

---

## Package root (`tty-assert`)

Exported today: **`startManagedTtySession`**, **`BufferedPtySession`**, managed-session types (`ManagedTtySession`, `ManagedTtyAssertOptions`, …), and JSON-task helpers (**`ManagedTtyAssertJsonPayload`**, **`managedTtyAssertOptionsFromJson`**) — enough for Doughnut’s Cypress plugin. The return type of **`dumpDiagnostics()`** is internal to the package. Lower-level modules (`waitForTextInSurface`, `ptySession`, replay helpers, `stripAnsi`, …) are **not** separate entry points; they are composed internally and covered by this package’s unit tests.

---

## Managed interactive session

Use this when one process owns **the same** PTY buffer, xterm headless instance, and assertion loop (retry + sync). That matches interactive CLI E2E: start once, write many times, assert many times, dispose once.

**Lifecycle**

1. **`startManagedTtySession(opts)`** — spawns the PTY (`ptySession`), returns **`ManagedTtySession`**.
2. **`write` / `submit`** — send bytes or a line (`\r` appended for `submit`).
3. **`assert(opts)`** — polls until timeout: syncs new raw bytes into xterm, then runs the same surface logic as `waitForTextInSurface` (or stripped-transcript path without replay). No need to pass an updated `raw` string from outside; it always reads `session.buf.text`.
4. **`dumpDiagnostics()`** — async diagnostic snapshot (previews of viewport, stripped tail, etc.).
5. **`getViewportAnimationPngs()`** / **`buildViewportAnimationGif()`** — when the session was started with **`startManagedTtySession`**, each PTY `onData` schedules a debounced viewport sample (deduped by viewport plaintext, ring buffer capped at 56 PNGs). **`buildViewportAnimationGif`** flushes the pending sample, then encodes those PNGs to an animated GIF (requires at least two distinct frames). Sessions from **`attachManagedTtySession`** without the internal bridge omit recording (empty PNG list; GIF build throws).
6. **`dispose()`** — idempotent: tears down xterm, disposes the buffered PTY session. Safe if the child already exited.

**`ManagedTtyAssertOptions`** (same assertion knobs as `waitForTextInSurface` except **`raw`** is omitted): `needle` (string or `RegExp`), `surface`, `timeoutMs`, `retryMs`, `strict`, `messagePrefix`, `startAfterAnchor`, `fallbackRowCount`, `cellExpectations`.

**Cypress:** Doughnut maps **`cy.task('cliAssert', payload)`** to `managed.assert(...)`. On failure the plugin saves a **viewport PNG** and, when enough frames were recorded, a **GIF** from **`buildViewportAnimationGif()`**, via `saveBufferToCurrentSpecFolder` (see `e2e_test/config/cliE2ePluginTasks.ts`). The task body must stay **JSON-serializable**: use `{ source, flags? }` instead of `RegExp` objects for needles and anchors.

**Node tests without Cypress:** Prefer `startManagedTtySession` from `tty-assert`; do not round-trip PTY text through a browser.

---

## `waitForTextInSurface` (internal module)

Used by **`ManagedTtySession.assert`** and unit-tested under `packages/tty-assert/tests/`. Direct use is via **relative imports** inside this package (see those tests), not a separate publish entry.

```ts
// e.g. from packages/tty-assert/tests/ — see waitForTextInSurface.test.ts
import { waitForTextInSurface } from '../src/waitForTextInSurface'

await waitForTextInSurface({
  raw: ptyBytes,
  needle: 'Expected',
  surface: 'viewableBuffer', // or 'fullBuffer' | 'strippedTranscript'
  timeoutMs: 3000,           // default 0 = single attempt (good for Vitest)
  retryMs: 50,
  strict: true,              // default; throws if multiple non-overlapping matches
})
```

After the needle matches, optional **`cellExpectations`** assert on xterm cells (string needle, `viewableBuffer` / `fullBuffer` only). Example — bold on the **first** match and **palette 8** background on the **last**:

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

- **`viewableBuffer`:** lines from xterm `baseY` through `length - 1` (the scrollable view’s active region, not the full scrollback).
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
