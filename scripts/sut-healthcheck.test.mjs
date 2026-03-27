import assert from 'node:assert'
import http from 'node:http'
import net from 'node:net'
import { test } from 'node:test'
import { checkHttpReady, checkTcpPort } from './sut-healthcheck.mjs'

function listenTcpServer() {
  return new Promise((resolve, reject) => {
    const server = net.createServer((socket) => socket.end())
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (!address || typeof address === 'string') {
        reject(new Error('failed to get TCP server address'))
        return
      }
      resolve({ server, host: '127.0.0.1', port: address.port })
    })
  })
}

function closeServer(server) {
  return new Promise((resolve) => server.close(() => resolve()))
}

test('checkTcpPort returns ok on an open TCP listener', async () => {
  const { server, host, port } = await listenTcpServer()
  try {
    const result = await checkTcpPort({ host, port, timeoutMs: 1_000 })
    assert.equal(result.ok, true)
  } finally {
    await closeServer(server)
  }
})

test('checkTcpPort returns failure on a closed TCP port', async () => {
  const { server, host, port } = await listenTcpServer()
  await closeServer(server)

  const result = await checkTcpPort({ host, port, timeoutMs: 1_000 })
  assert.equal(result.ok, false)
})

test('checkHttpReady returns ok for HTTP 200 response', async () => {
  const server = http.createServer((req, res) => {
    res.statusCode = 200
    res.end('ready')
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const address = server.address()
  if (!address || typeof address === 'string') {
    await closeServer(server)
    throw new Error('failed to get HTTP server address')
  }

  try {
    const result = await checkHttpReady({
      url: `http://127.0.0.1:${address.port}/__e2e__/ready`,
      timeoutMs: 1_000,
    })
    assert.equal(result.ok, true)
    assert.equal(result.status, 200)
  } finally {
    await closeServer(server)
  }
})

test('checkHttpReady returns failure for HTTP 503 response', async () => {
  const server = http.createServer((req, res) => {
    res.statusCode = 503
    res.end('not ready')
  })
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const address = server.address()
  if (!address || typeof address === 'string') {
    await closeServer(server)
    throw new Error('failed to get HTTP server address')
  }

  try {
    const result = await checkHttpReady({
      url: `http://127.0.0.1:${address.port}/__e2e__/ready`,
      timeoutMs: 1_000,
    })
    assert.equal(result.ok, false)
    assert.equal(result.status, 503)
  } finally {
    await closeServer(server)
  }
})
