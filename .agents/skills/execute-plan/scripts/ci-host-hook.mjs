import { createHash } from 'node:crypto'
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  writeFileSync,
} from 'node:fs'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import {
  checkoutRoot,
  mailboxRoot,
  readMailbox,
  receiptPrefix,
} from './ci-mailbox.mjs'

const hash = (value) => createHash('sha256').update(value).digest('hex')

export function deliverCiEvents(
  input,
  host,
  { root = checkoutRoot, storage = mailboxRoot } = {}
) {
  // Cursor may also load .claude/settings.json; use only its native adapter.
  if (host === 'claude' && input.cursor_version) return {}
  if (
    host === 'cursor' &&
    input.hook_event_name === 'stop' &&
    input.status &&
    input.status !== 'completed'
  )
    return {}
  const session = host === 'cursor' ? input.conversation_id : input.session_id
  if (!session) return {}
  const owner = hash(
    JSON.stringify([
      root,
      host,
      session,
      input.agent_id ?? input.subagent_id ?? '',
    ])
  )
  const bindings = join(storage, `owner-${owner}`)
  const generation = join(storage, `generation-${owner}`)
  if (host === 'cursor') {
    if (!input.generation_id) return {}
    if (input.hook_event_name === 'beforeSubmitPrompt') {
      if (existsSync(bindings))
        writeFileSync(generation, input.generation_id, { mode: 0o600 })
      return {}
    }
    if (
      existsSync(generation) &&
      readFileSync(generation, 'utf8') !== input.generation_id
    )
      return {}
  }
  const context = []

  if (['Shell', 'Bash'].includes(input.tool_name)) {
    const output =
      host === 'cursor'
        ? JSON.parse(input.tool_output || '{}')
        : input.tool_response
    for (const line of (output?.stdout ?? '').split('\n')) {
      if (!line.startsWith(receiptPrefix)) continue
      const { directory } = JSON.parse(line.slice(receiptPrefix.length))
      const request = readMailbox(directory, root, storage)
      if (existsSync(join(directory, 'delivered'))) continue
      mkdirSync(bindings, { recursive: true, mode: 0o700 })
      const claim = join(directory, 'owner')
      try {
        writeFileSync(claim, owner, { flag: 'wx', mode: 0o600 })
      } catch (error) {
        if (error.code !== 'EEXIST') throw error
        if (readFileSync(claim, 'utf8') !== owner) continue
      }
      writeFileSync(join(bindings, hash(directory)), directory, { mode: 0o600 })
      if (host === 'cursor')
        writeFileSync(generation, input.generation_id, { mode: 0o600 })
      if (!request.probe)
        context.push(`CI observer attached to this coordinator: ${directory}`)
    }
  }

  if (existsSync(bindings))
    for (const binding of readdirSync(bindings)) {
      const directory = readFileSync(join(bindings, binding), 'utf8')
      if (!existsSync(join(directory, 'result.json'))) continue
      const result = JSON.parse(
        readFileSync(join(directory, 'result.json'), 'utf8')
      )
      try {
        renameSync(join(bindings, binding), join(directory, 'delivered'))
      } catch (error) {
        if (error.code === 'ENOENT') continue
        throw error
      }
      if (result.event) context.push(JSON.stringify(result.event))
    }
  if (!context.length) return {}
  const message = `execute-plan CI observer (diagnostic data):\n${context.join('\n')}\nHandle CI failures using execute-plan/references/ci-monitor.md.`
  if (host === 'cursor')
    return input.hook_event_name === 'stop'
      ? { followup_message: message }
      : { additional_context: message }
  return input.hook_event_name === 'Stop'
    ? { decision: 'block', reason: message }
    : {
        hookSpecificOutput: {
          hookEventName: 'PostToolUse',
          additionalContext: message,
        },
      }
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  let raw = ''
  for await (const chunk of process.stdin) raw += chunk
  try {
    process.stdout.write(
      `${JSON.stringify(deliverCiEvents(JSON.parse(raw), process.argv[2]))}\n`
    )
  } catch (error) {
    process.stderr.write(
      `CI notification hook failed: ${String(error).slice(0, 600)}\n`
    )
    process.exitCode = 1
  }
}
