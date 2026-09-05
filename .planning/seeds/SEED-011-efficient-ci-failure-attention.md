---
id: SEED-011
status: dormant
planted: 2026-09-05
planted_during: retrospective of SEED-009 Story 2 publication execution, on developer request
trigger_when: before another long execute-plan run or when revising CI observation and repair coordination
scope: medium
---

# SEED-011: Detect CI failures without repeated AI coordination

## Why This Matters

For the developer supervising plan execution, CI observation should detect
failures promptly with no AI involvement in routine polling, while the
coordinator attends failures reliably without losing ongoing work or starting
overlapping repairs.

The completed notebook-publication execution exposed both overhead and delayed
attention. Its coordinator launched 14 observers for 13 revisions, including
one corrected launch. The model responses issuing those launches consumed
5,716 output tokens; this excludes SHA discovery, commentary, shutdown,
investigation, and repair. Ten launches regenerated the notification adapter.
The last four used ordinary process sessions without its notification bridge.
The first affected E2E job failed at 14:20:33 Singapore time and reached the
coordinator at 14:34:55, approximately 14 minutes later. The watcher itself
also waits for workflow completion before inspecting failed jobs.

Polling already uses no AI calls. The desired improvement is to remove repeated
model work around that polling and make failure delivery dependable. A known
failure can be recorded immediately even when handling it waits for ongoing
work to finish; detection, notification, and repair start are separate moments.

Developer clarification (2026-09-05): immediate interruption is not mandatory.
Finishing the current ongoing task before attending CI is acceptable if it is
more reliable for contemporary coding agents. Failures may accumulate under
either policy. The coordinator must know when a fix is already underway and
retain later failures for subsequent attention. No broad claim about model
capabilities is established by this one execution; evaluate the actual host
and representative agents before adding interruption complexity.

## Alternatives and Decision

1. **Defer:** retain the current observer. Polling remains token-free, but
   repeated launch/lifecycle work and the demonstrated delivery mistake remain.
2. **Smaller correction:** consistently use the existing notification bridge
   and verify its delivery once. This addresses the missing notifications, but
   leaves per-revision model coordination and workflow-completion latency.
3. **Manual or existing-tool workflow:** inspect CI at task boundaries. This
   can be an honest fallback when notification delivery is unavailable, but
   still needs repeated attention and can miss failures while work continues.
4. **Recommended direction:** observe the repository's main-branch CI once per
   execution, discovering relevant runs and their commits without repeated
   model setup. Deliver actionable failures through a verified notification
   path, with serialized repair and retained pending failures.

The strongest smaller alternative is a correct existing bridge. Preserve that
as a possible first implementation choice if it can meet the observable outcome;
the seed does not mandate a particular process, mailbox, hook, or API design.
Commit/run/attempt identities remain diagnostic facts even when the coordinator
no longer supplies a SHA for every launch.

## Story Decomposition

<a id="story-1"></a>

### 1. Notice relevant CI failures throughout execution with one setup

- **For / why:** The developer wants failures noticed without paying for
  repeated AI coordination after every push.
- **Evaluation:** Across several pushes during one execution, the coordinator
  establishes observation once. Pending and successful checks require no
  model turns. A completed failed job is delivered even if other jobs in that
  workflow still run. Delivery is demonstrated on the actual host, not inferred
  from a running process. Failed observation is reported once as lost coverage.
- **Value / learning:** Tests the highest-risk assumption: can this host deliver
  useful failure information to the ongoing coordinator without model polling?
  Compare setup/lifecycle model calls and generated tokens with the recorded
  baseline; separate these from necessary diagnosis and repair costs.
- **Effort hypothesis:** M — medium confidence; assumes an available host
  notification mechanism can be verified and reused.
- **Depends on:** none.
- **Safe stopping point:** Failures remain visible with the current repair
  workflow even if later coordination improvements are cancelled. Track relevant
  older unfinished runs as newer pushes arrive; watching only the newest run
  must not hide an older failure. Define a startup baseline to avoid replaying
  unrelated historical failures. Keep CI distinct from CD.

<a id="story-2"></a>

### 2. Finish ongoing work and resolve accumulated CI failures one repair at a time

- **For / why:** The developer wants a coding agent to handle CI failures
  reliably while preserving work and avoiding duplicate or nested repairs.
- **Evaluation:** A failure arrives during a bounded implementation or refactor
  task. Under the recommended initial policy, that task reaches its next safe
  handoff before repair begins. Before another task starts, the coordinator
  reviews pending failures. Further failures arriving during a fix remain
  queued; they do not start another repair or another stash cycle. After a fix,
  pending failures are checked against current code and the fix's focused proof.
  Distinct causes receive attention; demonstrated repetitions of the repaired
  cause do not trigger duplicate fixes.
- **Value / learning:** Tests whether explicit pending/repair-in-progress state
  makes coordination reliable without requiring immediate interruption across
  models. Notification latency and time to begin repair are evaluated separately.
- **Effort hypothesis:** M — medium confidence; assumes tasks already return
  bounded handoffs and repair ownership can survive a coordinator context reset.
- **Depends on:** Story 1's reliable delivery within this proposed ordering.
- **Safe stopping point:** One repair owns the checkout at a time. Do not stash
  beneath a live writer. Do not lose queued events on compaction or shutdown;
  report unresolved attention at handoff. Do not dismiss different run failures
  merely because they share a job name or because a newer run passed. Retain
  identities and diagnose equivalence from evidence.

## Ordering and Scope Reduction

First establish dependable low-overhead detection and delivery; then evaluate
serialized handling under accumulated failures. Story 2 is first to defer if
scope must shrink, while Story 1 must still preserve observed failure evidence
and use the existing repair workflow safely.

Immediate interruption is an optional policy to evaluate later, not a required
third story or a prerequisite for either outcome. The recommended initial
meaning of "finish the current task" is the currently delegated bounded task
through its safe handoff, before new task dispatch; it does not mean completing
the whole remaining plan. The developer has accepted delayed attendance but
has not selected this exact boundary as a final rule.

## Open Decisions

- Select the attention boundary: completed current delegated task, completed
  slice including wrap-up, or a safe pause when supported. Evaluate reliability
  and time to repair before choosing interruption; hard process termination is
  not an implied requirement.
- At startup, which already-running and most recently completed run should be
  included? Define the scope so current failures are seen without historical
  replay, including commits pushed by another session to the same branch.
- Should observation end with the execution and hand off pending work, or remain
  attached beyond completion? The current workflow stops without waiting for CI;
  persistent monitoring is not authorized by this capture.

## When to Surface

Before the next long multi-slice execution, or when revising observation across
Codex, Cursor, or Claude Code. No implementation, executable plan, or backlog
priority is selected by this seed.

## Breadcrumbs

- Developer's CI-watcher retrospective request and follow-up clarification in
  the notebook-publication execution task (2026-09-05).
- Reviewed feature execution through `dd1ca6415a`; observation introduced in
  `f4ed5cfd6a`, additional host bridges in `6506ecd5da`.
- `.agents/skills/execute-plan/references/ci-monitor.md` and
  `.agents/skills/execute-plan/references/ci-notify-hosts.md` — current contracts.
- `.agents/skills/execute-plan/scripts/watch-ci.mjs` — current observer.
- [SEED-010](SEED-010-execute-plan-wrap-up-token-efficiency.md) — general context,
  evidence-handoff, and log-volume improvements; their requirements stay there.
- Token evidence is launch-response output usage from this task's local session
  record, not total monitoring spend, billing cost, or a cross-model benchmark.
