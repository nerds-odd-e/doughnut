import { execFile } from 'node:child_process'
import { realpathSync } from 'node:fs'
import path from 'node:path'
import { promisify } from 'node:util'
import { parentPid } from './sut-listener-pids.mjs'

const execFileAsync = promisify(execFile)

function resolvePath(target) {
  try {
    return realpathSync(target)
  } catch {
    return path.resolve(target)
  }
}

function isSameOrUnder(candidate, root) {
  const resolvedCandidate = resolvePath(candidate)
  const resolvedRoot = resolvePath(root)
  return (
    resolvedCandidate === resolvedRoot ||
    resolvedCandidate.startsWith(`${resolvedRoot}${path.sep}`)
  )
}

function isJavaCommand(command) {
  return /(^|[\s/])java(\s|$)/.test(command)
}

function isSupportedBackendJvmCommand(command) {
  if (command.includes('com.odde.donut.DonutApplication')) return true
  if (
    command.includes('gradle-wrapper.jar') &&
    (/\s-p\s+backend(\s|$)/.test(command) ||
      command.includes(`${path.sep}backend${path.sep}`) ||
      command.includes('/backend/') ||
      command.includes('\\backend\\'))
  ) {
    return true
  }
  if (
    /backend[/\\]build[/\\]classes/.test(command) ||
    command.includes('bootRunE2E') ||
    command.includes('migrateTestDB')
  ) {
    return true
  }
  return false
}

function parseProcessTable(stdout) {
  const rows = []
  for (const line of String(stdout).split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const match = trimmed.match(/^(\d+)\s+(\d+)\s+(.*)$/)
    if (!match) continue
    const pid = Number(match[1])
    const ppid = Number(match[2])
    if (!(Number.isInteger(pid) && pid > 0 && Number.isInteger(ppid))) continue
    rows.push({ pid, ppid, command: match[3] })
  }
  return rows
}

function parseLsofCwdMap(stdout) {
  /** @type {Map<number, string>} */
  const cwdByPid = new Map()
  let currentPid
  for (const line of String(stdout).split(/\r?\n/)) {
    if (line.startsWith('p')) {
      const pid = Number(line.slice(1))
      currentPid = Number.isInteger(pid) && pid > 0 ? pid : undefined
      continue
    }
    if (line.startsWith('n') && currentPid !== undefined) {
      cwdByPid.set(currentPid, line.slice(1))
    }
  }
  return cwdByPid
}

async function listProcessTable({ execFileFn = execFileAsync } = {}) {
  const { stdout } = await execFileFn('ps', ['-axo', 'pid=,ppid=,command='])
  return parseProcessTable(stdout)
}

/**
 * Working directories for Java PIDs via `lsof`. Exit code 1 is treated as a
 * partial listing (common when some PIDs cannot be inspected); missing PIDs
 * are absent from the map. Callers must treat absence as unknown, not idle.
 */
export async function listJavaWorkingDirectories(
  javaPids,
  { execFileFn = execFileAsync } = {}
) {
  if (javaPids.length === 0) return new Map()
  try {
    const { stdout } = await execFileFn('lsof', [
      '-a',
      '-d',
      'cwd',
      '-Fn',
      '-p',
      javaPids.join(','),
    ])
    return parseLsofCwdMap(stdout)
  } catch (error) {
    if (error && typeof error === 'object' && error.code === 1) {
      return parseLsofCwdMap(error.stdout || '')
    }
    throw error
  }
}

async function excludedRetirementPids(
  { parentPidFn = parentPid } = {},
  selfPid = process.pid
) {
  const excluded = new Set([selfPid])
  const seen = new Set()
  let current = await parentPidFn(selfPid)
  while (Number.isInteger(current) && current > 0 && !seen.has(current)) {
    seen.add(current)
    excluded.add(current)
    current = await parentPidFn(current)
  }
  return excluded
}

function commandReferencesCheckout(command, checkoutRoot) {
  const resolved = resolvePath(checkoutRoot)
  return (
    command.includes(checkoutRoot) ||
    command.includes(resolved) ||
    command.includes(path.resolve(checkoutRoot))
  )
}

function inspectionFailure(kind, error) {
  const detail = error instanceof Error ? error.message : String(error)
  return new Error(
    `Refusing worktree database retirement: unable to inspect checkout ${kind}: ${detail}`
  )
}

/**
 * Bounded checkout-process vetoes for surviving supported backend JVMs.
 * Uses command-line and working-directory evidence. Empty ancestry walks are
 * not treated as clearance. A supported JVM whose checkout relation cannot be
 * resolved (no command-line path and unavailable cwd) is uncertain evidence,
 * not idle. Positively unrelated peer cwds remain eligible. Excludes the
 * retirement process and its ancestors.
 */
export async function inspectCheckoutBackendProcesses(
  checkoutRoot,
  {
    listProcessTableFn = listProcessTable,
    listJavaWorkingDirectoriesFn = listJavaWorkingDirectories,
    parentPidFn = parentPid,
    selfPid = process.pid,
  } = {}
) {
  let rows
  try {
    rows = await listProcessTableFn()
  } catch (error) {
    throw inspectionFailure('processes', error)
  }
  if (!Array.isArray(rows)) {
    throw new Error(
      'Refusing worktree database retirement: checkout process inspection returned ambiguous evidence.'
    )
  }

  const excluded = await excludedRetirementPids({ parentPidFn }, selfPid)
  const javaRows = rows.filter(
    (row) => !excluded.has(row.pid) && isJavaCommand(row.command)
  )

  const needsCwd = javaRows.filter(
    (row) => !commandReferencesCheckout(row.command, checkoutRoot)
  )
  let cwdByPid
  try {
    cwdByPid = await listJavaWorkingDirectoriesFn(
      needsCwd.map((row) => row.pid)
    )
  } catch (error) {
    throw inspectionFailure('process working directories', error)
  }
  if (!(cwdByPid instanceof Map)) {
    throw new Error(
      'Refusing worktree database retirement: checkout process working-directory inspection returned ambiguous evidence.'
    )
  }

  const vetoes = []
  for (const row of javaRows) {
    const relatedByCommand = commandReferencesCheckout(
      row.command,
      checkoutRoot
    )
    const cwd = cwdByPid.get(row.pid)
    const hasCwd = typeof cwd === 'string' && cwd !== ''
    const relatedByCwd = hasCwd && isSameOrUnder(cwd, checkoutRoot)

    if (relatedByCommand || relatedByCwd) {
      if (isSupportedBackendJvmCommand(row.command)) {
        vetoes.push(`surviving checkout backend JVM (pid ${row.pid})`)
        continue
      }
      vetoes.push(
        `ambiguous checkout JVM evidence (pid ${row.pid}; related by ${
          relatedByCommand ? 'command-line' : 'working-directory'
        })`
      )
      continue
    }

    if (isSupportedBackendJvmCommand(row.command) && !hasCwd) {
      vetoes.push(
        `uncertain checkout backend JVM evidence (pid ${row.pid}; working directory unavailable)`
      )
    }
  }
  return vetoes
}
