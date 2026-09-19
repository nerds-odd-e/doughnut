---
name: dough-story-wrap-up
description: >-
  Closes one completed feature story, bounded retrospective correction, or
  context-only planless execution using available execution context. Assimilates
  lasting product knowledge, handles existing follow-ups and authorized product
  decisions, removes that work's spent plan and history so Git can recover it,
  and reports truthfully when required inputs are missing or unfinished. Use to
  wrap up a story, correction, or completed contextual instruction, close
  completed work, or delete spent plan and execution history.
---

# Story wrap-up

Close one selected feature story, bounded retrospective correction, or contextual
instruction when the coordinator invokes wrap-up. Use available execution context
and optional retrospective advice. Leave this project with maintained product knowledge
and no spent source or plan history in the current snapshot. Do not invent findings,
records, or a requirement for another conversation.

## Resolve this project's context

Require one selected work identity. Use a canonical feature story only when it
is explicitly supplied as that work's active home. Otherwise use the bounded
correction plan itself when it satisfies the correction-input contract defined by
[planning scope and lifecycle](../dough-story-refinement/references/planning.md#choose-the-planning-level).
Do not require or create a seed for that correction. When the source is neither
a story nor a correction plan, use a contextual instruction with retained
execution identity, changes, and proof; do not require or create a story, plan,
or queue entry. Name any missing required field, leave the affected work intact,
and stop before closure. Do not invent conventions, artifacts, another review, or
claim closure.

Resolve from this project, not this skill's location:

- repository root and Git working tree;
- canonical seed location, story identity, heading or stable-anchor conventions
  for a selected feature story;
- for planned work: executable-plan location, status vocabulary, and the
  selected work's plan identity;
- for planless work: the supplied story or instruction, its changes, available
  execution results, and retained execution identity;
- optional retrospective advice when present, including an empty result;
- Git commit conventions used to preserve a recoverable revision;
- selected mode and available originating checkout, execution checkout and
  branch, and integration-target identity;
- the product backlog path when a **Taken** or **Backlog list** entry points
  at the selected work; and
- shared records that name the selected work: its seed when applicable, process
  log (`DearDough.md` unless this project sets another canonical location),
  incoming links, and assessment records.

Before deleting a plan that carries planned-execution identity, retain the
resolved mode and checkout, branch, and target values in the coordinator's
available execution context for the remaining wrap-up actions and report. Do not
create a parallel registry. Missing identity needed by a later action stops that
action instead of reconstructing or guessing it after plan deletion.

## Establish execution completion

Judge completion from the selected work and available execution evidence.

- Planned work is complete when every slice is done.
- Planless work is complete when the supplied story or instruction, changes, and
  execution results show the promised outcome, including evidenced no-change.
- Incomplete implementation leaves the affected active work intact. Report the
  unfinished implementation and stop. Do not delete an unfinished correction
  plan to manufacture a wrap-up.

Apply available retrospective advice under existing authority. Absent/empty advice
still permits ordinary closure from work context, implementation results,
maintained knowledge, existing follow-ups, and coordinator instructions.

## Assimilate lasting knowledge

Move lasting behavior and design into this project's maintained code, tests,
documentation, or current Accepted decisions. Describe the current product
without execution narration, impact chronology, story or plan identity, or
retrospective judgments. Preserve existing product tests and documents that
already state current behavior. Do not invent product knowledge. When a current
product fact is written only in spent execution context or review and is not
already stated in maintained documentation, write it into maintained documentation
before deleting that spent copy. Tests that exercise related behavior do not
replace that documentation step.

For North Star topics cited, added, or revised by the completed work, apply the
shared [topic-retirement instructions](../dough-slice-planning/references/architectural-thinking.md#retire-temporary-direction-during-ordinary-wrap-up)
as part of ordinary closure.

## Queue an existing follow-up plan first

When an existing follow-up plan is present, validate it against the
correction-input contract above, then put its one canonical active home first in
the queue. Do not refine, replan, or execute it. Preserve the plan contents
needed for later execution. Handle that follow-up by its presence, including when
retrospective advice is absent.

Resolve one canonical active home under [dough-product-backlog](../dough-product-backlog/SKILL.md#canonical-active-homes):

- If a canonical follow-up story is supplied, keep that story in its seed and
  link the plan there. Queue the story; do not duplicate it as a plan entry.
- Otherwise queue the existing correction plan directly. The plan is its
  canonical active home; do not create or recover a seed solely for queueing.
- If required correction input is missing, name the missing field and do not
  guess or queue the addition. Keep the follow-up plan and any other needed
  active-work context, report the gap, and stop before deleting the completed
  predecessor's history.

Preserve unrelated queue order after that first item, near-future direction,
and human text. Repeating wrap-up must recognize either canonical home and must
not duplicate the follow-up or queue entry.

## Apply product-review decisions

When retrospective product advice or additional human input is present, apply
only authorized compatible backlog and canonical-home changes. Follow
[dough-product-backlog](../dough-product-backlog/SKILL.md) for queue and
active-home conventions. Explicit human input wins over advice.

Supported changes: relevant reorder, queue membership, understood new-story
addition, understood bounded-correction plan addition, and canonical-detail
edits. Apply the backlog skill's canonical-home admission rules to every new
entry. Keep existing follow-up work first unless a later explicit human
instruction changes that priority. Preserve unrelated content, still-needed
acceptance work, and near-future direction.

A skipped, empty, or absent product review, or absent extra human input,
introduces no mandatory question. Ordinary closure continues using the other
available inputs. Leave unresolved necessary context with active work and report
the choice; do not invent scope, launch discovery, or start another review.

## Commit closure inputs and preserve Git recovery

After supported follow-up queue changes and before deleting anything, make the
current revision recoverable with this project's ordinary Git conventions.
Commit all owned review and closure-input changes in that revision, including
applicable retrospective edits to the process log, an uncommitted active
follow-up plan and its queue edit, assimilated product knowledge, and the spent
material. Preserve unrelated changes and include only files or portions whose
ownership is unambiguous. Resolve ownership of the intended cleanup targets at
this boundary too, before deleting any of them. Then record that revision as the
before-cleanup commit, even when the current revision was already suitable. If
commit conventions, ownership, or recovery cannot be resolved, leave the material
intact, report the gap, and do not claim closure. When the selected mode is Trunk
Mode, publish that commit through [wrap-up closure publication](../dough-execute-plan/references/trunk-publication.md#publish-wrap-up-closure)
before deleting spent history. Preserve Git history as the sole recovery surface
for spent execution material; keep current product knowledge in maintained project
content.

## Delete spent history, including shared records

After completion and Git recovery are established — including any required Trunk
Mode before-cleanup publication — delete the selected work's spent history:

- its executable plan and owned proof, evidence, and assessment records, even
  when the plan was retained at execution completion;
- its canonical story section when one exists, and its seed only when every
  remaining section is spent;
- its **Taken** or **Backlog list** entry when one exists;
- its process-log occurrences, and issues or containers left empty; and
- links whose sole purpose is preserving that history.

Remove empty directories belonging to the spent work, including untracked ones.
The current snapshot must be free of that history, both tracked and untracked,
with recovery available from the recorded before-cleanup commit.

Preserve unrelated human text, sibling stories and log entries, product and
version identity, maintained tests and documents, still-needed acceptance work,
and active follow-ups. Shared records lose only the portions attributable to
the completed work; leave uncertain portions intact and report the ambiguity.

Keep an active follow-up's queue link, correction input, and provenance. If its
source locator points into deleted predecessor history, replace the locator
with the before-cleanup commit and repository-relative path. Repair Markdown
links broken by cleanup without recreating spent history. Repeating wrap-up
must recognize already-completed cleanup without duplicating edits. Missing
artifacts alone do not establish completion of another work item.

## Commit final closure

Review and commit the owned closure changes using this project's Git conventions,
preserving unrelated changes. Both the before-cleanup revision and the final
closure must be committed in either execution mode; uncommitted cleanup is not
completion. Report unresolved ownership or commit failures without claiming
closure.

Direct-current-branch mode ends with committed closure. Trunk Mode publishes the
final-closure commit through [wrap-up closure publication](../dough-execute-plan/references/trunk-publication.md#publish-wrap-up-closure),
then continues with resource cleanup below. Story Branch Mode continues with integration and resource cleanup below.

## Integrate committed Story Branch Mode closure

Save the committed final-closure tip and integrate it into the recorded target
from its checkout using the retained identity and this project's local merge
conventions. Preserve unrelated target work; unresolved identity or unsafe integration
leaves execution resources intact with the blocker reported. Do not rebase or wait for CI.

When the final-closure tip is not already integrated (below), perform this merge through the integration
checkout's installed product backlog merge adapter, not a raw `git merge`, whenever it touches the product
backlog (often `PRODUCT-BACKLOG.md`); see [reconcile product backlog Git operations](../dough-product-backlog/references/merge-conflicts.md)
from the integration checkout's installed guidance for how to resolve and run it. A `conflict`/`refused`/`refused-before-commit`/`blocked` result leaves the unmerged backlog path exactly as
Git left it. Resolve it following that reference, `git add` it, then run the adapter's own `continue` for
this same merge — never a raw `git merge --continue`/manual commit — and complete its staged-result
verification before the merge is committed; selected-work cleanup and final-tip ancestry alone do not prove
sibling backlog changes survived. If neither the adapter nor that reference is available, leave the backlog
conflict unresolved and report the missing guidance. Resolve other conflicts from both sides' intended
behavior, surrounding code, history, and available work context. Verify with appropriate checks and
complete the merge. When evidence cannot justify a coherent resolution, stop and preserve the conflict for
a human decision; report the incompatible intentions or missing decision, conflicted paths, and Git state.

Integration requires the saved final-closure tip to be an ancestor of the recorded
target containing the committed closure. Recognize an integrated tip without merging
again. Unresolved integration preserves the execution branch and worktree and blocks
completion.

When the recorded target is `main`, push the integrated target to `origin` with
`git push origin main` from the target checkout after verifying integration. This
also applies to an already-integrated tip on retry. Require a successful push before
resource cleanup or claiming completion. If the push fails, retain the execution
resources and report the push failure separately from successful local integration;
do not force-push.

## Remove execution resources safely

After Story Branch Mode's verified integration and required target push, or after
Trunk Mode's wrap-up closure publications and wrap-up observer shutdown, remove
this execution's clean local worktree and local execution branch. Use retained
identity and non-force operations. Preserve unrelated resources, unique or
unpublished work, a dirty checkout, and a worktree that still hosts an active
checkout-bound observer. Trunk Mode never deletes a remote execution branch.
Story Branch Mode deletes the remote branch only when its tip is integrated in
the remote target. Verify removal, accept already-absent resources on retry,
report blocked or partial cleanup without repeating already-completed closure,
and skip cleanup in direct-current-branch mode.

## Report

Report the selected work and identity, completion judgment, mode and retained
checkout/branch/target, before-cleanup and final-closure commits when deletion
happened, Trunk Mode published closure SHAs and remaining CI coverage, assimilated
knowledge, deleted paths, Story Branch saved tip and integration/push results when
the target is `main`, worktree and branch cleanup results (remote deletion only when
verified absent), preserved material and resources, and any gap. Distinguish a new
merge from an already-integrated tip, integration from refused cleanup, local
integration from a successful `origin` push, and completed wrap-up from a refusal
that left files intact.

End successful closure with `## STORY WRAP-UP COMPLETE`. Missing context, unfinished
work, unresolved recovery/integration, required push, unpublished Trunk Mode closure,
an active wrap-up observer, or resource cleanup blocks that marker.
