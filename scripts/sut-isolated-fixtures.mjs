import http from 'node:http'
import net from 'node:net'
import { writeFileSync } from 'node:fs'
import path from 'node:path'
import { claimSutOwnership, startSutOwnerControl } from './sut-owner.mjs'
import { healthyOnce } from './sut-start-fixtures.mjs'
import { runSutStart } from './sut-start.mjs'

export const completeIsolatedConfig = {
  id: 'wt_a7c2',
  e2e: {
    database: 'doughnut_e2e_wt_a7c2',
    backendPort: 19081,
    vitePort: 15174,
    lbListenPort: 15173,
  },
}

export function writeIsolatedConfig(
  checkoutRoot,
  config = completeIsolatedConfig
) {
  writeFileSync(
    path.join(checkoutRoot, '.worktree.local.json'),
    JSON.stringify(config)
  )
}

export function withEnv(t, overrides) {
  const previous = {}
  for (const [name, value] of Object.entries(overrides)) {
    previous[name] = process.env[name]
    if (value === undefined) delete process.env[name]
    else process.env[name] = value
  }
  t.after(() => {
    for (const [name, value] of Object.entries(previous)) {
      if (value === undefined) delete process.env[name]
      else process.env[name] = value
    }
  })
}

function listenOnRandomPort(createServer) {
  return new Promise((resolve, reject) => {
    const server = createServer()
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (!address || typeof address === 'string') {
        reject(new Error('failed to get listen address'))
        return
      }
      resolve({ server, port: address.port })
    })
  })
}

export function listenTcp() {
  return listenOnRandomPort(() => net.createServer((socket) => socket.end()))
}

export function listenHttpReady() {
  return listenOnRandomPort(() =>
    http.createServer((_req, res) => {
      res.statusCode = 200
      res.end('ready')
    })
  )
}

export function closeServer(server) {
  return new Promise((resolve) => server.close(() => resolve()))
}

export async function startLiveOwner(checkoutRoot) {
  const owner = await claimSutOwnership(checkoutRoot)
  const server = await startSutOwnerControl(owner)
  return { owner, server }
}

export async function runConfiguredStart(checkoutRoot, spawn, extra = {}) {
  return runSutStart({
    checkoutRoot,
    spawnFn: spawn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: 5_000,
    pollMs: 50,
    log: extra.log ?? (() => undefined),
    errLog: () => undefined,
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    databaseExistsFn: extra.databaseExistsFn ?? (() => true),
    ...(extra.isPortOccupiedFn
      ? { isPortOccupiedFn: extra.isPortOccupiedFn }
      : extra.checkRealPorts
        ? {}
        : { isPortOccupiedFn: async () => false }),
  })
}
