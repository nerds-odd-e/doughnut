import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import {
  E2E_PORT_FIELDS,
  RESERVED_ISOLATED_E2E_PORTS,
} from './sut-e2e-ports.mjs'

export const portsModuleHref = new URL('./sut-e2e-ports.mjs', import.meta.url)
  .href

export function assertAllocatedIsolatedPorts(ports) {
  const values = E2E_PORT_FIELDS.map((field) => ports[field])
  for (const port of values) {
    assert.equal(Number.isInteger(port) && port > 0, true)
    assert.equal(RESERVED_ISOLATED_E2E_PORTS.includes(port), false)
  }
  assert.equal(new Set(values).size, 3)
}

export function makeClaimRoot(t) {
  const root = mkdtempSync(path.join(tmpdir(), 'doughnut-e2e-port-claims-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  return root
}

export function waitForClose(child) {
  return new Promise((resolve) => {
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (chunk) => {
      stdout += chunk
    })
    child.stderr.on('data', (chunk) => {
      stderr += chunk
    })
    child.on('close', (status) => {
      resolve({ status, stdout, stderr })
    })
  })
}

export function spawnEnsurePorts(checkoutRoot, claimRoot) {
  return spawn(
    process.execPath,
    [
      '--input-type=module',
      '-e',
      `import { ensureIsolatedE2ePorts } from ${JSON.stringify(portsModuleHref)}
await ensureIsolatedE2ePorts(${JSON.stringify(checkoutRoot)}, {
  claimRoot: ${JSON.stringify(claimRoot)},
})
`,
    ],
    { encoding: 'utf8' }
  )
}
