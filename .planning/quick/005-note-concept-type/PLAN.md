# Plan: Note concept type

**Status:** in progress (slice 5 next)

**Goal:** Ordinary notes persist `type: Note`. Relationship notes persist `type: Relationship`. Existing `note.content` is backfilled. ADRs describe that stored shape.

## Design

- One persist scenario per slice. Extend the same surgical helper; do **not** finish every YAML branch in the first persist slice.
- Call the helper only on **note** persist (`NoteConstructionService`, `TextContentController.updateNoteContent`). Never from `AuthoredNoteContent.prepareContentForSave` (readme uses that too).
- Title-only create initializes `content` via `NoteConceptType.ensureOrdinaryNoteType` in `NoteConstructionService.createNote` (null → leading `type: Note` fence).
- Leave `makeMe` content defaults alone. Typeless fixtures stay valid for later save/backfill tests.
- Structure slices sit immediately before the behavior they unlock (ADR 0004 before `type: Note`; ADR 0001 / Relationship spelling before compose).
- Next Flyway: **next unused** `V300000xxx` at execute time. `V300000267`–`V300000268` are taken (004-recall-log).
- Backfill is **gated** (`1=0` default; `1=1` only on the production deploy). Helper tests are permanent. Gate tests are temporary (slice 10).

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

### 5. Persisting frontmatter without type inserts type: Note first — Behavior — planned

**Pre:** Leading fence with other keys (`parent`, `aliases`, …) and no `type` (including “under current” create).  
**Trigger:** Create or save that content.  
**Post:** `type: Note` is the first key; the rest of the fence and body are verbatim. A note that already has a non-empty `type` is unchanged (delta test only). Blank `type` counts as missing.

E2E: `note_creation.feature` “under current” — assert `type: Note` without re-checking `parent`. Controller save with aliases is the save delta. Surgical insert only; no Relationship rename.

### 6. Lock type: Relationship in ADR 0001 and ADR 0004 — Structure — planned

Glossary **Relationship note** becomes `type: Relationship`. ADR 0004 uses that spelling. Fix the ADR index row for 0001 if it still says Proposed. Enables slice 7. Code still writes `relationship` until slice 7.

### 7. Adding a relationship writes type: Relationship — Behavior — planned

**Pre:** Two notes; add a relationship.  
**Trigger:** Create the relationship note.  
**Post:** Composed markdown has `type: Relationship`. Reads still match case-insensitively.

E2E: `add_relationship.feature`. Frontend compose + fixtures that assert composed markdown. Do not normalize on save. Do not backfill.

### 8. Saving a Doughnut type canonicalizes its spelling — Behavior — planned

**Pre:** Note whose `type` is `relationship` / `RELATIONSHIP` / `note` / `NOTE` (quoted or not), or some other non-empty type.  
**Trigger:** Save.  
**Post:** Doughnut types become `Relationship` / `Note` in place. Any other non-empty type is unchanged.

Controller/unit on the helper. New relationship notes (slice 7) are not re-asserted. No backfill.

### 9. Existing notes are backfilled — Behavior — planned

**Pre:** `note.content` rows from before slices 2–8 (empty, body-only, fence without type, `type: relationship`, unknown type).  
**Trigger:** Flyway Java migration with gate enabled.  
**Post:** Same rules as the helper. Default gate `1=0` changes nothing. Readme columns untouched.

Temporary tests: default no-op; enabled selects `note.content` only. Do not duplicate the YAML matrix. **Jidoka:** Flyway version vs 004-recall-log; record enable/revert in STATE.

### 10. Close C1/D2 on the tracker; drop the migration harness after production — Structure — planned

**Jidoka:** Human confirms production applied the backfill and the override is back to `1=0`.

Mark C1/D2 closed in [OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md) and SEED-003. Delete the migration-only gate tests. Do not rewrite the Flyway file. When this plan is fully executed, remove spent planning history from this directory.

## Out of scope

- D1 / C2 / P7, P1–P5, P8–P10, optional OKF families
- Accepting ADR 0004
- Git accept (ADR 0002)
- Inferring Relationship from `source`/`target`/`relation` when `type` is missing
- Changing `makeMe` default note content
