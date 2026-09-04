---
id: SEED-010
status: dormant
planted: 2026-09-04
planted_during: execute-plan run of SEED-009's notebook-clone quick plan (slices 1-8), on request for a process retrospective
trigger_when: when next revising execute-plan's wrap-up.md/delegation.md, the post-change-refactor or format-changed skills, or before running another long multi-slice execute-plan session
scope: medium
---

# SEED-010: `execute-plan` wrap-up spends tokens on steps that usually find nothing

## Why This Matters

During an 8-slice `execute-plan` run (`.planning/quick/002-open-existing-notebook-locally/PLAN.md`, slices 1-8), each slice's coordinator-owned wrap-up unconditionally spawned two fresh Task agents in addition to the implementer: `post-change-refactor` and `format-changed`. Tallying the token/outcome numbers each agent reported back across all 8 slices:

| Step | Total tokens (8 runs) | Share of subagent spend | Runs that found/changed anything |
|---|---|---|---|
| Implementer | ~707.5k | 43% | 8/8 (expected — this is the actual work) |
| `post-change-refactor` | ~592.6k | 36% | 4/8 (3 real cross-file duplication fixes, 1 comment-only) |
| `format-changed` | ~333.9k | 20% | 1/8 (2 files auto-fixed by biome) |

Total: ~1.63M subagent tokens across 24 fresh-agent calls for 8 slices.

**`format-changed` is the clearest waste.** It cost ~42k tokens every single time (a roughly fixed cost — mostly skill-loading and environment setup, not variable work) and found nothing to fix in 7 of 8 runs. Every commit in the session also ran the pre-commit hook's own check-only `scripts/lint_changed.sh` (spotlessCheck/lint), which passed every time — meaning the safety net already existed independent of this proactive step. A reactive design (attempt the commit; only spawn a formatter pass if the hook fails) would keep the identical safety guarantee while likely cutting this cost by ~87%.

**`post-change-refactor` earns its cost and should not be cut.** In 3 of 8 slices it caught genuine cross-file duplication that the *implementer* had introduced despite being explicitly told in the delegation prompt to reuse existing logic instead of replicating it (e.g. slice 4's implementer was told to reuse `NotebookExportService`'s folder/note-fetch pattern "or replicate," chose to replicate, and refactor extracted `NotebookExportRows`; similarly `GitBundleTestReader` in slice 6 and `loadAuthenticatedFetchContext` in slice 8). A cheap size/file-count triage before running it likely would not have helped — all three catches were small, individually clean-looking new files that duplicated logic located *elsewhere* in the codebase, which only a full read of related existing files can catch. That's already refactor's own first move, so "triage before refactoring" mostly collapses into "just run refactor."

**Inconsistent adherence to the skill's own "run tests only when the pass edits something" rule.** Slice 3's refactor agent correctly skipped tests when it made no edits. Slices 1, 2, and 7's refactor agents ran full test commands anyway despite reporting zero edits — the same written instruction, in the same skill file, was not reliably followed by different fresh-agent instantiations. Low individual cost, but free to fix by restating the rule explicitly in the coordinator's delegation prompt rather than relying solely on the agent reading it carefully in the skill file.

**Coordinator-side (non-subagent) research was sometimes done inline instead of forked out.** For riskier slices — especially slice 5's investigation into whether the fleet-cutover Flyway migration could safely depend on JPA repositories (it couldn't, due to a Flyway-vs-`EntityManagerFactory` bootstrap-ordering hazard in this app's test profile) — the coordinator read a lot of raw file content and ran many greps directly in its own persistent context. That raw output then stays in the coordinator's context for the rest of the session (all later slices), the most expensive place to keep it, even though only the distilled conclusion (a few paragraphs folded into that one delegation prompt) was ever needed again. A `fork` subagent (shares the coordinator's prompt cache, discards its own tool-call noise) was never used once in this 8-slice run, despite being exactly suited to "answer one question, throw away the trace" research. This is worth contrasting against the fact that this same upfront research plausibly *prevented* a costly failed-attempt-and-revert cycle in slice 5 — the investigation itself wasn't waste, only where it was performed (inline vs. forked) was suboptimal.

**Minor implementer over-verification.** At least one implementer (slice 4) ran a full `pnpm backend:test_only` suite as unrequested extra sanity beyond the focused tests actually asked for, which `execute-plan`'s own out-of-scope list discourages ("Do not run full CI before commit"). Low frequency in this sample, but worth an explicit "don't run the full suite unless the slice's own proof specifically requires it" line in delegation prompts (slice 5's `pnpm backend:verify` was correctly required by its own proof text and is not an example of this).

## When to Surface

Next time `.claude/skills/execute-plan/references/wrap-up.md` or `delegation.md` is being revised, or before kicking off another long (5+ slice) `execute-plan` run where this cost would compound again.

## Scope Estimate

**Medium.** No product code changes — this is entirely `.claude/skills/execute-plan/` (and possibly `.agents/skills/format-changed/SKILL.md`, `.agents/skills/post-change-refactor/SKILL.md`) rule/skill editing plus validating the new flow doesn't lose the safety properties it currently has (pre-commit hook still gates every commit either way). Candidate changes, roughly ordered by confidence:

1. Make `format-changed` reactive: attempt the coordinator's commit directly; only spawn a formatter-fixing step when the pre-commit hook actually fails, and consider having the coordinator run the mechanical formatter command itself (or via a `fork`) rather than a fresh full agent, reserving a fresh agent for the (so-far unobserved in 8 samples) case where the formatter's own output surfaces a real semantic/lint judgment call.
2. Keep `post-change-refactor` mandatory per slice; add an explicit "skip tests entirely if you make zero edits" line to the coordinator's delegation prompt for it, since the skill file's own version of this rule wasn't reliably followed.
3. Encourage the coordinator to `fork` (rather than inline-read) for one-off deep-dive research whose raw output won't be needed again after being distilled into a delegation prompt, to keep the coordinator's own context lean across long multi-slice runs.
4. Add a "do not run the full suite unless the slice's proof requires it" reminder to delegation prompts.

## Breadcrumbs

- `.claude/skills/execute-plan/references/wrap-up.md` — coordinator-owned wrap-up sequence (currently: fresh refactor agent → fresh format agent → plan update → commit → push, unconditionally every slice).
- `.claude/skills/execute-plan/references/delegation.md` — implementer delegation contract.
- `.agents/skills/format-changed/SKILL.md` — the step with the lowest hit rate (1/8) relative to cost.
- `.agents/skills/post-change-refactor/SKILL.md` — contains the "run related tests only when the refactor edits" rule that wasn't reliably followed by fresh agents in this sample.
- Session transcript executing `.planning/quick/002-open-existing-notebook-locally/PLAN.md` slices 1-8 (2026-09-04) — source of all the token/outcome numbers above.

## Notes

Sample size is small (n=8 slices, one plan, one session) — treat the specific percentages as directional, not definitive, especially for `post-change-refactor`'s hit rate. Re-check against a second multi-slice run before committing to specific numeric thresholds in any rule change. The user's own hypothesis going in ("maybe rely on the commit hook and only reformat on warning," "maybe triage before refactoring") is what prompted this data pull; the data confirms the first hypothesis fairly strongly and complicates the second (triage-before-refactor mostly re-derives what refactor's own first step already does).
