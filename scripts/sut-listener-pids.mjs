import { execFile } from 'node:child_process'

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
 * @param {{ execFileFn?: typeof execFile }} [deps]
 * @returns {Promise<number | undefined>}
 */
export function processGroupId(pid, { execFileFn = execFile } = {}) {
  return new Promise((resolve) => {
    execFileFn('ps', ['-o', 'pgid=', '-p', String(pid)], (err, stdout) => {
      if (err) {
        resolve(undefined)
        return
      }
      const pgid = Number(String(stdout).trim())
      resolve(Number.isInteger(pgid) && pgid > 0 ? pgid : undefined)
    })
  })
}
