#!/usr/bin/env node
/**
 * Checkout-local retirement admission gate and durable retirement marker.
 *
 * Runners hold the gate only from start through establishing their existing
 * ownership, then release it. Retirement (later) holds it across final
 * validation, marking, and deletion. A present gate or marker is a refusal —
 * not a queue, lease recovery, or timeout-based reclaim.
 */
import { existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'

export const RETIREMENT_ADMISSION_GATE_DIR_NAME = '.worktree.retire.gate'
export const RETIREMENT_MARKER_NAME = '.worktree.retire.marker'

export function retirementAdmissionGatePath(checkoutRoot) {
  return path.join(checkoutRoot, RETIREMENT_ADMISSION_GATE_DIR_NAME)
}

export function retirementMarkerPath(checkoutRoot) {
  return path.join(checkoutRoot, RETIREMENT_MARKER_NAME)
}

export function isCheckoutRetired(checkoutRoot) {
  return existsSync(retirementMarkerPath(checkoutRoot))
}

export function assertCheckoutNotRetired(checkoutRoot) {
  if (!isCheckoutRetired(checkoutRoot)) return
  throw new Error(
    `Refusing worktree runner start: this checkout is marked retired (${RETIREMENT_MARKER_NAME}). Databases will not be provisioned or launched.`
  )
}

/**
 * Acquire the checkout admission gate.
 * Runners must leave allowRetired false (default) so a marker refuses start.
 * Retirement mutation passes allowRetired: true so retry can finish drops.
 */
export function acquireRetirementAdmission(
  checkoutRoot,
  { allowRetired = false } = {}
) {
  if (!allowRetired) {
    assertCheckoutNotRetired(checkoutRoot)
  }
  const gateDir = retirementAdmissionGatePath(checkoutRoot)
  try {
    mkdirSync(gateDir)
  } catch (error) {
    if (error.code === 'EEXIST') {
      throw new Error(
        `Refusing worktree runner admission: retirement admission gate is already held (${RETIREMENT_ADMISSION_GATE_DIR_NAME}).`
      )
    }
    throw error
  }
  writeFileSync(path.join(gateDir, 'owner.pid'), String(process.pid))
}

export function releaseRetirementAdmission(checkoutRoot) {
  rmSync(retirementAdmissionGatePath(checkoutRoot), {
    recursive: true,
    force: true,
  })
}

/** Acquire the gate and return a one-shot release callback. */
export function holdRetirementAdmission(checkoutRoot, options = {}) {
  acquireRetirementAdmission(checkoutRoot, options)
  let held = true
  return () => {
    if (!held) return
    releaseRetirementAdmission(checkoutRoot)
    held = false
  }
}

/** Seed a durable retirement marker (tests and later retirement mutation). */
export function writeRetirementMarker(checkoutRoot) {
  writeFileSync(
    retirementMarkerPath(checkoutRoot),
    `${new Date().toISOString()}\n`
  )
}

function runCli(argv) {
  const [, , command, checkoutRoot] = argv
  if (!checkoutRoot) {
    throw new Error(
      'Worktree retirement admission command requires a checkout root.'
    )
  }
  if (command === 'acquire') {
    acquireRetirementAdmission(checkoutRoot)
    return
  }
  if (command === 'release') {
    releaseRetirementAdmission(checkoutRoot)
    return
  }
  throw new Error(`Unknown worktree retirement admission command: ${command}`)
}

const CLI_COMMANDS = new Set(['acquire', 'release'])

function isCliEntry() {
  const entry = process.argv[1]
  return Boolean(
    entry &&
      path.basename(entry) === 'worktree-retirement-admission.mjs' &&
      CLI_COMMANDS.has(process.argv[2])
  )
}

if (isCliEntry()) {
  try {
    runCli(process.argv)
  } catch (error) {
    process.stderr.write(
      `${error instanceof Error ? error.message : String(error)}\n`
    )
    process.exit(1)
  }
} else if (
  process.argv[1] &&
  path.basename(process.argv[1]) === 'worktree-retirement-admission.mjs'
) {
  process.stderr.write(
    'Usage: node scripts/worktree-retirement-admission.mjs acquire|release <checkoutRoot>\n'
  )
  process.exit(1)
}
