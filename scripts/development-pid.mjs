import { readFileSync } from 'node:fs'

export function isProcessAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    if (error.code === 'ESRCH') return false
    throw error
  }
}

/**
 * @param {string} pidFile
 * @returns {{ kind: 'absent' } | { kind: 'invalid', raw: string } | { kind: 'pid', pid: number }}
 */
export function readRecordedDevelopmentPid(pidFile) {
  let raw
  try {
    raw = readFileSync(pidFile, 'utf8').trim()
  } catch (error) {
    if (error.code === 'ENOENT') return { kind: 'absent' }
    throw error
  }
  if (raw === '' || !/^[0-9]+$/.test(raw)) {
    return { kind: 'invalid', raw }
  }
  return { kind: 'pid', pid: Number(raw) }
}

/** Live recorded pid, or null when absent, invalid, or not alive. */
export function readLiveDevelopmentPid(
  pidFile,
  isProcessAliveFn = isProcessAlive
) {
  const recorded = readRecordedDevelopmentPid(pidFile)
  if (recorded.kind !== 'pid') return null
  return isProcessAliveFn(recorded.pid) ? recorded.pid : null
}
