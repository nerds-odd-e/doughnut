# Review and close the local AI notebook effort

## Source

- Story: [SEED-048#story-1](../../seeds/SEED-048-local-ai-notebook-final-review.md#story-1)
- **Identity:** SEED-048#story-1
- Start condition met: SEED-046#story-9 closed in `a37ef2c97a`.

## Goal and scope

One outcome: the owner has one private review report page for the effort
(architecture, Open Dough process feedback, feature coverage, seed cleanup
and stale seeds). Urgent and important follow-ups are queued as concise
stories, and related seeds are trimmed.

Review range: `23f081f05b..<main when slice 1 starts>`. Record that end
SHA under Current decisions when slice 1 starts. Every slice uses the same
range.

Excluded (story's deferred promises): fixing any finding, editing Open Dough
skills, changing stale seeds unrelated to the effort, and rewriting the
backlog's near-future direction.

## Existing solutions reused (PFE)

- **Review lenses:** the product and process lenses of
  `dough-execution-retrospective` (intent vs. aggregate commits, whole-product
  architecture, tests, agent history). Apply them by reading that skill. Do
  not invoke it: it reviews one execution and writes `DearDough.md` and
  correction plans, and this story's feedback belongs only in the report.
- **Architecture yardstick:** `.planning/NORTH-STAR.md` topics, Accepted ADRs
  in `docs/adrs/` (0001 vocabulary, 0002 Git sync, 0004 Markdown format,
  0006 failure handling), and `docs/notebook-git-*.md`.
- **Known process findings:** `DearDough.md` ODF entries. The report cites
  existing codes instead of rediscovering them, and adds only new evidence.
- **Agent history:** Claude Code transcripts under
  `~/.claude/projects/-Users-terryyin-git-doughnut*` (main checkout and
  worktree directories), filtered to the review range's dates (2026-09-20
  onward).
- **Queued architecture work:** SEED-049#story-1 (frontend note store) is
  already queued. The report references it and queues no duplicate.
- **Backlog mechanics:** `product-backlog.mjs add` / `record-state` / `complete`
  and the seed format in `dough-story-decomposition/references/seed-format.md`.
- **Report page:** the Artifact tool. Load `artifact-design` before writing
  the page.

## Report home

The report source is `report.html` in this plan directory. It is committed
with each slice so the work can resume, and each slice republishes it to the
same page URL (record the URL under Current decisions on first publish). Plan
wrap-up deletes it together with this plan. Every finding cites evidence
(commit SHA, `file:line`, ODF code, or transcript file and date) and carries
an urgency judgment: *queue now* or *not now*.

## Outside-in proof

| Promise | Owning slice and observation |
| --- | --- |
| Architecture section: structural impact and improvements that map to the domain model | 1: the published page shows the section; each improvement cites evidence and the North Star topic or ADR it serves |
| Process feedback section: Open Dough architectural process, using thread history | 2: the published page shows the section; each suggestion names the guidance (skill file, plan instruction, or North Star wording) and cites transcript or ODF evidence |
| Feature coverage section: behavior organized as a system, E2E and unit coverage assessed | 3: the published page shows a feature map whose rows name the owning doc section and the E2E feature file and unit spec, or *gap* |
| Urgent follow-ups queued, nothing fixed | 4: `product-backlog.mjs read-state --link <href>` reports each new story recorded (`not-refined`, `unselected`), and `.planning/PRODUCT-BACKLOG.md` lists it; `git diff --stat <slice-4 base>` touches only `.planning/` |
| Related seeds trimmed; emptied seeds deleted without trace | 5: `git diff --stat` shows only the removed stories and seed files; `grep -rn '<deleted seed id>' .planning docs` finds nothing; every backlog link resolves |
| Stale seeds listed for the owner, left untouched | 5: the report's list matches `for f in .planning/seeds/*.md; do git log -1 --format="%ad $f" --date=short -- "$f"; done` for dates more than 15 days before the review date; those files are unchanged in the diff |

## Ordered slices

### 1. Architecture section published

Type: Behavior
Status: done
Proof: first publish of the report page with the Architecture section (see table).

Behavior: the effort has landed → the review reads the range's aggregate
structural changes against the North Star topics and Accepted ADRs → the
owner can open a private report page whose Architecture section states the
structural impact and each improvement needed, with evidence and urgency.

Use parallel read-only subagents, one per North Star topic (one notebook tree,
one set of names per folder, one format boundary, one accepted-change boundary,
one attachment content model), plus one for frontend and CLI. The coordinator
merges their findings into one section. Also judge whether each North Star
topic is realized and could be retired at wrap-up; report this but do not
change `.planning/NORTH-STAR.md`.

### 2. Process feedback section published

Type: Behavior
Status: done
Proof: republished page shows the Process feedback section (see table).

Behavior: slice 1's page exists → the review reads how plans, the North Star,
and ADRs steered this effort (North Star history in the range, plan
instructions in the skills used, the effort's transcripts, related ODF
entries) → the report gains concrete, evidenced suggestions for improving the
Open Dough architectural process.

### 3. Feature coverage section published

Type: Behavior
Status: done
Proof: republished page shows the feature map (see table).

Behavior: slice 2's page exists → the review organizes the effort's
retained features as system behavior (from `docs/notebook-git-*.md` and
ADRs 0002/0004) and maps each one to E2E features (`e2e_test/features/`,
mainly `cli`, `notebooks`, `folder_organization`, `note_view`) and unit
specs → the report gains the feature map, high- and low-level coverage
judgments, and named gaps.

### 4. Urgent follow-ups queued

Type: Behavior
Status: done
Proof: recorder and backlog observations (see table).

Behavior: sections 1–3 hold *queue now* findings → each becomes one concise
story (Goal and Scope only) in a suitable seed, recorded `not-refined` and
`unselected`, and is added to the backlog list at a position ranked under
`dough-product-backlog`'s rules (owner priority first, then direction and
value) → the report links each queued story and states its position. Do not duplicate a queued story (SEED-049#story-1);
extend its seed only if a finding belongs to it.

### 5. Seed cleanup and stale list published

Type: Behavior
Status: done
Proof: diff, grep, and stale-list observations (see table).

Behavior: the review is complete → in seeds related to the effort (at
planning time: SEED-009, SEED-016, SEED-030, SEED-037; confirm the set, and
keep stories queued in slice 4) only concise, urgent, and important stories
remain, deferred ones are deleted, and emptied seeds are deleted with no
tombstone (none of these seeds had a backlog entry at planning time) → the
report gains its final section, listing what changed and the stale seeds for
the owner's keep-or-delete decision.

## Current decisions

- Review range end: `5a4bfe2f92` (main when slice 1 started; claim `bace70a8d7`).
  Range is `23f081f05b..5a4bfe2f92`.
- Report page: https://claude.ai/artifact/5zjpVg4o5yvTtRQNn3LWjB (republish
  `report.html` from this directory to keep the URL).
- Slice 1 *queue now* findings (for slice 4): (a) moving a folder to another
  notebook bypasses the one name owner (`FolderMoveRelocation.java:141-148`,
  `FolderSubtree.java:120`); (b) one Portable path-kind classifier for server
  and CLI (CLI root `.keep` drift, `notebookPublishLfsSelection.ts:8` vs
  `NotebookGitProposalTreeShape.java:143`). Both spot-checked by the coordinator.
- Slice 2 has no *queue now* finding: the lasting release-gate rule dropped in
  `002bad682f` has no consumer now that every legacy store is removed, so it is
  reported as a wrap-up practice suggestion only.
- Slice 3 has no *queue now* gap; cited E2E feature files and doc headings were
  checked to exist.
- Slice 4 queued SEED-009#story-47 (cross-notebook folder move names) and
  SEED-009#story-48 (one path classifier) after owner-first SEED-049#story-1.
  SEED-009 story numbers up to 46 were already used in history.
- Slice 5 confirmed the related set as SEED-009, SEED-016, SEED-017, SEED-030,
  SEED-037 (SEED-017 added: SEED-009's deferred sibling, contradicted by the
  delivered rebase). SEED-009 trimmed to stories 47–48; the other four
  deleted. Stale list: SEED-014 only. Remaining mentions of deleted IDs are this
  story's own seed and plan (removed at wrap-up) and a `DearDough.md` evidence
  line (execution record, left as is).

- Report findings only; no product code changes. The execution wrap-up's
  refactor and API steps have nothing to act on; formatting and lint run on
  the changed `.planning/` Markdown as usual.
- Slices 1–3 are reading-bound and may exceed the ~5-minute leaf target.
  Reason: each is one cohesive assessment whose value is the cross-cutting
  judgment. Splitting by file area would fragment it, so parallel read-only
  subagents give the speed instead.

## Learnings

## Execution complete

Product advice: SEED-009#story-47 and #story-48 are queued after owner-first
SEED-049#story-1. At wrap-up, add the listing-refresh finding (report,
Frontend and CLI) to SEED-049's seed, and let the owner decide the North Star
retirements the report proposes and the stale SEED-014.
