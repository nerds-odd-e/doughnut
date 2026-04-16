#!/usr/bin/env node
/**
 * Start SUT services in the background, wait for health, then exit.
 *
 * Exit 0: all services healthy.
 * Exit 1: timeout or early process exit — diagnostics on stderr, log path printed.
 *
 * Topology reference: docs/gcp/prod_env.md (Local dev / Cypress).
 * Log file: sut.log (repo root, gitignored).
 * PID file: sut.pid (repo root, gitignored) — stores the process group ID so
 *           `pnpm sut:restart` can find and stop the group.
 *
 * Env:
 *   SUT_TIMEOUT_MS  – max ms to wait for healthy (default: 120000)
 *   SUT_POLL_MS     – ms between healthcheck polls (default: 3000)
 */
import { openSync } from 'node:fs'
import { readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { runSutHealthcheck } from './sut-healthcheck.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export const LOG_FILE = path.join(repoRoot, 'sut.log')
export const PID_FILE = path.join(repoRoot, 'sut.pid')

const TIMEOUT_MS = Number(process.env.SUT_TIMEOUT_MS ?? 120_000)
const POLL_MS = Number(process.env.SUT_POLL_MS ?? 3_000)

/** Number of log tail lines to include in failure output. */
const TAIL_LINES = 40

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** Read last N lines of a file. Returns empty string if the file cannot be read. */
async function tailFile(filePath, lines) {
  try {
    const content = await readFile(filePath, 'utf8')
    const all = content.split('\n')
    return all.slice(-lines).join('\n')
  } catch {
    return ''
  }
}

/**
 * Spawn the run-p service group detached, redirecting stdout+stderr to the log
 * file. Returns the child process handle (already unref'd).
 *
 * @param {{ spawnFn?: typeof spawn, logFile?: string }} [opts]
 * @returns {{ child: import('node:child_process').ChildProcess, logFile: string }}
 */
export function spawnSutServices({
  spawnFn = spawn,
  logFile = LOG_FILE,
} = {}) {
  const logFd = openSync(logFile, 'a')
  const child = spawnFn(
    'pnpm',
    ['exec', 'run-p', '-clnr', 'backend:sut', 'start:mb', 'local:lb:vite', 'frontend:sut'],
    {
      cwd: repoRoot,
      detached: true,
      stdio: ['ignore', logFd, logFd],
      shell: false,
    }
  )
  child.unref()
  return { child, logFile }
}

/**
 * Write the process group id (negative of PID) to the PID file so external
 * tools can kill the whole group with `process.kill(-pgid, 'SIGTERM')`.
 *
 * @param {number} pid
 * @param {{ pidFile?: string }} [opts]
 */
export async function writePidFile(pid, { pidFile = PID_FILE } = {}) {
  await writeFile(pidFile, String(pid), 'utf8')
}

/**
 * Poll the healthcheck until success, timeout, or early child exit.
 *
 * @param {{
 *   child: import('node:child_process').ChildProcess,
 *   timeoutMs?: number,
 *   pollMs?: number,
 *   logFile?: string,
 *   log?: (s: string) => void,
 *   errLog?: (s: string) => void,
 *   healthcheckFn?: typeof runSutHealthcheck,
 * }} opts
 * @returns {Promise<{ ok: boolean, exitCode: number }>}
 */
export async function waitForSutHealthy({
  child,
  timeoutMs = TIMEOUT_MS,
  pollMs = POLL_MS,
  logFile = LOG_FILE,
  log = (s) => process.stdout.write(s + '\n'),
  errLog = (s) => process.stderr.write(s + '\n'),
  healthcheckFn = runSutHealthcheck,
} = {}) {
  let childExitCode = null
  let childSignal = null

  child.once('exit', (code, signal) => {
    childExitCode = code ?? 1
    childSignal = signal
  })

  const deadline = Date.now() + timeoutMs
  let attempt = 0

  while (Date.now() < deadline) {
    // Check if the child exited prematurely
    if (childExitCode !== null || childSignal !== null) {
      const reason = childSignal
        ? `killed by signal ${childSignal}`
        : `exited with code ${childExitCode}`
      errLog(`SUT service process ${reason} before becoming healthy.`)
      errLog(`Log: ${logFile}`)
      const tail = await tailFile(logFile, TAIL_LINES)
      if (tail) {
        errLog(`--- last lines of ${path.basename(logFile)} ---`)
        errLog(tail)
        errLog('---')
      }
      return { ok: false, exitCode: 1 }
    }

    attempt++
    // Run healthcheck silently on every poll; only log progress dots or attempt number
    const result = await healthcheckFn({ log: () => {} })
    if (result.ok) {
      log(`SUT healthy after ${attempt} poll(s). Services running in background.`)
      log(`Log: ${logFile}`)
      return { ok: true, exitCode: 0 }
    }

    if (attempt === 1) {
      log(`Waiting for SUT to become healthy (timeout: ${timeoutMs / 1000}s)...`)
    }

    const remaining = deadline - Date.now()
    if (remaining <= 0) break
    await sleep(Math.min(pollMs, remaining))
  }

  // Timed out — run one final healthcheck with full logging to show what failed
  errLog('SUT did not become healthy within the timeout.')
  await healthcheckFn({ log: errLog })
  errLog(`Log: ${logFile}`)
  const tail = await tailFile(logFile, TAIL_LINES)
  if (tail) {
    errLog(`--- last lines of ${path.basename(logFile)} ---`)
    errLog(tail)
    errLog('---')
  }
  return { ok: false, exitCode: 1 }
}

/**
 * Full start flow: spawn services, write PID file, wait for health.
 *
 * @param {{
 *   spawnFn?: typeof spawn,
 *   logFile?: string,
 *   pidFile?: string,
 *   timeoutMs?: number,
 *   pollMs?: number,
 *   log?: (s: string) => void,
 *   errLog?: (s: string) => void,
 *   healthcheckFn?: typeof runSutHealthcheck,
 * }} [opts]
 * @returns {Promise<number>} exit code (0 = healthy, 1 = failed)
 */
export async function runSutStart({
  spawnFn = spawn,
  logFile = LOG_FILE,
  pidFile = PID_FILE,
  timeoutMs = TIMEOUT_MS,
  pollMs = POLL_MS,
  log = (s) => process.stdout.write(s + '\n'),
  errLog = (s) => process.stderr.write(s + '\n'),
  healthcheckFn = runSutHealthcheck,
} = {}) {
  log(`Starting SUT services... (log: ${logFile})`)
  const { child } = spawnSutServices({ spawnFn, logFile })
  await writePidFile(child.pid, { pidFile })

  const { exitCode } = await waitForSutHealthy({
    child,
    timeoutMs,
    pollMs,
    logFile,
    log,
    errLog,
    healthcheckFn,
  })
  return exitCode
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  try {
    const code = await runSutStart()
    process.exit(code)
  } catch (e) {
    process.stderr.write((e instanceof Error ? e.message : String(e)) + '\n')
    process.exit(1)
  }
}
