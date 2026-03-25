/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (ESC, arrows, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'
import {
  assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks,
  INTERACTIVE_INPUT_READY_OSC,
} from '../step_definitions/cliSectionParser'
import { cliEnv } from './cliEnv'
import type { InteractiveCliPtyKeystroke } from './interactiveCliPtyTypes'

const PTY_TIMEOUT_MS = 25_000
const CLI_POLL_MS = 10
/** After the readiness OSC appears, wait briefly so any trailing PTY chunks flush. */
const INTERACTIVE_INPUT_READY_FLUSH_MS = 80
/** CI PTY can deliver Ink output in multiple chunks after the OSC; wait until length is stable. */
const INTERACTIVE_INPUT_READY_STABLE_MS = 70
/** When the CLI omits a fresh readiness OSC (see `handleShellRendered`), require longer quiet output before continuing. */
const INTERACTIVE_INPUT_READY_SETTLED_FALLBACK_STABLE_MS = 220
const INTERACTIVE_INPUT_READY_DRAIN_MAX_MS = 450

const PTY_OPTIONS = {
  name: 'xterm-256color' as const,
  cols: 80,
  rows: 24,
}

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

interface PtyHandle {
  pty: IPty
  stdout: { value: string }
  dispose: () => void
}

/** Wait until stdout contains the interactive CLI’s invisible “ready for keystrokes” marker. */
type WaitForInteractiveInputReadyOptions = {
  getStdout: () => string
  maxWaitMs: number
  /**
   * When set, only search output appended after this byte offset (e.g. length before a `pty.write`).
   * Omit for startup: the marker may already exist earlier in the capture.
   */
  onlyInStdoutAfterByteLength?: number
  formatTimeoutError: (stdout: string) => string
}

async function waitForInteractiveInputReadyOsc(
  options: WaitForInteractiveInputReadyOptions
): Promise<void> {
  const {
    getStdout,
    maxWaitMs,
    onlyInStdoutAfterByteLength: afterLen,
    formatTimeoutError,
  } = options
  const start = Date.now()
  while (Date.now() - start < maxWaitMs) {
    const stdout = getStdout()
    const haystack =
      afterLen === undefined
        ? stdout
        : stdout.length > afterLen
          ? stdout.slice(afterLen)
          : ''
    if (haystack.includes(INTERACTIVE_INPUT_READY_OSC)) {
      await sleep(INTERACTIVE_INPUT_READY_FLUSH_MS)
      const drainDeadline = Date.now() + INTERACTIVE_INPUT_READY_DRAIN_MAX_MS
      let lastLen = getStdout().length
      let stableSince = Date.now()
      while (Date.now() < drainDeadline) {
        await sleep(CLI_POLL_MS)
        const len = getStdout().length
        if (len !== lastLen) {
          lastLen = len
          stableSince = Date.now()
        } else if (
          Date.now() - stableSince >=
          INTERACTIVE_INPUT_READY_STABLE_MS
        ) {
          return
        }
      }
      return
    }
    await sleep(CLI_POLL_MS)
  }
  throw new Error(formatTimeoutError(getStdout()))
}

/**
 * After a PTY write, the CLI often appends {@link INTERACTIVE_INPUT_READY_OSC}. Fetch-wait omits
 * it (see `handleShellRendered` in `ttyAdapter`). Accept either a fresh OSC in bytes after
 * `afterLen`, or stdout that grew past `afterLen` and then stayed stable — same idea as the
 * post-OSC drain loop.
 */
async function waitForInteractiveInputReadyOscOrSettled(
  options: WaitForInteractiveInputReadyOptions
): Promise<void> {
  const {
    getStdout,
    maxWaitMs,
    onlyInStdoutAfterByteLength: afterLen,
    formatTimeoutError,
  } = options
  const start = Date.now()
  let lastLen = getStdout().length
  let stableSince = Date.now()
  while (Date.now() - start < maxWaitMs) {
    const stdout = getStdout()
    const haystack =
      afterLen === undefined
        ? stdout
        : stdout.length > afterLen
          ? stdout.slice(afterLen)
          : ''

    if (haystack.includes(INTERACTIVE_INPUT_READY_OSC)) {
      await sleep(INTERACTIVE_INPUT_READY_FLUSH_MS)
      const drainDeadline = Date.now() + INTERACTIVE_INPUT_READY_DRAIN_MAX_MS
      lastLen = getStdout().length
      stableSince = Date.now()
      while (Date.now() < drainDeadline) {
        await sleep(CLI_POLL_MS)
        const len = getStdout().length
        if (len !== lastLen) {
          lastLen = len
          stableSince = Date.now()
        } else if (
          Date.now() - stableSince >=
          INTERACTIVE_INPUT_READY_STABLE_MS
        ) {
          return
        }
      }
      return
    }

    const len = stdout.length
    if (len !== lastLen) {
      lastLen = len
      stableSince = Date.now()
    } else if (
      afterLen !== undefined &&
      len > afterLen &&
      Date.now() - stableSince >=
        INTERACTIVE_INPUT_READY_SETTLED_FALLBACK_STABLE_MS
    ) {
      return
    }

    await sleep(CLI_POLL_MS)
  }
  throw new Error(formatTimeoutError(getStdout()))
}

function spawnPty(opts: {
  command: string
  args: string[]
  cwd: string
  env?: NodeJS.ProcessEnv
}): PtyHandle {
  const pty = require('@lydell/node-pty') as {
    spawn: (file: string, args: string[], options: object) => IPty
  }
  const envMerged = { ...process.env, ...cliEnv(opts.env) }
  const ptyProcess = pty.spawn(opts.command, opts.args, {
    ...PTY_OPTIONS,
    cwd: opts.cwd,
    env: envMerged as { [key: string]: string },
  })
  const stdout = { value: '' }
  const disposeData = ptyProcess.onData((data) => {
    stdout.value += data
  })
  return { pty: ptyProcess, stdout, dispose: () => disposeData.dispose() }
}

function formatPtyTimeoutDiagnostics(opts: {
  spawnLabel: string
  cwd: string
  stdout: string
  inputSent: boolean
  sawInteractiveInputReadyOsc: boolean
}): string {
  const lines = [
    `PTY timed out after ${PTY_TIMEOUT_MS / 1000}s`,
    `spawn: ${opts.spawnLabel}`,
    `cwd: ${opts.cwd}`,
    `interactive input ready OSC seen: ${opts.sawInteractiveInputReadyOsc}`,
    `input sent: ${opts.inputSent}`,
    `stdout (${opts.stdout.length} chars):`,
    opts.stdout
      ? opts.stdout.slice(-500).replace(/\r/g, '\\r').replace(/\n/g, '\\n ')
      : '(empty)',
  ]
  return lines.join('\n')
}

export async function runCliInPty(opts: {
  executablePath: string
  args?: string[]
  cwd: string
  env?: NodeJS.ProcessEnv
  input: string
}): Promise<string> {
  const args = [opts.executablePath, ...(opts.args ?? [])]
  const spawnLabel = [process.execPath, ...args].join(' ')
  const handle = spawnPty({
    command: process.execPath,
    args,
    cwd: opts.cwd,
    env: opts.env,
  })
  let sawInteractiveInputReadyOsc = false
  let inputSent = false
  return new Promise<string>((resolve, reject) => {
    const timeout = setTimeout(() => {
      handle.pty.kill('SIGKILL')
      handle.dispose()
      reject(
        new Error(
          formatPtyTimeoutDiagnostics({
            spawnLabel,
            cwd: opts.cwd,
            stdout: handle.stdout.value,
            inputSent,
            sawInteractiveInputReadyOsc,
          })
        )
      )
    }, PTY_TIMEOUT_MS)
    handle.pty.onExit(({ exitCode }) => {
      clearTimeout(timeout)
      handle.dispose()
      if (exitCode === 0) resolve(handle.stdout.value)
      else reject(new Error(`CLI exited with code ${exitCode}`))
    })
    waitForInteractiveInputReadyOsc({
      getStdout: () => handle.stdout.value,
      maxWaitMs: 10_000,
      formatTimeoutError: (stdout) =>
        `CLI prompt did not appear within 10s. stdout: ${stdout.slice(-300).replace(/\r/g, '\\r')}`,
    })
      .then(async () => {
        assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
          handle.stdout.value
        )
        sawInteractiveInputReadyOsc = true
        const withoutTrailingNl = opts.input.replace(/\r?\n+$/, '')
        const isSingleSegment = !(
          withoutTrailingNl.includes('\n') || withoutTrailingNl.includes('\r')
        )
        if (isSingleSegment) {
          handle.pty.write(withoutTrailingNl)
          await sleep(INTERACTIVE_INPUT_READY_FLUSH_MS)
          handle.pty.write('\r')
        } else {
          const input = opts.input.endsWith('\n')
            ? opts.input
            : `${opts.input}\n`
          handle.pty.write(input)
        }
        inputSent = true
      })
      .catch(reject)
  })
}

let interactiveHandle: PtyHandle | null = null

export async function startInteractiveCli(opts: {
  command: string
  args: string[]
  cwd: string
  env?: NodeJS.ProcessEnv
}): Promise<void> {
  if (interactiveHandle) {
    throw new Error(
      'Interactive CLI already running. Call stopInteractiveCli first.'
    )
  }
  const handle = spawnPty(opts)
  await waitForInteractiveInputReadyOsc({
    getStdout: () => handle.stdout.value,
    maxWaitMs: 10_000,
    formatTimeoutError: (stdout) =>
      `CLI prompt did not appear within 10s. stdout: ${stdout.slice(-300).replace(/\r/g, '\\r')}`,
  })
  assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
    handle.stdout.value
  )
  interactiveHandle = handle
}

/**
 * One stdin `read()` must not contain text + CR together: Ink treats that as pasted text (literal
 * `\r` in the box). Send the draft, pause for Ink, then send `\r` alone (same idea as Vitest
 * `pushTTYCommandBytes` + `pushTTYCommandEnter`).
 */
async function ptyWriteDraftThenCarriageReturnAndWait(
  draftBytes: string
): Promise<string> {
  if (!interactiveHandle) {
    throw new Error(
      'No interactive CLI running. Ensure @interactiveCLI Before hook ran.'
    )
  }
  interactiveHandle.pty.write(draftBytes)
  await sleep(INTERACTIVE_INPUT_READY_FLUSH_MS)
  const lenBeforeSubmit = interactiveHandle.stdout.value.length
  interactiveHandle.pty.write('\r')
  await waitForInteractiveInputReadyOscOrSettled({
    getStdout: () => interactiveHandle!.stdout.value,
    maxWaitMs: 15_000,
    onlyInStdoutAfterByteLength: lenBeforeSubmit,
    formatTimeoutError: (stdout) =>
      `CLI did not show input box after send within 15s. stdout grew by ${stdout.length - lenBeforeSubmit} chars. Tail: ${stdout.slice(-400).replace(/\r/g, '\\r')}`,
  })
  assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
    interactiveHandle.stdout.value
  )
  return interactiveHandle.stdout.value
}

async function ptyWritePayloadAndWaitForInputReady(
  payload: string
): Promise<string> {
  if (!interactiveHandle) {
    throw new Error(
      'No interactive CLI running. Ensure @interactiveCLI Before hook ran.'
    )
  }
  const lenBeforeSend = interactiveHandle.stdout.value.length
  interactiveHandle.pty.write(payload)
  await waitForInteractiveInputReadyOscOrSettled({
    getStdout: () => interactiveHandle!.stdout.value,
    maxWaitMs: 15_000,
    onlyInStdoutAfterByteLength: lenBeforeSend,
    formatTimeoutError: (stdout) =>
      `CLI did not show input box after send within 15s. stdout grew by ${stdout.length - lenBeforeSend} chars. Tail: ${stdout.slice(-400).replace(/\r/g, '\\r')}`,
  })
  assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
    interactiveHandle.stdout.value
  )
  return interactiveHandle.stdout.value
}

/** Deliver one keystroke to the shared interactive PTY and wait until the input box is ready again. */
export async function applyInteractiveCliPtyKeystroke(
  keystroke: InteractiveCliPtyKeystroke
): Promise<string> {
  switch (keystroke.kind) {
    case 'line':
      return ptyWriteDraftThenCarriageReturnAndWait(keystroke.text)
    case 'slashCommand':
      return ptyWriteDraftThenCarriageReturnAndWait(`${keystroke.commandLine} `)
    case 'enter':
      return ptyWritePayloadAndWaitForInputReady('\r')
    case 'escape':
      return ptyWritePayloadAndWaitForInputReady('\x1b')
    case 'rawKey': {
      if (keystroke.char.length !== 1) {
        throw new Error(
          `rawKey keystroke expects a single character, got ${JSON.stringify(keystroke.char)}`
        )
      }
      return ptyWritePayloadAndWaitForInputReady(keystroke.char)
    }
  }
}

export async function stopInteractiveCli(): Promise<void> {
  if (!interactiveHandle) return
  const { pty, dispose } = interactiveHandle
  interactiveHandle = null
  pty.kill('SIGKILL')
  dispose()
}
