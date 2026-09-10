---
name: dough-execution-retrospective
description: >-
  Reviews one completed or in-progress plan execution against its original story,
  aggregate commit set, current whole-product architecture, and test suite. Use for an execution
  retrospective, product review, or backlog recommendation even when cleanup
  removed the plan or the user supplies only a partial reference. `--skip-process`
  and `--skip-product` omit those reviews independently. May plan unresolved
  implementation findings, record supported process findings in `DearDough.md`,
  and recommend product work; never implements them.
---

# Review an execution

Recover what one plan intended, identify the commits that executed it, and
review their combined outcome, current product architecture, and whole test suite. By default,
cover implementation, process, and
product learning. Leave the project with evidence and, only when needed, a
plan for bounded corrections. Do not implement, commit, or push those
corrections. A retrospective authorizes product recommendations; it does not grant
backlog-write authority. Leave the completed plan and routine
completion or backlog actions for
[dough-story-wrap-up](../dough-story-wrap-up/SKILL.md).

## Select reviews

Ordinary invocation considers implementation, process, and product review.
`--skip-process` omits process analysis and recording, including any
`DearDough.md` write when that destination exists. `--skip-product` omits
product analysis and suggestions. Both flags may be supplied together.
Neither skips implementation review or its correction planning.

Choose the enabled set before loading focus-specific context or acting on that
focus. Skipped product review does not suppress the shared direction consideration
in implementation or enabled process review. Do not covertly review a skipped
focus or write its destination.

## Work from these principles

- **Original intent is the contract.** Recover the story, boundaries, approved
  changes, and promised proof before judging implementation.
- **Commit membership needs evidence.** A nearby commit is not part of the
  execution merely because it is in the same range.
- **Judge the aggregate result and current architecture.** Review the combined
  execution outcome and the whole product's conceptual structure, including
  relevant untouched code. Delivery decomposition is not a design objective;
  coincident product boundaries need independent domain justification.
- **Current truth decides remediation.** Report later fixes and do not plan work
  that is already resolved.
- **Plan state decides the destination.** Amend an unfinished plan; create a
  follow-up plan only for a completed execution.
- **The user owns disputed scope and constraints.** Stop when evidence cannot
  distinguish two plans or when a finding would change the story rather than
  correct it. Use the shared
  [plan-conflict handoff](../dough-execute-plan/references/execution-decisions.md#resolve-a-disputed-plan-restriction)
  for apparently accidental contractual restrictions; plan compliance does not
  settle their justification.
- **Product learning is not an implementation defect.** Keep product
  recommendations out of correction plans and process findings.
- **Direction is a criterion, not a deliverable.** Question alignment of
  work and process with the established near-future direction; never propose
  or edit that text.

## Resolve this project's context

Require one useful clue: a capability or story phrase, plan path, commit, or the
current execution conversation. Resolve this project's plan and story locations,
status vocabulary, cleanup lifecycle, repository navigation, and focused test
commands. Preserve existing working-tree changes.

Resolve this project's established near-future direction when present. When
product review is enabled, resolve backlog and canonical-story conventions when
that review needs them. Do not invent a direction, backlog, or seed location.

If context needed for a review decision is missing, name it and stop that path.
Do not invent a plan location, completion rule, or project convention.
Return retrospective evidence in the response; do not create a separate artifact
unless the user asks. Keep the repository read-only except for the process log
and an allowed plan update described below.

Read [dough-post-change-refactor](../dough-post-change-refactor/SKILL.md) and its
refactor checks before assessing refactoring residue; apply its smell definitions
to the aggregate result without running its editing workflow. Read
[dough-slice-planning](../dough-slice-planning/SKILL.md) only when unresolved
findings need planning, then follow its
[bounded-correction entry](../dough-slice-planning/SKILL.md#require-an-understood-story),
proof, sizing, and destination gates. Read [dough-product-backlog](../dough-product-backlog/SKILL.md) only when
product review is enabled and recommendations depend on those conventions.

## Recover one execution

Search the current conversation, current planning material, and Git history in
that order. A partial reference or a plan removed by normal cleanup is sufficient
when history identifies it. Recover the earliest execution-ready plan, its story
and intended outcome, and any later changes supported by user approval or new
evidence.

Determine completion from the latest plan state and execution evidence, not file
presence. Any planned or in-progress slice makes the plan unfinished. A deleted
plan needs history evidence of completion. If two candidates remain equally
plausible, ask the user to choose and do not combine them.

Build a manifest of related commits. Include each SHA with a reason grounded in
the plan, commit message, diff, or execution transcript. Inspect intervening
commits and exclude unrelated work. Treat planning-only commits as provenance,
not product findings.

Use one net diff only when the implementation commits form an uncontaminated
range. Otherwise review the selected patches together and inspect their files at
the last related implementation commit. Never mutate the worktree to reconstruct
history or mix later work into the historical boundary.

## Consider near-future direction

For every enabled review, treat the established near-future direction as a
high-priority criterion. Read it once from the resolved project location. If it
is missing, say alignment cannot be assessed against an established direction
and continue the independently supported reviews. Otherwise question apparent
alignment and digression. Explain justified exceptions such as urgent fixes. Do
not merely assert that the work fits.

Route a supported deviation through that review's existing authority:

- Implementation: supported defects and architectural weaknesses go to bounded
  correction planning under current truth. A needed product-constraint or
  promised-outcome change is the user's decision, not a rewritten historical
  contract.
- Process: produce a process recommendation; do not add it to an
  implementation correction plan.
- Product: recommend work or priorities. Do not treat a direction mismatch as
  an implementation defect.

A later change in direction does not retroactively make approved historical work
a defect. Judge that work against its original approved contract; use today's
direction only for remaining or proposed work.

Never propose or apply a replacement or revision of the direction itself.

## Review the outcome

Apply the shared direction consideration. Then compare the story contract and
approved changes with the aggregate code, tests, documentation, and proof at
the execution boundary. For the historical assessment of an unfinished plan,
judge only the completed slices;
do not call unexecuted planned behavior missing or its explicitly temporary
predecessor obsolete.

Keep only findings with concrete evidence and plausible impact:

1. bugs or regressions;
2. story drift or an unresolved scope dispute;
3. refactoring residue in complete implicated concepts;
4. consequential weaknesses in the current whole-product architecture; and
5. test coverage or execution-cost findings under the shared behavioral test guidance.

Assess overall responsibilities, dependencies, and representations against the
product's domain, not the story sequence. Ask whether successive examples
exercise a coherent model or accumulate special cases. Use the shared refactor
checks for concrete concept examination; shared helpers alone do not establish
cohesion. Follow architectural evidence beyond the changed files to relevant
untouched code, and explain the concrete impact of a weakness, such as divergent
domain rules or changes requiring repeated coordinated edits. Whole-product
assessment is required; it does not require speculative redesign or treating
cosmetic preferences as defects. Apply
[dough-adr-awareness](../dough-adr-awareness/SKILL.md) to genuine architectural
constraints and leave conflicting decisions with the human.

Keep historical attribution separate from current assessment: only claim this
execution introduced a defect when its provenance supports that claim. An older
weakness can warrant current correction without becoming an execution regression.

Identify whether E2E tests drove this execution's development, then assess the
whole suite, including older tests outside the story, using
[tests as behavioral documentation](../dough-post-change-refactor/references/refactor-checks.md#tests-as-behavioral-documentation).
No newly added E2E tests does not exempt existing coverage from review. Ground
retention, detail downgrades, and overlap consolidation in actual coverage and
cost findings; preserve important journey documentation and integration proof.
Use the project's testing guidance when supplied and the shared black-box
fallback otherwise. Whole-suite assessment does not require running every test.

Look explicitly for additions later worked around or replaced: dead branches,
flags, callers, fixtures, compatibility paths, overlapping tests, tests of
obsolete internals, and documentation that preserves implementation history
instead of product truth. An explicit user decision is not drift. Style
preferences, speculative redesigns, duplicate symptoms, and unsupported claims
are not findings. Do not retain a negative test or documentation merely to prove
that temporary behavior is gone unless its absence is an enduring requirement.

Surface a disputed plan restriction through that handoff before planning its
removal. Leave the disputed correction pending the human decision, retain genuine
constraints, and continue independently supported reviews. Do not silently
rewrite the historical contract or treat preservation as resolution.

Use focused read-only checks when they can confirm or dismiss a finding. Do not
run broad suites.

## Reconcile findings with current truth

Recheck every finding against the current revision and working tree. Report a
later fix and deduplicate remaining findings by root cause. Use current evidence
to bound needed corrections across the product, including outside the old
story's implementation footprint. Preserve existing product promises and genuine
constraints using the shared
[scope distinction](../dough-story-refinement/references/planning.md#examples-and-constraints);
review reach does not authorize new feature promises. If none remain, leave
planning unchanged.

Give unresolved test downgrades and consolidation, including older redundant
tests, explicit ownership in the bounded correction plan. Name the retained
meaningful coverage and integration proof for consolidation; existing retained
E2E coverage may suffice. For detail downgrades, make replacement unit coverage
a prerequisite to removing or narrowing the corresponding E2E tests. Apply the same current-truth and
destination rules as other corrections; the retrospective plans suite cleanup
and does not perform it.

For an unfinished plan, update that plan in place. Preserve completed and
in-progress evidence and history; place corrective work before still-planned
work and revise overlapping planned slices instead of duplicating them. Do not
renumber completed slices. Record the finding and reviewed commit manifest as a
concise learning when this project's plan format supports it.

For a completed execution, use `dough-slice-planning` to create one follow-up
plan in this project's established location. Cite the original story and commit
manifest as historical provenance, and the current findings as the correction's
scope and evidence. State one bounded correction outcome, affected concepts,
concrete impact, preserved behavior, and focused proof. This may reach beyond the
original story without rewriting its promises or attributing older defects to
it. If correction would change product constraints or promised outcomes, stop
for the user's decision. Stop likewise when the findings cannot form one bounded
correction.

When two authorized reviews cover the same plan, only the designated writer
reconciles findings into it; the other reviewer returns read-only evidence.
After any planning change, do not refine or execute that correction unless the
user separately requests it. Continue every other enabled review, then
report. That restriction applies to correction refinement and implementation, not
to other enabled reviews.

## Review process only from a real record

When process review is enabled, apply the shared direction consideration, then
when the current conversation or a sufficiently complete transcript contains the
execution, separately identify evidence-backed process improvements: wasted
work, rule-induced churn, a missing stop condition, a disproved sizing or
decomposition assumption, avoidable digression from direction, or a useful
practice to learn. Consider whether instructions were concise and context was
organized for easy consumption, including this retrospective's own avoidable
rereading, duplication, or reconstruction. Distinguish necessary investigation
from avoidable waste.

Record observations separately from inferred cost and cause. Use token counts
only when they are available in the record; otherwise cite the repeated work and
qualify the cost. Neither shorter text nor skipped necessary investigation proves
improvement. If the record is insufficient for a process conclusion, state that
limit instead of manufacturing a finding. Do not infer missing events, edit
guidance, require token measurement, or recursively launch another retrospective.
Do not put process proposals into the repository correction plan.

### Record supported process findings

After process analysis, record its supported findings in the project's canonical
`DearDough.md`. This is a narrow process-recording allowance; it does not authorize
other project or product maintenance. Product-only findings and implementation
corrections stay in their own destinations. Preserve every unrelated file.

Apply `--skip-process` before resolving, checking, or reading the log location.
When process review is skipped, do not create, read, or edit the log. When enabled
review yields no supported process finding, do not create an empty log and leave
an existing log unchanged. `--skip-product` does not suppress process recording.

Use `<project-root>/DearDough.md` unless the user or this project's conventions
explicitly establish another canonical location for that filename. An explicit
location wins over the root default. Do not search other projects or invent an
alternative. If the project root or canonical location is missing or conflicting,
return the findings with that limitation and stop recording only; continue every
independently supported review.

Identify the reviewed execution before writing. Reuse an execution identity
already present in the log when available. Otherwise combine its canonical plan
or story reference with its first related implementation commit. If it has no
implementation commit, use an available stable execution-record reference. A
later commit, another review, or the review date does not create another
execution. If identity evidence is missing or conflicting, return the findings
without a countable occurrence, report the limitation, and continue the other
reviews. Do not make an identity from today's date or introduce a tracking
system.

For a new log, write this minimal Markdown shape, assigning `DD-001` upward in
the order of supported findings:

```markdown
# DearDough Process Findings

## DD-001 — <descriptive issue title>

<concise concrete description>

### Occurrences

- Execution: <stable execution identity>
  - Tool: <Codex, Cursor, Claude Code, or another identified tool>
  - Model: <model identifier, when available>
  - Evidence: <decisive compact references or locators>
  - Observed effect: <what the record shows>
  - Inference: <qualified cause, cost, or uncertainty, only when needed>
```

Keep observation separate from inference. Use compact references rather than
transcript copies. The occurrence rows are the count; do not add a redundant
total. Record one-off costs, useful practices, potentially general problems,
and supported observations about this retrospective without claiming recurrence
or generality the evidence does not establish.

Record the tool name for every new occurrence. Use the tool that executed the
work, such as Codex, Cursor, or Claude Code, rather than the tool reviewing it.
Record the model identifier when the execution evidence supplies it; otherwise
omit the Model line instead of guessing or performing a separate lookup. If the
executing tool cannot be identified, report the finding without adding a
countable occurrence. Do not backfill older rows without supporting evidence.

When the canonical log exists, maintain it only when its issue headings,
descriptions, and occurrence rows are interpretable enough to identify the
affected issue, execution, and next unused local ID. Preserve existing IDs,
human notes, prior evidence, unrelated entries, and all content outside the
smallest supported edit. Do not migrate, normalize, reorder, delete, or
automatically merge existing content.

Match an existing issue only when decisive evidence supports the same concrete
process problem or useful practice; similar wording or symptoms do not establish
that match. If the relationship is uncertain but the log itself is interpretable,
create a separate issue with the next unused `DD-NNN` ID and briefly state the
matching uncertainty. Never change another issue's ID to fill a gap.

Within a matched issue, treat equal execution identities as one occurrence. An
identical rereview makes no edit. Add only newly available decisive evidence or
a corrected qualified conclusion to that existing row, without discarding its
prior evidence or human notes. Evidence of the same concrete issue in a distinct
execution adds one occurrence row. A second symptom in the same execution does
not add a row. Keep rows as the count; do not store or update a total.

If malformed or ambiguous content prevents safe identification of entries,
executions, or the next unused ID, leave the entire existing file byte-identical
and report a recording limitation. Do the same when a write fails. Return the
supported findings and continue other independently supported reviews; never
describe either case as a successful write.

In the final response, give a concise recording result: the canonical path and
created issue IDs with occurrence rows, or `unchanged`/`not recorded` and the
reason. Do not describe a skipped, refused, or failed write as successful.

Surface a concrete overlooked request, decision, warning, failed verification,
or Jidoka stop only when the record clearly shows that it still needs user
attention. Put this banner at the absolute end when that gate passes:

```text
!!!!!!!!!! DEVELOPER ATTENTION REQUIRED !!!!!!!!!!
<the overlooked item, its impact, and the response needed>
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
```

## Review product learning

When product review is enabled, apply the shared direction consideration, then
inspect queue entries and canonical stories only when they are relevant to this
execution or a supported finding. An isolated urgent fix with no connection to
other queued work does not trigger queue investigation. Preserve existing priority
instructions.

Connect supported learning to a relevant priority or story recommendation, or
state a reasoned no-change result. Do not claim to validate unread backlog
entries. Do not invent learning. Label inspirations as hypotheses. When
beneficiary or outcome is unresolved, return a concrete exploration proposal; do
not launch discovery or decomposition.

If backlog or story conventions cannot be resolved, keep product conclusions
provisional, identify that gap, and do not invent files. Independent supported
implementation and enabled process review still proceed.

Do not apply backlog or canonical-story edits during retrospective. Report
recommendations, remaining proposals, and unresolved choices for
[dough-story-wrap-up](../dough-story-wrap-up/SKILL.md). Standalone
[dough-product-backlog](../dough-product-backlog/SKILL.md) maintenance remains
available when a human requests it separately. A skip option is never write
authority.

Leave unresolved: a disputed goal or scope, conflicting explicit priorities, and
ideas whose beneficiary or outcome is unknown. Do not implement product or
implementation findings.

## Report

Report the resolved story and completion state, provenance, included commit
manifest and review boundary, findings ordered by impact or `none`, planning
result, and evidence limitations. Include supported process proposals only for
enabled process review. Include product recommendations or a reasoned no-change
result only for enabled product review; do not report backlog writes from this
skill. Omit skipped-focus analysis, suggestions, and destination
writes. Distinguish evidence from hypotheses, and recommendations from
proposals and unresolved choices. State whether planning was updated in place,
newly generated, read-only, or unchanged. End with:

`## EXECUTION RETROSPECTIVE COMPLETE`

Append the attention banner after that marker only when its evidence gate
passes; otherwise nothing follows the marker.
