/**
 * Slice 5 — real-Mountebank boundary proof for the two-service resource
 * contract. Proves the real-engine assumption: the current Mountebank version
 * serves two recording imposters with distinct responses under one owned
 * management listener, both serving listeners belong to the recorded child,
 * all owned listeners are gone after shutdown, an unrelated listener
 * survives, and a foreign serving endpoint refuses before mutation.
 *
 * Command: CURSOR_DEV=true nix develop -c node --test scripts/isolated-service-mocks.test.mjs
 */
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { startOwnedMountebankMockMulti } from './isolated-mountebank-mock.mjs'
import {
  allocateMountebankMockPortsMulti,
  SHARED_MOUNTEBANK_MANAGEMENT_PORT,
  SHARED_OPEN_AI_SERVING_PORT,
} from './isolated-mountebank-mock-ports.mjs'
import {
  assertOwnedMockListener,
  ownedProcessGroupId,
} from './isolated-mountebank-mock-ownership.mjs'
import { getListenerPids, processGroupId } from './sut-listener-pids.mjs'
import {
  allocateFreePort,
  closeServer,
  listenHttpReady,
} from './sut-isolated-fixtures.mjs'
import { listenForeignReadyTrackingMutations } from './isolated-openai-mock-test-fixtures.mjs'

// Wikidata's canonical shared serving port (slice 6 will wire its adapter;
// slice 5 only proves the two-service boundary and must exclude it from
// private allocation).
const SHARED_WIKIDATA_SERVING_PORT = 5002

const OPEN_AI_DISTINCT_BODY = 'openai-distinct-response'
const WIKIDATA_DISTINCT_BODY = 'wikidata-distinct-response'

function serviceConfigs() {
  return [
    {
      serviceLabel: 'OpenAI',
      stubs: [
        {
          responses: [
            {
              is: { statusCode: 200, body: OPEN_AI_DISTINCT_BODY },
            },
          ],
        },
      ],
    },
    {
      serviceLabel: 'Wikidata',
      stubs: [
        {
          responses: [
            {
              is: { statusCode: 200, body: WIKIDATA_DISTINCT_BODY },
            },
          ],
        },
      ],
    },
  ]
}

function excludeServingPorts() {
  return [SHARED_OPEN_AI_SERVING_PORT, SHARED_WIKIDATA_SERVING_PORT]
}

function allocation() {
  return {
    e2e: { backendPort: 19081, vitePort: 15174, lbListenPort: 15173 },
  }
}

function allocateMulti() {
  return (a) =>
    allocateMountebankMockPortsMulti(a, {
      excludeServingPorts: excludeServingPorts(),
      servingPortCount: 2,
    })
}

test('two-service mock starts one owned management process with distinct serving endpoints and both listeners belong to the recorded child', async (t) => {
  const mock = await startOwnedMountebankMockMulti({
    allocation: allocation(),
    allocatePortsFn: allocateMulti(),
    serviceConfigs: serviceConfigs(),
  })
  t.after(() => mock.stop())

  const managementPort = Number(new URL(mock.endpoints[0].managementUrl).port)
  const openaiPort = mock.endpoints[0].servingPort
  const wikidataPort = mock.endpoints[1].servingPort

  // Canonical mock and application ports are excluded from private allocation.
  assert.notEqual(managementPort, SHARED_MOUNTEBANK_MANAGEMENT_PORT)
  assert.notEqual(openaiPort, SHARED_OPEN_AI_SERVING_PORT)
  assert.notEqual(wikidataPort, SHARED_WIKIDATA_SERVING_PORT)
  assert.notEqual(openaiPort, 19081)
  assert.notEqual(wikidataPort, 19081)
  assert.notEqual(openaiPort, 15174)
  assert.notEqual(wikidataPort, 15174)
  assert.notEqual(openaiPort, 15173)
  assert.notEqual(wikidataPort, 15173)
  assert.notEqual(openaiPort, wikidataPort)
  assert.notEqual(managementPort, openaiPort)
  assert.notEqual(managementPort, wikidataPort)

  // Both serving listeners belong to the recorded child's process group.
  const ownedPgid = ownedProcessGroupId(mock.child, {
    serviceLabel: 'multi-mock',
  })
  for (const port of [managementPort, openaiPort, wikidataPort]) {
    const pids = await getListenerPids(port)
    assert.ok(pids.length > 0, `listener on port ${port} exists`)
    for (const pid of pids) {
      assert.equal(
        await processGroupId(pid),
        ownedPgid,
        `port ${port} listener belongs to owned child`
      )
    }
  }

  // Distinct responses from the two imposters.
  const openaiResponse = await (
    await fetch(`http://127.0.0.1:${openaiPort}/`)
  ).text()
  const wikidataResponse = await (
    await fetch(`http://127.0.0.1:${wikidataPort}/`)
  ).text()
  assert.equal(openaiResponse, OPEN_AI_DISTINCT_BODY)
  assert.equal(wikidataResponse, WIKIDATA_DISTINCT_BODY)
  assert.notEqual(openaiResponse, wikidataResponse)

  await mock.verifyOwnership()

  await mock.stop()
  // All owned listeners gone after shutdown.
  for (const port of [managementPort, openaiPort, wikidataPort]) {
    assert.equal(
      (await getListenerPids(port)).length,
      0,
      `owned listener gone on port ${port} after shutdown`
    )
  }
})

test('an unrelated listener survives the owned two-service mock shutdown', async (t) => {
  const peerBody = 'unrelated-listener-survives'
  const peer = await listenHttpReady(peerBody)
  t.after(() => closeServer(peer.server))

  const mock = await startOwnedMountebankMockMulti({
    allocation: allocation(),
    allocatePortsFn: allocateMulti(),
    serviceConfigs: serviceConfigs(),
  })
  t.after(() => mock.stop())

  const managementPort = Number(new URL(mock.endpoints[0].managementUrl).port)
  const openaiPort = mock.endpoints[0].servingPort
  const wikidataPort = mock.endpoints[1].servingPort

  await mock.stop()
  for (const port of [managementPort, openaiPort, wikidataPort]) {
    assert.equal(
      (await getListenerPids(port)).length,
      0,
      `owned listener gone on port ${port}`
    )
  }
  assert.equal(
    await (await fetch(`http://127.0.0.1:${peer.port}/`)).text(),
    peerBody
  )
})

test('foreign serving endpoint refuses two-service startup before mutation', async (t) => {
  const foreign = await listenForeignReadyTrackingMutations()
  t.after(() => closeServer(foreign.server))

  const managementPort = await allocateFreePort()
  const otherServingPort = await allocateFreePort()

  await assert.rejects(
    () =>
      startOwnedMountebankMockMulti({
        allocation: allocation(),
        allocatePortsFn: async () => ({
          managementPort,
          servingPorts: [foreign.port, otherServingPort],
        }),
        serviceConfigs: serviceConfigs(),
      }),
    /refuses foreign serving/
  )
  assert.equal(foreign.mutationCount(), 0)
})

test('missing serving endpoint evidence refuses ownership verification before mutation', async () => {
  // A serving port with no listener at all must refuse ownership verification
  // rather than silently adopting a missing endpoint. This is the "missing
  // endpoint evidence" refusal: the ownership check throws before any mutation
  // can proceed on an unproven endpoint.
  const unusedPort = await allocateFreePort()
  const fakeOwnedPgid = 1

  await assert.rejects(
    () =>
      assertOwnedMockListener(unusedPort, fakeOwnedPgid, 'serving', {
        serviceLabel: 'OpenAI',
      }),
    /has no listener owned by this run/
  )
})
