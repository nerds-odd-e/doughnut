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

Recommend accumulated local publication first: it tests the central assumption
that ordinary commit-by-commit work can be exchanged without rewriting history.
Then address append-only web saves and common web authoring gaps. The ordering
is proposed on value and risk, not measured user frequency.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are comparative,
low-confidence hypotheses; refinement may split work further. Example counts
are evidence of behavior, never limits on accepted histories or note counts.
All stories preserve authorization, authored content, note identity and learning
data; invalid or ambiguous changes must not silently discard work.

<a id="story-20"></a>

### 20. Publish accumulated local commits without rewriting history

- **For / why:** An owner can commit naturally while working locally, then publish the accumulated work when Donut's accepted head is an ancestor of local main.
- **Evaluation:** Given several successive local commits editing existing notes and no independent remote changes, publication brings Donut to the local tip with the same commit IDs and order; a clean receiving checkout obtains that history and content.
- **Value / learning:** Directly closes the explicitly identified remote-behind gap. Receiving already-accepted linear history is recorded as delivered; do not invent a duplicate catch-up story.
- **Scope:** Existing-note content commits are the first delivery promise; structural-history expansion is deferred. Divergence is outside this direction. Failure must preserve the local chain and leave any accepted progress explicit and retryable.
- **Effort hypothesis:** L, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-21"></a>

### 21. Keep web saves in an append-only Git history

- **For / why:** An owner can rely on previously created Git commits remaining unchanged while editing notes in Donut.
- **Evaluation:** Given consecutive changed web saves of a note, each durable Git change appends to the previous tip without replacing a commit, whether or not a client has downloaded it; later pull retains the chain.
- **Value / learning:** The prior batching contract permits amending unexposed tips, which conflicts with the newly stated strictly append-only direction. This is a proposed behavior change, not unfinished old batching work.
- **Scope:** No new history UI or migration rewriting old commits. Refinement must explicitly reconcile the existing batching behavior with this delivery; do not treat the old amendment policy as authority to rewrite new history.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-22"></a>

### 22. Receive a web note rename locally without losing its identity

- **For / why:** An owner can rename a learned note in Donut and continue refining it locally.
- **Evaluation:** Given a synchronized notebook and a local checkout with no unpublished work, a web rename appends accepted history; pull receives the renamed path, and a later local content publication retains the same learned note.
- **Value / learning:** Closes an ordinary web action that otherwise interrupts the shared editing loop. Web identity is known, avoiding inference from a local rename-with-edit.
- **Scope:** One notebook; local history is behind or equal. Preserve authored-reference semantics. No divergent reconciliation or cross-notebook transfer.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-23"></a>

### 23. Receive a web note deletion locally

- **For / why:** An owner can remove an obsolete note in Donut and have the local notebook reflect that decision.
- **Evaluation:** Given synchronized state and no unpublished local work, deleting a note on the web appends accepted history; pull removes its Portable file while retaining existing deletion and private-data semantics.
- **Value / learning:** Prevents deleted content remaining in the owner's active local knowledge set and blocking subsequent refinement.
- **Scope:** Ordinary-note deletion, without restoring deleted notes, reusing reserved deleted paths, or reconciling independent local edits.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-24"></a>

### 24. Publish edits to existing notebook and folder Readmes

- **For / why:** An owner can refine existing container descriptions locally and use the updated descriptions in Donut.
- **Evaluation:** Given represented notebook and folder Readmes, a direct-child commit edits their valid content; publication preserves the authored bytes and existing container identities, visible in Donut and a receiving checkout.
- **Value / learning:** Makes ongoing container authorship useful after initial publication; initial creation alone does not deliver this outcome.
- **Scope:** Existing represented containers. Container creation, removal and movement are separate concerns; no divergence or broad import promise.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-25"></a>

### 25. Receive a web note move locally without losing learning history

- **For / why:** An owner can organize a note between existing folders in Donut and continue local refinement at its new path.
- **Evaluation:** Given synchronized state and a clean local checkout, a web move within the notebook appends accepted history; pull receives the new location and subsequent local editing keeps the original note's learning data.
- **Value / learning:** Completes another common organizational step in the same editing loop, independently useful after rename synchronization.
- **Scope:** Existing represented destinations within one notebook; no new-folder or folder-subtree move promise, cross-notebook transfer, or divergent reconciliation.
- **Effort hypothesis:** M, low confidence pending focused refinement.
- **Depends on:** Existing clone, publication and clean fast-forward pull; no dependency on another new story is assumed.
- **Safe stopping point:** This workflow remains independently usable if later stories are cancelled, with work and learning data preserved.

<a id="story-26"></a>

### 26. Reconsider note-title uniqueness and soft deletion with Git versioning

- **For / why:** Notebook owners working between Donut and Git-compatible
  tools need deletion and title-reuse rules that support continued authoring.
- **Evaluation:** Confirm whether a soft-deleted note still reserves its title
  within its folder, identify whether this is enforced by a database unique
  index or application rules, and demonstrate the resulting effect on Git
  workflows. Produce an evidence-backed recommendation for title reuse and
  whether soft deletion should be retained, changed, or removed given Git
  versioning, leaving the product decision explicit.
- **Hypothesis, not confirmed:** A soft-deleted note may still hold its folder's
  unique title and prevent another note from using it, potentially blocking
  Git-compatible workflows. This capture does not establish the current behavior
  or the claimed incompatibility.
- **Key examples to investigate:** Delete a note, then create another note with
  the same title in that folder; publish a local Git change reusing the deleted
  note's path. Establish what happens to note identity and learning history,
  and what recovery Git history provides compared with soft deletion.
- **Scope:** Investigation and a product recommendation. Consider retaining
  current behavior, changing title reservation while keeping soft deletion,
  and replacing soft deletion where Git history provides sufficient recovery.
  Whether Git history covers all affected notes and recovery needs remains an
  open question. Implementation, migration, and removal of soft deletion are
  not authorized by this capture. Story 23 keeps its existing scope pending
  an explicit decision.
- **Value / learning:** Resolve the suspected title-reuse obstacle and whether
  soft deletion still serves owners alongside Git versioning before choosing
  a behavior change.
- **Effort hypothesis:** S–M, low confidence until investigation is bounded
  during refinement.
- **Depends on:** No new story prerequisite is established.
- **Safe stopping point:** Verified current behavior and a recorded recommendation
  remain useful even if implementation is deferred.
- **Source:** Owner's 2026-09-12 request to capture this reconsideration as the
  third product backlog story; both the suspected constraint and the future
  of soft deletion remain open questions.

## Ordering and Scope Reduction

The [product backlog](../PRODUCT-BACKLOG.md) owns selection and order.
The owner subsequently selected story 26 as second in the global queue, ahead
of the remaining Git workflow stories below.
Publish accumulated commits first, then make new web history append-only.
Prioritize web rename and deletion ahead of container descriptions and web
relocation. Drop the latter two from the queue first if learning changes the
priority; retain their candidates here. None of this authorizes execution.

## Deferred Directions

Keep these outside the current queue rather than cancelling them:

- Broader reconciliation of independently advanced local and remote histories,
  including multiple local commits, structural changes, and conflict recovery.
  Earlier proposals used ordinary Git rebase; the current direction excludes it.
- Recovery for notebooks whose live projection already differs from accepted
  history. Establish the owner's blocked journey and a deliberate preservation
  policy before selecting recovery work.
- Deleted-path reuse and restoration; wider folder operations and
  rename-with-content-edit identity decisions.
- Native standard Git transport, notebook binding within a project subdirectory,
  attachments, and history browsing or revision restoration.

Broader web-authoring coverage is retained in
[SEED-017](SEED-017-cohesive-design-corrections.md#open-product-decision).
The old proposal's rebase model and web-tip amendments are not constraints on
the newly selected append-only stories.

## Open Decisions

- How often do web renames, deletions and moves interrupt actual owner work?
  The order above is a value hypothesis, to revise with use.
- Multi-commit publication refinement must settle useful failure/retry behavior
  if an intermediate commit is invalid, without discarding or rewriting work.
- Additional web creation modes and container mutations need concrete owner
  journeys before story selection; this queue is not a completeness claim.

## Breadcrumbs

- Owner's 2026-09-12 direction clarification and cleanup/backlog request.
- Accepted ADR 0004 defines Portable content; Proposed ADR 0002 remains a
  broader, non-binding direction.
