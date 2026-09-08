import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { LOG_TARGETS } from './log-utils.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'

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
 *   runtimeTarget?: object,
 *   checkoutRoot?: string,
 * }} opts
 * @returns {Promise<{ ok: boolean, exitCode: number }>}
 */
export async function waitForSutHealthy({
  child,
  timeoutMs = TIMEOUT_MS,
  pollMs = POLL_MS,
  logFile = LOG_TARGETS.sut,
  log = (s) => process.stdout.write(`${s}\n`),
  errLog = (s) => process.stderr.write(`${s}\n`),
  healthcheckFn = runSutHealthcheck,
  runtimeTarget,
  checkoutRoot,
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
    const result = await healthcheckFn({
      log: () => undefined,
      runtimeTarget,
      checkoutRoot,
    })
    if (result.ok) {
      log(
        `SUT healthy after ${attempt} poll(s). Services running in background.`
      )
      log(`Log: ${logFile}`)
      return { ok: true, exitCode: 0 }
    }

    if (attempt === 1) {
      log(
        `Waiting for SUT to become healthy (timeout: ${timeoutMs / 1000}s)...`
      )
    }

    const remaining = deadline - Date.now()
    if (remaining <= 0) break
    await sleep(Math.min(pollMs, remaining))
  }

  // Timed out — run one final healthcheck with full logging to show what failed
  errLog('SUT did not become healthy within the timeout.')
  await healthcheckFn({ log: errLog, runtimeTarget, checkoutRoot })
  errLog(`Log: ${logFile}`)
  const tail = await tailFile(logFile, TAIL_LINES)
  if (tail) {
    errLog(`--- last lines of ${path.basename(logFile)} ---`)
    errLog(tail)
    errLog('---')
  }
  return { ok: false, exitCode: 1 }
}
