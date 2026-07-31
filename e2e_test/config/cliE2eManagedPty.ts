/**
 * Managed PTY session for CLI E2E Cypress tasks (interactive + one-shot).
 */

import type { ManagedTtyAssertInput, ManagedTtySession } from 'tty-assert'
import { startManagedTtySession } from 'tty-assert'
import { cliEnv } from './cliEnv'
import { formatCliVersionBanner } from './cliVersion'
import { readCliPackageVersion } from './cliE2eRepo'

const INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS = 20_000
const CLI_INTERACTIVE_WRITE_IDLE_MS = 40
const CLI_INTERACTIVE_WRITE_IDLE_TIMEOUT_MS = 15_000

/** Interactive CLI PTY size for Cypress (failure PNG/GIF); smaller than tty-assert defaults (120×48). */
const CLI_E2E_INTERACTIVE_PTY_COLS = 80
const CLI_E2E_INTERACTIVE_PTY_ROWS = 24

export const CLI_E2E_MANAGED_PTY_GEOMETRY = {
  cols: CLI_E2E_INTERACTIVE_PTY_COLS,
  rows: CLI_E2E_INTERACTIVE_PTY_ROWS,
} as const

export const NON_INTERACTIVE_CLI_EXIT_TIMEOUT_MS = 60_000

type PtyWithOnExit = {
  onExit: (listener: (e: { exitCode: number; signal?: number }) => void) => {
    dispose: () => void
  }
}

export type CliE2eManagedPtyOptions = {
  repoRoot: string
  saveBufferToCurrentSpecFolder: (
    stemPrefix: string,
    extensionWithDot: string,
    data: Buffer
  ) => string
}

async function waitForInteractiveCliTranscriptIdle(
  handle: ManagedTtySession,
  timeoutMs = CLI_INTERACTIVE_WRITE_IDLE_TIMEOUT_MS
): Promise<void> {
  const deadline = Date.now() + timeoutMs
  let last = handle.session.buf.text
  let stableSince = Date.now()
  while (Date.now() < deadline) {
    await new Promise<void>((resolve) =>
      setTimeout(resolve, CLI_INTERACTIVE_WRITE_IDLE_MS)
    )
    const current = handle.session.buf.text
    if (current !== last) {
      last = current
      stableSince = Date.now()
      continue
    }
    if (Date.now() - stableSince >= CLI_INTERACTIVE_WRITE_IDLE_MS) {
      return
    }
  }
  throw new Error(
    `Timeout after ${timeoutMs}ms waiting for interactive CLI transcript to settle`
  )
}

export function waitForPtyExit(
  pty: PtyWithOnExit,
  expectCode: number,
  timeoutMs: number
): Promise<void> {
  return new Promise((resolve, reject) => {
    let timeoutId: ReturnType<typeof setTimeout> | undefined
    const sub = pty.onExit(({ exitCode, signal }) => {
      if (timeoutId !== undefined) clearTimeout(timeoutId)
      sub.dispose()
      if (exitCode === expectCode) {
        resolve()
        return
      }
      reject(
        new Error(
          `CLI exited with code ${exitCode}${signal != null ? ` (signal ${signal})` : ''}`
        )
      )
    })
    timeoutId = setTimeout(() => {
      sub.dispose()
      reject(
        new Error(
          `Timeout after ${timeoutMs}ms waiting for non-interactive CLI to exit`
        )
      )
    }, timeoutMs)
  })
}

export function createCliE2eManagedPty(options: CliE2eManagedPtyOptions) {
  const { repoRoot, saveBufferToCurrentSpecFolder } = options
  let interactiveCliPtyHandle: ManagedTtySession | null = null

  function dispose(): void {
    interactiveCliPtyHandle?.dispose()
    interactiveCliPtyHandle = null
  }

  function requireHandle(caller: string): ManagedTtySession {
    if (!interactiveCliPtyHandle) {
      throw new Error(
        `${caller}: no managed CLI PTY session. Start interactive (runInstalledCliInteractive / runRepoCliInteractive) or run a one-shot command (runInstalledCli) first.`
      )
    }
    return interactiveCliPtyHandle
  }

  async function assertWithTerminalArtifacts(
    handle: ManagedTtySession,
    assertOpts: ManagedTtyAssertInput,
    logPrefix: string
  ): Promise<void> {
    try {
      await handle.assert(assertOpts)
    } catch (err) {
      try {
        const png = await handle.captureViewportPng()
        const pngPath = saveBufferToCurrentSpecFolder(
          'terminal-pty-assert-failure',
          '.png',
          png
        )
        let suffix = `\n\nTerminal viewport PNG: ${pngPath}`
        try {
          const gif = await handle.buildViewportAnimationGif()
          const gifPath = saveBufferToCurrentSpecFolder(
            'terminal-pty-anim',
            '.gif',
            gif
          )
          suffix += `\nTerminal viewport animation (GIF): ${gifPath}`
        } catch (gifErr) {
          const msg = gifErr instanceof Error ? gifErr.message : String(gifErr)
          if (!msg.includes('at least 2 distinct viewport frames')) {
            console.error(
              `${logPrefix}: failed to build/save terminal GIF`,
              gifErr
            )
          }
        }
        if (err instanceof Error) {
          err.message = `${err.message}${suffix}`
        }
      } catch (captureErr) {
        console.error(
          `${logPrefix}: failed to capture/save terminal failure artifacts`,
          captureErr
        )
      }
      throw err
    }
  }

  async function startInteractive(opts: {
    command: string
    args: string[]
    cwd: string
    env?: NodeJS.ProcessEnv
  }): Promise<void> {
    dispose()
    const managed = await startManagedTtySession(
      {
        command: opts.command,
        args: opts.args,
        cwd: opts.cwd,
        env: { ...process.env, ...cliEnv(opts.env) },
      },
      CLI_E2E_MANAGED_PTY_GEOMETRY
    )
    interactiveCliPtyHandle = managed
    await managed.assert({
      needle: formatCliVersionBanner(readCliPackageVersion(repoRoot)),
      surface: 'strippedTranscript',
      timeoutMs: INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS,
    })
  }

  return {
    dispose,
    getHandle: () => interactiveCliPtyHandle,
    requireHandle,
    setHandle(handle: ManagedTtySession) {
      interactiveCliPtyHandle = handle
    },
    startInteractive,
    assert(body: ManagedTtyAssertInput): Promise<void> {
      return assertWithTerminalArtifacts(
        requireHandle('cliAssert'),
        body,
        'cliAssert'
      )
    },
    async writeLine(line: string): Promise<void> {
      const handle = requireHandle('cliInteractiveWriteLine')
      handle.submit(line)
      await waitForInteractiveCliTranscriptIdle(handle)
    },
    async writeRaw(data: string): Promise<void> {
      const handle = requireHandle('cliInteractiveWriteRaw')
      handle.write(data)
      await waitForInteractiveCliTranscriptIdle(handle)
    },
  }
}

export type CliE2eManagedPty = ReturnType<typeof createCliE2eManagedPty>
