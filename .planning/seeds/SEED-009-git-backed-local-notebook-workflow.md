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

<a id="story-43"></a>

### 43. Rebaseline every existing notebook from current Donut content

- **Goal / beneficiary:** An owner of an existing live notebook can acquire a
  Git-backed copy whose initial accepted commit exactly represents the
  notebook's current Donut Portable tree, without stale baseline data left by
  earlier synchronization development.
- **Why now:** The owner explicitly prioritized a fleet-wide reset on
  2026-09-17. Deferring is the strongest smaller alternative because new
  notebooks already receive a root commit and an earlier migration rebuilt
  then-existing bindings. The owner now wants every current production binding
  reset once before the append-only workflow continues from a known current
  baseline.
- **Scope:** A one-time data migration runs automatically through ordinary
  Flyway application startup, with no toggle, placeholder gate, opt-in, or
  backup. It selects every live notebook that already has a Git binding and
  replaces that binding's accepted Git object ID and bundle with one fresh
  parentless commit built from the notebook's current canonical Portable tree.
  Preserve notebook, folder, note, binding, ownership, and learning identities
  and data. The replacement is atomic per notebook; an unbuildable notebook
  fails startup loudly rather than leaving that notebook with a partially
  replaced head and bundle. Manual release and deployment are outside this
  story.
- **Key example:** Given a live notebook whose current Donut data contains a
  notebook readme, nested folders and readmes, active notes, and notes beneath
  `_trash`, when the release migration runs, its accepted bundle contains
  exactly one root commit whose tree matches that current content under
  Accepted [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  while all Donut IDs, authored content, and learning history remain unchanged.
- **Fleet boundary example:** Given several live notebooks with existing Git
  bindings, one migration run rebaselines every one; a failure stops startup
  visibly without creating duplicate bindings or mixed head/bundle pairs.
  Notebooks created after the migration continue to receive their ordinary
  creation-time root commit.
- **History reset:** The replacement intentionally abandons the complete prior
  server Git graph: the original root commit and every later commit. No backup,
  retained ref, or ancestry bridge is required. Any previously downloaded local
  repository is obsolete and must be freshly acquired before publication.
- **Population:** Only live notebooks that already have a Git binding are in
  scope. Soft-deleted notebooks and anomalous live notebooks without a binding
  are neither selected nor repaired by this migration.
- **Boundaries:** This story does not change current MySQL notebook content,
  invent historical commits, reconcile or rewrite an owner's local clone,
  introduce branching/rebasing, add a new Git transport, or change the
  creation-time behavior for future notebooks. Accepted ADR 0006 permits the
  migration to fail loudly; this story still requires an atomic replacement so
  a failure cannot persist a mismatched object ID and bundle. The owner
  explicitly overrides the repository's default gated-DML guidance for this
  migration: it must run on the first application startup after release.
- **Architecture status:** Accepted ADR 0004 governs the rebuilt Portable tree.
  ADR 0002's fleet-bootstrap and single-mainline text is still Proposed and
  therefore informative rather than binding. The owner explicitly accepts this
  one-time break in the backlog's append-only direction; the replacement root
  becomes the beginning of the next append-only sequence.
- **Effort / status:** Refined, planned, and queued first. M (1–2 hours),
  moderate confidence. A retired fleet-rebuild implementation is useful
  evidence, not code to restore without rechecking the current schema and
  migration conventions. [Slice plan](../quick/132-rebaseline-existing-notebooks/PLAN.md).
- **Depends on / safe stopping point:** No unfinished product prerequisite.
  After the migration, each included notebook has one internally consistent
  current baseline even if later synchronization or performance work is
  cancelled.

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

<a id="story-42"></a>

### 42. Manually validate the complete append-only notebook workflow

- **Goal / beneficiary:** A notebook owner and product owner have current,
  externally observed evidence that one Git-backed notebook can move between
  Donut and ordinary local tools without copying work, rewriting accepted
  or local notebook history, or separating a note from its identity and
  learning history.
- **Why now:** The individual append-only capabilities have been delivered and
  proved automatically, but the complete owner journey has not received one
  bounded manual evaluation against the current product. Relying only on the
  component and E2E evidence is the strongest smaller alternative; it does not
  assess the joined browser, installed CLI, local Git, guidance, and refusal
  experience as an owner encounters it.
- **Scope:** Spend at most one hour preparing and manually observing the second
  paragraph of the backlog's near-future direction in one isolated E2E
  environment. Use the browser, installed CLI, and ordinary Git from the
  Story Branch Mode execution worktree. Cover both linear lag directions: a
  clean local checkout receives several accepted web commits by fast-forward,
  and the remote accepts several local commits as their original contiguous
  chain. Exercise representative content editing, rename or movement with
  content change, stable note identity, and retained learning state. Observe
  the product's owner-facing guidance as part of the journey.
- **Append-only boundary:** Independently advanced local and accepted histories
  are not reconciled. With unpublished local work and a newer accepted head,
  pull must refuse clearly while leaving the local head, files, index, and
  accepted history unchanged. No rebase, merge-based reconciliation, replayed
  replacement commit, or conflict-resolution journey is accepted behavior.
  The existing source audit predicts contrary rebase/replay behavior; manual
  execution must observe the product rather than treat that audit as the test
  result.
- **Key examples:**
  1. From a clean clone, make two local commits that rename or move and edit one
     learned note, publish once, and receive the result in another clean clone.
     Donut retains the original note route and learning state, the receiver has
     the exact final bytes, and the original local commits remain the accepted
     ancestor chain.
  2. From a clean clone, make several accepted web changes to that notebook,
     including an ordinary edit and rename or move, then pull once. The checkout
     fast-forwards through every accepted commit to the final paths and bytes
     without a second copy or rewritten commit.
  3. From a clean clone, commit unpublished local work and independently append
     a web change. Pull refuses without changing either history or starting a
     Git operation; the owner keeps both bodies of work and receives guidance
     consistent with the append-only boundary.
- **Manual-testing contract:** Apply `dough-manual-testing` for the one-hour
  coverage, evidence, and report policy, and the repository `manual-testing`
  skill for browser operation. Breadth comes before depth. Reuse existing
  setup and automated evidence where it is sufficient; do not spend the hour
  replaying a deterministic matrix. If the planned observations finish with no
  actionable finding or material uncertainty, report `Good.`; otherwise report
  only discrepancies, unresolved expectations, improvements, and material
  coverage gaps with evidence. Do not diagnose or repair findings during this
  story.
- **Time budget:** 60 minutes total from manual environment preparation through
  the observation report: 10 minutes preparation, 25 minutes breadth across the
  two linear directions and divergence refusal, 15 minutes selective depth on
  identity/learning/history and any surprise, and 10 minutes confirmation and
  reporting reserve. Stop at the budget and report unobserved promises as
  coverage gaps rather than extending the session.
- **Boundaries:** This is manual evaluation, not another automated acceptance
  suite, performance validation, a rebase/merge implementation, projection-
  drift recovery, cross-notebook synchronization, native Git transport, or a
  repair story. It does not cover the 10,000-note performance story. Temporary
  setup artifacts must remain isolated and be removed; findings do not authorize
  product changes.
- **Execution:** Run the plan through `dough-execute-plan` in its default Story
  Branch Mode (no `--trunk` and no current-branch override), so the queue claim,
  isolated worktree, observation, and delivery evidence follow the normal
  planned-execution lifecycle. [Slice plan](../quick/131-manually-validate-append-only-notebook-workflow/PLAN.md).
- **Effort / status:** Refined, planned, and queued. One 60-minute manual
  observation slice; environment startup and coverage selection are bounded by
  the same testing budget. No product-scope decision remains open.
- **Depends on / safe stopping point:** The delivered append-only Git-backed
  workflow and its existing testability/CLI setup. The report remains useful
  even when it finds discrepancies or later feature work is cancelled; it must
  not claim unobserved behavior.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order.

The owner placed story 43 first on 2026-09-17. This explicit priority is
preserved as a one-time explicit break in the append-only near-future direction:
all prior server history is abandoned without backup, and owners reacquire
their local copy from the replacement root. Story 44 follows it but cannot be
taken until the target environment confirms the migration succeeded; it removes
the spent migration and its migration-only complexity.

Story 43 is now the first queued item; story 44 follows as
its target-environment-gated cleanup, then story 42 owns the one-hour manual
validation of the completed append-only local/web workflow. Publication-scale
validation remains after it.

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
