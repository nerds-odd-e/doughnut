# Codex CI notification adapter

Follow [ci-monitor.md](ci-monitor.md) for CI selection and failure recovery.

With `functions.exec`, `yield_control`, `notify`, `tools.exec_command`, and
`tools.write_stdin`, start one yielded observer cell when execution begins,
before the first push. Reuse `watching` and terminal `finished` entries on
reentry. If volatile handles are lost, recover the active PLAN's observer note
before considering replacement. Substitute verified repository, checkout, and
coordinator below:

```js
const key = 'ci-watch-execution:OWNER/REPO:main:COORDINATOR'
if (['watching', 'finished'].includes(load(key)?.status)) exit()
try {
  let result = await tools.exec_command({
    cmd: './scripts/run.sh node .agents/skills/execute-plan/scripts/ci-mailbox.mjs stream --execution OWNER/REPO main',
    workdir: '/ABSOLUTE/VERIFIED/CHECKOUT_ROOT',
    tty: true,
    yield_time_ms: 1000,
    max_output_tokens: 2000,
  })
  let tail = ''
  let directory
  let pid
  let terminal
  const events = []
  const consume = (chunk) => {
    const lines = `${tail}${chunk}`.split('\n')
    tail = lines.pop()
    for (const line of lines) {
      if (line.startsWith('CI_OBSERVER_RESULT ')) {
        terminal = JSON.parse(line.slice('CI_OBSERVER_RESULT '.length)).terminal
        continue
      }
      if (line.startsWith('CI_OBSERVER ')) {
        ;({ directory, pid } = JSON.parse(line.slice('CI_OBSERVER '.length)))
        continue
      }
      if (!line.startsWith('{')) continue
      const record = JSON.parse(line)
      if (record.event?.type?.startsWith('CI_')) events.push(record.event)
    }
  }
  const deliver = () => {
    for (const event of events.splice(0)) notify(event)
  }
  consume(result.output)
  store(key, {
    status: result.session_id ? 'watching' : 'finished',
    sessionId: result.session_id,
    directory,
    pid,
    tail,
    terminal,
  })
  await yield_control()
  deliver()
  while (
    result.session_id &&
    ['watching', 'stopped'].includes(load(key)?.status)
  ) {
    result = await tools.write_stdin({
      session_id: result.session_id,
      chars: '',
      yield_time_ms: 1000,
      max_output_tokens: 2000,
    })
    consume(result.output)
    const stopping = load(key)?.status === 'stopped'
    store(key, {
      status: stopping
        ? 'stopped'
        : result.session_id
          ? 'watching'
          : 'finished',
      sessionId: result.session_id,
      directory,
      pid,
      tail,
      terminal,
    })
    deliver()
  }
  if (load(key)?.status === 'stopped') exit()
  store(key, {
    status: 'finished',
    sessionId: undefined,
    directory,
    pid,
    tail,
    terminal,
  })
} catch (error) {
  if (load(key)?.status === 'stopped') exit()
  store(key, { ...load(key), status: 'lost' })
  notify({ type: 'CI_MONITOR_UNAVAILABLE', key, reason: String(error).slice(-1000) })
}
```

After startup, save receipt directory/PID, coordinator, and checkout in the
active PLAN before the first push; retain cell/session handles too. The parser
retains chunk tails, consumes initial output before yielding, then notifies
queued events. Subsequent reads notify immediately; awaited work keeps the cell
alive. Continue delegation after yielding. `notify` delivers at the coordinator's
next boundary without model polling. Do not repeatedly `wait`, assign a watching
agent, or broaden ordinary network/filesystem permissions.

[Codex async command hooks](https://learn.chatgpt.com/docs/hooks#run-hooks-in-the-background)
also deliver at the next safe model request in compatible versions; they cannot
wake an idle task. A background shell or file alone cannot notify. Without a
bridge, report that limitation once and continue; never substitute recurring AI
polling or claim notifications.

Stop at completion, Jidoka, or cancellation:

- With handles, retain the session, mark saved status `stopped`, and send
  Ctrl-C (`chars: '\u0003'`) through `tools.write_stdin` to that exact PTY;
  plain pipes do not support this. The stream converts SIGINT/SIGTERM into a
  mailbox stop and uses the shared finite terminal-result wait. Confirm process
  exit, let the bridge finish, and reap its cell. Cell termination alone proves
  no subprocess exit.
- Without handles, recover the PLAN note. Match coordinator/checkout and validate
  the saved directory's `request.json` root, repository, branch, and execution
  mode. Run `./scripts/run.sh node .agents/skills/execute-plan/scripts/ci-mailbox.mjs stop DIRECTORY`
  from that checkout. Read its terminal receipt and `result.json`; confirm the
  recorded PID disappears using a finite local `ps` wait. Never signal that PID.
  Missing/mismatched identity means no guessed stop, newest-mailbox lookup, or
  replacement launch. Older unidentified observers cannot be recovered. Stop
  errors, missing terminal evidence, or unconfirmed exit mean unresolved
  shutdown; never force termination or claim closure.

Preserve recorded failures and report unread events and `pendingCi: unobserved`,
not green CI. Consume delivered failures before completion; never wait for pending
CI. Keep acknowledgment and repair semantics unchanged. On resume, rearm only
absent, `stopped`, or `lost` observers after confirming the old process ended;
never restart terminal `finished` observers.

Normal and repair pushes retain the key, session, directory, process, and cell.
The observer discovers successive main pushes, persists events before streaming,
and owns observation until shutdown. Changed HEAD/SHA never requires setup again.
