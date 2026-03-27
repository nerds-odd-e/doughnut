import React from 'react'
import * as readline from 'node:readline'
import { Writable } from 'node:stream'
import { render } from 'ink'
import { formatVersionOutput } from '../version.js'
import { cancelInteractiveFetchWaitFor } from '../interactiveFetchWait.js'
import {
  createInitialShellSessionState,
  type ShellSessionState,
} from '../shell/shellSessionState.js'
import { interactiveTtyStdout } from './interactiveTtyStdout.js'
import type { OutputAdapter } from '../types.js'
import { isAlternateLivePanel } from '../ui/ShellSessionRoot.js'
import type { TTYDeps } from './ttyDeps.js'
import { InteractiveTtyInkApp } from './interactiveTtyInkApp.js'

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

export function runInteractiveTtySession(stdin: TTYInput, deps: TTYDeps): void {
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

  const stdinForInk = stdin as NodeJS.ReadableStream & {
    ref?: () => void
    unref?: () => void
  }
  patchStdinForInk(stdinForInk)
  shellInstance = render(
    React.createElement(InteractiveTtyInkApp, {
      initialSession,
      deps,
      latestSessionRef,
      stdinTty,
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
   * and token-list Esc (list-selection live column). Fetch-wait has no active Ink `useInput` (same as
   * disabled `@inkjs/ui` `TextInput`); **Esc** to cancel that wait is handled here so raw-mode
   * lifecycle matches the pre–phase-16 shell. This listener is also **Ctrl+C** (exit before Ink).
   * Do not add MCQ Esc here: a late readline `escape` after Ink already handled it can duplicate
   * stop-confirm behavior.
   */
  stdin.on('keypress', (_str, key: ReadlineKey) => {
    if (key.ctrl && key.name === 'c') {
      interactiveTtyStdout.ctrlCExitNewline()
      doExit()
      return
    }
    if (
      key.name === 'escape' &&
      ttyOutputRef.current &&
      cancelInteractiveFetchWaitFor(ttyOutputRef.current)
    ) {
      return
    }
  })
}
