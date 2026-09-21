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
selection: one accepted append-only history, with either repository potentially
several commits behind. If local and web changes diverge, the local side rebases
unpublished work onto the latest accepted head before publishing; the remote
never merges, rebases, or rewrites accepted history (ADR 0002, amended 2026-09-20).

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

<a id="story-46"></a>

### Remove the ZIP export feature

- **Identity:** SEED-009#story-46
- **Goal:** Donut offers one way to take a notebook's Portable content out — Git
  acquisition — so owners and maintainers no longer carry a second, weaker export
  that every tree change (attachments now, images next) must also keep correct.
- **Evaluation:** The web notebook menu and notebook settings no longer offer a
  ZIP export, the `exportNotebook` API operation is gone, and an owner still
  obtains the complete notebook, including attachments, through Git acquisition.
- **Scope / value:** Owner decision 2026-09-21: ZIP export is no longer needed
  now that Git acquisition exists; remove it completely — web buttons, API
  operation and generated client, ZIP building, and its E2E feature. The shared
  live Portable tree stays: Git cutover, history reset, accepted web changes and
  drift detection still read it. Less code is the main value.
- **Effort hypothesis:** S–M, medium confidence; mostly deletion.
- **Depends on:** None. Queued immediately after delivered nested attachment
  continuity by owner instruction; that delivery intentionally proved nothing
  about ZIP output.
- **Safe stopping point:** Complete on its own; nothing later needs it.
- **Open decisions for refinement:**
  - ZIP export needs only read access, while Git bundle download needs full
    notebook authorization. Removing ZIP takes the only export away from
    read-only users (for example subscribers). Accept that, or open Git
    acquisition to readers?
  - Git acquisition is CLI-only today; the web offers no Git download. After
    removal a browser-only owner has no export. Accept that, or add a web
    download of the Git bundle (a file ordinary users cannot open without Git)?
  - Confirm no notebook remains without a Git binding, so none loses its only
    export.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns global order.

Completed story 44 removed the spent notebook rebaseline migration
(`V300000330__RebaselineExistingNotebookGitBindings` and its migration-only
helpers and test) once production confirmed it applied, and corrected the
db-migration skill's version guidance; it left the replacement root and later
accepted history untouched.

Completed stories 20 and 25 supply accumulated local publication and web-note
movement evidence to story 42; they are not remaining queue items. Publication
performance remains the next selected Git-scale item.

## Deferred Directions

Keep these outside the current queue rather than cancelling them:

- Additional Donut-assisted conflict-recovery experiences. Ordinary local Git
  rebase is the owner's selected reconciliation workflow; the local side resolves
  divergent work before publishing a fast-forward result. This is not a promise
  of automatic reconciliation by the CLI or remote.
- Recovery for notebooks whose live projection already differs from accepted
  history. Establish the owner's blocked journey and a deliberate preservation
  policy before selecting recovery work.
- Wider folder operations. Same-notebook web folder moves, rename-with-content-edit,
  and multi-commit note identity preservation are supported. Cross-notebook
  folder-move Git histories and further subtree composition remain deferred.
- Native standard Git transport, notebook binding within a project subdirectory,
  and history browsing or revision restoration. Supporting files and attachments
  are now selected in [SEED-035](SEED-035-ai-workspace-supporting-files.md).

Broader web-authoring coverage is retained in
[SEED-017](SEED-017-cohesive-design-corrections.md#open-product-decision).
Local rebasing of unpublished work is compatible with accepted append-only
history. Web-tip amendments and rewriting already accepted history remain
outside the selected workflow.

## Open Decisions

- How often do web renames, deletions and moves interrupt actual owner work?
  The order above is a value hypothesis, to revise with use.
- Ordinary-note rename-with-edit direction is settled in the North Star.
  Wider identity outcomes remain deferred;
  [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  is Accepted. Confirmed deletion/recreation starts a new identity.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Accepted ADR 0002 defines the broader synchronization architecture.
