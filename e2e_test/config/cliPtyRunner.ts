/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (arrows, list keys, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'
import { assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks } from '../step_definitions/cliSectionParser'
import { INTERACTIVE_INPUT_READY_OSC } from './interactiveInputReadyOsc'
import { cliEnv } from './cliEnv'
import type { InteractiveCliPtyKeystroke } from './interactiveCliPtyTypes'

type OAuthSimulationState = { done: boolean }

/** When PTY stdout contains a Google OAuth URL, hit the redirect_uri with a mock code (E2E only). */
function notifyOAuthSimulationIfNeeded(
  fullStdout: string,
  state: OAuthSimulationState
): void {
  if (state.done) return
  const authMatch = fullStdout.match(/https:\/\/accounts\.google\.com\/[^\s]+/)
  if (!authMatch) return
  const redirectUri = new URL(authMatch[0]).searchParams.get('redirect_uri')
  if (!redirectUri) return
  state.done = true
  fetch(`${redirectUri}?code=e2e_mock_auth_code`).catch(() => {
    /* ignore OAuth callback errors */
  })
}

const PTY_TIMEOUT_MS = 25_000
const CLI_POLL_MS = 10
/** After the readiness OSC appears, wait briefly so any trailing PTY chunks flush. */
const INTERACTIVE_INPUT_READY_FLUSH_MS = 80
/** CI PTY can deliver Ink output in multiple chunks after the OSC; wait until length is stable. */
const INTERACTIVE_INPUT_READY_STABLE_MS = 70
/** When the CLI omits a fresh readiness OSC (see `handleShellRendered`), require longer quiet output before continuing. */
const INTERACTIVE_INPUT_READY_SETTLED_FALLBACK_STABLE_MS = 220
const INTERACTIVE_INPUT_READY_DRAIN_MAX_MS = 450
/**
 * Inspect only the end of post-submit stdout. Fetch-wait UIs omit the readiness OSC until work
 * finishes; byte-length can go quiet mid-load and falsely trigger the settled fallback — see
 * `INTERACTIVE_FETCH_WAIT_LINES` in `cli/src/interactiveFetchWait.ts` and `interactiveFetchWait`
 * placeholder in `cli/src/renderer.ts`.
 */
const FETCH_WAIT_TAIL_WINDOW_CHARS = 12_000

/** Keep in sync with `INTERACTIVE_FETCH_WAIT_LINES` in `cli/src/interactiveFetchWait.ts`. */
const INTERACTIVE_FETCH_WAIT_SNIPPETS = [
  'Loading recall questions',
  'Regenerating question',
  'Loading recall status',
  'Adding access token',
  'Creating access token',
  'Removing access token',
  'Connecting Gmail',
  'Loading last email',
] as const

/** Lowercase animated hint under the input box during TTY fetch-wait (`placeholderText.interactiveFetchWait`). */
function tailLooksLikeInteractiveFetchWait(haystack: string): boolean {
  const tail = haystack.slice(-FETCH_WAIT_TAIL_WINDOW_CHARS)
  for (const s of INTERACTIVE_FETCH_WAIT_SNIPPETS) {
    if (tail.includes(s)) return true
  }
  return /loading \.{1,3}/.test(tail)
}

const PTY_OPTIONS = {
  name: 'xterm-256color' as const,
  cols: 80,
  rows: 24,
}

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

const INTERACTIVE_CLI_PROMPT_WAIT_MS = 10_000

function stdoutHaystackAfter(
  stdout: string,
  onlyAfterByteLength: number | undefined
): string {
  if (onlyAfterByteLength === undefined) return stdout
  return stdout.length > onlyAfterByteLength
    ? stdout.slice(onlyAfterByteLength)
    : ''
}

/** After {@link INTERACTIVE_INPUT_READY_OSC} appears, flush then wait until PTY output length stabilizes. */
async function drainStdoutUntilStableAfterOsc(
  getStdout: () => string
): Promise<void> {
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
    } else if (Date.now() - stableSince >= INTERACTIVE_INPUT_READY_STABLE_MS) {
      return
    }
  }
}

function formatCliPromptDidNotAppearError(stdout: string): string {
  return `CLI prompt did not appear within ${INTERACTIVE_CLI_PROMPT_WAIT_MS / 1000}s. stdout: ${stdout.slice(-300).replace(/\r/g, '\\r')}`
}

interface PtyHandle {
  pty: IPty
  stdout: { value: string }
  dispose: () => void
}

/** Wait until stdout contains the interactive CLI’s invisible “ready for keystrokes” marker. */
type WaitForInteractiveInputReadyOptions = {
  getStdout: () => string
  maxWaitMs: number
  allowSettledFallback: boolean
  /**
   * When set, only search output appended after this byte offset (e.g. length before a `pty.write`).
   * Omit for startup: the marker may already exist earlier in the capture.
   */
  onlyInStdoutAfterByteLength?: number
  formatTimeoutError: (
    stdout: string,
    details: {
      mode:
        | 'waiting-for-osc'
        | 'waiting-for-settled-fallback'
        | 'fetch-wait-tail-active'
      bytesAfterWindow: number
    }
  ) => string
}

/**
 * Input-ready sync contract used by PTY E2E:
 * - startup waits for a real readiness OSC
 * - post-write waits for OSC first, then allows settled fallback when fetch-wait omits OSC
 */
async function waitForInteractiveInputReady(
  options: WaitForInteractiveInputReadyOptions
): Promise<void> {
  const {
    getStdout,
    maxWaitMs,
    allowSettledFallback,
    onlyInStdoutAfterByteLength: afterLen,
    formatTimeoutError,
  } = options
  const start = Date.now()
  let lastLen = getStdout().length
  let stableSince = Date.now()
  let timeoutMode:
    | 'waiting-for-osc'
    | 'waiting-for-settled-fallback'
    | 'fetch-wait-tail-active' = 'waiting-for-osc'
  while (Date.now() - start < maxWaitMs) {
    const stdout = getStdout()
    const haystack = stdoutHaystackAfter(stdout, afterLen)

    if (haystack.includes(INTERACTIVE_INPUT_READY_OSC)) {
      await drainStdoutUntilStableAfterOsc(getStdout)
      return
    }

    const len = stdout.length
    if (len !== lastLen) {
      lastLen = len
      stableSince = Date.now()
    } else if (
      allowSettledFallback &&
      afterLen !== undefined &&
      len > afterLen &&
      Date.now() - stableSince >=
        INTERACTIVE_INPUT_READY_SETTLED_FALLBACK_STABLE_MS
    ) {
      if (!tailLooksLikeInteractiveFetchWait(haystack)) {
        return
      }
      timeoutMode = 'fetch-wait-tail-active'
      stableSince = Date.now()
    } else if (allowSettledFallback) {
      timeoutMode = 'waiting-for-settled-fallback'
    }

    await sleep(CLI_POLL_MS)
  }
  const stdout = getStdout()
  throw new Error(
    formatTimeoutError(stdout, {
      mode: timeoutMode,
      bytesAfterWindow: stdoutHaystackAfter(stdout, afterLen).length,
    })
  )
}

function spawnPty(opts: {
  command: string
  args: string[]
  cwd: string
  env?: NodeJS.ProcessEnv
  simulateOAuthCallback?: boolean
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
  const oauthState: OAuthSimulationState = { done: false }
  const disposeData = ptyProcess.onData((data) => {
    stdout.value += data
    if (opts.simulateOAuthCallback) {
      notifyOAuthSimulationIfNeeded(stdout.value, oauthState)
    }
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
    waitForInteractiveInputReady({
      getStdout: () => handle.stdout.value,
      maxWaitMs: INTERACTIVE_CLI_PROMPT_WAIT_MS,
      allowSettledFallback: false,
      formatTimeoutError: (stdout) => formatCliPromptDidNotAppearError(stdout),
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
  simulateOAuthCallback?: boolean
}): Promise<void> {
  if (interactiveHandle) {
    throw new Error(
      'Interactive CLI already running. Call stopInteractiveCli first.'
    )
  }
  const handle = spawnPty(opts)
  await waitForInteractiveInputReady({
    getStdout: () => handle.stdout.value,
    maxWaitMs: INTERACTIVE_CLI_PROMPT_WAIT_MS,
    allowSettledFallback: false,
    formatTimeoutError: (stdout) => formatCliPromptDidNotAppearError(stdout),
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
function requireInteractivePtyHandle(): PtyHandle {
  if (!interactiveHandle) {
    throw new Error(
      'No interactive CLI running. Ensure @interactiveCLI Before hook ran.'
    )
  }
  return interactiveHandle
}

const PTY_INPUT_READY_AFTER_SEND_MAX_MS = 15_000

function formatPtyInputBoxTimeoutError(
  stdout: string,
  lenBeforeSend: number,
  details: {
    mode:
      | 'waiting-for-osc'
      | 'waiting-for-settled-fallback'
      | 'fetch-wait-tail-active'
    bytesAfterWindow: number
  }
): string {
  return `CLI did not show input box after send within ${PTY_INPUT_READY_AFTER_SEND_MAX_MS / 1000}s. mode=${details.mode}, bytes after send window=${details.bytesAfterWindow}, stdout grew by ${stdout.length - lenBeforeSend} chars. Tail: ${stdout.slice(-400).replace(/\r/g, '\\r')}`
}

async function ptyWaitForInputReadyAfterWrite(
  handle: PtyHandle,
  lenBeforeSend: number
): Promise<string> {
  await waitForInteractiveInputReady({
    getStdout: () => handle.stdout.value,
    maxWaitMs: PTY_INPUT_READY_AFTER_SEND_MAX_MS,
    allowSettledFallback: true,
    onlyInStdoutAfterByteLength: lenBeforeSend,
    formatTimeoutError: (stdout, details) =>
      formatPtyInputBoxTimeoutError(stdout, lenBeforeSend, details),
  })
  assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
    handle.stdout.value
  )
  return handle.stdout.value
}

async function ptyWriteDraftThenCarriageReturnAndWait(
  draftBytes: string
): Promise<string> {
  const handle = requireInteractivePtyHandle()
  handle.pty.write(draftBytes)
  await sleep(INTERACTIVE_INPUT_READY_FLUSH_MS)
  const lenBeforeSubmit = handle.stdout.value.length
  handle.pty.write('\r')
  return ptyWaitForInputReadyAfterWrite(handle, lenBeforeSubmit)
}

async function ptyWritePayloadAndWaitForInputReady(
  payload: string
): Promise<string> {
  const handle = requireInteractivePtyHandle()
  const lenBeforeSend = handle.stdout.value.length
  handle.pty.write(payload)
  return ptyWaitForInputReadyAfterWrite(handle, lenBeforeSend)
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
