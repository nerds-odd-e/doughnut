# DearDough Process Findings

Compact open shared-process findings; project-only issues live in [DonutRetrospectiveFindings.md](DonutRetrospectiveFindings.md).
Full evidence and response tracking: [Open Dough catalog](https://github.com/terryyin/open-dough/blob/9d72ec5/docs/maintainer/finding-names.md).
Shipped responses are omitted locally; upstream effectiveness checks may remain open.
Occurrence details are recoverable from the exact Git snapshot below. Missing provenance is unknown;
current installed versions are not substituted for historical execution releases.

## ODF-030 — One-off profile capture treated as durable runner plumbing

Former local code: DD-013.
A one-off profile added durable runner options/tests that a later slice removed. Keep measurement-only plumbing disposable instead of creating cleanup-only delivery work.

### Occurrences

- Execution: quick/106-publish-large-notebooks-under-one-minute / 78c24f31bb; Tool: Cursor; Model: Cursor Grok 4.6; Open Dough release: 0.3.12.

## ODF-034 — CI observer started for a feature branch that this project's workflow never triggers on

Former local code: DD-017.
Observation watched branches excluded by the workflow and reported missing CI as pending. Verify actual branch trigger eligibility, not merely the workflow’s existence.

### Occurrences

- Execution: quick/108-publish-notebook-edits-faster / a6fcddacad; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.13.
- Execution: SEED-019 story 1 / quick/114-note-owned-memory-tracker-deletion / be43a2c1e5; Tool: Cursor; Model: GLM 5.2; Open Dough release: 0.3.15.
- Execution: SEED-031 story 1 / quick/147-resolve-deleted-failure-reports; Timestamp: 2026-09-18, ~20:00–22:00 +08:00 (all four slice deliveries); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.25.

## ODF-042 — Coordinator pre-filtered grep results for a test-only representation slice, missing sites the later field-removal slice had to fix

Former local code: DD-037.
Coordinator-filtered search results omitted two representation sites, shifting repairs into the later field-removal slice. The compile-time check caught them; delegation coverage remains the lesson.

### Occurrences

- Execution: SEED-019 story 1 / quick/114-note-owned-memory-tracker-deletion / be43a2c1e5; Tool: Cursor; Model: GLM 5.2; Open Dough release: 0.3.15.

## ODF-051 — Refactor subagent reported removing dead dependencies it did not actually remove

A refactor report claimed dead dependencies were removed, but the diff retained them. Coordinator inspection and a resumed pass corrected it; report text alone is insufficient evidence.

### Occurrences

- Execution: plan 100 `cursor/100-receive-web-note-moves` (SEED-009 story 25); Timestamp: 2026-09-15T13:52:00+08:00; Tool: Cursor; Model: glm-5.2-high; Open Dough release: unreleased.

## ODF-081 — Story Branch Mode edits made in the originating checkout got swept into an unrelated concurrent commit

Former local code: DD-061.
Plan edits targeted the originating checkout and entered another session’s commit. Content survived, but its execution branch and attribution did not. Reuse the resolved execution path for writes.

### Occurrences

- Execution: SEED-009 story 42 / quick/131-manually-validate-append-only-notebook-workflow; Timestamp: 2026-09-17T12:23:21+08:00; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-059 — Delegation guidance has no protocol for a subagent that dies mid-edit from an infrastructure error, leaving a silent partial change

Former local code: DD-062.
Host termination left a refactor agent’s partial edits without a handoff. Recovery required actual diff inspection; no lost work was reported. A reportless interruption needs explicit recovery ownership.

### Occurrences

- Execution: SEED-009 story 43 / quick/132-rebaseline-existing-notebooks / 301184431f; Timestamp: unknown (task-notification received 2026-09-17, exact time not captured; the error stated a session-limit reset at 4:50pm Asia/Singapore); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.24.

## ODF-082 — Coordinator kept editing the shared execution checkout while a delegated refactor subagent was inspecting the same files, forcing a wasted stop

Former local code: DD-063.
Coordinator edits raced a delegated refactor review in the same checkout, forcing a wasted stop and another pass. Keep that review’s working tree stable until handoff.

### Occurrences

- Execution: SEED-022 story 1 / quick/134-literal-note-title-recall; Timestamp: 2026-09-17, between approximately 17:29 and 17:37 +08:00 (the subagent was launched right after the slice-1 `pnpm backend:test_only` run completed and stopped before the slice-2 run, timestamped 17:37:42+08:00 in that run's Spring Boot startup log; the subagent's own elapsed time was reported as ~6 minutes); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-083 — A prior execution left the shared main checkout on its own feature branch with uncommitted work, blocking the next plan's execution setup

Former local code: DD-064.
Another execution left the shared checkout on its feature branch with 34 uncommitted files, blocking setup. Recovery preserved the work in its own worktree.

### Occurrences

- Execution: SEED-009 story 44 / quick/137-retire-notebook-rebaseline-migration; Timestamp: 2026-09-17, unknown exact time (diagnosed and repaired before this execution's delivery commit at 20:10:08 +08:00); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-084 — A concurrent session deleted an active Story Branch execution worktree and branch while a delegated subagent was mid-slice

Former local code: DD-072.
An active worktree and branch disappeared during delegation; the remover is unknown. No edits had been made, so recovery lost no work. Cleanup ownership and transient shared-tree observations remain concerns.

### Occurrences

- Execution: SEED-030 story 2 / quick/145-reset-notebook-git-history; Timestamp: 2026-09-18, ~17:06-17:20 +08:00 (between the plan 146 claim commit `c619aba43a` at 17:05:27 +08:00 and slice 1's delivery at 17:30 +08:00); Tool: Claude Code; Model: claude-opus-5; Open Dough release: unknown.

## ODF-085 — The documented `.claude/skills` runtime path did not exist at all in a freshly created execution worktree

Former local code: DD-074.
A missing .claude skill alias concealed an already-tracked same-checkout .agents runtime, causing copies or false unavailability; one execution left ten pushes unobserved. CI-delivery follow-up is queued upstream.

### Occurrences

- Execution: SEED-034 story 1 / quick/146-permanently-delete-trashed-content / 6f2a2ff1d8; Timestamp: 2026-09-18T17:06+08:00; Tool: Claude Code; Model: claude-opus-5; Open Dough release: unknown.
- Execution: SEED-035 story 1 / quick/148-cohesive-accepted-web-folder-changes / ea903668bb; Timestamp: unknown (during initial CI-observer setup, before the first slice was delegated); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-036 story 1 / quick/149-permanent-deletion-loose-ends / a3856444de; Timestamp: 2026-09-19T10:33+08:00 (during this execution's retrospective; the original miss happened at CI-observer setup, before slice 1); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-035 story 1 / quick/260922-paste-without-formatting / 0c5e5725a0; Timestamp: unknown (2026-09-20, during this execution's initial CI-observer setup, before the first slice was delegated); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-086 — The product backlog moved on the shared integration branch between the coordinator's read and its queue claim

Former local code: DD-075.
A concurrent Taken claim invalidated an earlier backlog read. Re-reading and asserting the exact move preserved both claims. Native startup acceptance is Taken upstream; no lost claim was observed.

### Occurrences

- Execution: SEED-034 story 1 / quick/146-permanently-delete-trashed-content / 6f2a2ff1d8; Timestamp: 2026-09-18T17:05:27+08:00; Tool: Claude Code; Model: claude-opus-5; Open Dough release: unknown.

## ODF-069 — The CI observer's fixed discovery-poll bound reports lost coverage for revisions whose CI run exists and later succeeds

Former local code: DD-076.
Four delivered revisions had real successful runs but remained unproved in observer records. Later diagnosis disproves “discovery never retries”; bounded listing remains a qualified cause. CI-delivery follow-up is queued upstream.

### Occurrences

- Execution: SEED-035 story 1 / quick/148-cohesive-accepted-web-folder-changes / ea903668bb; Timestamp: unknown; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-035 story 15 / quick/020-notebook-lfs-receive / 2dc0ce9478; Timestamp: 2026-09-24T09:50+08:00 (completion wait for f6b4578a15); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33. `complete-revision` timed out with all four registered revisions `undiscovered` while `gh run list` showed each completed `success`.
- Execution: SEED-035 story 1 / quick/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T12:20+08:00 (completion wait for a0ee337e40); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33. `complete-revision` on `story/browse-download-notebook-files` timed out with `70b3b67313`, `f41c291823`, `a0ee337e40` all `undiscovered`; `gh run list` showed runs 35951607689, 35953015884, 35953621136 completed `success`. The story-branch observer was started by hand (DD-107) after the first push.

## ODF-090 — Coordinator implemented a planned slice locally during multi-slice execution

Former local code: DD-097.
The coordinator implemented the first slice of a three-slice plan locally, applying the single-slice exception too broadly. No product defect was reported; the implementation handoff was missing.

### Occurrences

- Execution: quick/008-remove-zip-export / 38b5e1ef69; Timestamp: 2026-09-21T22:08:41+08:00; Tool: Cursor; Model: Cursor Grok 4.7; Open Dough release: 0.3.27.

## ODF-091 — Coordinator accepted its own refactor pass without a fresh refactor agent

Former local code: DD-098.
Three ordinary slice commits used coordinator self-review instead of a fresh refactor agent. No independent findings were recorded. This differs from interrupted-repair and post-refactor-correction omissions.

### Occurrences

- Execution: quick/008-remove-zip-export / 38b5e1ef69; Timestamp: 2026-09-21T22:08:41+08:00; Tool: Cursor; Model: Cursor Grok 4.7; Open Dough release: 0.3.27.

## DD-107 — Managed increment delivery left the first Claude Code publication unobserved because no guidance names the session identity it needs

`execution-increment-delivery.mjs deliver` returned `pendingCi: unobserved` ("host session identity is required to verify the notification bridge") although the Claude Code hook bridge was ready. The references document `--session-json` only in the usage line; recovery needed a manual probe, observer start, and `register-push`.

### Occurrences

- Execution: SEED-035 story 15 / quick/020-notebook-lfs-receive / 2dc0ce9478; Timestamp: 2026-09-24T08:49+08:00 (slice 1 delivery; its CI run was created 00:49:45Z); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: slice 1 delivery receipt `observation.reason`; later deliveries passing `--session-json '{"session_id":…}'` (from the session transcript path) reported `observation.state: reused`.
  - Observed effect: slice 1's SHA was registered by hand after its CI run already existed (`CI_DISCOVERY_DELAYED`); no coverage was lost afterwards.
  - Inference: the coordinator must discover its own session id outside guidance; a host without an obvious transcript path could leave every increment unobserved.
- Execution: SEED-035 story 1 / quick/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T11:28+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge"; recovered by `ci-mailbox.mjs probe` (hook added `CI_MONITOR_READY`), `start --execution … story/browse-download-notebook-files`, and `register-push`; slices 2 and 3 then reported `observation.state: reused` without `--session-json`.
  - Observed effect: slice 1's CI run was found late (`CI_DISCOVERY_DELAYED`); three extra coordinator calls; no later coverage loss.

## DD-108 — Queued startup receipt embeds the whole Git index and overflows the coordinator's tool output

`execution-start.mjs start` prints `beforeMaintenance.index` (the full staged index listing of the default checkout) inside its one-line JSON receipt. On this repository the receipt was 811 KB, so the host saved it to a file and showed only a 2 KB preview; the fields the coordinator must retain (`publishedSha`, `workspace`, `preparation`) happened to be in that preview.

### Occurrences

- Execution: SEED-035 story 1 / quick/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T11:09+08:00 (queued startup); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: startup call output "Output too large (811.3KB)"; preview shows `beforeMaintenance.result: deferred`, `reason: unclear-ownership`, then `index: "100644 … .agents/agent-map.md\n…"`.
  - Observed effect: the exact receipt the skill requires preserving was not fully visible in context; later fields (for example refresh results after the index) were unread.
  - Inference: a maintenance diagnostic that lists every tracked file scales with repository size; a count or digest would keep the receipt usable.

## Retention

- Highest allocated local number: 108
- Recovery: `33939aa45a17c08f5e6f8076178ce96437fdfbe8:DearDough.md` contains the complete pre-compaction log and earlier recovery references; `b0b385a184e96ecf8b0f6fc5572eaaab69bc8dad` preserves later history.
- Occurrence history is partial; full observations, effects, inference and historical provenance remain in that snapshot and the upstream catalog.
