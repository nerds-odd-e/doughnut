# Note stored type leftover

**Status:** planned (do not execute until asked)  
**Feeds:** production persist + backfill of `type: Note` / `type: Relationship` (spent `.planning/quick/005-note-concept-type`)

## Why this exists

A review of that implementation found persist holes and overlapping tests. Stored type is already the product contract; these leftovers can un-type a note or pin the same YAML twice.

## In scope

- Persist paths that write `note.content` without `NoteConceptType.ensureStoredType`
- `Note.prependContent` putting Wikidata text in front of the leading fence
- Persist/E2E tests that only re-assert the helper YAML matrix
- Test composers still emitting `type: relationship`

## Out of scope

- Rewriting Flyway `V300000269` / `V300000270`
- Deleting `NoteConceptTypeBackfill` or its placeholders (still invoked on migrate)
- Deleting `NoteConceptTypeTest` (YAML matrix is the helper’s domain-stable contract)
- Deploy health-probe timeout (MIG recreate + Flyway exceeded 3 minutes on 270; separate ops)
- Accepting ADR 0004; remaining OKF C2–C3 / P1–P10 / D1
