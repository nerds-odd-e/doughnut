/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (ESC, arrows, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'
import { cliEnv } from './cliEnv'

const PTY_TIMEOUT_MS = 25_000
/** Polling interval for state-based waits; not a fixed delay. */
const CLI_POLL_MS = 10
const PTY_OPTIONS = {
  name: 'xterm-256color' as const,
  cols: 80,
  rows: 24,
}

const CLI_READY_PATTERN = /\/ commands/

// The CLI renders placeholder text with ANSI grey (\x1b[90m) when the input
// buffer is empty (ready for input). In normal mode: `│ → \x1b[90m<placeholder>`.
// In selection mode (token list): the entire box is grey-wrapped with no arrow,
// so the content line is `\x1b[90m│ \x1b[90m<placeholder>`.
// Matching grey after the box border character detects both modes without
// enumerating placeholder strings (those live in renderer.ts PLACEHOLDER_BY_CONTEXT).
// biome-ignore lint/suspicious/noControlCharactersInRegex: matching ANSI escape codes in PTY output
const INPUT_BOX_READY_PATTERN = /(?:│ → |\x1b\[90m│ )\x1b\[90m/

interface PtyHandle {
  pty: IPty
  stdout: { value: string }
  dispose: () => void
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

async function waitForCliReady(getStdout: () => string): Promise<void> {
  const maxWaitMs = 10_000
  const start = Date.now()
  while (Date.now() - start < maxWaitMs) {
    if (CLI_READY_PATTERN.test(getStdout())) return
    await new Promise((r) => setTimeout(r, CLI_POLL_MS))
  }
  const stdout = getStdout()
  throw new Error(
    `CLI prompt did not appear within 10s. stdout: ${stdout.slice(-300).replace(/\r/g, '\\r')}`
  )
}

async function waitForInputBoxReady(
  getStdout: () => string,
  lenBeforeSend: number
): Promise<void> {
  const maxWaitMs = 15_000
  // Interactive fetch wait repaints every 400ms (ellipsis on the current prompt). The grey
  // disabled input box matches INPUT_BOX_READY_PATTERN too, so a short stability window (e.g. 30ms)
  // can return before the HTTP call finishes. Require stability longer than that interval.
  const INPUT_BOX_STABLE_MS = 550
  const stablePollsRequired = Math.ceil(INPUT_BOX_STABLE_MS / CLI_POLL_MS)
  const start = Date.now()
  let lastStdoutLen = 0
  let stablePolls = 0
  while (Date.now() - start < maxWaitMs) {
    const stdout = getStdout()
    if (stdout.length <= lenBeforeSend) {
      await new Promise((r) => setTimeout(r, CLI_POLL_MS))
      continue
    }
    const newContent = stdout.slice(lenBeforeSend)
    if (INPUT_BOX_READY_PATTERN.test(newContent)) {
      if (stdout.length === lastStdoutLen) {
        stablePolls++
        if (stablePolls >= stablePollsRequired) return
      } else {
        stablePolls = 0
      }
      lastStdoutLen = stdout.length
    } else {
      stablePolls = 0
    }
    await new Promise((r) => setTimeout(r, CLI_POLL_MS))
  }
  const stdout = getStdout()
  throw new Error(
    `CLI did not show input box after send within 15s. stdout grew by ${stdout.length - lenBeforeSend} chars. Tail: ${stdout.slice(-400).replace(/\r/g, '\\r')}`
  )
}

function formatPtyTimeoutDiagnostics(opts: {
  spawnLabel: string
  cwd: string
  stdout: string
  inputSent: boolean
  readyReached: boolean
}): string {
  const lines = [
    `PTY timed out after ${PTY_TIMEOUT_MS / 1000}s`,
    `spawn: ${opts.spawnLabel}`,
    `cwd: ${opts.cwd}`,
    `CLI ready (prompt shown): ${opts.readyReached}`,
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
  let readyReached = false
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
            readyReached,
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
    waitForCliReady(() => handle.stdout.value)
      .then(() => {
        readyReached = true
        const input = opts.input.endsWith('\n') ? opts.input : `${opts.input}\n`
        handle.pty.write(input)
      })
      .then(() => {
        inputSent = true
      })
      .catch(reject)
  })
}

/** Module-level handle for long-running interactive CLI. Used by Cypress tasks. */
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
  await waitForCliReady(() => handle.stdout.value)
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
  await waitForInputBoxReady(
    () => interactiveHandle!.stdout.value,
    lenBeforeSend
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
