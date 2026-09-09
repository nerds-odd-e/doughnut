import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  guardCypressNodeSetup,
  SUPPORTED_ISOLATED_CLI_SPEC,
  SUPPORTED_ISOLATED_CYPRESS_SPECS,
} from './isolated-cypress.mjs'
import {
  assertRefusesBeforeReset,
  cypressArgv,
  isolatedCypressOpts,
  isolatedCypressSpec,
  supportedConfig,
} from './isolated-cypress-test-helpers.mjs'
import { startLiveOwner } from './sut-isolated-fixtures.mjs'
import { isolatedBrowserOrigin } from './sut-runtime-target.mjs'

test('web-created-note CLI spec is allowlisted for isolated runs', () => {
  assert.equal(
    SUPPORTED_ISOLATED_CYPRESS_SPECS.includes(SUPPORTED_ISOLATED_CLI_SPEC),
    true
  )
})

test('two isolated worktrees admit the CLI spec with distinct origins; an unlisted CLI spec still refuses', async (t) => {
  const firstPorts = {
    database: 'doughnut_e2e_wt_peer_a',
    backendPort: 19091,
    vitePort: 15184,
    lbListenPort: 15183,
  }
  const secondPorts = {
    database: 'doughnut_e2e_wt_peer_b',
    backendPort: 19092,
    vitePort: 15186,
    lbListenPort: 15185,
  }
  const first = makePrimaryCheckout(t, {
    config: JSON.stringify({ id: 'wt_peer_a', e2e: firstPorts }),
  })
  const second = makePrimaryCheckout(t, {
    config: JSON.stringify({ id: 'wt_peer_b', e2e: secondPorts }),
  })
  const liveFirst = await startLiveOwner(first.root)
  t.after(() => liveFirst.server.close())
  const liveSecond = await startLiveOwner(second.root)
  t.after(() => liveSecond.server.close())

  const configFirst = supportedConfig(
    'http://localhost:5173',
    SUPPORTED_ISOLATED_CLI_SPEC
  )
  const isolatedFirst = await guardCypressNodeSetup(
    first.root,
    configFirst,
    isolatedCypressOpts({ argv: cypressArgv(SUPPORTED_ISOLATED_CLI_SPEC) })
  )
  t.after(() => isolatedFirst.release())

  const configSecond = supportedConfig(
    'http://localhost:5173',
    SUPPORTED_ISOLATED_CLI_SPEC
  )
  const isolatedSecond = await guardCypressNodeSetup(
    second.root,
    configSecond,
    isolatedCypressOpts({ argv: cypressArgv(SUPPORTED_ISOLATED_CLI_SPEC) })
  )
  t.after(() => isolatedSecond.release())

  assert.equal(configFirst.baseUrl, isolatedBrowserOrigin(firstPorts))
  assert.equal(configSecond.baseUrl, isolatedBrowserOrigin(secondPorts))
  assert.notEqual(configFirst.baseUrl, configSecond.baseUrl)

  const unlistedCli = 'e2e_test/features/cli/cli_notebook_clone.feature'
  await assertRefusesBeforeReset(
    () =>
      guardCypressNodeSetup(
        first.root,
        supportedConfig('http://localhost:5173', unlistedCli),
        isolatedCypressOpts({ argv: cypressArgv(unlistedCli) })
      ),
    isolatedCypressSpec
  )
})
