import assert from 'node:assert/strict'
import http from 'node:http'
import { test } from 'node:test'
import {
  activeProfilesIncludeDev,
  checkHttpBody,
  runDevelopmentHealthcheck,
} from './dev-healthcheck.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import { closeServer, listenTcp } from './sut-isolated-fixtures.mjs'

function listenHttp(handler) {
  return new Promise((resolve, reject) => {
    const server = http.createServer(handler)
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (!address || typeof address === 'string') {
        reject(new Error('failed to get HTTP server address'))
        return
      }
      resolve({ server, port: address.port })
    })
  })
}

async function withDevelopmentStack(profileBody, run) {
  const backend = await listenTcp()
  const vite = await listenTcp()
  const { server: lb, port: lbPort } = await listenHttp((req, res) => {
    if (req.url === '/__lb__/ready') {
      res.statusCode = 200
      res.end('ready')
      return
    }
    if (req.url === '/api/healthcheck') {
      res.statusCode = 200
      res.end(profileBody)
      return
    }
    res.statusCode = 404
    res.end()
  })
  try {
    return await run({
      runtimeTarget: {
        ...DEVELOPMENT_RUNTIME_TARGET,
        backendPort: backend.port,
        vitePort: vite.port,
        lbListenPort: lbPort,
      },
    })
  } finally {
    await closeServer(backend.server)
    await closeServer(vite.server)
    await closeServer(lb)
  }
}

test('activeProfilesIncludeDev matches Active Profile: dev', () => {
  assert.equal(
    activeProfilesIncludeDev('OK. Active Profile: dev. Commit: abc'),
    true
  )
  assert.equal(
    activeProfilesIncludeDev('OK. Active Profile: e2e. Commit: abc'),
    false
  )
  assert.equal(
    activeProfilesIncludeDev('OK. Active Profile: dev, something. Commit: x'),
    true
  )
})

test('checkHttpBody returns status and body', async () => {
  const { server, port } = await listenHttp((_req, res) => {
    res.statusCode = 200
    res.end('OK. Active Profile: dev. Commit: test')
  })
  try {
    const result = await checkHttpBody({
      url: `http://127.0.0.1:${port}/api/healthcheck`,
      timeoutMs: 1_000,
    })
    assert.equal(result.ok, true)
    assert.equal(result.status, 200)
    assert.match(result.body, /Active Profile: dev/)
  } finally {
    await closeServer(server)
  }
})

test('runDevelopmentHealthcheck requires Active Profile: dev', async () => {
  await withDevelopmentStack(
    'OK. Active Profile: e2e. Commit: bad',
    async (opts) => {
      const logs = []
      const result = await runDevelopmentHealthcheck({
        ...opts,
        log: (line) => logs.push(line),
      })
      assert.equal(result.ok, false)
      assert.equal(result.exitCode, 1)
      assert.ok(logs.some((line) => /FAIL Active Profile/.test(line)))
    }
  )
})

test('runDevelopmentHealthcheck passes when profile is dev', async () => {
  await withDevelopmentStack(
    'OK. Active Profile: dev. Commit: good',
    async (opts) => {
      const logs = []
      const result = await runDevelopmentHealthcheck({
        ...opts,
        log: (line) => logs.push(line),
      })
      assert.equal(result.ok, true)
      assert.equal(result.exitCode, 0)
      assert.ok(logs.some((line) => /Development healthcheck OK/.test(line)))
    }
  )
})
