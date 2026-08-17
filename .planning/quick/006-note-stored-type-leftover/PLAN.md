# Plan: Note stored type leftover

**Status:** in progress (slices 1–3 done)

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

### 2. Extracting a note keeps type on the original — Behavior — done

`createNoteFromExtractedSuggestion` persists original and new-note content through `persistNoteContent` (`prepareContentForSave` then `ensureStoredType`). Body-only remainder is stored as `---\ntype: Note\n---\n` plus that body.

**Learning:** the original-note write was the remaining persist hole; renaming `persistCreatedNoteContent` → `persistNoteContent` matches both callers.

### 3. Drop persist tests that only re-pin the helper YAML — Structure — done

Deleted controller siblings that only re-pinned helper YAML (empty wrap, insert-type-first, canonicalize `relationship`). Kept `shouldBeAbleToSaveNoteWhenValid` and already-typed frontmatter. Folded `type: Note` into the two existing create scenarios in `note_creation.feature` after their original post-conditions. Left `note_edit.feature` and `add_relationship.feature` alone.

**Learning:** folder-create navigates away from the note page, so the type check must reopen the editor on the created note rather than sitting between tree and folder assertions.

### 4. Relationship test composers write type: Relationship — Structure — planned

No product change. Writers match compose/backfill: `RelationshipNoteMarkdown`, E2E `testability.ts` relationship markdown, frontend relationship test fixtures (`relationshipNoteTestContent.ts`, `relationNoteReduceOnDelete.spec.ts`, `propertyRelationImageIndexTestSupport.ts`, `relationTypeOptions.spec.ts` parse samples). Reads stay case-insensitive. Javadoc on `PropertyKeyNaming` uses `type: Relationship`.

## Out of scope

- Flyway 269/270 / `NoteConceptTypeBackfill` / placeholders
- Helper YAML tests
- Deploy health-probe retries
- ADR 0004 accept; OKF C2–C3, P1–P10, D1
