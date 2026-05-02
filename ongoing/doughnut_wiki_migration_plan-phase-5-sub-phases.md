# Doughnut Wiki Migration Plan - Phase 5 Sub-Phases

## Parent Phase

Phase 5 of `ongoing/doughnut_wiki_migration_plan.md`: Convert Relationship Notes into Normal Notes.

## Goal

Relationship notes become **ordinary notes**: title, details, frontmatter, and **`note_wiki_title_cache`** carry relationship meaning; no separate product model for “relation notes.” **Note show** uses **`NoteRealm.references`** (5.21.3). **Graph** and **API cleanup** finish in **5.23** (especially **5.23.6** and **5.23.8**). **5.24+** drop legacy **`target_note_id`** / persistence once nothing reads them.

**Graph wire simplification:** **`relationToFocusNote`** is unused in the **frontend** today; **5.23.8** (done) trims wiki rows to **`linkFromFocus`** / **`linkHop2`** on the graph DTO and nullable structural `relationToFocusNote` so integrators and tests see one “wiki link graph” story (direct backlink vs next hop), without reference-vs-relationship vocabulary on the wire.

## Design decisions (constraints for 5.23–5.27)

- **Source of truth:** details/frontmatter for wiki links; **cache is derived** (refresh on edit; batched migration in **5.16–5.19**).
- **Legacy columns:** keep **`relation_type`** / **`target_note_id`** in DB only until runtime and migrations no longer need them (**5.24** target field, **5.22** already removed link type).
- **Legacy parent:** migrated non-relationship notes only get optional **`parent: "[[…]]"`**; new notes do not by default.
- **Titles:** required non-empty after migration + schema (**5.11**); relationship titles truncated to **`Note.MAX_TITLE_LENGTH`**.

## Sizing Rule

Each sub-phase below is sized as a **small cohesive slice** of work (roughly a five-minute *planning* granularity: one concern, one verify step). If a slice cannot be finished and tested inside that scope, split it further before continuing.

**Git commits:** There is **no** requirement to run `git commit` once per sub-phase or immediately after each one. Developers may batch several sub-phases, combine with unrelated fixes, or split one sub-phase across multiple commits as long as the tree stays green. The **`Commit boundary:`** lines under each sub-phase name the **natural cohesion / review unit** (what belongs together in one change when you *do* commit), not a literal commit cadence.

## Completed sub-phases (5.1–5.22)

**Status:** Implemented. This section is an **index only** (for traceability and handoff to **5.23+**); it is not a checklist for new work.

| Range | Summary |
|-------|---------|
| **5.1–5.7** | Relationship E2E, title + Markdown formatters, create path, edit title/details |
| **5.8–5.12** | Admin migration (titles, details), runtime non-blank titles, DB title non-null, delete regression |
| **5.13–5.15** | `note_wiki_title_cache`, refresh on details update, `NoteRealm.wikiTitles` from cache |
| **5.16–5.19** | Resumable batched admin migration + progress UI; relationship + legacy `parent:` cache backfill |
| **5.20** (5.20.1–5.20.9) | Relationship template UI as normal notes; relation from frontmatter; topology cleanup (`relationType`, `RelationshipOfNote`, `targetNoteTopology`, editor layering) |
| **5.21** (5.21.1–5.21.3) | Inbound + subject/parent slices from cache; unified **`NoteRealm.references`** + **`NoteReferences`**; legacy **`inboundReferences`** / **`relationshipsDeprecating`** remain for non–note-show consumers until **5.23.6** |
| **5.22** | Remove **`relation_type`** from persistence and API |

**Cache row shape** (still relevant for **5.23**): `note_wiki_title_cache` holds `note_id` (source), `target_note_id` (resolved target), `link_text` (full `[[]]` token); ordering by row `id` ascending.

**Production migration:** Code from **5.8–5.9** exists inside the **5.16–5.19** batched runner; do not run legacy one-shot title/details migration as a single long request. Production timing / checklist: parent plan `ongoing/doughnut_wiki_migration_plan.md`.

## Sub-Phase 5.23 - Note Graph Uses Cached Wiki References

**Type:** Behavior (split into **5.23.1**–**5.23.8** so each slice stays one observable concern with a short verify step, per [Sizing Rule](#sizing-rule)).

**Overall pre-condition:** Cached wiki-title references drive **note show** (5.21). `NoteRealm.references` is the merged inbound + subject/parent-linked list for note show.

**Overall trigger:** A user or AI flow requests a note graph, or code builds `NoteRealm` / OpenAPI clients after API cleanup.

**Overall post-condition:** Graph RAG discovers **wiki-linked** semantic neighbors only through **`WikiTitleCacheService`** (plus repository queries that service owns)—**not** by distinguishing legacy “relationship row” vs “reference row” shapes. **Structural** expansion (parent / children / ordered siblings / focus ancestor path, and any retained parent-peer neighborhood) stays explicit and tree-based. Deprecated **`NoteRealm.inboundReferences`** / **`NoteRealm.relationshipsDeprecating`** are removed from the wire (**5.23.6**). Related notes included from the wiki link cache expose **`linkFromFocus`** (direct cached link toward the focus) and **`linkHop2`** (second hop vs a note already included with `linkFromFocus`); fine-grained wiki-only **`relationToFocusNote`** spellings are retired in favor of those flags (**5.23.8**). OpenAPI and generated clients match; frontend did not require changes beyond typecheck/regeneration unless imports broke.

**Commit boundary:** One green merge per **5.23.x** slice (batch only when the tree stays green between slices).

### 5.23 implementation progress (repo state)

Snapshot of what is already merged in the codebase so handoff docs stay aligned with the tree.

| Sub-phase | Status | Notes |
|-----------|--------|-------|
| **5.23.1** | Done | Single cache-backed merge/dedupe for graph and note show; `WikiTitleCacheService` / `NoteRealm.references` path. |
| **5.23.2** | Done | `GraphRAGService.retrieve(Note, int, User viewer)`; entry points pass the read viewer. |
| **5.23.3** | Done | Primary wiki target + repository-backed relation-child scan; ordering/fallback per learnings below. |
| **5.23.4** | Done | Structural `Child` edges skip relation notes; semantic target from cache, not relation children as normal children. |
| **5.23.5** | Done | Wiki-linked discovery uses `referencesNotesForViewer(focus, viewer)` (no parallel FK vs cache merge in `GraphRAGService`); layer quotas exercised in `GraphRAGServiceTest`. |
| **5.23.6** | Done | `NoteRealm` wire: `references` (+ `note`, `wikiTitles`, folders, bazaar, etc.); deprecated `inboundReferences` / `relationshipsDeprecating` removed; OpenAPI + generated clients updated. |
| **5.23.7** | Done (keep extending) | Tests use shared notebook / `canReferTo` / folder-scoped trees where visibility requires it; graph asserts `linkFromFocus` / `linkHop2` where appropriate; add new regressions when edge cases appear. |
| **5.23.8** | Done | Graph `BareNote` / OpenAPI: `linkFromFocus`, `linkHop2`; wiki-sourced inclusion uses those flags; `relationToFocusNote` JSON omitted when null (`NON_NULL`); structural `RelationshipToFocusNote` values remain for tree expansion; obsolete wiki-only enum constants already removed where unused. |

**Structural follow-up (not a numbered slice):** `GraphRAGService` builds **one** `ReferenceByRelationshipHandler` with the full inbound list for the viewer; `PriorityLayer` drains it in a **single layer sweep** (via `consumeNextInboundReferrer`) so note order and budget behavior stay equivalent to the old list of one-note handlers. This refactor does **not** by itself shrink `RelationshipToFocusNote` further; any extra enum cleanup is a separate grep-driven pass.

### Learnings from exploratory implementation (avoid rework)

A single large 5.23 change hid several integration failures until late in `GraphRAGServiceTest`. Capture these in the relevant **5.23.x** slice:

- **Primary semantic target:** For a focus note whose target lives on a **relation child**, lazy `Note.getChildren()` can be empty in service paths—use **`NoteRepository`** (e.g. `findAllByParentId`) when scanning relation children under the focus. When a relation note’s **wiki cache rows** resolve **subject/parent links before** the true **`target_note_id`** semantic target, taking **`outgoing.get(0)`** can return the **wrong** note; **after** matching legacy target inside outgoing, if it is still missing, **fall back to authorized legacy `getTargetNote()`** instead of blindly returning the first outgoing row.
- **Viewer for cache authorization:** Tests that use `note.getCreator()` as viewer can disagree with **notebook ownership** paths inside the cache service; graph **`retrieve(focus, budget, viewer)`** must use the same **read** viewer as production (`AuthorizationService` + controller pattern). Align tests with **notebook `creatorEntity`** (or explicit session user) when asserting visibility.
- **Sibling-of-target and inbound discovery:** `inboundReferrerVisible` compares the **referrer parent’s notebook** to the **focal target’s notebook** (or `canReferTo`). Unrelated `makeMe.aNote()` notebooks filter out “siblings” unless tests put referrers and target in **one shared notebook** or otherwise satisfy sharing rules.
- **`TargetParentSibling` vs `TargetContextAncestor`:** `findStructuralPeerNotesInOrder` uses **folder scope**, else **all notebook roots**. A contextual-path chain built only as roots without a **folder** can put the target’s **grandparent** in the same “peer” list as the target’s parent, so **`TargetParentSiblingRelationshipHandler`** may add that note **first**; **dedupe by note id** then prevents **`TargetContextAncestorRelationshipHandler`** from labeling it correctly. Tests that need **both** edges should use a **dedicated folder** for the target’s ancestry chain (or otherwise constrain structural peers). **After 5.23.8**, prefer asserting **`linkFromFocus` / `linkHop2`** for wiki-derived inclusions; structural peer/ancestor collisions may shrink once target-specific handler labels are retired in favor of tree + wiki-hop labeling.
- **Regression:** Add one graph case where **cache rows** supply neighbors but **legacy FK inbound** is empty (mirror realm “cache vs FK” tests).

### Sub-Sub-Phase 5.23.1 - Single cache API for merged reference notes

**Type:** Behavior (cohesion—one merge/dedupe path).

**Pre-condition:** `WikiTitleCacheService` reads cache rows; `NoteRealmService` builds merged referrers for note show.

**Trigger:** Any caller needs the same list as `NoteRealm.references`.

**Post-condition:** One **`WikiTitleCacheService`** method returns merged, deduped, ordered referrer **`Note`** instances for `(focal, viewer)`; **`NoteRealmService`** sets **`references`** only through that method (no duplicate inbound + subject merge in callers).

**Work:** Keep **`mergeReferenceNotes`** (or equivalent) in one place next to that API; adjust imports and tests.

**Verify:** Focused **`NoteRealmService`** / **`WikiTitleCacheService`** tests; external note-show behavior unchanged.

**Commit boundary:** One realm + cache API commit.

### Sub-Sub-Phase 5.23.2 - Graph retrieval takes an explicit viewer

**Type:** Behavior.

**Pre-condition:** 5.23.1 exists.

**Trigger:** Graph RAG runs under real read rules.

**Post-condition:** **`GraphRAGService.retrieve(Note, int, User viewer)`** exists; overload without viewer delegates via **`AuthorizationService.getCurrentUser()`**; **`NoteController.getGraph`** (and any other entry) passes viewer after read authorization.

**Work:** Constructor injection; update tests that call **`retrieve`** without a session.

**Verify:** Focused controller or **`GraphRAGService`** tests with an explicit **`User`**.

**Commit boundary:** One viewer-plumbing commit.

### Sub-Sub-Phase 5.23.3 - Primary outgoing target and repository-backed child scan

**Type:** Behavior.

**Pre-condition:** 5.23.2 viewer plumbing.

**Trigger:** Graph (or tests) need the semantic primary target for arbitrary focus notes.

**Post-condition:** **`WikiTitleCacheService`** exposes authorized **outgoing** target list and **primary** target for graph expansion, including **scan of relation children via repository** when the focus is not the carrier relation note; legacy FK fallback and **ordering vs wiki rows** follow the learnings above.

**Work:** Optional **`NoteWikiTitleCacheRepository`** query for “sources sharing `target_note_id`” if sibling-of-target is deferred to 5.23.5; keep queries behind the service.

**Verify:** Focused **`WikiTitleCacheService`** tests (inputs/outputs); no graph handler wiring required beyond smoke if useful.

**Commit boundary:** One cache-target-resolution commit.

### Sub-Sub-Phase 5.23.4 - Structural children: ignore relation notes in graph child edges

**Type:** Behavior.

**Pre-condition:** 5.23.3 primary target available to graph wiring.

**Trigger:** Graph walks structural children.

**Post-condition:** **`ChildRelationshipHandler`** (or equivalent) uses **`!note.isRelation()`** for structural **Child** edges; semantic targets do not come from treating relation children as normal children; remove or stop enqueueing **`TargetOfRelationshipRelationshipHandler`** (or equivalent) where superseded by cache-backed target expansion.

**Work:** Minimal **`RelationshipHandler`** / layer changes for observable graph only.

**Verify:** **`GraphRAGServiceTest`** nested cases for related-child / regular children.

**Commit boundary:** One graph-structure commit.

### Sub-Sub-Phase 5.23.5 - Wire graph handlers to cache-backed lists and tune quota

**Type:** Behavior.

**Pre-condition:** 5.23.1–5.23.4.

**Trigger:** Full graph expansion for typical notes.

**Post-condition:** **Discovery and budget only** for wiki-linked neighbors: any path that previously merged “inbound reference” vs “relationship subject/parent” lists now pulls from the **same** cache-backed API (**`referencesNotesForViewer(focus, viewer)`** or the single method from **5.23.1**), with **no** duplicate merge logic in **`GraphRAGService`**. **Primary-target** and **sibling-of-target** style expansion, if still needed for ordering before **5.23.8**, use **5.23.3** resolution + **one** repository-backed query through the cache service—not parallel FK vs cache semantics. **priority-layer `notesBeforeNextLayer`** (or equivalent) is tuned so cache-driven expansions are not starved. **Wire labeling** of those notes may still use legacy **`RelationshipToFocusNote`** values until **5.23.8**; this slice must not depend on perfect final enum names.

**Work:** Handler constructors / **`GraphRAGService`** assembly; delete or narrow handlers that only existed to encode reference-vs-reference-target distinction when cache BFS replaces them.

**Verify:** **`GraphRAGServiceTest`** (by class)—assert **which notes appear** and **order within budget**, not final **`linkFromFocus`/`linkHop2`** unless this slice lands together with **5.23.8**.

**Commit boundary:** One graph-wiring commit (split only if interim tree stays green).

### Sub-Sub-Phase 5.23.6 - Remove deprecated `NoteRealm` wire fields

**Type:** Behavior / API cleanup.

**Pre-condition:** No production or test path requires **`inboundReferences`** / **`relationshipsDeprecating`** on **`NoteRealm`** after 5.23.5.

**Trigger:** Clients consume OpenAPI / **`NoteRealm`**.

**Post-condition:** Fields removed from Java DTO and OpenAPI; **`pnpm generateTypeScript`**; **`NoteRealmBuilder`**, MCP, and grep-clean tests.

**Work:** Follow **`generate-api-client`** skill for regeneration.

**Verify:** Backend compile; frontend typecheck if generated types change.

**Commit boundary:** One API-shape commit.

### Sub-Sub-Phase 5.23.7 - Test fixtures and regressions aligned with cache visibility

**Type:** Behavior (tests; fold into 5.23.5/5.23.6 if trivial).

**Pre-condition:** 5.23.1–5.23.6 implemented (and **5.23.8** if graph assertions move to **`linkFromFocus`/`linkHop2`**).

**Trigger:** CI runs backend tests.

**Post-condition:** Graph tests use **shared notebook** (or explicit **`canReferTo`**) where **`inboundReferrerVisible`** requires it; contextual-path fixtures use **folder-scoped** trees when both **target parent siblings** and **target context ancestors** must appear without dedupe collisions; at least one **graph** test proves neighbors from **cache only** when legacy FK inbound is empty; realm tests assert **`references`** post-5.23.6. After **5.23.8**, graph tests should prefer **observable JSON** checks on **`linkFromFocus`** / **`linkHop2`** (and remaining structural fields if any) over asserting deprecated wiki-specific **`relationToFocusNote`** spellings.

**Work:** Update **`GraphRAGServiceTest`**, **`NoteRealmServiceTest`**, builders.

**Verify:** Same focused test classes as 5.23.5–5.23.6 (extend to **5.23.8** when landed).

**Commit boundary:** One test commit (optional).

### Sub-Sub-Phase 5.23.8 - Graph API: `linkFromFocus` and `linkHop2` replace wiki-specific `relationToFocusNote` labels

**Type:** Behavior (OpenAPI / graph JSON contract; **frontend** does not consume **`relationToFocusNote`** today—still observable for MCP, tests, and future UI).

**Pre-condition:** 5.23.5 delivers wiki-linked neighbors through the unified cache path; related notes are deduped by note id in **`GraphRAGResultBuilder`** as today.

**Trigger:** Consumers need a stable, minimal description of **why** a related note was included from the **wiki link cache** without reference-vs-relationship vocabulary.

**Post-condition:**

- The graph-related-note DTO (e.g. **`BareNote`** / OpenAPI schema) exposes **`linkFromFocus`** and **`linkHop2`** (booleans or mutually exclusive flags—pick one representation and document it). Semantics:
  - **`linkFromFocus`:** `true` when the note was admitted **because** it has an **authorized** cached wiki link **to the focus** (direct “backlink” hop, replacing **`ReferenceBy`**, **`ReferencingNote`**, **`RelationshipTarget`**, and other wiki-sourced roles that only differed by legacy row shape).
  - **`linkHop2`:** `true` when the note was admitted **because** it has an **authorized** cached wiki link **to a note that is already in the result set with `linkFromFocus` true** (one “next hop” from the unified reference story).
- For notes included **only** via **structural** expansion (parent, child, siblings, focus ancestor path, parent-peer family if retained), **`linkFromFocus`/`linkHop2`** are **`false`** (unless a slice explicitly defines a dual-path rule—avoid unless needed).
- **`relationToFocusNote`:** Either **removed** from the graph DTO, **trimmed** to structural values + **`Self`**, or left populated only for structural rows with wiki rows using the new flags only—**choose the smallest wire** that keeps **`GraphRAGServiceTest`** and any MCP usage coherent; regenerate **`pnpm generateTypeScript`** and grep backend/tests for stale enum assertions on wiki-only values.

**Work:** Implement serialization and handler/callback hooks so the first wiki wave sets **`linkFromFocus`**, the second wave sets **`linkHop2`**; remove or stop emitting obsolete **`RelationshipToFocusNote`** enum constants that existed solely for reference-vs-relationship-vs-target labeling **if** nothing else references them; delete dead handlers after behavior is covered by tests.

**Verify:** **`GraphRAGServiceTest`** (and any controller JSON test) asserts **`linkFromFocus`/`linkHop2`** on at least one direct backlink and one second-hop case; OpenAPI diff reviewed. **Frontend:** typecheck only unless a generated type accidentally breaks an import.

**Commit boundary:** One graph-DTO + OpenAPI regeneration commit (split enum deletion vs field addition only if the tree must stay green mid-change).

## Sub-Phase 5.24 - Remove Relationship Target Field

**Type:** Persistence cleanup.

**Pre-condition:** Runtime reference behavior and relationship displays no longer read `Note.targetNote`.

**Trigger:** Database migrations and generated API are applied.

**Post-condition:** The `target_note_id` field is removed from the note model, schema, OpenAPI, generated client, and frontend/backend references.

**Work:** Drop the column, remove `targetNote` mappings and repository methods used only by the legacy column, and route remaining target behavior through frontmatter/cache.

**Verify:** Focused backend tests and targeted relationship E2E specs.

**Commit boundary:** One target-field-removal commit.

## Sub-Phase 5.25 - Title Rename Updates Cached Wiki References

**Type:** Behavior.

**Pre-condition:** Referring notes are discoverable from the wiki-title cache (and, after 5.21.3, surfaced on `NoteRealm.references` for note show).

**Trigger:** A note's title changes.

**Post-condition:** Notes that reference the old title are reverse-updated to reference the new title, and their cache rows are refreshed.

**Work:** Add a title-update test that creates at least one referencing note, changes the target title, and verifies both the source details/frontmatter and cache use the new wiki title.

**Verify:** Focused controller/service tests for note title update.

**Commit boundary:** One reverse-reference-title-update commit.

## Sub-Phase 5.26 - Note Show Stops Surfacing Relationships As Child Notes

**Type:** Behavior.

**Pre-condition:** References and graph retrieval use cached wiki-title references; **`NoteRealm.relationshipsDeprecating`** and **`NoteRealm.inboundReferences`** are removed from the API in **5.23.6** (note show already uses **`NoteRealm.references`** and **`NoteReferences`** from **5.21.3**).

**Trigger:** A user opens a note show page for a note that used to have relationship child notes.

**Post-condition:** Child relationship notes are not presented as a separate “relationship children” concept on note show; users find those edges only through the unified reference surface (`references` / wiki links). Any residual UI, copy, or E2E steps that still assumed a distinct relationship-child list or sidebar inbound panel are removed or rewritten.

**Work:** Align remaining note-show copy and E2E with **`NoteReferences`** + **`references`** only. If **5.23.6** already dropped `relationshipsDeprecating` from the wire, this sub-phase is mostly behavioral confirmation and test cleanup; otherwise complete that removal here in lockstep with the plan above.

**Verify:** Target the existing E2E feature that observes note-show relationships/references, plus focused frontend tests for `NoteReferences`.

**Commit boundary:** One note-show-reference-surface commit.

## Sub-Phase 5.27 - Phase 5 Closeout and Plan Update

**Type:** Structure / cleanup.

**Pre-condition:** Relationship creation, migration, edit, delete, cache-backed references, graph retrieval, and title invariants are passing. The resumable admin migration has reached complete status in production or the deployment checklist explicitly records why it has not.

**Trigger:** Final targeted verification for Phase 5.

**Post-condition:** Phase 5 docs reflect implementation status and no temporary `@wip` scenarios, dead code, or obsolete notes remain.

**Work:**

- Remove any `@wip` tags introduced while driving Phase 5 behavior.
- Update `ongoing/doughnut_wiki_migration_plan.md` with Phase 5 status and any discoveries.
- Remove obsolete relationship-note-specific code that depended on `relation_type`, `target_note_id`, child relationship notes, or child-derived incoming references.
- Drop the temporary migration progress table only after the production migration has completed and rollback/resume is no longer needed; otherwise leave it with an explicit follow-up cleanup note.
- Run the relationship specs touched in this phase with targeted `--spec` commands.

**Commit boundary:** One cleanup/docs commit that leaves Phase 5 closed and ready for Phase 6 (folder-first listing and removal of note **`shortDetails`**).
