import {
  closeSync,
  existsSync,
  mkdirSync,
  openSync,
  renameSync,
  rmSync,
  statSync,
  writeSync,
} from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export const DEFAULT_LOG_MAX_BYTES = 5 * 1024 * 1024
export const DEFAULT_LOG_BACKUPS = 3
export const DEFAULT_TAIL_LINES = 200

export const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export const LOG_TARGETS = {
  sut: path.join(repoRoot, 'sut.log'),
  'backend-e2e': path.join(repoRoot, 'backend/logs/doughnut-e2e.log'),
  mountebank: path.join(repoRoot, 'sut.log'),
}

export function backupLogPath(logFile, index) {
  return `${logFile}.${index}`
}

export function resolveLogTarget(target) {
  if (Object.hasOwn(LOG_TARGETS, target)) {
    return LOG_TARGETS[target]
  }
  throw new Error(
    `Unknown log target "${target}". Choose one of: ${Object.keys(LOG_TARGETS).join(', ')}`
  )
}

export function rotateLogFile(logFile, backups = DEFAULT_LOG_BACKUPS) {
  if (!existsSync(logFile)) return

  mkdirSync(path.dirname(logFile), { recursive: true })
  rmSync(backupLogPath(logFile, backups), { force: true })

  for (let index = backups - 1; index >= 1; index--) {
    const source = backupLogPath(logFile, index)
    if (existsSync(source)) {
      renameSync(source, backupLogPath(logFile, index + 1))
    }
  }

  renameSync(logFile, backupLogPath(logFile, 1))
}

export function rotateLogFileIfNeeded(
  logFile,
  maxBytes = DEFAULT_LOG_MAX_BYTES,
  backups = DEFAULT_LOG_BACKUPS
) {
  if (!existsSync(logFile)) return false
  if (statSync(logFile).size < maxBytes) return false

  rotateLogFile(logFile, backups)
  return true
}

export function createRotatingLogWriter(
  logFile,
  { maxBytes = DEFAULT_LOG_MAX_BYTES, backups = DEFAULT_LOG_BACKUPS } = {}
) {
  mkdirSync(path.dirname(logFile), { recursive: true })
  let fd = openSync(logFile, 'a')
  let bytesWritten = existsSync(logFile) ? statSync(logFile).size : 0

  const reopen = () => {
    closeSync(fd)
    rotateLogFile(logFile, backups)
    fd = openSync(logFile, 'a')
    bytesWritten = 0
  }

  return {
    write(chunk) {
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(String(chunk))
      if (bytesWritten > 0 && bytesWritten + buffer.length > maxBytes) {
        reopen()
      }
      writeSync(fd, buffer)
      bytesWritten += buffer.length
    },
    close() {
      closeSync(fd)
    },
  }
}

export async function tailLogFile(
  logFile,
  lines = DEFAULT_TAIL_LINES,
  { filter } = {}
) {
  const content = await readFile(logFile, 'utf8')
  const selectedLines = content
    .split(/\r?\n/)
    .filter((line) => !filter || filter.test(line))
  return selectedLines.slice(-lines).join('\n')
}
