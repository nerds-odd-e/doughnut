import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import { mkdirSync, mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import { publishMailboxEvent, receiptPrefix } from './ci-mailbox.mjs'
import {
  waitForFile,
  writeBlockingGithubListCommand,
} from './watch-ci-test-fixtures.mjs'

const exec = promisify(execFile)
const checkout = fileURLToPath(new URL('../../../../', import.meta.url))
const launcher = fileURLToPath(new URL('./ci-mailbox.mjs', import.meta.url))
const hooks = JSON.parse(
  readFileSync(new URL('../../../../.cursor/hooks.json', import.meta.url))
).hooks

function cursorInput(event, receipt = '', overrides = {}) {
  return {
    conversation_id: 'cursor-coordinator',
    generation_id: 'coordinator-turn',
    hook_event_name: event,
    tool_name: 'Shell',
    tool_output: JSON.stringify({ output: receipt, exitCode: 0 }),
    cursor_version: '3.19.7',
    workspace_roots: [checkout],
    transcript_path: '/test/cursor-coordinator.jsonl',
    ...overrides,
  }
}

async function configuredHook(event, input, env) {
  const [runtime, script, host] = hooks[event][0].command.split(' ')
  assert.equal(runtime, 'node')
  const child = exec(process.execPath, [join(checkout, script), host], { env })
  child.child.stdin.end(JSON.stringify(input))
  return JSON.parse((await child).stdout)
}

function createCursorReplay(launcherEnv, hookEnv = launcherEnv) {
  let observer
  let launches = 0

  return {
    async readiness(receipt = '') {
      const { stdout } = await exec(process.execPath, [launcher, 'probe'], {
        cwd: checkout,
        env: launcherEnv,
      })
      return configuredHook(
        'postToolUse',
        cursorInput('postToolUse', receipt || stdout),
        hookEnv
      )
    },
    async setup() {
      if (observer) return observer
      launches += 1
      const { stdout } = await exec(
        process.execPath,
        [launcher, 'start', '--execution', 'owner/repo', 'main', '60000'],
        { cwd: checkout, env: launcherEnv }
      )
      const directory = JSON.parse(stdout.slice(receiptPrefix.length)).directory
      const attachment = await configuredHook(
        'postToolUse',
        cursorInput('postToolUse', stdout),
        hookEnv
      )
      observer = { directory, attachment }
      return observer
    },
    boundary(overrides = {}) {
      return configuredHook(
        'postToolUse',
        cursorInput('postToolUse', '', overrides),
        hookEnv
      )
    },
    launches: () => launches,
    async stop() {
      if (!observer) return
      return exec(process.execPath, [launcher, 'stop', observer.directory], {
        cwd: checkout,
        env: launcherEnv,
      })
    },
  }
}

test('Cursor reuses one execution observer through pushes and stops its exact mailbox', async (t) => {
  const root = mkdtempSync(join(tmpdir(), 'ci-cursor-lifecycle-test-'))
  const bin = join(root, 'bin')
  writeBlockingGithubListCommand(bin)
  const nixTmp = join(root, 'nix-shell')
  const nativeTmp = join(root, 'native-tmp')
  mkdirSync(nixTmp)
  mkdirSync(nativeTmp)
  const shared = {
    ...process.env,
    DONUT_CI_MAILBOX_ROOT: root,
    CI_TEST_ROOT: root,
    PATH: `${bin}:${process.env.PATH}`,
  }
  const replay = createCursorReplay(
    { ...shared, TMPDIR: nixTmp },
    { ...shared, TMPDIR: nativeTmp }
  )
  t.after(async () => {
    await replay.stop()
    rmSync(root, { recursive: true, force: true })
  })

  const ready = await replay.readiness()
  assert.match(ready.additional_context, /CI_MONITOR_READY/)
  const attached = await replay.setup()
  await waitForFile(join(root, 'github-request-started'))
  assert.match(
    attached.attachment.additional_context,
    /CI observer attached to this coordinator/
  )

  const repeatedSetup = await replay.setup()
  const afterNormalPush = await replay.setup()
  const afterRepairPush = await replay.setup()
  assert.equal(replay.launches(), 1)
  assert.equal(repeatedSetup.directory, attached.directory)
  assert.equal(afterNormalPush.directory, attached.directory)
  assert.equal(afterRepairPush.directory, attached.directory)

  publishMailboxEvent(attached.directory, {
    type: 'CI_FAILURE',
    repo: 'owner/repo',
    runId: 42,
    attempt: 1,
  })
  assert.deepEqual(
    await replay.boundary({ generation_id: 'child-request' }),
    {}
  )
  assert.match(
    (await replay.boundary()).additional_context,
    /"type":"CI_FAILURE"/
  )
  publishMailboxEvent(attached.directory, {
    type: 'CI_MONITOR_UNAVAILABLE',
    repo: 'owner/repo',
    reason: 'fixture lost coverage',
  })
  assert.match(
    (await replay.boundary()).additional_context,
    /"type":"CI_MONITOR_UNAVAILABLE"/
  )

  publishMailboxEvent(attached.directory, {
    type: 'CI_FAILURE',
    repo: 'owner/repo',
    runId: 43,
    attempt: 1,
  })
  const stopped = await replay.stop()
  await waitForFile(join(root, 'github-request-stopped'))
  const terminal = JSON.parse(
    stopped.stdout.slice(receiptPrefix.length)
  ).terminal
  assert.deepEqual(terminal, {
    status: 'stopped',
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough: 3, deliveredThrough: 2, unread: 1 },
  })
  assert.deepEqual(
    JSON.parse(readFileSync(join(attached.directory, 'result.json'))),
    terminal
  )
})

test('Cursor native output payload attaches a Nix-launched probe despite TMPDIR mismatch', async (t) => {
  const root = mkdtempSync(join(tmpdir(), 'ci-cursor-native-payload-'))
  const nixTmp = join(root, 'nix-shell.veeYuy')
  const nativeTmp = join(root, 'native-tmp')
  mkdirSync(nixTmp)
  mkdirSync(nativeTmp)
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const shared = { ...process.env, DONUT_CI_MAILBOX_ROOT: root }
  const { stdout } = await exec(process.execPath, [launcher, 'probe'], {
    cwd: checkout,
    env: { ...shared, TMPDIR: nixTmp },
  })
  const directory = JSON.parse(stdout.slice(receiptPrefix.length)).directory
  assert.equal(resolve(directory, '..'), resolve(root))

  const output = `warning: Git tree '${checkout}' is dirty\n<<running within nix env>>\n${stdout}`
  const ready = await configuredHook(
    'postToolUse',
    cursorInput('postToolUse', output),
    { ...shared, TMPDIR: nativeTmp }
  )
  assert.match(ready.additional_context, /CI_MONITOR_READY/)
})
