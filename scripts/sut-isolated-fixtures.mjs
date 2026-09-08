import { spawn } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import http from 'node:http'
import net from 'node:net'
import { readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { claimSutOwnership, startSutOwnerControl } from './sut-owner.mjs'
import { healthyOnce } from './sut-start-fixtures.mjs'
import { runSutStart } from './sut-start.mjs'

export const identityAndPortsConfig = {
  id: 'wt_a7c2',
  e2e: {
    backendPort: 19081,
    vitePort: 15174,
    lbListenPort: 15173,
  },
}

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

export function readIsolatedConfig(checkoutRoot) {
  return JSON.parse(
    readFileSync(path.join(checkoutRoot, '.worktree.local.json'), 'utf8')
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

export function isTcpListening(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host: '127.0.0.1', port }, () => {
      socket.end()
      resolve(true)
    })
    socket.on('error', () => resolve(false))
  })
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
    timeoutMs: extra.timeoutMs ?? 5_000,
    pollMs: extra.pollMs ?? 50,
    log: extra.log ?? (() => undefined),
    errLog: extra.errLog ?? (() => undefined),
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    databaseExistsFn: extra.databaseExistsFn ?? (() => true),
    mysqlExecFn: extra.mysqlExecFn,
    schemaExistsFn: extra.schemaExistsFn,
    portClaimRoot:
      extra.portClaimRoot ??
      path.join(checkoutRoot, '.doughnut-e2e-port-claims'),
    signal: extra.signal,
    ...(extra.isPortOccupiedFn
      ? { isPortOccupiedFn: extra.isPortOccupiedFn }
      : extra.checkRealPorts
        ? {}
        : { isPortOccupiedFn: async () => false }),
  })
}

export function isPidAlive(pid) {
  try {
    process.kill(pid, 0)
    return true
  } catch (error) {
    if (error.code === 'ESRCH') return false
    throw error
  }
}

export function spawnForeignProcess() {
  const child = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], {
    stdio: 'ignore',
  })
  child.unref()
  return child
}

export function spawnOwnedTreeStandIn(checkoutRoot, extraEnv = {}) {
  const pidsFile = path.join(checkoutRoot, 'owned-pids.json')
  const script = path.join(checkoutRoot, 'owned-tree-stand-in.mjs')
  writeFileSync(
    script,
    `import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
const grandchild = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' })
writeFileSync(${JSON.stringify(pidsFile)}, JSON.stringify({ leader: process.pid, grandchild: grandchild.pid }))
if (process.env.SUT_STANDIN_EXIT === '1') process.exit(1)
setInterval(() => {}, 1000)
`
  )
  return {
    pidsFile,
    spawnFn: (cmd, _args, opts) =>
      spawn(cmd, [script], {
        ...opts,
        env: { ...opts.env, ...extraEnv },
      }),
  }
}

export async function waitForOwnedPids(pidsFile, timeoutMs = 3_000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      const pids = JSON.parse(await readFile(pidsFile, 'utf8'))
      if (Number.isInteger(pids.leader) && Number.isInteger(pids.grandchild)) {
        return pids
      }
    } catch {
      // not written yet
    }
    await new Promise((resolve) => setTimeout(resolve, 20))
  }
  throw new Error(`timed out waiting for owned pids at ${pidsFile}`)
}
