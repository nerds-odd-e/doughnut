# Preserve task-owned edits with shorter instructions

## Source, goal, and status

Source: [SEED-012 Story 4](../../seeds/SEED-012-priority-execution-process-improvements.md#story-4).
Status: slice 1 done; slice 2 planned.

Keep planning edits and commits attributable to their task while making the
changed skill documents shorter. Deliver the staging guard first; it prevents
an observed commit mistake independently of shared-review ownership.

## Scope and size target

- Slice 1 owns `.agents/skills/execute-plan/references/wrap-up.md`; slice 2 owns
  `.agents/skills/execution-retrospective/SKILL.md`. Preserve unrelated work.
- **Each document must finish with fewer words than its execution-start version.**
  Capture `wc -w` before the first edit and after completion; retain those two
  counts in the normal proof handoff. Current planning-time counts are 558 and
  1,794 words respectively; execution must capture its actual starting versions.
- Replace and consolidate existing wording rather than append another checklist.
  Concision edits may cover the named document, preserving every existing
  obligation except the selected ownership changes. Prefer meaningful removal of
  repetition over cosmetic shortening. Do not move text to another document,
  compress it into cryptic shorthand, or remove necessary guidance to meet size.
- Use existing review to check preserved obligations and readable instructions;
  do not add an audit agent or review stage. A passing replay alone cannot close
  a slice whose document grew or lost an existing obligation.
- No registry, hook, index tool, lock, worktree automation, task discovery,
  history rewrite, Pygardon rollout, sibling P1 changes, or wider skill cleanup.
  All-owned commits, disjoint writers, and single reviews retain the simple path.

## Ordered slices

### 1. Commit only attributable task-owned changes

Type: Behavior
Status: done
Proof: Staging replays below plus a shorter `wrap-up.md` with its other delivery
obligations preserved. `wc -w .agents/skills/execute-plan/references/wrap-up.md`:
558 → 557. Four instruction replays match the required decisions. Mixed tree
(`SEED-012`, `quick/015`) left unstaged.

Behavior: Coordinator prepares a commit in a potentially mixed worktree/index
→ reviews ownership and the complete staged diff → commits only attributable
owned content, resolving ambiguity or a mixed index before commit.

Rewrite the existing commit-preparation passage. Stage owned files or separable
owned hunks, inspect final staged content, and allow whole-change staging only
when all content is owned. Do not silently include, unstage, reset, or revert
another task's work. Resolve unclear ownership or unrelated staged content with
its owner before committing. Ordinary unrelated unstaged files need no approval.
Preserve the check-only hook, independent refactor, and other delivery rules;
remove repetition within this document to meet the size target.

| Supplied ownership snapshot | Required decision |
|---|---|
| Owned product edit A; another task's unstaged skill edit B | Stage/review A only, preserve B, and proceed without extra approval. |
| One file with separable owned and unrelated unstaged hunks | Stage/review only the attributable owned hunk; preserve the other. |
| Unrelated content already staged, or hunk ownership ambiguous | Resolve the commit boundary first; neither co-commit nor silently alter another task's index/worktree. |
| All changed/staged content confirmed owned | Whole-change staging remains allowed. |

One edit/replay loop, about five minutes, medium confidence; independently useful
if delivery stops here. Tighten wording in the same slice, not a later cleanup.

### 2. Give each shared retrospective artifact one writer

Type: Behavior
Status: planned
Proof: Ownership replays below plus a shorter `execution-retrospective/SKILL.md`
with its existing review and authorization obligations preserved.

Behavior: Coordinated authorized reviews overlap on a seed/PLAN → assign one
writer/reconciler before writes → other reviewers return read-only evidence for
that artifact, and the writer incorporates it against current content once.

Rewrite the existing ownership/context wording; name exact shared targets and
one writer through the task assignment, without another ownership record.
Consolidate repetition within this skill to meet the size target. Preserve its
write-permission boundaries and prohibition on executing findings; this change
does not authorize additional reviewers or artifact edits.

| Supplied assignment | Required decision |
|---|---|
| Two authorized reviews share a PLAN | One writer/reconciler incorporates both sets of findings against current content; the other reviewer is read-only there. |
| Disjoint PLANs or one reviewer | Continue the existing path without another reconciliation step or agent. |

One edit/replay loop, about five minutes, medium confidence. No Structure slice
or prerequisite work is needed.

## Coverage and completion

| Promise | Owning evidence |
|---|---|
| Scoped commit; preserve other owners' content and resolve ambiguity | Slice 1 staging cases |
| One writer for overlapping artifacts; ordinary paths stay simple | Slice 2 ownership cases |
| Each changed document becomes shorter without losing existing obligations | Each slice: before/after word counts and existing review of its full diff |
| No shifted documentation, new tooling, wider permissions, or sibling changes | Each slice: scoped diff inspection |

Use supplied scenarios for read-only instruction replays; no real Git mutation,
concurrent workers, product suites, or replay harness. Report the decisions as
instruction evidence, not executed product tests. Record size using
`wc -w <owned-document>`; preserve the exact command and counts in the normal
handoff. Keep compact results in this PLAN while active.

Follow ordinary `execute-plan` wrap-up for each slice. On completion, retain a
short installation/replay and size result in the home story, preserve its anchor
and siblings, and remove this spent PLAN. Observe live effectiveness in the next
natural relevant case; no forced exercise, monitor, or blocking follow-up slice.

## Readiness

Ready for direct execution: two bounded leaves with one instruction owner and
one decision loop each; modest deduplication accompanies the behavior edit.
Scrutinize after five minutes; after ten without a stated valid reason, preserve
owned work and refine this PLAN. Do not waive the size or preservation target to
fit the timebox. Sizing is a judgment, not a guarantee.
