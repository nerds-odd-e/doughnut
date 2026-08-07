/**
 * Cypress `task` handlers for CLI E2E. Depends only on `repoRoot` (repo checkout path).
 */

import { existsSync, mkdtempSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { delimiter, dirname, join } from 'node:path'
import { attachGoogleOAuthSimulation } from './cliE2eGoogleOAuthSimulation'
import {
  CLI_E2E_MANAGED_PTY_GEOMETRY,
  NON_INTERACTIVE_CLI_EXIT_TIMEOUT_MS,
  createCliE2eManagedPty,
  waitForPtyExit,
} from './cliE2eManagedPty'
import { createCliE2ePluginConfigDirTasks } from './cliE2ePluginConfigDirTasks'
import {
  bundleCliE2eInstall,
  CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH,
  cliRepoSpawnFromRoot,
  runShellCommandSync,
} from './cliE2eRepo'
import { cliEnv } from './cliEnv'
import { startManagedTtySession } from 'tty-assert'
import type { ManagedTtyAssertInput, ManagedTtySession } from 'tty-assert'

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
  const pty = createCliE2eManagedPty({
    repoRoot,
    saveBufferToCurrentSpecFolder: options.saveBufferToCurrentSpecFolder,
  })

  return {
    ...createCliE2ePluginConfigDirTasks(),
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
      pty.dispose()
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
      pty.dispose()
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
        pty.setHandle(managed)
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
      await pty.startInteractive({
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
      await pty.startInteractive({
        command,
        args: baseArgs,
        cwd: repoRoot,
        env,
      })
      return null
    },
    cliInteractivePtyEnableGoogleOAuthSimulation() {
      const handle = pty.getHandle()
      if (!handle) {
        throw new Error(
          'cliInteractivePtyEnableGoogleOAuthSimulation: no active interactive CLI PTY. Start the session first (e.g. runRepoCliInteractive).'
        )
      }
      attachGoogleOAuthSimulation(handle.session)
      return null
    },
    cliInteractivePtyDispose() {
      pty.dispose()
      return null
    },
    async cliAssert(body: ManagedTtyAssertInput): Promise<null> {
      await pty.assert(body)
      return null
    },
    async cliInteractiveWriteLine({
      line,
    }: CliInteractiveWriteLineTask): Promise<null> {
      await pty.writeLine(line)
      return null
    },
    async cliInteractiveWriteRaw({
      data,
    }: CliInteractiveWriteRawTask): Promise<null> {
      await pty.writeRaw(data)
      return null
    },
  }
}
