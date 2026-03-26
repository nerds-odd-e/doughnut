/**
 * Thin TTY entry: **`runTTY`** → {@link runInteractiveTtySession}.
 *
 * ## Product invariant
 * - **No `-c`** — rejected in `cli/src/run.ts`.
 * - **No piped-stdin interactive shell** and **no `pipedAdapter`** (removed).
 *
 * ## Approved non-Ink bytes / stdin bridges (keep minimal)
 * Document new cases here **and** next to the code; do not add ad-hoc `process.stdout.write` “just once.”
 *
 * 1. **Private OSC** — input-ready signal for PTY integration (`interactiveTtyStdout`, `renderer`).
 * 2. **Hardware cursor** — explicit CSI hide/show only for exception flows (e.g. fetch-wait, exit). Default command-line live paint must not emit `HIDE_CURSOR` (`interactiveTtyStdout`, `ansi.ts`).
 * 3. **Exit path** — farewell lines, Ctrl+C newline before exit (`interactiveTtyStdout.exitFarewellBlock`, `ctrlCExitNewline`).
 * 4. **Pre-Ink banner** — `process.stdout.write` for version lines before `render()` (`interactiveTtySession`).
 * 5. **`patchConsole`** — enabled when `console.Console` is constructible; off under Vitest `spyOn(console, …)` (`inkPatchConsoleSupported` in `interactiveTtySession`).
 * 6. **readline `keypress`** — **Ctrl+C** (exit before Ink) and fetch-wait **Esc** cancel. Token-list Esc is Ink-owned. See `stdin.on('keypress')` in `interactiveTtySession.ts`. **Do not** handle MCQ Esc on readline (duplicate / wrong ordering).
 */
import { runInteractiveTtySession } from './interactiveTtySession.js'
import type { TTYDeps } from './ttyDeps.js'

export type { TTYDeps } from './ttyDeps.js'

type TTYInput = NodeJS.ReadableStream & {
  setRawMode?: (mode: boolean) => void
  resume?: () => void
  setEncoding?: (encoding: BufferEncoding) => void
}

export async function runTTY(stdin: TTYInput, deps: TTYDeps): Promise<void> {
  runInteractiveTtySession(stdin, deps)
}
