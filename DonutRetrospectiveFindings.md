# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved findings are removed; Git history keeps their evidence.

A finding is Donut’s when its correction lands in Donut’s product, scripts, or
Donut-authored skills and `.agents/agent-map.md`; Donut details that only
illustrate a shared lesson stay in DearDough as occurrence evidence.

## Open findings

### Queued: The owner's removal rule does not reach delegated agents

Story: [Removals leave no trace without the owner restating the rule](.planning/seeds/SEED-068-owner-rules-reach-every-agent.md#removal-rule-in-guidance) — SEED-068#removal-rule-in-guidance.

Related: DD-142, now part of shared ODF-187 in DearDough, where a plan also
prescribed an absence assertion for removed UI against the same rule.

#### DD-202 — A plan asked for a "no longer changes titles" docs note, against the owner's no-trace rule for removals

The plan for a feature removal told the implementer to turn a docs section into a short note that the removed behavior no longer happens. The owner's standing rule is that a removal leaves no negation or historical note in docs. The implementer, the refactor agent and the coordinator all accepted the note; only the retrospective caught it, and it needed a correction story.

##### Occurrences

- Execution: SEED-066#author-controlled-titles / slice-plans/002-keep-titles-under-author-control / aa093fffeb
  - Timestamp: unknown (slice 1 accepted on 2026-10-03, about 14:53 +08:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.54
  - Evidence: `0df1788796:.planning/slice-plans/002-keep-titles-under-author-control/PLAN.md` slice 1 ("becomes a short note that dictation no longer changes titles"); `aa093fffeb:docs/voice-input.md` section "Dictation does not change titles" ("The observations above that mention title requests predate this"), plus five other title-service mentions left in the same file; owner memory `doughnut-removal-leaves-no-trace`; correction `SEED-066#voice-docs-title-trace` (`ac59c040e5`).
  - Observed effect: a docs-only correction story and plan were needed after delivery.
  - Inference: the rule lives only in the coordinator's memory, which delegated planners, implementers and refactor agents do not see, and the plan's explicit wording outranked it. Removal plans may need a "clean the whole product scope, including docs" check written into the plan.

### Queued: Local app stacks broken by backend build output

Story: [Start and keep local app stacks on current backend code](.planning/seeds/SEED-067-dependable-local-app-stacks.md#stacks-survive-other-builds) — SEED-067#stacks-survive-other-builds.

Both findings are a running stack reading `backend/build` output that something
else left or rewrote. Resolved DD-103 was the same shared output with the E2E
runner's own compiler as the writer; its correction still holds.

#### DD-159 — The Development stack failed to start on stale compiled backend classes in the default checkout

`pnpm dev` from the default checkout failed with `No qualifying bean of type NotebookGitCutoverService`: `backend/build/classes` still held classes from before that service was removed. Deleting `backend/build/classes` let the next start succeed.

##### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T07:25:42+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `dev.log` "APPLICATION FAILED TO START" with the missing-bean message; `git grep NotebookGitCutoverService -- backend/src` found nothing; the next `pnpm dev` was healthy.
  - Observed effect: one failed start and a short diagnosis before the UAT setup could continue.
  - Inference: the Development start's incremental build did not drop classes whose sources were deleted. Qualified: cause not investigated further.

#### DD-200 — Concurrent backend verification disrupted an E2E hot-reloading runtime

Backend Gradle resource processing and the E2E application shared one checkout's build output. A resource refresh provoked an application restart while the browser journey was beginning.

##### Occurrences

- Execution: SEED-066#preserve-existing-content / `979cac31fc19f756bdfc480d9b24f1ab7dfeec34:.planning/slice-plans/001-preserve-existing-content/PLAN.md` / 64173ad25fbbe7457705aeea972a959d9d3f8dd4
  - Timestamp: unknown (2026-10-03, slice 2 verification; runtime log shows 13:26:20)
  - Tool: Codex
  - Open Dough release: 0.3.54 (unchanged execution-checkout VERSION)
  - Evidence: preserve_body proof handoff; `sut.log:522–564` reports hot-restart Flyway missing migration resources and LB connection refusal; backend `processResources` was concurrent. The E2E attempt failed with Bad Gateway before setup. Serial retry after Gradle terminated passed.
  - Observed effect: one failed setup and one extra integrated run; no failed product assertion was dismissed.
  - Inference: serialize Gradle resource writes and hot-reloading E2E in the same checkout, or provide genuinely separate build output. Database and port isolation alone did not prevent this overlap. Qualified: one evidenced occurrence.

### Open, not queued: Observing branch code in a held app stack

Not queued: one occurrence whose cost was mostly one-time environment repair (see Priority). Related evidence not yet on main, recorded there under shared ODF-190: `origin/claude/keep-completed-speech-as-dictation-continues`
at `cb4b71c8f4`, DearDough.md ODF-190 row — linked worktrees refuse the
Development stack, so a real-service proof of branch code waited about
30 minutes for the owner and detached the primary checkout onto branch code.

#### DD-161 — Real-book manual acceptance had no supported way to hold a disposable stack or run MinerU

The MinerU version part is resolved: every install hint now names `pip install 'mineru[pipeline]==3.4.5' six` on Python 3.10–3.13. Still open: no repo command keeps a disposable E2E stack up for manual use, and a `.venv-mineru` whose Python lived in a garbage-collected Nix store path must be rebuilt by hand.

The plan's manual slice needed real MinerU and a running app to `/attach` real PDFs through the CLI. `.venv-mineru`'s Python pointed into a garbage-collected Nix store path, the unpinned `pip install 'mineru[pipeline]'` in the repo's docstrings installs MinerU 4.x (no `pipeline` extra, no `mineru.cli.common`), and no repo command keeps a disposable E2E stack up without Cypress, so the agent wrote a temporary `hold-stack.mjs` around `runE2eInteractive`.

##### Occurrences

- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 6, after 6f36cb2952); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 6 agent report (1,692 s, about 197k tokens, 96 tool uses); plan premise "`.venv-mineru/bin` has no `python`"; venv rebuilt with Homebrew Python 3.12, `mineru[pipeline]==3.4.5` and `six`; `cli/python/mineru_book_outline.py` and `regenerate_mineru_output_for_refactoring.sh` still say unpinned.
  - Observed effect: about 28 minutes for one manual slice, most of it environment repair and stack scaffolding rather than observation.
  - Inference: much of the cost was necessary once; a pinned MinerU install and a documented "hold a disposable stack" command would make the next real-book acceptance cheaper. Donut tooling, so correction belongs to Donut, not shared guidance.

## Review — 2026-10-03

Reviewed `DearDough.md` at `857425b04a`, then again after
`origin/claude/keep-note-titles-under-the-author-s-control` landed (`afc9f1163d`).

### Moved from DearDough

- DD-202 (open; above). The removal rule is the owner’s Donut convention; it is
  in no Donut guidance (`AGENTS.md`, `CLAUDE.md`, `.agents/`), and shared
  Open Dough guidance keeps absence assertions when absence is the promise. So
  the correction lands in Donut.
- DD-200 (open; above). Donut’s scripts own the shared `backend/build` output
  and its documented concurrency (`docs/worktree-backend-tests.md` covers only
  database isolation), so the correction lands in Donut.
- DD-177 — the plan put API regeneration in a later slice than the controller
  change that `RobotsTests.openApiDocsMatchCommittedYaml` requires
  (SEED-062#story-1, plan 006). Donut’s generation trigger, so Donut’s finding.
  Resolved by `c3a7e2f826`, which says in the `generate-api-client` skill to
  regenerate in the same change as the controller edit; no later occurrence.
  Removed here; Git history keeps its evidence.

### Kept in DearDough

All other entries’ corrections land in shared guidance, scripts or the host.
The Donut facts in them behaved as documented:

- ODF-100: `frontend/tsconfig.json` includes `tests/**`, so the `frontend`
  skill’s plain `vue-tsc --noEmit` covers test files; the agents piped it
  into `tail`.
- ODF-150, ODF-190, ODF-074: Donut specs, viewports and the agent map were the
  facts that planning or proof selection did not check. ODF-150’s phone
  viewport failure was a fragile assertion, repaired in `5206b08370`.
- ODF-187: its unfailable-case half is shared; its absence-check half is
  related evidence for DD-202 above.
- ODF-152, ODF-189, ODF-195: the file-size rule is in the shared
  `dough-post-change-refactor` references.
- DD-176: the shared `dough-execute-plan` wrap-up requires a fresh refactor
  agent per slice; Donut’s `CLAUDE.md` only mirrors it.
- DD-174, DD-175, DD-201 and the CI observer entries: shared delivery and
  observation scripts. DD-178, DD-199: shared refactor delegation.

### Pending on an unlanded branch

`origin/claude/keep-completed-speech-as-dictation-continues` (`cb4b71c8f4`)
also allocates DD-202, for a different finding (a CI observer worker exit);
renumber it when that branch lands. Its stale `dev.pid` restart failure is
Donut tooling; record it here if it recurs.

### Priority

A finding group is queued only for high impact or high frequency, with impact
ranked first.

1. SEED-068#removal-rule-in-guidance: highest impact. A wrong docs change
   reached main and needed its own correction story and plan (DD-202); the
   same rule was broken in planning before (DD-142). Two occurrences.
2. SEED-067#stacks-survive-other-builds: highest frequency. Two open
   occurrences in five days across Claude Code and Codex (DD-159, DD-200),
   following resolved DD-103 on the same build output; each cost a failed
   start or run and a diagnosis, so impact is low.

Not queued: DD-161. Its remaining part is one occurrence; most of its
28 minutes was rebuilding the MinerU environment, whose version part is
fixed. The owner already declined to queue that remainder on 2026-09-29. The
related 30-minute owner wait on the unlanded branch was recorded as shared
planning feedback (ODF-190), not as Donut tooling.

### Resolved findings

Recheck these when a new occurrence arrives; none is contradicted today:

- DD-073 — frontend proof typechecks (`7b1d80b4e8`).
- DD-103 — E2E runner backend race (`d86864023c`); DD-200 is a different writer.
- DD-121 — commit hook and other slices’ unstaged files (`574d61b52c`,
  `c76f69b62e`, `120753a097`).
- DD-165 — script tests coupled to a live spec name (`7580fceccc`).
- DD-177 — API regeneration in the controller’s slice (`c3a7e2f826`).

### Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
