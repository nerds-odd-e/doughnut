import { LEGACY_SUT_RUNTIME_TARGET } from './sut-runtime-target.mjs'
import { readFileSync } from 'node:fs'
import net from 'node:net'
import path from 'node:path'
import { verifyLiveSutOwner } from './sut-owner.mjs'
import { healthyOnce } from './sut-start-fixtures.mjs'

export function isolatedLifetimeOpts(checkoutRoot, standIn, extra = {}) {
  return {
    checkoutRoot,
    spawnFn: standIn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: extra.timeoutMs ?? 5_000,
    pollMs: extra.pollMs ?? 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    databaseExistsFn: () => true,
    portClaimRoot: path.join(checkoutRoot, '.doughnut-e2e-port-claims'),
    isPortOccupiedFn: async () => false,
  }
}

export function healthcheckWaitingForPids(pidsFile) {
  return async () => {
    try {
      const pids = JSON.parse(readFileSync(pidsFile, 'utf8'))
      if (Number.isInteger(pids.leader) && pids.leader > 0) {
        return {
          ok: true,
          tcpResults: [],
          readinessResult: { ok: true },
          exitCode: 0,
        }
      }
    } catch {
      // pids file not written yet
    }
    return {
      ok: false,
      tcpResults: [],
      readinessResult: { ok: false },
      exitCode: 1,
    }
  }
}

export function healthcheckWaitingForLiveOwner(checkoutRoot) {
  return async () => {
    const live = await verifyLiveSutOwner(checkoutRoot)
    if (!live.ok) {
      return {
        ok: false,
        tcpResults: [],
        readinessResult: { ok: false },
        exitCode: 1,
      }
    }
    return {
      ok: true,
      tcpResults: [],
      readinessResult: { ok: true },
      exitCode: 0,
    }
  }
}

export function listenTcpOnPort(port) {
  return new Promise((resolve, reject) => {
    const server = net.createServer((socket) => socket.end())
    server.once('error', reject)
    server.listen(port, '127.0.0.1', () => {
      resolve({ server, port })
    })
  })
}

export function isPortStillListening(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host: '127.0.0.1', port }, () => {
      socket.end()
      resolve(true)
    })
    socket.on('error', () => resolve(false))
  })
}

export function trackOwnedTree(t, getOwned) {
  t.after(() => {
    const owned = getOwned()
    if (Number.isInteger(owned?.leader) && owned.leader > 0) {
      try {
        process.kill(-owned.leader, 'SIGKILL')
      } catch {
        // already gone
      }
    }
    for (const pid of [owned?.leader, owned?.grandchild]) {
      if (!Number.isInteger(pid) || pid <= 0) continue
      try {
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
  })
}

export function primaryLifetimeOpts(checkoutRoot, standIn, extra = {}) {
  return {
    checkoutRoot,
    spawnFn: standIn.spawnFn,
    logFile: path.join(checkoutRoot, 'sut.log'),
    pidFile: path.join(checkoutRoot, 'sut.pid'),
    timeoutMs: extra.timeoutMs ?? 5_000,
    pollMs: extra.pollMs ?? 50,
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: extra.healthcheckFn ?? healthyOnce,
    isPortOccupiedFn: async () => false,
  }
}

export function readyLifetimeStandIn() {
  return {
    child: { kill: () => undefined, pid: 0 },
    target: {},
    ready: Promise.resolve({ ok: true, exitCode: 0 }),
    shutdown: async () => undefined,
  }
}

export const BUILT_RUNTIME_TARGET = {
  ...LEGACY_SUT_RUNTIME_TARGET,
  built: true,
}
