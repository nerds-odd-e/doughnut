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

During an 8-slice `execute-plan` run (`.planning/quick/002-open-existing-notebook-locally/PLAN.md`, slices 1-8), each slice's coordinator-owned wrap-up unconditionally spawned two fresh Task agents in addition to the implementer: `post-change-refactor` and `format-changed`.

| Step | Total tokens (8 runs) | Share of subagent spend | Runs that found/changed anything |
|---|---|---|---|
| Implementer | ~707.5k | 43% | 8/8 (expected — this is the actual work) |
| `post-change-refactor` | ~592.6k | 36% | 4/8 (3 real cross-file duplication fixes, 1 comment-only) |
| `format-changed` | ~333.9k | 20% | 1/8 (2 files auto-fixed by biome) |

Total: ~1.63M subagent tokens across 24 fresh-agent calls for 8 slices. Five conclusions this evidence supported (all now implemented — see below):

1. **`format-changed` was the clearest waste** — a ~42k fixed cost per run (fresh-agent instantiation/skill-loading, not variable work) for a 1/8 hit rate, when the pre-commit hook's own check-only lint already caught every issue independently.
2. **`post-change-refactor` earned its cost and should not be cut** — 3/8 slices had genuine cross-file duplication the implementer had just reintroduced despite being told to reuse existing logic (e.g. `NotebookExportRows`, `GitBundleTestReader`, `loadAuthenticatedFetchContext`), each a small, individually clean-looking new file duplicating logic located elsewhere — catchable only by a full read of related files, which is already refactor's first move. A cheaper size/file-count triage would not have helped.
3. **The skill's own "skip tests on zero edits" rule wasn't reliably followed** by every fresh refactor-agent instantiation (3/8 ran full tests anyway) — cheap to fix by restating the rule in the coordinator's delegation prompt rather than trusting the agent to find it in the skill file.
4. **Coordinator-side research was sometimes done inline instead of forked out** — e.g. the slice-5 investigation into a Flyway-vs-JPA bootstrap-ordering hazard, whose raw trace then sat in coordinator context for the rest of the session even though only its distilled conclusion was ever reused. The research itself was valuable (it prevented a costly failed-attempt cycle); only its *location* (inline vs. a disposable `fork`) was suboptimal.
5. **Minor implementer over-verification** — at least one implementer ran a full suite as unrequested extra sanity beyond the slice's own focused proof, which `execute-plan`'s out-of-scope list already discourages.

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
- `execution-retrospective` on the same plan's completed 15-slice run (2026-09-05) — source of the Further Process Findings above; also produced follow-up correction `.planning/quick/004-notebook-clone-checkout-cleanup/PLAN.md` (repository bugs, tracked separately from this process seed).

## Notes

Sample size was small (n=8 slices, one plan, one session) — treat the original percentages as directional. Pygardon's independent seven-slice retrospective (`../../../pygardon/.planning/seeds/SEED-008-efficient-plan-execution-evidence-handoffs.md`) reached the same `post-change-refactor`-stays-mandatory conclusion and confirmed the `format-changed` fix should be a proactive direct command (not reactive-on-hook-failure), since `scripts/format_changed.sh` is a plain deterministic shell delegate with no LLM call in it — the measured cost was fresh-agent overhead, not command runtime. Pygardon SEED-008 stories 1–4 and 6 were folded into this same outcome as one story (story 5 stayed unselected).

**Implementation outcome (2026-09-04):** A five-slice execution updated the routine wrap-up to use one coordinator-run selective formatter command, added compact implementer proof handoffs and default reuse, made refactorers consume that proof and skip tests after a no-edit judgment, isolated disposable investigations from coordinator context, and added a destructive-work check against named later outcomes. `post-change-refactor` remained mandatory (its ordered checks were extracted to a cohesive reference once the edited skill crossed 250 lines); `format-changed` remains available on demand.

Dogfood evidence: the Doughnut execution recorded five independent refactor judgments, three no-edit passes that correctly skipped tests, two file-size refactors that reran only the invalidated focused proof, and five passing check-only commit hooks with one routine direct formatter command per later slice. A follow-up Pygardon dogfood completed six atomic commits/pushes with six passing hooks and no unnecessary proof reruns, and surfaced one more contract gap (an implementer abbreviating an exact command with a placeholder) that was fixed by treating a placeholder as missing/ambiguous proof while still permitting recovery of the original command before any rerun.

## Further Process Findings (2026-09-05)

Source: `execution-retrospective` on the completed SEED-009 Story 1 plan (`.planning/quick/002-open-existing-notebook-locally/PLAN.md`, its final slices 13-15 plus an aggregate review of the full 15-slice execution). Neither finding changed a rule yet — both are candidates for the next revision of `execute-plan`'s references.

1. **Delegation-prompt overconfidence on an unverified claim.** A Slice 15 delegation prompt asserted a suspected code gap (`downloadNotebookGitBundle`'s plain-object `throw`) was "a real gap I already confirmed by reading the code," without the coordinator having actually traced the full call chain (`withBackendClient`'s catch/rewrap logic in `donutBackendClient.ts`). The implementer independently verified the claim, found it was *not* a bug, and avoided a regression the coordinator had nearly prescribed — but a less careful implementer could have trusted the "confirmed" framing at face value. Lesson: a coordinator should phrase a suspected-but-untraced gap as "verify this" rather than "confirmed" whenever it hasn't followed the call chain to its actual handler.
2. **A PLAN's "Current decisions" claims aren't automatically threaded into any slice's proof obligation.** The retrospective found a real bug (a dangling `origin` Git remote left in every successful `notebook clone` checkout) that directly contradicted a decision recorded up front in the original plan ("The CLI removes the temporary bundle origin") — yet no slice's proof ever asserted anything about `git remote` state; every Git-state check across all 15 slices covered branch/commit-count/tree-contents only. Lesson: when a "Current decisions" entry makes a concrete, checkable claim about final state, `slice-planning`/`slice-plan-refinement` should make sure at least one slice's proof actually checks it, not just the higher-level behavior it's bundled with.
