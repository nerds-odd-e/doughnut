#!/usr/bin/env node
/**
 * Stop SUT listeners on 5173 / 5174 / 9081 (not mountebank), then run `pnpm sut`.
 * Run: `CURSOR_DEV=true nix develop -c pnpm sut:restart`
 */
import { execFile, spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

/** Ports used by `pnpm sut` except mountebank (2525). See docs/gcp/prod_env.md */
export const SUT_RESTART_PORTS = [5173, 5174, 9081]

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export function parsePidsFromLsofStdout(stdout) {
  const lines = String(stdout)
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
  return [
    ...new Set(lines.map(Number).filter((n) => Number.isInteger(n) && n > 0)),
  ]
}

/**
 * @param {number} port
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number[]>}
 */
export function getListenerPids(port, { execFileFn = execFile } = {}) {
  return new Promise((resolve, reject) => {
    execFileFn(
      'lsof',
      ['-nP', `-iTCP:${port}`, '-sTCP:LISTEN', '-t'],
      (err, stdout) => {
        if (err) {
          if (err.code === 1) {
            resolve(parsePidsFromLsofStdout(stdout || ''))
            return
          }
          if (err.code === 'ENOENT') {
            reject(
              new Error(
                'lsof not found; use `CURSOR_DEV=true nix develop` or install lsof.'
              )
            )
            return
          }
          reject(err)
          return
        }
        resolve(parsePidsFromLsofStdout(stdout || ''))
      }
    )
  })
}

/**
 * @param {number} port
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number[]>} PIDs signalled (may include already-dead)
 */
export async function terminateTcpListenersOnPort(port, deps) {
  const pids = await getListenerPids(port, deps)
  for (const pid of pids) {
    try {
      process.kill(pid, 'SIGTERM')
    } catch (e) {
      if (e.code !== 'ESRCH') throw e
    }
  }
  return pids
}

/**
 * @param {{ log?: (s: string) => void, ports?: number[], execFileFn?: typeof execFile, spawnFn?: typeof spawn }} [opts]
 */
export async function stopSutPorts({
  log = console.log,
  ports = SUT_RESTART_PORTS,
  execFileFn = execFile,
} = {}) {
  for (const port of ports) {
    const pids = await getListenerPids(port, { execFileFn })
    if (pids.length === 0) {
      log(`port ${port}: no TCP listener`)
    } else {
      log(`port ${port}: sending SIGTERM to PID(s) ${pids.join(', ')}`)
      for (const pid of pids) {
        try {
          process.kill(pid, 'SIGTERM')
        } catch (e) {
          if (e.code !== 'ESRCH') throw e
        }
      }
    }
  }
}

/**
 * @param {{ cwd?: string, spawnFn?: typeof spawn }} [opts]
 * @returns {Promise<number>} exit code of `pnpm sut`
 */
export function startPnpmSut({ cwd = repoRoot, spawnFn = spawn } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawnFn('pnpm', ['sut'], {
      cwd,
      stdio: 'inherit',
      shell: false,
    })
    child.on('error', (e) => {
      reject(
        new Error(
          e.code === 'ENOENT'
            ? 'pnpm not found on PATH; use the repo dev shell or install pnpm.'
            : e.message
        )
      )
    })
    child.on('close', (code, signal) => {
      if (signal) {
        resolve(1)
        return
      }
      resolve(code ?? 1)
    })
  })
}

export async function runSutRestart(opts = {}) {
  await stopSutPorts(opts)
  return startPnpmSut({ cwd: opts.cwd ?? repoRoot, spawnFn: opts.spawnFn })
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runSutRestart()
    process.exit(code)
  } catch (e) {
    console.error(e instanceof Error ? e.message : e)
    process.exit(1)
  }
}
