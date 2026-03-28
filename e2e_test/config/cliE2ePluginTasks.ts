/**
 * Cypress `task` handlers for CLI E2E. Depends only on `repoRoot` (repo checkout path).
 */

import { existsSync, mkdtempSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import {
  bundleCliE2eInstall,
  CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH,
  runShellCommandSync,
} from './cliE2eRepo'
import { cliEnv } from './cliEnv'
import { stripAnsiCliPty } from './cliPtyAnsi'

type WithOptionalCliEnv = { env?: NodeJS.ProcessEnv }

type RunInstalledCliTask = WithOptionalCliEnv & {
  doughnutPath: string
  args?: string[]
}

type RunInstalledCliInteractiveTask = WithOptionalCliEnv & {
  doughnutPath: string
}

type CliInteractiveWriteLineTask = {
  line: string
}

const INSTALLED_CLI_INTERACTIVE_STARTUP_SUBSTRING = 'doughnut 0.1.0'
const INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS = 20_000
const INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS = 500

const PREVIEW_LEN = 500

type CliInteractivePtySession = {
  buf: { text: string }
  pty: { write(data: string): void; kill(): void }
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
            stripped.length > PREVIEW_LEN
              ? `${stripped.slice(0, PREVIEW_LEN)}...`
              : stripped
          reject(
            new Error(
              `Timeout after ${timeoutMs}ms waiting for substring ${JSON.stringify(needle)} in installed CLI interactive PTY output. Preview (ANSI-stripped):\n${preview}`
            )
          )
          return
        }
        setTimeout(tick, 50)
      }
      tick()
    })
  }

  return {
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
      disposeInteractiveCliPtySession()

      const cwd = dirname(doughnutPath)
      const { spawn } = await import('@lydell/node-pty')
      const p = spawn(process.execPath, [doughnutPath], {
        name: 'xterm-256color',
        cols: 120,
        rows: 32,
        cwd,
        env: { ...process.env, ...cliEnv(env) },
      })
      const buf = { text: '' }
      p.onData((data: string) => {
        buf.text += data
      })
      interactiveCliPtySession = { pty: p, buf }
      await installedCliInteractiveWaitForSubstring(
        () => buf.text,
        INSTALLED_CLI_INTERACTIVE_STARTUP_SUBSTRING,
        INSTALLED_CLI_INTERACTIVE_STARTUP_TIMEOUT_MS
      )
      return null
    },
    cliInteractivePtyGetBuffer(): string {
      if (!interactiveCliPtySession) {
        throw new Error(
          'cliInteractivePtyGetBuffer: no active interactive CLI PTY session. Run installation interactive mode first.'
        )
      }
      return interactiveCliPtySession.buf.text
    },
    async cliInteractiveWriteLine({
      line,
    }: CliInteractiveWriteLineTask): Promise<null> {
      if (!interactiveCliPtySession) {
        throw new Error(
          'cliInteractiveWriteLine: no active interactive CLI PTY session. Run installation interactive mode first.'
        )
      }
      const { pty } = interactiveCliPtySession
      pty.write(`${line}\r`)
      await new Promise<void>((resolve) =>
        setTimeout(resolve, INSTALLED_CLI_INTERACTIVE_WRITE_SETTLE_MS)
      )
      return null
    },
  }
}
