# Cursor and Claude Code notification adapters

Use this adapter only on Cursor or Claude Code. Both use the same detached
Node watcher and private temporary mailbox; only their hook JSON differs.
Follow [ci-monitor.md](ci-monitor.md) for CI selection and failure recovery.
The coordinator, not a work agent, starts and stops observers. Cursor and
Claude Code each own one execution observer for the whole execute-plan run,
reused across repeated setup, normal pushes, and repair pushes.

## Verify the host bridge once per execution

The repository ships `.cursor/hooks.json` and `.claude/settings.json`. Their
post-tool hooks inspect local mailbox files only: no network requests, model
calls, or waiting for CI. Their stop hooks deliver only already-ready events;
pending or successful CI never extends an agent turn.

Run this harmless probe from the checkout root using the normal Shell/Bash
tool, not a work agent or a tool that hides its stdout:

```sh
./scripts/run.sh node .agents/skills/execute-plan/scripts/ci-mailbox.mjs probe
```

The command prints a `CI_OBSERVER` receipt, **not** `CI_MONITOR_READY`. Proceed
with observation only if the host hook adds separate `CI_MONITOR_READY`
context. A receipt alone proves neither hook registration nor notification
delivery. Rerun the probe after a host/session change.

If readiness is missing, inspect the current host's hook errors, Node PATH,
project settings, workspace trust, and whether hooks are disabled. Preserve
other hooks and settings; do not bypass user approval or trust checks. Reload
the project's hook configuration through the host's supported UI or start a
new approved session if needed. Report an unavailable bridge once and continue
the plan without promising monitoring; do not replace it with AI polling.

The hook commands intentionally use an installed `node` directly (Node 20+,
standard library only). This is a narrow exception to the repo's Nix wrapper:
starting Nix on every tool boundary would delay ongoing work. Observer launch
and test commands still use `./scripts/run.sh` / the usual Nix environment.

## Start once and continue immediately

After readiness succeeds, start observation when execute-plan begins, before
the first push. Use the repository resolved from the actual `main` push
remote:

```sh
./scripts/run.sh node .agents/skills/execute-plan/scripts/ci-mailbox.mjs start --execution OWNER/REPO main
```

This starts a detached non-AI process and returns immediately. Retain the one
directory from its `CI_OBSERVER` receipt as the coordinator's execution
handle. The hook must add `CI observer attached to this coordinator`; absence
of that labelled context means observation is not connected. Re-entering setup,
including after a normal or repair push, reuses that directory and must not run
the launcher again. The observer discovers each later `main` push itself, so a
push changes neither its owner binding nor its process handle. Continue
delegation and plan execution immediately.

The next coordinator hook invocation after a result is ready adds the event to the owning
coordinator's context exactly once. Pending polls and successful CI add no
context. The mailbox is claimed by checkout, host, conversation, and worker
identity; Cursor additionally binds to the coordinator's `generation_id`
because its children can share the conversation ID, and `beforeSubmitPrompt`
updates that binding on a new user message; arbitrary child tool calls cannot
rebind it, and missing generation identity fails the readiness probe. Claude
Code isolates instead by `session_id` plus `agent_id`/`subagent_id`, so a
sub-agent sharing the coordinator's session cannot consume its notification.
Keep the same coordinator session when resuming; if replacing it, stop the old
observers using their recorded directories and register new ones in the new
session.

Mailboxes live outside the checkout under `/tmp/donut-ci-$UID`, not under the
process `TMPDIR`, so a Nix-wrapped launcher and a native hook share the same
observer directory. Stashing untracked work does not remove them. The watcher
never modifies Git state, pauses workers, or fixes code. On a delivered
failure, the coordinator follows the shared protocol: classify, obtain
quiescent handoffs from all writers, stash, delegate repair, wrap up and
push, restore, then resume. Use the host's available worker message/resume
handles. If a worker cannot be paused until its current command returns, wait
for that safe handoff before touching its working tree.

## Stop without waiting for CI

On completion, Jidoka, cancellation, or coordinator replacement, Cursor and
Claude Code each stop their one execution observer using the exact saved
directory:

```sh
./scripts/run.sh node .agents/skills/execute-plan/scripts/ci-mailbox.mjs stop /EXACT/RECORDED/MAILBOX
```

This signals cancellation, including an outstanding GitHub request or polling
timer. Confirm `result.json` in that directory reaches `stopped` or `finished`;
this is local process shutdown, not waiting for CI. The hook drains an
already-finished event even if stop was requested. Unread records remain in the
mailbox, and a deliberate stop reports pending CI as unobserved. Handle
delivered failures before claiming completion. Retain these small recovery
records for interrupted sessions; never kill by a broad process-name pattern.
A watcher also has the bounded lifetime in the shared contract if its
coordinator disappears without stopping it.

## Host contracts

- [Cursor hooks](https://cursor.com/docs/hooks): `postToolUse` receives
  `conversation_id` and JSON-stringified `tool_output`; the hook returns
  `additional_context`. The `stop` adapter returns `followup_message` only
  for an available event. It uses project-relative commands and does not
  depend on Claude compatibility mode.
- [Claude Code hooks](https://code.claude.com/docs/en/hooks): `PostToolUse`
  receives `session_id` and `tool_response`; the hook returns
  `hookSpecificOutput.additionalContext`. `Stop` uses `decision: block` with
  the event as its reason only once. Commands resolve via
  `CLAUDE_PROJECT_DIR`; local permissions/preferences remain in ignored
  `settings.local.json`. If Cursor also loads this configuration through
  third-party compatibility, the Claude adapter ignores its `cursor_version`
  payload so only the native Cursor adapter can claim notifications.

Neither bridge wakes an idle session for periodic polling. Delivery happens
at a tool/stop boundary of the ongoing coordinator. Native hook support must
pass the readiness probe on the installed host; do not infer it from the
product name or from merely running the script in a terminal.
