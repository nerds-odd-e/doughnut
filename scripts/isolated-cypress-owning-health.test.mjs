import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { guardCypressNodeSetup } from './isolated-cypress.mjs'
import {
  assertRefusesBeforeReset,
  isolatedCypressOpts,
  supportedConfig,
} from './isolated-cypress-test-helpers.mjs'
import {
  allocateFreePort,
  closeServer,
  isTcpListening,
  listenHttpReady,
  writeIsolatedE2ePorts,
} from './sut-isolated-fixtures.mjs'
import { startOwnedListeningOwner } from './sut-owned-listening-fixtures.mjs'
import { isolatedBrowserOrigin } from './sut-runtime-target.mjs'

test('foreign ready listeners refuse Cypress setup before reset and stay alive', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const foreign = await listenHttpReady()
  t.after(() => closeServer(foreign.server))
  const ports = {
    backendPort: await allocateFreePort(),
    vitePort: await allocateFreePort(),
    lbListenPort: foreign.port,
  }
  writeIsolatedE2ePorts(checkout.root, ports)
  const origin = isolatedBrowserOrigin(ports)
  await startOwnedListeningOwner(t, checkout.root, {
    ...ports,
    omitService: 'local LB',
  })
  await assertRefusesBeforeReset(
    () =>
      guardCypressNodeSetup(
        checkout.root,
        supportedConfig(origin),
        isolatedCypressOpts({ realHealthcheck: true })
      ),
    /healthy owning SUT/
  )
  assert.equal(await isTcpListening(foreign.port), true)
})

test('healthy owned allocation passes Cypress setup through real healthcheck', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const ports = {
    backendPort: await allocateFreePort(),
    vitePort: await allocateFreePort(),
    lbListenPort: await allocateFreePort(),
  }
  writeIsolatedE2ePorts(checkout.root, ports)
  const origin = isolatedBrowserOrigin(ports)
  await startOwnedListeningOwner(t, checkout.root, ports)
  const config = supportedConfig(origin)
  const isolated = await guardCypressNodeSetup(
    checkout.root,
    config,
    isolatedCypressOpts({ realHealthcheck: true })
  )
  t.after(() => isolated.release())
  assert.equal(config.baseUrl, origin)
})
