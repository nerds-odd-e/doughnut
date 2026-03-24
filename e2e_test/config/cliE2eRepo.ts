/**
 * Run or bundle the Doughnut CLI from a repo checkout (Node only; no Cypress).
 */

import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { cliEnv } from './cliEnv'

export const CLI_BUNDLE_RELATIVE_PATH = 'cli/dist/doughnut-cli.bundle.mjs'

/** Cypress install scenarios build here so version bumps do not overwrite the default bundle. */
export const CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH =
  'cli/dist/e2e-install-doughnut-cli.bundle.mjs'

const CLI_E2E_INSTALL_BUNDLE_OUTFILE =
  './dist/e2e-install-doughnut-cli.bundle.mjs'

/** Env: set to `1` to spawn `tsx src/index.ts` instead of the bundle (debug only). */
export const CLI_E2E_USE_TSX_ENV = 'DOUGHNUT_CLI_E2E_USE_TSX' as const

const SKIP_DIR_NAMES = new Set(['node_modules', '.git'])

/** Max wall time for one non-interactive `spawnCliFromRepo` run before SIGKILL. */
export const CLI_NON_INTERACTIVE_SPAWN_TIMEOUT_MS = 25_000

/** How Cypress starts the CLI from `repoRoot` (bundle after freshness check, or tsx when opted in). */
export type CliRepoSpawn = {
  command: string
  baseArgs: string[]
}

function maxMtimeMsOfFiles(repoRoot: string, relPaths: string[]): number {
  let max = 0
  for (const rel of relPaths) {
    const p = join(repoRoot, rel)
    if (!existsSync(p)) continue
    const st = statSync(p)
    if (st.mtimeMs > max) max = st.mtimeMs
  }
  return max
}

function maxMtimeMsUnderDir(absDir: string): number {
  let max = 0
  const walk = (dir: string) => {
    let entries
    try {
      entries = readdirSync(dir, { withFileTypes: true })
    } catch {
      return
    }
    for (const e of entries) {
      if (SKIP_DIR_NAMES.has(e.name)) continue
      const full = join(dir, e.name)
      if (e.isDirectory()) {
        walk(full)
      } else if (e.isFile()) {
        try {
          const st = statSync(full)
          if (st.mtimeMs > max) max = st.mtimeMs
        } catch {
          /* skip broken symlinks */
        }
      }
    }
  }
  walk(absDir)
  return max
}

/** Rebuild `cli/dist/doughnut-cli.bundle.mjs` when missing or older than CLI / doughnut-api sources. */
export function ensureCliBundleFresh(repoRoot: string): void {
  const bundlePath = join(repoRoot, CLI_BUNDLE_RELATIVE_PATH)
  let inputMax = maxMtimeMsOfFiles(repoRoot, [
    'cli/package.json',
    'cli/tsconfig.json',
  ])
  for (const rel of ['cli/src', 'packages/doughnut-api/src'] as const) {
    const d = join(repoRoot, rel)
    if (existsSync(d)) {
      inputMax = Math.max(inputMax, maxMtimeMsUnderDir(d))
    }
  }
  const stale =
    !existsSync(bundlePath) || statSync(bundlePath).mtimeMs < inputMax
  if (stale) {
    runShellCommandSync('pnpm -C cli bundle', { cwd: repoRoot })
  }
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

export function bundleCliE2eInstall(repoRoot: string, env?: NodeJS.ProcessEnv) {
  runShellCommandSync('pnpm -C cli bundle', {
    cwd: repoRoot,
    env: {
      ...process.env,
      ...env,
      CLI_BUNDLE_OUTFILE: CLI_E2E_INSTALL_BUNDLE_OUTFILE,
    },
  })
  const bundle = join(repoRoot, CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH)
  if (!existsSync(bundle)) {
    throw new Error(
      `Missing E2E install CLI bundle at ${bundle} after pnpm -C cli bundle`
    )
  }
}

export function cliRepoSpawnFromRoot(repoRoot: string): CliRepoSpawn {
  if (process.env[CLI_E2E_USE_TSX_ENV] === '1') {
    return {
      command: 'pnpm',
      baseArgs: ['-C', join(repoRoot, 'cli'), 'exec', 'tsx', 'src/index.ts'],
    }
  }
  ensureCliBundleFresh(repoRoot)
  return {
    command: process.execPath,
    baseArgs: [join(repoRoot, CLI_BUNDLE_RELATIVE_PATH)],
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
