import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import {
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  renameSync,
  rmSync,
  watch,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'

const exec = promisify(execFile)
const launcher = fileURLToPath(new URL('./ci-mailbox.mjs', import.meta.url))
const hook = fileURLToPath(new URL('./ci-host-hook.mjs', import.meta.url))
const sha = 'a'.repeat(40)

async function waitForFile(path) {
  if (existsSync(path)) return
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      subscription.close()
      reject(new Error(`Missing ${path}`))
    }, 5000)
    const check = () => {
      if (!existsSync(path)) return
      clearTimeout(timer)
      subscription.close()
      resolve()
    }
    const subscription = watch(join(path, '..'), check)
    check()
  })
}

async function setup(t) {
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
  const output = () => {
    if (!fs.existsSync(release)) return false;
    process.stdout.write(fs.readFileSync(release));
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
    TMPDIR: directory,
    CI_TEST_ROOT: directory,
    PATH: `${bin}:${process.env.PATH}`,
  }
  const { stdout } = await exec(
    process.execPath,
    [launcher, 'start', 'owner/repo', sha, 'main'],
    { env, timeout: 5000 }
  )
  const mailbox = JSON.parse(stdout.slice('CI_OBSERVER '.length)).directory
  t.after(async () => {
    await exec(process.execPath, [launcher, 'stop', mailbox], { env })
    await waitForFile(join(mailbox, 'result.json'))
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

for (const host of ['cursor', 'claude'])
  for (const conclusion of ['failure', 'success']) {
    test(`${host}: detached CLI watcher delivers ${conclusion} through the actual hook process`, async (t) => {
      const { directory, mailbox, stdout, deliver } = await setup(t)
      await waitForFile(join(directory, 'started'))
      assert.equal(existsSync(join(mailbox, 'result.json')), false)
      await deliver(host, stdout)
      assert.deepEqual(await deliver(host), {})
      writeFileSync(
        join(directory, 'release.tmp'),
        JSON.stringify([
          {
            databaseId: 42,
            attempt: 1,
            headSha: sha,
            headBranch: 'main',
            workflowName: 'donut CI',
            event: 'push',
            status: 'completed',
            conclusion,
          },
        ])
      )
      renameSync(join(directory, 'release.tmp'), join(directory, 'release'))
      await waitForFile(join(mailbox, 'result.json'))
      const delivered = await deliver(host)
      if (conclusion === 'failure')
        assert.match(JSON.stringify(delivered), /CI_FAILURE/)
      else assert.deepEqual(delivered, {})
      assert.deepEqual(await deliver(host), {})
    })
  }

test('stop CLI cancels an outstanding GitHub subprocess without waiting for CI', async (t) => {
  const { directory, mailbox, env } = await setup(t)
  await waitForFile(join(directory, 'started'))
  await exec(process.execPath, [launcher, 'stop', mailbox], { env })
  await waitForFile(join(mailbox, 'result.json'))
  assert.deepEqual(JSON.parse(readFileSync(join(mailbox, 'result.json'))), {
    status: 'stopped',
  })
})
