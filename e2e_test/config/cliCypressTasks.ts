/**
 * Cypress `task` handlers for CLI E2E. Depends on repo root only; no Cypress types required for the implementations.
 */

import { existsSync, mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import {
  bundleAndCopyCliToBuildOutput,
  CLI_SPAWN_TIMEOUT_MS,
  getCliRunConfig,
  runSync,
  spawnCliFromRepo,
} from './cliNode'
import { cliEnv } from './cliEnv'
import {
  interactiveSendEnter,
  interactiveSendEsc,
  interactiveSendLine,
  interactiveSendSlashCommand,
  runCliInPty,
  sendToInteractiveCli as sendToInteractiveCliLegacy,
  startInteractiveCli as startInteractiveCliProcess,
  stopInteractiveCli as stopInteractiveCliProcess,
} from './cliPtyRunner'

export function createCliPluginTasks(repoRoot: string) {
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
    async bundleAndCopyCli() {
      try {
        bundleAndCopyCliToBuildOutput(repoRoot)
        return true
      } catch (error) {
        console.error('Failed to bundle and copy CLI:', error)
        throw error
      }
    },
    async bundleAndCopyCliWithVersion(version: string) {
      try {
        bundleAndCopyCliToBuildOutput(repoRoot, {
          ...process.env,
          CLI_VERSION: version,
        })
        return true
      } catch (error) {
        console.error('Failed to bundle and copy CLI:', error)
        throw error
      }
    },
    async installCli(baseUrl: string) {
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
      runSync(`bash ${installScriptPath}`, {
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
    async runCliDirectWithInput({
      input,
      env,
      simulateOAuthCallback,
    }: {
      input: string
      env?: NodeJS.ProcessEnv
      simulateOAuthCallback?: boolean
    }) {
      return spawnCliFromRepo({
        repoRoot,
        stdin: input,
        env,
        simulateOAuthCallback,
        timeoutMs: CLI_SPAWN_TIMEOUT_MS,
      })
    },
    async startInteractiveCli({ env }: { env?: NodeJS.ProcessEnv }) {
      const config = getCliRunConfig(repoRoot)
      await startInteractiveCliProcess({
        command: config.command,
        args: config.baseArgs,
        cwd: repoRoot,
        env: { ...cliEnv(env) },
      })
      return true
    },
    async sendToInteractiveCli({ input }: { input: string }) {
      return sendToInteractiveCliLegacy(input)
    },
    async sendInteractiveCliSlashCommand({ command }: { command: string }) {
      return interactiveSendSlashCommand(command)
    },
    async sendInteractiveCliLine({ line }: { line: string }) {
      return interactiveSendLine(line)
    },
    async sendInteractiveCliEnter() {
      return interactiveSendEnter()
    },
    async sendInteractiveCliEsc() {
      return interactiveSendEsc()
    },
    async stopInteractiveCli() {
      await stopInteractiveCliProcess()
      return null
    },
    async runCliDirectWithArgs({
      args,
      env,
    }: {
      args: string[]
      env?: NodeJS.ProcessEnv
    }) {
      return spawnCliFromRepo({ repoRoot, args, env })
    },
    async runInstalledCli({
      doughnutPath,
      input,
      args,
      env,
    }: {
      doughnutPath: string
      input?: string
      args?: string[]
      env?: NodeJS.ProcessEnv
    }) {
      if (!doughnutPath || typeof doughnutPath !== 'string') {
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
      if (input !== undefined) {
        return runCliInPty({
          executablePath: doughnutPath,
          args: args ?? [],
          input,
          cwd,
          env,
        })
      }
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
  }
}
