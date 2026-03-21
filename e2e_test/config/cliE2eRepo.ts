/**
 * Run or bundle the Doughnut CLI from a repo checkout (Node only; no Cypress).
 */

import { spawnSync } from 'node:child_process'
import { copyFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { cliEnv } from './cliEnv'

export const CLI_BUNDLE_RELATIVE_PATH = 'cli/dist/doughnut-cli.bundle.mjs'

/** Max wall time for one non-interactive `spawnCliFromRepo` run before SIGKILL. */
export const CLI_NON_INTERACTIVE_SPAWN_TIMEOUT_MS = 25_000

/** How Cypress starts `pnpm … tsx` or the bundled `.mjs` from `repoRoot`. */
export type CliRepoSpawn = {
  command: string
  baseArgs: string[]
}

export function runShellCommandSync(
  cmd: string,
  opts: { cwd?: string; env?: NodeJS.ProcessEnv } = {}
) {
  const result = spawnSync(cmd, [], {
    ...opts,
    shell: true,
    stdio: 'pipe',
    encoding: 'utf8',
  })
  if (result.stdout) console.log(result.stdout)
  if (result.stderr) console.error(result.stderr)
  if (result.status !== 0) {
    throw new Error(`Command failed with exit code ${result.status}`)
  }
}

export function bundleCliIntoBackendStatic(
  repoRoot: string,
  env?: NodeJS.ProcessEnv
) {
  runShellCommandSync('pnpm cli:bundle', { cwd: repoRoot, env })
  const src = join(repoRoot, CLI_BUNDLE_RELATIVE_PATH)
  const destDir = join(
    repoRoot,
    'backend/build/resources/main/static/doughnut-cli-latest'
  )
  mkdirSync(destDir, { recursive: true })
  copyFileSync(src, join(destDir, 'doughnut'))
}

export function cliRepoSpawnFromRoot(repoRoot: string): CliRepoSpawn {
  if (process.env.CI === '1') {
    return {
      command: process.execPath,
      baseArgs: [join(repoRoot, CLI_BUNDLE_RELATIVE_PATH)],
    }
  }
  return {
    command: 'pnpm',
    baseArgs: ['-C', join(repoRoot, 'cli'), 'exec', 'tsx', 'src/index.ts'],
  }
}

export type SpawnCliFromRepoOptions = {
  repoRoot: string
  args?: string[]
  stdin?: string
  env?: NodeJS.ProcessEnv
  simulateOAuthCallback?: boolean
  timeoutMs?: number
}

export async function spawnCliFromRepo(
  opts: SpawnCliFromRepoOptions
): Promise<string> {
  const { spawn } = await import('node:child_process')
  const config = cliRepoSpawnFromRoot(opts.repoRoot)
  const args = [...config.baseArgs, ...(opts.args ?? [])]
  return new Promise<string>((resolve, reject) => {
    const proc = spawn(config.command, args, {
      cwd: opts.repoRoot,
      env: { ...process.env, ...cliEnv(opts.env) },
      stdio: ['pipe', 'pipe', 'pipe'],
    })
    let stdout = ''
    const append = (chunk: string) => {
      stdout += chunk
      return stdout
    }
    proc.stdout?.on('data', (chunk: Buffer) => {
      const out = append(chunk.toString())
      if (opts.simulateOAuthCallback) {
        const authMatch = out.match(/https:\/\/accounts\.google\.com\/[^\s]+/)
        if (authMatch) {
          const redirectUri = new URL(authMatch[0]).searchParams.get(
            'redirect_uri'
          )
          if (redirectUri) {
            fetch(`${redirectUri}?code=e2e_mock_auth_code`).catch(() => {
              /* ignore OAuth callback errors */
            })
          }
        }
      }
    })
    if (opts.simulateOAuthCallback) {
      proc.stderr?.on('data', (chunk: Buffer) => append(chunk.toString()))
    }
    const timeout =
      opts.timeoutMs &&
      setTimeout(() => {
        proc.kill('SIGKILL')
        reject(
          new Error(
            `CLI timed out after ${opts.timeoutMs}ms. stdout tail: ${stdout.slice(-300)}`
          )
        )
      }, opts.timeoutMs)
    if (opts.stdin !== undefined) {
      proc.stdin?.write(
        opts.stdin.endsWith('\n') ? opts.stdin : `${opts.stdin}\n`
      )
      proc.stdin?.end()
    } else {
      proc.stdin?.end()
    }
    proc.on('close', (code) => {
      if (timeout) clearTimeout(timeout)
      if (code === 0) resolve(stdout)
      else reject(new Error(`CLI exited with code ${code}`))
    })
    proc.on('error', (err) => {
      if (timeout) clearTimeout(timeout)
      reject(err)
    })
  })
}
