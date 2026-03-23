/**
 * Cypress `task` handlers for CLI E2E. Depends only on `repoRoot` (repo checkout path).
 */

import { existsSync, mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import {
  bundleCli,
  CLI_NON_INTERACTIVE_SPAWN_TIMEOUT_MS,
  cliRepoSpawnFromRoot,
  runShellCommandSync,
  spawnCliFromRepo,
} from './cliE2eRepo'
import { cliEnv } from './cliEnv'
import {
  applyInteractiveCliPtyKeystroke,
  runCliInPty,
  startInteractiveCli as startInteractiveCliPtySession,
  stopInteractiveCli as stopInteractiveCliPtySession,
} from './cliPtyRunner'
import {
  INTERACTIVE_CLI_PTY_KEYSTROKE_TASK,
  type InteractiveCliPtyKeystroke,
} from './interactiveCliPtyTypes'

type WithOptionalCliEnv = { env?: NodeJS.ProcessEnv }

type RunCliDirectWithInputTask = WithOptionalCliEnv & {
  input: string
  simulateOAuthCallback?: boolean
}

type RunCliDirectWithArgsTask = WithOptionalCliEnv & {
  args: string[]
}

type RunInstalledCliTask = WithOptionalCliEnv & {
  doughnutPath: string
  input?: string
  args?: string[]
}

async function bundleCliOrThrow(
  repoRoot: string,
  env?: NodeJS.ProcessEnv
): Promise<true> {
  try {
    bundleCli(repoRoot, env)
    return true
  } catch (error) {
    console.error('Failed to bundle CLI:', error)
    throw error
  }
}

export function createCliE2ePluginTasks(repoRoot: string) {
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
    async bundleCli() {
      return bundleCliOrThrow(repoRoot)
    },
    async bundleCliWithVersion(version: string) {
      return bundleCliOrThrow(repoRoot, {
        ...process.env,
        CLI_VERSION: version,
      })
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
    async runCliDirectWithInput({
      input,
      env,
      simulateOAuthCallback,
    }: RunCliDirectWithInputTask) {
      return spawnCliFromRepo({
        repoRoot,
        stdin: input,
        env,
        simulateOAuthCallback,
        timeoutMs: CLI_NON_INTERACTIVE_SPAWN_TIMEOUT_MS,
      })
    },
    async startInteractiveCli({ env }: WithOptionalCliEnv) {
      const { command, baseArgs } = cliRepoSpawnFromRoot(repoRoot)
      await startInteractiveCliPtySession({
        command,
        args: baseArgs,
        cwd: repoRoot,
        env: { ...cliEnv(env) },
      })
      return true
    },
    [INTERACTIVE_CLI_PTY_KEYSTROKE_TASK]: (
      keystroke: InteractiveCliPtyKeystroke
    ) => applyInteractiveCliPtyKeystroke(keystroke),
    async stopInteractiveCli() {
      await stopInteractiveCliPtySession()
      return null
    },
    async runCliDirectWithArgs({ args, env }: RunCliDirectWithArgsTask) {
      return spawnCliFromRepo({ repoRoot, args, env })
    },
    async runInstalledCli({
      doughnutPath,
      input,
      args,
      env,
    }: RunInstalledCliTask) {
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
