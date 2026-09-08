import assert from 'node:assert/strict'
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  assertRefusedBeforeGradle,
  outputOf,
  runLauncher,
  runLauncherAsync,
  waitForFile,
} from './backend-test-worktree-launcher-fixtures.mjs'
import { makeCheckout } from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  RETIREMENT_ADMISSION_GATE_DIR_NAME,
  RETIREMENT_MARKER_NAME,
  retirementAdmissionGatePath,
  retirementMarkerPath,
  writeRetirementMarker,
} from './worktree-retirement-admission.mjs'

const configuredId = 'wt_a7c2'

function configuredCheckout(t) {
  return makeCheckout(t, {
    config: JSON.stringify({ id: configuredId }),
  })
}

test('held retirement admission gate refuses backend launch before gradle or mysql', (t) => {
  const checkout = configuredCheckout(t)
  mkdirSync(retirementAdmissionGatePath(checkout.root))
  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.match(outputOf(result), new RegExp(RETIREMENT_ADMISSION_GATE_DIR_NAME))
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.lock')),
    false
  )
})

test('admission resume after retirement refuses before provisioning', async (t) => {
  const checkout = configuredCheckout(t)
  const barrierDir = path.join(checkout.root, 'admission-race-barrier')
  mkdirSync(barrierDir)
  const observedClear = path.join(barrierDir, 'observed-clear')
  const continueRun = path.join(barrierDir, 'continue')
  const markerPath = retirementMarkerPath(checkout.root)
  const preloadPath = path.join(checkout.root, 'admission-race-preload.mjs')
  writeFileSync(
    preloadPath,
    [
      "import fs from 'node:fs'",
      "import { syncBuiltinESMExports } from 'node:module'",
      "import { spawnSync } from 'node:child_process'",
      '',
      'const barrierDir = process.env.RETIREMENT_ADMISSION_RACE_BARRIER_DIR',
      'const markerPath = process.env.RETIREMENT_ADMISSION_RACE_MARKER',
      'if (barrierDir && markerPath) {',
      '  const originalExistsSync = fs.existsSync',
      '  let tripped = false',
      '  fs.existsSync = (filePath) => {',
      '    const present = originalExistsSync(filePath)',
      '    if (!tripped && filePath === markerPath && !present) {',
      '      tripped = true',
      "      fs.writeFileSync(`${barrierDir}/observed-clear`, '')",
      '      const continuePath = `${barrierDir}/continue`',
      '      const deadline = Date.now() + 5000',
      '      while (!originalExistsSync(continuePath)) {',
      '        if (Date.now() >= deadline) {',
      '          throw new Error(`Timed out waiting for ${continuePath}`)',
      '        }',
      "        spawnSync('sleep', ['0.05'], { stdio: 'ignore' })",
      '      }',
      '    }',
      '    return present',
      '  }',
      '  syncBuiltinESMExports()',
      '}',
      '',
    ].join('\n')
  )

  const launcher = runLauncherAsync(checkout, {
    env: {
      FAKE_SCHEMA_MISSING: '1',
      NODE_OPTIONS: `--import ${preloadPath}`,
      RETIREMENT_ADMISSION_RACE_BARRIER_DIR: barrierDir,
      RETIREMENT_ADMISSION_RACE_MARKER: markerPath,
    },
  })

  await waitForFile(observedClear)
  writeRetirementMarker(checkout.root)
  writeFileSync(continueRun, '')

  const result = await launcher.waitForExit()
  assertRefusedBeforeGradle(checkout, result)
  assert.equal(existsSync(checkout.mysqlInvocation), false)
  assert.match(outputOf(result), new RegExp(RETIREMENT_MARKER_NAME))
  assert.doesNotMatch(outputOf(result), /Provisioning database administration/)
  assert.equal(existsSync(markerPath), true)
  assert.equal(existsSync(retirementAdmissionGatePath(checkout.root)), false)
  assert.equal(
    existsSync(path.join(checkout.root, '.worktree.local.lock')),
    false
  )
})
