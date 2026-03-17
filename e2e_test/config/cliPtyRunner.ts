/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (ESC, arrows, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'

const PTY_TIMEOUT_MS = 25_000
const PTY_OPTIONS = {
  name: 'xterm-256color' as const,
  cols: 80,
  rows: 24,
}

function cliEnv(overrides?: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  return {
    DOUGHNUT_API_BASE_URL: 'http://localhost:9081',
    ...overrides,
  }
}

export type CliPtyInput = string | { text: string; delayAfterMs?: number }[]

function normalizeInput(input: string): string {
  return input.endsWith('\n') ? input : `${input}\n`
}

async function writeInput(ptyProcess: IPty, input: CliPtyInput): Promise<void> {
  if (typeof input === 'string') {
    ptyProcess.write(normalizeInput(input))
    return
  }
  for (const chunk of input) {
    ptyProcess.write(chunk.text)
    if (chunk.delayAfterMs) {
      await new Promise((r) => setTimeout(r, chunk.delayAfterMs))
    }
  }
}

// Wait for the rendered prompt, not just version output.
// Version text is printed before readline key handlers are fully wired.
const CLI_READY_PATTERN = /\/ commands/

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

async function waitForCliReady(
  ptyProcess: IPty,
  getStdout: () => string
): Promise<void> {
  const maxWaitMs = 10_000
  const pollMs = 50
  const start = Date.now()
  while (Date.now() - start < maxWaitMs) {
    if (CLI_READY_PATTERN.test(getStdout())) return
    await new Promise((r) => setTimeout(r, pollMs))
  }
  const stdout = getStdout()
  throw new Error(
    `CLI prompt did not appear within 10s. stdout: ${stdout.slice(-300).replace(/\r/g, '\\r')}`
  )
}

export async function runCliInPty(opts: {
  /** Program to run (e.g. 'pnpm' or process.execPath) and full args. Use for TS source locally. */
  command?: string
  args?: string[]
  /** Legacy: spawn node with [executablePath, ...args]. Use for bundle or installed binary. */
  executablePath?: string
  cwd: string
  env?: NodeJS.ProcessEnv
  input: CliPtyInput
}): Promise<string> {
  const pty = require('@lydell/node-pty') as {
    spawn: (file: string, args: string[], options: object) => IPty
  }
  const envMerged = { ...process.env, ...cliEnv(opts.env) }
  const [file, fileArgs] =
    opts.command !== undefined
      ? [opts.command, opts.args ?? []]
      : [process.execPath, [opts.executablePath!, ...(opts.args ?? [])]]
  const spawnLabel = [file, ...fileArgs].join(' ')
  const ptyProcess = pty.spawn(file, fileArgs, {
    ...PTY_OPTIONS,
    cwd: opts.cwd,
    env: envMerged as { [key: string]: string },
  })
  let stdout = ''
  let readyReached = false
  let inputSent = false
  const disposeData = ptyProcess.onData((data) => {
    stdout += data
  })
  return new Promise<string>((resolve, reject) => {
    const timeout = setTimeout(() => {
      ptyProcess.kill('SIGKILL')
      disposeData.dispose()
      reject(
        new Error(
          formatPtyTimeoutDiagnostics({
            spawnLabel,
            cwd: opts.cwd,
            stdout,
            inputSent,
            readyReached,
          })
        )
      )
    }, PTY_TIMEOUT_MS)
    ptyProcess.onExit(({ exitCode }) => {
      clearTimeout(timeout)
      disposeData.dispose()
      if (exitCode === 0) resolve(stdout)
      else reject(new Error(`CLI exited with code ${exitCode}`))
    })
    waitForCliReady(ptyProcess, () => stdout)
      .then(() => {
        readyReached = true
        return writeInput(ptyProcess, opts.input)
      })
      .then(() => {
        inputSent = true
      })
      .catch(reject)
  })
}
