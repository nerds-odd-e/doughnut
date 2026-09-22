# Finish or stop

On completion, human-judgment stop, or cancellation, close the observer through the current
host adapter: handle delivered failures, then stop observers without waiting for CI. Report pending CI
as unobserved.

After all planned slices satisfy proof/delivery and required observer shutdown succeeds,
report completion, retained evidence, and CI limitations. `--skip-retro` skips only this
execution's automatic retrospective; it changes no preferences or proof/delivery/shutdown
obligations. Explicit omit/defer instructions also take precedence. When skipped, report it
and end with `## PLAN EXECUTION COMPLETE`, retaining plan/evidence for later review and wrap-up.

Otherwise report `## PLAN EXECUTION COMPLETE`, then invoke
[dough-execution-retrospective](../../dough-execution-retrospective/SKILL.md) without another
confirmation. Preserve explicit review instructions and project preferences through its
review selection; its authority excludes implementing findings or changing the backlog.

Continue in the recorded execution project/checkout. Supply available references/context:
source contract, original plan and approved changes, attributable commits, decisions, proof,
delivery state, CI limitations, and checkout/branch identity. Trunk Mode attributable
commits are that identity's retained published revisions, not another ledger or a
rewrite's unpublished SHA. Include an initial quick attempt and its planned
continuation as one execution. Reuse context without another handoff artifact
or transcript copy; retrospective validates attribution and recovers real gaps.

Execution completion and review completion are distinct. A retrospective context stop leaves
execution complete: report missing input without rerunning implementation or claiming review
completion. On recovery, use retained review state to continue or recognize completed review;
ambiguous state requires recovery rather than duplicate review or guessed completion.

Retain the completed plan, evidence, execution checkout, branch, and worktree for story
wrap-up; do not invoke it here. Wholly planless completion retains source, conversation,
identity, delivered changes, and proof, reports delivered work and shutdown, and ends with
`## QUICK EXECUTION COMPLETE` after required delivery/shutdown, without automatic
retrospective. The coordinator invokes wrap-up after successful branch delivery. Do not
report integrated completion here; wrap-up owns Story Branch merge and required
target push, Trunk Mode closure publication, and resource cleanup.

For incomplete work, failed delivery/shutdown, cancellation, or a human-judgment stop, report
source, active plan/next slice or quick-slice state, preserved work, observer state,
and required decision/recovery action. Emit no completion marker or automatic retrospective.
