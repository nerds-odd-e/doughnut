import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

const execFileAsync = promisify(execFile)

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
 * @param {number} pid
 * @param {'pgid' | 'ppid'} column
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number | undefined>}
 */
function psColumn(pid, column, { execFileFn = execFile } = {}) {
  return new Promise((resolve) => {
    execFileFn('ps', ['-o', `${column}=`, '-p', String(pid)], (err, stdout) => {
      if (err) {
        resolve(undefined)
        return
      }
      const value = Number(String(stdout).trim())
      resolve(Number.isInteger(value) && value > 0 ? value : undefined)
    })
  })
}

/**
 * @param {number} pid
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number | undefined>}
 */
export function processGroupId(pid, deps) {
  return psColumn(pid, 'pgid', deps)
}

/**
 * @param {number} pid
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number | undefined>}
 */
export function parentPid(pid, deps) {
  return psColumn(pid, 'ppid', deps)
}

/**
 * True when the process is in the application group, or an ancestor's PID or
 * PGID matches the published application group (Gradle-forked Boot JVMs).
 *
 * @param {number} pid
 * @param {number} applicationGroupId
 * @param {{ processGroupIdFn?: typeof processGroupId, parentPidFn?: typeof parentPid }} [deps]
 */
export async function isOwnedByApplicationTree(
  pid,
  applicationGroupId,
  { processGroupIdFn = processGroupId, parentPidFn = parentPid } = {}
) {
  if (!Number.isInteger(pid) || pid <= 0) return false
  if (!Number.isInteger(applicationGroupId) || applicationGroupId <= 0) {
    return false
  }

  const ownPgid = await processGroupIdFn(pid)
  if (ownPgid === applicationGroupId) return true

  const seen = new Set()
  let current = await parentPidFn(pid)
  while (Number.isInteger(current) && current > 0 && !seen.has(current)) {
    seen.add(current)
    if (current === applicationGroupId) return true
    const ancestorPgid = await processGroupIdFn(current)
    if (ancestorPgid === applicationGroupId) return true
    current = await parentPidFn(current)
  }
  return false
}

/**
 * All live PIDs whose PPID walk reaches `rootPid` (not including rootPid).
 *
 * @param {number} rootPid
 * @param {{ execFileFn?: typeof execFileAsync }} [deps]
 * @returns {Promise<number[]>}
 */
export async function descendantPidsByParentWalk(
  rootPid,
  { execFileFn = execFileAsync } = {}
) {
  if (!Number.isInteger(rootPid) || rootPid <= 0) return []
  let stdout
  try {
    ;({ stdout } = await execFileFn('ps', ['-axo', 'pid=,ppid=']))
  } catch {
    return []
  }
  /** @type {Map<number, number[]>} */
  const childrenByParent = new Map()
  for (const line of String(stdout).split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const parts = trimmed.split(/\s+/).map(Number)
    if (parts.length < 2) continue
    const [pid, ppid] = parts
    if (!(Number.isInteger(pid) && Number.isInteger(ppid))) continue
    const siblings = childrenByParent.get(ppid)
    if (siblings) siblings.push(pid)
    else childrenByParent.set(ppid, [pid])
  }
  const descendants = []
  const queue = [...(childrenByParent.get(rootPid) ?? [])]
  const seen = new Set()
  while (queue.length > 0) {
    const pid = queue.shift()
    if (!Number.isInteger(pid) || seen.has(pid)) continue
    seen.add(pid)
    descendants.push(pid)
    const children = childrenByParent.get(pid)
    if (children) queue.push(...children)
  }
  return descendants
}
