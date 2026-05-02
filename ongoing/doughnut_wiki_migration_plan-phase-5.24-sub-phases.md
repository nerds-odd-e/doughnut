# Phase 5.24 — Remove `note.target_note_id` (split delivery)

Parent context: wiki migration Phase 5, sub-phase **5.24** in `doughnut_wiki_migration_plan-phase-5-sub-phases.md`.

This document **re-sequences** 5.24 into smaller slices aligned with `.cursor/rules/planning.mdc`: each slice is either **behavior** (externally observable) or **structure** (no observable change; tests stay green). **Dropping the `note` column and FK is the final slice**, after nothing in application code relies on them.

---

## Anchor design (unchanged from parent doc)

- Relationship notes are identified by **leading YAML** (`type: relationship`) and **`source:` / `target:`** wikilinks — not by a FK on `note`.
- **Semantic target** resolves via **`WikiLinkResolver`** (and cache rows in `note_wiki_title_cache`, which keeps its own `target_note_id` — explicitly in scope only as cache storage, not dropped here).
- Inbound references to a focal note for destroy / restore / recall extend **semantic** “points at focal” queries, scoped to relationship-shaped notes joined through the cache — not “every note that wikilinks focal.”

---

## What “SQL / targetNote cleanup” means (before dropping the column)

| Kind | Action |
|------|--------|
| **Committed Flyway history** (`V10000063__baseline.sql`, `V300000114__…`, `V300000158__…`, `V300000118__…`, etc.) | **Do not edit.** They recorded past schema/state. New work uses **new** migrations only after the runtime no longer reads the column (see **last slice**). |
| **Runtime JPQL / JDBC on `note` via `target_note_id` or `Note.targetNote`** | Eliminate in **early slices**: repository methods (`findAllByTargetNote`-style paths), Hibernate cascade predicates wired through FK. Prefer **`note_wiki_title_cache`** join + relationship-marker filter aligned with YAML predicate (**`RelationshipNoteMarkdownFormatter`** family). |
| **`note_wiki_title_cache`** (`target_note_id`, `NoteWikiTitleCache.targetNote`, JDBC `INSERT … note_wiki_title_cache … target_note_id`) | **Remain** until a later parent phase explicitly changes cache shape; optional **rename-only** refactor (`resolvedLinkedNoteId`, `:linkedNoteId`) is fine — semantics refer to **cache**, not **`note.target_note_id`**. |

---

## Recommended sub-phases (stop-safe order)

### 5.24a — Predicate: relationship-note classification (**structure**)

- **`Note.isRelation()`**: Stop branching on **`getTargetNote() != null`**; delegate to one **`RelationshipNoteMarkdownFormatter`**-adjacent predicate (frontmatter **`type: relationship`**).
- **Deliverable:** No removal of **`Note.targetNote`** mapping yet; observable behavior unchanged for notes that already have compliant frontmatter (operational data gate stays with batched migrations from parent phases).

### 5.24b — Semantic target resolver (**behavior**, narrow surface)

- **`RelationshipNoteEndpointResolver`**: `relation` + viewer → **`Optional<Note>`** via **`target:` YAML** + **`WikiLinkResolver.resolveWikiInnerTitle`**.
- Wire first at **`NoteService.refreshRelationshipNoteTitle`**, **`RelationController`** (realm), any single-call-site slice that today reads **`relation.getTargetNote()`**.

### 5.24c — Soft-delete / restore without relationship or reference cascade (**behavior**)

- **`NoteService.destroy`**: Soft-delete the focal note **without** cascade soft-delete of its **relationship notes** or **referencing notes** (remove **`findAllByTargetNote`**-driven destroy batches; adjust any **descendant** walk so relationship / reference rows are not delete fallout). **Non-relation structural children** policy stays explicit in code and tests in the same slice so behavior matches E2E.
- **`NoteService.restore`**: Drop restore logic that assumed those relationship/reference notes were deleted in the same timestamp batch; after undo, previously live edges resolve again.
- **`WikiTitleCacheService` / destroy path:** **Do not** remove or invalidate **`note_wiki_title_cache`** rows as part of note soft-delete. Cache rows remain; a soft-deleted target simply does not participate in resolution the way a live note does.
- **Observable UX:** Referencing relationship notes **stay in the tree**; wikilinks whose **semantic target** is soft-deleted **read as dead links** until the target is restored.
- **Undo delete:** Restoring the focal note returns prior resolution behavior; **every link works again** without repopulating cache from scratch (cache was never torn down for this path).
- **Tests:** Align **`NoteControllerTests`** delete / restore scenarios with the new non-cascade contract; update Cypress **`e2e_test/features/note_creation_and_update/note_deletion.feature`** (relationship + reference scenarios) so they expect relationship/reference notes to **remain listed**, with dead-link assertions where the UI exposes link state (or the narrowest observable the suite already supports).

### 5.24d — No recall setting propagation (**behavior**)

- **`NoteController.updateNoteRecallSetting`**: Apply **`BeanUtils.copyProperties`** (and the existing **`rememberSpelling` → reassimilation** branch) **only to the authorized note**. **Remove** the **`getRelationshipsAndRefers().forEach(…)`** loop that raised **related** notes’ recall **`level`** toward the submitted setting — no propagation helper, no semantic-relationship extension, and **no** replacement path that bulk-updates other notes from this endpoint.
- **Observable:** Recall level, skip-memory-tracking, and remember-spelling changes affect **that note only**; relationship and reference notes keep their prior recall settings until changed on each note individually.
- **Tests:** **`NoteControllerTests.UpdateNoteRecallSetting`**: drop or rewrite **`shouldUpdateRelationshipLevel`** / **`shouldUpdateReferenceLevel`** so they assert **no** level change on the relation when source or target is updated; keep **`shouldPutNoteBackToAssimilationListWhenRememberSpellingIsAddedLater`** (focal-only behavior).
- **Flyway:** still **before** dropping **`note.target_note_id`**.

### 5.24e — GraphRAG: no FK target on graph paths (**behavior**)

- **Scope (this slice only):** **`GraphRAGService`** / **`GraphRAGResultBuilder`** (and any graph helper they call) must not resolve relationship targets via **`Note.getTargetNote()`** or JDBC that assumes **`note.target_note_id`**. Use **5.24a** relationship classification plus **`WikiTitleCacheService`** / **`WikiLinkResolver`** (same viewer rules as elsewhere) so semantic targets come from the wiki-title cache, not the legacy FK.
- **Out of scope here (Phase 7.13):** **`FocusNote`** shape (`links`, **`inboundReferences`**, folder crumbs, removal of **`children`**), **`BareNote`** field deletion (**`subject`**, **`relation_type`**, **`target`**, **`parent`**, **`linkFromFocus`**, **`linkHop2`**), wiki-link **`BareNote.uri`** formatting, **`RelationshipToFocusNote`** enum shrink, and **`UriAndTitle`** removal or graph-local URI rules — see **`ongoing/doughnut_wiki_migration_plan-phase-7.13-sub-phases.md`** (7.13.1–7.13.10). Do not expand **5.24e** into that JSON/DTO cleanup; keep **`BareNote` / `FocusNote`** wiring minimal for “no **`getTargetNote()`** on graph paths” only.
- **Tests:** **`GraphRAGServiceTest`** (and **`GraphRAGResultTest`** only if it still asserts graph assembly touched by this slice). Prefer observable graph output or builder boundaries; avoid introducing new **`BareNote`** constructors or overload disambiguation solely for fields **7.13.8** will remove.

### 5.24f — AI stacks (**behavior**)

- **`OpenAIChatRequestBuilder`**, **`AiQuestionGenerator`**, automation / fine-tuning factories: pass resolver or pre-resolved **`UriAndTitle`**; remove entity-level **`preserveNoteContent` / `getTargetNote()`** branches if they existed only for FK legacy.
- **Tests:** generator / builder tests (Spring or unit as today).

### 5.24g — Test builders (**structure**)

- **`NoteBuilder.afterCreate`**: after persisting a relationship note, call **`WikiTitleCacheService.refreshForNote`** (parity with **`NoteService.createRelationship`**), guarded when **`MakeMe.wikiTitleCacheService`** is null (plain **`makeMeWithoutFactoryService`**).
- **`NoteRealmServiceTest`**: “stale inbound” cases simulate by **`deleteByNote_Id`** on cache for the relation after create; rename tests that mention **legacy FK**.

### 5.24h — Thin JPA (**structure**, column still exists)

- Drop **`inboundReferences`** and dead **`getRelationshipsAndRefers`** once **5.24d** is live; grep for **`Note.getTargetNote`** in `src/main/java` should be empty (cache entity **`NoteWikiTitleCache.getTargetNote`** allowed).

### 5.24i — Flyway: drop **`note.target_note_id`** (**behavior** / deploy gate — **last**)

- Single new migration: drop **`note_ibfk_1`**, index, column (`note` table only).
- Remove **`Note.targetNote`** field and **`NoteRepository.findAllByTargetNote`** if still present.
- **`pnpm backend:verify`** where CI runs migrations; **`pnpm backend:test_only`**; Cypress **`--spec`** `e2e_test/features/relationships/add_relationship.feature`, `relationship_edit_and_remove.feature`.

---

## Verification summary

| Milestone | Typical gate |
|-----------|----------------|
| Through 5.24h | **`pnpm backend:test_only`** (targeted Gradle slices while iterating) |
| 5.24i | **`backend:verify`**, full backend tests, relationship Cypress specs |

---

## Operational note

Production / shared DBs must complete frontmatter + cache backfill (parent **5.16–5.19** gate) **before** deploying **5.24i** (`DROP COLUMN`). No merge of **5.24i** ahead of that gate on environments that may still store relations without cache rows.
