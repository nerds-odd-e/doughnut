import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
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
  readFileSync(new URL('../../../../.claude/settings.json', import.meta.url))
).hooks

function claudeInput(event, receipt = '', overrides = {}) {
  const base = {
    session_id: 'claude-coordinator',
    hook_event_name: event,
    transcript_path: '/test/claude-coordinator.jsonl',
  }
  if (event === 'PostToolUse')
    Object.assign(base, {
      tool_name: 'Bash',
      tool_response: { stdout: receipt },
    })
  return { ...base, ...overrides }
}

async function configuredHook(event, input, env) {
  const command = hooks[event][0].hooks[0].command
  const child = exec('sh', ['-c', command], {
    env: { ...env, CLAUDE_PROJECT_DIR: checkout },
  })
  child.child.stdin.end(JSON.stringify(input))
  return JSON.parse((await child).stdout)
}

function createClaudeReplay(env) {
  let observer
  let launches = 0

  return {
    async readiness(receipt = '') {
      const { stdout } = await exec(process.execPath, [launcher, 'probe'], {
        cwd: checkout,
        env,
      })
      return configuredHook(
        'PostToolUse',
        claudeInput('PostToolUse', receipt || stdout),
        env
      )
    },
    async setup() {
      if (observer) return observer
      launches += 1
      const { stdout } = await exec(
        process.execPath,
        [launcher, 'start', '--execution', 'owner/repo', 'main', '60000'],
        { cwd: checkout, env }
      )
      const directory = JSON.parse(stdout.slice(receiptPrefix.length)).directory
      const attachment = await configuredHook(
        'PostToolUse',
        claudeInput('PostToolUse', stdout),
        env
      )
      observer = { directory, attachment }
      return observer
    },
    boundary(overrides = {}) {
      const event = overrides.hook_event_name ?? 'PostToolUse'
      return configuredHook(event, claudeInput(event, '', overrides), env)
    },
    launches: () => launches,
    async stop() {
      if (!observer) return
      return exec(process.execPath, [launcher, 'stop', observer.directory], {
        cwd: checkout,
        env,
      })
    },
  }
}

test('Claude Code reuses one execution observer through pushes and stops its exact mailbox', async (t) => {
  const root = mkdtempSync(join(tmpdir(), 'ci-claude-lifecycle-test-'))
  const bin = join(root, 'bin')
  writeBlockingGithubListCommand(bin)
  const env = {
    ...process.env,
    DONUT_CI_MAILBOX_ROOT: root,
    CI_TEST_ROOT: root,
    PATH: `${bin}:${process.env.PATH}`,
  }
  const replay = createClaudeReplay(env)
  t.after(async () => {
    await replay.stop()
    rmSync(root, { recursive: true, force: true })
  })

  const ready = await replay.readiness()
  assert.match(ready.hookSpecificOutput.additionalContext, /CI_MONITOR_READY/)
  assert.deepEqual(
    await configuredHook(
      'PostToolUse',
      claudeInput('PostToolUse', '', { cursor_version: 'test' }),
      env
    ),
    {}
  )

  const attached = await replay.setup()
  await waitForFile(join(root, 'github-request-started'))
  assert.match(
    attached.attachment.hookSpecificOutput.additionalContext,
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
  assert.deepEqual(await replay.boundary({ agent_id: 'child-agent' }), {})
  assert.match(
    (await replay.boundary()).hookSpecificOutput.additionalContext,
    /"type":"CI_FAILURE"/
  )
  publishMailboxEvent(attached.directory, {
    type: 'CI_MONITOR_UNAVAILABLE',
    repo: 'owner/repo',
    reason: 'fixture lost coverage',
  })
  assert.match(
    (await replay.boundary()).hookSpecificOutput.additionalContext,
    /"type":"CI_MONITOR_UNAVAILABLE"/
  )

  publishMailboxEvent(attached.directory, {
    type: 'CI_FAILURE',
    repo: 'owner/repo',
    runId: 43,
    attempt: 1,
  })
  const stopBoundary = await replay.boundary({ hook_event_name: 'Stop' })
  assert.equal(stopBoundary.decision, 'block')
  assert.match(stopBoundary.reason, /"runId":43/)

  const stopped = await replay.stop()
  await waitForFile(join(root, 'github-request-stopped'))
  const terminal = JSON.parse(
    stopped.stdout.slice(receiptPrefix.length)
  ).terminal
  assert.deepEqual(terminal, {
    status: 'stopped',
    coverage: { state: 'ended', pendingCi: 'unobserved' },
    evidence: { recordedThrough: 3, deliveredThrough: 3, unread: 0 },
  })
  assert.deepEqual(
    JSON.parse(readFileSync(join(attached.directory, 'result.json'))),
    terminal
  )
})
