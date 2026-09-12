import { spawn } from 'node:child_process'
import { isTcpListening } from './sut-isolated-fixtures.mjs'
import {
  claimSutOwnership,
  releaseSutOwnership,
  startSutOwnerControl,
} from './sut-owner.mjs'

/**
 * Live owner whose applicationGroupId is a real detached child that listens
 * on the given ports. Pass omitService to leave that recorded endpoint for a
 * separately owned (foreign) listener.
 */
export async function startOwnedListeningOwner(
  t,
  checkoutRoot,
  { backendPort, vitePort, lbListenPort, omitService } = {}
) {
  const owner = await claimSutOwnership(checkoutRoot)
  const ports = { backendPort, vitePort, lbListenPort }
  const ownedListeners = [
    {
      service: 'backend',
      line: `net.createServer((s) => s.end()).listen(${backendPort}, '127.0.0.1')`,
      port: backendPort,
    },
    {
      service: 'frontend vite',
      line: `net.createServer((s) => s.end()).listen(${vitePort}, '127.0.0.1')`,
      port: vitePort,
    },
    {
      service: 'local LB',
      line: `http.createServer((_q, r) => { r.statusCode = 200; r.end('ready') }).listen(${lbListenPort}, '127.0.0.1')`,
      port: lbListenPort,
    },
  ].filter((entry) => entry.service !== omitService)
  const child = spawn(
    process.execPath,
    [
      '-e',
      `import http from 'node:http'
import net from 'node:net'
${ownedListeners.map((entry) => entry.line).join('\n')}
setInterval(() => {}, 1000)
`,
    ],
    { stdio: 'ignore', detached: true, env: process.env }
  )
  child.unref()
  t.after(() => {
    try {
      process.kill(-child.pid, 'SIGKILL')
    } catch {
      // already gone
    }
    try {
      child.kill('SIGKILL')
    } catch {
      // already gone
    }
  })
  const server = await startSutOwnerControl({
    ...owner,
    getApplicationGroupId: () => child.pid,
  })
  t.after(async () => {
    await new Promise((resolve) => server.close(resolve))
    await releaseSutOwnership(checkoutRoot)
  })
  const expectedPorts = ownedListeners.map((entry) => entry.port)
  const deadline = Date.now() + 5_000
  while (Date.now() < deadline) {
    const up = await Promise.all(
      expectedPorts.map((port) => isTcpListening(port))
    )
    if (up.every(Boolean)) {
      return { owner, server, child, ports, applicationGroupId: child.pid }
    }
    await new Promise((resolve) => setTimeout(resolve, 20))
  }
  throw new Error(
    `timed out waiting for owned listeners on ${expectedPorts.join(',')}`
  )
}
