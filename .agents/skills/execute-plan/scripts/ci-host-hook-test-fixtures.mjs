import { spawn, execFile } from 'node:child_process'
import { once } from 'node:events'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { promisify } from 'node:util'
import { receiptPrefix } from './ci-mailbox.mjs'

const exec = promisify(execFile)
const hook = fileURLToPath(new URL('./ci-host-hook.mjs', import.meta.url))

export function setup(root = '/test/donut') {
  const storage = mkdtempSync(join(tmpdir(), 'ci-hook-test-'))
  return { root, storage }
}

export const failure = { type: 'CI_FAILURE', runId: 42, attempt: 1 }

export const input = (host, directory, overrides = {}) => ({
  session_id: 'main',
  conversation_id: 'main',
  generation_id: 'coordinator-turn',
  transcript_path: '/test/main.jsonl',
  hook_event_name: host === 'cursor' ? 'postToolUse' : 'PostToolUse',
  tool_name: host === 'cursor' ? 'Shell' : 'Bash',
  tool_output: JSON.stringify({
    output: directory
      ? `${receiptPrefix}${JSON.stringify({ directory })}\n`
      : '',
  }),
  tool_response: {
    stdout: directory
      ? `${receiptPrefix}${JSON.stringify({ directory })}\n`
      : '',
  },
  ...overrides,
})

export const context = (output) =>
  output.additional_context ?? output.hookSpecificOutput?.additionalContext

export async function runHostHook(host, hookInput, { storage }) {
  const child = exec(process.execPath, [hook, host], {
    env: { ...process.env, DONUT_CI_MAILBOX_ROOT: storage },
    maxBuffer: 4 * 1024 * 1024,
  })
  child.child.stdin.end(JSON.stringify(hookInput))
  return JSON.parse((await child).stdout)
}

export async function interruptHostHookWhileWriting(
  host,
  hookInput,
  { storage }
) {
  const child = spawn(process.execPath, [hook, host], {
    env: { ...process.env, DONUT_CI_MAILBOX_ROOT: storage },
    stdio: ['pipe', 'pipe', 'pipe'],
  })
  child.stdin.end(JSON.stringify(hookInput))
  await once(child.stdout, 'readable')
  const exited = once(child, 'exit')
  child.kill('SIGKILL')
  await exited
}
