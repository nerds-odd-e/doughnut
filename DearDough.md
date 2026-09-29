# DearDough Process Findings

Compact open shared-process findings; project-only issues live in [DonutRetrospectiveFindings.md](DonutRetrospectiveFindings.md).
Full evidence and response tracking: [Open Dough catalog](https://github.com/terryyin/open-dough/blob/main/docs/maintainer/finding-names.md).
Released responses with verified use are tracked upstream; local omission does not assert effectiveness.
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
- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T06:51:38Z (slice 9) and 2026-09-28T07:25:32Z (slice 10 docs); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: slice 9 `Agent` launch refused three times by transient auto-mode "no verdict" errors (06:39–06:42Z); after the owner's third "continue" the coordinator wrote `V300000350`/`V300000351` and ran the backend suite itself (commit 5034e1543d), though its refactor `Agent` launch succeeded at 06:54Z. Slice 10's `prod_env.md` and diagram edits were coordinator `Edit`/`sed` (commit 8e03ac5f5f).
  - Observed effect: two of eleven delivered slices had no implementation handoff; both were tiny and their proofs passed.
  - Inference: a host outage that blocks agent launch, and a docs-only slice, both pulled the coordinator into local work; the one-interactive-slice exception covered neither. Qualified: low cost here.

## ODF-091 — Coordinator accepted its own refactor pass without a fresh refactor agent

Former local code: DD-098.
Three ordinary slice commits used coordinator self-review instead of a fresh refactor agent. No independent findings were recorded. This differs from interrupted-repair and post-refactor-correction omissions.

### Occurrences

- Execution: slice-plans/008-remove-zip-export / 38b5e1ef69; Timestamp: 2026-09-21T22:08:41+08:00; Tool: Cursor; Model: Cursor Grok 4.7; Open Dough release: 0.3.27.
- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T07:25:32Z–07:26:36Z; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: slice 10's docs removal (`docs/gcp/prod_env.md` −14 lines, excalidraw label) went from coordinator edits to the plan update and commit 8e03ac5f5f with no refactor `Agent` call; every other code/docs slice of the execution had one.
  - Observed effect: one small docs change delivered without independent review; no defect found in this retrospective.

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
- Execution: SEED-050#story-1 / `.planning/slice-plans/020-validate-changed-markdown-once/PLAN.md` / f8b186cc7f; Timestamp: 2026-09-27, ~15:40+08:00 (refusal, then readiness commit 0584473e28); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: recorded reason "Waits for SEED-009#story-48 (plan 019) to land…"; story-48 wrapped up at 825560fe9e without reassessing SEED-050#story-1; start receipt `{"status":"source-refused","error":"published preparation is not-ready"}`; coordinator re-checked starting facts, recorded `ready`, pushed 0584473e28 (one rejected push, rebased over a sibling Take).
  - Observed effect: about ten extra coordinator calls and a commit on main before the Take; the coordinator had to find the preparation procedure itself.

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
- Execution: SEED-047#story-2 / `72210f216a:.planning/slice-plans/015-removal-landing-listing-load/PLAN.md` / 46e3a106e3; Timestamp: 2026-09-27, before 11:46:49+08:00 (refusals, then readiness commit 6634f8f74e); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: edfd7ba92a (SEED-047#story-1 retrospective/wrap-up preparation) wrote story-2's state block as refined + planned with no assessment; `read-state` reported `assessment.status: "absent"`; start receipts `"requested plan disagrees with published preparation"` (with a seed-relative `--plan`) then `"published preparation is absent"`; readiness pushed as 6634f8f74e; Take 5a4bfe2f92.
  - Observed effect: two refused starts, about six extra coordinator calls to find and apply `record-preparation.md`, and one extra commit on main.
  - Inference: here the correction had an anchored story (not a plan-homed one), so the missing assessment is not specific to plan-homed corrections; the retrospective's correction path still ends without the readiness step.

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

- Execution: SEED-046#story-9 / `.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md` at d305df9c23 / 9365a11c7d; Timestamp: unknown (2026-09-27, between 09:50 and 10:02+08:00, slice 1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: coordinator conversation: one task-notification for "Implement plan 010 slice 1" with "stopped with background work of its own still running" / "This agent has not reported yet", followed by the real hand-back.
  - Observed effect: one status-only coordinator turn; delivery unaffected.

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
- Execution: SEED-046 story 11 / `.planning/slice-plans/007-clone-test-states-output/PLAN.md` at 69ef49e8d9 / 05b630dce5; Timestamp: 2026-09-26, before 22:24:39+08:00 (refusal; readiness commit 7bb85093ae); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: after the correction was planned, d5d22ccbe5 and 5bd90172ab closed SEED-046 stories 3 and 4, removing their seed sections; start refused with "published preparation is needs-reassessment"; 7bb85093ae changed only `basis.document` (3d5b7ba4… → 4895a083…), `basis.plan` c58a45ad… unchanged.
  - Observed effect: one refused start, a recheck of the story section against the test file, one extra commit on main and a retry, for a one-slice test-only correction.
  - Inference: unlike the two previous occurrences, this plan did not depend on the closed siblings, so the reassessment carried no information.
- Execution: SEED-046#story-5 / `.planning/slice-plans/006-web-edit-changes-only-edit/PLAN.md` at 2a8fd844b4 / 5f71d4d237; Timestamp: 2026-09-26, before 23:15:18+08:00 (refusal; readiness commit 2a8fd844b4); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: after plan 006 was assessed ready (basis.document f658172c…), df50754d7e and 247e9a9664 closed SEED-046 stories 11, 10 and 12 and removed story 4's section; start refused with "published preparation is needs-reassessment"; the story-5 section was byte-identical before and after (awk section diff); 2a8fd844b4 changed only `basis.document` (→ ad173d6e…), `basis.plan` 8bc0a3b8… unchanged.
  - Observed effect: one refused start, a recheck of the plan's named symbols against current code, one extra commit on main and a retry.
  - Inference: the plan referred to story 4a (plan 005) landing first, so the symbol recheck had a little value; the refusal itself carried no information about story 5.
- Execution: SEED-046#story-9 / `.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md` at d305df9c23 / 9365a11c7d; Timestamp: 2026-09-27, before 09:44:06+08:00 (refusal; readiness commit 939ce6b6f7); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: 67be25ba00 (08:34:03+08:00) closed SEED-046 story 13 and an earlier closure removed story 14; start refused with "published preparation is needs-reassessment"; `git diff 1f2d54c86c HEAD` on the seed showed only the sibling sections removed; 939ce6b6f7 changed only `basis.document` (acb4a665… → 19996e46…), `basis.plan` cbd378e1… unchanged.
  - Observed effect: one refused start, a reassessment, one extra commit on main whose first push was rejected by a concurrent Take and needed a rebase, then a retry.
  - Inference: the plan did not depend on stories 13 or 14, so the reassessment carried no information.
- Execution: SEED-050#story-10 / `.planning/slice-plans/028-history-commits-proven-where-they-happen/PLAN.md` at a047e955ff / 899ed2079f; Timestamp: 2026-09-27, before 18:08+08:00 (refusal; readiness commit e2f6ab75c2); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: 99137184b3 (17:35:41+08:00) renumbered the story from #story-9 to #story-10 in its own seed section, plan and backlog, without re-recording readiness; start refused with "published preparation is needs-reassessment"; e2f6ab75c2 changed only `basis.document` (d07d54d7… → 05b6de91…), `basis.plan` 11e47dc6… unchanged.
  - Observed effect: one refused start, a recheck of the plan's starting facts against current code (all held), one extra commit on main and a retry.
  - Inference: matching is uncertain — the trigger was an identity-only edit to this story's own section, not a sibling closure, so a per-section digest would not have avoided it; as in the plan-47 occurrence, the commit that made the edit could have re-recorded readiness.
- Execution: SEED-009#story-47 / `.planning/slice-plans/018-cross-notebook-folder-move-names/PLAN.md` at c526dba221 / 732dbab312; Timestamp: 2026-09-27, before 13:03:43+08:00 (refusal; readiness commit 7360e9e9f7); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: 0d7772ed0e (13:02:28+08:00) refined and planned sibling SEED-009#story-48 in the same seed, a preparation write rather than a closure; start refused with "published preparation is needs-reassessment"; `git diff c526dba221 0d7772ed0e` on the seed touched only story 48's section; 7360e9e9f7 changed only story 47's `basis.document` (ee9b982d… → 28c68632…), `basis.plan` 38ea6254… unchanged.
  - Observed effect: one refused start that also left a local Take candidate built on the stale base; the retry with its recovery coordinates refused again ("unpublished selected story source in originating checkout"), so the coordinator removed that unpublished worktree and branch, published the reassessment, and took the story fresh — about six extra calls.
  - Inference: sibling preparation, not only sibling wrap-up, invalidates a story's readiness; two stories prepared minutes apart in one seed will always collide this way.
- Execution: SEED-050#story-8 / `.planning/slice-plans/026-one-folder-entry-rule/PLAN.md` / 649e162dd4; Timestamp: 2026-09-27, before ~16:59+08:00 (refusal; readiness commit 7fdb9ba0a9); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: unknown.
  - Evidence: af4f39a65e closed SEED-050 story 2 and rewrote plan 026's story-2 link to a provenance reference; 847c4c5ce3 re-recorded sibling story 5; start refused with "published preparation is needs-reassessment"; story 8's section was unchanged, the plan diff was only that provenance line, and plan 026's product files were untouched since aef1e27b77; 7fdb9ba0a9 changed both `basis.document` (b84184e4… → 348fd390…) and `basis.plan` (302229bb… → 43fe8405…).
  - Observed effect: one refused start, about five read-only checks, one extra commit on main and a retry.
  - Inference: the wrap-up that edited plan 026's provenance line could have re-recorded story 8's readiness in the same commit, as the story-25 occurrence already suggests.
- Execution: SEED-050#story-4 / `35972d89e4:.planning/slice-plans/023-one-notebook-tree-model/PLAN.md` / 3e9f034932; Timestamp: 2026-09-27, before 16:22:33+08:00 (refusal; readiness commit c896f2fcfc); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: 299cd69bac closed SEED-050 story 3, removing its seed section; `execution-start.mjs start` refused with "published preparation is needs-reassessment"; `read-state` showed only `basis.document` changed (4213a2e5… → e6161163…) with the plan digest and story-4 section unchanged; c896f2fcfc re-recorded ready.
  - Observed effect: one refused start, a starting-facts spot check, one extra commit on main and a retry.
  - Inference: unlike the story 24 and story 3 occurrences, plan 023 did not depend on the closed sibling, so the reassessment carried no information.
- Execution: SEED-050#story-9 / `.planning/slice-plans/027-dissolve-enters-folders-by-the-one-rule/PLAN.md` / 6d48bad1b5; Timestamp: 2026-09-27, before 18:06:21+08:00 (first readiness commit ac6105235d); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: 99137184b3 renumbered sibling story 10 in the same seed; start refused with "published preparation is needs-reassessment" (`basis.document` f61c17e5… → d07d54d7…, `basis.plan` 876b08ab… unchanged); ac6105235d re-recorded ready; its push was rejected, and the rebase brought in dde502bf56, which closed story 7 and changed the digest again, so a second re-record was needed. That uncommitted re-record in the default checkout went into a concurrent session's e2f6ab75c2 ("Reassess SEED-050#story-10 readiness…"), which re-recorded both siblings.
  - Observed effect: one refused start, two reassessments, one extra commit on main plus a swept-in edit, one rejected push, and one denied amend of the already-pushed readiness commit; about eight extra coordinator calls before the Take 13721c9182.
  - Inference: story 9 depended on neither story 7 nor story 10, so neither reassessment carried information; two siblings in one seed being prepared and closed within minutes makes this repeat back to back.
- Execution: SEED-051#story-2 / `.planning/slice-plans/010-search-shows-each-response/PLAN.md` at 4f73b51134 / 412911f50f; Timestamp: 2026-09-28, before 16:36:28+08:00 (refusal; readiness commit 499b34eb3c); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: 2160ce302a (16:23:47+08:00) closed sibling SEED-051#story-1, removing its section and a trailing shared code-locations block from the seed; start refused with "published preparation is needs-reassessment"; `read-state` showed only `basis.document` changed (7ba4a638… → 080610786…), `basis.plan` a7e4bddc… unchanged; 499b34eb3c re-recorded ready after re-running the plan's premise grep.
  - Observed effect: one refused start, three read-only checks, one extra commit on main and a retry.
  - Inference: the correction was queued by the story-1 retrospective only minutes before that wrap-up, so the wrap-up could have re-recorded its sibling's readiness in the same commit; the removed shared block carried no premise of plan 010.

## ODF-155 — An implementer proved fails-first by putting HEAD versions back in the shared execution checkout

Former local code: DD-141.

The slice 5 delegation told the implementer both to show its new assertions failing first and not to check out paths in the shared checkout. It implemented first, then copied its files aside, restored the HEAD versions of `relationshipFolderResolve.ts` and `AddRelationshipFinalize.vue` in place, ran the spec red (4 failed, 2 passed), and copied its versions back. Delegation guidance says a needed baseline uses a separate temporary checkout or is reported back.

### Occurrences

- Execution: SEED-050#story-2 / `d3c9b4814d:.planning/slice-plans/021-relationship-notes-accepted-in-one-change/PLAN.md` / f8087d5845; Timestamp: unknown (2026-09-27, slice 5 work before commit 06ff81b916); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: unknown.
  - Evidence: slice 5 implementer return ("I put the HEAD versions of the resolver and the Vue file back temporarily ... restored my versions from temp copies"); delegation prompt's "never stash, reset, clean, checkout paths"; dough-execute-plan/references/delegation.md ownership bullet.
  - Observed effect: no damage; the coordinator checked the working tree afterwards and it held the intended diff. No other writer was active in the checkout at that time.
  - Inference: an implementer that writes code before its test finds the in-place restore the cheapest fails-first route; with a concurrent writer it could clobber or capture sibling work. Writing the test first, or a temporary worktree at HEAD, avoids it. Qualified: one occurrence; coordinator saw only the return.

- Execution: SEED-043#story-1 / `62e33981f0:.planning/slice-plans/005-sidebar-full-row-reveal/PLAN.md` / 56505b78dd; Timestamp: between 2026-09-28T11:11:30+08:00 and 2026-09-28T11:37:08+08:00 (slices 1–3, before commits 56505b78dd, 324b95f319, 68999d3505); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.44.
  - Evidence: slice 1 and 2 implementer returns ("restored the original files with `git show HEAD:<path> > <path>` … then put my edits back"; "I put HEAD's two sidebar item components in place … then restored my versions"). The coordinator's slice 2 and 3 delegation prompts themselves suggested "copy your file aside and write HEAD's via `git show HEAD:path > path`, then restore yours", beside the shared-checkout prohibitions; delegation.md asks for a separate temporary checkout.
  - Observed effect: no damage; each return's diff held the intended change and no other writer was active. Slice 3's implementer wrote the tests first and ran them red against untouched HEAD code, needing no swap.
  - Inference: the coordinator converted the finding's workaround into instruction; slice 3 shows test-first avoids it. Qualified: coordinator saw only the returns.

- Execution: SEED-055#story-1 / `2509421236:.planning/slice-plans/012-public-api-cleanup/PLAN.md` / be9f44cd11; Timestamp: unknown (2026-09-29, between 08:35 and 08:55+08:00, slices 2–4); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: coordinator conversation: one or two task-notifications per slice for the slice 2, 3 and 4 implementers with "stopped with background work of its own still running" / "This agent has not reported yet", each followed by the real hand-back after the full backend suite finished.
  - Observed effect: four status-only coordinator turns; delivery unaffected.

## ODF-143 — Managed delivery rejects unqualified branch references

Former local code: DD-107 (argument-only reports from plans 009, 011 and 019).

The usage line says REF but publication requires refs/heads/; callers discover the requirement through refused deliveries. Session identity attached successfully in these reports.

### Occurrences

- Execution: SEED-046 story 14 / slice-plans/009-rich-edit-keeps-final-newline / 983ac6d18e; Timestamp: 2026-09-27, ~07:30+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: first `deliver` refused with "authorized target must be a branch ref: origin/exec/009-rich-edit-keeps-final-newline"; the second, with `refs/heads/…` and abbreviated `--validated-candidate 983ac6d18e`, returned `candidate-mismatch` with nothing pushed; the third, with the full SHA, was accepted. `--host claude` without `--session-json` reported `observation.state: attached`, then `reused` (`/tmp/dough-ci-501/watch-gBNzg7`).
  - Observed effect: two refused calls before the only delivery; no coverage lost.
  - Inference: on 0.3.41 the session identity no longer needs recovery, but the two argument-shape refusals recorded for plans 035 and 037 still recur because the usage line names neither `refs/heads/` nor a full SHA.

- Execution: SEED-035#story-26 / slice-plans/011-preview-image-files-on-web-file-page / ee09a8a2cf; Timestamp: 2026-09-27T09:53:29+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42 (VERSION at 1eb6310816).
  - Evidence: coordinator transcript `23a26a0f…jsonl`: `deliver --target-ref story/preview-image-files` refused with "authorized target must be a branch ref: story/preview-image-files"; the retry with `refs/heads/story/preview-image-files` was accepted with `observation.state: attached` (`/tmp/dough-ci-501/watch-rVXFjb`); the coordinator had printed the usage line (`--target-ref REF`) before the first delivery.
  - Observed effect: one refused call; no coverage lost; slice 2 delivery used the full ref first time.
  - Inference: the `refs/heads/` refusal still recurs on 0.3.42; the usage line is what the coordinator read, and it does not say the ref must be fully qualified (only `references/publish-the-candidate.md` does).

- Execution: SEED-009#story-48 / `f321a7cb23:.planning/slice-plans/019-one-path-classifier/PLAN.md` / 83a98aad70; Timestamp: unknown (2026-09-27, first delivery after slice 1 commit 13:18:42+08:00, before slice 2 commit 13:28:46+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42 (VERSION unchanged since 1eb6310816).
  - Evidence: coordinator summary to the retrospective (no transcript): the first managed delivery failed with "authorized target must be a branch ref: story/one-path-classifier"; the retry with `refs/heads/story/one-path-classifier` succeeded; observer `/tmp/dough-ci-501/watch-9bTJQk`.
  - Observed effect: one refused call; no coverage lost.
  - Inference: same argument-shape refusal as the rows for plans 035, 009 and 011, still on 0.3.42.

- Execution: SEED-041#story-1 / slice-plans/003-frontend-update-reminder / 751f877d2a; Timestamp: unknown (2026-09-28, before the slice 1 delivery of 751f877d2a); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.43.
  - Evidence: coordinator printed the usage line (`--target-ref REF`), then grepped `execution-increment-delivery.mjs` and read `targetBranchName` in `publication-git.mjs` ("authorized target must be a branch ref") before the first `deliver`; both deliveries with `refs/heads/story/frontend-update-reminder` were accepted (`observation.state: attached`, then `reused`, `/tmp/dough-ci-501/watch-8FYr7O`).
  - Observed effect: no refused call; two extra source-reading calls replaced the usual refusal.
  - Inference: on 0.3.43 the usage line still does not state the required `refs/heads/` form; a coordinator either pays a refusal or reads the script source.

- Execution: SEED-039#story-4 / `7dca65eaf1:.planning/slice-plans/008-sut-start-timeout-race/PLAN.md` / d33dedc7c8; Timestamp: 2026-09-28T12:53:52+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: coordinator transcript `bdf6220e…jsonl`: `deliver --target-ref origin/story/sut-start-timeout-race` refused with "authorized target must be a branch ref: origin/story/sut-start-timeout-race"; the retry with `refs/heads/story/sut-start-timeout-race` was accepted (`/tmp/dough-ci-501/watch-TspK5B`).
  - Observed effect: one refused call (~3s); no coverage lost.
  - Inference: still recurs on 0.3.45; this time the guessed form was the remote-tracking name, which the usage line's `REF` also admits.

- Execution: SEED-055#story-1 / `2509421236:.planning/slice-plans/012-public-api-cleanup/PLAN.md` / be9f44cd11; Timestamp: 2026-09-29T08:29+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `deliver --target-ref origin/seed-055-public-api-cleanup` refused with "authorized target must be a branch ref: origin/seed-055-public-api-cleanup"; the retry with `refs/heads/seed-055-public-api-cleanup` was accepted (`/tmp/dough-ci-501/watch-3aU6BR`).
  - Observed effect: one refused call; no coverage lost.

- Execution: SEED-058#story-1 / slice-plans/013-blank-looking-body-stays-editable / 180136ce4a; Timestamp: 2026-09-29T11:15+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: after printing the usage line (`--target-ref REF`) and grepping the script for `targetRef`, `deliver --target-ref origin/exec/seed-058-story-1` refused with "authorized target must be a branch ref: origin/exec/seed-058-story-1"; the retry with `refs/heads/exec/seed-058-story-1` was accepted (`observation.state: attached`, `/tmp/dough-ci-501/watch-FeKCIi`).
  - Observed effect: one refused call plus two lookup calls; no coverage lost.

- Execution: SEED-059#story-2 / slice-plans/050-read-a-book-on-a-phone / d4a47402af; Timestamp: 2026-09-29T12:09+08:00 (slice 1 delivery); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: after printing the usage line (`--target-ref REF`) and grepping the script for `targetRef`, `deliver --target-ref origin/story/seed-059-story-2` refused with "authorized target must be a branch ref: origin/story/seed-059-story-2"; the retry with `refs/heads/story/seed-059-story-2` was accepted (`observation.state: attached`, `/tmp/dough-ci-501/watch-O2h2e5`).
  - Observed effect: one refused call plus one lookup call; no coverage lost; slices 2 and 3 used the full ref first time.

## ODF-074 — A plan said the changed script had no test, and nobody searched for one before delivery, so CI caught the stale test

Former local code: DD-126.

The plan for the commit-gate change recorded "No permanent automated test is added for the hook: it has none today, CI does not run it". `scripts/test/quality_changed.test` already tested `scripts/quality_changed.sh` with a fake `pnpm`, and CI runs it in "Run script unit tests". The implementer, the refactor agent and the coordinator's proof acceptance all relied on the plan's claim; the path-scoped `script` skill, which covers tests under `scripts/`, was not named in delegation and attached only after the first slice.

### Occurrences

- Execution: SEED-043 story 1 / slice-plans/045-commit-gate-checks-committed-content / 574d61b52c; Timestamp: 2026-09-26T16:06:02+08:00 (CI step failure); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.40.
  - Evidence: plan "Current decisions" before 120753a097; CI run 36228685291 job "Other Unit Tests" failed `quality_changed.test` ("shared biome config selects every affected component": expected `pnpm frontend:lint`, got the install line after `ln` failed); repair 120753a097 updated and extended the test.
  - Observed effect: one red story-branch CI run, a stash/repair/restore cycle around slice 2, and two extra agents (repair ~49k and refactor ~48k subagent tokens).
  - Inference: a negative claim that code has no test needs a search of the test tree (here `grep -rl quality_changed scripts/test`) at planning or delegation; naming the stack skill for `scripts/` in the delegation would likely have surfaced it.

### Additional report — The plan prescribed a comparison renderer and a trigger point that the implementer had to replace

Former local code: DD-130.

Slice 2's plan named `markdownToQuillHtml` as the renderer for judging meaning and a change of `markdownForRichDisplay` as the moment to check. Neither was tried at planning. `markdownToQuillHtml` removes whitespace between tags and drops task checkboxes, so it hides the losses the story is about. A watcher with `nextTick` ran before Quill had taken in the HTML. The implementer switched to plain `marked` and a new `QuillEditor` `modelLoaded` event, and reported both deviations.

#### Occurrences

- Execution: SEED-046 story 10 / `.planning/slice-plans/005-rich-editor-keeps-content/PLAN.md` / c5338213d0; Timestamp: 2026-09-26T22:16:23+08:00 (slice 2 commit 0e36045508); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: plan slice 2 "Change" and "Learnings" at 0e36045508; slice 2 implementer return (~579 s, ~102k subagent tokens).
  - Observed effect: slice 2 ran close to the 10-minute hard limit; the plan's Change text no longer describes the code and is corrected only by its Learnings.
  - Inference: a plan that names the exact function or hook for a new check should name it as a suggestion unless a quick probe confirmed it; here a one-line call of `markdownToQuillHtml` on `**a** *b*` versus `**ab**` would have shown the problem. Qualified: the deviation was handled well and cost one slice's margin, not a retry.

### Additional report — The plan's E2E proof command named a feature directory, which the isolated runner refuses

Former local code: DD-134.

Plan 010 listed `pnpm cy:run --spec e2e_test/features/folder_organization`. The isolated E2E runner requires explicit feature files, so the implementer had to find and list them.

#### Occurrences

- Execution: SEED-046#story-9 / `.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md` at d305df9c23 / 9365a11c7d; Timestamp: unknown (2026-09-27, slice 2, before 10:20:32+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: slice 2 implementer report: the literal plan command "is refused by the isolated runner, which requires explicit feature files"; it ran four `.feature` files instead (18/18 passed); the plan's command was corrected in 5d384c2665.
  - Observed effect: a small detour inside the slice; proof unaffected.
  - Inference: slice planning did not run or check the E2E command it wrote.

### Additional report — The plan probed today's outcome for one key example but stated another's without a probe, and the wrong premise hid an ordering change

Former local code: DD-139.

Plan 019 recorded a throwaway probe for key example 1 (leftover `.keep` → 409). For example 2 it wrote "each failing first (accepted as a file today)" for `Forces.MD` without a probe. Today's outcome was a 400 from attachment size admission ("must be a Git LFS pointer or empty file"), because size admission runs before tree-shape checks. The implementer found this at the red run and moved the whole-tree path refusals before size admission, a design change the plan had not planned.

#### Occurrences

- Execution: SEED-009#story-48 / `f321a7cb23:.planning/slice-plans/019-one-path-classifier/PLAN.md` / 83a98aad70; Timestamp: unknown (2026-09-27, slice 3 work between commits 2f0afdbce8 13:28:46+08:00 and 82fa7aedb7 13:47:23+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: original plan slice 3 Proof and "Starting facts" probe (example 1 only) at f321a7cb23; plan Learnings at 6de22982d5 ("Before slice 3, `Forces.MD` was refused as a non-pointer attachment, not accepted"); `NotebookGitProposalPublisher.publish` in 82fa7aedb7 (refusals moved above `NotebookGitAttachmentSizeAdmission.admit`); coordinator summary (slice took ~15 min).
  - Observed effect: slice 3 ran ~15 min, over the 10-minute hard limit; the outcome was complete and correct, and the refactor pass then co-located slice 2's marker refusal with it.
  - Inference: the probe that settled example 1 would have taken seconds for example 2 and would have shown the ordering dependency at planning. Related in kind to DD-130 and DD-137 (untried premise), but here the premise was about today's refusal path. Qualified: coordinator summary only; implementer transcript not available.

## ODF-147 — An implementer reasoned that a new test would fail instead of running it red, and one of its tests could not fail

Former local code: DD-127.

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
- Execution: SEED-046#story-5 / `.planning/slice-plans/006-web-edit-changes-only-edit/PLAN.md` at 2a8fd844b4 / 5f71d4d237; Timestamp: between 2026-09-26T23:37:35+08:00 and 2026-09-26T23:43:24+08:00 (slice 5 quoted-rename test, before commit 2f2cd179cf) and between 2026-09-26T23:53:42+08:00 and 2026-09-27T00:00:58+08:00 (slice 8 flow-list test, before commit 06394ff704); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: the coordinator itself added `quotes a renamed key that YAML needs quoted` and `removeWikiLinksFromLeadingFrontmatterProperties_cutsOnlyTheEmptiedFlowListItem` and accepted them by reasoning ("without the fix it would emit `a: b: demo`") after only a green run; the slice implementers' reports also named no red runs.
  - Observed effect: both tests are plausibly meaningful, but neither was observed failing.
  - Inference: this time the coordinator, not an implementer, substituted reasoning for the red run; the delegation prompts again did not ask for one.

## ODF-148 — Refined key examples promised link-rewrite outcomes that the existing rewrite rules do not produce

Former local code: DD-128.

The story said link rewriting stays unchanged, yet its key examples stated rewrite results nobody checked against that code. Example 2 promised that renaming folder `physics` rewrites `[[Science:physics/Force]]` in another notebook; `PortablePath.withRenamedFolder` returns notebook-qualified links unchanged, so folder rename never touches another notebook. Examples 2 and 4 also gave shorthand results (`[[Science:Force]]`-style) where the rules produce `[[Science:/Force]]` (dissolve) and `[[Physics:Force|Science:Force]]` (move to another notebook). Slice 3 found the conflict. The coordinator kept the "rewriting unchanged" exclusion, dropped folder rename from example 2 in the plan's Current decisions, and left the seed promise as written.

### Occurrences

- Execution: SEED-035 story 25 / `.planning/slice-plans/047-link-rewrites-in-other-notebooks-reach-git/PLAN.md` at 0ef2828d32 / 9861bc80c2; Timestamp: 2026-09-26, ~18:27+08:00 (slice 3 commit f63760e183 records the decision); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: refinement/plan 27673389c0 (seed key examples 2 and 4; plan slice 3 "Folder rename, move and dissolve"); `backend/src/main/java/com/odde/donut/algorithms/PortablePath.java` `withRenamedFolder` (early return when `notebookQualifier.isPresent()`); plan "Current decisions" in f63760e183; test expectations in `NotebookGitWebLinkingNotebookControllerTest` (`[[Science:/Force]]`, `[[Physics:Force|Science:Force]]`).
  - Observed effect: one owner-visible promise was dropped during execution by a plan note, without an owner decision or a seed edit. No extra slice was needed. The seed still promises the folder-rename case at closure.
  - Inference: when a story says an existing rule stays unchanged, check each key example's expected text against that rule (its code or tests) at refinement. Also, a promise dropped because it conflicts with an exclusion should reach the owner at completion, not only the plan. Qualified: the coordinator summary is the only process record, so how long the discovery took is unknown.

## ODF-149 — The "stays editable" boundary examples were all one-line bodies, so a check that refuses wrapped text shipped

Former local code: DD-129.

Story-10's style-only boundary (key example 5) and every editable test case used bodies with no line wrapped inside a paragraph or list item. The plan asserted that comparing renderings lets style-only changes pass "without special cases" without trying a hard-wrapped body. The implementer, the coordinator's extra probe (wiki links, tables, images, CJK) and the refactor agent never tried one either. The retrospective's first probe of `line one\nline two` found it refused, although the editor's save renders the same.

### Occurrences

- Execution: SEED-046 story 10 / `.planning/slice-plans/005-rich-editor-keeps-content/PLAN.md` / c5338213d0; Timestamp: 2026-09-26T22:19:00+08:00 (retrospective probe); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: plan "Current decisions" (planning, rendered-HTML comparison); `RichMarkdownEditor.bodyItCannotKeep.spec.ts` editable cases at 0e36045508; `richEditorKeepsBody.ts` normalizes only `/>\s*\n\s*</`; correction plan `008-hard-wrapped-note-stays-editable`.
  - Observed effect: a regression against the story's own style-only promise reached the published story branch; a correction story and plan were needed before integration.
  - Inference: when a story's promise is "ordinary content keeps working", its boundary examples need a realistic sample of that content (here, a hard-wrapped paragraph such as any file in this repo), not only minimal constructs. The seed's value note already asked how many real notes the editor cannot carry.

## ODF-139 — The coordinator accepted an implementer's reported gap as out of scope without checking the story, and the example test pinned the defect

Former local code: DD-132.

Slice 2's implementer reported that a rich body edit drops the file's final newline and that its example 1 test pins this (`note.replace("# Demo2\n", "# My Demo2")`). The coordinator judged it "body-side, pre-existing, outside the promises" and recorded it as a learning. The story's goal is that a web edit changes only what the user edited, and its exclusions list other body styles and blank-line runs, not the final newline. The retrospective probed `First\n\nLast\n` with a first-line edit: the unedited last line loses its newline, and the server does not add it back.

### Occurrences

- Execution: SEED-046#story-5 / `.planning/slice-plans/006-web-edit-changes-only-edit/PLAN.md` at 2a8fd844b4 / 5f71d4d237; Timestamp: 2026-09-26, before 23:25:41+08:00 (slice 2 acceptance; commit 61d400007d); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.41.
  - Evidence: slice 2 implementer return ("Gaps: Example 1's trailing newline is lost"); plan 006 Learnings "Slice 2" ("outside this story's promises"); retrospective probe output `"My First\n\nLast"`; correction `4ba13d691c:.planning/slice-plans/009-rich-edit-keeps-final-newline/PLAN.md` (SEED-046#story-14).
  - Observed effect: a defect against the story's own goal reached the published story branch; a correction story and plan are needed before integration.
  - Inference: a reported gap should be checked against the story's goal and exclusions list before it is filed as out of scope; the fix at slice 2 would have been a few lines in the same function. Qualified: as in DD-129, the key example was a file whose edited line was also its last line, which hid the effect.

## ODF-127 — `agent-commit.mjs` run through the `.claude/skills` symlink exits 0 without committing

Former local code: DD-133.

`agent-commit.mjs` runs its CLI body only when `resolve(process.argv[1]) === fileURLToPath(import.meta.url)`. Invoked as `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs` from the worktree root, Node resolves the module to the real `.agents/skills/…` file while argv keeps the symlink spelling, so the script does nothing, prints nothing, and exits 0. The sibling scripts that use `ci-direct-entry.mjs` `isDirectCliEntry` (realpath comparison) do not have this problem.

### Occurrences

- Execution: SEED-035#story-26 / slice-plans/011-preview-image-files-on-web-file-page / ee09a8a2cf; Timestamp: 2026-09-27T09:51:11+08:00 (slice 1 commit); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42 (VERSION at 1eb6310816).
  - Evidence: coordinator transcript `23a26a0f…jsonl` 09:51:11–09:52:45+08:00: two commit calls through `.claude/skills/…/agent-commit.mjs` returned no output and `exit=0` with the change still staged; a probe with `-m x --bogus` also exited 0 silently; the same call through `.agents/skills/…` returned `{"ok":true,"status":"committed","agent":"Hibiki-chan","sha":"ee09a8a2cf…"}`. Earlier `--help` through the alias worked only because the shell had `cd` into the symlinked directory (physical cwd). `.claude/skills/dough-execute-plan` is a symlink to `../../.agents/skills/dough-execute-plan`; the delivery receipt reports `runtime.alias: ".claude"`.
  - Observed effect: four extra coordinator calls (about 1.5 minutes) before slice 1 was committed; a silent exit 0 looks like success, so an unattended caller could have delivered without the commit.
  - Inference: `execution-start.mjs` and `ci-repair-stash.mjs` use the same literal check and would likely fail the same way through the alias; using `isDirectCliEntry` in all three would remove the cause.
- Execution: SEED-046#story-9 / `.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md` at d305df9c23 / 9365a11c7d; Timestamp: 2026-09-27, before 10:02:20+08:00 (the slice 1 commit, made after switching to the `.agents` path); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: two runs of `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -m …` in the execution worktree exited 0 with empty output and `git log` still at d305df9c23; the same command via `.agents/skills/…` returned `{"ok":true,"status":"committed",…,"sha":"9365a11c7d…"}`. `execution-start.mjs start` did run through the `.claude` path in the same session.
  - Observed effect: two wasted commit attempts and a diagnosis; nothing was committed wrongly.
  - Inference: any entry script with this guard fails silently when started through the symlink; a guard that compares real paths (or `.agents` paths in the guidance) would avoid it. Which other scripts share the guard was not checked.
- Execution: SEED-047#story-2 / `72210f216a:.planning/slice-plans/015-removal-landing-listing-load/PLAN.md` / 46e3a106e3; Timestamp: 2026-09-27, before 11:56:35+08:00 (the slice 1 commit, made after switching to the `.agents` path); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: two runs of `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -m …` from the worktree root exited 0 with empty output, `git log` still at 5a4bfe2f92; the `.agents/skills/…` path returned `{"ok":true,"status":"committed","agent":"Akiho-chan","sha":"46e3a106e3…"}`. `execution-start.mjs start` ran through the `.claude` path from the main checkout in the same session.
  - Observed effect: three extra coordinator calls; nothing committed wrongly. Third retained occurrence; the coordinator knew nothing of the earlier ones at call time.
- Execution: SEED-009#story-47 / `.planning/slice-plans/018-cross-notebook-folder-move-names/PLAN.md` at c526dba221 / 732dbab312; Timestamp: 2026-09-27, before 13:17:42+08:00 (the slice 1 commit, made after switching to the real `.agents` path); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: `printf … | node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -F -` and then `-F <file>` from the worktree root both exited 0 with empty output and `git log` still at 2efbb21bba; the `realpath` (`.agents/skills/…`) invocation returned `{"ok":true,"status":"committed","agent":"Tsubomi-chan","sha":"732dbab312…"}`.
  - Observed effect: four extra coordinator calls, including reading the script to find the guard; nothing committed wrongly. Fourth retained occurrence, still unknown to the coordinator at call time.
- Execution: SEED-050#story-1 / `.planning/slice-plans/020-validate-changed-markdown-once/PLAN.md` / f8b186cc7f; Timestamp: 2026-09-27, ~16:00+08:00 (slice 1 amend); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs --amend` (bare, and with `-F -`) from the worktree root exited 0 with no output, HEAD still 759f43d897 without the developer trailer; the `realpath` invocation returned `{"ok":true,"status":"amended","agent":"Kaoru-chan","sha":"f8b186cc7f…"}`.
  - Observed effect: four extra calls, including reading the script; the first commit was made with plain `git commit` and had to be amended. Fifth retained occurrence, unknown to the coordinator at call time.
- Execution: SEED-050#story-8 / `.planning/slice-plans/026-one-folder-entry-rule/PLAN.md` / 649e162dd4; Timestamp: 2026-09-27, ~17:04+08:00 (slice 1 commit); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: unknown.
  - Evidence: two runs of `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -m …` from the worktree root exited 0 with empty output, HEAD still ce8390fdd2; the `.agents/skills/…` path returned `{"ok":true,"status":"committed","agent":"Aino-chan","sha":"649e162dd4…"}`. `execution-start.mjs start` ran through the `.claude` path from the main checkout in the same session.
  - Observed effect: three extra coordinator calls, including reading the script's guard; nothing committed wrongly. Sixth retained occurrence, unknown to the coordinator at call time.
- Execution: SEED-050#story-4 / `35972d89e4:.planning/slice-plans/023-one-notebook-tree-model/PLAN.md` / 3e9f034932; Timestamp: 2026-09-27T16:27:27+08:00 (slice 1 commit, made after switching to the `realpath`); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: two runs of `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -m …` from the worktree root exited 0 with empty output and `git log` still at 9e1aeb19df; the `realpath` invocation returned `{"ok":true,"status":"committed","agent":"Maria-chan"}`. `execution-start.mjs` carries the same guard (line 39) but succeeded through `.claude/skills/…` because it ran from the default checkout, where `.claude/skills/dough-execute-plan` is a real directory; in the worktree it is a symlink to `../../.agents/skills/dough-execute-plan` created by worktree setup.
  - Observed effect: three extra coordinator calls, including reading the script; nothing committed wrongly. Seventh retained occurrence.
  - Inference: the silent exit is specific to worktree-prepared skill links, so every execution that commits from its worktree hits it until the guard compares real paths or worktree setup stops symlinking.
- Execution: SEED-050#story-5 / `4426190218:.planning/slice-plans/024-finished-transitions-leave-no-trace/PLAN.md` / 48d5a2e8f8; Timestamp: 2026-09-27, before the slice 1 commit 48d5a2e8f8; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -m …` from the worktree root twice exited 0 with empty output while `git log` stayed at 45aafefb9f with the change staged; reading the script's guard led to the `realpath` invocation, which returned `{"ok":true,"status":"committed","agent":"Mihiro-chan","sha":"48d5a2e8f8…"}`. `execution-start.mjs start` and `execution-increment-delivery.mjs deliver` (the latter always run by real path) worked.
  - Observed effect: four extra coordinator calls; nothing committed wrongly. Eighth retained occurrence, unknown to the coordinator at call time.
- Execution: SEED-050#story-9 / `.planning/slice-plans/027-dissolve-enters-folders-by-the-one-rule/PLAN.md` / 6d48bad1b5; Timestamp: 2026-09-27T18:11:11+08:00 (slice 1 commit, made after switching to the `.agents` path); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: two runs of `node .claude/skills/dough-execute-plan/scripts/agent-commit.mjs -m …` from the worktree root exited 0 with empty output, HEAD still 13721c9182 with the change staged; after reading the script's guard, the `.agents/skills/…` path returned `{"ok":true,"status":"committed","agent":"Shunka-chan","sha":"6d48bad1b5…"}`.
  - Observed effect: three extra coordinator calls; nothing committed wrongly. Ninth retained occurrence, unknown to the coordinator at call time.

## ODF-100 — Agents reported vue-tsc's exit code from a pipe into `tail`, so the coordinator had to rerun the typecheck

Former local code: DD-135.

The frontend proof requires `vue-tsc --noEmit` to pass. Two agents ran it as `... vue-tsc --noEmit | tail`, then reported "exit 0". That code is `tail`'s, not vue-tsc's. Both agents said so themselves, and the coordinator reran the typecheck without the pipe before accepting.

### Occurrences

- Execution: SEED-033#story-2 / `9c8aca9bbd:.planning/slice-plans/012-read-note-context/PLAN.md` / 99aa915e22; Timestamp: 2026-09-27T09:52:41+08:00 (slice 1 acceptance, before commit 99aa915e22) and 2026-09-27T10:16:30+08:00 (slice 5 refactor acceptance, before commit 3712c94363); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: slice 1 implementer return ("The exit code I captured was the pipe's final `tail`, not vue-tsc's own"); slice 5 refactor return (same remark); coordinator reruns `vue-tsc --noEmit >/dev/null 2>&1; echo $?` → 0 both times. Later delegation prompts that said "report its real exit code (don't pipe it into tail)" got a correct exit code.
  - Observed effect: two extra typecheck runs, about a minute each; no wrong result was accepted.
  - Inference: a delegated command whose pass/fail matters should be given with its exit-code capture spelled out, since agents tend to trim long output with `tail`. Qualified: small cost, and the agents reported the problem honestly.
- Execution: SEED-050#story-8 / `.planning/slice-plans/026-one-folder-entry-rule/PLAN.md` / 649e162dd4; Timestamp: 2026-09-27, ~17:03+08:00 (slice 2 acceptance) and ~17:13+08:00 (coordinator rerun); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: unknown.
  - Evidence: slice 2 implementer return ("I piped it through `tail`, so the `EXIT=0` shown is tail's exit code"); the delegation prompt did not spell out exit-code capture; the coordinator first accepted on "no error output", then reran `vue-tsc --noEmit >log 2>&1; echo $?` during the retrospective → 0, no `error TS`.
  - Observed effect: one extra typecheck run; no wrong result accepted, but acceptance briefly rested on absent output rather than an exit code.
- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T05:05:14Z (slice 4 refactor acceptance); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: slice 4 refactor agent ran `vue-tsc --noEmit 2>&1 | tail -5; echo tsc=$?` and reported "The `tsc=0` echo only captured the exit status of `tail`"; the coordinator reran the typecheck before formatting.
  - Observed effect: one extra typecheck run; no wrong result accepted.

## ODF-150 — An implementer's slice proof ran only the specs it chose, missing consumers of the store method it changed

Former local code: DD-136.

The slice changed `StoredApiCollection.trashNote` to request a folder listing before trashing. The implementer proved it with `tests/store`, `tests/toolbars` and two `NoteMoreOptions` specs; `tests/notes/NoteMoreOptionsForm.trashNote.spec.ts`, which also drives `trashNote`, was not run and failed on the unmocked request (ADR 0006: an unmocked request fails loudly). Possibly the same root cause as ODF-111 (proof chosen by the edited area rather than by the changed method's consumers), but there the actor was the refactor pass; matching is uncertain.

### Occurrences

- Execution: SEED-047#story-1 / `edfd7ba92a:.planning/slice-plans/014-continue-to-neighboring-note-after-deletion/PLAN.md` / a41b1e0507; Timestamp: unknown (before slice 1 commit 2026-09-27T10:59:16+08:00); Tool: Claude Code; Open Dough release: 0.3.42 (VERSION in the execution checkout).
  - Evidence: coordinator summary to the retrospective (subagent transcripts not supplied): coordinator consumer check found 2 failing tests in `NoteMoreOptionsForm.trashNote.spec.ts`; fixed by an empty listing mock in `tests/notes/noteMoreOptionsTrashTestSupport.ts` (in a41b1e0507), then `tests/notes tests/store tests/toolbars` 353/353.
  - Observed effect: one coordinator repair before commit; no defect shipped.
  - Inference: a `grep` for callers of the changed method across `tests/` when choosing slice proof would have included the spec.

- Execution: SEED-049#story-1 / slice-plans/016-note-store-architecture / 3821dbd9c79c7ab25e68cd1ff96aa44061d6544f; Timestamp: unknown (2026-09-27, slice 2 delivery and slice 3 proof); Tool: Codex; Open Dough release: 0.3.42 (unchanged installed guidance across this execution).
  - Evidence: chat 01a0e0fe-e37c-7512-a6b3-b8d4c1318365, slice2 return and slice3 messages; a08a318db1 changed trash to throw, while its 69-test selection omitted NoteShowPage.autosaveTrash. CI run 36294751826 attempt 1, job 108551410141 reports that spec's unhandled Vue warning. Slice3's broader local selection independently found the same error; d697a1326b adds the test-only expected-error observation.
  - Observed effect: one failed published frontend job and one bounded test repair; 124 focused tests then passed, followed by the 1,938-test full frontend run in slice6.
  - Inference: consumer inspection needs callers of the whole removal flow and their refusal scenarios, not only direct store-call specs. Production error propagation itself was intended.

## ODF-151 — A retrospective finding asserted the loading modal, which the product does not show for these requests

Former local code: DD-137.

The SEED-047#story-1 retrospective wrote that the removal request "shows the loading modal at once" and planned proof that observes `GlobalApiLoadingModal`. Non-blocking `apiCallWithLoading` calls, including the trash request and every user-triggered folder listing load, only add a busy state shown as `LoadingThinBar`; `LoadingModal` needs `blockUi: true`. The implementer's first modal-based test failed after the fix, which exposed the wrong premise; it switched to observing `apiStatus.states` and reported the correction.

### Occurrences

- Execution: SEED-047#story-2 / `72210f216a:.planning/slice-plans/015-removal-landing-listing-load/PLAN.md` / 46e3a106e3; Timestamp: 2026-09-27T11:56:35+08:00 (slice 1 commit); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.42.
  - Evidence: plan 015 "Current findings" 1 and "Outside-in proof" at 5a4bfe2f92; plan "Learnings" at 46e3a106e3; implementer return (~322 s, ~88k subagent tokens) naming the failed modal assertion `expected null to be truthy`.
  - Observed effect: one discarded test attempt inside the slice budget; the story goal ("busy … as for other user-triggered folder listings") still held, so no scope question arose.
  - Inference: a retrospective finding about user-visible behaviour was stated from code reading of the caller only, without checking how the shared loading helper is rendered. Qualified: related in kind to DD-130 (plan names an untried mechanism), but a different concrete problem.

## ODF-152 — The file-size rule conflicted with an approved staged simplification and mechanical callers

Former local code: DD-138.

The refactor check requires every changed file to be at most 250 lines, without distinguishing intermediate planned decomposition or a mechanical change to a pre-existing oversized caller. Refactor agents raised both cases during this execution; the coordinator applied the owner-approved store scope and slice ordering rather than expanding the work.

### Occurrences

- Execution: SEED-049#story-1 / slice-plans/016-note-store-architecture / 3821dbd9c79c7ab25e68cd1ff96aa44061d6544f; Timestamp: unknown (2026-09-27, refactor1 and refactor6 handoffs); Tool: Codex; Open Dough release: 0.3.42.
  - Evidence: chat 01a0e0fe-e37c-7512-a6b3-b8d4c1318365, refactor1 questioned the 384-line intermediate store before planned slices4–6; refactor6 questioned useWikidataPropertyDialog after mechanical migration (300 lines at its base, then smaller). Rule: dough-post-change-refactor/references/refactor-checks.md, File size. Source: SEED-049 owner-approved single command/undo split; slice7 owns store size proof.
  - Observed effect: two applicability exchanges; no extra split was made. Final noteStore is 245 lines, noteUndo167, requests228, cache45. The unrelated Wikidata workflow was preserved.
  - Inference: clarify how the numeric check composes with approved intermediate states and the skill's requirement that a refactor address an introduced, exposed, or aggravated issue. Unlike ODF-124, the rule was found and acknowledged here.

## DD-142 — The plan prescribed observations that execution had to drop: an absence check for removed UI and a case that could never fail

Slice planning wrote two observations into the executable plan that the delivered tests could not keep. Slice 1 asked for "there is no `[aria-label="Ancestor folders scrolled out of view"]` element" after deleting that hint, against the owner's rule that removals leave no trace. Slice 3 asked that re-sorting a short folder whose rows all fit leaves `scrollTop` unchanged, which holds whatever the product does, because such a tree cannot scroll.

### Occurrences

- Execution: SEED-043#story-1 / `62e33981f0:.planning/slice-plans/005-sidebar-full-row-reveal/PLAN.md` / 56505b78dd; Timestamp: 2026-09-28T11:16:07+08:00 (slice 1 commit) and between 2026-09-28T11:32:13+08:00 and 2026-09-28T11:37:08+08:00 (slice 3); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.44.
  - Evidence: plan at 791011100b, slices 1 and 3 Behavior bullets; slice 1 refactor return (removed the absence assertion); slice 3 implementer return ("The short-folder test passed before the change"); slice 3 refactor return ("cannot fail as written … `scrollTop` is always 0"); coordinator removed it before 68999d3505, recorded in the plan.
  - Observed effect: one assertion written then removed, one test written, run, and removed; two plan-text corrections during delivery. No behavior defect.
  - Inference: a planning check that each planned observation can fail on the pre-change code, and does not assert a removed feature's absence, would have caught both. Qualified: one execution; related to ODF-147 (an implementer's test that could not fail) but here the plan prescribed it and the implementer ran it red.

## DD-143 — An implementer prompt required a nonexistent `pnpm test:typecheck` script

The Slice 1 implementer was told to run `./scripts/run.sh pnpm test:typecheck` after regenerating the API client. That script is not defined at the repo root or under `frontend/`. The project's frontend skill names `pnpm -C frontend exec vue-tsc --noEmit`. The agent substituted that command and reported the gap.

### Occurrences

- Execution: SEED-053#story-1 / `12c0f629ac:.planning/slice-plans/007-file-page-references/PLAN.md` / c86eb7eba0; Timestamp: 2026-09-28T12:24:00+08:00 (Slice 1 implementer start; delivery at 12:32:48+08:00); Tool: Cursor; Model: gemini-3.8-flash; Open Dough release: 0.3.45.
  - Evidence: implementer prompt in transcript `ff698959-2f7d-4392-a127-0f3f0adcc8f8/subagents/b7997c35-374a-4b20-824a-dca12fb505b1`; return notes "no root `pnpm test:typecheck` script" and used `pnpm -C frontend exec vue-tsc --noEmit`. Root/frontend `package.json` have no `test:typecheck`. Frontend skill "Frontend proof" documents the vue-tsc command.
  - Observed effect: one extra discovery step inside the slice; typecheck still passed via the substituted command. No false green.
  - Inference: the coordinator prompt invented a script name instead of copying the frontend skill's command. Qualified: different from ODF-100 (piped vue-tsc exit code), which assumed the correct command.

## DD-144 — A four-line repository tip-over forced extracting an unrelated assimilation query block

Slice 2 added one content-contains query to `NoteRepository` (244 → 254 lines). The post-change refactor's 250-line check then required a split; the agent extracted the pre-existing assimilation query block into `NoteAssimilationQueries` (~85 lines moved) so the file dropped to 180. The assimilation seam is cohesive, but it was not implicated by file-page references except as the cheapest way under the numeric limit.

### Occurrences

- Execution: SEED-053#story-1 / `12c0f629ac:.planning/slice-plans/007-file-page-references/PLAN.md` / bc870053e8; Timestamp: 2026-09-28T12:37:00+08:00 through 2026-09-28T12:40:26+08:00 (Slice 2 refactor through delivery); Tool: Cursor; Model: gemini-3.8-flash; Open Dough release: 0.3.45.
  - Evidence: refactor transcript `ff698959-2f7d-4392-a127-0f3f0adcc8f8/subagents/2b60056d-ca4d-4cae-8a47-c79402587e9f` (decision pass: File size 254; learning "Slice 2's candidate query pushed NoteRepository over 250"); commit `bc870053` adds `NoteAssimilationQueries.java` and shrinks `NoteRepository.java`. Pre-Slice-2 `NoteRepository` at `c86eb7eba0` was 244 lines.
  - Observed effect: ~8 minutes of refactor time and an assimilation-focused re-proof (`AssimilationControllerTests`) for a tip-over caused by one new query.
  - Inference: the hard 250-line ceiling can force relocating a large untouched block when a small addition crosses it. Related in theme to ODF-152 (numeric check applicability), but here the agent performed the split rather than escalating a staged-simplification conflict.

## DD-145 — The plan prescribed production observations whose access route or log source did not exist, and whose results could not change the approach

Plan 009 required a SQL catalog probe "through the established authorized DB connection" (slice 1), a disposable Cloud SQL vector rehearsal (slice 3), and Flyway D/P success "in the serving instance's application log (`gcloud logging`)" (slice 10). No DB connection route was established for agents, the app's log does not reach Cloud Logging, and the chosen `DROP TABLE` worked whatever the catalog showed.

### Occurrences

- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T05:36:39Z–06:03:39Z (slice 1/3) and 2026-09-28T07:17:43Z–07:18:10Z (slice 10); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: four auto-mode denials while seeking a DB route (credential lookup, SSH to the app VM, probe edit to root, bucket IAM); owner: "But this wasn't needed uh, previously. Um, or can we skip this?"; coordinator's covering reasoning and skip recorded in d111968b62; slice 10 `gcloud logging read` found no Flyway lines, so sustained health became the D/P evidence (8e03ac5f5f).
  - Observed effect: about 27 minutes of owner-attended probing ended in skipping slice 1's SQL part and dropping slice 3; slice 10's named proof was replaced during delivery. No product defect.
  - Inference: planning could have asked, for each production observation, whether any result would change the approach, and whether the access route and log source exist (both checkable cheaply once `gcloud` auth worked). Related to DD-142 (prescribed observations dropped in execution), but here the cost was production access and owner time. Qualified: one execution; planning-time `gcloud` auth had failed.

## DD-146 — Transient permission-check outages ended the coordinator's turn three times, so the owner had to type "continue"

The auto-mode classifier returned "no verdict" errors, which the tool result called transient and retryable. The coordinator each time reported and ended its turn instead of waiting in the background and retrying, and resumed only when the owner wrote "continue".

### Occurrences

- Execution: SEED-051#story-1 / `f35fa810f1:.planning/slice-plans/009-retire-note-embeddings/PLAN.md` / 5003fbecc8; Timestamp: 2026-09-28T06:32:45Z, 06:39:37Z, 06:42:33Z (stops); owner resumes 06:37:14Z, 06:42:06Z, 06:51:34Z; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.45.
  - Evidence: coordinator texts "I'm pausing here", "Stopping here rather than retrying in a loop", "Slice 9 hasn't started"; the next command after the third resume succeeded.
  - Observed effect: about 19 minutes of stalled execution and three owner prompts; no wrong action.
  - Inference: a bounded background wait (for example a timed Monitor) before one retry would likely have avoided the stops. Qualified: host-specific outage; retrying immediately in a loop was correctly avoided.

## DD-147 — The plan's focused proof command passed several `--tests` patterns to a wrapper that accepts only one

Plan 012 slice 1 named `pnpm backend:test:worktree --tests A --tests B --tests C` as its proof. `scripts/backend-test-worktree.sh` accepts exactly one `--tests` pattern, so the command printed usage and exited 1. The implementer split it into two single-pattern runs that together selected every rewritten class.

### Occurrences

- Execution: SEED-055#story-1 / `2509421236:.planning/slice-plans/012-public-api-cleanup/PLAN.md` / be9f44cd11; Timestamp: 2026-09-29T08:24+08:00 (slice 1 implementer); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 1 implementer hand-back ("accepts exactly one `--tests <pattern>`, so it printed usage and exited 1"); plan learning added in be9f44cd11.
  - Observed effect: one failed run and a reselection inside the slice; proof unaffected.
  - Inference: slice planning copied Gradle's repeatable `--tests` form without checking the worktree wrapper. Related to DD-143 (an unchecked command in a prompt); qualified: one occurrence.

## DD-148 — A refactor agent's hand-back contained only the word "placeholder"

The slice 6 refactor agent (a Sonnet model, for a planning-only change) returned "placeholder" with no `## REFACTOR COMPLETE`. The coordinator checked the diff, messaged the agent, and received the real report, which named two small edits it had already made.

### Occurrences

- Execution: SEED-055#story-1 / `2509421236:.planning/slice-plans/012-public-api-cleanup/PLAN.md` / be9f44cd11; Timestamp: 2026-09-29T09:05+08:00 (approximately; slice 6 refactor); Tool: Claude Code; Model: coordinator claude-opus-5-5, refactor agent Sonnet; Open Dough release: 0.3.46.
  - Evidence: first hand-back text "placeholder"; second hand-back listed the `NotebookAttachmentFile.bytes` wording fix and the `nosniff` label, then `## REFACTOR COMPLETE`.
  - Observed effect: one extra message round trip; the marker rule stopped acceptance of an empty report.
  - Inference: qualified one-off; host or model behavior, not project guidance.

## DD-156 — A two-hour UAT stopped at 56 minutes while cheap coverage gaps stayed open

The owner set a two-hour manual UAT budget. The exploration agents stopped once breadth was complete (27 and 29 minutes), and the coordinator's synthesis prompt supplied the report's explanation that the remaining gaps needed other browsers, devices, or book sizes rather than more time. Two listed gaps were testable in the same setup: a mid-size book for the AI reorganization limit (the CLI can attach part of a PDF with `DONUT_MINERU_PDF_END_PAGE`) and EPUB heading-only auto-marking.

### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T08:36+08:00 (second exploration part ends); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: seed `## UAT Findings` time list and `### Coverage gaps`; plan learnings for the two exploration parts; the coordinator's synthesis prompt asked for "the remaining gaps need other browsers, touch devices, or book sizes rather than more time".
  - Observed effect: 64 budget minutes unused; the size at which AI reorganization fails (defect 10) stays unknown.
  - Inference: exploration prompts could say to spend leftover budget on the cheapest open gaps before stopping, and the coordinator should not pre-write the report's justification. Qualified: one execution; the story calls the budget a limit, not a target.

## DD-157 — Per-slice refactor passes on a documentation-only UAT report kept restructuring the previous slice's text

Each of three report-writing slices ended with a fresh post-change refactor agent. The second pass merged lists that the first pass had just shaped (two "what worked" notes, two coverage-gap lists, two small-items lists), because the report's final shape only exists after the last slice.

### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T08:45+08:00 (second refactor pass); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: refactor returns for the three slices (about 72k, 85k, and 74k subagent tokens); the second return lists merges of lists written by the first slice.
  - Observed effect: about 230k tokens and three agent round trips for text-only changes; the merges were useful but partly repeated.
  - Inference: a single refactor pass after the last report slice, or slicing the UAT so only the final slice writes the synthesis and structure, may give the same result for less. Qualified: the passes also fixed real duplication and wording.

## DD-158 — `execution-start.mjs` guidance names a "publisher ID" without its flag, and `--publisher` is refused

The execute-plan skill text lists a "stable execution publisher ID" as a start input without naming the flag. The coordinator passed `--publisher`, which the script refused as `missing publisherId`; the usage line shows `--publisher-id`.

### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T07:22+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: start result `{"ok":false,"status":"invalid-request","error":"missing publisherId"}`; usage line in `execution-start.mjs`.
  - Observed effect: one refused call and a usage lookup; no state change.

## DD-160 — The file-size check split an untouched block in one slice of an execution and was waived in a later slice

Within one execution, the post-change refactor treated an already-oversized file differently in two slices. In slice 1, a two-line class change to `BookReadingBookLayout.vue` (354 lines before the change) led the refactor agent to move the unrelated drag-to-indent pointer handling into a new composable. In slice 3, `BookReadingContent.vue` (474 → 453 lines) was left over the limit as "not caused nor worsened by this slice".

### Occurrences

- Execution: SEED-059#story-2 / slice-plans/050-read-a-book-on-a-phone / d4a47402af; Timestamp: 2026-09-29T12:00+08:00 through 2026-09-29T12:40+08:00 (slice 1 and slice 3 refactor passes); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 1 refactor report ("`BookReadingBookLayout.vue` is in the diff and was 354 lines, over the 250-line limit"; new `useBookLayoutBlockPointerDrag.ts`, commit d4a47402af); slice 3 refactor report ("`BookReadingContent.vue` is 453 lines … It was 474 before this change … I left it for the owner to decide"). Pre-change size: `1e19f8224a:frontend/src/components/book-reading/BookReadingBookLayout.vue` has 354 lines.
  - Observed effect: about 15 minutes of refactor time in slice 1, plus a desktop re-proof (`reorganize_layout.feature`, `book_browsing.feature`) for code the story did not touch. Slice 3 took the other path, with no extraction.
  - Inference: `refactor-checks.md` "File size" does not say whether a file that was already over the limit, and that a slice barely touches, must be split. Agents resolve this differently, and the time cost follows whichever reading they pick. Related to DD-144 (a small addition tipping a file over the limit) and ODF-152; here the file was over the limit before the change.

## DD-162 — A grep-based plan premise named tests that never reached the changed path

The plan listed five tests to switch to real PDFs because they held fake `%PDF` bytes. Two of them store books through `makeMe`, not attach, so the new check never ran for them. Slice 1 changed them anyway, and its refactor pass reverted both.

### Occurrences

- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 1); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: plan *Decisive premises* row "Backend tests attach fake PDF bytes" (`grep -rn "0x25, 0x50"`); slice 1 refactor report item 2 (`BooksControllerTest`, `NotebookGitWebAttachmentDeleteControllerTest` back to HEAD).
  - Observed effect: two files changed and reverted within one slice; small cost.
  - Inference: the premise matched a symptom (fake bytes) rather than the path (attach callers). Related to the planning-premise family (DD-128, DD-137).

## DD-163 — A probe on a copy of the viewer missed how landing affects the current block, so slice 2 failed to converge twice

The owner-requested probe measured landing in headless Chrome on a standalone page that copied `EpubBookViewer`. It found the landing cause but did not observe the current-block reporting that depends on landing, or overlapping epub.js displays. The plan split landing (slice 2) from the current-block rule (slice 3) with interim `@ignore`s on that assumption.

### Occurrences

- Execution: SEED-059#story-1 / `e733844d01:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md` / 485ed2eb49; Timestamp: unknown (2026-09-29, before the slice 2 CI run at 04:45Z); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 2 attempt 1 (about 6 min) broke "Resume EPUB reading at the last position"; the consolidated attempt (about 25 min, 201k subagent tokens) stopped at 11 of 13; refinements b1746859a4 and 179187e58b; the third attempt converged in about 5 min from the parked patch.
  - Observed effect: two non-converged attempts and two plan refinements before slice 2 was delivered.
  - Inference: a probe through the real app, or one that also recorded the `relocated` events after landing, would likely have shown the coupling. Parking each attempt as a patch in the plan folder kept its work and let the next attempt start from it. Qualified: one execution.

## DD-164 — An implementer reshaped a test fixture until the new scenario passed, and reported that no product change was needed

For "opening a new EPUB marks nothing", the implementer's first cover (a separate spine file, the Gutenberg shape) had no layout block and the scenario failed. It then moved the cover inside a file the table of contents targets, which the extractor already handled, and returned "no product change was needed".

### Occurrences

- Execution: SEED-059#story-1 / `e733844d01:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md` / 485ed2eb49; Timestamp: unknown (2026-09-29, slice 4); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 4 hand-back listing the moved cover and the gap "a cover in a spine file that no entry targets gets no block"; the coordinator's check of the real Alice EPUB (`wrap0000.xhtml` first in the spine, not targeted); the corrected slice with an extractor change in b2faf56d14.
  - Observed effect: one extra implementation round; without the check, the story's key example would have been reported as met while failing on the real book.
  - Inference: fixture changes that turn a failing scenario green should be checked against the story's real example, not only against the scenario. The implementer did name the gap, which made the check possible.

## DD-166 — Plan edits by text replacement silently did nothing, and five slices' learnings never reached the plan

The coordinator appended learnings with a script that replaced an anchor copied from its own earlier edit. The file's line wrapping differed, so the replacement matched nothing, and each later append used the previous one as its anchor. Only the slice 1 learning stayed in the plan.

### Occurrences

- Execution: SEED-059#story-1 / `e733844d01:.planning/slice-plans/049-epub-land-and-track-chosen-place/PLAN.md` / 485ed2eb49; Timestamp: unknown (2026-09-29, from the first slice 2 refinement onward); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `git show <commit>:…/PLAN.md | grep -c '^- \*\*'` was 3 at every commit b1746859a4..26c34d6649; the retrospective's outcome review found the missing learnings; they were restored from the conversation in the completion commit.
  - Observed effect: during execution the plan did not carry the slice 2 attempts, the CI repair, or the slice 3–6 causes, so a resumed execution would have lost them. No product effect.
  - Inference: asserting that each anchor is present before replacing would have stopped the first failed edit.

## DD-167 — A correction to merge two lookups was planned without checking that they choose the same result

The retrospective that created SEED-059#story-14 saw two EPUB rendered-view lookups and planned to merge them. Nobody compared their matching rules before planning. At execution, the first slice 2 attempt found that they choose different sections when one stored path matches two spine items. Execution had to stop for an owner decision. The plan's "stop and report instead of choosing one" guard worked as intended.

### Occurrences

- Execution: SEED-059#story-14 / `.planning/slice-plans/055-epub-resume-tests-and-rendered-view/PLAN.md` / 09ca632dea; Timestamp: 2026-09-29 (slice 2 first attempt; exact time unknown); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 2 Decision paragraph in plan 055 at 0e9048812c; `epubSpinePathMatches` suffix rule in `frontend/src/lib/book-reading/epubHrefMatch.ts`.
  - Observed effect: one implementation-agent round of about 57k tokens returned no change, and the owner was asked one question. The recommended option was accepted and no work was lost.
  - Inference: reading the two matching rules while planning the correction (a few minutes) would have found the difference and taken the decision to the owner before the plan. Qualified: one occurrence.

## Retention

- Highest allocated local number: 167. Removed local codes are never reused.
- Full pre-maintenance log and earlier recovery locators: `99fa1b9835e3dff2473837ba2a1f8b11967d5938:DearDough.md`.
- Occurrence history is partial; active evidence stays here or in the Open Dough catalog and watch list.
