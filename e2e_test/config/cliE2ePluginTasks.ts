/**
 * Cypress `task` handlers for CLI E2E. Depends only on `repoRoot` (repo checkout path).
 */

import type { IPty } from '@lydell/node-pty'
import { existsSync, mkdtempSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { attachGoogleOAuthSimulation } from './cliE2eGoogleOAuthSimulation'
import {
  bundleCliE2eInstall,
  CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH,
  cliRepoSpawnFromRoot,
  runShellCommandSync,
} from './cliE2eRepo'
import { cliEnv } from './cliEnv'
import {
  CLI_INTERACTIVE_PTY_COLS,
  CLI_INTERACTIVE_PTY_ROWS,
} from './tty-assert-staging/geometry'
import { TERMINAL_ERROR_PREVIEW_LEN } from './tty-assert-staging/errorSnapshotFormatting'
import { stripAnsiCliPty } from './tty-assert-staging/stripAnsi'

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

const INSTALLED_CLI_INTERACTIVE_STARTUP_SUBSTRING = 'doughnut 0.1.0'
const INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS = 20_000
const INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS = 500

type CliInteractivePtySession = {
  buf: { text: string }
  pty: IPty
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

export function createCliE2ePluginTasks(repoRoot: string) {
  let interactiveCliPtySession: CliInteractivePtySession | null = null

  function disposeInteractiveCliPtySession(): void {
    if (!interactiveCliPtySession) return
    try {
      interactiveCliPtySession.pty.kill()
    } catch {
      /* already exited */
    }
    interactiveCliPtySession = null
  }

  function installedCliInteractiveWaitForSubstring(
    getRawOutput: () => string,
    needle: string,
    timeoutMs: number
  ): Promise<void> {
    const started = Date.now()
    return new Promise((resolve, reject) => {
      const tick = () => {
        const stripped = stripAnsiCliPty(getRawOutput())
        if (stripped.includes(needle)) {
          resolve()
          return
        }
        if (Date.now() - started >= timeoutMs) {
          const preview =
            stripped.length > TERMINAL_ERROR_PREVIEW_LEN
              ? `${stripped.slice(0, TERMINAL_ERROR_PREVIEW_LEN)}...`
              : stripped
          reject(
            new Error(
              `Timeout after ${timeoutMs}ms waiting for substring ${JSON.stringify(needle)} in interactive CLI PTY output. Preview (ANSI-stripped):\n${preview}`
            )
          )
          return
        }
        setTimeout(tick, 50)
      }
      tick()
    })
  }

  async function startInteractiveCliPtySession(opts: {
    command: string
    args: string[]
    cwd: string
    env?: NodeJS.ProcessEnv
  }): Promise<void> {
    disposeInteractiveCliPtySession()
    const { spawn } = await import('@lydell/node-pty')
    const p = spawn(opts.command, opts.args, {
      name: 'xterm-256color',
      cols: CLI_INTERACTIVE_PTY_COLS,
      rows: CLI_INTERACTIVE_PTY_ROWS,
      cwd: opts.cwd,
      env: { ...process.env, ...cliEnv(opts.env) },
    })
    const buf = { text: '' }
    const session: CliInteractivePtySession = { pty: p, buf }
    p.onData((data: string) => {
      buf.text += data
    })
    interactiveCliPtySession = session
    await installedCliInteractiveWaitForSubstring(
      () => buf.text,
      INSTALLED_CLI_INTERACTIVE_STARTUP_SUBSTRING,
      INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS
    )
  }

  return {
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
      disposeInteractiveCliPtySession()
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
      const { spawn } = await import('node:child_process')
      return new Promise<string>((resolve, reject) => {
        const proc = spawn(process.execPath, [doughnutPath, ...(args ?? [])], {
          cwd,
          env: { ...process.env, ...cliEnv(env) },
          stdio: ['pipe', 'pipe', 'pipe'],
        })
        let stdout = ''
        proc.stdout?.on('data', (chunk: Buffer) => {
          stdout += chunk.toString()
        })
        proc.stdin?.end()
        proc.on('close', (code) => {
          if (code === 0) resolve(stdout)
          else reject(new Error(`CLI exited with code ${code}`))
        })
        proc.on('error', reject)
      })
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
      const session = interactiveCliPtySession
      if (!session) {
        throw new Error(
          'cliInteractivePtyEnableGoogleOAuthSimulation: no active interactive CLI PTY. Start the session first (e.g. runRepoCliInteractive).'
        )
      }
      attachGoogleOAuthSimulation(session)
      return null
    },
    cliInteractivePtyDispose() {
      disposeInteractiveCliPtySession()
      return null
    },
    cliInteractivePtyGetBuffer(): string {
      if (!interactiveCliPtySession) {
        throw new Error(
          'cliInteractivePtyGetBuffer: no active interactive CLI PTY session. Ensure @interactiveCLI started the session or run the installed CLI in interactive mode first.'
        )
      }
      return interactiveCliPtySession.buf.text
    },
    async cliInteractiveWriteLine({
      line,
    }: CliInteractiveWriteLineTask): Promise<null> {
      if (!interactiveCliPtySession) {
        throw new Error(
          'cliInteractiveWriteLine: no active interactive CLI PTY session. Ensure @interactiveCLI started the session or run the installed CLI in interactive mode first.'
        )
      }
      const { pty } = interactiveCliPtySession
      pty.write(`${line}\r`)
      await new Promise<void>((resolve) =>
        setTimeout(resolve, INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS)
      )
      return null
    },
    async cliInteractiveWriteRaw({
      data,
    }: CliInteractiveWriteRawTask): Promise<null> {
      if (!interactiveCliPtySession) {
        throw new Error(
          'cliInteractiveWriteRaw: no active interactive CLI PTY session. Ensure @interactiveCLI started the session or run the installed CLI in interactive mode first.'
        )
      }
      const { pty } = interactiveCliPtySession
      pty.write(data)
      await new Promise<void>((resolve) =>
        setTimeout(resolve, INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS)
      )
      return null
    },
  }
}
