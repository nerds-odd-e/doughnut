# Web note trash and immediate undo

Status: awaiting evidence-based slice revision
Source: [SEED-009, story 29](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-29)
Authority: User requested updating the plan with the simplify-before-resplitting
assessment and explicitly instructed not to execute yet. No implementation,
commit, or push is authorized by this update.

## Goal and boundaries

Replace the web note Delete/Undo interaction with path-preserving trash and
immediate undo, keeping note identity, history, tracking preferences, reference
choices, direct access, and existing legacy recovery. New notes may reuse the
old path; repeated trashing preserves both identities with numbered suffixes.

Story 34 owns improved discovery of older trash through navigation and remains
unrefined. Preserve current views and direct access here. Story 32 owns Restore;
story 31 owns legacy migration and removal of note `deleted_at`; story 33 owns
folder Trash. Story 28 alone owns new trash/Git/local compatibility. No new
permanent-delete action, trash dashboard, persistent restore journal, metadata,
expiry, migration, or folder Trash button is in this plan.

## North Star and current decisions

Follow the [parent North Star and continuity contract](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#north-star-and-completion-boundary).
Accepted [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash)
owns root `_trash` membership and eligibility; [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md)
keeps direct routes identity-based. [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
governs retained scheduling/history; [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
governs failure handling. ADR 0002 remains Proposed; no new Git contract is
introduced here. Preserve currently supported callers and storage behavior.

Before revising implementation slices, check whether the independently Taken
memory-tracker simplification has landed and inspect its actual result. Do not
assume planned changes are implemented or reintroduce removed tracker state.

Root ancestry determines trash membership, without stored trash flags or dates.
During coexistence a legacy-deleted note also remains unavailable. Centralize
that distinction at note availability, preserving independent authorization,
notebook settings, tracking preferences, type filters, ordering and limits.
Availability is not permission to erase a note from direct reads or storage.
Do not reuse a learning/search filter for trash content persistence/export.

Undo reverses the performed action, including its collision rename, through the
existing ephemeral history and placement semantics. A later Restore is different:
it strips the current visible prefix and keeps suffixes. Undo conflicts preserve
the note in trash and report the ordinary conflict; no overwrite. Reference
properties explicitly removed on trash are not reconstructed by Undo.

## PFE findings and implementation responsibilities

- `FolderTrailSegments` already traverses root ancestry; `FolderSubtree` walks
  descendants; `FolderRepository` owns parent/child queries. Reuse the hierarchy,
  not a second path or timestamp record. Root matching is case-insensitive.
- `FolderConstructionService` and `FolderSiblingNameValidation` own parent
  placement and naming checks. Expose the suitable domain operation if necessary
  instead of importing controller DTO orchestration into a second folder builder.
- `NoteMotionService` owns placement. Reuse/change it for the current operation;
  trash uses the existing deletion-reference policy, not move link rewriting.
  Numbered incoming-name allocation is the bounded missing responsibility.
- `NoteService`/`NoteReferenceHandling` own current delete transformations.
  Resolve references before movement changes their meaning. Preserve both
  property cleanup and eligible relationship reduction with existing proofs.
- `NoteRepository`, `NotePropertyIndexRepository`, `MemoryTrackerRepository`,
  alias/embedding/structural-peer queries, and `WikiLinkResolver` consume activity.
  Consolidate the note condition within those existing owners; do not create
  a second resolver or universal eligibility engine.
- `NoteEditingHistory`, `StoredApiCollection`, `NoteUndoButton`, and the existing
  move-undo path supply the UI/history lifecycle. Existing history records carry
  original placement; extend that same ephemeral record for original title only
  as needed to reverse a collision suffix. Keep legacy Undo working.
- `NoteTextContent` already renders the deletion warning. Reuse the current view
  and a derived trash indication; any changed wire shape uses generated API code.

## Continuity and query-shape concern

Preparatory leaves preserve currently exposed behavior. Do not progressively
activate trash in search, learning, then links across released boundaries.
Prepare their common condition first; switch the public operation only with
placement, availability, direct warning, and Undo ready together. No feature flag
or permanent parallel lifecycle is prescribed. If preparatory code becomes an
unused framework, revise the cut rather than accumulate speculative machinery.

The hierarchy is adjacency-based, and consumers include both JPA and native SQL.
The inspected code does not establish one existing database trash predicate.
Before changing readers, choose a derived hierarchy query shape that filters
before pagination/counts, handles root notes and descendants, and preserves
current query semantics. Existing hierarchy traversal plus query parameters is
an available baseline; neither a persisted cache nor an unproved recursive SQL
feature is assumed by this plan. If an engine-specific alternative is chosen,
first prove it on the isolated test database and record its exact command and
result here. No database experiment or runtime performance claim was made during
planning. This is a remaining implementation/sizing concern, not a product choice.

## Simplify before resplitting

Keep the current two-story boundary: immediate trash/Undo here, discovery of
older trash in unrefined story 34. A slice-count threshold is a warning, not proof
that the user outcome should be split again. Search, recall, and link consistency
are parts of the same usable trash behavior, not separate user stories.

The former 17-slice sequence is withdrawn as an implementation prescription.
It allocated 16 preparatory leaves by technical consumer, left the shared query
approach unresolved, and assumed the final integration would be small. That did
not establish 17 necessary changes. Do not execute or merely regroup those old
headings to make the count look smaller.

Seventeen leaves could still be justified by concrete necessary changes, bounded
proof loops and safe stopping points. Prefer a coherent story over artificial
search/learning stories. First remove speculative work, identify reuse, and
settle the shared implementation; then assess the resulting size.

## Next planning assessment

This is the next planning activity when work resumes, not an implementation
slice, product feature, or authorization to execute. Record its findings in this
same plan, then replace the pending implementation section below.

1. **Read the actual tracker-simplification result.** Identify which availability
   checks and query patterns now exist. If that prerequisite is unfinished,
   record it and keep dependent implementation pending rather than guessing its
   final structure.
2. **Select the smallest coherent availability solution.** Follow the PFE
   findings above and inspect the actual hierarchy/query boundaries. Explain how
   the solution supports JPA/native consumers, filters before paging/counts,
   retains direct/content access, and preserves legacy behavior during coexistence.
   Do not assume a new universal filter, cache, or recursive SQL feature is needed.
3. **Classify consumers by evidence.** For each affected responsibility, record
   its current owner, required production change (if any), sufficient existing
   regression proof, and genuinely missing observation. A preservation obligation
   does not automatically require its own implementation slice or new test.
4. **Trace one actual trash/Undo journey.** Establish the required placement,
   reference handling, identity/history retention, warning, and session Undo
   changes through existing owners. Identify the real public transition and its
   safe boundary; do not presume a final one-line activation or require dormant
   frameworks to make that presumption work.
5. **Rewrite and size concrete slices.** Each leaf must own a demonstrated
   Behavior or necessary Structure change and a bounded proof loop. Keep required
   preservation live at every story boundary, preferably every slice boundary.
   Place necessary preparation immediately before the behavior it enables.
   Account for integration work instead of hiding it in the last leaf.

Assessment completion means the selected solution is described with code/query
and proof evidence; necessary changes are separated from verification-only
coverage; all story promises map to concrete slices; and the public transition
has a plausible, safe implementation boundary. No current-runtime performance
claim is supported until measured. Use isolated representative proof if the
selected design depends on uncertain engine-specific behavior.

## Implementation slices — pending that assessment

No replacement count or technical-consumer sequence is prescribed yet. Populate
this section from the assessment before starting product changes. Keep the same
story unless evidence identifies an independently valuable, safe smaller outcome.

Use the repository's ~5-minute slice target; scrutinize >5 minutes and
finer-decompose >10 minutes unless an explicit good reason applies. Required
external test-suite wait may exceed active implementation time and must be
reported separately. Fewer headings alone do not establish simplification.

If the evidenced plan still exceeds 15 leaves, retain the guideline's resplit
recommendation and evaluate it against actual user value and continuity. Keep
more leaves only with concrete justification. Do not split participation rules
into separately broken trash releases, remove agreed behavior, or expand/refine
story 34 or the deferred Git story to accommodate implementation convenience.

## Proof obligations to map into the revised slices

These are preserved requirements, not a list of mandatory new tests or slices.
Reuse sufficient existing tests, and assign each promise a final owning slice
when the concrete implementation sequence is written.

| Promise | Required observable evidence and existing starting point |
| --- | --- |
| Web trash and immediate Undo retain the same learned note | Actual web action/Undo in note_deletion.feature, supported by NoteControllerDeleteTests; retain identity/history and stopped-tracking preference. |
| Search, learning, and wiki matching follow location | Trash through the real mutation, then observe existing search/recall/assimilation/reference boundaries; undo restores availability. Include descendants and case-insensitive root matching where they distinguish the rule. |
| Full-path placement, parent creation, collision suffixing, old-name reuse | Create/trash twice; observe distinct retained identities and first free suffix, without changing earlier trash. |
| Reference choices remain effective | Existing delete-reference and reduce-to-source controller tests establish the selected transformation; Undo does not reconstruct explicitly removed properties. |
| Direct URL, warning, editing/Move and post-action navigation | Existing note view/controller and store tests retain authorized access and existing navigation; the warning reflects trash membership. |
| Legacy recovery and unrelated active behavior stay alive | Existing deletion/recovery and affected query regression proof runs against the selected replacement; no migration occurs in this story. |
| Undo placement/title and occupied-target safety | Existing session-undo/placement boundaries observe successful reversal or an ordinary conflict leaving the note retained in trash. |
| Existing storage and supported callers remain working | Reuse relevant existing persistence/caller proof; no new Git compatibility journey is included. |

Preparatory proof cannot substitute for the real web-triggered final state. Do
not count a fixture that starts trashed as proof that the web action creates
valid trash. Keep proof at the highest useful stable boundary without repeating
all assertions at every layer.

## Verification and delivery contract

Backend changes: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
(all backend unit tests required by backend.mdc).
Frontend changes: `CURSOR_DEV=true nix develop -c pnpm frontend:test`
(full suite after focused iteration).
Web outside-in proof:
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_deletion.feature`
The wrapper owns its test stack. No manual testing or full E2E suite is requested.
Generated API when needed: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.

On later authorized execution, each delivery follows Jidoka, fresh
`dough-post-change-refactor` agent, API generation if needed, one coordinator
`./scripts/run.sh pnpm format:changed`, plan update, commit/check-only hook, push
and asynchronous CI handling. Keep this plan through retrospective/wrap-up.
Planning alone runs none of that execution workflow.

## Current assessment and resume point

The story boundary remains coherent; there is no demonstrated reason to resplit
it solely because the withdrawn plan had 17 leaves. The issue is insufficiently
established implementation structure and mechanically allocated preservation
work, not evidence of 17 irreducible changes.

Next action: perform the planning assessment above and write the concrete
replacement slices in this file. This update records that requirement; it does
not claim the assessment, query validation, or implementation has occurred.
The story stays queued, story 34 stays second and unrefined, and implementation
remains unstarted. The user's current instruction explicitly prohibits execution.
