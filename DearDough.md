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

## ODF-069 — The CI observer's fixed discovery-poll bound reports lost coverage for revisions whose CI run exists and later succeeds

Former local code: DD-076.
Four delivered revisions had real successful runs but remained unproved in observer records. Later diagnosis disproves “discovery never retries”; bounded listing remains a qualified cause. CI-delivery follow-up is queued upstream.

### Occurrences

- Execution: SEED-035 story 1 / quick/148-cohesive-accepted-web-folder-changes / ea903668bb; Timestamp: unknown; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-035 story 15 / quick/020-notebook-lfs-receive / 2dc0ce9478; Timestamp: 2026-09-24T09:50+08:00 (completion wait for f6b4578a15); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33. `complete-revision` timed out with all four registered revisions `undiscovered` while `gh run list` showed each completed `success`.
- Execution: SEED-035 story 1 / quick/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T12:20+08:00 (completion wait for a0ee337e40); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33. `complete-revision` on `story/browse-download-notebook-files` timed out with `70b3b67313`, `f41c291823`, `a0ee337e40` all `undiscovered`; `gh run list` showed runs 35951607689, 35953015884, 35953621136 completed `success`. The story-branch observer was started by hand (DD-107) after the first push.
- Execution: SEED-035 story 3 / quick/024-note-local-picture-file / f0cc15be6a; Timestamp: 2026-09-24T14:40+08:00 (completion wait for f0cc15be6a); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. `complete-revision` on `story/local-image-display` timed out with `f0cc15be6a` `undiscovered` (shutdown confirmed); `gh run list --commit` showed run 35964391742 completed `success`.
- Execution: SEED-035 story 14 / quick/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24T16:35+08:00 and 16:55+08:00 (completion waits for 44c90c8cfa and 22cea6694e); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. The first wait ended `observation_unavailable` after `CI_MONITOR_UNAVAILABLE` (a failed `gh run list`), with `44c90c8cfa` `undiscovered`, while runs 35974797130 and 35975568223 had completed **failure** (a CLI test). The failures were found only by a manual `gh run list`. The repair wait for `22cea6694e` timed out `undiscovered` while run 35976936886 (created about 2 minutes after the push) completed `success`.
- Execution: SEED-035 story 20 / quick/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24T19:10+08:00 (completion wait for 47a3bdd0ab); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. `complete-revision` on `main` timed out with all twelve registered revisions `undiscovered` (shutdown confirmed) after an early `CI_DISCOVERY_DELAYED`; `gh run list --branch main` showed "donut CI" push runs completed `success` for each checked revision, including 47a3bdd0ab (created 10:58:15Z).
- Execution: SEED-035 story 4 / quick/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24T22:25+08:00 (completion wait for f4d1ec8d5c); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. `complete-revision` on `main` timed out with all six registered revisions `undiscovered` (shutdown confirmed) after an early `CI_DISCOVERY_DELAYED`; `gh run list --branch main` showed "donut CI" runs completed `success` for 4250de93e1, a061808c28, 682259779a, 0e760d4933 and f4d1ec8d5c (created 14:02:40Z); the planning-only claim 09a299b665 had no run of its own.

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

## ODF-092 — Managed increment delivery left the first Claude Code publication unobserved because no guidance names the session identity it needs

Former local code: DD-107.

`execution-increment-delivery.mjs deliver` returned `pendingCi: unobserved` ("host session identity is required to verify the notification bridge") although the Claude Code hook bridge was ready. The references document `--session-json` only in the usage line; recovery needed a manual probe, observer start, and `register-push`.

### Occurrences

- Execution: SEED-035 story 15 / quick/020-notebook-lfs-receive / 2dc0ce9478; Timestamp: 2026-09-24T08:49+08:00 (slice 1 delivery; its CI run was created 00:49:45Z); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: slice 1 delivery receipt `observation.reason`; later deliveries passing `--session-json '{"session_id":…}'` (from the session transcript path) reported `observation.state: reused`.
  - Observed effect: slice 1's SHA was registered by hand after its CI run already existed (`CI_DISCOVERY_DELAYED`); no coverage was lost afterwards.
  - Inference: the coordinator must discover its own session id outside guidance; a host without an obvious transcript path could leave every increment unobserved.
- Execution: SEED-035 story 1 / quick/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T11:28+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge"; recovered by `ci-mailbox.mjs probe` (hook added `CI_MONITOR_READY`), `start --execution … story/browse-download-notebook-files`, and `register-push`; slices 2 and 3 then reported `observation.state: reused` without `--session-json`.
  - Observed effect: slice 1's CI run was found late (`CI_DISCOVERY_DELAYED`); three extra coordinator calls; no later coverage loss.
- Execution: SEED-035 story 3 / quick/024-note-local-picture-file / f0cc15be6a; Timestamp: 2026-09-24T14:25+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut story/local-image-display`, and `register-push` for f0cc15be6a.
  - Observed effect: same three-call manual recovery; the release update from 0.3.33 to 0.3.37 did not change this.
- Execution: SEED-035 story 14 / quick/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24T15:50+08:00 (slice 1 delivery; its CI run was created 07:50:15Z); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut exec/seed-035-story-14`, and `register-push`; later deliveries reported `observation.state: reused`.
  - Observed effect: the same three-call manual recovery as earlier occurrences.
- Execution: SEED-035 story 20 / quick/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~17:40+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut main`, and `register-push` for 5c2e95c367 and 5859e6d663; the next ten deliveries reported `observation.state: reused`.
  - Observed effect: the same three-call manual recovery; the claim and slice 1 were registered late (`CI_DISCOVERY_DELAYED`).
- Execution: SEED-035 story 4 / quick/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24, ~21:22+08:00 (slice 1 delivery; commit 21:21:16+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut main`, and `register-push` for 09a299b665 and 4250de93e1; later deliveries passing `--session-json '{"session_id":…}'` reported `observation.state: reused`.
  - Observed effect: the same manual recovery plus a read of `ci-host-bridge.mjs` to learn the flag's shape; the claim and slice 1 were discovered late (`CI_DISCOVERY_DELAYED`); unchanged in 0.3.38.

- Execution: SEED-035 story 19 / quick/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25T09:21+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`) and re-running `deliver` for the accepted SHA with `--session-json '{"session_id":…}'`, which reported `observation.state: attached`.
  - Observed effect: two extra calls; a simpler recovery than `start` plus `register-push`, still discovered only by reading the receipt.
- Execution: SEED-035 story 5 / quick/033-move-legacy-note-pictures / 4dad58408f; Timestamp: 2026-09-25, ~14:23+08:00 (slice 1 delivery; commit 14:23:10+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode, bridge probe already `CI_MONITOR_READY`); a re-run passing the original base was refused ("rebase left the pre-rebase SHA as the candidate"); a re-run with the accepted SHA as base and `--session-json` from `CLAUDE_CODE_SESSION_ID` reported `observation.state: reused`.
  - Observed effect: three extra calls (one refused) plus a `grep` of the delivery scripts to learn the flag's shape; later deliveries passing `--session-json` reported `reused`.

## ODF-099 — Queued startup receipt embeds the whole Git index and overflows the coordinator's tool output

Former local code: DD-108.

`execution-start.mjs start` prints `beforeMaintenance.index` (the full staged index listing of the default checkout) inside its one-line JSON receipt. On this repository the receipt was 811 KB, so the host saved it to a file and showed only a 2 KB preview; the fields the coordinator must retain (`publishedSha`, `workspace`, `preparation`) happened to be in that preview.

### Occurrences

- Execution: SEED-035 story 1 / quick/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T11:09+08:00 (queued startup); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: startup call output "Output too large (811.3KB)"; preview shows `beforeMaintenance.result: deferred`, `reason: unclear-ownership`, then `index: "100644 … .agents/agent-map.md\n…"`.
  - Observed effect: the exact receipt the skill requires preserving was not fully visible in context; later fields (for example refresh results after the index) were unread.
  - Inference: a maintenance diagnostic that lists every tracked file scales with repository size; a count or digest would keep the receipt usable.
- Execution: SEED-035 story 3 / quick/024-note-local-picture-file / f0cc15be6a; Timestamp: 2026-09-24T14:10+08:00 (queued startup); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: startup call output "Output too large (814.6KB)"; `beforeMaintenance.index` holds the full index listing.
  - Observed effect: the coordinator needed an extra `python3` call to read the receipt's later fields (`afterMaintenance: advanced`, `projectSetupRequired: true`).
- Execution: SEED-035 story 14 / quick/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24, ~15:45+08:00 (queued startup, between readiness commit 47df9474c2 and slice 1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: startup call output "Output too large (815.7KB)"; `beforeMaintenance.index` holds the full index listing.
  - Observed effect: an extra `python3` call was needed to read `afterMaintenance` and `projectSetupRequired`.
- Execution: SEED-035 story 20 / quick/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~17:28+08:00 (queued startup); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: startup call output "Output too large (816.5KB)"; `beforeMaintenance.index` and `afterMaintenance.index` hold the full index listing.
  - Observed effect: an extra `python3` call was needed to read `afterMaintenance` and `projectSetupRequired`.
- Execution: SEED-035 story 4 / quick/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24, ~21:17+08:00 (queued startup; Take commit 21:16:55+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: startup call output "Output too large (817.8KB)"; `beforeMaintenance.index` and `afterMaintenance.index` hold the full index listing.
  - Observed effect: an extra `python3` call was needed to read `afterMaintenance` and `projectSetupRequired`.

- Execution: SEED-035 story 19 / quick/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25T09:02+08:00 (queued startup; Take commit 09:01:48+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: startup call output "Output too large (817.8KB)"; `beforeMaintenance.index` holds the full index listing.
  - Observed effect: an extra `node` call was needed to read `afterMaintenance` and `projectSetupRequired`.
- Execution: SEED-035 story 5 / quick/033-move-legacy-note-pictures / 4dad58408f; Timestamp: 2026-09-25, ~14:19+08:00 (queued startup, Take commit 269a569079); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: startup call output "Output too large (816.6KB)"; `beforeMaintenance.index` holds the full index listing.
  - Observed effect: an extra `node` call was needed to read `afterMaintenance` and `projectSetupRequired`.

## ODF-110 — A readiness replay observed only the plan's named seam, not the rest of the slice's journey

Former local code: DD-109.

A plan was recorded not-ready because slice 3 relied on an inferred CLI path. The coordinator's disposable replay reproduced exactly the named seam (fast-forward with LFS smudge skipped, then fill-in) and recorded ready. The slice's own journey continued with a publish after the pull, and that publish failed in the CLI, forcing a mid-execution stop and an owner scope decision.

### Occurrences

- Execution: SEED-035 story 14 / quick/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24, ~15:35+08:00 (replay and readiness record 47df9474c2), failure observed ~16:05+08:00 (slice 3 E2E); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: research prompt scoped to "pull" risks only; its report noted "the publish check went only as far as the pointer blob being committed"; slice 3 E2E then failed at the second `donut notebook publish` ("Attachment at <commit> must be a Git LFS pointer…", `cli/src/commands/notebook/notebookPublishLfsSelection.ts`); plan recorded the stop in 1e2ed84c35.
  - Observed effect: one human round-trip and a scope change (CLI change, option A) that preparation could have surfaced before Take.
  - Inference: when resolving a readiness concern by observation, replay the slice's full promised journey (here pull, then publish), not only the mechanism the concern names; the replay's own "not covered" list was the signal.

## ODF-104 — Increment delivery reports the default-checkout refresh as deferred without a reason

Former local code: DD-110.

`execution-increment-delivery.mjs deliver` returned `maintenance: "deferred"` on every Trunk Mode publication with no reason or remote/head fields, while the startup receipt reports its maintenance result with `reason`, `remoteSha` and `head`.

### Occurrences

- Execution: SEED-035 story 20 / quick/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~17:40–18:55+08:00 (eleven increment deliveries); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: delivery receipts for 5859e6d663 … 47a3bdd0ab each show only `maintenance: deferred`; at the end the default checkout was clean at eb957e4e7e while origin/main was 47a3bdd0ab.
  - Observed effect: the coordinator could not tell whether the refresh was blocked (ownership, dirty tree) or just skipped, and the default checkout was left behind trunk.
  - Inference: surfacing the same maintenance record as startup would let the coordinator decide whether a manual refresh is safe.
- Execution: SEED-035 story 4 / quick/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24, ~21:22–22:03+08:00 (five increment deliveries); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: delivery receipts for 4250de93e1, a061808c28, 682259779a, 0e760d4933 and f4d1ec8d5c each show only `maintenance: deferred`, while startup's `afterMaintenance` had advanced the default checkout.
  - Observed effect: same as before; the coordinator could not tell why the default checkout was left behind trunk.

## ODF-111 — Refactor re-proof covered fewer consumers than the shared helper it changed

Former local code: DD-111.

A post-change refactor moved the backend test base's `pointerFor` and the LFS conversion service onto a new production method (`NotebookAttachmentContent.storeAsLfsPointer`) but reran only four backend test classes, although `pointerFor` is inherited by most `NotebookGit*` controller tests.

### Occurrences

- Execution: SEED-035 story 20 / quick/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~18:45+08:00 (slice 10 refactor return); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 10 refactor report ("Backend (conversion service and test base)": four `--tests` classes); the coordinator then ran the full backend suite (2668 tests green) before committing 47a3bdd0ab.
  - Observed effect: one extra full-suite run by the coordinator; no defect found.
  - Inference: the refactor contract's caller analysis was applied to production callers but not to inherited test-support consumers. A similar broad refactor in slice 5 took about 35 minutes against a 10-minute slice limit; it was valuable cleanup but was not escalated.

## ODF-112 — The CI observer delivered no failure for failed story-branch runs, so later slices were built on a red branch

Former local code: DD-112.

The attached Story Branch observer recorded only one `CI_DISCOVERY_DELAYED` event while two later registered revisions failed CI. The failures were found only by a manual `gh run list` at the planned stop boundary.

### Occurrences

- Execution: SEED-035 story 19 / quick/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25, runs failed ~09:40–10:03+08:00, found ~10:50+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: runs 36083055193 (0d84006f7d) and 36084271448 (9ad0b9bf9f) failed "Backend Unit tests"; mailbox `/tmp/dough-ci-501/watch-7llTFe/events` held only sequence 1 (`CI_DISCOVERY_DELAYED` for 0284ea7f52); both deliveries had reported `observation.state: reused`. A later `CI_MONITOR_UNAVAILABLE` (network error on `gh run list`) arrived during the repair.
  - Observed effect: slices 3 and 4 were implemented, refactored and published on a failing branch for about an hour; the repair then had to cover three failed revisions.
  - Later evidence: at completion, `complete-revision` for aba0dc2509 returned `unresolvedReason: timeout` with the revision still `undiscovered`, while `gh run list` showed that run `completed success`.
  - Inference: cause unverified (discovery after the delay advisory, per-revision registration, or hook delivery); a manual `gh run list` check before each delegation would have caught it one slice later.

## ODF-113 — A failure was called pre-existing by comparing against a revision that already contained this execution's earlier slices

Former local code: DD-113.

An implementation agent reported a full-suite heap exhaustion as "also on the base commit", using the previous slice's tip rather than a known-green revision; the coordinator repeated that label to the owner before CI history showed slice 1 and main were green.

### Occurrences

- Execution: SEED-035 story 19 / quick/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25T10:44+08:00 (slice 4 return; commit e9cbbc6547); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 4 report ("same thing happens on the unchanged base commit `9ad0b9bf9f`"); CI runs green for 0284ea7f52 and main 9d2d091d0a; heap dump later traced the leak to slice 2's large LFS test payloads retained by `InMemoryNotebookAttachmentContent`.
  - Observed effect: the first stop report told the owner the failure was pre-existing and out of scope; the correction came only after the CI check.
  - Inference: "pre-existing" needs a baseline from before this execution's first change (claim revision or last green CI), not the prior slice.

## Retention

- Highest allocated local number: 113
- Recovery: `33939aa45a17c08f5e6f8076178ce96437fdfbe8:DearDough.md` contains the complete pre-compaction log and earlier recovery references; `b0b385a184e96ecf8b0f6fc5572eaaab69bc8dad` preserves later history.
- Occurrence history is partial; full observations, effects, inference and historical provenance remain in that snapshot and the upstream catalog.
