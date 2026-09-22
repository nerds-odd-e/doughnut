# Decide, publish, or discard the written result

Apply this reference once [prepare records in an owned
workspace](preparation-workspace.md) has established this preparation's owned
workspace and recorded its local checkout role and target selection in
that reference's [Select or reuse the
workspace](preparation-workspace.md#select-or-reuse-the-workspace). It covers
only what happens to an already-written seed, story, plan, or bug-triage
record. A bug-triage record is the authorized canonical story, executable
plan, or backlog change left after disposable reproduction changes are
removed. Unrelated exploration content is not that record. After applying
it, the calling skill returns to [Close or retain the
workspace](preparation-workspace.md#close-or-retain-the-workspace). Bug
fixing supplies the explicit keep, leave-unpublished, or discard instruction
and does not choose the backlog home here.

## Decide what happens to the written result

Leave a written seed, story, plan, or bug-triage record in the owned
workspace for the developer's review by default. Do not commit it to a
shared or host checkout or publish it merely because the write finished.
Treat a "quick" or already-decided edit the same way — it is not
authorization to skip this step.
Three explicit developer decisions change that default:

- **An explicit instruction to keep this preparation's retained result**
  authorizes committing it in the owned workspace, reconciling it with the
  fetched authorized remote target, and publishing it from that workspace
  through
  [Keep and publish the retained result](#keep-and-publish-the-retained-result)
  below. Only an explicit instruction counts as keep: continuing discussion,
  pausing for more review, or silence is never a keep decision.
- **An explicit instruction to leave the result unpublished** is preserved and
  overrides any default publication. The record stays in the owned workspace
  exactly as the developer left it; this reference performs no additional
  commit or push.
- **An explicit instruction to discard an identified draft** removes that
  specific session-owned content, under
  [Discard an identified draft](#discard-an-identified-draft) below. The same
  rule that governs keep governs discard: only an explicit instruction that
  identifies what to discard counts. Continuing discussion, pausing, going
  quiet, or the session simply ending is never a discard decision, exactly as
  none of those is ever a keep decision.

Absent an explicit instruction, continue leaving the draft isolated: no
commit, integration, publication, or removal happens under this reference.
The draft stays recoverable in the owned workspace, and the result states
that pending disposition.

## Inspect an advanced integration target without deciding

A resumed (or still-active) preparation session may fetch or inspect the
current state of the recorded integration checkout and the authorized remote
target in
[Select or reuse the
workspace](preparation-workspace.md#select-or-reuse-the-workspace) — to see
what changed while paused, or to inform a discussion with the developer —
without that inspection itself becoming an integration or a keep decision.
Looking is not deciding: only an explicit keep instruction, validated under
[Validate a keep instruction before acting](#validate-a-keep-instruction-before-acting)
immediately below, enters
[Keep and publish the retained result](#keep-and-publish-the-retained-result).
"Let me check what's on the integration target now" is inspection for
discussion, not the start of that sequence — do not treat it as one.

## Validate a keep instruction before acting

Before committing or publishing anything, confirm — or require the calling
skill (one of the four preparation skills [prepare records in an owned
workspace](preparation-workspace.md) applies to, or bug fixing for a
bug-triage record, per its disposition/report step) to have already
confirmed — that the keep instruction:

- names this session's own retained seed, story, plan, or bug-triage
  record, not implementation, unrelated exploration content, disposable
  reproduction changes, or another session's work;
  and
- has a known, unambiguous local checkout role and a separate target
  selection, per the record in
  [Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace).
  Publish onto the authorized remote target. Do not treat the integration
  checkout path as the publication destination.

A keep instruction that does not clearly identify its own retained result, or
whose destination is unknown or ambiguous, stops before any commit or
publication; report the exact gap. This is a real stop, not permission to
guess a destination or assume "the usual place."

## Keep and publish the retained result

Apply this sequence only after a validated explicit keep instruction.

1. **Commit the retained result.** If the developer's retained seed, story,
   plan, or bug-triage record is not already committed in the owned
   workspace, commit exactly the files the keep instruction names there —
   nothing else. This produces the owned workspace's unpublished suffix: one
   or more commits on its branch, since its recorded starting revision, not
   yet on the authorized remote target. Do not commit implementation,
   unrelated exploration content, disposable reproduction changes, or another
   session's changes.
2. **Use the recorded target.** Reuse the local checkout role and the target
   selection recorded in
   [Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace).
   The integration checkout path is the default checkout for the separate
   maintenance outcome. The authorized remote target is the publication
   destination. The checkout is not a stage the candidate must pass through.
   If either record is missing, contradictory, or ambiguous, stop before any
   commit or publication and report the exact gap.
3. **Publish from the owned workspace.** Apply
   [publish the candidate](../../dough-execute-plan/references/publish-the-candidate.md)
   from this preparation workspace. Do not invent a second publication
   sequence. For this caller the suffix is step 1's commit(s). The supplied
   validation is that those commits are exactly the named retained record and
   nothing else. No CI observer is bound, so nothing is registered. After the
   remote accepts the candidate, record the separate
   [maintenance outcome](../../dough-execute-plan/references/maintain-default-checkout.md#independent-maintenance-outcome)
   by that owner's inspection. Do not refresh the default checkout in this
   sequence. After acceptance, attempt
   [Refresh eligibility](../../dough-execute-plan/references/maintain-default-checkout.md#refresh-eligibility).
   This publication does not acquire default-checkout access, so without a
   declared owner that is this caller the attempt is deferred and does not
   fast-forward. Report that result separately from acceptance. An unresolved
   rejection stops as the publisher describes; report it and do not loop.

After a successful push, the retained result is on the authorized remote
target, including other writers' reconciled commits. Report that acceptance
and the maintenance outcome separately. A deferred checkout is not a failed
publication. When the developer gave an explicit no-push instruction, this
section is never entered: the record stays in the owned workspace,
recoverable and unpublished, and the remote is unchanged.

[Close or retain the workspace](preparation-workspace.md#close-or-retain-the-workspace)
treats remote acceptance of that candidate — the fetched authorized target
contains the SHA — as the confirmed keep-and-publish. Default-checkout
convergence is not required. An attempt interrupted before that acceptance is
not confirmed; see
[Resume an interrupted keep-and-publish](#resume-an-interrupted-keep-and-publish).

## Resume an interrupted keep-and-publish

A resumed session may find publication interrupted before the remote accepted
the candidate, or after a push whose response was lost. This is recovery of a
publication already under way, distinct from
[Pause and resume a preparation session](preparation-workspace.md#pause-and-resume-a-preparation-session)
before any keep decision. Reuse the same workspace and recorded target. Do
not create a replacement.

Apply [publish the candidate's resume](../../dough-execute-plan/references/publish-the-candidate.md#resume-an-interrupted-publication)
from real Git and remote refs, and continue only the first unfinished
obligation. The suffix is step 1's commit(s) in the owned workspace. The
candidate is the SHA retained immediately before the push, which is the
rewritten SHA when a rewrite was retained. When that resume still has to
publish, the previously published base is the last SHA this session recorded
as published or, for a first keep, the workspace's recorded starting
revision. Preparation binds no observer, so
the "Missing registration" row does not apply. Resume classification does
not refresh the default checkout and does not remove this workspace. Record
the separate maintenance inspection. Once that classification has recorded
acceptance, attempt
[Refresh eligibility](../../dough-execute-plan/references/maintain-default-checkout.md#refresh-eligibility)
as in step 3, still without acquiring default-checkout access. Cleanup stays in
[Close or retain the workspace](preparation-workspace.md#close-or-retain-the-workspace)
and runs only after remote acceptance.

Never duplicate the commit or the push, and never create a replacement
workspace.

**Conflicting scope change.** If resuming reveals that the human's story or
plan scope changed in a way that conflicts with what was about to be
published — not merely that the remote advanced, which is ordinary
reconciliation — preserve both the retained draft and the remote target, and
name the exact human decision needed. This is
[Stop for human judgment](../../dough-execute-plan/references/execution-decisions.md#stop-for-human-judgment).

This publication does not acquire default-checkout access. A step that must
mutate that checkout uses
[establish access before local mutation](../../dough-execute-plan/references/maintain-default-checkout.md#establish-access-before-local-mutation);
nothing here releases that access on a timeout. Do not turn an unavailable
mutation of that checkout into a failed publication.

## What keep does not do

Keep authorizes only committing and publishing this session's own retained
seed, story, plan, or bug-triage record. It does not:

- move a backlog entry to **Taken** or perform any part of
  [take queued work](../../dough-execute-plan/SKILL.md#take-queued-work);
- start implementation of the kept record;
- start, arm, or register with a CI/execution observer — that remains
  execution's own concern under
  [Own one observer](../../dough-execute-plan/references/ci-monitor.md#own-one-observer),
  not preparation's;
- establish an execution identity, mode, or claim.

This keeps "keep" a narrower operation than an execute-plan delivery: only the
retained planning record reaches the authorized remote target. Planning-only
execution limits are preserved — this capability never begins implementation
or takes the story, whatever the kept record describes.

## Discard an identified draft

Apply this section only after a validated explicit discard instruction, per
[Decide what happens to the written result](#decide-what-happens-to-the-written-result)
above: one that identifies this session's own retained seed, story, plan, or
bug-triage content, not implementation, unrelated exploration content,
disposable reproduction changes, or another session's work. This is the same
identification requirement
[Validate a keep instruction before acting](#validate-a-keep-instruction-before-acting)
already applies to a keep instruction's own retained-record check. Discard
needs no destination check: it never publishes anywhere, so that section's
local checkout role and target selection requirement does not apply here.

Discard removes only the specific session-owned draft or edit the developer
identified — the seed, story, plan, or bug-triage content this session wrote,
whether it is still uncommitted or already committed-but-unpublished in the
owned workspace. It does not remove:

- **the owned workspace itself.** A reused or host-owned workspace may hold
  other in-progress work — another story, plan, or session's own edits —
  that must survive a discard aimed only at this session's draft. Whether the
  workspace later becomes safe to remove is a separate, independent decision
  governed by [Close or retain the
  workspace](preparation-workspace.md#close-or-retain-the-workspace) (its
  session-created, clean, unambiguous test); discard here is about removing
  the identified draft content, not necessarily the workspace that held it;
- **unrelated edits** already present in that workspace before or alongside
  this session's write; or
- **another session's work**, even one sharing the same workspace.

If the identified content cannot be unambiguously isolated from other work in
the workspace — for example, an uncommitted mix of this session's edits and
another session's uncommitted edits touching the same file — stop and report
the exact conflict rather than guessing which content belongs to which
session. A discard that stops here is not a confirmed disposition: [Close or
retain the workspace](preparation-workspace.md#close-or-retain-the-workspace)
applies cleanup only after discard actually removes the identified content,
never merely because an attempt was made.
