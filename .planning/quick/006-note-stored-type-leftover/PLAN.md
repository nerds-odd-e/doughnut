# Plan: Note stored type leftover

**Status:** in progress (slice 1 done)

**Goal:** Every product write of `note.content` keeps a leading stored type. Tests pin that once per boundary, not twice.

## Design

- `NoteConceptType.ensureStoredType` stays the only YAML policy. New persist holes call it; do not copy fence strings.
- `Note.prependContent` delegates to `NoteLeadingFrontmatter.prependToBody`: closed leading fence stays in place; unfenced content keeps “addition then previous”.
- Helper YAML matrix stays on `NoteConceptTypeTest`. Controller/E2E keep one wiring or user scenario each; drop siblings that only re-pin the matrix.
- Test composers that **write** relationship markdown use `type: Relationship`. Inputs that prove canonicalize-on-save may stay lowercase.

## Slices

### 1. Wikidata create keeps type as the leading fence — Behavior — done

Wikidata location prepend lands in the body after a closed `type: Note` fence. Assertion is on the existing location-create test in `NotebookRootNoteCreationWithWikidataTests`.

**Learning:** fence-aware prepend belongs on `NoteLeadingFrontmatter.prependToBody`, not inline in `Note.prependContent`.

### 2. Extracting a note keeps type on the original — Behavior — planned

Pre-condition: source note has stored type (or body-only AI remainder).  
Trigger: `createExtractedNote`.  
Post-condition: original note content is `prepareContentForSave` then `ensureStoredType` (same as the new note). Body-only remainder becomes `---\ntype: Note\n---\n` plus that body.

Controller: `AiControllerCreateExtractedNoteTest` (`shouldCreateExtractedNoteFromSourceNote` currently expects untyped remainder).

### 3. Drop persist tests that only re-pin the helper YAML — Structure — planned

Enables a smaller leftover surface for slice 4. Existing tests still pass; no product change.

Delete controller tests that duplicate `NoteConceptTypeTest`: empty wrap, insert-type-first on save, canonicalize `relationship` on save. Keep `shouldBeAbleToSaveNoteWhenValid` (save wiring) and already-typed frontmatter round-trip.

Fold E2E `type: Note` into the existing create scenarios in `note_creation.feature`; delete the two dedicated sibling scenarios that only re-create to assert type. Keep `note_edit.feature` save-without-frontmatter and `add_relationship.feature`.

### 4. Relationship test composers write type: Relationship — Structure — planned

No product change. Writers match compose/backfill: `RelationshipNoteMarkdown`, E2E `testability.ts` relationship markdown, frontend relationship test fixtures (`relationshipNoteTestContent.ts`, `relationNoteReduceOnDelete.spec.ts`, `propertyRelationImageIndexTestSupport.ts`, `relationTypeOptions.spec.ts` parse samples). Reads stay case-insensitive. Javadoc on `PropertyKeyNaming` uses `type: Relationship`.

## Out of scope

- Flyway 269/270 / `NoteConceptTypeBackfill` / placeholders
- Helper YAML tests
- Deploy health-probe retries
- ADR 0004 accept; OKF C2–C3, P1–P10, D1
