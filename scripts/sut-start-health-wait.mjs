import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { LOG_TARGETS } from './log-utils.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'

const TIMEOUT_MS = Number(process.env.SUT_TIMEOUT_MS ?? 120_000)
const POLL_MS = Number(process.env.SUT_POLL_MS ?? 3_000)

/** Number of log tail lines to include in failure output. */
const TAIL_LINES = 40

function sleep(ms, signal) {
  return new Promise((resolve) => {
    if (signal?.aborted) {
      resolve()
      return
    }
    let onAbort
    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', onAbort)
      resolve()
    }, ms)
    onAbort = () => {
      clearTimeout(timer)
      resolve()
    }
    signal?.addEventListener('abort', onAbort, { once: true })
  })
}

function reportStartCancelled(errLog, logFile) {
  errLog('SUT start was cancelled.')
  errLog(`Log: ${logFile}`)
  return { ok: false, exitCode: 1 }
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
 * Poll the healthcheck until success, timeout, cancellation, or early child exit.
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
 *   signal?: AbortSignal,
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
  signal,
} = {}) {
  let childExitCode = null
  let childSignal = null

  if (
    typeof child.kill === 'function' &&
    (child.exitCode != null || child.signalCode)
  ) {
    childExitCode = child.exitCode ?? 1
    childSignal = child.signalCode
  } else {
    child.once('exit', (code, exitSignal) => {
      childExitCode = code ?? 1
      childSignal = exitSignal
    })
  }

  const deadline = Date.now() + timeoutMs
  let attempt = 0

  while (Date.now() < deadline) {
    if (signal?.aborted) {
      return reportStartCancelled(errLog, logFile)
    }

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
    await sleep(Math.min(pollMs, remaining), signal)
  }

  if (signal?.aborted) {
    return reportStartCancelled(errLog, logFile)
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
