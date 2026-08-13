# CONTEXT: Remove note-level skip memory tracking

Ad-hoc removal (not a roadmap milestone). Resume here + `PLAN.md`. Progress: [STATE.md](./STATE.md) in this directory — do not edit `.planning/STATE.md`.

## Product intent

Remove **note-level** `skipMemoryTracking` (`NoteRecallSetting` / `note.skip_memory_tracking`): the recall-settings checkbox, queue filters, wiki-link gate special case, API field, testability inject column, DB column, and Proposed ADR 0001 wording that still describes skip on a note.

**Keep:**

- Notebook `skipMemoryTrackingEntirely` (settings label **Skip Memory Tracking**, Bazaar subscribe gate)
- Assimilation **Skip recall** / tracker **Remove from recall** / **Revive**
- `AssimilationRequestDTO.skipMemoryTracking` (that action’s payload; **not** the note setting)

**Existing flagged notes:** no backfill. After the flag is gone they are ordinary unassimilated notes.

## Two different `skipMemoryTracking`s

| Name | Layer | Fate |
|------|--------|------|
| `NoteRecallSetting.skipMemoryTracking` | Note embeddable + `note.skip_memory_tracking` | **Remove** |
| `AssimilationRequestDTO.skipMemoryTracking` | Skip-recall action → `MemoryTracker.removedFromTracking` | **Keep** |
| `NotebookSettings.skipMemoryTrackingEntirely` | Notebook | **Keep** |

E2E `notebookPage.skipMemoryTracking()` is the **notebook** checkbox. Keep it.

## Remaining note-flag surface (after Phases 1–3)

Gone: recall-settings checkbox, `NoteBuilder.skipMemoryTracking()`, inject that set the flag (inject now skip-recalls).

Still present:

- **Queues:** `NoteRepository.recallWhereClause` and `NotePropertyIndexRepository` still skip rows when the flag is true (Phase 4).
- **API / embeddable:** `NoteRecallSetting.skipMemoryTracking` (Phase 5).
- **E2E inject column** `"Skip Memory Tracking"` on `NotesTestData` (Phase 6).
- **DB:** `note.skip_memory_tracking` (Phase 7).
- **Proposed ADR 0001** still describes skip on a note (Phase 8).

CLI and MCP do not expose the note flag.

## Wiki-link gate after removal

Today a flagged target unblocks property assimilation without a tracker. After removal, a target unblocks only if it has a note-level tracker (including skipped) or is deleted — same as any other note.

## ADR 0001

Proposed [0001-ubiquitous-language.md](../../docs/adrs/0001-ubiquitous-language.md) currently says skip recall opts **a note or notebook** out, and lists “skip recall / Skip Memory Tracking” as two names for one concept. After this work those are different layers (tracker action vs notebook setting). Update the Proposed draft for consistency; **leave Status Proposed** (humans own Accept).
