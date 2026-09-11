import { spawn } from 'node:child_process'
import http from 'node:http'
import { observePrivateMockChild } from './isolated-openai-mock.mjs'
import { listenEphemeralPort } from './sut-e2e-port-listen.mjs'

export function spawnDetachedNode(source) {
  const child = spawn(process.execPath, ['-e', source], {
    detached: true,
    stdio: 'ignore',
    env: process.env,
  })
  child.unref()
  return child
}

export function spawnIdleMockChild() {
  return spawnDetachedNode('setInterval(() => {}, 1000)')
}

const defaultPrivateMockEndpoint = {
  managementUrl: 'http://127.0.0.1:18025',
  servingPort: 18001,
}

/** Idle child wrapped with the same failure/stop surface as a private mock. */
export function spawnIdlePrivateMockHandle(
  endpoint = defaultPrivateMockEndpoint
) {
  const child = spawnIdleMockChild()
  const lifecycle = observePrivateMockChild(child)
  return {
    endpoint,
    child,
    getFailure: lifecycle.getFailure,
    async verifyOwnership() {
      return true
    },
    stop: lifecycle.stop,
    killSync: lifecycle.killSync,
  }
}

/** Process-free test double with the private-mock handle surface. */
export function stubPrivateOpenAiMockHandle(
  endpoint = defaultPrivateMockEndpoint
) {
  return {
    endpoint,
    child: {
      once() {
        /* stub */
      },
      pid: 4242,
    },
    async verifyOwnership() {
      return true
    },
    async stop() {
      /* stub */
    },
    killSync() {
      /* stub */
    },
    getFailure() {
      return null
    },
  }
}

export function spawnOwnedManagementListener(managementPort) {
  return spawnDetachedNode(`
import http from 'node:http'
http.createServer((_q, r) => { r.statusCode = 200; r.end('owned-management') })
  .listen(${managementPort}, '127.0.0.1')
setInterval(() => {}, 1000)
`)
}

/** Foreign ready listener that counts non-GET/HEAD requests as mutations. */
export async function listenForeignReadyTrackingMutations() {
  let mutations = 0
  const listener = await listenEphemeralPort(() =>
    http.createServer((req, res) => {
      if (req.method !== 'GET' && req.method !== 'HEAD') {
        mutations += 1
      }
      res.statusCode = 200
      res.end('foreign-ready')
    })
  )
  return {
    ...listener,
    mutationCount: () => mutations,
  }
}
