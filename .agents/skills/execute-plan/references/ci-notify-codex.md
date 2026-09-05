# Codex CI notification adapter

Follow [ci-monitor.md](ci-monitor.md) for CI selection and failure recovery.

In a Codex runtime exposing `functions.exec`, `yield_control`, `notify`,
`tools.exec_command`, and `tools.write_stdin`, use one yielded JavaScript cell
for the whole execution observer. Start it when execute-plan begins, before the
first push. Re-entering setup, including after a normal or repair push, must
reuse the saved `watching` or terminal `finished` entry; it must not launch a
new process. The foreground mailbox command discovers successive main pushes,
persists each event before streaming its JSON record, and remains responsible
until execution shutdown. Replace the command arguments, `workdir`, and owner
key below with the verified repository, checkout root, and current coordinator
identity:

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
        directory = JSON.parse(line.slice('CI_OBSERVER '.length)).directory
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
    tail,
    terminal,
  })
} catch (error) {
  if (load(key)?.status === 'stopped') exit()
  store(key, { ...load(key), status: 'lost' })
  notify({ type: 'CI_MONITOR_UNAVAILABLE', key, reason: String(error).slice(-1000) })
}
```

The parser keeps an incomplete final line as `tail` across arbitrary tool-output
chunks. It consumes the initial `exec_command` output before yielding, then
delivers those queued complete records after the yield, so an early event is not
lost. Later complete records are notified after each foreground process read,
without waiting for observer exit. Keep the cell alive by awaiting its work.
After the initial yield, continue delegation and plan work; the JavaScript loop,
including process-output reads, does not require model turns. `notify` injects a
result for the active coordinator to handle at the next available boundary. Do
not repeatedly call `wait`, poll from the model, or spend a sub-agent on
watching. Never grant a watcher broader network or filesystem permissions than
normal tools allow.

[Codex async command hooks](https://learn.chatgpt.com/docs/hooks#run-hooks-in-the-background)
are another supported delivery mechanism in compatible versions: their
informational output reaches the next safe model request, and does not wake an
idle task. A plain background shell process or a file alone is **not** a
notification mechanism. If no bridge is exposed, report that limitation once
and continue execution; do not silently substitute recurring AI polling or
claim that an unconnected watcher can notify the coordinator.

Keep observer cell/session handles so they can be stopped at plan completion,
Jidoka, or user cancellation. To stop, retain its session handle, mark the saved
status `stopped`, and send Ctrl-C (`chars: '\u0003'`) with `tools.write_stdin`
to that exact session. The PTY enables this interruption; do not assume plain
pipes accept Ctrl-C. The stream translates SIGINT/SIGTERM into a mailbox stop,
so wait for its terminal result and report its unread-event count and
`pendingCi: unobserved` coverage rather than treating shutdown as green CI.
Confirm that the known observer process exited, then let the bridge finish and
reap its yielded cell. Terminating only the cell does not prove the subprocess
stopped. Consume already-delivered failures before claiming completion; never
wait for pending CI. On resume, rearm only absent, `stopped`, or `lost`
observers after confirming their old process has ended. Keep `watching` and
terminal `finished` entries deduplicated; do not restart an observer that
already delivered its result.

After every successful normal or repair push, keep the same key, session ID,
mailbox directory, process, and yielded cell. The observer discovers that push
on its next local poll; do not run the setup cell again merely because HEAD or
the pushed SHA changed.
