import { writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const worktreeRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)
const runPBin = path.join(worktreeRoot, 'node_modules/.bin/run-p')
export const peerNames = ['backend', 'lb', 'frontend']

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

export function writeRunPPeers(checkoutRoot) {
  writeFileSync(
    path.join(checkoutRoot, 'package.json'),
    `${JSON.stringify({
      name: 'sut-supervisor-child-exit-fixture',
      private: true,
      scripts: {
        'backend:sut': 'node ./peer.mjs backend',
        'backend:sut:ci': 'node ./peer.mjs backend',
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
import { runSutServices, sutServiceArgs } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-services.mjs')
    )}
import { startSutOwnerControlFromEnv } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-owner.mjs')
    )}

await startSutOwnerControlFromEnv()
runSutServices({
  checkoutRoot: process.env.SUT_CHECKOUT_ROOT,
  logFile: process.env.SUT_LOG_FILE,
  serviceArgs: sutServiceArgs({}, process.env.SUT_BACKEND_RELOAD !== 'false'),
  spawnFn: (_cmd, args, opts) => {
    const runP = args.indexOf('run-p')
    return spawn(${JSON.stringify(runPBin)}, args.slice(runP + 1), opts)
  },
})
`
  )
}

export function writePersistentlyLiveSupervisor(checkoutRoot) {
  writeFileSync(
    path.join(checkoutRoot, 'run-supervisor.mjs'),
    `import { spawn } from 'node:child_process'
import { runSutServices } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-services.mjs')
    )}
import { startSutOwnerControlFromEnv } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-owner.mjs')
    )}

await startSutOwnerControlFromEnv()
const child = spawn(process.execPath, ['-e', 'process.exit(17)'], {
  stdio: ['ignore', 'pipe', 'pipe'],
  detached: true,
})
const realKill = process.kill
process.kill = (pid, signal) => {
  if (pid === -child.pid) return signal === 0 ? true : undefined
  return realKill(pid, signal)
}
runSutServices({
  checkoutRoot: process.env.SUT_CHECKOUT_ROOT,
  logFile: process.env.SUT_LOG_FILE,
  serviceArgs: [],
  spawnFn: () => child,
})
`
  )
}

export async function readPeerPids(checkoutRoot) {
  const pids = {}
  for (const name of peerNames) {
    pids[name] = Number(
      await readFile(path.join(checkoutRoot, `${name}.pid`), 'utf8')
    )
  }
  return pids
}
