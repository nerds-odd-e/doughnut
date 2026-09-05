import assert from 'node:assert/strict'
import {
  existsSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import {
  createMailbox,
  publishMailboxEvent,
  readMailboxEvents,
  readWorkerIdentity,
  recordWorkerIdentity,
} from './ci-mailbox.mjs'
import {
  exec,
  launcher,
  releaseRun,
  sha,
  setupProcessMailbox,
  spawnIdleNode,
} from './ci-mailbox-process-test-fixtures.mjs'
import { waitForFile } from './watch-ci-test-fixtures.mjs'

test('execution launcher returns before startup discovery and appends its eventual failure', async (t) => {
  const { directory, mailbox, stdout, deliver } = await setupProcessMailbox(t, [
    '--execution',
    'owner/repo',
    'main',
    '60000',
  ])
  await waitForFile(join(directory, 'started'))
  assert.equal(existsSync(join(mailbox, 'result.json')), false)
  writeFileSync(join(directory, 'caller-continued'), '')
  await deliver('cursor', stdout)

  const retained = { type: 'CI_INCOMPLETE', runId: 41, attempt: 1 }
  publishMailboxEvent(mailbox, retained)
  releaseRun(directory, { createdAt: '2026-09-05T12:00:00Z' })

  await waitForFile(join(mailbox, 'events', '000000000002.json'))
  assert.equal(existsSync(join(directory, 'caller-continued')), true)
  assert.deepEqual(
    readMailboxEvents(mailbox).map(({ event }) => event),
    [
      retained,
      {
        type: 'CI_FAILURE',
        repo: 'owner/repo',
        sha,
        branch: 'main',
        workflow: 'ci.yml',
        runId: 42,
        attempt: 1,
        conclusion: 'failure',
        failedJobs: [],
      },
    ]
  )
  const delivered = await deliver('cursor')
  assert.match(
    delivered.additional_context,
    /"type":"CI_INCOMPLETE","runId":41/
  )
  assert.match(
    delivered.additional_context,
    /"type":"CI_FAILURE","repo":"owner\/repo"/
  )
  assert.deepEqual(await deliver('cursor'), {})
})

for (const host of ['cursor', 'claude'])
  for (const conclusion of ['failure', 'success']) {
    test(`${host}: detached execution observer delivers ${conclusion} through the actual hook process`, async (t) => {
      const { directory, mailbox, stdout, deliver, env } =
        await setupProcessMailbox(t)
      await waitForFile(join(directory, 'started'))
      assert.equal(existsSync(join(mailbox, 'result.json')), false)
      await deliver(host, stdout)
      assert.deepEqual(await deliver(host), {})
      releaseRun(directory, { conclusion })
      await waitForFile(join(directory, 'observed'))
      if (conclusion === 'failure')
        await waitForFile(join(mailbox, 'events', '000000000001.json'))
      const delivered = await deliver(host)
      if (conclusion === 'failure')
        assert.match(JSON.stringify(delivered), /CI_FAILURE/)
      else assert.deepEqual(delivered, {})
      assert.deepEqual(await deliver(host), {})
      await exec(process.execPath, [launcher, 'stop', mailbox], { env })
    })
  }

test('launcher retains its exact worker while receipt and normal stop stay unchanged', async (t) => {
  const {
    directory,
    mailbox,
    stdout: launchReceipt,
    env,
  } = await setupProcessMailbox(t, [
    '--execution',
    'owner/repo',
    'main',
    '60000',
  ])
  await waitForFile(join(directory, 'started'))
  assert.deepEqual(readWorkerIdentity(mailbox), {
    pid: Number(readFileSync(join(directory, 'worker-pid'))),
  })
  assert.equal(
    launchReceipt,
    `CI_OBSERVER ${JSON.stringify({ directory: mailbox })}\n`
  )
  const { stdout } = await exec(process.execPath, [launcher, 'stop', mailbox], {
    env,
  })
  await waitForFile(join(directory, 'request-stopped'))
  const expectedTerminal = {
    status: 'stopped',
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough: 0, deliveredThrough: 0, unread: 0 },
  }
  assert.deepEqual(
    JSON.parse(readFileSync(join(mailbox, 'result.json'))),
    expectedTerminal
  )
  assert.deepEqual(JSON.parse(stdout.slice('CI_OBSERVER '.length)), {
    directory: mailbox,
    terminal: expectedTerminal,
  })

  const repeated = await exec(process.execPath, [launcher, 'stop', mailbox], {
    env,
  })
  assert.equal(repeated.stdout, stdout)
})

test('missing terminal publication stops only the retained worker and reports lost coverage', async (t) => {
  const { directory, mailbox, env } = await setupProcessMailbox(t, [
    '--execution',
    'owner/repo',
    'main',
    '60000',
  ])
  await waitForFile(join(directory, 'started'))
  const { pid } = readWorkerIdentity(mailbox)
  const unrelated = await spawnIdleNode(t, [launcher, 'worker'])

  const retained = { type: 'CI_FAILURE', runId: 41, attempt: 1 }
  publishMailboxEvent(mailbox, retained)
  await exec('mkfifo', [join(mailbox, 'result.json.tmp')])

  const { stdout } = await exec(process.execPath, [launcher, 'stop', mailbox], {
    env,
    timeout: 10000,
  })
  const terminal = JSON.parse(stdout.slice('CI_OBSERVER '.length)).terminal

  assert.deepEqual(readMailboxEvents(mailbox), [
    { sequence: 1, event: retained },
  ])
  assert.deepEqual(terminal, {
    status: 'stopped',
    coverage: {
      state: 'lost',
      pendingCi: 'unobserved',
      reason:
        'CI observer terminal result was not published before its lifecycle deadline',
    },
    evidence: { recordedThrough: 1, deliveredThrough: 0, unread: 1 },
  })
  assert.throws(() => process.kill(pid, 0), { code: 'ESRCH' })
  assert.doesNotThrow(() => process.kill(unrelated.pid, 0))
})

test('a reused worker pid is not signaled when it does not belong to the mailbox', async (t) => {
  const storage = mkdtempSync(join(tmpdir(), 'ci-reused-pid-test-'))
  const directory = createMailbox(
    {
      mode: 'execution',
      repo: 'owner/repo',
      branch: 'main',
      maxDurationMs: 60000,
    },
    { storage }
  )
  const unrelated = await spawnIdleNode(t)
  t.after(() => {
    rmSync(storage, { recursive: true, force: true })
  })
  const retained = { type: 'CI_FAILURE', runId: 41, attempt: 1 }
  publishMailboxEvent(directory, retained)
  recordWorkerIdentity(directory, { pid: unrelated.pid })

  await assert.rejects(
    exec(process.execPath, [launcher, 'stop', directory], {
      env: { ...process.env, DONUT_CI_MAILBOX_ROOT: storage },
      timeout: 10000,
    }),
    /does not match this mailbox/
  )

  assert.doesNotThrow(() => process.kill(unrelated.pid, 0))
  assert.equal(existsSync(join(directory, 'result.json')), false)
  assert.deepEqual(readMailboxEvents(directory), [
    { sequence: 1, event: retained },
  ])
})

test('stop and publication race retains prior unread evidence without continuation', async (t) => {
  const { directory, mailbox, stdout, deliver, env } =
    await setupProcessMailbox(t, ['--execution', 'owner/repo', 'main', '60000'])
  await waitForFile(join(directory, 'started'))
  await deliver('cursor', stdout)
  const retained = { type: 'CI_FAILURE', runId: 41, attempt: 1 }
  publishMailboxEvent(mailbox, retained)

  releaseRun(directory, { createdAt: '2026-09-05T12:00:00Z' })
  const stopped = await exec(process.execPath, [launcher, 'stop', mailbox], {
    env,
  })

  const records = readMailboxEvents(mailbox)
  assert.deepEqual(records[0], { sequence: 1, event: retained })
  const terminal = JSON.parse(readFileSync(join(mailbox, 'result.json')))
  assert.equal(terminal.evidence.unread >= 1, true)
  const delivered = await deliver('cursor')
  assert.match(delivered.additional_context, /"runId":41/)
  assert.deepEqual(await deliver('cursor', stopped.stdout), {})
})
