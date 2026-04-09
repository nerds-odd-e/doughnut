/**
 * Cypress `task` handlers for CLI E2E. Depends only on `repoRoot` (repo checkout path).
 */

import { existsSync, mkdtempSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { delimiter, dirname, join } from 'node:path'
import { attachGoogleOAuthSimulation } from './cliE2eGoogleOAuthSimulation'
import {
  bundleCliE2eInstall,
  CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH,
  cliRepoSpawnFromRoot,
  runShellCommandSync,
} from './cliE2eRepo'
import { cliEnv } from './cliEnv'
import {
  managedTtyAssertOptionsFromJson,
  startManagedTtySession,
  type ManagedTtyAssertJsonPayload,
  type ManagedTtyAssertOptions,
  type ManagedTtySession,
} from 'tty-assert'

/** Cypress `cy.task` body: same as {@link ManagedTtyAssertJsonPayload} from `tty-assert`. */
export type ManagedTtyAssertTaskPayload = ManagedTtyAssertJsonPayload

type WithOptionalCliEnv = { env?: NodeJS.ProcessEnv }

type RunInstalledCliTask = WithOptionalCliEnv & {
  doughnutPath: string
  args?: string[]
}

type RunInstalledCliInteractiveTask = WithOptionalCliEnv & {
  doughnutPath: string
}

type RunRepoCliInteractiveTask = WithOptionalCliEnv

type CliInteractiveWriteLineTask = {
  line: string
}

type CliInteractiveWriteRawTask = {
  data: string
}

export type CliE2ePluginTasksOptions = {
  /** Saves under the current Cypress spec screenshot folder (see `attachCypressSpecScreenshotSink`). */
  saveBufferToCurrentSpecFolder: (
    stemPrefix: string,
    extensionWithDot: string,
    data: Buffer
  ) => string
}

const INSTALLED_CLI_INTERACTIVE_STARTUP_SUBSTRING = 'doughnut 0.1.0'
const INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS = 20_000
const INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS = 500

/** Interactive CLI PTY size for Cypress (failure PNG/GIF); smaller than tty-assert defaults (120×48). */
const CLI_E2E_INTERACTIVE_PTY_COLS = 80
const CLI_E2E_INTERACTIVE_PTY_ROWS = 24

const CLI_E2E_MANAGED_PTY_GEOMETRY = {
  cols: CLI_E2E_INTERACTIVE_PTY_COLS,
  rows: CLI_E2E_INTERACTIVE_PTY_ROWS,
} as const

const NON_INTERACTIVE_CLI_EXIT_TIMEOUT_MS = 60_000

type PtyWithOnExit = {
  onExit: (listener: (e: { exitCode: number; signal?: number }) => void) => {
    dispose: () => void
  }
}

function waitForPtyExit(
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

async function bundleCliE2eInstallOrThrow(
  repoRoot: string,
  env?: NodeJS.ProcessEnv
): Promise<true> {
  try {
    bundleCliE2eInstall(repoRoot, env)
    return true
  } catch (error) {
    console.error('Failed to bundle E2E install CLI:', error)
    throw error
  }
}

export function createCliE2ePluginTasks(
  repoRoot: string,
  options: CliE2ePluginTasksOptions
) {
  let interactiveCliPtyHandle: ManagedTtySession | null = null

  function disposeManagedCliPtySession(): void {
    interactiveCliPtyHandle?.dispose()
    interactiveCliPtyHandle = null
  }

  async function managedAssertWithTerminalArtifacts(
    handle: ManagedTtySession,
    assertOpts: ManagedTtyAssertOptions,
    logPrefix: string
  ): Promise<void> {
    try {
      await handle.assert(assertOpts)
    } catch (err) {
      try {
        const png = await handle.captureViewportPng()
        const pngPath = options.saveBufferToCurrentSpecFolder(
          'terminal-pty-assert-failure',
          '.png',
          png
        )
        let suffix = `\n\nTerminal viewport PNG: ${pngPath}`
        try {
          const gif = await handle.buildViewportAnimationGif()
          const gifPath = options.saveBufferToCurrentSpecFolder(
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

  async function startInteractiveCliPtySession(opts: {
    command: string
    args: string[]
    cwd: string
    env?: NodeJS.ProcessEnv
  }): Promise<void> {
    disposeManagedCliPtySession()
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
      needle: INSTALLED_CLI_INTERACTIVE_STARTUP_SUBSTRING,
      surface: 'strippedTranscript',
      timeoutMs: INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS,
    })
  }

  return {
    getMineruE2eMockSitePath(): string {
      return join(repoRoot, 'e2e_test', 'python_stubs', 'mineru_site')
    },
    prependMineruMockToPythonPath(mockSite: string): string {
      const tail = process.env.PYTHONPATH?.trim()
      return tail ? `${mockSite}${delimiter}${tail}` : mockSite
    },
    getE2eFixtureAbsolutePath(relativePath: string): string {
      const normalized = relativePath.replace(/^\/+/, '')
      const p = join(repoRoot, 'e2e_test', 'fixtures', normalized)
      if (!existsSync(p)) {
        throw new Error(
          `E2E fixture not found: ${p} (relative to e2e_test/fixtures: ${normalized})`
        )
      }
      return p
    },
    createCliConfigDir() {
      return mkdtempSync(join(tmpdir(), 'cypress-cli-config-'))
    },
    createCliConfigDirWithGmail(gmailConfig: Record<string, unknown>) {
      const configDir = mkdtempSync(join(tmpdir(), 'cypress-cli-gmail-'))
      writeFileSync(
        join(configDir, 'gmail.json'),
        JSON.stringify(gmailConfig, null, 2)
      )
      return configDir
    },
    async bundleCliE2eInstall() {
      return bundleCliE2eInstallOrThrow(repoRoot)
    },
    async bundleCliE2eInstallWithVersion(version: string) {
      return bundleCliE2eInstallOrThrow(repoRoot, {
        ...process.env,
        CLI_VERSION: version,
      })
    },
    removeE2eInstallCliBundle() {
      const p = join(repoRoot, CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH)
      if (existsSync(p)) unlinkSync(p)
      return null
    },
    async installCli(baseUrl: string) {
      disposeManagedCliPtySession()
      const installDir = mkdtempSync(join(tmpdir(), 'cypress-doughnut-cli-'))
      const installScriptPath = join(installDir, 'install.sh')
      const response = await fetch(`${baseUrl}/install`)
      if (!response.ok) {
        throw new Error(
          `installCli: failed to fetch install script from ${baseUrl}/install: ${response.status}`
        )
      }
      const script = await response.text()
      writeFileSync(installScriptPath, script, { mode: 0o755 })
      runShellCommandSync(`bash ${installScriptPath}`, {
        env: {
          ...process.env,
          INSTALL_PREFIX: installDir,
          BASE_URL: baseUrl,
        },
      })
      const doughnutPath = join(installDir, 'bin', 'doughnut')
      if (!existsSync(doughnutPath)) {
        throw new Error(
          `installCli: doughnut binary not found at ${doughnutPath} after install. Check that ${baseUrl}/doughnut-cli-latest/doughnut is served.`
        )
      }
      return doughnutPath
    },
    async runInstalledCli({ doughnutPath, args, env }: RunInstalledCliTask) {
      if (!doughnutPath) {
        throw new Error(
          `runInstalledCli: doughnutPath required, got ${JSON.stringify(doughnutPath)}`
        )
      }
      if (!existsSync(doughnutPath)) {
        throw new Error(
          `runInstalledCli: doughnut binary not found at ${doughnutPath}. Ensure prior step "I install the CLI" succeeded.`
        )
      }
      const cwd = dirname(doughnutPath)
      disposeManagedCliPtySession()
      let managed: ManagedTtySession | undefined
      try {
        managed = await startManagedTtySession(
          {
            command: process.execPath,
            args: [doughnutPath, ...(args ?? [])],
            cwd,
            env: { ...process.env, ...cliEnv(env) },
          },
          CLI_E2E_MANAGED_PTY_GEOMETRY
        )
        await waitForPtyExit(
          managed.session.pty,
          0,
          NON_INTERACTIVE_CLI_EXIT_TIMEOUT_MS
        )
        interactiveCliPtyHandle = managed
      } catch (e) {
        managed?.dispose()
        throw e
      }
      return null
    },
    async runInstalledCliInteractive({
      doughnutPath,
      env,
    }: RunInstalledCliInteractiveTask): Promise<null> {
      if (!doughnutPath) {
        throw new Error(
          `runInstalledCliInteractive: doughnutPath required, got ${JSON.stringify(doughnutPath)}`
        )
      }
      if (!existsSync(doughnutPath)) {
        throw new Error(
          `runInstalledCliInteractive: doughnut binary not found at ${doughnutPath}. Ensure prior install step succeeded.`
        )
      }
      await startInteractiveCliPtySession({
        command: process.execPath,
        args: [doughnutPath],
        cwd: dirname(doughnutPath),
        env,
      })
      return null
    },
    async runRepoCliInteractive({
      env,
    }: RunRepoCliInteractiveTask = {}): Promise<null> {
      const { command, baseArgs } = cliRepoSpawnFromRoot(repoRoot)
      await startInteractiveCliPtySession({
        command,
        args: baseArgs,
        cwd: repoRoot,
        env,
      })
      return null
    },
    cliInteractivePtyEnableGoogleOAuthSimulation() {
      const handle = interactiveCliPtyHandle
      if (!handle) {
        throw new Error(
          'cliInteractivePtyEnableGoogleOAuthSimulation: no active interactive CLI PTY. Start the session first (e.g. runRepoCliInteractive).'
        )
      }
      attachGoogleOAuthSimulation(handle.session)
      return null
    },
    cliInteractivePtyDispose() {
      disposeManagedCliPtySession()
      return null
    },
    async cliAssert(body: ManagedTtyAssertTaskPayload): Promise<null> {
      const handle = interactiveCliPtyHandle
      if (!handle) {
        throw new Error(
          'cliAssert: no managed CLI PTY session. Start interactive (runInstalledCliInteractive / runRepoCliInteractive) or run a one-shot command (runInstalledCli) first.'
        )
      }
      await managedAssertWithTerminalArtifacts(
        handle,
        managedTtyAssertOptionsFromJson(body),
        'cliAssert'
      )
      return null
    },
    async cliInteractiveWriteLine({
      line,
    }: CliInteractiveWriteLineTask): Promise<null> {
      if (!interactiveCliPtyHandle) {
        throw new Error(
          'cliInteractiveWriteLine: no active interactive CLI PTY session. Ensure @interactiveCLI started the session or run the installed CLI in interactive mode first.'
        )
      }
      interactiveCliPtyHandle.submit(line)
      await new Promise<void>((resolve) =>
        setTimeout(resolve, INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS)
      )
      return null
    },
    async cliInteractiveWriteRaw({
      data,
    }: CliInteractiveWriteRawTask): Promise<null> {
      if (!interactiveCliPtyHandle) {
        throw new Error(
          'cliInteractiveWriteRaw: no active interactive CLI PTY session. Ensure @interactiveCLI started the session or run the installed CLI in interactive mode first.'
        )
      }
      interactiveCliPtyHandle.write(data)
      await new Promise<void>((resolve) =>
        setTimeout(resolve, INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS)
      )
      return null
    },
  }
}
