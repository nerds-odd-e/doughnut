# Prepare records in an owned workspace

Apply this rule in [dough-story-decomposition](../../dough-story-decomposition/SKILL.md),
[dough-story-refinement](../SKILL.md) itself,
[dough-slice-planning](../../dough-slice-planning/SKILL.md), and
[dough-slice-plan-refinement](../../dough-slice-plan-refinement/SKILL.md)
before any of them writes a seed, story, or plan record, including a small,
already-decided correction. Reading, discussing, answering questions, or
reviewing an existing seed or plan needs no workspace at all.

Once a write is done, [decide what happens to the written
result](preparation-disposition.md) is the only disposition for that record.
Decomposition, refinement, planning, and plan refinement all use it for keep,
leaving the result unpublished, or discarding it. None of them publishes by
another path. Then close or retain the workspace below.

## Determine whether a write needs a workspace

Only the first write to a seed, story, or plan record in this preparation
session requires an owned workspace. Continue ordinary discussion, inspection,
and question-answering without one. Once a write is about to happen, establish
or confirm the workspace immediately before making it.

## Select or reuse the workspace

Apply [own a temporary exploration workspace](../../dough-manual-testing/references/exploration-workspace.md)
"Select the checkout", "Record local checkout role and target selection",
and "Use and resume it" as this preparation's Git lifecycle; do not duplicate
its recipe here. First check whether the current story, active plan, session,
or a host-supplied workspace already owns a suitable checkout for this
preparation. Use it, and do not create a nested or per-invocation workspace
merely because a different one of the four skills above is now writing. When
no suitable owned workspace exists, start one using that reference's create
step, from a suitable existing host workspace when one is available, otherwise
from the verified current revision of the checkout this preparation was
invoked from.

Verify a candidate against that reference before writing into it. The suitable
owner is the current story, plan, session, or host. An unverifiable or
ambiguous match is a missing workspace.

Resolve this project's own conventions for the write — seed directory and
ID/filename rules, plan root and layout, required metadata, and installed
skill guidance — from the intended, owned checkout, not from wherever the
invocation started.

Record local checkout role and target selection through that reference, using
the actual established paths. The owned workspace path is the preparation
workspace. The integration checkout path is the checkout this preparation was
invoked from, or a reused host workspace's already-recorded integration
checkout — the project's established checkout for ordinary work, never the
owned preparation workspace itself. Target selection is the authorized remote
target, recorded separately from that path. A later keep decision publishes
onto this recorded target; see
[Decide what happens to the written result](preparation-disposition.md#decide-what-happens-to-the-written-result).
Preparation's continuation after this selection is the record write and that
disposition. It does not apply execution mode or project-command readiness.

## Continue related preparation

Reuse the same workspace across decomposition, refinement, planning, and
plan refinement while they continue the same story, plan, or session,
including across discussion, a pause for a developer's answer, and successive
invocations of any of the four skills above. Do not start a second workspace
for continuation work that could reuse the first.

## Stop only the write that needs it

When ownership cannot be established or stays ambiguous after the checks
above, stop only the pending record write and report the exact gap: what was
checked and what remains unresolved. Reading, discussing, or continuing to
answer questions does not require resolving it first. Do not invent or guess a
workspace to avoid reporting the gap.

## Pause and resume a preparation session

Pausing for a developer's answer, ending a conversation turn, or any other
interruption before a keep or discard decision leaves the draft exactly as it
is in its owned workspace: nothing is committed, published, or discarded
merely by pausing. See
[Decide what happens to the written result](preparation-disposition.md#decide-what-happens-to-the-written-result)
for what only an explicit instruction can trigger.

On resume, before continuing to write into the workspace, apply [own a
temporary exploration workspace](../../dough-manual-testing/references/exploration-workspace.md)
"Use and resume it" — the same verification
[Select or reuse the workspace](#select-or-reuse-the-workspace) above already
requires before any write. Resuming after a pause is one more trigger for it,
not a different check.

If that verification finds partial prior setup, an identity mismatch, or an
otherwise ambiguous match, apply
[Stop only the write that needs it](#stop-only-the-write-that-needs-it)
above: stop only the resumed write and preserve every resource exactly as
found. Do not silently replace the workspace, create a second one alongside
it, or guess which candidate is the right one — the same rule that already
governs an unresolved first-time workspace selection.

This resume verification relies only on the local checkout role already
recorded when the workspace was selected or created — the actual paths and
story, plan, session, or host ownership. Do not add a session registry, log,
or other persistent index to track preparation sessions across time; resume
continues to depend purely on verifying that recorded role against actual
Git state.

## Tiny corrections are included; the Taken transition is not

A small or already-decided drafting correction — for example fixing a typo or
a misordered example while refining a seed or plan — still goes through this
workspace rule. It gains no direct-edit exception on a shared or host checkout
merely because it is small or already decided.

This rule does not apply to [take queued work](../../dough-execute-plan/SKILL.md#take-queued-work)'s
own commit moving a backlog entry to **Taken**. That execution-startup
transition keeps its existing location, timing, and authority; none of the
four preparation skills above route it through this reference or change its
behavior.

## Leave the shared checkout free for other writers

Preparation under this reference never holds an integration turn on a shared
or host checkout, unlike delivery. A second writer may fetch, integrate, and
push their own prepared or published increment onto the shared integration
branch at any time, including while this preparation is mid-question. Nothing
in this reference locks, blocks, or reserves that checkout.

When the write is finished, apply [decide what happens to the written
result](preparation-disposition.md#decide-what-happens-to-the-written-result)
before [closing or retaining the workspace](#close-or-retain-the-workspace)
below.

## Close or retain the workspace

Cleanup runs only after one of this session's three decisions under [Decide
what happens to the written result](preparation-disposition.md#decide-what-happens-to-the-written-result)
is actually **confirmed**, never merely attempted or merely because the
session is ending:

- a **keep-and-publish** whose candidate the fetched authorized remote
  contains, per
  [Keep and publish the retained result](preparation-disposition.md#keep-and-publish-the-retained-result).
  The default checkout need not match that SHA. A deferred maintenance result
  does not withhold this confirmation;
- an explicit **discard** that actually removed the identified draft under
  [Discard an identified draft](preparation-disposition.md#discard-an-identified-draft),
  not one that stopped because the content could not be unambiguously
  isolated; or
- an explicit **no-publish** instruction under [Decide what happens to the
  written result](preparation-disposition.md#decide-what-happens-to-the-written-result)
  given together with the developer's explicit confirmation that this
  preparation session itself is finished, not merely paused for later
  resumption. An ordinary no-publish instruction on its own leaves the
  session resumable under [Pause and resume a preparation
  session](#pause-and-resume-a-preparation-session) above and confirms no
  disposition for cleanup purposes.

Failed or unconfirmed publication never triggers cleanup. A keep-and-publish
interrupted before the authorized remote contains the candidate — see [Resume
an interrupted keep-and-publish](preparation-disposition.md#resume-an-interrupted-keep-and-publish) —
is not a confirmed disposition merely because the session is ending. Treat it
as still unresolved and preserve every resource exactly as found under
[preserve pending local work](../../dough-execute-plan/references/maintain-default-checkout.md#preserve-pending-local-work),
including a pending human edit on the default checkout. Pausing, going quiet,
or ending the conversation before a decision is confirmed is never itself a
trigger, exactly as it is never itself a keep or discard decision.

Once a confirmed disposition applies, apply [own a temporary exploration
workspace](../../dough-manual-testing/references/exploration-workspace.md)
"Close or retain it": remove only a clean, unambiguous, session-created
workspace; retain and report a reused, host-owned, or otherwise unsafe one
instead of forcing its removal. Delete a removed session-created branch only
when its tip is contained in the fetched authorized remote target. That
containment is what makes deletion safe; the default checkout being behind
that target does not make the branch unmerged, and it does not authorize a
force delete. A clean working directory is a necessary
check there, not by itself proof of ownership: a workspace can be clean
because a keep-and-publish just succeeded in it while still being the same
reused or host-owned workspace [Select or reuse the
workspace](#select-or-reuse-the-workspace) above recorded — one that may
still hold other in-progress work belonging to a different story, plan, or
session, the same "other in-progress work ... that must survive" concept
[Discard an identified draft](preparation-disposition.md#discard-an-identified-draft)
already preserves for discard. Removal depends on the session-created-versus-
reused/host-owned identity that step already recorded, not on cleanliness
alone; do not infer ownership from a clean directory, an empty `git status`,
or the absence of other visible edits.

When cleanup is unsafe, report the exact workspace path and branch, and why
it is being retained — reused, host-owned, holds other in-progress work, or
otherwise unsafe or ambiguous — the same report [own a temporary exploration
workspace](../../dough-manual-testing/references/exploration-workspace.md)
"Close or retain it" already requires. State this alongside, not instead of,
any disposition report already owed to the developer.

Preparation work that never reached a confirmed keep-and-publish — still
isolated in an owned workspace, discarded, or left unpublished by a
confirmed no-publish/session-finished instruction — has no presence in any
progress view this project derives only from published remote state (an
origin-only dashboard, where one exists): that view reflects what reached
the authorized remote target, not what a preparation session still holds
locally, exactly as an unpublished Taken claim stays invisible to it. Report
that gap explicitly rather than letting local absence from such a view read
as lost or completed work.

This composes with, and does not replace or weaken, [own a temporary
exploration workspace](../../dough-manual-testing/references/exploration-workspace.md)'s
own close/retain criteria; manual testing and bug fixing keep relying on
that reference's behavior unchanged.
