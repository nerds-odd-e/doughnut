# New note audio startup readiness repair

CI run [38123730120](https://github.com/nerds-odd-e/doughnut/actions/runs/38123730120),
attempt 1, owned revision `f75da95cb40fda4c5bb9579ddb5ae22bbb1eec9f`, failed only
New note spoken-title creation in the note creation/update E2E job (42 passed).
The arrival marker became synchronous when range capture moved before startup;
the scenario's immediate cached audio could reach the previous retained worklet
handler before the new recorder connected. The helper returned before listening.

The sole repair in `noteCreationForm.speakTheTitle` waits for enabled “Stop
speaking the title”, the public listening state, before simulated audio delivery.
No frontend product change or typecheck trigger. Independent refactor found no
necessary changes; whitespace and selective formatting passed.

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
```

Temporary external-device setup: delay `getUserMedia` 1500ms only while the New
note form is present. Exact disposable patch:
`/tmp/dough-new-note-startup-diagnostic.patch`.
Unchanged helper: 4 pass / 1 fail, New note remains “Untitled” rather than hearing
the transcript. Same delay plus repair: 5 pass. Diagnostic removed, ordinary
mocks: 5 pass. Each command's terminal exit was observed (1, 0, 0). Logs:
`/tmp/dough-new-note-startup-red.log`, `-green.log`, `-final.log`.
CI displayed an empty field rather than the reproduction's initial “Untitled”;
both lost the current transcript. The controlled red/green experiment identifies
the readiness defect rather than relying on a passing retry.

The existing feature observes New note field text and subsequent creation,
existing-title renames, body conversion and retained-recording retry. No extra
scenario or permanent artificial latency is retained. Slice 3's work and proof
were paused and preserved with the installed CI stash command; its status stays
planned until ordinary delivery completes.
