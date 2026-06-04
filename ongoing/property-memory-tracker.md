# Property Memory Tracker

Add memory trackers that target **one property** of a note, alongside the existing note-level trackers.
A note can have one note-level tracker (normal and/or spelling) **and** several property trackers
(one per property). Property trackers behave like normal (MCQ) trackers.

## Requirement analysis

Source request, broken into observable outcomes:

1. Assimilate settings has an **expandable Properties section**; expanding shows each property with an **Assimilate** button.
2. Clicking the button **assimilates that property** (creates a property memory tracker).
3. The memory-tracker list (assimilate settings) **includes property trackers and indicates which property**.
4. The **memory tracker page** indicates the note **and the focused property** clearly.
5. AI question generation for a property tracker **focuses on that one property**, inferring meaning from the **property name, note content, and link targets**.
6. **Reducing a relation note to a property** moves any memory tracker on the reduced note to be **a property tracker** on the source.

## How the current system works (grounding)

- **Properties are YAML frontmatter** inside `note.content` (no `NoteProperty` entity). Frontend reads/writes them via `frontend/src/utils/noteContentFrontmatter.ts` (`PropertyRow = {key, value}`); backend via `algorithms/Frontmatter.java` + `NoteContentMarkdown.java`.
- **`MemoryTracker`** (`backend/.../entities/MemoryTracker.java`, table `memory_tracker`) is keyed by `note_id` + `user_id`, with a boolean `spelling`. Unique active index today: `(user_id, note_id, spelling, <active>)`. There is **no** type enum — "normal" vs "spelling" is the boolean.
- **Assimilate**: `POST /api/assimilation` → `MemoryTrackerService.assimilate(AssimilationRequestDTO{noteId, skipMemoryTracking})`. Creates a normal tracker, and a spelling sibling when the note's `rememberSpelling` is on.
- **Per-note tracker list** in assimilate settings: `AssimilationSettings.vue` → `NoteInfoBar.vue` → `GET /api/notes/{note}/note-info` (`NoteRecallInfo.memoryTrackers`) → `NoteInfoComponent.vue` table → `NoteInfoMemoryTracker.vue` (currently shows only `spelling ? 'spelling' : 'normal'`).
- **Memory tracker detail page**: `/memory-trackers/:id` → `MemoryTrackerPage.vue` / `MemoryTrackerPageView.vue` (shows note via `NoteUnderQuestion.vue`).
- **AI question (normal only)**: `MemoryTrackerController.askAQuestion` → `RecallQuestionService.generateAQuestion(memoryTracker)` → `PredefinedQuestionService` → `AiQuestionGenerator` → `NoteQuestionGenerationService` → **`QuestionGenerationRequestBuilder`** assembles focus-context markdown + optional scoped instruction. **Link targets** = `note_wiki_title_cache` / `NoteRealm.wikiTitles`.
- **Reduce relation → property**: `POST /api/notes/{note}/delete` with `referenceHandling=REDUCE_TO_SOURCE_PROPERTY` + `sourcePropertyKey` → `NoteService.destroy` → `reduceRelationNoteToSourceProperty` (adds frontmatter key=relation label, value=target scalar to source), then soft-deletes the relation note **and its memory trackers**. E2E in `e2e_test/features/relationships/relationship_edit_and_remove.feature`.

## Key design decisions

- **Identify the property with `property_key`** (the frontmatter key, e.g. `"a part of"`), stored on `memory_tracker`. Use **`VARCHAR NOT NULL DEFAULT ''`** where empty string = note-level tracker (avoids MySQL "NULLs are distinct" breaking the per-note uniqueness). New unique active index: `(user_id, note_id, spelling, property_key, <active>)`.
- **Property trackers are normal only** (`spelling=false`); no spelling sibling is created for a property.
- **One tracker per property** per user (enforced by the unique index; assimilate is idempotent like today).
- **Assimilable properties**: **all** of the note's frontmatter properties are offered, with no exclusions.
- **AI focus**: pass `property_key` into the generation path; `QuestionGenerationRequestBuilder` adds an instruction telling the model to focus on that one property, with the property name, the note content, and resolved link targets (`wikiTitles`) as the inference material.

## Resolved decisions

1. **Assimilable properties:** all frontmatter properties; no keys excluded.
2. **Reduce-move conflict:** none to handle — the existing reduce flow already returns a **409** when the source already has that property key, so the property is never added and the relation note (with its tracker) is left intact. Phase 4 only re-homes the tracker on the **successful** reduce path.
3. **Spelling:** property trackers are **normal MCQ only**; no spelling variant.

---

## Phases and commit-sized sub-phases

**Commit discipline:** every sub-phase below is **one commit** that compiles and leaves all unit + touched E2E tests green (a sub-phase that authors an E2E scenario for not-yet-built behavior tags it `@wip` so it is green in CI). Push after each commit. The **deploy gate** (let CD deploy) applies at each **phase** boundary, not every sub-phase. Targeted E2E only: `cypress run --spec` for the feature file(s) touched.

Capability-named permanent artifacts:
- E2E: `e2e_test/features/recall/property_memory_tracker.feature` (+ steps/page objects), and extend `e2e_test/features/relationships/relationship_edit_and_remove.feature` for Phase 4.

---

### Phase 1 — Assimilate a note property and review it  *(Behavior, bullets 1–3)*

**Outcome:** In assimilate settings the user expands a **Properties** section, sees each property with an **Assimilate** button, clicks it, and a **property memory tracker** is created. It appears in the note's Memory Trackers list **labeled with the property name**, and flows into recall as a normal MCQ tracker.

**Interim:** the recall question still uses the note-level focus context (replaced in Phase 2 — no throwaway code; `property_key` simply isn't consumed by the prompt builder yet).

- **1a — Schema + entity field** *(structure)* — **Status: done**
  - Flyway migration: add `memory_tracker.property_key VARCHAR NOT NULL DEFAULT ''`; drop and recreate the active unique index to `(user_id, note_id, spelling, property_key, <active>)`.
  - Add `propertyKey` field + getter/setter to `MemoryTracker`; add `buildMemoryTrackerForProperty(note, key)`.
  - Regenerate `docs/database-erd.md` (`pnpm export:database-erd`).
  - **Green check:** existing backend tests pass unchanged (`backend:verify`). No observable change. **Commit.**

- **1b — Backend assimilate accepts `propertyKey`** *(behavior)* — **Status: done**
  - Add optional `propertyKey` to `AssimilationRequestDTO`.
  - `MemoryTrackerService.assimilate`: when `propertyKey` is present and non-empty, create one property tracker (`spelling=false`, no spelling sibling) scoped by `(note, propertyKey)`; idempotent per property; coexists with the note-level tracker.
  - Regenerate the TS API client (`pnpm generateTypeScript`) so `MemoryTracker` + request types carry `propertyKey`.
  - **Tests (this commit):** backend unit — creates a property tracker; second assimilate of same property is a no-op; note-level + property trackers coexist on one note. **Commit.**

- **1c — E2E scenario (red, `@wip`)** *(behavior, test-first)* — **Status: done**
  - Author `property_memory_tracker.feature`: assimilate a property → it shows under the note's Memory Trackers labeled with the property → it appears in a recall session. Add step defs / page-object hooks. Tag `@wip`.
  - **Green check:** `@wip` is skipped in CI (green); run locally with `--spec` to confirm it fails for the right reason (UI not built). **Commit.**

- **1d — Frontend Properties section + assimilate** *(behavior)* — **Status: done**
  - Add an expandable **Properties** section to `AssimilationSettings.vue` listing **all** frontmatter properties (via `noteContentFrontmatter.ts`), each with an **Assimilate** button that calls `AssimilationController.assimilate({ noteId, propertyKey })` and refreshes note-info.
  - **Tests (this commit):** frontend unit — section renders a row + button per property; clicking calls the API with the right `propertyKey`. **Commit.**

- **1e — Tracker-list property label + close E2E** *(behavior)* — **Status: done**
  - `NoteInfoMemoryTracker.vue` Type column shows `property: <key>` when `propertyKey` is set (else `normal`/`spelling` as today).
  - **Tests (this commit):** frontend unit for the label; rerun `property_memory_tracker.feature --spec` until green, then **remove `@wip`**. **Commit.** → **Phase 1 deploy gate.**

**Phase 1 — Status: complete** (deploy gate: let CD deploy before Phase 2).

---

### Phase 2 — Property-focused AI questions  *(Behavior, bullet 5)*

**Outcome:** Generating a question for a property tracker instructs the AI to **focus on that one property**, inferring its meaning from the **property name, note content, and link targets** (`wikiTitles`). Replaces the Phase-1 interim note-level question.

- **2a — Prompt focuses on the property** *(behavior)* — **Status: done**
  - Thread `propertyKey` from `MemoryTracker` through `RecallQuestionService` → `AiQuestionGenerator` → `NoteQuestionGenerationService` → `QuestionGenerationRequestBuilder` (single commit so the value is consumed, not dead).
  - In `QuestionGenerationRequestBuilder`, when a property key is present, add an instruction block: focus on property `"<key>"` of the focus note; infer meaning from the property name, the note content, and the listed link targets. Confirm link targets (`wikiTitles`) are in the focus context (extend the context if missing).
  - **Tests (this commit):** backend unit on the builder (black-box: note + property key → request contains the focus-on-property instruction, the property key/value, and link targets). **Commit.**

- **2b — E2E: property recall sends property focus** *(behavior, `@wip`→green)*
  - Extend `property_memory_tracker.feature`: recalling a property tracker produces an MCQ; assert (via mountebank stub) the model request includes the property focus instruction. `@wip` first, then green and remove tag.
  - **Commit.** → **Phase 2 deploy gate.**

---

### Phase 3 — Memory tracker page clearly shows the focused property  *(Behavior, bullet 4)*

**Outcome:** The memory tracker detail page and the recall "note under question" header indicate the note **and** the focused property prominently (e.g. a `Focused property: a part of` badge), distinct from note-level trackers. `propertyKey` already rides on the `MemoryTracker` payload from Phase 1, so this is display-only.

- **3a — Detail page indicator** *(behavior)*
  - Render a focused-property indicator in `MemoryTrackerPageView.vue` for property trackers; omit for note-level.
  - **Tests (this commit):** frontend unit — indicator shown for a property tracker, absent for a note-level tracker. **Commit.**

- **3b — Recall session header indicator** *(behavior)*
  - Show the focused property in the recall `NoteUnderQuestion.vue` header.
  - **Tests (this commit):** frontend unit for the header. **Commit.**

- **3c — E2E: open property tracker page** *(behavior, `@wip`→green)*
  - Extend `property_memory_tracker.feature`: open the property tracker's page and see the note + focused property. **Commit.** → **Phase 3 deploy gate.**

---

### Phase 4 — Reducing a relation note to a property moves its memory tracker  *(Behavior, bullet 6)*

**Outcome:** When reducing a relation note to a source property (the **successful** path), a normal memory tracker on the relation note is **moved to be a property tracker** on the source note with `property_key = sourcePropertyKey` instead of being soft-deleted. (A duplicate-key reduce still 409s before any change and leaves the relation note + tracker intact. A spelling tracker on the relation note is soft-deleted as today, since property trackers are normal-only.)

- **4a — Re-home tracker on reduce** *(behavior)*
  - In `NoteService.reduceRelationNoteToSourceProperty`, after the frontmatter property is added, move the relation note's normal memory tracker to the source note with the property key, preserving schedule/stats, instead of soft-deleting it.
  - **Tests (this commit):** extend `NoteControllerTests` reduce cases — a tracked relation note reduced yields a property tracker on the source with the relation-label key and preserved schedule; the existing duplicate-key 409 case still leaves the relation note + tracker intact. **Commit.**

- **4b — E2E: tracked relationship reduced keeps tracking** *(behavior, `@wip`→green)*
  - Extend `relationship_edit_and_remove.feature`: a tracked relationship reduced to a property leaves the source note with a **property memory tracker** for that relation. **Commit.** → **Phase 4 deploy gate.**

---

## Stop-safe / value ordering rationale

- **P1** delivers the whole create→list→recall loop (usable immediately, even with generic questions).
- **P2** makes property trackers genuinely valuable (questions actually about the property).
- **P3** is display polish for clarity.
- **P4** is the most specific scenario (migrating an existing tracker during reduce).

Each phase — and each sub-phase commit — leaves coherent, tested state with no orphaned scaffolding; stopping at any boundary wastes nothing.
