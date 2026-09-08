import { spawn } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

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
    `import { writeFileSync } from 'node:fs'
writeFileSync(new URL(\`./\${process.argv[2]}.pid\`, import.meta.url), String(process.pid))
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

export function spawnDetachedOwnedSupervisor(
  t,
  { checkoutRoot, owner, logFile, afterCleanup } = {}
) {
  const supervisor = spawn(
    process.execPath,
    [path.join(checkoutRoot, 'run-supervisor.mjs')],
    {
      cwd: checkoutRoot,
      env: {
        ...process.env,
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
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
    afterCleanup?.()
  })
  return { supervisor, state }
}
