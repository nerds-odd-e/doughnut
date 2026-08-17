# Plan: Note concept type

**Status:** in progress (V300000270 prod backfill — gate `1=1` this deploy)

**Goal:** Ordinary notes persist `type: Note`. Relationship notes persist `type: Relationship`. Existing `note.content` is backfilled. ADRs describe that stored shape.

## Design

- One persist scenario per slice. Extend the same surgical helper; do **not** finish every YAML branch in the first persist slice.
- Call the helper only on **note** persist (`NoteConstructionService`, `TextContentController.updateNoteContent`). Never from `AuthoredNoteContent.prepareContentForSave` (readme uses that too).
- Title-only create initializes `content` via `NoteConceptType.ensureStoredType` in `NoteConstructionService.createNote` (null → leading `type: Note` fence).
- Leave `makeMe` content defaults alone. Typeless fixtures stay valid for later save/backfill tests.
- Structure slices sit immediately before the behavior they unlock (ADR 0004 before `type: Note`; ADR 0001 / Relationship spelling before compose).
- Flyway: **V300000269** applied on production as a 13ms no-op (`1=0`). **V300000270** is the same helper; do not rewrite 269.
- Backfill is **gated** (`1=0` in non-prod; `1=1` in `application-prod.yml` for the deploy that first applies 270). Revert prod to `1=0` after that apply. Helper tests are permanent. Gate tests are temporary (slice 10).

## Slices

### 1. Lock stored type: Note in ADR 0004 — Structure — done

Decision / Bundle and concepts now: stored note markdown carries `type` and valid YAML; ordinary notes use `type: Note`; relationship notes are concepts (`type: relationship`); OKF unknown types allowed; author-owned / unknown keys preserved on persist and round-trip. Status still Proposed.

**Learnings:** The existing Decision already mentioned `type: Note` for the portable tree. The lock was stored markdown + relationship-as-concept. Split those claims so valid YAML is not ordinary-note-only. `type: relationship` spelling unchanged until slice 6.

### 2. Creating a title-only note stores type: Note — Behavior — done

Title-only create persists `---\ntype: Note\n---\n`. Helper `NoteConceptType.ensureOrdinaryNoteType` wraps null/empty/unclosed `---` and leaves a closed leading fence unchanged. Wired only from `NoteConstructionService.createNote` with `null` (create-with-content still overwrites until slice 4). Save/readme untouched.

E2E: `note_creation.feature` (open markdown source). Unit: wrap null/empty, unclosed fence, closed fence unchanged.

**Learnings:** Reuse `NoteLeadingFrontmatter.splitVerbatim` for closed-fence detection. Under-current create already has a `parent` fence, so it stays without `type` until slice 5.

### 3. Saving a note with no frontmatter stores type: Note — Behavior — done

`updateNoteContent` wraps with `NoteConceptType.ensureOrdinaryNoteType` after `prepareContentForSave`. Empty/body-only content gets a leading `type: Note` fence; closed fences unchanged. Readme save still has no wrap.

E2E: `note_edit.feature`. Controller: body-only, empty, fenced delta. Autosave keeps last-saved when the store already echoed wrapped content (otherwise the editor stays dirty).

**Learnings:** Persist mutates the body, so autosave must not stamp last-saved to the *sent* unwrapped text after the store has the wrap. Use `mockSdkServiceWithImplementation` for that frontend echo, not a cast `mockImplementation`.

### 4. Creating a note with a body and no frontmatter stores type: Note — Behavior — done

Create-with-content (DTO and extract new-note) goes through `persistCreatedNoteContent`: `prepareContentForSave` then `ensureOrdinaryNoteType`. Body-only markdown stores `---\ntype: Note\n---\n` plus the body. Original-note extract updates still prepare without wrapping.

**Learnings:** Title-only still uses wrap(null) on first save; create-with-content overwrites via the helper. Slice 5 insert-into-fence should extend `ensureOrdinaryNoteType` so both save and `persistCreatedNoteContent` pick it up.

### 5. Persisting frontmatter without type inserts type: Note first — Behavior — done

`ensureOrdinaryNoteType` inserts `type: Note` as the first key when the leading fence is missing or blank `type`; remainder of fence + body stay verbatim. Non-empty `type` is unchanged. Create and save pick this up through the existing helper.

E2E: under-current in `note_creation.feature`. Helper: insert/verbatim/blank/unchanged. Controller: aliases save + already-typed delta.

**Learnings:** Read `type` with `Frontmatter.parse`; edit the verbatim block (drop blank `type:` line, prepend). `VerbatimSplit.yamlRaw` is the inner YAML. Do not canonicalize spelling until slice 8.

### 6. Lock type: Relationship in ADR 0001 and ADR 0004 — Structure — done

Glossary **Relationship note** is `type: Relationship`. ADR 0004 Decision uses that spelling. Index 0001 is **Approved** (matches the ADR file). 0004 stays Proposed. Code still writes `relationship` until slice 7.

**Learnings:** 0001 file already said Approved; only the index was stale. Do not rename to `*-accepted.md` in this plan.

### 7. Adding a relationship writes type: Relationship — Behavior — done

`formatRelationshipNoteMarkdown` (`relationshipNoteCompose.ts`) writes `type: Relationship`. E2E `add_relationship.feature` and AddRelationship create-payload test assert that spelling. Save/backfill/reads unchanged (lowercase still matches).

**Learnings:** One compose constant `NOTE_TYPE`. Slice 8 canonicalizes on save; do not change compose again.

### 8. Saving a Doughnut type canonicalizes its spelling — Behavior — done

`NoteConceptType.ensureStoredType` rewrites the verbatim `type` value in place: `note`/`relationship` (any case, quoted or not) → `Note`/`Relationship`. Other non-empty types unchanged. Save and create-with-content share the helper. Compose not re-asserted.

**Learnings:** Renamed from `ensureOrdinaryNoteType` because it also canonicalizes Relationship. Fence rebuild is `VerbatimSplit.rebuild`. Slice 9 should call `ensureStoredType`.

### 9. Existing notes are backfilled — Behavior — done

Gated Java Flyway `V300000269__backfill_note_concept_type` calls `NoteConceptTypeBackfill` → `ensureStoredType` on `note.content` only. Placeholder `note_concept_type_backfill` is `1=0` in all profiles (no-op). Enable `1=1` only on the production deploy that first applies it, then revert to `1=0`. Readme columns never selected.

Temporary tests: `NoteConceptTypeBackfillTest` (default no-op; enabled updates note, leaves readmes). YAML matrix stays on the helper. STATE records the leftover.

**Learnings:** Gate is a `1=0`/`1=1` flag, not concatenated SQL. Production applied 269 at 2026-08-17 11:54:30 UTC in 13ms with `1=0`; 26,633 notes, 0 with `type: Note`. Flyway will not re-run 269.

### 9b. Apply the note type backfill on production — Behavior — in progress

Pre-condition: 269 is recorded; ordinary notes still lack `type: Note`.
Trigger: deploy `V300000270__backfill_note_concept_type` with prod placeholder `1=1`.
Post-condition: existing `note.content` has stored type (`type: Note` or canonical `type: Relationship`); Flyway 270 success; then revert prod to `1=0`.

### 10. Close C1/D2 on the tracker; drop the migration harness after production — Structure — planned

**Jidoka:** Human confirms production applied **V300000270** (not merely 269) and the override is back to `1=0`.

Mark C1/D2 closed in [OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md) and SEED-003. Delete the migration-only gate tests. Do not rewrite the Flyway file. When this plan is fully executed, remove spent planning history from this directory.

## Out of scope

- D1 / C2 / P7, P1–P5, P8–P10, optional OKF families
- Accepting ADR 0004
- Git accept (ADR 0002)
- Inferring Relationship from `source`/`target`/`relation` when `type` is missing
- Changing `makeMe` default note content
