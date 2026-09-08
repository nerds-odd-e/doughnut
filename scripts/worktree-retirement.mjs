#!/usr/bin/env node
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  acquireRetirementAdmission,
  releaseRetirementAdmission,
  writeRetirementMarker,
} from './worktree-retirement-admission.mjs'
import {
  collectRecordedRetirementVetoes,
  formatRecordedEvidenceRefusal,
} from './worktree-retirement-evidence.mjs'
import { dropRetirementDatabase } from './worktree-retirement-mysql.mjs'
import {
  inspectDisposableDatabaseTargets,
  unitDatabaseNameForIdentity,
} from './worktree-retirement-targets.mjs'

export { inspectDisposableDatabaseTargets, unitDatabaseNameForIdentity }

function formatInspection(targets) {
  const lines = [
    'Disposable database targets (idle snapshot; not a deletion reservation).',
    `Worktree id: ${targets.id}`,
    `Unit database: ${targets.unitDatabase}`,
  ]
  if (targets.e2eDatabase) {
    lines.push(`E2E database: ${targets.e2eDatabase}`)
  }
  lines.push(
    'Idle snapshot: no busy recorded ownership, listeners, database sessions, or surviving checkout backend JVMs.'
  )
  return `${lines.join('\n')}\n`
}

function formatRetirementReport(targets, unitResult) {
  return [
    'Worktree database retirement complete (unit-test allocation).',
    `Worktree id: ${targets.id}`,
    `Unit database: ${targets.unitDatabase} — ${unitResult}`,
    'Retirement marker retained; identity and checkout left in place for you to remove afterward.',
    'Supported starts refuse this marker; retry retirement to complete any missing drops.',
  ].join('\n')
}

function parseArgs(argv) {
  if (argv.length === 0) {
    return { mode: 'retire' }
  }
  if (argv.length === 1 && argv[0] === '--check') {
    return { mode: 'check' }
  }
  throw new Error(`Unknown worktree retirement arguments: ${argv.join(' ')}`)
}

/** Unit-only mutation refuses a recorded E2E allocation before marking or DROP. */
function refuseRecordedE2eAllocation(targets) {
  if (!targets.e2eDatabase) return
  throw new Error(
    `Refusing database retirement: recorded E2E database ${targets.e2eDatabase} is not reclaimable yet. Unit-only allocations can be retired; E2E reclaim is not enabled in this command version.`
  )
}

export function defaultCheckoutRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
}

async function assessIdleTargets(checkoutRoot, evidenceDeps) {
  const targets = inspectDisposableDatabaseTargets(checkoutRoot)
  const vetoes = await collectRecordedRetirementVetoes(
    checkoutRoot,
    targets,
    evidenceDeps
  )
  if (vetoes.length > 0) {
    throw new Error(formatRecordedEvidenceRefusal(vetoes))
  }
  return targets
}

async function retireUnitAllocation({
  checkoutRoot,
  out,
  evidenceDeps,
  mysqlExecFn,
  hooks = {},
}) {
  refuseRecordedE2eAllocation(inspectDisposableDatabaseTargets(checkoutRoot))

  let gateHeld = false
  try {
    acquireRetirementAdmission(checkoutRoot, { allowRetired: true })
    gateHeld = true
    if (hooks.afterGateAcquired) {
      await hooks.afterGateAcquired(checkoutRoot)
    }

    const targets = await assessIdleTargets(checkoutRoot, evidenceDeps)
    refuseRecordedE2eAllocation(targets)

    writeRetirementMarker(checkoutRoot)
    if (hooks.afterMarkerPublished) {
      await hooks.afterMarkerPublished(checkoutRoot, targets)
    }

    let unitResult
    try {
      unitResult = dropRetirementDatabase(targets.unitDatabase, { mysqlExecFn })
    } catch (error) {
      const detail = error instanceof Error ? error.message : String(error)
      throw new Error(
        [
          'Partial worktree database retirement: unit DROP failed after the retirement marker was written.',
          `Worktree id: ${targets.id}`,
          `Unit database target: ${targets.unitDatabase}`,
          `Failure: ${detail}`,
          'Retirement marker retained; retry pnpm worktree:retire to complete missing drops. Supported starts remain blocked.',
        ].join('\n')
      )
    }

    out.write(`${formatRetirementReport(targets, unitResult)}\n`)
    return 0
  } finally {
    if (gateHeld) {
      releaseRetirementAdmission(checkoutRoot)
    }
  }
}

export async function runWorktreeRetire({
  argv = process.argv.slice(2),
  checkoutRoot = defaultCheckoutRoot(),
  out = process.stdout,
  err = process.stderr,
  evidenceDeps = {},
  mysqlExecFn,
  hooks = {},
} = {}) {
  try {
    const { mode } = parseArgs(argv)
    if (mode === 'check') {
      const targets = await assessIdleTargets(checkoutRoot, evidenceDeps)
      out.write(formatInspection(targets))
      return 0
    }
    return await retireUnitAllocation({
      checkoutRoot,
      out,
      evidenceDeps,
      mysqlExecFn,
      hooks,
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    err.write(`${message}\n`)
    err.write('Usage: pnpm worktree:retire [--check]\n')
    return 1
  }
}

const isMain = process.argv[1]
  ? fileURLToPath(import.meta.url) === path.resolve(process.argv[1])
  : false

if (isMain) {
  runWorktreeRetire().then((code) => process.exit(code))
}
