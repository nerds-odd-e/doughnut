import assert from 'node:assert/strict'

import { EventEmitter } from 'node:events'
import { test } from 'node:test'
import { SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC } from './isolated-cypress-spec-selection.mjs'
import { defaultSpawnCypress, defaultSpawnCypressOpen } from './e2e-runner.mjs'

test('interactive spawn: `cypress open` is invoked WITHOUT `--spec` (spec selection happens in the Cypress UI)', () => {
  const captured = []
  const fakeChild = new EventEmitter()
  fakeChild.pid = 0
  fakeChild.kill = () => undefined
  fakeChild.unref = () => undefined
  const spawnFn = (cmd, args, opts) => {
    captured.push({ cmd, args, opts })
    return fakeChild
  }

  // The wrapper still passes `specs` through to the spawner (so the caller's
  // shape is stable), but `cypress open` must NOT receive `--spec` — even when
  // a preselected spec was supplied as a resource-requirement hint upstream.
  defaultSpawnCypressOpen({
    specs: [SUPPORTED_ISOLATED_OPEN_AI_MOCK_SPEC],
    cwd: '/repo',
    env: {},
    cypressBin: '/repo/node_modules/cypress/bin/cypress',
    configFile: 'e2e_test/config/ci.ts',
    stdio: 'inherit',
    spawnFn,
  })

  assert.equal(captured.length, 1, 'spawn must be invoked exactly once')
  const { args } = captured[0]
  assert.equal(args[0], '/repo/node_modules/cypress/bin/cypress')
  assert.equal(args[1], 'open', 'first Cypress arg is the open mode')
  assert.equal(
    args.includes('--spec'),
    false,
    '--spec must NOT be forwarded to cypress open'
  )
  assert.equal(args.includes('--e2e'), true, '--e2e is forwarded')
  assert.ok(
    args.includes('--config-file') && args.includes('e2e_test/config/ci.ts'),
    '--config-file is forwarded'
  )
})

test('defaultSpawnCypress: --browser chrome forwarded as ["--browser","chrome"] in cypress run args', () => {
  const captured = []
  const fakeChild = new EventEmitter()
  fakeChild.pid = 0
  fakeChild.kill = () => undefined
  fakeChild.unref = () => undefined
  const spawnFn = (cmd, args, opts) => {
    captured.push({ cmd, args, opts })
    return fakeChild
  }

  defaultSpawnCypress({
    specs: ['e2e_test/features/foo/**'],
    cwd: '/repo',
    env: {},
    cypressBin: '/repo/node_modules/cypress/bin/cypress',
    configFile: 'e2e_test/config/ci.ts',
    stdio: 'inherit',
    browser: 'chrome',
    spawnFn,
  })

  assert.equal(captured.length, 1)
  const { args } = captured[0]
  const browserIdx = args.indexOf('--browser')
  assert.ok(browserIdx >= 0, '--browser must be in cypress run args')
  assert.equal(args[browserIdx + 1], 'chrome', 'browser value is chrome')
})

test('defaultSpawnCypress: without --browser no --browser arg is emitted', () => {
  const captured = []
  const fakeChild = new EventEmitter()
  fakeChild.pid = 0
  fakeChild.kill = () => undefined
  fakeChild.unref = () => undefined
  const spawnFn = (cmd, args, opts) => {
    captured.push({ cmd, args, opts })
    return fakeChild
  }

  defaultSpawnCypress({
    specs: ['e2e_test/features/foo/**'],
    cwd: '/repo',
    env: {},
    cypressBin: '/repo/node_modules/cypress/bin/cypress',
    configFile: 'e2e_test/config/ci.ts',
    stdio: 'inherit',
    spawnFn,
  })

  assert.equal(captured.length, 1)
  const { args } = captured[0]
  assert.equal(
    args.includes('--browser'),
    false,
    'no --browser arg without a browser'
  )
})
