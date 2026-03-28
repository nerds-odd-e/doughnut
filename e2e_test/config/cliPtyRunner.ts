/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (arrows, list keys, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'
import {
  assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks,
  ptyTranscriptSimulatedPlainScreen,
} from '../step_definitions/cliSectionParser'
import { cliEnv } from './cliEnv'
import type { InteractiveCliPtyKeystroke } from './interactiveCliPtyTypes'

const PTY_TIMEOUT_MS = 25_000
const CLI_POLL_MS = 10
/** After writing draft bytes, pause before carriage return so Ink does not treat it as pasted text. */
const DRAFT_THEN_ENTER_GAP_MS = 80
/** Visible readiness requires briefly stable stdout to avoid PTY chunk races. */
const VISIBLE_READY_STABLE_MS = 120
/** Inspect only tail output while waiting for visible readiness. */
const FETCH_WAIT_TAIL_WINDOW_CHARS = 12_000

/** Keep in sync with `INTERACTIVE_FETCH_WAIT_LINES` in `cli/src/interactiveFetchWait.ts`. */
const INTERACTIVE_FETCH_WAIT_SNIPPETS = [
  'Loading recall questions',
  'Regenerating question',
  'Adding access token',
  'Creating access token',
  'Removing access token',
] as const
const VISIBLE_TAIL_LINES_WINDOW = 10
/** Match `PROMPT` in `cli/src/renderer.ts` — full-grid scan avoids missing `→` when replay leaves many trailing blank rows after long output (e.g. `/help`). */
const VISIBLE_COMMAND_LINE_PREFIX = '→ '
const VISIBLE_READY_TAIL_SNIPPETS = [
  'y or n; /stop to exit recall',
  'type your answer; /stop to exit recall',
  'y/N',
  'n/Y',
  '↑↓ Enter or number to select; Esc to cancel',
  '↑↓ Enter to select; other keys cancel',
] as const
// biome-ignore lint/suspicious/noControlCharactersInRegex: ANSI stripping for PTY readiness checks
const ANSI_AND_CSI_RE = /\x1b(?:\[[0-9;?]*[A-Za-z]|].*?(?:\x07|\x1b\\))/g

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

function formatCliPromptDidNotAppearError(stdout: string): string {
  return `CLI prompt did not appear within ${INTERACTIVE_CLI_PROMPT_WAIT_MS / 1000}s. stdout: ${stdout.slice(-300).replace(/\r/g, '\\r')}`
}

interface PtyHandle {
  pty: IPty
  stdout: { value: string }
  dispose: () => void
}

type WaitForInteractiveInputReadyOptions = {
  getStdout: () => string
  maxWaitMs: number
  /**
   * When set, only inspect output appended after this byte offset (e.g. length before a `pty.write`).
   */
  onlyInStdoutAfterByteLength?: number
  formatTimeoutError: (
    stdout: string,
    details: {
      visiblePredicate:
        | 'loading-tail-active'
        | 'no-command-line-row'
        | 'unstable-output'
        | 'ready'
      bytesAfterWindow: number
      hasVisibleCommandLineRow: boolean
      hasVisibleReadyPanel: boolean
      hasActiveFetchWaitTail: boolean
    }
  ) => string
}

async function waitForInteractiveInputReady(
  options: WaitForInteractiveInputReadyOptions
): Promise<void> {
  const {
    getStdout,
    maxWaitMs,
    onlyInStdoutAfterByteLength: afterLen,
    formatTimeoutError,
  } = options
  const start = Date.now()
  let stableSince = Date.now()
  let lastVisualSignature = ''
  let visiblePredicate:
    | 'loading-tail-active'
    | 'no-command-line-row'
    | 'unstable-output'
    | 'ready' = 'unstable-output'
  let timeoutDetails = {
    hasVisibleCommandLineRow: false,
    hasVisibleReadyPanel: false,
    hasActiveFetchWaitTail: false,
  }
  while (Date.now() - start < maxWaitMs) {
    const stdout = getStdout()
    const haystack = stdoutHaystackAfter(stdout, afterLen)
    const simulated = ptyTranscriptSimulatedPlainScreen(stdout)
    const screenTail = simulated
      .split('\n')
      .slice(-VISIBLE_TAIL_LINES_WINDOW)
      .join('\n')
    const rawTail = haystack.slice(-FETCH_WAIT_TAIL_WINDOW_CHARS)
    const rawTailPlain = rawTail.replace(ANSI_AND_CSI_RE, '')
    const rawFullPlain = stdout.replace(ANSI_AND_CSI_RE, '')
    const lineLooksLikeCommandPrompt = (line: string) =>
      line.trimStart().startsWith(VISIBLE_COMMAND_LINE_PREFIX)
    const hasCommandLineRow = simulated
      .split('\n')
      .some(lineLooksLikeCommandPrompt)
    const hasRawCommandLineRow = rawFullPlain
      .split('\n')
      .some(lineLooksLikeCommandPrompt)
    const hasVisibleCommandLineRow = hasCommandLineRow || hasRawCommandLineRow
    const hasVisibleReadyPanel = VISIBLE_READY_TAIL_SNIPPETS.some(
      (snippet) =>
        screenTail.includes(snippet) || rawTailPlain.includes(snippet)
    )
    const hasActiveFetchWaitTail = tailLooksLikeInteractiveFetchWait(screenTail)
    timeoutDetails = {
      hasVisibleCommandLineRow,
      hasVisibleReadyPanel,
      hasActiveFetchWaitTail,
    }
    const visualSignature = [screenTail, rawTailPlain.slice(-600)].join(
      '\n---\n'
    )
    if (visualSignature !== lastVisualSignature) {
      lastVisualSignature = visualSignature
      stableSince = Date.now()
    }

    if (
      hasActiveFetchWaitTail &&
      !hasVisibleCommandLineRow &&
      !hasVisibleReadyPanel
    ) {
      visiblePredicate = 'loading-tail-active'
      await sleep(CLI_POLL_MS)
      continue
    }

    if (!(hasVisibleCommandLineRow || hasVisibleReadyPanel)) {
      visiblePredicate = 'no-command-line-row'
      await sleep(CLI_POLL_MS)
      continue
    }

    if (Date.now() - stableSince >= VISIBLE_READY_STABLE_MS) {
      visiblePredicate = 'ready'
      if (afterLen === undefined || stdout.length > afterLen) {
        return
      }
      if (
        afterLen !== undefined &&
        (hasVisibleCommandLineRow || hasVisibleReadyPanel)
      ) {
        return
      }
    }
    visiblePredicate = 'unstable-output'

    await sleep(CLI_POLL_MS)
  }
  const stdout = getStdout()
  throw new Error(
    formatTimeoutError(stdout, {
      visiblePredicate,
      bytesAfterWindow: stdoutHaystackAfter(stdout, afterLen).length,
      ...timeoutDetails,
    })
  )
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
}): string {
  const lines = [
    `PTY timed out after ${PTY_TIMEOUT_MS / 1000}s`,
    `spawn: ${opts.spawnLabel}`,
    `cwd: ${opts.cwd}`,
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
      formatTimeoutError: (stdout) => formatCliPromptDidNotAppearError(stdout),
    })
      .then(async () => {
        assertInteractiveCliPtyTranscriptHasNoSimulatedEscapeLeaks(
          handle.stdout.value
        )
        const withoutTrailingNl = opts.input.replace(/\r?\n+$/, '')
        const isSingleSegment = !(
          withoutTrailingNl.includes('\n') || withoutTrailingNl.includes('\r')
        )
        if (isSingleSegment) {
          handle.pty.write(withoutTrailingNl)
          await sleep(DRAFT_THEN_ENTER_GAP_MS)
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
  await waitForInteractiveInputReady({
    getStdout: () => handle.stdout.value,
    maxWaitMs: INTERACTIVE_CLI_PROMPT_WAIT_MS,
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
    visiblePredicate:
      | 'loading-tail-active'
      | 'no-command-line-row'
      | 'unstable-output'
      | 'ready'
    bytesAfterWindow: number
    hasVisibleCommandLineRow: boolean
    hasVisibleReadyPanel: boolean
    hasActiveFetchWaitTail: boolean
  }
): string {
  return `CLI did not show input box after send within ${PTY_INPUT_READY_AFTER_SEND_MAX_MS / 1000}s. visible predicate=${details.visiblePredicate}, has command row=${details.hasVisibleCommandLineRow}, has ready panel=${details.hasVisibleReadyPanel}, fetch wait tail=${details.hasActiveFetchWaitTail}, bytes after send window=${details.bytesAfterWindow}, stdout grew by ${stdout.length - lenBeforeSend} chars. Tail: ${stdout.slice(-400).replace(/\r/g, '\\r')}`
}

async function ptyWaitForInputReadyAfterWrite(
  handle: PtyHandle,
  lenBeforeSend: number
): Promise<string> {
  await waitForInteractiveInputReady({
    getStdout: () => handle.stdout.value,
    maxWaitMs: PTY_INPUT_READY_AFTER_SEND_MAX_MS,
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
  await sleep(DRAFT_THEN_ENTER_GAP_MS)
  const lenBeforeSubmit = handle.stdout.value.length
  handle.pty.write('\r')
  return ptyWaitForInputReadyAfterWrite(handle, lenBeforeSubmit)
}

/** Deliver one keystroke to the shared interactive PTY and wait until the input box is ready again. */
export async function applyInteractiveCliPtyKeystroke(
  keystroke: InteractiveCliPtyKeystroke
): Promise<string> {
  return ptyWriteDraftThenCarriageReturnAndWait(keystroke.text)
}

export async function stopInteractiveCli(): Promise<void> {
  if (!interactiveHandle) return
  const { pty, dispose } = interactiveHandle
  interactiveHandle = null
  pty.kill('SIGKILL')
  dispose()
}
