---
id: SEED-009
status: active
planted: 2026-09-04
planted_during: ADR 0002 v1 discussion
trigger_when: when selecting the next Git-backed notebook workflow story from the product backlog
scope: large
---

# SEED-009: Refine a Donut notebook locally with Obsidian and AI-enabled IDEs

## Why This Matters

Notebook owners should move between local refinement and Donut without manual
copying, losing work, or separating notes from their learning history.
The [near-future direction](../PRODUCT-BACKLOG.md#near-future-direction) governs
selection: one append-only history, no branching or rebasing, with either
repository potentially several commits behind.

These candidates are planning hypotheses grounded in the retained workflow
boundaries and the owner's clarified direction, not a fresh implementation
audit. Confirm each gap during refinement before planning. Performance work
remains separately owned and is not decomposed or selected here.

## Alternatives and Decision

Publishing after every local commit is the strongest smaller workaround, but
does not meet the explicit requirement to catch up across accumulated commits.
Manual copying sacrifices the continuous workflow and can lose identity.
Deferring all further work would leave that requirement unanswered even after
publication becomes faster.

The owner prioritized append-only web saves, receiving web renames, and receiving
web deletions ahead of accumulated local publication on 2026-09-13. These basic
web workflow changes take precedence while publishing after each local commit
remains a smaller workaround. Accumulated publication is retained after them;
its frequency and urgency remain unmeasured.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are comparative,
low-confidence hypotheses; refinement may split work further. Example counts
are evidence of behavior, never limits on accepted histories or note counts.
All stories preserve authorization, authored content, note identity and learning
data; invalid or ambiguous changes must not silently discard work.

<a id="story-44"></a>

### 44. Retire the spent notebook rebaseline migration

- **Goal / beneficiary:** Donut maintainers work with a migration chain and
  notebook-Git implementation that no longer carry one-time rebaseline code,
  fixtures, and tests after the target environment has completed the reset.
- **Scope:** After production, and any other deliberately retained long-lived
  target environment, confirms successful application of story 43's migration,
  remove the spent Flyway migration and all production/test support whose
  caller graph ends in that migration. Preserve current notebook creation,
  accepted Git history after the new root, Portable-tree encoding, Flyway
  startup/repair, and product-level behavior tests. No cleanup toggle or
  permanent migration framework is added.
- **Key example:** Given production has recorded and successfully completed the
  rebaseline migration, when maintainers install a fresh database or restart an
  already-upgraded database after cleanup, Flyway succeeds and current notebook
  Git behavior remains green without the rebaseline migration, its raw-JDBC
  helpers, or migration-only tests in the product tree.
- **Boundary:** Production confirmation is a genuine prerequisite; do not take
  or execute this story beforehand. Manual deployment and confirmation are not
  product implementation scope. Do not remove shared runtime Portable-tree or
  notebook-creation owners merely because the migration reused them.
- **Value / learning:** The migration is intentionally temporary. Removing it
  after its only target has crossed the conversion prevents a destructive reset
  recipe and its duplicate JDBC projection code from becoming permanent
  product complexity.
- **Effort / status:** Queued immediately after story 43 and not yet planned. S
  (30–60 minutes), moderate confidence based on the earlier spent-migration
  cleanup precedent.
- **Depends on / safe stopping point:** Production confirmation that story 43's
  migration completed successfully. Cleanup leaves the new root and all commits
  appended after it untouched.

<a id="story-45"></a>

### 45. Refuse to pull when local and accepted notebook history have diverged independently

- **Goal / beneficiary:** A notebook owner who has unpublished local work can
  ask for newer accepted history without Donut rewriting that work or leaving
  the checkout inside a Git operation. When neither head contains the other,
  `donut notebook pull` refuses clearly and preserves both lines of work.
- **Purpose challenged:** Preventing a broken checkout alone would permit the
  smaller policy of attempting a rebase and automatically aborting only when
  it conflicts. That is not the selected purpose. The governing near-future
  direction also preserves original commits in one append-only sequence and
  excludes rebase, merge, and synthetic replay as ways to reconcile independent
  advancement. Therefore even a conflict-free automatic rebase is the wrong
  product outcome: it replaces the owner's commit and silently chooses a
  reconciliation policy the product has deferred.
- **Why now / priority:** Story 42's manual validation (plan 131) directly
  observed a normal owner journey leave the checkout detached, mid-rebase, and
  conflicted, with guidance to continue or abort the rebase. The current CLI
  also deliberately advertises and tests successful rebase and replay paths,
  so this is a live contract conflict rather than a speculative edge. Retain
  this story ahead of the current alternatives: story 44 is blocked on target-
  environment confirmation; SEED-020 story 1 corrects maintainer vocabulary
  without changing owner behavior; and SEED-018 story 4 is valuable but still
  lacks a chosen 10,000-note workload and acceptable waiting boundary after
  substantial measured gains at 1,000-note scale. This ordering is justified
  by safety and consistency with the selected near-future direction, not by
  usage-frequency evidence; the frequency of independent advancement remains
  unmeasured.
- **Scope:** On a clean local `main` whose history shares an ancestor with the
  accepted history, compare the two heads before beginning any reconciliation.
  If neither head is an ancestor of the other, refuse regardless of commit
  count, changed paths, or whether Git could combine the content cleanly. The
  refusal identifies the local and accepted heads and explains that independent
  advancement is unsupported and local work was not changed. Preserve the
  local branch ref, checked-out HEAD, index, working-tree bytes and cleanliness,
  and create no rebase, merge, cherry-pick, or replay state. Pull remains
  read-only with respect to the remote accepted head.
- **Key examples:**
  1. Given one unpublished local edit and a later accepted edit of the same
     note, pull refuses, names both heads, leaves the checkout on the original
     branch at the original local head with a clean index and working tree, and
     creates no Git-operation state or conflict markers.
  2. Given independent local and accepted edits that Git could combine without
     a content conflict, pull still gives the same refusal and does not replace
     the local commit. Conflict-freedom is not permission to reconcile.
- **Required linear counterexamples:** If local `main` is an ancestor of the
  accepted head, pull retains the existing fast-forward behavior. If the
  accepted head is an ancestor of local `main`, pull keeps the existing
  supported already-based behavior and does not change unpublished commits.
  These are linear lag, not independent advancement; this story does not widen
  the currently supported count or shape of unpublished local commits.
- **Excluded:** No automated or guided reconciliation, recovery command,
  conflict editor, backup branch, remote-tracking ref, web UI, publish-policy
  change, or projection-drift recovery. Existing dirty-checkout, unrelated-
  history, concurrent-local-change, download, and authentication failures keep
  their own behavior. The story does not promise that downloading or inspecting
  history leaves the repository's internal object store byte-for-byte unchanged;
  the preserved owner state is the refs, HEAD, index, and working tree. It does
  deliberately retire successful divergent rebase and exact-subtree replay
  behavior rather than preserving those earlier product promises.
- **Architecture constraints:** Accepted
  [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs the Portable tree but explicitly leaves Git integration behavior
  outside its decision. Accepted
  [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md)
  permits a handled failure for this business outcome and requires a clearer
  message. [Proposed ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
  describes the same no-rewrite refusal direction but remains non-binding;
  this story is governed by the product backlog's human-owned near-future
  direction, not by treating that proposal as Accepted.
- **Value / learning:** Restores one predictable pull contract before more
  owners depend on the contradictory reconciliation behavior: linear lag can
  advance without rewritten commits; independent advancement stops safely for
  a future human-owned policy. It removes the only discrepancy found by story
  42 while retaining both bodies of work.
- **Effort / status:** Refined, not yet planned. M (1–2 hours), low confidence.
  The ancestry rule and refusal are small, but current CLI help and a broad set
  of tests intentionally promise rebase, conflict continuation, absorption,
  publication after rebase, and exact-subtree replay; refinement must not hide
  the cost of retiring those promises.
- **Depends on / safe stopping point:** None; can be taken immediately. Safe
  to stop once every independently advanced history is refused before checkout
  mutation, the two linear ancestry cases remain green, and no reconciliation
  or recovery behavior has been added.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order.

The owner placed story 43 first on 2026-09-17. This explicit priority is
preserved as a one-time explicit break in the append-only near-future direction:
all prior server history is abandoned without backup, and owners reacquire
their local copy from the replacement root. Story 44 follows it but cannot be
taken until the target environment confirms the migration succeeded; it removes
the spent migration and its migration-only complexity.

Story 42's manual validation found one discrepancy against the settled
append-only direction: `pull` rebases instead of refusing on independently
advanced histories. The owner placed the resulting fix, story 45, first in
the queue on 2026-09-17. Refinement retains that order because it removes an
observed unsafe owner outcome and a live contract conflict. Story 44 cannot
start before target-environment confirmation; the trash-vocabulary correction
does not change owner behavior; and publication-scale validation still lacks
its representative workload and acceptance boundary. If the owner no longer
selects the no-rebase direction, story 45 should not merely move down the queue:
its purpose disappears and the product needs a separate reconciliation-policy
decision instead.

Completed stories 20 and 25 supply accumulated local publication and web-note
movement evidence to story 42; they are not remaining queue items. Publication
performance remains the next selected Git-scale item.

## Deferred Directions

Keep these outside the current queue rather than cancelling them:

- Broader reconciliation of independently advanced local and remote histories,
  including multiple local commits, structural changes, and conflict recovery.
  Earlier proposals used ordinary Git rebase; the current direction excludes it.
- Recovery for notebooks whose live projection already differs from accepted
  history. Establish the owner's blocked journey and a deliberate preservation
  policy before selecting recovery work.
- Wider folder operations. Same-notebook web folder moves, rename-with-content-edit,
  and multi-commit note identity preservation are supported. Cross-notebook
  folder-move Git histories and further subtree composition remain deferred.
- Native standard Git transport, notebook binding within a project subdirectory,
  attachments, and history browsing or revision restoration.

Broader web-authoring coverage is retained in
[SEED-017](SEED-017-cohesive-design-corrections.md#open-product-decision).
The old proposal's rebase model and web-tip amendments are not constraints on
the newly selected append-only stories.

## Open Decisions

- How often do web renames, deletions and moves interrupt actual owner work?
  The order above is a value hypothesis, to revise with use.
- Ordinary-note rename-with-edit direction is settled in the North Star.
  Wider identity outcomes remain deferred;
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
  remains Proposed. Confirmed deletion/recreation starts a new identity.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
