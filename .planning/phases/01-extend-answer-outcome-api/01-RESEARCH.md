# Phase 1: Extend Answer outcome API - Research

**Researched:** 2026-07-23
**Domain:** Spring Boot → OpenAPI → TypeScript contract round-trip (answer outcome types)
**Confidence:** HIGH

## Summary

Phase 1 is a pure **Structure** phase: extend the backend→frontend answer **contract** (OpenAPI types/schema) so it can represent a third outcome (accidental-match with a matched-note id) and an overlap flag, then regenerate the TypeScript client. No backend behavior is wired — the new states must be representable but not yet returned by any code path.

The central finding: the `Answer` contract type is **also a persisted JPA entity** (`entities/Answer.java`, table `quiz_answer`, with a `@NotNull Boolean correct` column). It is embedded by **two** response DTOs — `controllers/dto/AnsweredQuestion.java` and `controllers/dto/RecallPromptHistoryItem.java` — so any contract change to `Answer` propagates to both. Jackson is configured with `JsonInclude.Include.NON_NULL` (`configs/ObjectMapperConfig.java:25`), so **nullable fields that no code path sets are omitted from JSON and become optional in the generated TS** — this is what makes a purely additive, zero-behavior contract extension safe: the existing frontend keeps type-checking.

**Primary recommendation:** Extend the contract **without a Flyway migration** by adding the new state as `@Transient` (non-persisted) nullable fields on the `Answer` entity plus optional fields on the `AnsweredQuestion` DTO, keeping `correct: boolean` required and untouched. Then run `pnpm generateTypeScript` and confirm the frontend type-checks. The one decision to confirm with the developer (Jidoka) is whether the accidental-match state should be an explicit `outcome` enum or inferred from a nullable `matchedNoteId`.

## User Constraints (from CONTEXT.md substitute — discuss-phase skipped)

These are LOCKED developer decisions. Treat as authoritative; do not propose alternatives that violate them.

### Locked Decisions
1. **Phase 1 is a pure STRUCTURE phase.** It extends the backend→frontend answer CONTRACT (types/schema) only. No behavioral change. No new grading logic. No new endpoint that returns the new states. No new backend service.
2. **No new backend service that is not used by an externally observable frontend.** For Phase 1 specifically: introduce NO new backend service at all. The new outcome states must be representable in the contract but NOT yet returned by any backend code path (Phases 2, 5, 6 wire the behavior that returns them).
3. **Success boundary (from ROADMAP):** the regenerated OpenAPI client compiles and the frontend type-checks against the new contract. No backend behavior is wired yet — the new states are representable but not yet returned.
4. **Reuse, don't reinvent:** `Note.matchAnswer`, `WikiLinkResolver`, `LinkInsertionChoice` / `AddRelationshipFinalize`, `updateFortingCurve` are existing primitives later phases reuse. Phase 1 must NOT alter their behavior; it only needs the contract to be able to carry (a) an accidental-match state with a matched-note id, and (b) an overlap flag / matched-note topology in `AnsweredQuestion`.
5. **Match scope (for later phases, but the contract must support it):** accidental-match search is across all notebooks the user can read — broader than notebook-scoped `WikiLinkResolver`. The Phase 1 contract should be shaped so it can carry a matched-note id (or list) without committing Phase 2's search implementation.
6. **No UI in Phase 1.** The ROADMAP `UI hint` for Phase 1 is `no`. Do NOT research or propose any Vue component / UI work. Frontend involvement is limited to: regenerated TS types compile and the existing frontend that consumes `AnsweredQuestion`/`Answer` still type-checks (possibly with the new fields optional/nullable so nothing breaks).

### Claude's Discretion
- Concrete shape of the contract extension (enum vs nullable id; bare `List<NoteTopology>` vs richer `MatchedNote` object) — research options and recommend.
- Whether to extend the `Answer` entity directly (`@Transient`) or introduce a separate response DTO.

### Deferred Ideas (OUT OF SCOPE)
- MCQ accidental-match (spelling only in v1).
- Fuzzy / partial / substring answer matching (exact title/alias only in v1).
- Cross-notebook qualified `Notebook:Title` typing.
- Auto-creating links without user choice.
- LLM / semantic match.

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| API-01 | The `Answer` outcome is extended beyond a boolean `correct` to represent accidental-match (with matched note id) and overlap states. | `Answer` entity (`entities/Answer.java`) is the contract type; add `@Transient` nullable `matchedNoteId` (+ optional `outcome` enum) so the `Answer` OpenAPI schema carries the new state. Jackson `NON_NULL` keeps new fields optional in TS. |
| API-02 | The `AnsweredQuestion` response carries matched-note topology and an overlap flag; the OpenAPI client is regenerated. | `AnsweredQuestion` DTO (`controllers/dto/AnsweredQuestion.java`); add optional `overlap: Boolean` and `matchedNotes: List<NoteTopology>`; regen via `pnpm generateTypeScript`. |

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Answer outcome contract (types/schema) | API / Backend (DTOs + entity) | Generated SDK | The OpenAPI schema is generated from backend controllers/DTOs; the `Answer` entity is the contract type. Phase 1 edits here only. |
| OpenAPI spec generation | Build (Gradle `generateOpenAPIDocs`) | — | springdoc reads controllers/DTOs → `open_api_docs.yaml`. Never hand-edited. |
| TS client regeneration | Build (`@hey-api/openapi-ts`) | — | `pnpm generateTypeScript` → `packages/generated/doughnut-backend-api/`. |
| Frontend type-checking against contract | Browser / Client (Vue SPA) | — | Existing consumers read `answeredQuestion.answer.correct`; must stay green with new optional fields. |
| Persisted answer state (`correct` column) | Database / Storage | — | OUT OF SCOPE for Phase 1 — do not add columns; new state is `@Transient` (non-persisted). |

## Contract Round-Trip (current state)

### 1. The `Answer` and `AnsweredQuestion` types

**`Answer`** — `backend/src/main/java/com/odde/doughnut/entities/Answer.java` (JPA entity, table `quiz_answer`):

```24:27
  @Column(name = "correct")
  @Setter
  @NotNull
  private Boolean correct;
```
Full current field shape (entity): `id`, `choiceIndex` (Integer, nullable), `createdAt` (`@JsonIgnore`), `correct` (`@NotNull Boolean`), `thinkingTimeMs` (Integer, nullable), `spellingAnswer` (String, nullable). `Answer.buildAnswer(...)` sets `correct` via `predefinedQuestion.checkAnswer(answerDTO)`.

**`AnsweredQuestion`** — `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java`:

```14:30
@Data
@NoArgsConstructor
public class AnsweredQuestion {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private int id;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private QuestionType questionType;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private int memoryTrackerId;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private RecalledNote recalledNote;
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) private Answer answer;
  private PredefinedQuestion predefinedQuestion;
```
`AnsweredQuestion.from(RecallPrompt)` is the single factory; it sets `answer` from `recallPrompt.getAnswer()` (the persisted entity).

**`RecallPromptHistoryItem`** — `backend/src/main/java/com/odde/doughnut/controllers/dto/RecallPromptHistoryItem.java:29` also embeds `private Answer answer;` (second contract surface for the same entity).

### 2. Backend construction + endpoint

- Spelling-answer endpoint: `POST /api/recall-prompts/{recallPrompt}/answer-spelling` → `AnsweredQuestion` (`controllers/RecallPromptController.java:81-95`), delegating to `services/MemoryTrackerService.answerSpelling(RecallPrompt, AnswerSpellingDTO, User, Timestamp)` (`MemoryTrackerService.java:255-281`).
- MCQ endpoint: `POST /api/recall-prompts/{recallPrompt}/answer` → `AnsweredQuestion` (`RecallPromptController.java:68-79`) via `AnswerService.createAnswerForQuestion` → `Answer.buildAnswer`.
- History endpoint: `GET /api/recalls/previously-answered` → `List<AnsweredQuestion>` (`controllers/RecallsController.java:54-67`).

In `MemoryTrackerService.answerSpelling` the grading primitive is `Note.matchAnswer` (`entities/Note.java:135-140`, returns `boolean` matching title or `FrontmatterAliases`). Phase 1 must NOT alter it.

### 3. OpenAPI generation → client regen

- Backend schema → `open_api_docs.yaml` via Gradle `generateOpenAPIDocs` (springdoc-openapi 3.0.3). Current `Answer` schema (`open_api_docs.yaml:3633-3651`): `required: [correct, id]`; `choiceIndex/thinkingTimeMs/spellingAnswer` optional. `AnsweredQuestion` (`3652-3677`): `required: [answer, id, memoryTrackerId, questionType, recalledNote]`; `predefinedQuestion` optional.
- Regen command (Nix-prefixed, from `generate-api-client` SKILL): `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` → `@hey-api/openapi-ts` 0.99.0 writes `packages/generated/doughnut-backend-api/{sdk.gen.ts,types.gen.ts}`.
- Current generated TS (`types.gen.ts:282-297`):
```282:297
export type Answer = { id: number; choiceIndex?: number; correct: boolean; thinkingTimeMs?: number; spellingAnswer?: string; };
export type AnsweredQuestion = { id: number; questionType: 'MCQ' | 'SPELLING'; memoryTrackerId: number; recalledNote: RecalledNote; answer: Answer; predefinedQuestion?: PredefinedQuestion; };
```

### 4. Frontend consumption (must keep type-checking)

- `frontend/src/components/recall/AnsweredSpellingQuestion.vue:2-4` reads `answeredQuestion.answer.correct` (boolean) and `answeredQuestion.answer.spellingAnswer`; also `answeredQuestion.recalledNote.noteTopology.id`, `answeredQuestion.memoryTrackerId`.
- `frontend/src/components/recall/AnsweredQuestionComponent.vue` reads `answeredQuestion.recalledNote`, `memoryTrackerId`, `predefinedQuestion.*`, `answeredQuestion.answer` (passed to `QuestionDisplay`), `answeredQuestion.id`.
- Direct `Answer` type import: only `frontend/tests/components/recall/QuestionDisplay.spec.ts` and the fixture `packages/doughnut-test-fixtures/src/AnsweredQuestionBuilder.ts` (casts the answer object `as Answer`, sets `{ id, correct, choiceIndex? }`).
- CLI: `cli/tests/recallSpellingInteractive.test.tsx` imports `AnsweredQuestion` from `doughnut-api`.

**Implication:** as long as `correct: boolean` stays **required** and the new fields are **optional/nullable**, every consumer above keeps type-checking with zero source edits. The fixture builder keeps working because the new fields are optional.

## Design Options for the Contract Extension

All options below are **purely additive** and keep `correct: boolean` required. They differ in where the new state lives and whether persistence is touched.

### Option A — `@Transient` fields on the `Answer` entity (RECOMMENDED)

Add non-persisted nullable fields to `entities/Answer.java`:

```java
@Transient private Long matchedNoteId;          // non-null ⇒ accidental match (primary matched note)
@Transient private AnswerOutcome outcome;       // optional explicit grading state
```
Plus a new enum `AnswerOutcome { CORRECT, WRONG, ACCIDENTAL_MATCH, OVERLAP }` (or omit if Option A-min below).

And on `controllers/dto/AnsweredQuestion.java`:

```java
private Boolean overlap;                         // optional; true ⇒ "correct but try again" (Phase 6)
private List<NoteTopology> matchedNotes;         // optional; matched-note topology for reveal (P3) + link pre-select (P4)
```

- **No Flyway migration** — `@Transient` (JPA) excludes the fields from persistence; Jackson does **not** honor `jakarta.persistence.Transient`, so the fields are still serialized → appear in OpenAPI → optional in TS. `[CITED: Jackson/JPA semantics — verify via regen]`
- `AnsweredQuestion.from()` and `MemoryTrackerService.answerSpelling` do NOT set the new fields → they stay null → omitted from JSON (`NON_NULL`) → existing frontend green.
- One entity edit updates **both** DTOs (`AnsweredQuestion` + `RecallPromptHistoryItem`) automatically, since both embed the `Answer` entity.
- `matchedNotes` reuses the existing `NoteTopology` (id + title) — exactly what Phase 3 (reveal) and Phase 4 (`LinkInsertionChoice` pre-select by id, display by title) need. `[VERIFIED: codebase — NoteTopology at types.gen.ts:233-238]`

**Option A-min (most minimal):** drop the `outcome` enum; infer accidental-match from `matchedNoteId != null` and overlap from `AnsweredQuestion.overlap`. Fewer types; state is implicit.

### Option B — Separate `AnswerView` response DTO (no entity edit)

Replace `AnsweredQuestion.answer: Answer` (and `RecallPromptHistoryItem.answer`) with a new `controllers/dto/AnswerView` DTO carrying `correct` + optional `matchedNoteId`/`outcome` + existing fields; map from the persisted entity in both `from()` methods.
- **Pro:** zero entity change; contract fully decoupled from persistence.
- **Con:** broader blast radius — 2 DTO edits + new DTO + 2 mappers + fixture builder update (`as Answer` → `as AnswerView`) + `QuestionDisplay.spec.ts` import. More moving parts for a "no behavior" phase.

### Option C — Add persisted columns to `Answer` + Flyway migration

Add nullable `matched_note_id` / `outcome` columns to `quiz_answer`.
- **Pro:** matched-note id persisted for answer history.
- **Con:** touches persistence (a schema migration with no writer in Phase 1 = speculative persistence), and a single `matched_note_id` column cannot hold the multi-match list (AM-03). Locked decision #1 ("extends the … CONTRACT (types/schema) only") and the planning rule against speculative structure both weigh **against** this for Phase 1. If the developer later wants the matched note persisted, that belongs in Phase 2.

### Recommendation

**Option A** (with the `outcome` enum — "Option A-full") is recommended:
- `Answer` carries the accidental-match state (`matchedNoteId` + explicit `outcome`) → satisfies SC#1 / API-01 literally.
- `AnsweredQuestion` carries `overlap` + `matchedNotes: List<NoteTopology>` → satisfies SC#2 / API-02 and locked decision #4b.
- No Flyway, no new service, no new endpoint, no behavior, no UI. Purely additive; existing frontend stays green.
- The explicit `outcome` enum makes Phase 2 ("a third SRS outcome", AM-02) and Phase 6 (frontend distinguishes "correct" vs "correct-but-overlap") clean. `correct` remains the persisted SRS-credit boolean (unchanged semantics for `markAsRecalled` + threshold logic).

**Minor redundancy to flag:** `Answer.matchedNoteId` (singular) overlaps with `AnsweredQuestion.matchedNotes[0].id`. Singular = quick pointer required by SC#1; list = full topology for AM-03 multi-match. Acceptable; the planner may drop the singular id if they prefer a single source of truth.

### How the frontend distinguishes outcomes (Phase 6 forward-view)

With Option A-full, the frontend reads `answer.outcome`:
- `CORRECT` → success UI.
- `WRONG` (or null + `correct=false`) → incorrect UI.
- `ACCIDENTAL_MATCH` → reveal both notes (Phase 3), offer link (Phase 4); `matchedNoteId`/`matchedNotes` populated.
- `OVERLAP` → "correct, but we're looking for another answer — try again", no credit (Phase 6); `AnsweredQuestion.overlap=true`.

`correct` stays the SRS-credit signal (overlap = no credit → `correct=false` per OVL-01 success criterion 2). The `outcome` enum is the richer display signal. This keeps `correct`'s existing semantics intact while letting later phases express the new states.

## No-Behavior Verification Surface (commands to prove "structure only")

All Nix-prefixed (`CURSOR_DEV=true nix develop -c …`); git runs without the prefix.

| Check | Command | What it proves |
|------|---------|----------------|
| OpenAPI regen | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` | New `Answer`/`AnsweredQuestion` schema flows into `types.gen.ts`/`sdk.gen.ts`; `open_api_docs.yaml` regenerated. |
| OpenAPI lint (Redocly) | `CURSOR_DEV=true nix develop -c pnpm openapi:lint` | New schema is valid (no duplicate operationIds, etc.). Fix controllers, not YAML. |
| Backend build/test | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` | No migration ⇒ `test_only` is enough; existing spelling-answer tests stay green. |
| Frontend type-check + tests | `CURSOR_DEV=true nix develop -c pnpm frontend:test` | Existing consumers of `Answer`/`AnsweredQuestion` still type-check and pass. |
| Format/lint | `CURSOR_DEV=true nix develop -c pnpm format:all` then `CI=true ... pnpm lint:all` | Biome + Redocly clean. |

**Existing tests that pin the current `Answer` shape** (must stay green; they read `getCorrect()`, not whole-object equality, so adding nullable fields is safe):
- `backend/src/test/java/com/odde/doughnut/controllers/RecallPromptControllerTests.java` — e.g. `:519`, `:525`, `:532`, `:540`, `:546` assert `controller.answerSpelling(...).getAnswer().getCorrect()`.
- `backend/src/test/java/com/odde/doughnut/controllers/RecallsControllerTests.java` — `previously-answered` returns `List<AnsweredQuestion>`.
- Frontend: `frontend/tests/components/recall/QuestionDisplay.spec.ts` (imports `Answer`), `AnsweredQuestionComponent.spec.ts`, `RecallPage.spec.ts`, and the `AnsweredQuestionBuilder` fixture.

**No new behavior assertions in Phase 1.** Do NOT add tests asserting `outcome == ACCIDENTAL_MATCH` — no code path returns it yet. Phase 1 tests (if any) should assert the new fields are **absent/null** on the existing spelling path (proving "representable but not returned"), e.g. a controller test that `answerResult.getAnswer().getMatchedNoteId()` is null and `answerResult.getOverlap()` is null/false.

## Regression Risks of a Pure Contract Change

- **Deserialization:** Nothing `@RequestBody`-deserializes `Answer`/`AnsweredQuestion` (they are response-only types; request bodies are `AnswerDTO`/`AnswerSpellingDTO`). New optional response fields cannot break inbound parsing. `[VERIFIED: codebase — controllers use AnswerDTO/AnswerSpellingDTO as @RequestBody]`
- **JSON-shape pinning:** No whole-`Answer` JSON equality test found (controller tests read `getCorrect()` directly; `NoteRealmJsonSerializationTest` covers `NoteRealm`, not `Answer`). Low risk — but the planner should `rg` for any `jsonEquals`/`assertJson` over the answer payload before finalizing.
- **Generated-client drift:** Any hand-edited `sdk.gen.ts`/`types.gen.ts`/`open_api_docs.yaml` would be overwritten — these are never hand-edited (rule). Whitespace gate uses `scripts/check_diff_whitespace.sh`, not raw `git diff --check`.
- **`RecallPromptHistoryItem` second surface:** Because it also embeds `Answer`, Option A automatically extends it too. Confirm the history endpoint's consumers (CLI/frontend) tolerate the new optional fields — they will, since `NON_NULL` omits them.
- **`@Transient` serialization assumption:** If `@hey-api/openapi-ts`/springdoc do NOT pick up a JPA-`@Transient` field, the field won't appear in the contract. **Mitigation:** regen, then grep `types.gen.ts` for the new field; if absent, fall back to Option B (explicit DTO). This is the single highest-risk assumption — flag for the planner.

## Phase-Boundary Check (report, don't decide)

**Can Phase 1 be delivered as pure structure with no new backend service and no behavior? — YES.**

Evidence:
- The contract is generated from DTOs + the `Answer` entity; adding `@Transient` nullable fields + optional DTO fields changes the schema only. `[VERIFIED: codebase]`
- No code path is forced to set the new fields: `AnsweredQuestion.from()` and `MemoryTrackerService.answerSpelling` can stay exactly as-is, leaving new fields null. `[VERIFIED: MemoryTrackerService.java:255-281, AnsweredQuestion.java:32-45]`
- No new endpoint needed — the existing `answer-spelling`/`answer`/`previously-answered` endpoints already return `AnsweredQuestion`; the schema change flows through regen.
- No new backend service is required to *represent* the states; the service that *returns* them is Phase 2 (accidental-match) / Phase 6 (overlap). `[VERIFIED: ROADMAP phase deps]`

**One caveat to surface (honest finding):** the `@Transient`-serialization behavior is the only place the current architecture could force a non-trivial choice. If `@Transient` fields do not surface in the OpenAPI schema, the developer must either (a) accept Option B (a real DTO edit — still structure, still no behavior, but broader), or (b) accept Option C (a Flyway migration — touches persistence, still no behavior). Neither introduces behavior or a new service; both remain Structure. So the phase boundary holds regardless — the choice is about blast radius, not about being forced into behavior. The developer said to "adjust the phases if necessary"; no adjustment is needed here, only a shape decision at planning.

## Common Pitfalls

### Pitfall 1: Making `correct` optional or removing it
**What goes wrong:** Frontend (`AnsweredSpellingQuestion.vue:2-4`) reads `answeredQuestion.answer.correct` as a required boolean; the persisted column is `@NotNull`. **How to avoid:** Keep `correct: boolean` required and untouched; add new state as separate optional fields.

### Pitfall 2: Adding a Flyway migration for fields no code writes
**What goes wrong:** Speculative persistence; a single `matched_note_id` column can't hold AM-03's multi-match list; violates the "contract only" locked decision. **How to avoid:** Use `@Transient` (Option A) — no migration in Phase 1.

### Pitfall 3: Assuming `@Transient` surfaces in OpenAPI without verifying
**What goes wrong:** Field silently absent from `types.gen.ts` → SC#1 not met. **How to avoid:** After regen, grep `types.gen.ts` for the new field; fall back to Option B if missing.

### Pitfall 4: Hand-editing generated files
**What goes wrong:** `pnpm generateTypeScript` overwrites edits; CI fails. **How to avoid:** Edit Java only; regenerate; use `scripts/check_diff_whitespace.sh`.

### Pitfall 5: Forgetting the second contract surface
**What goes wrong:** Only thinking about `AnsweredQuestion`, missing `RecallPromptHistoryItem` (also embeds `Answer`). **How to avoid:** Option A handles both via the shared entity; if Option B, update both DTOs.

## Code Examples

### Recommended contract extension (Option A)

```java
// entities/Answer.java — add non-persisted contract fields (no Flyway)
@Transient private Long matchedNoteId;
@Transient private AnswerOutcome outcome;
// Lombok @Getter/@Setter on the class already generates accessors.
```

```java
// new file: entities/AnswerOutcome.java
package com.odde.doughnut.entities;
public enum AnswerOutcome { CORRECT, WRONG, ACCIDENTAL_MATCH, OVERLAP }
```

```java
// controllers/dto/AnsweredQuestion.java — add optional topology + overlap flag
import com.odde.doughnut.entities.NoteTopology;
import java.util.List;
// ...
private Boolean overlap;
private List<NoteTopology> matchedNotes;
// AnsweredQuestion.from(...) stays UNCHANGED — new fields left null (no behavior).
```

### Expected generated TS (after regen)

```typescript
export type Answer = {
  id: number; choiceIndex?: number; correct: boolean;
  thinkingTimeMs?: number; spellingAnswer?: string;
  matchedNoteId?: number; outcome?: 'CORRECT' | 'WRONG' | 'ACCIDENTAL_MATCH' | 'OVERLAP';
};
export type AnsweredQuestion = {
  id: number; questionType: 'MCQ' | 'SPELLING'; memoryTrackerId: number;
  recalledNote: RecalledNote; answer: Answer; predefinedQuestion?: PredefinedQuestion;
  overlap?: boolean; matchedNotes?: Array<NoteTopology>;
};
```
Existing frontend reads (`answeredQuestion.answer.correct`) compile unchanged; new fields are optional.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| TS API types | Local interfaces for `Answer`/`AnsweredQuestion` | `@generated/doughnut-backend-api` (regen) | Drift breaks at runtime; rule forbids hand-rolled wire shapes. |
| OpenAPI schema | Hand-edit `open_api_docs.yaml` | springdoc from controllers/DTOs + `pnpm generateTypeScript` | Generated; CI expects regeneration. |
| Matched-note topology shape | A new ad-hoc note-ref type | Existing `NoteTopology` (id + title) | Already the contract's note-pointer shape; Phase 3/4 consume it. |
| Grading primitive | A new matcher in Phase 1 | `Note.matchAnswer` (untouched) | Locked decision #4; Phase 2 extends, Phase 1 only carries state. |

**Key insight:** The contract already has the right primitives (`NoteTopology`, `Answer`, `AnsweredQuestion`, `NON_NULL` Jackson). Phase 1 is additive fields + regen — no new abstraction.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Answer.correct: boolean` only | + optional `outcome` enum + `matchedNoteId` (transient) | This phase | Third/fourth outcome representable without persistence change. |
| Hand-rolled API types | `@hey-api/openapi-ts` 0.99.0 from springdoc 3.0.3 | existing stack | Regenerate, never hand-edit. |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | JPA `@Transient` fields are serialized by Jackson and surfaced by springdoc into OpenAPI (→ optional TS fields). | Design Option A, Pitfall 3 | Field absent from contract → SC#1 unmet. Mitigation: grep `types.gen.ts` post-regen; fall back to Option B. `[CITED — verify]` |
| A2 | No whole-`Answer` JSON-equality test exists that would break on new optional fields. | Regression Risks | A pinned JSON test could fail. Mitigation: `rg` for `assertJson`/`jsonEquals` over answer payloads. `[ASSUMED]` |
| A3 | `AnswerOutcome` enum values `CORRECT/WRONG/ACCIDENTAL_MATCH/OVERLAP` match the states Phases 2–6 will return. | Design Options | Phase 2/6 might want different naming (e.g. `ACCIDENTAL`). Cosmetic; rename is cheap pre-wire. `[ASSUMED]` |
| A4 | `List<NoteTopology>` is sufficient topology for Phase 4's `LinkInsertionChoice` pre-select (needs id) + Phase 3 reveal (needs title). | Design Option A | Phase 4 might need `notebookId`/`ancestorFolders` → enrich to a `MatchedNote` DTO then. `[ASSUMED]` |

## Open Questions

1. **Enum vs inferred state (A3):** Should the contract carry an explicit `AnswerOutcome` enum, or infer accidental-match from `matchedNoteId != null` and overlap from `AnsweredQuestion.overlap`?
   - Recommendation: explicit enum (clearer for Phase 2/6 frontend), but A-min is acceptable if the developer prefers fewer types.
2. **Persist the matched-note id? (A4/Option C):** Phase 1 says no. If Phase 2 wants answer history to remember the matched note, that's a Phase 2 migration — out of scope here.
3. **`matchedNotes` richness:** bare `List<NoteTopology>` vs `List<MatchedNote { noteTopology, notebookId }>`. Defer enrichment to Phase 4 if `LinkInsertionChoice` needs notebook context.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Nix dev shell (`nix develop`) | All repo tooling | ✓ | nixos-26.05 | — |
| pnpm | Workspace scripts | ✓ | 11.11.0 | — |
| Gradle (backend) | `generateOpenAPIDocs`, `backend:test_only` | ✓ | 9.6.1 (wrapper) | — |
| `@hey-api/openapi-ts` | TS client regen | ✓ | 0.99.0 | — |
| springdoc-openapi | OpenAPI generation | ✓ | 3.0.3 | — |
| Redocly CLI | `openapi:lint` | ✓ | per `redocly.yaml` | — |
| `pnpm sut` (backend+frontend+LB) | Runtime/E2E | assumed running | — | `pnpm sut:healthcheck` |

**Missing dependencies with no fallback:** none — Phase 1 uses only the existing toolchain.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework (backend) | JUnit 5 (Spring Boot starter-test) |
| Framework (frontend) | Vitest 4.1.10 |
| Config (backend) | `backend/build.gradle`; `@SpringBootTest @ActiveProfiles("test") @Transactional` |
| Config (frontend) | `frontend/vitest.config.ts` |
| Quick run (backend) | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` |
| Quick run (frontend) | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/QuestionDisplay.spec.ts` |
| Full suite (frontend) | `CURSOR_DEV=true nix develop -c pnpm frontend:test` |
| Contract gate | `CURSOR_DEV=true nix develop -c pnpm generateTypeScript && pnpm openapi:lint` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-01 | `Answer` contract carries optional `matchedNoteId`/`outcome`; absent on existing spelling path | unit (backend) | `pnpm backend:test_only` (RecallPromptControllerTests) | ✅ existing (extend) |
| API-01 | Generated `Answer` TS type has the new optional fields | build/regen | `pnpm generateTypeScript` + grep `types.gen.ts` | ❌ Wave 0 (add grep check) |
| API-02 | `AnsweredQuestion` carries optional `overlap` + `matchedNotes`; absent on existing path | unit (backend) | `pnpm backend:test_only` (RecallsControllerTests) | ✅ existing (extend) |
| API-02 | Regenerated client compiles; frontend type-checks | build (frontend) | `pnpm frontend:test` | ✅ existing (green) |

### Sampling Rate
- **Per task commit:** `pnpm backend:test_only` + `pnpm generateTypeScript` + targeted `pnpm frontend:test` for recall components.
- **Per wave merge:** `pnpm frontend:test` + `pnpm openapi:lint`.
- **Phase gate:** Full backend + frontend green, OpenAPI lint green, `types.gen.ts` contains new fields, before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] Add a backend controller test asserting the new fields are **null/absent** on the existing spelling-answer path (proves "representable but not returned" — no behavior assertion).
- [ ] Add a regen-verification step: grep `packages/generated/doughnut-backend-api/types.gen.ts` for `matchedNoteId` / `outcome` / `matchedNotes` / `overlap`.
- [ ] No framework install needed — existing JUnit/Vitest infrastructure covers all requirements.

## Security Domain

`security_enforcement: true`, ASVS level 1, block on `high`. This phase changes only response DTO/schema types — no new inputs, endpoints, auth, crypto, or persistence.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Unchanged (existing OAuth2/Bearer). |
| V3 Session Management | no | Unchanged. |
| V4 Access Control | no | Existing endpoints keep `assertCanMutateRecallPrompt` / `assertReadAuthorization`; no new endpoint. |
| V5 Input Validation | no | No new `@RequestBody` types; `AnswerDTO`/`AnswerSpellingDTO` unchanged. New fields are response-only. |
| V6 Cryptography | no | No crypto touched. |

### Known Threat Patterns for this change

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Information exposure via new response fields | Information Disclosure | New fields are `@Transient`/optional and unset in Phase 1 (omitted via `NON_NULL`) — no matched-note data is returned until Phase 2 wires it behind existing `assertReadAuthorization`. |
| Mass-assignment of new fields | Tampering | `Answer` is response-only (no `@RequestBody` binds it); no inbound deserialization of the new fields. |

**Verdict:** No new attack surface. Phase 2 must re-check V4 (matched-note search across all readable notebooks) when it wires behavior.

## Sources

### Primary (HIGH confidence)
- Codebase: `backend/.../entities/Answer.java`, `controllers/dto/AnsweredQuestion.java`, `controllers/dto/RecallPromptHistoryItem.java`, `controllers/dto/RecalledNote.java`, `services/MemoryTrackerService.java`, `controllers/RecallPromptController.java`, `controllers/RecallsController.java`, `entities/Note.java` (`matchAnswer`), `configs/ObjectMapperConfig.java` (`NON_NULL`).
- Generated contract: `packages/generated/doughnut-backend-api/types.gen.ts:233-304`, `open_api_docs.yaml:3633-3692`.
- Skills/rules: `.cursor/skills/generate-api-client/SKILL.md`, `.cursor/rules/frontend-api.mdc`, `.cursor/rules/linting_formating.mdc`, `.cursor/rules/backend-code.mdc`, `.cursor/rules/backend-testing.mdc`.
- Tests: `RecallPromptControllerTests.java`, `RecallsControllerTests.java`, `AnsweredQuestionBuilder.ts`.

### Secondary (MEDIUM confidence)
- Jackson `JsonInclude.Include.NON_NULL` behavior — `[VERIFIED: ObjectMapperConfig.java:25]`.
- `@hey-api/openapi-ts` 0.99.0 + springdoc 3.0.3 regen path — `[VERIFIED: STACK.md, ARCHITECTURE.md]`.

### Tertiary (LOW confidence — flag for verification)
- JPA `@Transient` field serialization into OpenAPI/TS — `[CITED: Jackson/JPA semantics]` — **verify via regen + grep** (Pitfall 3 / A1).

## Project Constraints (from .cursor/rules/)

- **Nix prefix required** for all repo tooling: `CURSOR_DEV=true nix develop -c …`. Git commands run without the prefix. (general.mdc, user rule)
- **Never hand-edit** `packages/generated/doughnut-backend-api/**` or `open_api_docs.yaml` — regenerate via `pnpm generateTypeScript`. (agent-map.md, generate-api-client SKILL, linting_formating.mdc)
- **OpenAPI lint failures:** fix Java controllers, then regenerate; never edit the YAML. (linting_formating.mdc)
- **Backend return values:** prefer returning entities/existing API body types; introduce a DTO only when the wire shape differs. (backend-code.mdc) — informs the Option A (`@Transient` on entity) vs Option B (new DTO) trade-off.
- **Backend tests:** prefer controller-level behavior tests; run `pnpm backend:test_only` (no migration) or `pnpm backend:verify`. (backend-testing.mdc)
- **Frontend API:** import types from `@generated/doughnut-backend-api`; wrapped response `{ data, error, … }`; `apiCallWithLoading` for user actions. (frontend-api.mdc) — no Phase 1 UI work, but the regenerated types must keep this pattern intact.
- **Phase quality:** one observable behavior per phase; Phase 1 is Structure (no external behavior change); stop-safe; existing tests must stay green at the phase boundary. (planning.mdc)
- **Biome:** do not enable project-domain/MFA rules at root (Vue SFC resolution breaks). Use `format:all` while developing, `CI=true pnpm lint:all` for CI. (linting_formating.mdc)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new packages; existing springdoc/hey-api/redocly toolchain.
- Contract round-trip: HIGH — traced end-to-end in code; Jackson `NON_NULL` confirmed.
- Design options: HIGH for A/B; MEDIUM for the `@Transient`-serialization assumption (A1 — verify at regen).
- Pitfalls/regression: HIGH (deserialization is response-only); MEDIUM on absence of JSON-shape tests (A2 — grep to confirm).

**Research date:** 2026-07-23
**Valid until:** 2026-08-22 (stable; contract code rarely shifts fast)
