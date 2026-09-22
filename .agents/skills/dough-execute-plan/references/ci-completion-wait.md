# Completion-wait command and receipt mechanics

[CI observation and repair](ci-monitor.md#await-the-applicable-revision-at-completion)
owns when this single completion wait is required and how its outcome affects
execution. This reference supplies only the local command and receipt mechanics.

Invoke the reader with the exact retained mailbox and accepted revision:

```sh
node '/ABSOLUTE/RESOLVED/SKILL/scripts/ci-mailbox.mjs' await-revision '/EXACT/RECORDED/MAILBOX' FULL_ACCEPTED_SHA
```

The command stays silent while pending and uses its fixed ten-minute bound from
invocation. A discovery-delay advisory neither renews nor ends that bound. The
reader uses local mailbox evidence only: it does not contact the provider,
acknowledge notifications, stop observation, or cancel execution. Never invoke
it after ordinary slice delivery, assign an agent to poll it, or substitute the
observer's eight-hour lifetime for this bound.

Inspect the single `CI_OBSERVER` receipt and retain its requested revision,
target, effective evidence, and result together:

- `verdict: "success"` satisfies the completion observation.
- `verdict: "failure"` ends the wait without waiving the failure.
- `unresolvedReason` is one of `timeout`, `observation_unavailable`,
  `wait_cancelled`, `incomplete`, `evidence_unreadable`, or
  `missing_registration`; it ends the wait without establishing success.

Return that complete receipt to the owning procedure. Do not interpret a
successful local command exit as a successful CI verdict.
