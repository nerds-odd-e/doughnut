# Resolve a publication rebase conflict

A conflict while rebasing the owned unpublished suffix is not permission to
take `--ours` or `--theirs`, skip the commit, or continue Git blindly.

The ordinary rebase in [Publish the candidate](publish-the-candidate.md#publish-the-candidate) step 3 is run through this project's
installed product backlog rebase adapter, not a raw `git rebase`, whenever it touches the product backlog
(often `PRODUCT-BACKLOG.md`); see [reconcile product backlog Git operations](../../dough-product-backlog/references/merge-conflicts.md)
for how to resolve and run it. Its own `conflict`/`refused`/`blocked` result already identifies the real
replayed commit, its parent, and the current destination from Git's own rebase state, never from
ours/theirs labels. Resolve the backlog's own unmerged path following that reference, `git add` it, then
run the adapter's own `continue` for this same rebase — never a raw `git rebase --continue`. A clean
replay the adapter reports as `disputed` is not a Git conflict and has nothing staged to resolve the usual
way: repair the backlog by hand, or decide the current result should stand as is, then run the adapter's
own `validate` before this section's own revalidation below and before publishing. If neither the adapter
nor that reference is available, preserve the conflict and report the missing guidance.

[Recover a rejected push](publish-the-candidate.md#recover-a-rejected-push)'s two `--onto` rebases are not run through this
adapter: its CLI has no equivalent for rebasing a range other than the currently checked-out branch onto a
ref. Until that gap is closed, resolve a conflict touching the backlog on either of those two rebases with
[the fallback domain knowledge](../../dough-product-backlog/references/merge-conflicts.md#fallback-domain-knowledge)
instead, applied by hand exactly as below.

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
