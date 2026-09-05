import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { publishMailboxEvent, receiptPrefix } from './ci-mailbox.mjs'
import {
  waitForFile,
  writeBlockingGithubListCommand,
} from './watch-ci-test-fixtures.mjs'

const launcher = fileURLToPath(new URL('./ci-mailbox.mjs', import.meta.url))
const completingFixture = fileURLToPath(
  new URL('./ci-observer-stream-fixture.mjs', import.meta.url)
)
const key = 'ci-watch-execution:owner/repo:main:coordinator'

function waitForLine(stream) {
  return new Promise((resolve, reject) => {
    let buffered = ''
    const receive = (chunk) => {
      buffered += chunk
      const end = buffered.indexOf('\n')
      if (end === -1) return
      cleanup()
      resolve(buffered.slice(0, end))
    }
    const fail = (error) => {
      cleanup()
      reject(error)
    }
    const cleanup = () => {
      stream.off('data', receive)
      stream.off('error', fail)
    }
    stream.on('data', receive)
    stream.on('error', fail)
  })
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
      const directory = JSON.parse(
        receipt.slice(receiptPrefix.length)
      ).directory
      const state = {
        status: 'watching',
        sessionId: child.pid,
        directory,
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

test('Codex reuses one execution observer through normal and repair pushes, then stops its exact handles', async (t) => {
  const root = mkdtempSync(join(tmpdir(), 'ci-codex-lifecycle-test-'))
  const bin = join(root, 'bin')
  writeBlockingGithubListCommand(bin)
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const replay = createCodexReplay({
    ...process.env,
    DONUT_CI_MAILBOX_ROOT: root,
    TMPDIR: root,
    CI_TEST_ROOT: root,
    PATH: `${bin}:${process.env.PATH}`,
  })

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
