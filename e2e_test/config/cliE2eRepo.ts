/**
 * Run or bundle the Doughnut CLI from a repo checkout (Node only; no Cypress).
 */

import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'
import {
  GMAIL_E2E_GOOGLE_CLIENT_ID,
  GMAIL_E2E_GOOGLE_CLIENT_SECRET,
} from './cliGmailE2eConfig'

export const CLI_BUNDLE_RELATIVE_PATH = 'cli/dist/doughnut-cli.bundle.mjs'

function envForDefaultE2eCliBundle(): NodeJS.ProcessEnv {
  return {
    ...process.env,
    GOOGLE_CLIENT_ID: GMAIL_E2E_GOOGLE_CLIENT_ID,
    GOOGLE_CLIENT_SECRET: GMAIL_E2E_GOOGLE_CLIENT_SECRET,
  }
}

function defaultCliBundleHasGmailE2eClientId(repoRoot: string): boolean {
  const bundlePath = join(repoRoot, CLI_BUNDLE_RELATIVE_PATH)
  try {
    return readFileSync(bundlePath, 'utf8').includes(GMAIL_E2E_GOOGLE_CLIENT_ID)
  } catch {
    return false
  }
}

/** Cypress install scenarios build here so version bumps do not overwrite the default bundle. */
export const CLI_E2E_INSTALL_BUNDLE_RELATIVE_PATH =
  'cli/dist/e2e-install-doughnut-cli.bundle.mjs'

const CLI_E2E_INSTALL_BUNDLE_OUTFILE =
  './dist/e2e-install-doughnut-cli.bundle.mjs'

/** Env: set to `1` to spawn `tsx src/index.ts` instead of the bundle (debug only). */
export const CLI_E2E_USE_TSX_ENV = 'DOUGHNUT_CLI_E2E_USE_TSX' as const

const SKIP_DIR_NAMES = new Set(['node_modules', '.git'])

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
    'e2e_test/config/cliGmailE2eConfig.ts',
  ])
  for (const rel of ['cli/src', 'packages/doughnut-api/src'] as const) {
    const d = join(repoRoot, rel)
    if (existsSync(d)) {
      inputMax = Math.max(inputMax, maxMtimeMsUnderDir(d))
    }
  }
  const stale =
    !existsSync(bundlePath) ||
    statSync(bundlePath).mtimeMs < inputMax ||
    !defaultCliBundleHasGmailE2eClientId(repoRoot)
  if (stale) {
    runShellCommandSync('pnpm -C cli bundle', {
      cwd: repoRoot,
      env: envForDefaultE2eCliBundle(),
    })
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
