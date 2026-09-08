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

function retirementDropPlan(targets) {
  const plan = [
    { kind: 'unit', label: 'Unit database', database: targets.unitDatabase },
  ]
  if (targets.e2eDatabase) {
    plan.push({
      kind: 'E2E',
      label: 'E2E database',
      database: targets.e2eDatabase,
    })
  }
  return plan
}

function formatInspection(targets) {
  const lines = [
    'Disposable database targets (idle snapshot; not a deletion reservation).',
    `Worktree id: ${targets.id}`,
  ]
  for (const entry of retirementDropPlan(targets)) {
    lines.push(`${entry.label}: ${entry.database}`)
  }
  lines.push(
    'Idle snapshot: no busy recorded ownership, listeners, database sessions, or surviving checkout backend JVMs.'
  )
  return `${lines.join('\n')}\n`
}

function formatDropLine(label, database, result) {
  return `${label}: ${database} — ${result}`
}

function formatRetirementReport(targets, dropResults) {
  const lines = [
    'Worktree database retirement complete.',
    `Worktree id: ${targets.id}`,
  ]
  for (const entry of dropResults) {
    lines.push(formatDropLine(entry.label, entry.database, entry.result))
  }
  lines.push(
    'Retirement marker retained; identity and checkout left in place for you to remove afterward.',
    'Supported starts refuse this marker; retry retirement to complete any missing drops.'
  )
  return lines.join('\n')
}

function formatPartialRetirementFailure({
  targets,
  completed,
  failed,
  detail,
}) {
  const lines = [
    `Partial worktree database retirement: ${failed.kind} DROP failed after the retirement marker was written.`,
    `Worktree id: ${targets.id}`,
  ]
  for (const entry of completed) {
    lines.push(formatDropLine(entry.label, entry.database, entry.result))
  }
  lines.push(
    `${failed.label} target: ${failed.database}`,
    `Failure: ${detail}`,
    'Retirement marker retained; retry pnpm worktree:retire to complete missing drops. Supported starts remain blocked.'
  )
  return lines.join('\n')
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

async function retireAllocation({
  checkoutRoot,
  out,
  evidenceDeps,
  mysqlExecFn,
  hooks = {},
}) {
  let gateHeld = false
  try {
    acquireRetirementAdmission(checkoutRoot, { allowRetired: true })
    gateHeld = true
    if (hooks.afterGateAcquired) {
      await hooks.afterGateAcquired(checkoutRoot)
    }

    const targets = await assessIdleTargets(checkoutRoot, evidenceDeps)

    writeRetirementMarker(checkoutRoot)
    if (hooks.afterMarkerPublished) {
      await hooks.afterMarkerPublished(checkoutRoot, targets)
    }

    const completed = []
    for (const drop of retirementDropPlan(targets)) {
      try {
        if (hooks.beforeDrop) {
          await hooks.beforeDrop(drop.database, targets)
        }
        completed.push({
          ...drop,
          result: dropRetirementDatabase(drop.database, { mysqlExecFn }),
        })
      } catch (error) {
        const detail = error instanceof Error ? error.message : String(error)
        throw new Error(
          formatPartialRetirementFailure({
            targets,
            completed,
            failed: drop,
            detail,
          })
        )
      }
    }

    out.write(`${formatRetirementReport(targets, completed)}\n`)
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
    return await retireAllocation({
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
