import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { healthyOnce, neverHealthy } from './sut-start-fixtures.mjs'
import { runE2eBatch, runE2eInteractive } from './e2e-runner.mjs'
import { LEGACY_SUT_RUNTIME_TARGET } from './sut-runtime-target.mjs'
import {
  makeCypressChild,
  cypressArgv,
} from './e2e-runner-cypress-fixtures.mjs'

for (const { name, run, built, reload } of [
  { name: 'local batch', run: runE2eBatch, built: false, reload: false },
  { name: 'interactive', run: runE2eInteractive, built: false, reload: true },
  { name: 'built batch', run: runE2eBatch, built: true, reload: false },
]) {
  test(`${name}: runner propagates backend reload intent to service launch`, async (t) => {
    const checkout = makePrimaryCheckout(t)
    const launchFile = path.join(checkout.root, 'launch.json')
    const supervisorFile = path.join(checkout.root, 'supervisor.mjs')
    writeFileSync(
      supervisorFile,
      `
import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import { runSutServices } from ${JSON.stringify(new URL('./sut-services.mjs', import.meta.url).href)}
runSutServices({
  spawnFn: (command, args, options) => {
    writeFileSync(${JSON.stringify(launchFile)}, JSON.stringify({ command, args, env: options.env }))
    return spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], options)
  },
})
`
    )
    const code = await run({
      argv: cypressArgv(),
      checkoutRoot: checkout.root,
      runtimeTarget: { ...LEGACY_SUT_RUNTIME_TARGET, built },
      spawnFn: (command, _args, options) =>
        spawn(command, [supervisorFile], options),
      logFile: path.join(checkout.root, 'sut.log'),
      pidFile: path.join(checkout.root, 'sut.pid'),
      healthcheckFn: () =>
        existsSync(launchFile) ? healthyOnce() : neverHealthy(),
      isPortOccupiedFn: async () => false,
      timeoutMs: 5000,
      pollMs: 20,
      log: () => undefined,
      errLog: () => undefined,
      spawnCypress: () => makeCypressChild(0),
    })
    assert.equal(code, 0)
    const { command, args, env } = JSON.parse(readFileSync(launchFile, 'utf8'))
    assert.equal(command, 'pnpm')
    const backend = args.find((arg) => arg.startsWith('backend:'))
    assert.equal(backend, reload ? 'backend:sut' : 'backend:sut:ci')
    const { scripts } = JSON.parse(
      readFileSync(new URL('../package.json', import.meta.url), 'utf8')
    )
    assert.match(scripts['backend:sut:ci'], /bootRunE2E/)
    assert.doesNotMatch(scripts['backend:sut:ci'], /watch|continuous|run-p/)
    if (reload) assert.match(scripts[backend], /backend:watch backend:sut:ci/)
    assert.equal(args.includes('frontend:sut'), !built)
    assert.equal(args.includes('local:lb:vite'), !built)
    assert.equal(Boolean(env.LOCAL_LB_VITE_UPSTREAM), !built)
  })
}
