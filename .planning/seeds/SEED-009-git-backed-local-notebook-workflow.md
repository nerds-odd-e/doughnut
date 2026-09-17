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

- **Goal:** Donut maintainers work with a migration chain and notebook-Git
  code that no longer carry the one-time rebaseline. After the only target has
  crossed the reset, the destructive rebuild recipe and its duplicate raw-JDBC
  projection of notebook content stop being permanent product complexity.
  Owners see no change.
- **Scope (required):**
  - Delete `V300000330__RebaselineExistingNotebookGitBindings` and everything
    whose caller graph ends in it. Inspection on 2026-09-17 found:
    `NotebookGitBaselineRebuild`, `NotebookGitRows`, and
    `RebaselineExistingNotebookGitBindingsMigrationTest`. Re-check callers at
    execution time; delete only what is still migration-only.
  - Keep the migration-chain guidance truthful. The db-migration skill must
    name `V300000329__ReplaceNoteTitleFunctionalIndex.java` as the newest
    remaining file, and must say new migrations exceed **`300000330`**,
    because long-lived `flyway_schema_history` tables still own that version.
    The current guidance says "exceed `300000329`" and uses a `V300000330`
    example, which already invites reuse.
  - Rely on the existing `flyway.repair()`-before-`migrate()` startup, as the
    earlier "Retire spent data migrations" cleanup did. No squash, no new
    placeholder, no cleanup toggle, no `ignoreMigrationPatterns` change.
- **Scope (preserve):** notebook creation and its cutover root, accepted Git
  history appended after the rebaselined root, Portable-tree encoding and bundle
  building, export row types, Flyway startup/repair, and product-level
  notebook-Git behavior tests. Do not remove shared owners such as
  `NotebookGitCutoverService`, `NotebookGitBundleBuilder`, or
  `notebookExport` types merely because the migration used them.
- **Deferred / not promised:** re-verifying or re-running the rebaseline;
  squashing the baseline; generalizing a spent-migration retirement mechanism.
- **Key examples:**
  1. Given production recorded version `300000330` as successful, when the
     cleaned-up build starts against that database, Flyway starts without error,
     does not rebuild any notebook, and each notebook keeps its current accepted
     head.
  2. Given an empty database, when the cleaned-up build migrates it, the schema
     installs through `300000329` and the backend suite passes.
  3. Given a maintainer adds the next migration, when they follow the
     db-migration skill, they choose a version above `300000330`.
  4. Given a notebook created or edited after cleanup, when its Git history is
     read or appended to, behavior matches before cleanup (existing notebook-Git
     tests stay green).
  5. Given the product tree after cleanup, a search for the rebaseline
     migration, `NotebookGitBaselineRebuild`, or `NotebookGitRows` finds
     nothing outside planning history.
- **Prerequisite (genuine constraint):** production, and any other
  deliberately retained long-lived environment, must confirm that version
  `300000330` is recorded as successful in `flyway_schema_history`. Deleting it
  earlier would leave unconverted notebooks on their abandoned history
  permanently. As of 2026-09-17, release tag `v1.3.7` contains the migration;
  successful production application is **not yet confirmed**. Do not take or
  execute this story before that confirmation. Deployment and confirmation are
  manual owner actions, not implementation scope.
- **Value / learning:** The migration is intentionally temporary; removal keeps
  the notebook-Git service surface limited to live runtime paths.
- **Effort / status:** Refined 2026-09-17; not yet planned. S (30–60 minutes),
  moderate-high confidence: the caller graph is three files plus guidance, and
  the earlier cleanup is a direct precedent.
- **Depends on / safe stopping point:** the production confirmation above.
  Cleanup is one deletion commit; it leaves the new roots and all later commits
  untouched.

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
