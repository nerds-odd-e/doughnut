/**
 * Runner-owned private Mountebank for one isolated OpenAI Cypress spec.
 * Does not call start_mb.sh (shared-port adoption / swallowed failure).
 */
import { spawn } from 'node:child_process'
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { allocatePrivateOpenAiMockPorts } from './isolated-openai-mock-ports.mjs'
import {
  assertOwnedMockListener,
  assertPortFreeBeforeMockMutation,
  ownedProcessGroupId,
  waitForOwnedManagementListener,
} from './isolated-openai-mock-ownership.mjs'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'

export {
  SHARED_MOUNTEBANK_MANAGEMENT_PORT,
  SHARED_OPEN_AI_SERVING_PORT,
} from './isolated-openai-mock-ports.mjs'
export {
  GET_ISOLATED_OPEN_AI_MOCK_ENDPOINT_TASK,
  ISOLATED_OPEN_AI_MOCK_ENV_KEY,
  OPEN_AI_MOCK_ENDPOINT_ENV_KEY,
  VERIFY_ISOLATED_OPEN_AI_MOCK_OWNERSHIP_TASK,
} from './open-ai-mock-endpoint-expose-keys.mjs'
export { assertOwnedMockListener } from './isolated-openai-mock-ownership.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

function mbBinary(checkoutRoot) {
  return path.join(checkoutRoot, 'node_modules', '.bin', 'mb')
}

function postJson(url, body) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(body)
    const target = new URL(url)
    const req = http.request(
      {
        hostname: target.hostname,
        port: target.port,
        path: target.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload),
        },
      },
      (res) => {
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () => {
          resolve({
            statusCode: res.statusCode ?? 0,
            body: Buffer.concat(chunks).toString('utf8'),
          })
        })
      }
    )
    req.on('error', reject)
    req.write(payload)
    req.end()
  })
}

export async function createEmptyRecordingImposter(managementUrl, servingPort) {
  const response = await postJson(`${managementUrl}/imposters`, {
    protocol: 'http',
    port: servingPort,
    recordRequests: true,
    stubs: [],
  })
  if (response.statusCode !== 201) {
    throw new Error(
      `Private OpenAI mock failed to create recording imposter on port ${servingPort}: ` +
        `status ${response.statusCode} ${response.body}`
    )
  }
}

/**
 * Failure observation + stop/kill for one owned private-mock child.
 * Used by the real starter and by the spawned mock-failure proof runner.
 */
export function observePrivateMockChild(child) {
  const failState = { error: null }
  const onChildGone = (code, signal) => {
    if (failState.error) return
    failState.error = new Error(
      `Private OpenAI mock process exited unexpectedly ` +
        `(code ${code ?? 'unknown'}, signal ${signal ?? 'none'}).`
    )
  }
  child.once('exit', onChildGone)
  child.once('error', (error) => {
    failState.error = new Error(
      `Private OpenAI mock failed to start: ${error.message}`
    )
  })

  let stopped = false
  const disarm = () => child.removeListener('exit', onChildGone)

  return {
    getFailure() {
      return failState.error
    },
    async stop() {
      if (stopped) return
      stopped = true
      disarm()
      await stopOwnedSutProcessTree(child)
    },
    killSync() {
      if (stopped) return
      stopped = true
      disarm()
      const pgid = child.pid
      if (!Number.isInteger(pgid) || pgid <= 0) return
      try {
        process.kill(-pgid, 'SIGKILL')
      } catch (error) {
        if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
      }
    },
  }
}

/**
 * @returns {Promise<{
 *   endpoint: { managementUrl: string, servingPort: number },
 *   child: import('node:child_process').ChildProcess,
 *   verifyOwnership: () => Promise<true>,
 *   stop: () => Promise<void>,
 *   killSync: () => void,
 * }>}
 */
export async function startPrivateOpenAiMock({
  checkoutRoot = repoRoot,
  allocation,
  spawnFn = spawn,
  allocatePortsFn = allocatePrivateOpenAiMockPorts,
} = {}) {
  const { managementPort, servingPort } = await allocatePortsFn(allocation)
  const managementUrl = `http://127.0.0.1:${managementPort}`
  const pidfile = path.join(
    checkoutRoot,
    `.isolated-openai-mb.${process.pid}.${managementPort}.pid`
  )

  const child = spawnFn(
    mbBinary(checkoutRoot),
    [
      'start',
      '--port',
      String(managementPort),
      '--pidfile',
      pidfile,
      '--nologfile',
      '--loglevel',
      'error',
    ],
    {
      cwd: checkoutRoot,
      detached: true,
      stdio: 'ignore',
      shell: false,
    }
  )
  child.unref()

  const lifecycle = observePrivateMockChild(child)

  const throwIfMockFailed = () => {
    const failure = lifecycle.getFailure()
    if (failure) throw failure
  }

  try {
    await waitForOwnedManagementListener(child, managementPort)
    throwIfMockFailed()
    await assertOwnedMockListener(
      managementPort,
      ownedProcessGroupId(child),
      'management'
    )
    await assertPortFreeBeforeMockMutation(servingPort, 'serving')
    throwIfMockFailed()
    await createEmptyRecordingImposter(managementUrl, servingPort)
    throwIfMockFailed()
    await assertOwnedMockListener(
      servingPort,
      ownedProcessGroupId(child),
      'serving'
    )
    throwIfMockFailed()
  } catch (error) {
    await lifecycle.stop()
    throw error
  }

  const verifyOwnership = async () => {
    throwIfMockFailed()
    const ownedPgid = ownedProcessGroupId(child)
    await assertOwnedMockListener(managementPort, ownedPgid, 'management')
    await assertOwnedMockListener(servingPort, ownedPgid, 'serving')
    throwIfMockFailed()
    return true
  }

  return {
    endpoint: { managementUrl, servingPort },
    child,
    verifyOwnership,
    stop: lifecycle.stop,
    killSync: lifecycle.killSync,
    getFailure: lifecycle.getFailure,
  }
}
