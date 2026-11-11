import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { flagValues } from '../.agents/skills/dough-execute-plan/scripts/ci-direct-entry.mjs'
import { conflictPaths } from '../.agents/skills/dough-execute-plan/scripts/ci-repair-stash-applied.mjs'
import {
  gitOutputOrNull,
  inventory,
  stashEntryByOid,
} from '../.agents/skills/dough-execute-plan/scripts/ci-repair-stash-git.mjs'
import {
  dropRepairStash,
  restoreRepairStash,
  saveRepairStash,
} from '../.agents/skills/dough-execute-plan/scripts/ci-repair-stash.mjs'

const usage =
  'usage: ci-repair-stash.mjs save --checkout PATH --label TEXT | restore --record FILE | drop --record FILE | resolve --record FILE --reviewed-oid OID'

async function readRecord(file) {
  if (!file) throw new Error(usage)
  return JSON.parse(await readFile(file, 'utf8'))
}

async function recordRestore(file, record, status, applied) {
  record.restore = { status, applied }
  await writeFile(file, `${JSON.stringify(record, null, 2)}\n`, { mode: 0o600 })
}

async function restore({ record: file }) {
  const result = await restoreRepairStash({ record: file })
  if (result.status !== 'conflict' || result.applied !== 'none') return result

  // Unmerged index entries show that Git began applying the saved work.
  // Git can also name paths when it refuses to apply anything.
  const record = await readRecord(file)
  if (!(await conflictPaths(record.checkout, '')).length) return result
  await recordRestore(file, record, 'conflict', 'partial')
  return { ...result, applied: 'partial' }
}

async function reviewResolution(record, reviewedOid) {
  const result = { ok: false, oid: record.oid }
  if (
    !reviewedOid ||
    reviewedOid !== record.oid ||
    record.restore?.status !== 'conflict'
  )
    return { ...result, status: 'review-required' }

  const root = await gitOutputOrNull(
    record.checkout,
    'rev-parse',
    '--show-toplevel'
  )
  const branch = await gitOutputOrNull(
    record.checkout,
    'symbolic-ref',
    '--short',
    '-q',
    'HEAD'
  )
  if (
    !root ||
    resolve(root) !== resolve(record.checkout) ||
    branch !== record.branch
  )
    return { ...result, status: 'checkout-mismatch' }

  const paths = await conflictPaths(record.checkout, '')
  if (paths.length) return { ...result, status: 'unresolved', paths }
  const current = await inventory(record.checkout)
  const stagedNotRestored = record.staged.filter(
    (path) => !current.staged.includes(path)
  )
  if (stagedNotRestored.length)
    return { ...result, status: 'partial', stagedNotRestored }
  if (!(await stashEntryByOid(record.checkout, record.oid)))
    return { ...result, status: 'missing' }

  return { ok: true, status: 'resolved', applied: 'reviewed', oid: record.oid }
}

async function resolveRepair({ record: file, reviewedOid }) {
  const record = await readRecord(file)
  const result = await reviewResolution(record, reviewedOid)
  if (result.ok)
    await recordRestore(file, record, result.status, result.applied)
  return { record: file, ...result }
}

const operations = new Map([
  ['save', saveRepairStash],
  ['restore', restore],
  ['drop', dropRepairStash],
  ['resolve', resolveRepair],
])

try {
  const [command, ...args] = process.argv.slice(2)
  const operation = operations.get(command)
  if (!operation) throw new Error(usage)
  const result = await operation(flagValues(args))
  console.log(JSON.stringify(result))
  process.exitCode = result.ok ? 0 : 1
} catch (error) {
  console.error(error.message)
  process.exitCode = 2
}
