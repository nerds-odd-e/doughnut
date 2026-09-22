# Finish or stop

On a human-judgment stop or cancellation, handle delivered failures, then close
the observer through the current host adapter without waiting for CI and report
pending CI as unobserved. On normal completion, use
[the one bounded applicable-revision wait](ci-monitor.md#await-the-applicable-revision-at-completion),
handle its result, and perform existing shutdown before the final handoff.

After all planned slices satisfy proof/delivery, `--skip-retro` skips only this
execution's automatic retrospective; it changes no preferences or
proof/delivery/wait/shutdown obligations. Explicit omit/defer instructions also
take precedence. When skipped, await the applicable accepted revision at this
execution-completion boundary, handle its result, shut down the observer, report
the CI verdict or exact unresolved reason, and end with
`## PLAN EXECUTION COMPLETE`, retaining plan/evidence for later review and
wrap-up.

Otherwise invoke
[dough-execution-retrospective](../../dough-execution-retrospective/SKILL.md)
without another confirmation as soon as delivered implementation is available,
including while applicable CI is pending. Preserve explicit review instructions
and project preferences through its review selection; its authority excludes
implementing findings or changing the backlog. Do not emit
`## PLAN EXECUTION COMPLETE` first or require that banner as retrospective
input. When review returns, perform the applicable-revision wait, handle its
result, shut down the observer, then report completion, retained evidence, CI
verdict or limitation, and `## PLAN EXECUTION COMPLETE` as the final
execution/review handoff.

Continue in the recorded execution project/checkout. Supply available references/context:
source contract, original plan and approved changes, attributable commits, decisions, proof,
delivery state, CI limitations, and checkout/branch identity. Trunk Mode attributable
commits are that identity's retained published revisions, not another ledger or a
rewrite's unpublished SHA. Include an initial quick attempt and its planned
continuation as one execution. Reuse context without another handoff artifact
or transcript copy; retrospective validates attribution and recovers real gaps.

Execution completion and review completion are distinct. A retrospective context
stop leaves implementation delivered but the final execution/review handoff
incomplete: preserve its partial review, observer, applicable target/revision,
and CI state, and report the missing input without rerunning implementation or
claiming review completion. On recovery, use retained review state to continue
or recognize completed review; ambiguous state requires recovery rather than
duplicate review or guessed completion. A CI failure handled through an
authorized repair invalidates only the conclusions affected by changed code;
resume those conclusions instead of restarting the full retrospective.

Retain the completed plan, evidence, execution checkout, branch, and worktree for story
wrap-up; do not invoke it here. Wholly planless completion retains source, conversation,
identity, delivered changes, and proof, awaits the applicable accepted revision
when publication created that CI obligation,
reports delivered work, the CI verdict or exact unresolved reason, and shutdown,
and ends with `## QUICK EXECUTION COMPLETE` after required delivery/wait/shutdown,
without automatic retrospective. The coordinator invokes wrap-up after successful branch delivery. Do not
report integrated completion here; wrap-up owns Story Branch merge and required
target push, Trunk Mode closure publication, and resource cleanup.

For incomplete work, failed delivery/shutdown, cancellation, or a human-judgment stop, report
source, active plan/next slice or quick-slice state, preserved work, observer state,
and required decision/recovery action. An applicable CI failure that existing
handling cannot resolve is such an incomplete stop, even when review finished
and observer shutdown succeeded. Emit no completion marker or automatic retrospective.
