import { spawn } from 'node:child_process'
import { readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import {
  DEVELOPMENT_RUNTIME_TARGET,
  developmentServicesScript,
} from './development-runtime.mjs'
import { allocateFreePort } from './sut-isolated-fixtures.mjs'
import { waitUntil } from './sut-owned-supervisor-fixtures.mjs'

export function targetFor(checkoutRoot, ports = {}) {
  return {
    ...DEVELOPMENT_RUNTIME_TARGET,
    ...ports,
    logFile: path.join(checkoutRoot, 'dev.log'),
  }
}

function killQuietly(target) {
  try {
    process.kill(target, 'SIGKILL')
  } catch {
    // already gone
  }
}

/**
 * Real stand-in Development stack in `checkoutRoot`: a detached
 * `scripts/development-services.mjs` whose child spawns a detached grandchild
 * listening on an allocated port. Only the parent walk connects the listener
 * to the services process, as in the real stack. Killed after the test.
 *
 * @returns {Promise<{ port: number, pids: { services: number, child: number, listener: number } }>}
 */
export async function startStandInDevelopmentStack(t, checkoutRoot) {
  const port = await allocateFreePort()
  const pidsFile = path.join(checkoutRoot, 'stand-in-stack-pids.json')
  const script = developmentServicesScript(checkoutRoot)
  await writeFile(
    script,
    `import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
const listenerSource = \`
import net from 'node:net'
net.createServer((s) => s.end()).listen(${port}, '127.0.0.1', () => process.send('listening'))
\`
const childSource = \`
import { spawn } from 'node:child_process'
const listener = spawn(process.execPath, ['--input-type=module', '-e', \${JSON.stringify(listenerSource)}], {
  detached: true,
  stdio: ['ignore', 'ignore', 'ignore', 'ipc'],
})
listener.once('message', () => {
  process.send({ child: process.pid, listener: listener.pid })
  listener.disconnect()
})
setInterval(() => {}, 1000)
\`
const child = spawn(process.execPath, ['--input-type=module', '-e', childSource], {
  stdio: ['ignore', 'ignore', 'ignore', 'ipc'],
})
child.once('message', (pids) => {
  writeFileSync(${JSON.stringify(pidsFile)}, JSON.stringify({ services: process.pid, ...pids }))
  child.disconnect()
})
setInterval(() => {}, 1000)
`
  )
  const services = spawn(process.execPath, [script], {
    cwd: checkoutRoot,
    detached: true,
    stdio: 'ignore',
  })
  services.unref()
  const state = { pids: { services: services.pid } }
  t.after(() => {
    killQuietly(-services.pid)
    for (const pid of Object.values(state.pids)) killQuietly(pid)
  })
  state.pids = await waitUntil(
    async () => {
      try {
        return JSON.parse(await readFile(pidsFile, 'utf8'))
      } catch {
        return false
      }
    },
    5_000,
    'timed out waiting for stand-in Development stack'
  )
  return { port, pids: state.pids }
}
