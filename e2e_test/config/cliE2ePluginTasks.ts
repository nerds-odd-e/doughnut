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

type WithOptionalCliEnv = { env?: NodeJS.ProcessEnv }

type RunInstalledCliTask = WithOptionalCliEnv & {
  doughnutPath: string
  args?: string[]
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
  }
}
