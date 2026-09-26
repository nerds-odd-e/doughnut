# DearDough Process Findings

Compact open shared-process findings; project-only issues live in [DonutRetrospectiveFindings.md](DonutRetrospectiveFindings.md).
Full evidence and response tracking: [Open Dough catalog](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Shipped responses are omitted locally; upstream effectiveness checks may remain open.
Occurrence details are recoverable from the exact Git snapshot below. Missing provenance is unknown;
current installed versions are not substituted for historical execution releases.

## ODF-030 — One-off profile capture treated as durable runner plumbing

Former local code: DD-013.
A one-off profile added durable runner options/tests that a later slice removed. Keep measurement-only plumbing disposable instead of creating cleanup-only delivery work.

### Occurrences

- Execution: slice-plans/106-publish-large-notebooks-under-one-minute / 78c24f31bb; Tool: Cursor; Model: Cursor Grok 4.6; Open Dough release: 0.3.12.

## ODF-034 — CI observer started for a feature branch that this project's workflow never triggers on

Former local code: DD-017.
Observation watched branches excluded by the workflow and reported missing CI as pending. Verify actual branch trigger eligibility, not merely the workflow’s existence.

### Occurrences

- Execution: slice-plans/108-publish-notebook-edits-faster / a6fcddacad; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.13.
- Execution: SEED-019 story 1 / slice-plans/114-note-owned-memory-tracker-deletion / be43a2c1e5; Tool: Cursor; Model: GLM 5.2; Open Dough release: 0.3.15.
- Execution: SEED-031 story 1 / slice-plans/147-resolve-deleted-failure-reports; Timestamp: 2026-09-18, ~20:00–22:00 +08:00 (all four slice deliveries); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.25.

## ODF-042 — Coordinator pre-filtered grep results for a test-only representation slice, missing sites the later field-removal slice had to fix

Former local code: DD-037.
Coordinator-filtered search results omitted two representation sites, shifting repairs into the later field-removal slice. The compile-time check caught them; delegation coverage remains the lesson.

### Occurrences

- Execution: SEED-019 story 1 / slice-plans/114-note-owned-memory-tracker-deletion / be43a2c1e5; Tool: Cursor; Model: GLM 5.2; Open Dough release: 0.3.15.

## ODF-051 — Refactor subagent reported removing dead dependencies it did not actually remove

A refactor report claimed dead dependencies were removed, but the diff retained them. Coordinator inspection and a resumed pass corrected it; report text alone is insufficient evidence.

### Occurrences

- Execution: plan 100 `cursor/100-receive-web-note-moves` (SEED-009 story 25); Timestamp: 2026-09-15T13:52:00+08:00; Tool: Cursor; Model: glm-5.2-high; Open Dough release: unreleased.

## ODF-081 — Story Branch Mode edits made in the originating checkout got swept into an unrelated concurrent commit

Former local code: DD-061.
Plan edits targeted the originating checkout and entered another session’s commit. Content survived, but its execution branch and attribution did not. Reuse the resolved execution path for writes.

### Occurrences

- Execution: SEED-009 story 42 / slice-plans/131-manually-validate-append-only-notebook-workflow; Timestamp: 2026-09-17T12:23:21+08:00; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-059 — Delegation guidance has no protocol for a subagent that dies mid-edit from an infrastructure error, leaving a silent partial change

Former local code: DD-062.
Host termination left a refactor agent’s partial edits without a handoff. Recovery required actual diff inspection; no lost work was reported. A reportless interruption needs explicit recovery ownership.

### Occurrences

- Execution: SEED-009 story 43 / slice-plans/132-rebaseline-existing-notebooks / 301184431f; Timestamp: unknown (task-notification received 2026-09-17, exact time not captured; the error stated a session-limit reset at 4:50pm Asia/Singapore); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: 0.3.24.

## ODF-082 — Coordinator kept editing the shared execution checkout while a delegated refactor subagent was inspecting the same files, forcing a wasted stop

Former local code: DD-063.
Coordinator edits raced a delegated refactor review in the same checkout, forcing a wasted stop and another pass. Keep that review’s working tree stable until handoff.

### Occurrences

- Execution: SEED-022 story 1 / slice-plans/134-literal-note-title-recall; Timestamp: 2026-09-17, between approximately 17:29 and 17:37 +08:00 (the subagent was launched right after the slice-1 `pnpm backend:test_only` run completed and stopped before the slice-2 run, timestamped 17:37:42+08:00 in that run's Spring Boot startup log; the subagent's own elapsed time was reported as ~6 minutes); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-083 — A prior execution left the shared main checkout on its own feature branch with uncommitted work, blocking the next plan's execution setup

Former local code: DD-064.
Another execution left the shared checkout on its feature branch with 34 uncommitted files, blocking setup. Recovery preserved the work in its own worktree.

### Occurrences

- Execution: SEED-009 story 44 / slice-plans/137-retire-notebook-rebaseline-migration; Timestamp: 2026-09-17, unknown exact time (diagnosed and repaired before this execution's delivery commit at 20:10:08 +08:00); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-084 — A concurrent session deleted an active Story Branch execution worktree and branch while a delegated subagent was mid-slice

Former local code: DD-072.
An active worktree and branch disappeared during delegation; the remover is unknown. No edits had been made, so recovery lost no work. Cleanup ownership and transient shared-tree observations remain concerns.

### Occurrences

- Execution: SEED-030 story 2 / slice-plans/145-reset-notebook-git-history; Timestamp: 2026-09-18, ~17:06-17:20 +08:00 (between the plan 146 claim commit `c619aba43a` at 17:05:27 +08:00 and slice 1's delivery at 17:30 +08:00); Tool: Claude Code; Model: claude-opus-5; Open Dough release: unknown.

## ODF-085 — The documented `.claude/skills` runtime path did not exist at all in a freshly created execution worktree

Former local code: DD-074.
A missing .claude skill alias concealed an already-tracked same-checkout .agents runtime, causing copies or false unavailability; one execution left ten pushes unobserved. Response and remaining observation limits are tracked upstream.

### Occurrences

- Execution: SEED-034 story 1 / slice-plans/146-permanently-delete-trashed-content / 6f2a2ff1d8; Timestamp: 2026-09-18T17:06+08:00; Tool: Claude Code; Model: claude-opus-5; Open Dough release: unknown.
- Execution: SEED-035 story 1 / slice-plans/148-cohesive-accepted-web-folder-changes / ea903668bb; Timestamp: unknown (during initial CI-observer setup, before the first slice was delegated); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-036 story 1 / slice-plans/149-permanent-deletion-loose-ends / a3856444de; Timestamp: 2026-09-19T10:33+08:00 (during this execution's retrospective; the original miss happened at CI-observer setup, before slice 1); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-035 story 1 / slice-plans/260922-paste-without-formatting / 0c5e5725a0; Timestamp: unknown (2026-09-20, during this execution's initial CI-observer setup, before the first slice was delegated); Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.

## ODF-069 — The CI observer's fixed discovery-poll bound reports lost coverage for revisions whose CI run exists and later succeeds

Former local code: DD-076.
Four delivered revisions had real successful runs but remained unproved in observer records. Later diagnosis disproves “discovery never retries”; bounded listing remains a qualified cause. Response and remaining observation limits are tracked upstream.

### Occurrences

- Execution: SEED-035 story 1 / slice-plans/148-cohesive-accepted-web-folder-changes / ea903668bb; Timestamp: unknown; Tool: Claude Code; Model: claude-sonnet-5; Open Dough release: unknown.
- Execution: SEED-035 story 15 / slice-plans/020-notebook-lfs-receive / 2dc0ce9478; Timestamp: 2026-09-24T09:50+08:00 (completion wait for f6b4578a15); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33. `complete-revision` timed out with all four registered revisions `undiscovered` while `gh run list` showed each completed `success`.
- Execution: SEED-035 story 1 / slice-plans/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T12:20+08:00 (completion wait for a0ee337e40); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33. `complete-revision` on `story/browse-download-notebook-files` timed out with `70b3b67313`, `f41c291823`, `a0ee337e40` all `undiscovered`; `gh run list` showed runs 35951607689, 35953015884, 35953621136 completed `success`. The story-branch observer was started by hand (DD-107) after the first push.
- Execution: SEED-035 story 3 / slice-plans/024-note-local-picture-file / f0cc15be6a; Timestamp: 2026-09-24T14:40+08:00 (completion wait for f0cc15be6a); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. `complete-revision` on `story/local-image-display` timed out with `f0cc15be6a` `undiscovered` (shutdown confirmed); `gh run list --commit` showed run 35964391742 completed `success`.
- Execution: SEED-035 story 14 / slice-plans/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24T16:35+08:00 and 16:55+08:00 (completion waits for 44c90c8cfa and 22cea6694e); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. The first wait ended `observation_unavailable` after `CI_MONITOR_UNAVAILABLE` (a failed `gh run list`), with `44c90c8cfa` `undiscovered`, while runs 35974797130 and 35975568223 had completed **failure** (a CLI test). The failures were found only by a manual `gh run list`. The repair wait for `22cea6694e` timed out `undiscovered` while run 35976936886 (created about 2 minutes after the push) completed `success`.
- Execution: SEED-035 story 20 / slice-plans/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24T19:10+08:00 (completion wait for 47a3bdd0ab); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37. `complete-revision` on `main` timed out with all twelve registered revisions `undiscovered` (shutdown confirmed) after an early `CI_DISCOVERY_DELAYED`; `gh run list --branch main` showed "donut CI" push runs completed `success` for each checked revision, including 47a3bdd0ab (created 10:58:15Z).
- Execution: SEED-035 story 4 / slice-plans/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24T22:25+08:00 (completion wait for f4d1ec8d5c); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. `complete-revision` on `main` timed out with all six registered revisions `undiscovered` (shutdown confirmed) after an early `CI_DISCOVERY_DELAYED`; `gh run list --branch main` showed "donut CI" runs completed `success` for 4250de93e1, a061808c28, 682259779a, 0e760d4933 and f4d1ec8d5c (created 14:02:40Z); the planning-only claim 09a299b665 had no run of its own.

- Execution: slice-plans/037-fold-picture-attach-step-into-upload / 1b822a6bf7; Timestamp: 2026-09-25T23:37+08:00 (completion wait for 1b822a6bf7); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38. `complete-revision` on `story/037-fold-picture-attach-step-into-upload` timed out with `1b822a6bf7` `undiscovered` (shutdown confirmed; `CI_DISCOVERY_DELAYED` delivered afterwards); `gh run list --branch` showed "donut CI" run 36154138303 (created 15:26:06Z) completed `success`.
- Execution: SEED-039 story 4 / slice-plans/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26, completion check started ~09:45+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: `complete-revision` for fe990eff58 on observer /tmp/dough-ci-501/watch-UaGJbW returned `unresolvedReason: timeout` with 70dd09501d, 8df4a87262 and fe990eff58 all `undiscovered`; `gh run list --branch story/faster-frontend-unit-tests` shows `donut CI` success for 8df4a87262 (run 36208396723, created 01:25:55Z), 70dd09501d (36207506131) and c9347a9ee3 (36206320436); fe990eff58 touches only `.planning/**`, which the workflow ignores.
  - Observed effect: a ten-minute wait ended without a verdict although every applicable run had already succeeded; the coordinator confirmed green with `gh run list`.

## ODF-090 — Coordinator implemented a planned slice locally during multi-slice execution

Former local code: DD-097.
The coordinator implemented the first slice of a three-slice plan locally, applying the single-slice exception too broadly. No product defect was reported; the implementation handoff was missing.

### Occurrences

- Execution: slice-plans/008-remove-zip-export / 38b5e1ef69; Timestamp: 2026-09-21T22:08:41+08:00; Tool: Cursor; Model: Cursor Grok 4.7; Open Dough release: 0.3.27.

## ODF-091 — Coordinator accepted its own refactor pass without a fresh refactor agent

Former local code: DD-098.
Three ordinary slice commits used coordinator self-review instead of a fresh refactor agent. No independent findings were recorded. This differs from interrupted-repair and post-refactor-correction omissions.

### Occurrences

- Execution: slice-plans/008-remove-zip-export / 38b5e1ef69; Timestamp: 2026-09-21T22:08:41+08:00; Tool: Cursor; Model: Cursor Grok 4.7; Open Dough release: 0.3.27.

## ODF-092 — Managed increment delivery left the first Claude Code publication unobserved because no guidance names the session identity it needs

Former local code: DD-107.

`execution-increment-delivery.mjs deliver` returned `pendingCi: unobserved` ("host session identity is required to verify the notification bridge") although the Claude Code hook bridge was ready. The references document `--session-json` only in the usage line; recovery needed a manual probe, observer start, and `register-push`.

### Occurrences

- Execution: SEED-035 story 15 / slice-plans/020-notebook-lfs-receive / 2dc0ce9478; Timestamp: 2026-09-24T08:49+08:00 (slice 1 delivery; its CI run was created 00:49:45Z); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: slice 1 delivery receipt `observation.reason`; later deliveries passing `--session-json '{"session_id":…}'` (from the session transcript path) reported `observation.state: reused`.
  - Observed effect: slice 1's SHA was registered by hand after its CI run already existed (`CI_DISCOVERY_DELAYED`); no coverage was lost afterwards.
  - Inference: the coordinator must discover its own session id outside guidance; a host without an obvious transcript path could leave every increment unobserved.
- Execution: SEED-035 story 1 / slice-plans/022-browse-download-notebook-files / 70b3b67313; Timestamp: 2026-09-24T11:28+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.33.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge"; recovered by `ci-mailbox.mjs probe` (hook added `CI_MONITOR_READY`), `start --execution … story/browse-download-notebook-files`, and `register-push`; slices 2 and 3 then reported `observation.state: reused` without `--session-json`.
  - Observed effect: slice 1's CI run was found late (`CI_DISCOVERY_DELAYED`); three extra coordinator calls; no later coverage loss.
- Execution: SEED-035 story 3 / slice-plans/024-note-local-picture-file / f0cc15be6a; Timestamp: 2026-09-24T14:25+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut story/local-image-display`, and `register-push` for f0cc15be6a.
  - Observed effect: same three-call manual recovery; the release update from 0.3.33 to 0.3.37 did not change this.
- Execution: SEED-035 story 14 / slice-plans/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24T15:50+08:00 (slice 1 delivery; its CI run was created 07:50:15Z); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut exec/seed-035-story-14`, and `register-push`; later deliveries reported `observation.state: reused`.
  - Observed effect: the same three-call manual recovery as earlier occurrences.
- Execution: SEED-035 story 20 / slice-plans/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~17:40+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut main`, and `register-push` for 5c2e95c367 and 5859e6d663; the next ten deliveries reported `observation.state: reused`.
  - Observed effect: the same three-call manual recovery; the claim and slice 1 were registered late (`CI_DISCOVERY_DELAYED`).
- Execution: SEED-035 story 4 / slice-plans/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24, ~21:22+08:00 (slice 1 delivery; commit 21:21:16+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut main`, and `register-push` for 09a299b665 and 4250de93e1; later deliveries passing `--session-json '{"session_id":…}'` reported `observation.state: reused`.
  - Observed effect: the same manual recovery plus a read of `ci-host-bridge.mjs` to learn the flag's shape; the claim and slice 1 were discovered late (`CI_DISCOVERY_DELAYED`); unchanged in 0.3.38.

- Execution: SEED-035 story 19 / slice-plans/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25T09:21+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`) and re-running `deliver` for the accepted SHA with `--session-json '{"session_id":…}'`, which reported `observation.state: attached`.
  - Observed effect: two extra calls; a simpler recovery than `start` plus `register-push`, still discovered only by reading the receipt.
- Execution: SEED-035 story 5 / slice-plans/033-move-legacy-note-pictures / 4dad58408f; Timestamp: 2026-09-25, ~14:23+08:00 (slice 1 delivery; commit 14:23:10+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode, bridge probe already `CI_MONITOR_READY`); a re-run passing the original base was refused ("rebase left the pre-rebase SHA as the candidate"); a re-run with the accepted SHA as base and `--session-json` from `CLAUDE_CODE_SESSION_ID` reported `observation.state: reused`.
  - Observed effect: three extra calls (one refused) plus a `grep` of the delivery scripts to learn the flag's shape; later deliveries passing `--session-json` reported `reused`.

- Execution: SEED-035 story 17 / slice-plans/034-book-source-as-notebook-file / 36eb15caaa; Timestamp: 2026-09-25, ~15:55+08:00 (slice 1 delivery; commit 15:55:12+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 and slice 7 (96c756d531) receipts `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode); recovered after slice 1 by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut story/book-source-as-notebook-file`, and `register-push` for 36eb15caaa; slices 2-6 reported `reused`.
  - Observed effect: the same three-call manual recovery; after the observer ended (see DD-115) the slice 7 delivery could not reattach without the session identity and stayed unobserved.

- Execution: SEED-035 story 18 / slice-plans/036-remove-legacy-picture-storage / 89e3f8a67e; Timestamp: 2026-09-25T22:14:43+08:00 (slice 1 delivery receipt); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: coordinator said before delivery "Managed delivery sets up CI observation itself, so no manual observer start is needed"; slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge"; recovered by a `grep` of the delivery scripts for the flag, `ci-mailbox.mjs probe`, a re-run with `--session-json` refused ("rebase left the pre-rebase SHA as the candidate"), `start --execution nerds-odd-e/doughnut story/036-remove-legacy-picture-storage` (observer watch-2ENnNk), and `register-push` for 89e3f8a67e; slices 2-5 passed `--session-json` and reported `reused`.
  - Observed effect: five extra calls (one refused) and a `CI_DISCOVERY_DELAYED` advisory for early revisions; no later coverage loss seen before the completion wait.
  - Inference: the same refused re-run as the story 5 occurrence recurred, so the recovery path is still rediscovered per execution.

- Execution: slice-plans/037-fold-picture-attach-step-into-upload / 1b822a6bf7; Timestamp: 2026-09-25, ~23:30+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: first `deliver` call (no `--session-json`, abbreviated `--validated-candidate 1b822a6bf7`) returned `candidate-mismatch` with `observation.reason` "host session identity is required to verify the notification bridge"; the coordinator then read `execution-increment-observation.mjs` and `ci-host-bridge.mjs` for the flag's shape; a re-run with the full SHA and `--session-json` built from the session transcript path reported `observation.state: attached` (watch-fa5IXa).
  - Observed effect: one refused call and two script reads before the only delivery; no coverage lost, because the refused call published nothing.
  - Inference: the `candidate-mismatch` was likely caused by the abbreviated SHA, which the usage line does not rule out. The coordinator read the ODF-092 occurrences only after delivery, so the logged fix did not reach it beforehand.

- Execution: slice-plans/037-share-backend-test-context / c7ea84e3a7; Timestamp: unknown (first delivery after the plan commit 2026-09-25 23:24:51+08:00; last publication after db643ef844 at 2026-09-26 00:09:04+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: coordinator summary handed to the execution retrospective (no transcript): the first `deliver` returned `candidate-mismatch` because an abbreviated SHA was passed to `--validated-candidate`, and a retry with the full SHA was accepted; every publication (0f8709dfc1, c7ea84e3a7, 7cb7c300b1, 6455811f4d, 5bf185ba0d, db643ef844) reported "host session identity is required to verify the notification bridge".
  - Observed effect: CI on `story/037-share-backend-test-context` was never observed during execution; the retrospective started with CI unknown.
  - Inference: both the abbreviated-SHA refusal and the missing session identity recurred about an hour after the same pair was recorded for `slice-plans/037-fold-picture-attach-step-into-upload` on main, which this execution's base (5c8bb75741) did not contain. Whether a recovery was attempted is not in the summary.
- Execution: SEED-039 story 4 / slice-plans/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26T08:50+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 1 receipt `observation.reason: host session identity is required to verify the notification bridge`; a re-run with `--session-json` refused ("rebase left the pre-rebase SHA as the candidate") because nothing new was left to publish; later deliveries with `--session-json` reported `observation.state: reused`.
  - Observed effect: c9347a9ee3 stayed unobserved; the coordinator found `--session-json` again only from the usage line and `ci-host-bridge.mjs`.
- Execution: SEED-035 story 2 / slice-plans/035-delete-notebook-file-on-web / 20968e7810; Timestamp: 2026-09-26, ~10:24+08:00 (slice 1 delivery; commit 10:24:02+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: coordinator summary handed to the execution retrospective (no transcript): the first managed delivery failed because the target ref lacked the `refs/heads/` prefix, then `candidate-mismatch` for an abbreviated SHA and a missing host session identity (`--session-json`); later deliveries succeeded and the observer /tmp/dough-ci-501/watch-HC7nuY covers `refs/heads/story/delete-notebook-file-on-web`.
  - Observed effect: at least two refused delivery calls before slice 1 was published; the release update from 0.3.38 to 0.3.40 did not remove either earlier cause.
  - Inference: the `refs/heads/` requirement is a new third argument-shape refusal on the same first delivery; the three are rediscovered one refusal at a time. The exact call count is not in the summary.
- Execution: SEED-035 story 23 / slice-plans/042-moved-note-keeps-its-picture / 96186af758; Timestamp: 2026-09-26, ~16:30+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: all three slice receipts (96186af758, 5db1d2beb4, e22c4c1c9a on `refs/heads/story/042-moved-note-keeps-its-picture`) reported `observation.state: unobserved`, "host session identity is required to verify the notification bridge"; the coordinator noted the gap after slice 1 and deferred it to completion instead of passing `--session-json` (`CLAUDE_CODE_SESSION_ID` was set) on slices 2 and 3.
  - Observed effect: no increment was observed during execution; the retrospective started with CI unknown for three pushes.
  - Inference: the coordinator read the ODF-092 occurrences only during the retrospective, so the logged recovery again did not reach delivery.
- Execution: SEED-043 story 1 / slice-plans/045-commit-gate-checks-committed-content / 574d61b52c; Timestamp: 2026-09-26, ~16:04+08:00 (slice 1 delivery; commit 16:03:40+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode); recovered by `ci-mailbox.mjs probe` (`CI_MONITOR_READY`), `start --execution nerds-odd-e/doughnut story/045-commit-gate-checks-committed-content`, and `register-push` for 574d61b52c; later deliveries passing `--session-json '{"session_id":…}'` reported `observation.state: reused`.
  - Observed effect: four extra coordinator calls, including a `grep` of `ci-host-bridge.mjs` for the flag's shape; the recovered observer then delivered slice 1's CI failure (DD-126).
- Execution: SEED-035 story 24 / slice-plans/046-moves-to-another-notebook-reach-git / bc9a0ab229; Timestamp: 2026-09-26, ~17:05+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: slice 1 receipt `observation.reason` "host session identity is required to verify the notification bridge" (background Claude Code job, Story Branch Mode); the coordinator found the flag's shape by grepping `ci-host-bridge.mjs` and `CLAUDE_CODE_SESSION_ID` via `env`; re-running `deliver` with `--session-json` for the already-pushed SHA was refused ("rebase left the pre-rebase SHA as the candidate"); slices 2 and 3 with `--session-json` reported `observation.state: reused` (`/tmp/dough-ci-501/watch-xFqwGh`).
  - Observed effect: five extra coordinator calls; slice 1 was never registered by hand, and the observer started by the refused call picked up later pushes.
  - Inference: the fix recorded in earlier rows (pass `--session-json` from `CLAUDE_CODE_SESSION_ID` on the first delivery) is still not in the delivery guidance, so each execution rediscovers it.

## ODF-110 — A readiness replay observed only the plan's named seam, not the rest of the slice's journey

Former local code: DD-109.

A plan was recorded not-ready because slice 3 relied on an inferred CLI path. The coordinator's disposable replay reproduced exactly the named seam (fast-forward with LFS smudge skipped, then fill-in) and recorded ready. The slice's own journey continued with a publish after the pull, and that publish failed in the CLI, forcing a mid-execution stop and an owner scope decision.

### Occurrences

- Execution: SEED-035 story 14 / slice-plans/025-convert-raw-notebooks-to-lfs / 071d0e0861; Timestamp: 2026-09-24, ~15:35+08:00 (replay and readiness record 47df9474c2), failure observed ~16:05+08:00 (slice 3 E2E); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: research prompt scoped to "pull" risks only; its report noted "the publish check went only as far as the pointer blob being committed"; slice 3 E2E then failed at the second `donut notebook publish` ("Attachment at <commit> must be a Git LFS pointer…", `cli/src/commands/notebook/notebookPublishLfsSelection.ts`); plan recorded the stop in 1e2ed84c35.
  - Observed effect: one human round-trip and a scope change (CLI change, option A) that preparation could have surfaced before Take.
  - Inference: when resolving a readiness concern by observation, replay the slice's full promised journey (here pull, then publish), not only the mechanism the concern names; the replay's own "not covered" list was the signal.

## ODF-104 — Increment delivery reports the default-checkout refresh as deferred without a reason

Former local code: DD-110.

`execution-increment-delivery.mjs deliver` returned `maintenance: "deferred"` on every Trunk Mode publication with no reason or remote/head fields, while the startup receipt reports its maintenance result with `reason`, `remoteSha` and `head`.

### Occurrences

- Execution: SEED-035 story 20 / slice-plans/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~17:40–18:55+08:00 (eleven increment deliveries); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: delivery receipts for 5859e6d663 … 47a3bdd0ab each show only `maintenance: deferred`; at the end the default checkout was clean at eb957e4e7e while origin/main was 47a3bdd0ab.
  - Observed effect: the coordinator could not tell whether the refresh was blocked (ownership, dirty tree) or just skipped, and the default checkout was left behind trunk.
  - Inference: surfacing the same maintenance record as startup would let the coordinator decide whether a manual refresh is safe.
- Execution: SEED-035 story 4 / slice-plans/027-web-uploaded-pictures-as-notebook-files / 4250de93e1; Timestamp: 2026-09-24, ~21:22–22:03+08:00 (five increment deliveries); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: delivery receipts for 4250de93e1, a061808c28, 682259779a, 0e760d4933 and f4d1ec8d5c each show only `maintenance: deferred`, while startup's `afterMaintenance` had advanced the default checkout.
  - Observed effect: same as before; the coordinator could not tell why the default checkout was left behind trunk.

## ODF-111 — Refactor re-proof covered fewer consumers than the shared helper it changed

Former local code: DD-111.

A post-change refactor moved the backend test base's `pointerFor` and the LFS conversion service onto a new production method (`NotebookAttachmentContent.storeAsLfsPointer`) but reran only four backend test classes, although `pointerFor` is inherited by most `NotebookGit*` controller tests.

### Occurrences

- Execution: SEED-035 story 20 / slice-plans/026-test-file-journeys-on-lfs / 5859e6d663; Timestamp: 2026-09-24, ~18:45+08:00 (slice 10 refactor return); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.37.
  - Evidence: slice 10 refactor report ("Backend (conversion service and test base)": four `--tests` classes); the coordinator then ran the full backend suite (2668 tests green) before committing 47a3bdd0ab.
  - Observed effect: one extra full-suite run by the coordinator; no defect found.
  - Inference: the refactor contract's caller analysis was applied to production callers but not to inherited test-support consumers. A similar broad refactor in slice 5 took about 35 minutes against a 10-minute slice limit; it was valuable cleanup but was not escalated.

## ODF-112 — The CI observer delivered no failure for failed story-branch runs, so later slices were built on a red branch

Former local code: DD-112.

The attached Story Branch observer recorded only one `CI_DISCOVERY_DELAYED` event while two later registered revisions failed CI. The failures were found only by a manual `gh run list` at the planned stop boundary.

### Occurrences

- Execution: SEED-035 story 19 / slice-plans/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25, runs failed ~09:40–10:03+08:00, found ~10:50+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: runs 36083055193 (0d84006f7d) and 36084271448 (9ad0b9bf9f) failed "Backend Unit tests"; mailbox `/tmp/dough-ci-501/watch-7llTFe/events` held only sequence 1 (`CI_DISCOVERY_DELAYED` for 0284ea7f52); both deliveries had reported `observation.state: reused`. A later `CI_MONITOR_UNAVAILABLE` (network error on `gh run list`) arrived during the repair.
  - Observed effect: slices 3 and 4 were implemented, refactored and published on a failing branch for about an hour; the repair then had to cover three failed revisions.
  - Later evidence: at completion, `complete-revision` for aba0dc2509 returned `unresolvedReason: timeout` with the revision still `undiscovered`, while `gh run list` showed that run `completed success`.
  - Inference: cause unverified (discovery after the delay advisory, per-revision registration, or hook delivery); a manual `gh run list` check before each delegation would have caught it one slice later.

## ODF-113 — A failure was called pre-existing by comparing against a revision that already contained this execution's earlier slices

Former local code: DD-113.

An implementation agent reported a full-suite heap exhaustion as "also on the base commit", using the previous slice's tip rather than a known-green revision; the coordinator repeated that label to the owner before CI history showed slice 1 and main were green.

### Occurrences

- Execution: SEED-035 story 19 / slice-plans/029-remove-raw-file-storage / 0284ea7f52; Timestamp: 2026-09-25T10:44+08:00 (slice 4 return; commit e9cbbc6547); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: slice 4 report ("same thing happens on the unchanged base commit `9ad0b9bf9f`"); CI runs green for 0284ea7f52 and main 9d2d091d0a; heap dump later traced the leak to slice 2's large LFS test payloads retained by `InMemoryNotebookAttachmentContent`.
  - Observed effect: the first stop report told the owner the failure was pre-existing and out of scope; the correction came only after the CI check.
  - Inference: "pre-existing" needs a baseline from before this execution's first change (claim revision or last green CI), not the prior slice.

## ODF-120 — A not-ready assessment whose only reason was a satisfied start condition blocked queued startup

Former local code: DD-114.

The plan's recorded assessment was `not-ready` solely because its start condition (the story it builds on being on main) was unmet. That story was later merged and wrapped up without re-assessing dependents, so `execution-start.mjs start` refused with "published preparation is needs-reassessment". The coordinator checked the reused names on main, recorded `ready`, and published a separate readiness commit on main before the Take.

### Occurrences

- Execution: SEED-035 story 17 / slice-plans/034-book-source-as-notebook-file / 36eb15caaa; Timestamp: 2026-09-25, ~15:43+08:00 (refusal, then readiness commit 741dbf31ba); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: `read-state` reasons "Start condition unmet: reuses naming helpers and the startup-move shape from the picture move (slice-plans/033, SEED-035#story-5), not yet on main."; story 5 merged at 72021b8efe; start receipt `{"status":"source-refused","error":"published preparation is needs-reassessment"}`; readiness recorded and pushed as 741dbf31ba.
  - Observed effect: about six extra calls and one extra commit on main; an execution coordinator performed a preparation assessment.
  - Inference: when a start condition names another story, that story's wrap-up (or the start command) could re-check dependents whose only blocking reason it resolves.

## ODF-121 — One transient GitHub TLS timeout ended CI observation for the rest of the execution

Former local code: DD-115.

During slice 7, the observer emitted `CI_MONITOR_UNAVAILABLE` for a single `gh run list` call that failed with "net/http: TLS handshake timeout". Observation then stopped, so the slice 6 run already in progress and the slice 7 publication had no notification coverage; the coordinator had to check `gh run list` directly.

### Occurrences

- Execution: SEED-035 story 17 / slice-plans/034-book-source-as-notebook-file / 36eb15caaa; Timestamp: unknown (event delivered between slice 6 commit 2026-09-25 16:44:12 +0800 and slice 7 commit 2026-09-25 16:58:36 +0800); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: hook context `{"type":"CI_MONITOR_UNAVAILABLE",…,"reason":"Command failed: gh run list … TLS handshake timeout"}` for observer /tmp/dough-ci-501/watch-WnDwf2; slice 7 receipt `observation.state: unobserved`.
  - Observed effect: lost coverage for db13a2d99c and 96c756d531; manual CI checks replaced notifications.
  - Inference: a bounded retry for a transient network error before declaring the observer unavailable would likely have kept coverage.

## ODF-122 — A correction plan written by a retrospective had no readiness record, so queued startup refused it

Former local code: DD-117.

The execution retrospective for SEED-035 story 18 created and queued the correction plan without recording its preparation (`read-state` reported `not-recorded`). `execution-start.mjs start` refused with "selected canonical preparation or identity is unresolved". The coordinator reviewed the plan, recorded `ready` with `record-state`, and published a separate readiness commit on main before the Take. A second start then refused `--plan` ("requested plan disagrees with published preparation") because a whole-document correction's plan is its own canonical home, so the flag has to be left out.

### Occurrences

- Execution: slice-plans/037-fold-picture-attach-step-into-upload / 1b822a6bf7; Timestamp: 2026-09-25, ~23:20+08:00 (refusals, then readiness commit d9f95b830e); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: plan queued by db601a2e42 with no story-state block; start receipts `{"status":"source-refused","error":"selected canonical preparation or identity is unresolved"}` and `"requested plan disagrees with published preparation"`; readiness pushed as d9f95b830e; Take 256cdb3d08.
  - Observed effect: about seven extra calls (reading `execution-source.mjs` and `record-preparation.md`) and one extra commit on main; an execution coordinator performed a preparation assessment, as in DD-114.
  - Inference: the retrospective's correction-planning path goes through slice planning, which says to record readiness, but that step was skipped or not reached for a plan that is its own home. Recording the assessment when the retrospective writes the plan would remove the refusal.

## ODF-106 — Two concurrent executions allocated the same slice-plan number from different bases

Former local code: DD-118.

Slice planning takes the number after the highest plan entry in the checkout that writes the plan and rechecks only that path. A plan written in an execution worktree whose base predates a plan already on main got the same number, so two different executions are both "037".

### Occurrences

- Execution: slice-plans/037-share-backend-test-context / c7ea84e3a7; Timestamp: 2026-09-25T23:24:51+08:00 (plan commit 0f8709dfc1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: base 5c8bb75741 (22:09:45+08:00) lists plans 007, 035, 036; main's db601a2e42 (23:06:25+08:00) had already added plan `037-fold-picture-attach-step-into-upload`; 0f8709dfc1 added plan `037-share-backend-test-context`. Number 116 and 117 of this log were likewise allocated on main after the base, so this entry uses 118.
  - Observed effect: DearDough rows and `.planning/test-optimization-candidates.md` ("plan 037 cut the suite…") refer to "037" for two different executions once both plans are deleted at wrap-up; the retrospective's correction plan had to reword the candidate record.
  - Inference: the same stale-base allocation applies to DD numbers in this log, so a merge can also produce duplicate finding codes. Whether the coordinator fetched `origin/main` before planning is not recorded.

## ODF-123 — A story whose owner deferred planning to execution could not be started until the coordinator planned and published it separately

Former local code: DD-119.

The story said "The owner chose to skip story refinement; the plan is made during execution" and was queued with `approach: unselected`. `execution-start.mjs start` accepts only a published `planned`+`ready` or `planless` preparation, so it refused. The coordinator created a worktree, profiled, wrote the plan, recorded `refined`/`planned`/`ready`, pushed that plan straight to main, and then ran startup again against the same worktree.

### Occurrences

- Execution: SEED-039 story 4 / slice-plans/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26T08:14+08:00 (first refusal); plan commit 43149dd7ef 08:23+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: receipt `{"status":"source-refused","error":"queued planless authority or plan link is inconsistent"}`; `execution-source.mjs` requires `approach.kind` planned or planless; plan 43149dd7ef then Take 44738a145a.
  - Observed effect: about ten extra calls reading `execution-source.mjs`, `preparation-disposition.md`, `preparation-workspace.md`, and `record-preparation.md`; a planning commit reached main without a separate keep decision.
  - Inference: the coordinator treated the story's own text plus `/dough-execute-plan` as keep authority. No guidance names a "plan during execution" path, so either queued stories need a planned approach before queueing or startup needs a documented plan-first step.

## ODF-124 — The coordinator told parallel agents a guidance rule did not exist after searching only SKILL.md files

Former local code: DD-120.

A refactor agent cited a 250-line file limit. The coordinator grepped `SKILL.md` files and lint scripts, found nothing, and messaged two running implementers and one refactor agent that the limit did not exist. The rule is in `dough-post-change-refactor/references/refactor-checks.md` ("File size"). One refactor then produced a 400-line spec and another a 268-line spec, and the coordinator had to retract the claim and send two follow-up refactors.

### Occurrences

- Execution: SEED-039 story 4 / slice-plans/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26, between 08:50 and 09:08+08:00 (between slice 1 and slice 3 commits); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: grep over `.agents/skills/*/SKILL.md` returned nothing; `refactor-checks.md:128` "Shorten or split every checked file exceeding **250 lines**"; follow-up refactors split `RecallPage.answering.spec.ts` (400 → 225 + 221) and shortened `RichMarkdownEditor.propertyEntry.spec.ts` (268 → 242).
  - Observed effect: two extra refactor agents (~125k subagent tokens) and one failed commit; a plan learning had to be rewritten.
  - Inference: a negative claim about guidance needs a search of the whole skill tree, references included, before it is broadcast to agents.

## ODF-125 — Interim "agent has not reported yet" notifications repeatedly woke the coordinator with nothing to decide

Former local code: DD-122.

A delegated implementation agent started its tests in the background and ended its turn while it waited. Each time it stopped, the host sent the coordinator a completed-task notification whose result said the report was still pending. The coordinator woke, answered "still waiting", and went idle again.

### Occurrences

- Execution: SEED-035#story-11 / slice-plans/007-dissolve-merge-folders-with-files / 1a8b7abff7; Timestamp: 2026-09-26T05:32:17Z–05:37:01Z (10 notifications during slice 8), plus 2026-09-26T05:55:06Z (slice 9); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: session `fe371aa9-…` task-notifications for "Implement slice 8 of plan 007", each with the note "stopped with background work of its own still running" and the result "This agent has not reported yet"; 11 coordinator turns, each reading about 235k–240k cached input tokens, output 19–37 tokens.
  - Observed effect: about 2.6M cache-read tokens went on status-only turns. The final reports and the execution were unaffected.
  - Inference: asking delegated implementers to run focused tests in the foreground, or having the coordinator stay idle on an interim notification, would avoid this. Qualified: the host notification behavior is outside the project's control.

- Execution: SEED-046#story-1 / slice-plans/002-publish-cost-follows-change / 78e337f12b; Timestamp: unknown (about 2026-09-26T20:40+08:00–21:30+08:00, from worktree file times and the measurement run id; slice 1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: unknown.
  - Evidence: coordinator conversation for this execution: about 12 task-notifications for the slice 1 implementer, each "stopped with background work of its own still running" / "has not reported yet"; its tool-use count stayed at 76 across most of them. A process check found no test or measurement still running, and the report arrived only after the coordinator sent a message asking for it.
  - Observed effect: about 12 status-only coordinator turns, plus one inspection and one nudge. The report, proof and delivery were unaffected.
  - Inference: this time the agent kept waiting after its background work had ended, so the notifications continued past the point where anything was running. A coordinator message asking for the report ended the wait. Qualified: token counts for these turns were not captured.

## ODF-116 — Closing one story in a shared seed made a sibling story's ready assessment stale

Former local code: DD-124.

A story's readiness basis is a digest of its whole seed document. Wrapping up another story in the same seed removed that story's section, which changed the digest, so `execution-start.mjs start` refused the unchanged, ready sibling with "published preparation is needs-reassessment". The coordinator rechecked the story text and plan assumptions, recorded `ready` again, and published a separate readiness commit on main before the Take.

### Occurrences

- Execution: SEED-035 story 2 / slice-plans/035-delete-notebook-file-on-web / 20968e7810; Timestamp: 2026-09-26, before 10:09:55+08:00 (refusal; readiness commit 7b949a9246 at 10:09:55+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: bfb3d92154 (09:06:00+08:00) closed SEED-035's Book storage story, removing 70 lines of `SEED-035-ai-workspace-supporting-files.md`; 7b949a9246 changed only `basis.document` (627ac528… → f56f518f…) while `basis.plan` stayed e9eb6a56…; the story-2 section and plan were unchanged. Main's 2921f4d44b re-recorded SEED-035#story-11 as ready after the same removal.
  - Observed effect: one refused start, a reassessment, one extra commit on main and a retry; an execution coordinator performed a preparation assessment, as in DD-114.
  - Inference: every story in a multi-story seed is invalidated by any sibling's wrap-up. A digest of the story's own section (plus shared seed context) would keep unrelated closures from forcing reassessment. Qualified: the number of extra calls is not in the summary.
- Execution: SEED-035 story 24 / slice-plans/046-moves-to-another-notebook-reach-git / bc9a0ab229; Timestamp: 2026-09-26, ~16:55+08:00 (readiness re-record before Take); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: c040694118 (16:46:01+08:00) closed SEED-035 story 23, removing its section; `read-state` for story 24 then reported `needs-reassessment` (basis.document c5063546… → f8d817e5…) while the story-24 section was unchanged; the coordinator re-recorded ready in f68e4235cf together with a plan note that story 23 had landed.
  - Observed effect: the owner asked for an up-to-date check anyway, so the reassessment was wanted work here; the digest mismatch itself carried no information about story 24.
  - Inference: when the plan also depends on the closed sibling (here "start after story 23 lands"), the reassessment is useful; the refusal cannot tell that case from an unrelated closure.
- Execution: SEED-035 story 25 / `.planning/slice-plans/047-link-rewrites-in-other-notebooks-reach-git/PLAN.md` at 0ef2828d32 / 9861bc80c2; Timestamp: 2026-09-26, before 17:53:07+08:00 (refusal; readiness commit df199a2190); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: dfe13fa6ba (17:37:44+08:00) closed story 24 and on purpose restated the delivered moves in plan 047's text; start then refused with "published preparation is needs-reassessment"; df199a2190 changed only `basis.plan` (92dc2b80… → 28979d16…), `basis.document` unchanged.
  - Observed effect: one refused start, a recheck of three plan facts, and one extra commit on main before the Take.
  - Inference: this time the plan digest changed, not the seed digest. The wrap-up that edited the sibling's plan had just checked those facts, so it could have re-recorded the sibling's readiness in the same commit.
- Execution: SEED-046 story 3 / `.planning/slice-plans/003-clone-and-publish-name-next-step/PLAN.md` at df5ba26eb0 / 6d807aa4ea; Timestamp: 2026-09-26, before 21:31:45+08:00 (refusal; readiness commit d24c9d7ae6); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: 5306a7f573 closed SEED-046 story 2, removing its seed section; start refused with "published preparation is needs-reassessment"; d24c9d7ae6 changed only `basis.document` (c4e317cd… → 945f97ea…), `basis.plan` a20d0e1c… unchanged.
  - Observed effect: one refused start, a recheck of the plan's change points against post-story-2 code, one extra commit on main and a retry.
  - Inference: as in the story 24 occurrence, the plan stated "executed after story 2 lands", so the recheck had some value; the refusal still could not distinguish it from an unrelated closure.

## DD-126 — A plan said the changed script had no test, and nobody searched for one before delivery, so CI caught the stale test

The plan for the commit-gate change recorded "No permanent automated test is added for the hook: it has none today, CI does not run it". `scripts/test/quality_changed.test` already tested `scripts/quality_changed.sh` with a fake `pnpm`, and CI runs it in "Run script unit tests". The implementer, the refactor agent and the coordinator's proof acceptance all relied on the plan's claim; the path-scoped `script` skill, which covers tests under `scripts/`, was not named in delegation and attached only after the first slice.

### Occurrences

- Execution: SEED-043 story 1 / slice-plans/045-commit-gate-checks-committed-content / 574d61b52c; Timestamp: 2026-09-26T16:06:02+08:00 (CI step failure); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: plan "Current decisions" before 120753a097; CI run 36228685291 job "Other Unit Tests" failed `quality_changed.test` ("shared biome config selects every affected component": expected `pnpm frontend:lint`, got the install line after `ln` failed); repair 120753a097 updated and extended the test.
  - Observed effect: one red story-branch CI run, a stash/repair/restore cycle around slice 2, and two extra agents (repair ~49k and refactor ~48k subagent tokens).
  - Inference: a negative claim that code has no test needs a search of the test tree (here `grep -rl quality_changed scripts/test`) at planning or delegation; naming the stack skill for `scripts/` in the delegation would likely have surfaced it.

## DD-127 — An implementer reasoned that a new test would fail instead of running it red, and one of its tests could not fail

The slice 3 implementer skipped the red run, arguing that before the change the cross-notebook path made no commit, so the `parents == previous commits` assertions could not pass. The undo test had no such assertion: without the fix neither move committed, so both notebooks stayed at their starting tree and the test passed. The coordinator found this by restoring the old main code and running the new tests (2 of 3 failed), then added a commit-count assertion (3 of 3 failed, then 5 of 5 passed with the fix).

### Occurrences

- Execution: SEED-035 story 24 / slice-plans/046-moves-to-another-notebook-reach-git / bc9a0ab229; Timestamp: 2026-09-26, ~17:30+08:00 (coordinator red check before slice 3 refactor); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: slice 3 return ("I did not run the new tests red first"); red run with `NotebookFolderController` and `FolderRelocationService` reset to HEAD: `movingAFolderBackAsUndoRestoresBothNotebooks` passed; `NotebookGitWebFolderCrossNotebookMoveControllerTest` in 3d5c1bf394 asserts Engineering's history grew by two commits.
  - Observed effect: two extra focused test runs by the coordinator; the delivered undo test now fails without the fix.
  - Inference: a round-trip test whose end state equals its start state passes when nothing happens; "red first if practical" in the delegation let the agent substitute reasoning for observation. Slice 2's implementer ran red and its undo test was meaningful.
- Execution: SEED-046#story-4 / `.planning/slice-plans/004-picture-upload-explains-and-shows/PLAN.md` at 254e59d685 / a388ecc2d6; Timestamp: 2026-09-26, ~21:55+08:00 (retrospective red runs after slice 3 delivery 54cab40fba); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: slice 3 return ("I did not check the accepted-upload test against the code without the change"); slice 2 return reports no red run. Retrospective red runs: `storedApi.spec` with `StoredApiCollection.ts` from 113c9b18f1 → 1 of 12 failed (meaningful). `NoteControllerUploadNoteImageTests` with slice 2 main code reverted to a388ecc2d6 → the three refusals failed, but `aPictureAtTheLimitIsAccepted` and `theDeclaredContentTypeAndTheBytesAreNotChecked` passed: the old type/size refusal lived in `@Valid` binding, which the direct `noteController.uploadNoteImage` call never runs.
  - Observed effect: two extra focused runs by the reviewer; two delivered acceptance tests cannot fail if the content-type refusal returns to the DTO. No behavior defect.
  - Inference: the delegation prompt this time did not ask for a red run at all, so neither implementer attempted one; a red run would have shown that key example 4's "today refused" lies outside the controller-call test boundary.

## DD-128 — Refined key examples promised link-rewrite outcomes that the existing rewrite rules do not produce

The story said link rewriting stays unchanged, yet its key examples stated rewrite results nobody checked against that code. Example 2 promised that renaming folder `physics` rewrites `[[Science:physics/Force]]` in another notebook; `PortablePath.withRenamedFolder` returns notebook-qualified links unchanged, so folder rename never touches another notebook. Examples 2 and 4 also gave shorthand results (`[[Science:Force]]`-style) where the rules produce `[[Science:/Force]]` (dissolve) and `[[Physics:Force|Science:Force]]` (move to another notebook). Slice 3 found the conflict. The coordinator kept the "rewriting unchanged" exclusion, dropped folder rename from example 2 in the plan's Current decisions, and left the seed promise as written.

### Occurrences

- Execution: SEED-035 story 25 / `.planning/slice-plans/047-link-rewrites-in-other-notebooks-reach-git/PLAN.md` at 0ef2828d32 / 9861bc80c2; Timestamp: 2026-09-26, ~18:27+08:00 (slice 3 commit f63760e183 records the decision); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: refinement/plan 27673389c0 (seed key examples 2 and 4; plan slice 3 "Folder rename, move and dissolve"); `backend/src/main/java/com/odde/donut/algorithms/PortablePath.java` `withRenamedFolder` (early return when `notebookQualifier.isPresent()`); plan "Current decisions" in f63760e183; test expectations in `NotebookGitWebLinkingNotebookControllerTest` (`[[Science:/Force]]`, `[[Physics:Force|Science:Force]]`).
  - Observed effect: one owner-visible promise was dropped during execution by a plan note, without an owner decision or a seed edit. No extra slice was needed. The seed still promises the folder-rename case at closure.
  - Inference: when a story says an existing rule stays unchanged, check each key example's expected text against that rule (its code or tests) at refinement. Also, a promise dropped because it conflicts with an exclusion should reach the owner at completion, not only the plan. Qualified: the coordinator summary is the only process record, so how long the discovery took is unknown.

## DD-129 — The "stays editable" boundary examples were all one-line bodies, so a check that refuses wrapped text shipped

Story-10's style-only boundary (key example 5) and every editable test case used bodies with no line wrapped inside a paragraph or list item. The plan asserted that comparing renderings lets style-only changes pass "without special cases" without trying a hard-wrapped body. The implementer, the coordinator's extra probe (wiki links, tables, images, CJK) and the refactor agent never tried one either. The retrospective's first probe of `line one\nline two` found it refused, although the editor's save renders the same.

### Occurrences

- Execution: SEED-046 story 10 / `.planning/slice-plans/005-rich-editor-keeps-content/PLAN.md` / c5338213d0; Timestamp: 2026-09-26T22:19:00+08:00 (retrospective probe); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: plan "Current decisions" (planning, rendered-HTML comparison); `RichMarkdownEditor.bodyItCannotKeep.spec.ts` editable cases at 0e36045508; `richEditorKeepsBody.ts` normalizes only `/>\s*\n\s*</`; correction plan `008-hard-wrapped-note-stays-editable`.
  - Observed effect: a regression against the story's own style-only promise reached the published story branch; a correction story and plan were needed before integration.
  - Inference: when a story's promise is "ordinary content keeps working", its boundary examples need a realistic sample of that content (here, a hard-wrapped paragraph such as any file in this repo), not only minimal constructs. The seed's value note already asked how many real notes the editor cannot carry.

## DD-130 — The plan prescribed a comparison renderer and a trigger point that the implementer had to replace

Slice 2's plan named `markdownToQuillHtml` as the renderer for judging meaning and a change of `markdownForRichDisplay` as the moment to check. Neither was tried at planning. `markdownToQuillHtml` removes whitespace between tags and drops task checkboxes, so it hides the losses the story is about. A watcher with `nextTick` ran before Quill had taken in the HTML. The implementer switched to plain `marked` and a new `QuillEditor` `modelLoaded` event, and reported both deviations.

### Occurrences

- Execution: SEED-046 story 10 / `.planning/slice-plans/005-rich-editor-keeps-content/PLAN.md` / c5338213d0; Timestamp: 2026-09-26T22:16:23+08:00 (slice 2 commit 0e36045508); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: plan slice 2 "Change" and "Learnings" at 0e36045508; slice 2 implementer return (~579 s, ~102k subagent tokens).
  - Observed effect: slice 2 ran close to the 10-minute hard limit; the plan's Change text no longer describes the code and is corrected only by its Learnings.
  - Inference: a plan that names the exact function or hook for a new check should name it as a suggestion unless a quick probe confirmed it; here a one-line call of `markdownToQuillHtml` on `**a** *b*` versus `**ab**` would have shown the problem. Qualified: the deviation was handled well and cost one slice's margin, not a retry.

## Retention

- Highest allocated local number: 130
- Recovery: `33939aa45a17c08f5e6f8076178ce96437fdfbe8:DearDough.md` contains the complete pre-compaction log and earlier recovery references; `b0b385a184e96ecf8b0f6fc5572eaaab69bc8dad` preserves later history.
- Occurrence history is partial; full observations, effects, inference and historical provenance remain in that snapshot and the upstream catalog.
