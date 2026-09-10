---
name: dough-story-wrap-up
description: >-
  Closes one completed story after plan execution and retrospective. Assimilates
  lasting product knowledge, removes that story's spent plan and history so Git
  can recover it, and reports truthfully when required inputs are missing or
  unfinished. Use to wrap up a story, close a completed story, or delete spent
  plan and execution history after retrospective.
---

# Story wrap-up

Close one selected story after its plan execution and retrospective are
complete. Leave this project with maintained product knowledge and no spent
story or plan history in the current snapshot. Do not invent findings, records,
or a requirement for another conversation.

## Resolve this project's context

Require a selected story. Resolve from this project, not this skill's location:

- repository root and Git working tree;
- canonical seed location, story identity, heading or stable-anchor conventions;
- executable-plan location, status vocabulary, and the selected story's plan;
- how this project records that a retrospective finished, including an empty result;
- Git commit conventions used to preserve a recoverable revision;
- the product backlog path when a queue entry or finished-history entry
  points at the selected story; and
- shared records that name the selected story: its seed, process log
  (`DearDough.md` unless this project sets another canonical location), incoming
  links, and assessment or recognition records.

An empty retrospective result is valid input. Do not invent a retrospective
artifact or require another review. Do not invent a plan path, completion rule,
seed location, or Git convention.

If context needed for a closure decision is missing, name the gap, leave
affected material intact, and do not claim closure.

## Confirm execution and retrospective are complete

Judge the selected plan from its latest state and execution evidence. Any
planned or in-progress slice is unfinished. A missing retrospective completion
is unfinished even when the plan is done. Empty retrospective output still
counts as complete when this project or the user records that the review
finished with nothing to act on.

If execution or retrospective is unfinished, leave the story, plan, and
related files intact. Report what remains and stop. Do not delete unfinished
work to manufacture a wrap-up.

## Preserve Git recovery

Before deleting the only copy of spent material, make that revision
recoverable with this project's ordinary Git conventions. If spent files are
uncommitted, commit them first using those conventions, then record that
revision as the before-cleanup commit. If commit conventions, ownership, or
recovery cannot be resolved, leave the material intact and report the gap.

Do not rewrite Git history. Do not create an archive, tombstone, finished-list
entry, or replacement summary for later readers.

## Assimilate lasting knowledge

Move lasting behavior and design into this project's maintained code, tests,
documentation, or current Accepted decisions. Describe the current product
without execution narration, impact chronology, story or plan identity, or
retrospective judgments. Preserve existing product tests and documents that
already state current behavior. Do not invent product knowledge.

## Queue an existing follow-up plan first

When the retrospective supplies an existing follow-up plan, put that plan's
active story first in the queue. Do not replan or execute it. Preserve the
plan contents needed for later execution.

Resolve one canonical story reference:

- If a canonical follow-up story already exists, link the existing plan there
  and remove historical references to the closed execution.
- If none exists, create one home only when the supplied outcome names a
  beneficiary and an evaluable outcome. Use this project's seed and anchor
  conventions. Do not invent either field.
- If beneficiary or outcome is missing, do not guess the addition. Keep the
  follow-up plan and any other needed active-work context, report the gap, and
  continue supported wrap-up of the closed story.

Preserve unrelated queue order after that first item, and preserve
near-future direction. Repeating wrap-up must not duplicate the follow-up
story or queue entry.

## Apply product-review decisions

When retrospective product advice or additional human input is present, apply
only authorized compatible backlog and canonical-story changes. Follow
[dough-product-backlog](../dough-product-backlog/SKILL.md) for queue and
story conventions. Explicit human input wins over advice.

Supported changes: relevant reorder, queue membership, understood new-story
addition, and canonical-detail edits. A new queued story needs a named
beneficiary and an evaluable outcome. Keep existing follow-up work first
unless a later explicit human instruction changes that priority. Preserve
unrelated content and near-future direction.

A skipped or empty product review, or absent extra human input, introduces
no mandatory question. Leave unresolved necessary context with active work
and report the choice; do not invent scope, launch discovery, or start
another review.

## Leave no extra wrap-up ceremony

Do not launch discovery or another review. Empty retrospective product
advice is valid and changes nothing beyond the supported closure and
follow-up actions above.

## Delete spent history, including shared records

When completion and recovery are resolved, delete only material the selected
story's references identify as spent:

- its executable plan and owned proof, evidence, and assessment records;
- its canonical story section; delete the seed only when every remaining
  section is spent;
- queue entries and finished-history / recently-done entries for that
  completed story;
- related occurrences in the process log, and an issue or container that
  becomes empty afterward; and
- incoming links that exist solely to preserve that spent history.

Preserve unrelated human text, sibling stories, unrelated log issues and
occurrences, product and version identity, maintained tests or documents,
and any follow-up plan queued above. Do not treat that follow-up plan as
spent history of the closed story.
Resolve ambiguous attribution before deleting that portion; if a log issue or
link cannot be tied to the selected execution, leave it intact and say so.

Repair remaining Markdown links that this deletion breaks; do not leave a
live link to a removed path. Unrelated links stay unchanged.

Do not replace deleted history with a summary, archive, tombstone, recently-done
ledger, or judgment for later readers. Assimilate current product knowledge
instead of preserving the removed story's identity. An already-absent artifact
does not prove a different story complete. Repeating wrap-up must not
recreate history, duplicate edits, or claim that missing files close a
different story.

Inspect tracked and untracked files. Absence is the current snapshot, including
untracked paths. Recover removed files with
`git show <before-cleanup-commit>:<spent-path>` using the recorded revision.

## Report

Report the selected story, completion judgment, before-cleanup commit when
deletion happened, assimilated knowledge, deleted paths, preserved
unsupported material, and any gap that blocked closure. Distinguish a completed
wrap-up from a refusal that left files intact.

End a successful closure with:

`## STORY WRAP-UP COMPLETE`

Do not emit that marker when required context, unfinished work, or
unresolved recovery blocked deletion.
