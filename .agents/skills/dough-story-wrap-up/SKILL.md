---
name: dough-story-wrap-up
description: >-
  Closes one completed feature story or bounded retrospective correction using
  available execution context. Assimilates lasting product knowledge, handles
  existing follow-ups and authorized product decisions, removes that work's spent
  plan and history so Git can recover it, and reports truthfully when required
  inputs are missing or unfinished. Use to wrap up a story or correction, close
  completed work, or delete spent plan and execution history.
---

# Story wrap-up

Close one selected feature story or bounded retrospective correction when the
coordinator invokes wrap-up. Use available execution context and optional
retrospective advice. Leave this project with maintained product knowledge and
no spent source or plan history in the current snapshot. Do not invent
findings, records, or a requirement for another conversation.

## Resolve this project's context

Require one selected work identity. Use a canonical feature story only when it
is explicitly supplied as that work's active home. Otherwise use the bounded
correction plan itself when it satisfies the correction-input contract defined by
[planning scope and lifecycle](../dough-story-refinement/references/planning.md#choose-the-planning-level).
Do not require or create a seed for that correction. Name any missing required
field, leave the affected work intact, and stop before closure.

Resolve from this project, not this skill's location:

- repository root and Git working tree;
- canonical seed location, story identity, heading or stable-anchor conventions
  for a selected feature story;
- for planned work: executable-plan location, status vocabulary, and the
  selected work's plan identity;
- for planless feature work: the story, its changes, and available execution
  results;
- optional retrospective advice when it is present, including an empty
  result;
- Git commit conventions used to preserve a recoverable revision;
- for planned execution, its selected mode and available originating checkout,
  execution checkout and branch, and integration-target identity;
- the product backlog path when a **Taken** or **Backlog list** entry points
  at the selected work; and
- shared records that name the selected work: its seed when applicable, process
  log (`DearDough.md` unless this project sets another canonical location),
  incoming links, and assessment or recognition records.

Do not invent a retrospective artifact or require another review. Do not invent
a plan path, completion rule, feature-story seed location, or Git convention.
Planless feature work does not require a plan. Keep the bounded-correction
identity contract above; do not invent a planless correction format.

If context needed for a closure decision is missing, name the gap, leave
affected material intact, and do not claim closure.

Before deleting a plan that carries planned-execution identity, retain the
resolved mode and checkout, branch, and target values in the coordinator's
available execution context for the remaining wrap-up actions and report. Do
not create a parallel registry. Missing identity needed by a later action stops
that action instead of reconstructing or guessing it after plan deletion.

## Establish execution completion

Judge completion from the selected work and available execution evidence.

- Planned work is complete when every slice is done.
- Planless feature work is complete when the supplied story, changes, and
  execution results show the promised outcome is delivered.
- Incomplete implementation leaves the affected active work intact. Report
  the unfinished implementation and stop. Do not delete an unfinished
  correction plan to manufacture a wrap-up.

When retrospective advice is present, apply it under existing authority in the
closure actions below. When it is absent or empty, perform the same ordinary
closure using the other available inputs: the story or bounded-work context,
implementation results, maintained product knowledge, existing follow-ups, and
coordinator instructions.

## Assimilate lasting knowledge

Move lasting behavior and design into this project's maintained code, tests,
documentation, or current Accepted decisions. Describe the current product
without execution narration, impact chronology, story or plan identity, or
retrospective judgments. Preserve existing product tests and documents that
already state current behavior. Do not invent product knowledge. When a current
product fact is written only in spent execution context or review and is not
already stated in maintained documentation, write it into maintained
documentation before deleting that spent copy. Tests that exercise related
behavior do not replace that documentation step.

For North Star topics cited, added, or revised by the completed work, apply the
shared [topic-retirement
instructions](../dough-slice-planning/references/architectural-thinking.md#retire-temporary-direction-during-ordinary-wrap-up)
as part of ordinary closure.

## Queue an existing follow-up plan first

When an existing follow-up plan is present, validate it against the
correction-input contract above, then put its one canonical active home first
in the queue. Do not refine, replan, or execute it. Preserve the plan contents
needed for later execution. Handle that follow-up by its presence, including
when retrospective advice is absent.

Resolve one canonical active home under
[dough-product-backlog](../dough-product-backlog/SKILL.md#canonical-active-homes):

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
available inputs. Leave unresolved necessary context with active work and
report the choice; do not invent scope, launch discovery, or start another
review.

## Leave no extra wrap-up ceremony

Do not launch discovery or another review. Absent or empty retrospective
advice is valid and changes nothing beyond the supported closure and follow-up
actions above.

## Commit closure inputs and preserve Git recovery

After supported follow-up queue changes and before deleting anything, make the
current revision recoverable with this project's ordinary Git conventions.
Commit all owned review and closure-input changes in that revision, including
applicable retrospective edits to the process log, an uncommitted active
follow-up plan and its queue edit, assimilated product knowledge, and the spent
material. Preserve unrelated changes and include only files or portions whose
ownership is unambiguous. Resolve ownership of the intended cleanup targets at
this boundary too, before deleting any of them. Then record that revision as
the before-cleanup commit, even when the current revision was already suitable.
If commit conventions, ownership, or recovery cannot be resolved, leave the
material intact, report the gap, and do not claim closure.

Preserve Git history as the sole recovery surface for spent execution material.
Keep current product knowledge in maintained project content.

## Delete spent history, including shared records

When completion and recovery are resolved, delete only material the selected
work's identity and references identify as spent:

- its executable plan and owned proof, evidence, and assessment records, when
  a plan exists. A plan decision that retained the plan at execution completion
  still leaves that spent plan for wrap-up to delete.
- its canonical story section when it has one; delete the seed only when every
  remaining section is spent;
- the completed work's entry in **Taken** or **Backlog list**;
- related occurrences in the process log, and an issue or container that
  becomes empty afterward; and
- incoming links that exist solely to preserve that spent history.

After deleting spent files, remove directories named by that spent work when
they are empty, including nested untracked evidence directories. Verify those
directory paths are absent, not merely free of files.

Preserve unrelated human text, sibling stories, unrelated log issues and
occurrences, product and version identity, maintained tests or documents,
still-needed acceptance work, and any follow-up plan queued above. A direct
queue link to that plan is active navigation, not an incoming historical link
to delete with the completed predecessor. Preserve the active plan's correction
input and provenance. When its source locator points into the predecessor being
deleted, replace that locator with the before-cleanup commit and the predecessor's
repository-relative path rather than deleting the active context or recreating
spent history.
Resolve ambiguous attribution before deleting that portion; if a log issue or
link cannot be tied to the selected execution, leave it intact and say so.

Repair remaining Markdown links that this deletion breaks; do not leave a
live link to a removed path. Unrelated links stay unchanged.

Keep maintained content focused on current product knowledge, with the removed
work's identity and execution judgments recoverable through Git. An already-absent
artifact does not prove different work complete. Repeating wrap-up must not recreate
history, duplicate edits, or claim that missing files close a different story
or correction.

Inspect tracked and untracked files. Absence is the current snapshot, including
untracked paths. Recover removed files with
`git show <before-cleanup-commit>:<spent-path>` using the recorded revision.

## Commit final closure

After the spent-history deletion and link repair above, inspect the complete
closure diff and commit all owned closure changes with this project's ordinary
Git conventions. Preserve unrelated staged and unstaged changes. A successful
wrap-up requires a committed final snapshot; staged or unstaged deletion is not
completion. If ownership or commit completion is ambiguous or the commit fails,
retain the material and Git state, report the unresolved closure, and do not
claim success.

Finish both the before-cleanup and final-closure commits in caller-selected
direct-current-branch mode as well as worktree mode. In worktree mode, complete
these commits before any later integration or owned worktree/branch removal.
Direct-current-branch mode has no later integration or worktree-removal action.

## Integrate committed worktree closure

For worktree mode, save the committed final-closure tip and use the retained
planned-execution identity to resolve the exact integration-target branch and
the checkout that holds it. Leave the execution checkout before integration;
run target inspection and integration from the target checkout. If the target
checkout cannot be resolved uniquely, does not hold the recorded target branch,
or no longer matches the retained identity, preserve the execution branch and
worktree, report the mismatch, and stop this action. Direct-current-branch mode
skips integration.

Inspect the target checkout's branch, staged and unstaged changes, untracked
files, and any unfinished Git operation before merging. Preserve unrelated
target work. Do not stash, reset, overwrite, silently include it, or proceed
through a state whose safety or ownership is ambiguous. An unsafe target state
stops integration with both the execution branch and worktree intact.

First test whether the saved execution tip is already an ancestor of the target
branch. If so, treat integration as already done and do not merge again.
Otherwise merge that committed tip into the target branch using this project's
ordinary Git merge conventions. This integration is local: do not rebase, push
the target branch, delete a remote branch, or introduce CI waiting policy. A
merge conflict remains in the target checkout for explicit resolution; do not
abort, reset, remove the execution worktree, or delete either branch. Report
the conflicted paths and actual Git state, and do not claim wrap-up complete.

After a successful merge or an already-integrated result, verify that the saved
execution tip is an ancestor of the recorded target branch. A failed ancestry
check is unresolved integration: preserve the branch and worktree, report the
observed refs, and do not claim completion. Successful integration alone does
not remove those owned resources; keep them for the later safe cleanup action.

## Remove integrated worktree resources safely

After verified worktree-mode integration, use the retained execution identity
to inspect the current state of the exact execution-checkout path and local
execution branch. For each resource still present, confirm from Git's worktree
and ref state that its path, checked-out branch, and tip match the retained
identity, and that the path is not the originating or target checkout. Treat an
identified resource's absence as an already-completed cleanup step only when
the current Git state contains no conflicting resource at that identity. A
missing retained identity, changed present resource, or ambiguity stops cleanup;
do not infer ownership from a branch name or reconstruct it from history.

When the execution checkout is present, inspect its tracked changes, untracked
files, index, and unfinished Git-operation state. Recheck that the saved
execution tip is an ancestor of the recorded target branch whether resources
are present or already absent.

Dirty tracked or staged changes, untracked content, an unfinished operation, or
another ownership ambiguity leaves the execution worktree and branch intact.
Report the retained data and the already-completed integration separately. Do
not stash, reset, clean, or force removal.

From a surviving checkout outside the execution directory, remove the exact
owned clean worktree when it remains with ordinary non-force `git worktree
remove`, then delete the exact integrated local execution branch when it remains
with ordinary non-force `git branch -d`. Never remove the originating checkout
or a caller-owned checkout or branch. Never force either operation or delete a
remote branch.

Treat each cleanup operation independently. If worktree removal succeeds but
branch deletion fails, retain the branch and any other reported resource; do
not escalate to force or remove an unrelated worktree that now uses it. Report
integration as complete but cleanup as partial, without the completion marker.
On retry, use the retained saved identity and current Git worktree and ref state.
Recognize already-absent owned resources without recreating history, and stop on
an identity mismatch instead of selecting a similarly named branch or path.

Report successful closure only from a surviving checkout after verifying all of
these outcomes: the execution directory is absent, Git's worktree listing no
longer contains its path, the local execution branch is absent, the saved
execution tip remains an ancestor of the recorded target branch, and the target
contains the committed closure. Direct-current-branch mode has no cleanup
action and never treats its caller-owned checkout or branch as spent execution
resources.

## Report

Report the selected work and its canonical identity, completion judgment,
execution mode and retained checkout/branch/target identity when applicable,
before-cleanup and final-closure commits when deletion happened, assimilated
knowledge, deleted paths, the saved execution tip and local integration result
in worktree mode, worktree and local-branch cleanup results, preserved
unsupported material and resources, and any gap that blocked closure.
Distinguish a new merge from an already-integrated tip, integration success from
partial or refused cleanup, and name unsafe or conflicted state without implying
that the target was pushed or a remote branch was deleted.
Distinguish a completed wrap-up from a refusal that left files intact.

End a successful closure with:

`## STORY WRAP-UP COMPLETE`

Do not emit that marker when required context, unfinished work, unresolved
recovery or worktree-mode integration, or remaining required worktree cleanup
blocks closure.
