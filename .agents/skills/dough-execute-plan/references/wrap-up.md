# Slice wrap-up

The coordinator runs this sequence after implementation, including an owned CI
repair. Publication of a validated increment or that repair is
[increment and repair publication](trunk-publication.md#publish-an-execution-increment-or-repair).

## Accept proof

Apply [proof ownership](../../dough-story-refinement/references/planning.md#own-executable-proof).
Treat the implementation return as an index, not as accepted evidence. For each
promise, inspect the actual uncommitted change at its reported product boundary
and the concrete setup and assertion or signal locations. Confirm that setup
supplies only the starting precondition and that the product establishes the
promised outcome. A passing command, test name, `proof:` summary, or assertion
whose setup supplies the outcome does not establish the promise.

Accept only the observations the inspected locations and result support. Retain
the promise, accepted boundary, inspected locations, and literal command in the
current slice wrap-up so refactoring can distinguish a proof reference from
proof already inspected and accepted. Do not routinely load the raw agent trace,
full command output, or reread unchanged parts of the diff. Expand inspection to
the smallest relevant underlying callers, setup, assertions, or implementation
when a location is missing, the boundary is unclear, or evidence contradicts the
change. Return incomplete or contradictory evidence to implementation, naming
the promise, inspected locations, and gap; refactoring cannot supply missing
behavior.

Judge the return by its substance, not its layout. The `proof:` block under
[the return contract](delegation.md#return-a-targeted-report-with-focused-proof)
is an example representation; accept an equivalent layout that carries the same
inspectable evidence — literal commands, results, owned changes, promise
coverage, boundaries, and setup and observation locations — without a
report-only resend. Formatting never substitutes for substance: a
canonical-looking report missing the terminal result or contradicting inspected
evidence remains incomplete whatever its layout. Explicit completion markers
with a separate workflow contract, such as `## REFACTOR COMPLETE`, stay
verbatim.

Reuse accepted inspection while its promise, boundary, implementation, setup,
and observations remain unchanged. Recover literal commands from the original
handoff when possible. Rerun only for missing or ambiguous proof, a covered
boundary changed during wrap-up, or omitted integration proof required by the
slice. Do not sample randomly or repeat valid proof to manufacture process
compliance. Preserve failures under
[execution decisions](execution-decisions.md#diagnose-failed-proof).

Require CI-safe work before delivery: no deliberate failing tests or unfinished
end-to-end proof represented as complete. Use this project's convention to mark
unfinished proof. Do not run full CI before commit unless explicitly required.

## Deliver the change

Run this sequence in the selected execution location established by
[execution location](execution-location.md). Pass its checkout to every
delegated refactor, generator, and host operation; do not let an inherited
working directory redirect delivery. This slice delivery does not integrate
or remove a retained execution branch or worktree. After the commit, publish
the owned unpublished increment or repair through
[increment and repair publication](trunk-publication.md#publish-an-execution-increment-or-repair).

As each obligation finishes, retain the current delivery boundary in the
execution conversation with the owned unfinished paths, accepted proof and its
boundary, applicable implementation or refactor return, exact observer
identity when present, and the unpublished candidate, previously published
base, and accepted revision and target after any rewrite. On recovery,
reconcile this focused record with actual Git, agent, and observer state under
[execution-boundary recovery](../SKILL.md#continue-or-recover-at-an-execution-boundary).
Classify the increment or repair with
[interrupted publication](trunk-publication.md#resume-an-interrupted-publication)
and continue that unfinished obligation only.

1. Spawn a fresh agent to run
   [dough-post-change-refactor](../../dough-post-change-refactor/SKILL.md).
   Supply the execution source, execution checkout and branch, slice,
   accepted proof with its inspected setup and observation locations, changed
   paths and boundaries, project context, and ownership boundaries; supply the
   plan path only when one exists. Keep
   formatting and hook-owned lint with the coordinator. Do not
   add instructions contradicting that skill's decide-before-testing contract;
   explicit human verification requests remain authoritative.
2. Inspect its report, require `## REFACTOR COMPLETE`, and recheck
   [execution decisions](execution-decisions.md). Use the reported changed paths,
   boundaries, and proof effects to inspect only newly affected implementation
   and observation locations. Reuse accepted inspection for boundaries reported
   and confirmed unchanged; when refactoring invalidated a boundary, accept its
   rerun or replacement proof only after inspecting the new boundary and
   observation locations under **Accept proof**. A refactor stop, missing marker,
   contradiction, or unresolved proof gap prevents commit. Report unnecessary
   test runs as deviations; reuse valid evidence instead of repeating work.
3. Run this project's generator if its trigger changed and the required output was
   not already regenerated and verified during refactoring. Fix generation at
   source and validate affected consumers.
4. Run this project's selective formatting command directly once; let it select
   affected components, including a planning-only no-op. Require success before
   staging. Repair mechanical failures and repeat only when the repair
   invalidates preparation. Stop for semantic or design judgment.
5. For planned execution, update the active plan and any project-required
   summary with learnings, slice status, accepted proof needed for reuse, and
   revised remaining slices under
   [plan refinement](../../dough-story-refinement/references/planning.md#refine-the-active-plan).
   For stale feature-story understanding, record `awaiting story review` and
   identify the selected story in its seed and the affected field. For stale
   correction understanding, record `awaiting correction review` and identify
   the correction plan and affected field. Stop at the safe delivery boundary
   without changing other stories or the correction outcome. This plan update
   alone does not trigger another formatting pass. Record a CI repair result
   with the interrupted slice's existing status. For quick execution, do not
   create or update a plan, completion note, project summary, or substitute
   execution record; retain learnings and delivery progress in the conversation.
   If source understanding is stale, identify the affected field and, when a
   story is the source, its canonical seed location; stop for human review
   without changing its scope or inventing a story.
6. Stage only owned files or separable owned changes and inspect the staged diff.
   Stage all content only when all of it is owned. Unrelated unstaged work does
   not block delivery. Resolve unrelated staged content or ambiguous ownership
   with its owner; never silently unstage, reset, or revert another task's work.
7. Commit CI-safe work using this project's check-only lint hook on staged
   components, with no formatting or index mutation. Resolve a different hook
   contract before committing. Fix mechanical findings; stop for semantic or
   design judgment. Do not run hook-owned lint independently. If hook repairs
   invalidate preparation, rerun formatting before restaging and retrying.
8. Immediately before publishing, resolve the owned unpublished suffix in the
   execution workspace. Publish it through
   [increment and repair publication](trunk-publication.md#publish-an-execution-increment-or-repair)
   when this caller has publication authority.
   Caller-selected current-branch work and an already-supported host-owned
   execution stay in the recorded checkout. Create no worktree and do not
   switch branches. Without that authority, commit there and report the
   revision as committed and pending publication. Do not push. Remote refs
   stay unchanged, and the checkout identity stays the recorded path.
   With that authority, publish from that same checkout through the increment
   owner above. The receipt is the accepted SHA and the authorized target.
   A pending human edit on that checkout stays out of the published commit.
   When the selected checkout is the default checkout, apply
   [default-checkout preservation](maintain-default-checkout.md#preserve-pending-local-work)
   before mutating it. A local commit or a local merge stays a local
   operation; do not report it as remote publication. Codex, Cursor, and
   Claude keep the recorded checkout and authorized target their existing
   adapters already supply. A rejected push follows
   [rejected-push recovery](trunk-publication.md#recover-a-rejected-push).
   A [publication stop](trunk-publication.md#preconditions),
   including a
   [rebase conflict stop](trunk-publication.md#resolve-a-publication-rebase-conflict)
   or a recovery stop, leaves that candidate unpublished and recoverable.
   A [default-checkout preservation result](maintain-default-checkout.md#preserve-pending-local-work)
   defers maintenance and does not by itself reject an accepted remote
   candidate or block a push from a separate owned workspace. Do not register
   a SHA the remote has not accepted, and do not treat that unpublished
   candidate as delivered.
   After confirmed success, register the
   accepted SHA with the existing observer bound to that target by running
   `node '/ABSOLUTE/RESOLVED/SKILL/scripts/ci-mailbox.mjs' register-push
   OBSERVER_DIRECTORY SHA`. Use the observer directory and checkout-bound
   runtime retained for this execution. Apply that same registration after an
   owned repair publication and after a Trunk Mode claim once that observer
   is armed. Do not register a publication stop. Do not read a later moving
`HEAD` or start another observer. Registration failure is lost coverage:
report it and do not claim the revision was observed. An unavailable bridge leaves publications
   unobserved: report that gap and continue. Success completes routine
   delivery; a post-slice decision stop occurs after safe work is delivered.
   Keep the [CI observer](ci-monitor.md) running and handle delivered failures
   through its repair protocol. Never wait for CI or deployment after a normal
   or repair publication.
