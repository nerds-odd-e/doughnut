---
id: SEED-010
status: resolved
planted: 2026-09-04
planted_during: execute-plan run of SEED-009's notebook-clone quick plan (slices 1-8), on request for a process retrospective
trigger_when: when next revising execute-plan's wrap-up.md/delegation.md, the post-change-refactor or format-changed skills, or before running another long multi-slice execute-plan session
scope: medium
selected: 2026-09-04
selected_as: one story covering this seed plus Pygardon SEED-008 stories 1–4 and 6
resolved: 2026-09-04
resolution: implemented as one five-slice execution-contract change; retained as the execution retrospective by developer request
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

**`format-changed` is the clearest waste.** It cost ~42k tokens every single time (a roughly fixed cost — mostly skill-loading and environment setup, not variable work) and found nothing to fix in 7 of 8 runs. Every commit in the session also ran the pre-commit hook's own check-only `scripts/lint_changed.sh` (spotlessCheck/lint), which passed every time — meaning the safety net already existed independent of the fresh formatting agent. The initial retrospective proposed a reactive formatter after hook failure; cross-project follow-up refined that into the implemented policy: run the cheap deterministic formatter command directly before committing and eliminate only the agent overhead.

**`post-change-refactor` earns its cost and should not be cut.** In 3 of 8 slices it caught genuine cross-file duplication that the *implementer* had introduced despite being explicitly told in the delegation prompt to reuse existing logic instead of replicating it (e.g. slice 4's implementer was told to reuse `NotebookExportService`'s folder/note-fetch pattern "or replicate," chose to replicate, and refactor extracted `NotebookExportRows`; similarly `GitBundleTestReader` in slice 6 and `loadAuthenticatedFetchContext` in slice 8). A cheap size/file-count triage before running it likely would not have helped — all three catches were small, individually clean-looking new files that duplicated logic located *elsewhere* in the codebase, which only a full read of related existing files can catch. That's already refactor's own first move, so "triage before refactoring" mostly collapses into "just run refactor."

**Inconsistent adherence to the skill's own "run tests only when the pass edits something" rule.** Slice 3's refactor agent correctly skipped tests when it made no edits. Slices 1, 2, and 7's refactor agents ran full test commands anyway despite reporting zero edits — the same written instruction, in the same skill file, was not reliably followed by different fresh-agent instantiations. Low individual cost, but free to fix by restating the rule explicitly in the coordinator's delegation prompt rather than relying solely on the agent reading it carefully in the skill file.

**Coordinator-side (non-subagent) research was sometimes done inline instead of forked out.** For riskier slices — especially slice 5's investigation into whether the fleet-cutover Flyway migration could safely depend on JPA repositories (it couldn't, due to a Flyway-vs-`EntityManagerFactory` bootstrap-ordering hazard in this app's test profile) — the coordinator read a lot of raw file content and ran many greps directly in its own persistent context. That raw output then stays in the coordinator's context for the rest of the session (all later slices), the most expensive place to keep it, even though only the distilled conclusion (a few paragraphs folded into that one delegation prompt) was ever needed again. A `fork` subagent (shares the coordinator's prompt cache, discards its own tool-call noise) was never used once in this 8-slice run, despite being exactly suited to "answer one question, throw away the trace" research. This is worth contrasting against the fact that this same upfront research plausibly *prevented* a costly failed-attempt-and-revert cycle in slice 5 — the investigation itself wasn't waste, only where it was performed (inline vs. forked) was suboptimal.

**Minor implementer over-verification.** At least one implementer (slice 4) ran a full `pnpm backend:test_only` suite as unrequested extra sanity beyond the focused tests actually asked for, which `execute-plan`'s own out-of-scope list discourages ("Do not run full CI before commit"). Low frequency in this sample, but worth an explicit "don't run the full suite unless the slice's own proof specifically requires it" line in delegation prompts (slice 5's `pnpm backend:verify` was correctly required by its own proof text and is not an example of this).

## When to Surface

Resolved on 2026-09-04. Retain this retrospective when evaluating future changes to `execute-plan` evidence handoffs or if a later long run suggests that formatter-agent overhead, redundant proof runs, or coordinator-context growth has regressed.

## Implemented Scope

**Medium.** No product code changed. The work stayed within execution skills, rules, and indexes while preserving the independent check-only pre-commit hook. Implemented changes, in delivery order:

1. Have the coordinator run `./scripts/run.sh pnpm format:changed` directly (no fresh formatting sub-agent) once after refactor/codegen and before the commit attempt, keeping the pre-commit hook's check-only `pnpm lint:changed` as the final independent check. Mechanical findings are repaired directly; semantic or design judgment is Jidoka. This supersedes an earlier reactive draft of this candidate (attempt commit first, format only on hook failure): cross-project comparison against Pygardon's independent SEED-008 retrospective, plus inspecting `scripts/format_changed.sh` (a plain deterministic delegate to `quality_changed.sh format`, no LLM call inside it), confirmed the ~42k token cost was fresh-agent instantiation/skill-loading overhead, not the command's own runtime — so running the command directly, proactively, captures nearly all the same savings without making a failed-commit-then-retry cycle part of the normal wrap-up path.
2. Keep `post-change-refactor` mandatory per slice; add an explicit "skip tests entirely if you make zero edits" line to the coordinator's delegation prompt for it, since the skill file's own version of this rule wasn't reliably followed.
3. Encourage the coordinator to `fork` (rather than inline-read) for one-off deep-dive research whose raw output won't be needed again after being distilled into a delegation prompt, to keep the coordinator's own context lean across long multi-slice runs.
4. Add a "do not run the full suite unless the slice's proof requires it" reminder to delegation prompts.

## Breadcrumbs

- `.claude/skills/execute-plan/references/wrap-up.md` — coordinator-owned wrap-up sequence (fresh refactor agent → one direct selective-format command → plan update → commit → push).
- `.claude/skills/execute-plan/references/delegation.md` — implementer delegation contract.
- `.agents/skills/format-changed/SKILL.md` — the step with the lowest hit rate (1/8) relative to cost.
- `.agents/skills/post-change-refactor/SKILL.md` — contains the "run related tests only when the refactor edits" rule that wasn't reliably followed by fresh agents in this sample.
- Session transcript executing `.planning/quick/002-open-existing-notebook-locally/PLAN.md` slices 1-8 (2026-09-04) — source of all the token/outcome numbers above.

## Notes

Sample size is small (n=8 slices, one plan, one session) — treat the specific percentages as directional, not definitive, especially for `post-change-refactor`'s hit rate. Re-check against a second multi-slice run before committing to specific numeric thresholds in any rule change. The user's own hypothesis going in ("maybe rely on the commit hook and only reformat on warning," "maybe triage before refactoring") is what prompted this data pull; the data confirms the first hypothesis fairly strongly and complicates the second (triage-before-refactor mostly re-derives what refactor's own first step already does).

**Cross-project resolution (2026-09-04):** Pygardon's independent seven-slice retrospective (`../../../pygardon/.planning/seeds/SEED-008-efficient-plan-execution-evidence-handoffs.md`) reached the same conclusion on `post-change-refactor` (keep it mandatory and unconditional; "bounded triage" is the refactorer's own concept-aware decide-first move, not a separate prefilter agent) but proposed a proactive rather than reactive formatting policy: run the deterministic formatter directly, once, before the commit attempt, rather than only after a hook failure. Checking `scripts/format_changed.sh` confirmed it is a plain deterministic shell delegate with no LLM call in it, which resolved the open question in Pygardon's favor — the measured ~42k cost was fresh-agent overhead, not command runtime. Candidate #1 above was revised accordingly.

**Selected as one story (2026-09-04):** Pygardon SEED-008 stories 1–4 and 6 are slices of this same outcome, not sibling stories. Pygardon story 5 stayed unselected.

**Implementation outcome (2026-09-04):** The five-slice execution updated the routine wrap-up to use one coordinator-run selective formatter command, added compact implementer proof handoffs and default reuse, made refactorers consume that proof and skip tests after a no-edit judgment, isolated disposable investigations from coordinator context, and added a destructive-work check against named later outcomes. `post-change-refactor` remained mandatory; its ordered checks were extracted to a cohesive reference when the edited skill crossed 250 lines. `format-changed` remains available on demand.

Dogfood evidence from the Doughnut execution: five independent refactor judgments; three no-edit passes that skipped tests; two file-size refactors that reran only the invalidated focused proof; one old-policy formatter agent on the installing slice and zero formatter agents on the next four slices; one routine direct formatter command on each later slice; and five passing check-only commit hooks. The run was explicitly squashed and not pushed, so it does not validate per-slice push behavior. Disposable-research and destructive-conflict behavior were installed, not exercised. The spent executable plan was removed after completion, while this seed was retained as the retrospective record by developer request.

Follow-up Pygardon dogfood completed six atomic commits and pushes with six passing hooks and no focused proof reruns. One implementer abbreviated an exact command with a placeholder; the coordinator recovered the literal command without rerunning it. The executable contract now treats placeholders as missing or ambiguous proof while permitting recovery of the original command before any rerun.
