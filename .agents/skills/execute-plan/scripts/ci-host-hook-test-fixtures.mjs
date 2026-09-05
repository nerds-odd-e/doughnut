import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { receiptPrefix } from './ci-mailbox.mjs'

export function setup() {
  const storage = mkdtempSync(join(tmpdir(), 'ci-hook-test-'))
  return { root: '/test/donut', storage }
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
