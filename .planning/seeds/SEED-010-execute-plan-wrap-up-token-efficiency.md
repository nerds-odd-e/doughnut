---
id: SEED-010
status: dormant
planted: 2026-09-04
planted_during: execute-plan run of SEED-009's notebook-clone quick plan (slices 1-8), on request for a process retrospective
trigger_when: when next revising execute-plan's wrap-up.md/delegation.md, the post-change-refactor or format-changed skills, or before running another long multi-slice execute-plan session
scope: medium
selected: 2026-09-04
selected_as: one story covering this seed plus Pygardon SEED-008 stories 1–4 and 6
previously_resolved: 2026-09-04
previous_resolution: implemented as one five-slice execution-contract change; retained as the execution retrospective by developer request
reopened: 2026-09-05
reopened_reason: additional process findings from the completed notebook-publication execution; earlier implemented scope remains complete
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

The original scope was resolved on 2026-09-04. Reopened on 2026-09-05 for the
unimplemented findings below. Surface when evaluating `execute-plan` evidence
handoffs, retiring interim behavior, planning transaction-sensitive work, or
when another long run shows redundant proof runs or coordinator-context growth.
CI watcher lifecycle and repair scheduling have their own home in
[SEED-011](SEED-011-efficient-ci-failure-attention.md).

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

Source: `execution-retrospective` on the completed SEED-009 Story 1 plan (`.planning/quick/002-open-existing-notebook-locally/PLAN.md`, its final slices 13-15 plus an aggregate review of the full 15-slice execution), and a second `execution-retrospective` the same day on SEED-009 Story 2's in-progress plan (`.planning/quick/004-publish-local-note-content/PLAN.md`, slices 1-9, stopped by developer request). None of these findings changed a rule yet — all are candidates for the next revision of `execute-plan`'s references.

1. **Delegation-prompt overconfidence on an unverified claim.** A Slice 15 delegation prompt asserted a suspected code gap (`downloadNotebookGitBundle`'s plain-object `throw`) was "a real gap I already confirmed by reading the code," without the coordinator having actually traced the full call chain (`withBackendClient`'s catch/rewrap logic in `donutBackendClient.ts`). The implementer independently verified the claim, found it was *not* a bug, and avoided a regression the coordinator had nearly prescribed — but a less careful implementer could have trusted the "confirmed" framing at face value. Lesson: a coordinator should phrase a suspected-but-untraced gap as "verify this" rather than "confirmed" whenever it hasn't followed the call chain to its actual handler.
2. **A PLAN's "Current decisions" claims aren't automatically threaded into any slice's proof obligation.** The retrospective found a real bug (a dangling `origin` Git remote left in every successful `notebook clone` checkout) that directly contradicted a decision recorded up front in the original plan ("The CLI removes the temporary bundle origin") — yet no slice's proof ever asserted anything about `git remote` state; every Git-state check across all 15 slices covered branch/commit-count/tree-contents only. Lesson: when a "Current decisions" entry makes a concrete, checkable claim about final state, `slice-planning`/`slice-plan-refinement` should make sure at least one slice's proof actually checks it, not just the higher-level behavior it's bundled with.
3. **Refactor-pass prompts cost as much as or more than implementer prompts, largely restating the skill's own checklist.** Across the 9-slice Story 2 run, refactor-agent token usage ranged ~76k-154k tokens per pass — several times matching or exceeding the paired implementer's usage — even on passes that found nothing or one small fix. Each refactor delegation restated 5-8 bespoke "specifically check…" bullets largely duplicating `post-change-refactor`'s own `references/refactor-checks.md`. Lesson: keep refactor delegation prompts short (plan path, slice text, proof blocks, a pointer to run the skill) and trust the skill's own checklist to drive the findings, rather than the coordinator re-deriving and restating per-slice bullets every time.
4. **Pre-delegation research was still done inline rather than forked, despite this same seed's earlier fix.** The Story 2 coordinator read multiple source files directly (via targeted `sed`/offset excerpts, not full dumps) across all 9 slices to build delegation prompts, rather than using a disposable `fork` per `disposable-research.md`'s guidance. The targeted-excerpt discipline limited the damage, but the practice itself persisted across a full session despite being named in this seed's implemented scope (item 3 under "Implemented Scope" above). Lesson: the "fork for one-off research" guidance needs to be more forcing (e.g., stated as a default rather than an encouragement) or it will keep being skipped when a quick inline `sed`/`Read` feels cheaper in the moment.
5. **A backend full-suite reconfirmation doesn't need to run after every backend-touching slice once a pre-existing-failure baseline is established.** The Story 2 coordinator reran the full `pnpm backend:test_only` suite only twice (after the first and last of six backend-touching slices) rather than after each one, once the exact failure signature (`StructuredResponseCreateParamsSerializerTest`, then later different `QuestionGeneration*` classes — both the same Spring `ApplicationContext` load-failure-cascade signature) was confirmed reproducible and unrelated to the change. Lesson: state this explicitly in `execute-plan`'s verification guidance so it's a documented judgment call, not something each session has to independently decide and justify.
6. **A useful Vitest file-size-vs-single-file-scheduling technique surfaced but isn't documented anywhere.** A Story 2 CLI test file that had to stay in one Vitest file (fixtures shared a temp-dir-leak assertion sensitive to concurrent file scheduling) but exceeded the 250-line guideline was later resolved (outside the reviewed session, by the developer directly) by extracting per-concept `*.suite.ts` files that each export a `describeXxx()` function, called from one thin runner `.test.ts` file — satisfying the size guideline without risking the concurrency-sensitive split. Lesson: add this pattern to `post-change-refactor`'s file-size guidance so a future refactor pass facing the same tension has a documented option besides reverting the split.

## Merged Findings from Completed Publication Execution (2026-09-05)

Source: the completed 18-slice SEED-009 Story 2 execution through `dd1ca6415a`
and its execution-retrospective. These are process proposals, not implemented
changes. The watcher proposal and developer's tolerance for finishing current
work before CI repair are captured only in SEED-011.

- **Strengthen finding 3: compact prompts also need compact inherited context.**
  Later implementer and refactor handoffs repeatedly used `fork_turns: "all"`,
  passing accumulated execution history despite the delegation contract's
  request for current slice text and compact proof. Short prompt wording alone
  does not fix that. Evaluate fresh agents receiving only the current outcome,
  paths, constraints, and exact proof, with resume state read from the plan as
  needed. Preserve independent refactor judgment and required instruction reads;
  do not infer that eliminating review is the saving.
- **Retire an interim behavior across its whole user path.** Slice 16 enabled
  publishing but missed an installed-CLI clone assertion of the old guidance,
  causing three CI failures before repair `dd1ca6415a`. The aggregate review
  also found CLI rejection bodies still discarded in favor of "publishing is
  not available yet", with fixtures and comments preserving that interim
  contract. When replacing temporary behavior, check its callers, fixtures,
  tests, and documentation, and verify the final observable errors as well as
  success. Do not add historical-only tests merely asserting removal of old
  code. The actual product correction is separately planned in
  `../quick/005-report-notebook-publish-rejections/PLAN.md`.
- **Validate shared transaction-fixture assumptions before broad migration.**
  Two attempts were reversed after rollback-scoped fixtures conflicted with
  `REQUIRES_NEW` binding locks. A dedicated committed fixture did not resolve
  the dependency in the entire existing controller-test family. Establish the
  transaction ownership of the affected fixture family early, then use the
  smallest representative proof before repeating production migration work.
  The lesson is targeted uncertainty resolution, not a generic preparatory
  architecture phase or more test layers.
- **Bound diagnostic evidence before it enters coordinator context.** CI
  investigation returned full job metadata and broad logs, hit truncation,
  then retrieved the same evidence again. Select failed jobs and meaningful
  excerpts before returning results to the model, retain provenance and a path
  to deeper evidence, and expand only when the cause remains uncertain. Useful
  diagnosis costs tokens; repeated irrelevant metadata does not improve it.
  This principle also applies outside CI and belongs in this seed.

Finding 5 needs an evidence qualification: later execution identified local
MySQL connection exhaustion and obtained green full backend runs after a
runtime capacity adjustment. Earlier changing ApplicationContext failures
should not become a blanket rule to classify future failures as harmless or
skip mandatory proof. Distinguish demonstrated baseline issues, diagnosed
environment limits, and unresolved defects before choosing focused reuse.

## Cross-project Retrospective Findings and Possible Responses (2026-09-05)

The developer relayed Pygardon's agreement that Doughnut should lead two small,
transferable correctness improvements. Its independent evidence is retained in
[Pygardon SEED-008](../../../pygardon/.planning/seeds/SEED-008-efficient-plan-execution-evidence-handoffs.md).
These remain retrospective findings and possible responses, not a story queue.

- **Promises can escape proof wherever they are written.** Generalize the
  dangling-clone-origin finding beyond "Current decisions": connect every
  checkable final-state promise in the selected contract to a named slice and
  observable verification. Preserve the connection when refining, replacing,
  or resuming slices. Existing proof may satisfy several promises; do not
  manufacture a separate test or slice for each sentence.
- **A replacement can leave obsolete behavior along the affected user path.**
  Doughnut retained interim publication refusals; Pygardon independently found
  obsolete report payloads surviving a replacement. Consider callers, fixtures,
  assertions, documentation, and final success/rejection behavior together when
  planning and accepting the replacement. Search results locate affected scope;
  observable final behavior supplies the proof.
- **Direct exception propagation does not prove lifecycle ownership.**
  Pygardon's Telegram retrospective found missing timely observation of failed
  background work and cleanup skipped after failure. When asynchronous ownership
  changes, name the owning lifecycle and prove its failure observation and
  resource release at the appropriate boundary. Apply this conditionally to
  relevant product work; it is not a requirement for extra propagation tests at
  every caller, or authorization for CI-related work.
- **Behavioral proof belongs to planning and execution.** Refactor review can
  identify incoherence, but its structure-only remit must not become responsible
  for supplying missing behavior. Route a discovered behavioral gap back to the
  implementation/proof loop before accepting completion.
- **Wording alone is not evidence of improvement.** Evaluate the proposed
  contracts against the original escaped defects and a real execution. Preserve
  compact, attributable evidence and focused commit IDs for Pygardon to adapt,
  keeping each repository's tooling and CI policies intact. Distinguish a
  retrospective replay from prospective execution evidence.

## Selected Follow-up and Deferred Responses

The developer selected planning of the two correctness improvements above in
[the proof-coverage plan](../quick/008-preserve-promises-through-execution/PLAN.md).
No recommendation above was rejected. The async ownership case is a conditional
application of behavioral proof, not a third improvement project. Implementation
and evaluation remain pending; the original implemented scope stays complete.

Keep the implemented direct formatter, reusable proof, and independent refactor
pass. Prefer small additions at the existing instruction owners, without new
agents, duplicate checklists, or a separate tracking framework.

Concise handoffs/inherited context and bounded diagnostics remain possible
efficiency responses to evaluate against measured context and review quality.
Shared transaction-fixture checks remain a response for the next applicable
transaction change; establish ownership with a small representative proof before
repeating migration work. The Vitest suite-extraction technique remains optional
guidance for a future matching case. These are deferred, not execution leaves.
All CI-related issues are excluded from the selected work and handled separately.
