import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { isTcpListening } from './sut-isolated-fixtures.mjs'

const worktreeRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)
const runPBin = path.join(worktreeRoot, 'node_modules/.bin/run-p')

export const sutPeerNames = ['backend', 'lb', 'frontend']

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function waitUntil(predicate, timeoutMs, message) {
  const deadline = Date.now() + timeoutMs
  let last
  while (Date.now() < deadline) {
    last = await predicate()
    if (last) return last
    await pause(50)
  }
  throw new Error(message)
}

export function writeOwnedSupervisorRunPPeers(checkoutRoot) {
  writeFileSync(
    path.join(checkoutRoot, 'package.json'),
    `${JSON.stringify({
      name: 'sut-owned-supervisor-fixture',
      private: true,
      scripts: {
        'backend:sut': 'node ./peer.mjs backend',
        'local:lb:vite': 'node ./peer.mjs lb',
        'frontend:sut': 'node ./peer.mjs frontend',
      },
    })}\n`
  )
  writeFileSync(
    path.join(checkoutRoot, 'peer.mjs'),
    `import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const name = process.argv[2]
const here = path.dirname(fileURLToPath(import.meta.url))
writeFileSync(path.join(here, \`\${name}.pid\`), String(process.pid))

if (name === 'backend' && process.env.SUT_FIXTURE_BACKEND_PORT) {
  const port = Number(process.env.SUT_FIXTURE_BACKEND_PORT)
  const readyFile = path.join(here, 'backend-listener.ready')
  const listenerPidFile = path.join(here, 'backend-listener.pid')
  const listener = spawn(
    process.execPath,
    [
      '-e',
      \`import { writeFileSync } from 'node:fs'
import net from 'node:net'
net.createServer((s) => s.end()).listen(\${port}, '127.0.0.1', () => {
  writeFileSync(\${JSON.stringify(readyFile)}, 'ready')
})
setInterval(() => {}, 1000)
\`,
    ],
    { detached: true, stdio: 'ignore' }
  )
  writeFileSync(listenerPidFile, String(listener.pid))
  listener.unref()
}

setInterval(() => {}, 1000)
`
  )
  writeFileSync(
    path.join(checkoutRoot, 'run-supervisor.mjs'),
    `import { spawn } from 'node:child_process'
import { startOwnedSutSupervisor, sutServiceArgs } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-services.mjs')
    )}

await startOwnedSutSupervisor({
  checkoutRoot: process.env.SUT_CHECKOUT_ROOT,
  logFile: process.env.SUT_LOG_FILE,
  env: process.env,
  serviceArgs: sutServiceArgs({}),
  spawnFn: (_cmd, args, opts) => {
    const runP = args.indexOf('run-p')
    return spawn(${JSON.stringify(runPBin)}, args.slice(runP + 1), opts)
  },
})
`
  )
}

export async function readPeerPids(checkoutRoot) {
  const pids = {}
  for (const name of sutPeerNames) {
    pids[name] = Number(
      await readFile(path.join(checkoutRoot, `${name}.pid`), 'utf8')
    )
  }
  return pids
}

export async function waitForPeerPids(checkoutRoot, timeoutMs = 5_000) {
  return waitUntil(
    async () => {
      try {
        const pids = await readPeerPids(checkoutRoot)
        if (
          sutPeerNames.every(
            (name) => Number.isInteger(pids[name]) && pids[name] > 0
          )
        ) {
          return pids
        }
      } catch {
        // pid file not written yet
      }
      return false
    },
    timeoutMs,
    'timed out waiting for run-p peer pids'
  )
}

export async function waitForDetachedBackendListener(
  checkoutRoot,
  port,
  timeoutMs = 5_000
) {
  return waitUntil(
    async () => {
      try {
        const listenerPid = Number(
          await readFile(
            path.join(checkoutRoot, 'backend-listener.pid'),
            'utf8'
          )
        )
        const ready = await readFile(
          path.join(checkoutRoot, 'backend-listener.ready'),
          'utf8'
        )
        if (
          Number.isInteger(listenerPid) &&
          listenerPid > 0 &&
          ready.trim() === 'ready' &&
          (await isTcpListening(port))
        ) {
          return listenerPid
        }
      } catch {
        // not ready yet
      }
      return false
    },
    timeoutMs,
    'timed out waiting for detached backend listener readiness'
  )
}

export function spawnDetachedOwnedSupervisor(
  t,
  { checkoutRoot, owner, logFile, afterCleanup, env } = {}
) {
  const supervisor = spawn(
    process.execPath,
    [path.join(checkoutRoot, 'run-supervisor.mjs')],
    {
      cwd: checkoutRoot,
      env: {
        ...process.env,
        ...env,
        SUT_OWNER_TOKEN: owner.token,
        SUT_OWNER_CONTROL_PATH: owner.controlPath,
        SUT_CHECKOUT_ROOT: checkoutRoot,
        SUT_LOG_FILE: logFile,
      },
      stdio: 'ignore',
      detached: true,
    }
  )
  const state = { pids: {} }
  t.after(() => {
    try {
      supervisor.kill('SIGKILL')
    } catch {
      // already gone
    }
    for (const pid of Object.values(state.pids)) {
      if (!Number.isInteger(pid) || pid <= 0) continue
      try {
        process.kill(-pid, 'SIGKILL')
      } catch {
        // already gone or not a group leader
      }
      try {
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
    afterCleanup?.()
  })
  return { supervisor, state }
}
