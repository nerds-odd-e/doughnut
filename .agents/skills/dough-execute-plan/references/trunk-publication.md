# Publish onto shared trunk

Use this rule for Trunk Mode work that must reach the authorized remote trunk
without publishing the execution branch. Startup uses it to publish a queue
claim. Slice delivery and story wrap-up use the same Git steps for each
verified increment, including wrap-up's before-cleanup and final-closure
commits. Do not invent a second publication sequence.

This rule does not create execution authority, wait for CI, or push the
execution branch. Observation is armed from the execution checkout against the
authorized target branch. A queue claim may be published before that workspace
exists; retain its published SHA and register it after the observer is armed.

## Publish a queue claim

After [Take queued work](../SKILL.md#take-queued-work) commits the local Taken
claim on the resolved integration branch, publish it with the steps below, then
create the local execution branch/worktree from the published revision. Do not
start implementation from an unpublished claim. An unavailable destination or
failed publication leaves the exact remaining state and does not authorize
starting unclaimed queued work.

## Publish a verified increment

After wrap-up proof, refactor, format, and commit succeed, publish the owned
unpublished increment with the same steps below. Keep the same execution
worktree. Planned, quick, and contextual Trunk Mode work share this rule.

The owned unpublished suffix is the execution commit or consecutive commits
not yet on the authorized remote trunk. The previously published base is
that suffix's parent: the last recorded published revision when this
execution has one. When remote trunk is still that parent, the increment is
already based on current trunk: do not rewrite it; fast-forward the local
target to that commit and publish it.

When fetch shows a newer remote trunk than that parent, publish through the
candidate steps so only that suffix is rebased onto current trunk.
Previously published revisions remain ancestors; never rewrite them or
another writer's commits.

## Preconditions

Apply exclusive-turn and target cleanliness checks only when this publication
must mutate the shared integration checkout. They do not gate execution-checkout
commits, proof, or formatting. Do not invent a merge queue, lock, or extra claim.

Resolve source, mode, target branch, authorized remote, and the owned
unpublished suffix before mutating the shared integration checkout. For a
queue claim, that suffix is the Taken commit only. For a verified increment,
use the suffix defined in the increment rule.

Acquire an exclusive integration turn through available coordinator context
before changing the shared integration checkout. A clean working directory is
not exclusivity. Git lock files are not a transaction lock. Coordinate with a
declared owner, or stop. Unclear ownership uses
[human judgment](execution-decisions.md#stop-for-human-judgment). Apply the
same ownership stop as [delivery staging](wrap-up.md#deliver-the-change) and
[resume](../SKILL.md#continue-or-recover-at-an-execution-boundary).

A stop here mutates nothing on the target. Report the inspectable reason:
the declared competing writer, and/or dirty, ambiguous, or unrelated-commit
target state. Preserve unrelated staged and unstaged target content; do not
stash, reset, unstage, or revert it. Leave the owned unpublished suffix SHA
recoverable on the execution branch, or the local Taken commit when no
workspace exists yet. Do not register publication or continue as delivered.
After an exclusive turn, inspect the target; dirty or ambiguous state, known
unrelated local commits, or unknown ownership still stop as above. Do not
silently publish those commits. Ordinary candidate publication proceeds only
after that turn and a usable target.

## Publish the candidate

Rebase only owned unpublished execution work; never rewrite published trunk
history or another writer's commits. No force push. An increment keeps the
same execution worktree; a claim may have none yet.

1. Fetch the authorized remote for the target branch.
2. Reconcile from the fetched target. Current trunk is the fetched remote
   target, not a stale local target tip. A local target that is only behind
   that remote is not a stop. If the local target has unpublished commits
   that are not this execution's owned suffix, or ownership is ambiguous,
   stop and preserve that state.
3. Rebase only the owned unpublished suffix onto the current remote trunk.
   For a claim, that is the Taken commit. For an increment, that is the
   unpublished execution suffix. When that suffix is already based on
   current trunk, leave its commits unchanged. When trunk advanced, rewrite
   only that suffix onto it and replace the unpublished candidate SHA with
   the rewritten SHA; the pre-rebase SHA is not the increment. Update the
   execution branch to the rewritten candidate when a worktree already
   exists; otherwise keep the candidate on the integration checkout until
   workspace setup uses it. A rebase conflict uses
   [publication rebase conflicts](#resolve-a-publication-rebase-conflict)
   before continuing Git.
4. Validate the candidate. For a claim, confirm the selected entry is
   **Taken** on the candidate and that no empty commit was invented. For an
   increment, reuse accepted proof whose promise, boundary, implementation,
   setup, and observations still match. An unchanged-trunk fast-forward does
   not invalidate that proof. A newer-trunk rebase invalidates only proof
   the combined changes affect; reverify that behavior and reuse the rest.
   Do not treat rebase success as behavioral proof, rerun unrelated checks,
   or wait for CI.
5. Fast-forward the local target to the exact candidate by running
   `git -C <integration-checkout> merge --ff-only <candidate>` on the
   integration checkout named in [execution location](execution-location.md),
   not on the execution checkout. Do not substitute a same-command SHA push
   from the execution worktree (for example `git push origin <candidate>:main`),
   `git update-ref`, or `git branch -f` on that branch: none of these move the
   integration checkout's `HEAD` or working tree, so it would still report a
   stale `main` after the remote moved. Do not merge with any strategy other
   than `--ff-only`.
6. Immediately before pushing, retain the full candidate SHA and the
   previously published base. Push that exact candidate from the integration
   checkout to the authorized remote target, then fetch again on that
   checkout to refresh its view of the target. After confirmed success,
   append that SHA to this execution's retained published revisions in the
   existing plan or conversation. Do not drop earlier published SHAs of this
   execution, add a pre-rebase unpublished SHA, or treat a later moving
   `HEAD` as that publication. When an observer is already bound to the
   execution checkout, register that SHA with it. Registration failure is
   lost coverage: report it and do not claim the revision was observed. Do
   not wait for CI. Do not report or register success until local `main`,
   the freshly fetched remote, the retained SHA, and the execution branch
   all agree on the candidate SHA and `main...origin/main` is `0	0` on the
   integration checkout; a mismatch among those four identities is an
   unfinished publication, not a completed one.

## Publish wrap-up closure

Story wrap-up treats each owned wrap-up commit on the execution checkout as a
verified increment. Publish it immediately through
[verified-increment publication](#publish-a-verified-increment) before the
next wrap-up mutation that depends on that revision being recoverable on
shared trunk. Do not merge an execution branch. Do not push the execution
branch. Do not wait for CI.

Resolve observation ownership before the first wrap-up publication: recover
the execution's observer when it still exists; if execution already stopped
it, arm one observer from the same execution checkout against the authorized
target using [CI observation](ci-monitor.md). Register each confirmed
published SHA with that observer. After the last wrap-up publication this
invocation will perform, stop only that observer through the host adapter,
report the exact published closure SHAs and remaining coverage, and do not
wait. An unavailable bridge or registration failure is lost coverage: report
it and continue.

A publication stop leaves the commit recoverable on the execution branch.
Do not delete spent history, remove resources, or claim closure. After the
final-closure publication and wrap-up observer shutdown succeed, wrap-up
removes only this execution's clean local worktree and local execution branch.

## Recover a rejected push

A non-fast-forward rejection after local integration is not publication
and is not permission to force-push or to publish the execution branch.
Retain the rejected candidate SHA, previously published base, exclusive
turn, and unpublished publication state through the race. The local
target tip is the owned unpublished suffix, not a published revision.

If exclusive ownership is now unknown, the target is dirty or ambiguous,
or local trunk has unpublished commits that are not this execution's
owned suffix, stop. Preserve the exact refs, worktree, and index, and
report that retained state. Do not silently push those commits. Do not
undo the local suffix.

Otherwise refresh actual remote state and reconcile only the owned local
suffix, then retry one ordinary push:

1. Fetch the authorized remote for the target branch. Current trunk is
   the fetched remote target.
2. On the integration checkout, replay only commits after the previously
   published base onto that fetched trunk:
   `git rebase --onto <fetched-remote-trunk> <previously-published-base> <target-branch>`.
   Do not rebase from the rejected candidate; that would drop the suffix
   or rewrite another writer's commits.
3. Replace the unpublished candidate SHA with the rewritten target tip;
   the rejected SHA is not the increment. When an execution worktree
   exists and that branch has no remaining commits beyond the rejected
   candidate, move it with
   `git rebase --onto <target-branch> <rejected-candidate> <execution-branch>`.
   That second rebase is not permission to drop additional unfinished
   work. Update retained identity to the rewritten candidate.
   A conflict on either rebase uses
   [publication rebase conflicts](#resolve-a-publication-rebase-conflict).
4. Revalidate as in candidate step 4. The post-rejection rebase
   invalidates only proof the combined changes affect.
5. Push the rewritten candidate once with an ordinary push. After
   confirmed success, record that SHA as in candidate step 6.

A second rejection or other persistent failure stops. Preserve remaining
state and report it. Do not loop.

## Resolve a publication rebase conflict

A conflict while rebasing the owned unpublished suffix is not permission to
take `--ours` or `--theirs`, skip the commit, or continue Git blindly.

Inspect unmerged paths. For this project's product backlog, apply
[backlog merge conflicts](../dough-product-backlog/references/merge-conflicts.md)
before continuing Git. If that reference is unavailable, preserve the
conflict and report the missing guidance.

For other product or code paths, read the three Git versions (ancestor,
current side, and incoming side; index stages 1, 2, and 3). Identify the
fetched trunk versus the unpublished suffix from the actual commits; Git's
ours/theirs labels during rebase do not name intent. Compare each side with
the ancestor and retain a brief account of what each contributor changed.

When both sides' intent is understood and compatible, combine those changes.
Apply identical edits once. An unchanged region does not override the other
side. Do not choose an entire side. Continue the rebase only after the
combined working tree matches that account, then revalidate as in candidate
step 4: the combined change invalidates only the affected proof. Rebase
success is not behavioral proof. Do not publish until that recheck succeeds.

If identity, incompatible product intent, or a competing restriction remains
unresolved, preserve the exact refs, worktree, and index, including conflict
markers. Do not discard either side. Stop publication and report the specific
missing decision:

- Unclear value, domain meaning, architecture, or ambiguity that could
  waste a commit uses
  [human judgment](execution-decisions.md#stop-for-human-judgment).
- A change that would drop or weaken a required rejection or other
  contractual product constraint uses
  [a disputed plan restriction](execution-decisions.md#resolve-a-disputed-plan-restriction).

Do not register publication or continue as delivered. After a human
decision, resume from the preserved conflict state rather than inventing a
side.

## Resume an interrupted publication

After interruption during Trunk Mode delivery, classify the owned increment
from actual refs, retained rewritten identities, and observer receipts.
Continue the first unfinished obligation. Do not duplicate the commit, push
an already-published candidate, or replace the execution worktree. Verify
identity first as in
[execution location](execution-location.md). Fetch the authorized remote
before treating a push as unfinished.

Match the owned suffix to the retained rewritten candidate when that SHA
exists. A pre-rebase SHA that is no longer the tip is not a second increment.

| Boundary | Actual state | Continue with |
| --- | --- | --- |
| Only committed | Execution branch has the owned suffix; neither local target nor fetched remote trunk contains that candidate | [Publish a verified increment](#publish-a-verified-increment) from its preconditions. Do not commit again. |
| Integrated locally | Local target tip is the owned candidate; fetched remote trunk does not contain it | Exclusive-turn checks, then candidate push (step 6). Do not rebase or commit again unless a newer remote requires [rejected-push recovery](#recover-a-rejected-push). |
| Already published | Fetched remote trunk contains the candidate, or the retained rewritten SHA that replaced it | Append that SHA to retained published revisions if identity omitted it. Do not push again. |
| Missing CI registration | Remote trunk contains the published SHA; the existing observer's coverage or `register-push` receipt does not | Register that SHA with the existing observer. Do not push, and do not start a replacement observer. |

A lost or unknown push response is not unpublished. If the exact candidate is
already an ancestor of fetched remote trunk, treat it as already published.

If mode, checkout, branch, or candidate SHA is missing, contradictory, or
matches no unique owned suffix, preserve every existing worktree, branch, and
index. Report the gap. Do not create a replacement worktree, switch branches,
or guess which commit to publish.

## Preserve remaining state

Setup or publication failure preserves remaining state. It is not permission
to start unclaimed queued work, start implementation from an unpublished
claim, or substitute Story Branch Mode publication.
