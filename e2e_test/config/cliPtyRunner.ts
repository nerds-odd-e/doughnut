/**
 * Runs the Doughnut CLI in a pseudo-terminal (PTY) for interactive E2E tests.
 * Uses @lydell/node-pty so that keypress handling (ESC, arrows, etc.) works.
 */

import type { IPty } from '@lydell/node-pty'

const PTY_TIMEOUT_MS = 55_000
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

const CLI_READY_PATTERN = /doughnut\s+[\d.]+/

async function waitForCliReady(
  ptyProcess: IPty,
  getStdout: () => string
): Promise<void> {
  const maxWaitMs = 5_000
  const pollMs = 50
  const start = Date.now()
  while (Date.now() - start < maxWaitMs) {
    if (CLI_READY_PATTERN.test(getStdout())) return
    await new Promise((r) => setTimeout(r, pollMs))
  }
  throw new Error('CLI did not show readiness within 5s')
}

export async function runCliInPty(opts: {
  executablePath: string
  args?: string[]
  cwd: string
  env?: NodeJS.ProcessEnv
  input: CliPtyInput
}): Promise<string> {
  const pty = require('@lydell/node-pty') as {
    spawn: (file: string, args: string[], options: object) => IPty
  }
  const envMerged = { ...process.env, ...cliEnv(opts.env) }
  const ptyProcess = pty.spawn(
    process.execPath,
    [opts.executablePath, ...(opts.args ?? [])],
    {
      ...PTY_OPTIONS,
      cwd: opts.cwd,
      env: envMerged as { [key: string]: string },
    }
  )
  let stdout = ''
  const disposeData = ptyProcess.onData((data) => {
    stdout += data
  })
  return new Promise<string>((resolve, reject) => {
    const timeout = setTimeout(() => {
      ptyProcess.kill('SIGKILL')
      disposeData.dispose()
      reject(new Error('PTY timed out'))
    }, PTY_TIMEOUT_MS)
    ptyProcess.onExit(({ exitCode }) => {
      clearTimeout(timeout)
      disposeData.dispose()
      if (exitCode === 0) resolve(stdout)
      else reject(new Error(`CLI exited with code ${exitCode}`))
    })
    waitForCliReady(ptyProcess, () => stdout)
      .then(() => writeInput(ptyProcess, opts.input))
      .catch(reject)
  })
}
