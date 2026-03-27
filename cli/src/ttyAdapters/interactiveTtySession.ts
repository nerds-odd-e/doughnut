/**
 * TTY interactive shell — **`runInteractiveTtySession`** (started from the interactive CLI entry in `interactive.ts`).
 *
 * ## Product invariant
 * - **No `-c`** — rejected in `cli/src/run.ts`.
 * - **No piped-stdin interactive shell** and **no `pipedAdapter`** (removed).
 *
 * ## Approved non-Ink bytes / stdin bridges (keep minimal)
 * Document new cases here **and** next to the code; do not add ad-hoc `process.stdout.write` “just once.”
 *
 * 1. **Private terminal control sequence** — input-ready signal for PTY integration (`interactiveTtyStdout`, `renderer`).
 * 2. **Hardware cursor** — explicit CSI hide/show only for exception flows (e.g. fetch-wait, exit). Default command-line live paint must not emit `HIDE_CURSOR` (`interactiveTtyStdout`, `ansi.ts`).
 * 3. **Exit path** — farewell lines, Ctrl+C newline before exit (`interactiveTtyStdout.exitFarewellBlock`, `ctrlCExitNewline`).
 * 4. **Pre-Ink banner** — `process.stdout.write` for version lines before `render()` (below).
 * 5. **`patchConsole`** — enabled when `console.Console` is constructible; off under Vitest `spyOn(console, …)` (`inkPatchConsoleSupported`).
 * 6. **readline `keypress`** — **Ctrl+C** (exit before Ink) only. Fetch-wait **Esc** is Ink `useInput` in `FetchWaitDisplay`. Token-list Esc is Ink-owned. **Do not** handle MCQ Esc on readline (duplicate / wrong ordering).
 *
 * **`InteractiveAppTerminalContract`** is the only path from **`ui/interactiveApp.tsx`** to TTY bytes, cursor, and OSC — the UI package does not import `interactiveTtyStdout` or call `process.stdout.write` for shell chrome.
 */
import React from 'react'
import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import { render } from 'ink'
import { formatVersionOutput } from '../commands/version.js'
import { getInteractiveFetchWaitLine } from '../interactiveFetchWait.js'
import {
  createInitialShellSessionState,
  type ShellSessionState,
} from '../shell/shellSessionState.js'
import { interactiveTtyStdout } from './interactiveTtyStdout.js'
import type { OutputAdapter } from '../types.js'
import { getTerminalWidth } from '../renderer.js'
import { isAlternateLivePanel } from '../ui/ShellSessionRoot.js'
import {
  InteractiveApp,
  type InteractiveAppTerminalContract,
} from '../ui/interactiveApp.js'
import type { InteractiveShellDeps } from '../interactiveShellDeps.js'

type ReadlineKey = Pick<readline.Key, 'name' | 'shift' | 'ctrl' | 'meta'>

type TTYInput = NodeJS.ReadableStream & {
  setRawMode?: (mode: boolean) => void
  resume?: () => void
  setEncoding?: (encoding: BufferEncoding) => void
  ref?: () => void
  unref?: () => void
}

const inkPatchConsoleProbeOut = new Writable({
  write(_chunk, _encoding, cb) {
    cb()
  },
})
const inkPatchConsoleProbeErr = new Writable({
  write(_chunk, _encoding, cb) {
    cb()
  },
})

/** Ink's patch-console calls `new console.Console(...)`. Vitest `spyOn(console, 'log')` can break that constructor; real Node TTY keeps patching enabled. */
function inkPatchConsoleSupported(): boolean {
  const C = console.Console
  if (typeof C !== 'function') return false
  try {
    Reflect.construct(C, [inkPatchConsoleProbeOut, inkPatchConsoleProbeErr])
    return true
  } catch {
    return false
  }
}

export function runInteractiveTtySession(
  stdin: TTYInput,
  deps: InteractiveShellDeps
): void {
  process.stdout.write(`${formatVersionOutput()}\n\n`)

  stdin.setRawMode?.(true)
  stdin.resume?.()
  stdin.setEncoding?.('utf8')
  const noopOutput = new Writable({
    write(_chunk, _encoding, cb) {
      cb()
    },
  })
  const rl = readline.createInterface({
    input: stdin,
    output: noopOutput,
    escapeCodeTimeout: 50,
  })
  readline.emitKeypressEvents(stdin, rl)
  const initialSession = createInitialShellSessionState()
  const latestSessionRef: { current: ShellSessionState } = {
    current: initialSession,
  }
  const ttyOutputRef: { current: OutputAdapter | null } = { current: null }
  let shellInstance: ReturnType<typeof render> | null = null

  const stdinTty = stdin as TTYInput
  const innerSetRawMode = stdinTty.setRawMode?.bind(stdinTty)
  if (typeof innerSetRawMode === 'function') {
    stdinTty.setRawMode = (enable: boolean) => {
      if (!enable && isAlternateLivePanel(latestSessionRef.current, deps)) {
        return stdinTty
      }
      return innerSetRawMode(enable)
    }
  }

  function patchStdinForInk(
    stream: NodeJS.ReadableStream & { ref?: () => void; unref?: () => void }
  ): void {
    if (typeof stream.ref !== 'function')
      stream.ref = () => {
        /* noop — keep event loop alive */
      }
    if (typeof stream.unref !== 'function')
      stream.unref = () => {
        /* noop */
      }
  }

  const doExit = () => {
    if (shellInstance) {
      shellInstance.unmount()
      shellInstance = null
    }
    interactiveTtyStdout.showCursor()
    rl.close()
    process.exit(0)
  }

  function handleShellRendered(
    session: ShellSessionState,
    shellDeps: InteractiveShellDeps
  ): void {
    if (isAlternateLivePanel(session, shellDeps)) {
      stdinTty.ref?.()
      stdinTty.setRawMode?.(true)
    }
    if (getInteractiveFetchWaitLine() !== null) {
      interactiveTtyStdout.hideCursor()
    }
  }

  const terminalContract: InteractiveAppTerminalContract = {
    writeCurrentPromptLine: (msg) => {
      interactiveTtyStdout.greyCurrentPromptLine(msg)
    },
    beginCurrentPrompt: () => {
      interactiveTtyStdout.currentPromptSeparator(getTerminalWidth())
    },
    onShellSessionLayoutEffect: (session, shellDeps) => {
      handleShellRendered(session, shellDeps)
    },
    writeExitFarewellBlock: ({ previousInputContent, outputLines, tone }) => {
      interactiveTtyStdout.exitFarewellBlock({
        width: getTerminalWidth(),
        previousInputContent,
        outputLines,
        tone,
      })
    },
    writeCtrlCExitNewline: () => {
      interactiveTtyStdout.ctrlCExitNewline()
    },
  }

  const stdinForInk = stdin as NodeJS.ReadableStream & {
    ref?: () => void
    unref?: () => void
  }
  patchStdinForInk(stdinForInk)
  shellInstance = render(
    React.createElement(InteractiveApp, {
      initialSession,
      deps,
      latestSessionRef,
      terminalContract,
      ttyOutputRef,
      exitSession: doExit,
    }),
    {
      stdin: stdinForInk as NodeJS.ReadStream,
      stdout: process.stdout,
      patchConsole: inkPatchConsoleSupported(),
      exitOnCtrlC: false,
      maxFps: 0,
    }
  )

  /**
   * Readline `emitKeypressEvents` runs alongside Ink on the same stdin. Ink owns typing, list keys,
   * fetch-wait Esc (`FetchWaitDisplay`), and token-list Esc (list-selection live column). This
   * listener is **Ctrl+C** (exit before Ink) only. Do not add MCQ Esc here: a late readline
   * `escape` after Ink already handled it can duplicate stop-confirm behavior.
   */
  stdin.on('keypress', (_str, key: ReadlineKey) => {
    if (key.ctrl && key.name === 'c') {
      interactiveTtyStdout.ctrlCExitNewline()
      doExit()
      return
    }
  })
}
