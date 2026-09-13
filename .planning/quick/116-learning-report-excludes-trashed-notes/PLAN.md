# Exclude trashed notes from commissioned learning reports

Status: planned
Source: bounded correction from the retrospective of
[SEED-009, story 29](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-29)
and its completed [execution plan](../115-web-note-trash-and-undo/PLAN.md).
Authority: Planning only, authorized by the 2026-09-13
`dough-execution-retrospective` instruction. No implementation, commit, push,
or backlog change is authorized.

## Execution provenance and current finding

Reviewed execution identity: plan 115 at first implementation commit
`2be6138738`, on `codex/115-web-note-trash-and-undo`.

Reviewed implementation commits:

- `2be6138738` — Name note availability consistently
- `195d80d919` — Share native note availability predicate
- `241d4cdbc3` — Derive trash membership from folder location
- `3db481941d` — Exclude trashed notes from active participation
- `0eb8e717b0` — Reuse folder construction for trash paths
- `f3344c1d72` — Add reversible note placement
- `73f28fdf0c` — Reuse note reference deletion policy
- `4d9c7cfce2` — Add atomic note trash and undo API
- `de5c3e161d` — Use note trash and immediate undo on web

Planning-only provenance is `423199b4b1`, `ebd4839d26`, and `b1a571154b`.

The completed execution correctly kept
`NoteRepository.findLiveNotesByNotebookIdOrderByIdAsc` inclusive of trashed
notes because Git state loading, export, health checks, and ordinary retained
content need those files. `LearningSessionService.record` also uses that
storage-oriented query to match report titles. Unlike due-work selection, this
recording path does not subsequently check `Note.isAvailable()` or
`MemoryTracker.isActive()`. A commissioned report can therefore append a
RecallLog and reschedule a trashed note, contrary to story 29 and
[ADR 0004's trash rule](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash).

## Goal and bounded scope

When a learner submits a commissioned learning-session report naming a trashed
note, Donut rejects that entry and leaves its tracker and recall history
unchanged. Active commissioned notes continue to record normally.

Included:

- Make commissioned-report title matching use the existing shared
  location-aware note availability rule.
- Keep the storage/export meaning of the existing `findLiveNotes...` query
  unchanged.
- Add focused controller-boundary regression proof for a trashed commissioned
  note and retain the existing active-note recording proof.

Excluded:

- Changes to trash placement, browser Undo, discovery, Restore, migration,
  folder Trash, Git synchronization, or report syntax.
- A general repository-query rename or cleanup of unrelated legacy
  `deleted_at` readers.
- New failure-catching behavior; the existing rejected-entry outcome remains
  the business response, consistent with
  [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md).

## PFE and architecture

Reuse `Note.JPA_AVAILABLE`, already shared by search, assimilation, recent-note,
and tracker queries. The gap is a boundary-specific available-note collection
for commissioned report matching. Add or expose the smallest repository query
that applies that predicate for one notebook; do not change the distinct
storage/export query whose inclusive meaning is independently justified.

This preserves one availability representation while keeping storage inclusion
and learning participation as separate domain responsibilities. It follows
[ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md),
where commissioned reports are Grades on matched commissioned trackers, and
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash),
where trashed notes retain tracker history but do not participate in learning.
ADR 0002 remains Proposed and is not a constraint for this correction.

## Key proof

| Pre-condition | Trigger | Required result |
| --- | --- | --- |
| A commissioned note is beneath the notebook-root `_trash` folder and has no prior RecallLog for the submitted time. | The learner records a report containing that note title and a valid Grade. | The entry is rejected as having no eligible commissioned tracker; no RecallLog is appended and tracker scheduling state is unchanged. |
| A commissioned note is active. | The learner records the same valid report shape. | Existing recording, RecallLog creation, and scheduling behavior remain green. |

## Ordered slices

### 1. Trashed commissioned notes cannot be graded by report
Type: Behavior
Status: planned
Size: about 5 minutes; one repository selection change and one controller proof
loop, with backend-suite wait outside active work.
Proof: `LearningSessionRecordTests` exercises the public controller record
boundary for a commissioned note under root trash and observes a rejected entry,
no new RecallLog, and unchanged tracker state; its existing active-note scenario
continues to record and schedule normally. Run
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Behavior: Given a commissioned note whose folder location makes it trashed,
when its title appears in a submitted commissioned learning report, the report
does not Grade that tracker and returns the existing ineligible-entry outcome.

Use a repository selection that composes `Note.JPA_AVAILABLE` for this learning
boundary. Do not narrow `findLiveNotesByNotebookIdOrderByIdAsc`; its inclusive
storage/export callers must continue seeing retained trashed files.

## Proof ownership and delivery

The single Behavior slice owns both correction promises: trashed entries are
not recorded, and active entries remain recordable. Existing plan-115 tests
continue to own location membership, due recall exclusion, search/wiki
eligibility, trash placement, and web Undo; do not duplicate those journeys.

If separately authorized, execution follows the repository's ordinary Jidoka,
fresh post-change refactor, one coordinator `./scripts/run.sh pnpm
format:changed`, plan update, commit, push, and applicable CI handling. This
plan creates no execution authority.

## Planning assessment

No remaining slice-specific concerns were identified. The correction has one
observable outcome and one proof loop. The important design boundary is explicit:
learning matching uses availability, while Git/export retention keeps its
inclusive live-note query.
