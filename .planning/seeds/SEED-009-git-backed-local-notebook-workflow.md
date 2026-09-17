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

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order.

Story 44 cannot be taken until the target environment confirms story 43's
migration succeeded. It then removes the spent migration and its migration-only
complexity without changing the replacement root or later accepted history.

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
