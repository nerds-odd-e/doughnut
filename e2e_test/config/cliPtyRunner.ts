/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (ESC, arrows, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'
import { assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks } from '../step_definitions/cliSectionParser'
import { cliEnv } from './cliEnv'

/**
 * Inlined: must stay byte-identical to `INTERACTIVE_INPUT_READY_OSC` in `cli/src/renderer.ts`.
 * The Cypress plugin process does not resolve imports into `cli/` from this file reliably.
 */
const INTERACTIVE_INPUT_READY_OSC =
  '\x1b]900;doughnut-interactive-input-ready\x07' as const

const PTY_TIMEOUT_MS = 25_000
const CLI_POLL_MS = 10
/** After the readiness OSC appears, wait briefly so any trailing PTY chunks flush. */
const INTERACTIVE_INPUT_READY_FLUSH_MS = 50

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
      .then(() => {
        assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
          handle.stdout.value
        )
        sawInteractiveInputReadyOsc = true
        const input = opts.input.endsWith('\n') ? opts.input : `${opts.input}\n`
        handle.pty.write(input)
      })
      .then(() => {
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

export async function sendToInteractiveCli(input: string): Promise<string> {
  if (!interactiveHandle) {
    throw new Error(
      'No interactive CLI running. Ensure @interactiveCLI Before hook ran.'
    )
  }
  const trimmed = input.trim()
  const toSend =
    trimmed === '\x1b'
      ? '\x1b'
      : trimmed.startsWith('/') && !trimmed.endsWith(' ')
        ? `${trimmed} \n`
        : trimmed.endsWith('\n')
          ? trimmed
          : `${trimmed}\n`
  const lenBeforeSend = interactiveHandle.stdout.value.length
  interactiveHandle.pty.write(toSend)
  await waitForInteractiveInputReadyOsc({
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

export async function stopInteractiveCli(): Promise<void> {
  if (!interactiveHandle) return
  const { pty, dispose } = interactiveHandle
  interactiveHandle = null
  pty.kill('SIGKILL')
  dispose()
}
