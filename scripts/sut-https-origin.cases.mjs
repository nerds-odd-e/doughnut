import assert from 'node:assert/strict'
import http from 'node:http'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { runE2eBatch } from './e2e-runner.mjs'
import { checkTcpPort } from './sut-healthcheck.mjs'
import { startOwnedSutLifetime } from './sut-owned-lifetime.mjs'
import {
  healthyOnce,
  makeMockChild,
  neverHealthy,
} from './sut-start-fixtures.mjs'

async function fixture(t) {
  const checkoutRoot = await mkdtemp(path.join(tmpdir(), 'sut-https-'))
  const upstream = http.createServer((req, res) => {
    res.setHeader('content-type', 'application/json')
    res.end(
      JSON.stringify({
        path: req.url,
        authorization: req.headers.authorization,
      })
    )
  })
  await new Promise((resolve) => upstream.listen(0, '127.0.0.1', resolve))
  const origin = `http://127.0.0.1:${upstream.address().port}`
  t.after(async () => {
    await new Promise((resolve) => upstream.close(resolve))
    await rm(checkoutRoot, { recursive: true, force: true })
  })
  const options = {
    checkoutRoot,
    runtimeTarget: {
      backendPort: 19081,
      vitePort: 15174,
      lbListenPort: upstream.address().port,
    },
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    https: true,
    healthcheckFn: healthyOnce,
    log: () => undefined,
    errLog: () => undefined,
    spawnFn: () => makeMockChild(),
  }
  return { options, origin }
}

async function closed(origin) {
  const result = await checkTcpPort({
    host: '127.0.0.1',
    port: Number(new URL(origin).port),
  })
  assert.deepEqual(result, { ok: false, reason: 'ECONNREFUSED' })
}

test('owned HTTPS config reaches the child before startup and forwards until shutdown', async (t) => {
  const { options, origin } = await fixture(t)
  const env = Object.freeze({
    DONUT_CANONICAL_ORIGIN: 'https://caller.example',
  })
  let childOrigin
  let retainedProxy
  const code = await runE2eBatch({
    ...options,
    argv: ['--spec', 'e2e_test/features/mcp/mcp_services.feature'],
    sutEnv: env,
    isPortOccupiedFn: async () => false,
    spawnFn: (_command, _args, opts) => {
      childOrigin = opts.env.DONUT_CANONICAL_ORIGIN
      assert.equal(new URL(childOrigin).protocol, 'https:')
      return makeMockChild(0)
    },
    session: async ({ tlsProxy }) => {
      retainedProxy = tlsProxy
      assert.equal(childOrigin, tlsProxy.httpsOrigin)
      const response = await tlsProxy.trustedFetch(`${childOrigin}/api/mcp`, {
        headers: { Authorization: 'Bearer fixture-token' },
      })
      assert.deepEqual(await response.json(), {
        path: '/api/mcp',
        authorization: 'Bearer fixture-token',
      })
      return 0
    },
  })
  assert.equal(code, 0)
  assert.equal(retainedProxy.forwardOrigin, origin)
  assert.equal(env.DONUT_CANONICAL_ORIGIN, 'https://caller.example')
  await closed(childOrigin)
  assert.equal((await fetch(origin)).status, 200)
})

for (const outcome of ['timeout', 'cancelled', 'healthcheck error']) {
  test(`owned HTTPS closes when readiness ends with ${outcome}`, async (t) => {
    const { options } = await fixture(t)
    const controller = new AbortController()
    const lifetime = await startOwnedSutLifetime({
      ...options,
      signal: controller.signal,
      timeoutMs: 20,
      pollMs: 5,
      healthcheckFn:
        outcome === 'healthcheck error'
          ? async () => {
              throw new Error('healthcheck failed')
            }
          : neverHealthy,
    })
    t.after(() => lifetime.shutdown())
    if (outcome === 'cancelled') controller.abort()
    if (outcome === 'healthcheck error') {
      await assert.rejects(lifetime.ready, /healthcheck failed/)
    } else {
      assert.equal((await lifetime.ready).ok, false)
    }
    await closed(lifetime.tlsProxy.httpsOrigin)
  })
}

test('owned HTTPS releases its listener when spawning services throws', async (t) => {
  const { options } = await fixture(t)
  let origin
  await assert.rejects(
    startOwnedSutLifetime({
      ...options,
      spawnFn: (_command, _args, opts) => {
        origin = opts.env.DONUT_CANONICAL_ORIGIN
        throw new Error('spawn failed')
      },
    }),
    /spawn failed/
  )
  await closed(origin)
})
