import assert from 'node:assert/strict'
import { execFile, spawn } from 'node:child_process'
import { once } from 'node:events'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createInterface } from 'node:readline'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import {
  checkoutRoot,
  publishMailboxEvent,
  readMailbox,
  readMailboxEvents,
  receiptPrefix,
} from './ci-mailbox.mjs'
import {
  blockingGithubEnvironment,
  waitForFile,
  waitForPidExit,
} from './watch-ci-test-fixtures.mjs'

const launcher = fileURLToPath(new URL('./ci-mailbox.mjs', import.meta.url))
const completingFixture = fileURLToPath(
  new URL('./ci-observer-stream-fixture.mjs', import.meta.url)
)
const key = 'ci-watch-execution:owner/repo:main:coordinator'
const runCommand = promisify(execFile)

async function waitForLine(stream) {
  const lines = createInterface({ input: stream })
  try {
    const [line] = await once(lines, 'line')
    return line
  } finally {
    lines.close()
  }
}

function waitForExit(child) {
  return new Promise((resolve, reject) => {
    child.once('exit', (code, signal) =>
      code === 0
        ? resolve()
        : reject(new Error(`observer exited ${code ?? signal}`))
    )
    child.once('error', reject)
  })
}

function createCodexReplay(
  env,
  command = [launcher, 'stream', '--execution', 'owner/repo', 'main', '60000']
) {
  const saved = new Map()
  let launches = 0

  return {
    async setup() {
      const retained = saved.get(key)
      if (['watching', 'finished'].includes(retained?.status)) return retained
      launches += 1
      const child = spawn(process.execPath, command, { env })
      const receipt = await waitForLine(child.stdout)
      const { directory, pid } = JSON.parse(receipt.slice(receiptPrefix.length))
      const state = {
        status: 'watching',
        sessionId: child.pid,
        directory,
        pid,
        process: child,
      }
      saved.set(key, state)
      child.once('exit', () => {
        if (saved.get(key)?.status !== 'watching') return
        saved.set(key, {
          status: 'finished',
          sessionId: undefined,
          directory,
          process: child,
        })
      })
      return state
    },
    launches: () => launches,
    state: () => saved.get(key),
    forgetHandles: () => saved.clear(),
    async stop() {
      const state = saved.get(key)
      saved.set(key, { ...state, status: 'stopped' })
      state.process.kill('SIGINT')
      await waitForExit(state.process)
      return saved.get(key)
    },
  }
}

test('Codex retains its execution handles until natural observer completion', async (t) => {
  const root = mkdtempSync(join(tmpdir(), 'ci-codex-completion-test-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const replay = createCodexReplay(
    { ...process.env, DONUT_CI_MAILBOX_ROOT: root, TMPDIR: root },
    [completingFixture, root]
  )

  const attached = await replay.setup()
  t.after(() => attached.process.kill('SIGTERM'))
  await waitForFile(join(root, 'first-failure-recorded'), 15_000)
  const repeatedSetup = await replay.setup()
  writeFileSync(join(root, 'release-second-failure'), '')
  await waitForExit(attached.process)

  const finished = replay.state()
  assert.equal(replay.launches(), 1)
  assert.equal(repeatedSetup.sessionId, attached.sessionId)
  assert.equal(repeatedSetup.directory, attached.directory)
  assert.equal(finished.status, 'finished')
  assert.equal(finished.sessionId, undefined)
  assert.equal(finished.directory, attached.directory)
  assert.equal(finished.process.pid, attached.process.pid)
  assert.equal(attached.process.exitCode, 0)
})

test('Codex recovers only its retained identity after losing handles and cooperatively stops that observer', async (t) => {
  const env = blockingGithubEnvironment(t)
  const root = env.CI_TEST_ROOT
  const replay = createCodexReplay(env)
  let attached = await replay.setup()
  const child = attached.process
  t.after(() => child.kill('SIGTERM'))
  await waitForFile(join(root, 'github-request-started'))
  assert.equal(attached.pid, child.pid)
  const note = join(root, 'PLAN.md')
  writeFileSync(
    note,
    `Observer: ${JSON.stringify({
      coordinator: key,
      checkout: checkoutRoot,
      directory: attached.directory,
      pid: attached.pid,
    })}\n`
  )
  const failure = {
    type: 'CI_FAILURE',
    repo: 'owner/repo',
    runId: 42,
    attempt: 1,
  }
  publishMailboxEvent(attached.directory, failure)
  const other = createCodexReplay(env)
  const unaffected = await other.setup()
  t.after(() => unaffected.process.kill('SIGTERM'))

  replay.forgetHandles()
  attached = undefined
  assert.equal(replay.state(), undefined)
  const retained = JSON.parse(
    readFileSync(note, 'utf8').slice('Observer: '.length)
  )
  assert.equal(retained.coordinator, key)
  assert.equal(retained.checkout, checkoutRoot)
  const request = readMailbox(retained.directory, retained.checkout, root)
  assert.equal(request.mode, 'execution')
  assert.equal(request.repo, 'owner/repo')
  assert.equal(request.branch, 'main')
  const { stdout } = await runCommand(
    process.execPath,
    [launcher, 'stop', retained.directory],
    { env, timeout: 10_000 }
  )
  assert.equal(
    await waitForPidExit(retained.pid),
    true,
    'the recorded stream PID exits within the deadline'
  )
  const terminal = JSON.parse(
    readFileSync(join(retained.directory, 'result.json'))
  )
  assert.deepEqual(terminal, {
    status: 'stopped',
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough: 1, deliveredThrough: 0, unread: 1 },
  })
  assert.deepEqual(
    JSON.parse(stdout.slice(receiptPrefix.length)).terminal,
    terminal
  )
  assert.deepEqual(
    readMailboxEvents(retained.directory).map(({ event }) => event),
    [failure]
  )
  assert.equal(replay.launches(), 1)
  assert.equal(other.launches(), 1)
  assert.equal(unaffected.process.exitCode, null)
  const live = await runCommand('ps', [
    '-p',
    String(unaffected.pid),
    '-o',
    'pid=',
  ])
  assert.equal(Number(live.stdout.trim()), unaffected.pid)
  await other.stop()
})

test('Codex reuses one execution observer through normal and repair pushes, then stops its exact handles', async (t) => {
  const env = blockingGithubEnvironment(t)
  const root = env.CI_TEST_ROOT
  const replay = createCodexReplay(env)

  const attached = await replay.setup()
  t.after(() => attached.process.kill('SIGTERM'))
  await waitForFile(join(root, 'github-request-started'))
  const repeatedSetup = await replay.setup()
  const afterNormalPush = await replay.setup()
  const afterRepairPush = await replay.setup()

  assert.equal(replay.launches(), 1)
  for (const retained of [repeatedSetup, afterNormalPush, afterRepairPush]) {
    assert.equal(retained.sessionId, attached.sessionId)
    assert.equal(retained.directory, attached.directory)
    assert.equal(retained.process.pid, attached.process.pid)
  }

  publishMailboxEvent(attached.directory, {
    type: 'CI_FAILURE',
    repo: 'owner/repo',
    runId: 42,
    attempt: 1,
  })
  const stopped = await replay.stop()
  await waitForFile(join(root, 'github-request-stopped'))

  assert.equal(stopped.sessionId, attached.sessionId)
  assert.equal(stopped.directory, attached.directory)
  assert.equal(attached.process.exitCode, 0)
  assert.deepEqual(
    JSON.parse(readFileSync(join(attached.directory, 'result.json'))),
    {
      status: 'stopped',
      coverage: { state: 'ended', pendingCi: 'unobserved' },
      evidence: { recordedThrough: 1, deliveredThrough: 0, unread: 1 },
    }
  )
})
