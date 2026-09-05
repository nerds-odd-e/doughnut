import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import { publishMailboxEvent, readMailboxEvents } from './ci-mailbox.mjs'
import { waitForFile } from './watch-ci-test-fixtures.mjs'

const exec = promisify(execFile)
const launcher = fileURLToPath(new URL('./ci-mailbox.mjs', import.meta.url))
const hook = fileURLToPath(new URL('./ci-host-hook.mjs', import.meta.url))
const sha = 'a'.repeat(40)

async function setup(
  t,
  startArguments = ['--execution', 'owner/repo', 'main', '60000']
) {
  const directory = mkdtempSync(join(tmpdir(), 'ci-process-test-'))
  const bin = join(directory, 'bin')
  mkdirSync(bin)
  writeFileSync(
    join(bin, 'gh'),
    `#!${process.execPath}
const fs = require('node:fs');
const path = require('node:path');
const root = process.env.CI_TEST_ROOT;
const release = path.join(root, 'release');
if (process.argv[3] === 'list') {
  fs.writeFileSync(path.join(root, 'started'), '');
  process.on('SIGTERM', () => {
    fs.writeFileSync(path.join(root, 'request-stopped'), '');
    process.exit(0);
  });
  const output = () => {
    if (!fs.existsSync(release)) return false;
    process.stdout.write(fs.readFileSync(release));
    fs.writeFileSync(path.join(root, 'observed'), '');
    return true;
  };
  if (!output()) {
    const watcher = fs.watch(root, () => { if (output()) watcher.close(); });
  }
} else { process.stdout.write(JSON.stringify({jobs: []})); }
`,
    { mode: 0o700 }
  )
  const env = {
    ...process.env,
    DONUT_CI_MAILBOX_ROOT: directory,
    TMPDIR: directory,
    CI_TEST_ROOT: directory,
    PATH: `${bin}:${process.env.PATH}`,
  }
  const { stdout } = await exec(
    process.execPath,
    [launcher, 'start', ...startArguments],
    { env, timeout: 5000 }
  )
  const mailbox = JSON.parse(stdout.slice('CI_OBSERVER '.length)).directory
  t.after(async () => {
    await exec(process.execPath, [launcher, 'stop', mailbox], { env })
    rmSync(directory, { recursive: true, force: true })
  })
  const deliver = async (host, receipt = '') => {
    const input = JSON.stringify({
      session_id: 'process-test',
      conversation_id: 'process-test',
      generation_id: 'coordinator-turn',
      transcript_path: '/test/coordinator.jsonl',
      hook_event_name: host === 'cursor' ? 'postToolUse' : 'PostToolUse',
      tool_name: host === 'cursor' ? 'Shell' : 'Bash',
      tool_output: JSON.stringify({ stdout: receipt }),
      tool_response: { stdout: receipt },
    })
    const result = exec(process.execPath, [hook, host], { env })
    result.child.stdin.end(input)
    return JSON.parse((await result).stdout)
  }
  return { directory, mailbox, stdout, deliver, env }
}

function releaseRun(directory, overrides = {}) {
  const run = {
    databaseId: 42,
    attempt: 1,
    headSha: sha,
    headBranch: 'main',
    workflowName: 'donut CI',
    event: 'push',
    status: 'completed',
    conclusion: 'failure',
    ...overrides,
  }
  writeFileSync(join(directory, 'release.tmp'), JSON.stringify([run]))
  renameSync(join(directory, 'release.tmp'), join(directory, 'release'))
}

test('execution launcher returns before startup discovery and appends its eventual failure', async (t) => {
  const { directory, mailbox, stdout, deliver } = await setup(t, [
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
      const { directory, mailbox, stdout, deliver, env } = await setup(t)
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

test('stop CLI cancels an outstanding GitHub subprocess and reports pending coverage', async (t) => {
  const { directory, mailbox, env } = await setup(t, [
    '--execution',
    'owner/repo',
    'main',
    '60000',
  ])
  await waitForFile(join(directory, 'started'))
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

test('stop and publication race retains prior unread evidence without continuation', async (t) => {
  const { directory, mailbox, stdout, deliver, env } = await setup(t, [
    '--execution',
    'owner/repo',
    'main',
    '60000',
  ])
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
