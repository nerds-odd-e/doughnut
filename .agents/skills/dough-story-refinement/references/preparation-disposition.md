# Decide, publish, or discard the written result

Apply this reference once [prepare records in an owned
workspace](preparation-workspace.md) has established this preparation's owned
workspace and recorded its integration checkout, branch, and authorized
remote target in that reference's [Select or reuse the
workspace](preparation-workspace.md#select-or-reuse-the-workspace). It covers
only what happens to an already-written seed, story, or plan record. After
applying it, the calling skill returns to [Close or retain the
workspace](preparation-workspace.md#close-or-retain-the-workspace).

## Decide what happens to the written result

Leave a written seed, story, or plan record in the owned workspace for the
developer's review by default. Do not commit it to a shared or host checkout,
integrate, or publish it merely because the write finished. Treat a "quick" or
already-decided edit the same way — it is not authorization to skip this step.
Three explicit developer decisions change that default:

- **An explicit instruction to keep this preparation's retained result**
  authorizes committing it in the owned workspace, reconciling it with the
  current integration target, and publishing it through
  [Keep and publish the retained result](#keep-and-publish-the-retained-result)
  below. Only an explicit instruction counts as keep: continuing discussion,
  pausing for more review, or silence is never a keep decision.
- **An explicit instruction to leave the result unpublished** is preserved and
  overrides any default publication. The record stays in the owned workspace
  exactly as the developer left it; this reference performs no additional
  commit, integration, or push.
- **An explicit instruction to discard an identified draft** removes that
  specific session-owned content, under
  [Discard an identified draft](#discard-an-identified-draft) below. The same
  rule that governs keep governs discard: only an explicit instruction that
  identifies what to discard counts. Continuing discussion, pausing, going
  quiet, or the session simply ending is never a discard decision, exactly as
  none of those is ever a keep decision.

Absent an explicit instruction, continue leaving the draft isolated: no
commit, integration, publication, or removal happens under this reference.

## Inspect an advanced integration target without deciding

A resumed (or still-active) preparation session may fetch or inspect the
current state of the integration checkout and target recorded in
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
workspace](preparation-workspace.md) applies to, per its disposition/report
step) to have already confirmed — that the keep instruction:

- names this preparation session's own retained seed, story, or plan
  record, not implementation, unrelated changes, or another session's work;
  and
- has a known, unambiguous integration checkout, branch, and authorized
  remote target to publish onto, per the identity recorded in
  [Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace)
  above.

A keep instruction that does not clearly identify its own retained result, or
whose destination is unknown or ambiguous, stops before any commit or
publication; report the exact gap. This is a real stop, not permission to
guess a destination or assume "the usual place."

## Keep and publish the retained result

Apply this sequence only after a validated explicit keep instruction.

1. **Commit the retained result.** If the developer's retained seed, story,
   or plan record is not already committed in the owned workspace, commit
   exactly the files the keep instruction names there — nothing else. This
   produces the owned workspace's own unpublished suffix: one or more commits
   on its branch, since its recorded starting revision, not yet on the
   authorized remote target. Do not commit implementation, unrelated edits,
   or another session's changes.
2. **Use the recorded integration checkout and target.** Reuse the
   integration checkout, branch, and authorized remote target recorded in
   [Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace);
   do not invent a different or preparation-specific destination. If that
   identity is missing, contradictory, or ambiguous, stop before any commit or
   publication and report the exact gap — a missing destination or unknown
   integration ownership is a real stop, not permission to guess.
3. **Publish through the existing procedure.** Apply
   [publish the candidate](../../dough-execute-plan/references/publish-the-candidate.md)
   "Preconditions", "Publish the candidate", and, if a push is rejected,
   "Recover a rejected push" — do not invent a second publication sequence or
   a preparation-specific merge/rebase policy. For this caller:
   - the owned unpublished suffix is the commit(s) from step 1;
   - the integration checkout and authorized remote target are the ones
     resolved in step 2;
   - "Preconditions"' exclusive integration turn applies unchanged; this
     reference adds no second coordination mechanism;
   - validating the candidate (step 4 of "Publish the candidate") means
     reconfirming that the suffix's commits are exactly the retained record
     the keep instruction named and nothing else — there is no **Taken**
     commit to confirm and no accepted implementation proof to reuse for a
     planning record. An unchanged-trunk fast-forward needs no
     re-validation; a rebase onto advanced trunk invalidates only content the
     combined changes actually touch;
   - no CI/execution observer is bound to a preparation workspace, so no
     registration happens — see
     [What keep does not do](#what-keep-does-not-do) below;
   - an unresolved rejection, unrelated unpublished local commits, or
     ambiguous target ownership stops exactly as that reference already
     describes; report it and do not loop.

After a successful push, the retained result is reachable at the authorized
remote target, with any writer's intervening work still present in its
history. When the developer instead gave an explicit no-push instruction (see
[Decide what happens to the written result](#decide-what-happens-to-the-written-result)),
this section is never entered: the retained result stays committed (or
uncommitted, as the developer left it) in the owned workspace, recoverable and
unpublished, and the authorized remote target is left unchanged.

Step 6's own agreement check above — not merely reaching this sequence, and
not merely local integration without a confirmed push — is what [Close or
retain the workspace](preparation-workspace.md#close-or-retain-the-workspace)
treats as a confirmed keep-and-publish before any cleanup. An attempt
interrupted before that agreement is reached is not one, however the session
ends; see [Resume an interrupted keep-and-publish](#resume-an-interrupted-keep-and-publish)
below.

## Resume an interrupted keep-and-publish

A resumed preparation session may find [Keep and publish the retained
result](#keep-and-publish-the-retained-result) interrupted — after local
integration but before push, or after a push whose confirmation response was
lost. This is recovery from a publication already under way, distinct from
[Pause and resume a preparation session](preparation-workspace.md#pause-and-resume-a-preparation-session)'s
case of a session paused before any keep decision was made at all; do not
conflate the two. Reuse the same workspace, integration checkout, and target
[Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace)
already recorded — never a replacement workspace or checkout to "start over."

Apply [publish the candidate's resume](../../dough-execute-plan/references/publish-the-candidate.md#resume-an-interrupted-publication)
to classify the actual state from real Git and remote refs — never from a
stored status field or any new preparation-specific tracking mechanism — and
continue only the first unfinished obligation. For this caller:

- the owned unpublished suffix is step 1's commit(s), in the owned workspace;
- the previously published base is the last SHA this preparation session
  itself recorded as published, when it has one. A first-time keep for this
  session has none: the suffix's parent is simply the workspace's own
  recorded starting revision from
  [Select or reuse the workspace](preparation-workspace.md#select-or-reuse-the-workspace),
  not a previously published revision. A resumed or retried publication that
  already recorded a published SHA — including one this classification itself
  discovers, below — treats that SHA as the previously published base for
  anything still unpublished;
- the integration checkout and target are step 2's recorded identity,
  unchanged by resuming;
- candidate identity after a rebase is exactly the retained rewritten SHA
  from [Publish the candidate](../../dough-execute-plan/references/publish-the-candidate.md#publish-the-candidate)
  step 3; classify against that current SHA, never a stale pre-rebase one;
- the shared table's "Missing registration" row never applies here:
  preparation binds no CI/execution observer to a publication, per
  [What keep does not do](#what-keep-does-not-do) below, so there is nothing
  to register.

Never duplicate the commit or the push, and never create a replacement
workspace or worktree.

**Conflicting scope change.** If resuming reveals that the human's story or
plan scope changed in a way that conflicts with what was about to be
published — not merely that origin advanced, which is ordinary reconciliation
already covered by
[Publish the candidate](../../dough-execute-plan/references/publish-the-candidate.md#publish-the-candidate) —
preserve both the preparation's own retained draft and whatever is now on the
integration target, and name the exact human decision needed rather than
guessing which side wins. This is the same stop already used for any
unresolved value, design, or scope decision:
[Stop for human judgment](../../dough-execute-plan/references/execution-decisions.md#stop-for-human-judgment).

**Blocked-turn handoff, never timeout.** If
[Preconditions](../../dough-execute-plan/references/publish-the-candidate.md#preconditions)'
exclusive integration turn cannot be acquired or completed, that is an
explicit recovery/handoff requirement, exactly like any other interrupted
publication. Nothing here introduces, or should be read to imply, a
timeout-based automatic release of that turn.

## What keep does not do

Keep authorizes only committing and publishing this preparation's own
retained seed, story, or plan record. It does not:

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
above: one that identifies this preparation session's own retained seed,
story, or plan content, not implementation, unrelated changes, or another
session's work. This is the same identification requirement
[Validate a keep instruction before acting](#validate-a-keep-instruction-before-acting)
already applies to a keep instruction's own retained-record check. Discard
needs no destination check: it never publishes anywhere, so that section's
integration-checkout/target requirement does not apply here.

Discard removes only the specific session-owned draft or edit the developer
identified — the seed, story, or plan content this session wrote, whether it
is still uncommitted or already committed-but-unpublished in the owned
workspace. It does not remove:

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
