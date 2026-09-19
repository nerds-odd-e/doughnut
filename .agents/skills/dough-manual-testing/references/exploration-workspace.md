# Own a temporary exploration workspace

Use this lifecycle when a calling skill requires checkout-bound exploration but
no workflow has already established a checkout.

## Select the checkout

First determine whether a story, active plan slice, explicit caller selection,
or another workflow already owns a checkout for the work. Use that checkout and
do not create a nested temporary workspace. Its owning workflow retains delivery
and cleanup responsibility.

Otherwise, before checkout-bound setup or investigation, verify the current Git
revision. Create one temporary branch and paired worktree from that revision
(for example, `git worktree add <path> -b <branch> <verified-revision>`). Record
the branch, worktree path, and starting revision as the exploration workspace
identity. If creation only partly succeeds, retain and report the observed state
instead of guessing that it is safe to retry or remove.

## Use and resume it

Use the same workspace for all checkout-bound preparation, exploration,
evidence handling, and reporting in that session. Do not create a second
workspace alongside it. Leave the originating checkout unchanged throughout
the session unless the calling skill explicitly begins its own post-exploration
integration sequence.

After an interruption, verify the recorded branch-worktree association and
starting revision against actual Git state before reusing the workspace. If the
identity cannot be verified, stop and report it rather than continuing in a
possibly different checkout.

The workspace isolates only the Git checkout. Shared accounts, services,
databases, and other external state remain governed by their existing rules.

## Close or retain it

Before cleanup, remove session-owned temporary artifacts and resolve any
caller-authorized durable evidence through the caller's own preservation and
integration rules. Remove only a clean, unambiguous worktree, then delete its
branch (`git worktree remove`, followed by safe branch deletion).

If cleanup would be unsafe because of dirty state, unresolved evidence,
ambiguous ownership, or an identity mismatch, do not force, reset, or delete
anything. Retain and report the exact branch, worktree path, starting revision,
and evidence state so the session can be resumed or deliberately disposed of.
