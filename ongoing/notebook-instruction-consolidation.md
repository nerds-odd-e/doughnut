# Notebook instruction consolidation

Consolidate notebook-level AI guidance onto the **note-property** mechanism and remove the
separate "Additional Instructions to AI" / "Assistant Management" feature entirely.

## Goals (from the request, refined)

1. **Remove** notebook "Additional Instructions to AI". The notebook-level instruction now comes
   from the `question_generation_instruction` property of the **notebook index** note.
2. **Remove the "Assistant Management" concept completely** — entity, endpoints, DTO, service
   methods, UI, generated client entries, tests, and the `notebook_ai_assistant` table. No
   historical trace in live schema, docs, or app code.
3. **Nested instructions, no override, labeled.** Question generation includes **every** level
   that defines a `question_generation_instruction` — notebook index, each folder on the trail
   (outermost → innermost), and the focus note — in a **single** user message, each block prefixed
   with its source.

## Confirmed decisions

- **Q (scope of replacement):** Only **question generation** consumes the notebook-index
  `question_generation_instruction`. The other flows that read the old notebook field — **chat
  conversation, question evaluation, note automation, question refinement** — simply stop receiving
  any notebook-level instruction. They are **not** rewired to the index property.
- **Q (labels):** Label by role + name:
  - `Instruction from notebook "<notebook title>":`
  - `Instruction from folder "<folder name>":`
  - `Instruction from the focus note:`
- **DB removal:** Add a new Flyway migration that **drops** `notebook_ai_assistant`. Do **not**
  modify the committed `V100000000__baseline.sql` (migrations are immutable). The regenerated ERD
  removes it from `docs/database-erd.md`.

## Grounding (current code)

- **Old field**: `NotebookAiAssistant.additional_instructions_to_ai` (1:1 with notebook). Read via
  `Note.getNotebookAssistantInstructions()`.
- **Consumers of `getNotebookAssistantInstructions()`** (all to be cut):
  - `QuestionGenerationRequestBuilder.addNotebookAssistantInstructionsIfPresent` (question gen)
  - `NoteQuestionGenerationService.evaluateQuestion` (evaluation)
  - `AiNoteAutomationService.buildStructuredResponseParams` (automation/evaluation)
  - `ConversationAiRequestBuilder.addNoteContext` (chat)
  - `AiQuestionGenerator.forNote` → `AiQuestionGeneratorForNote` (refinement)
- **Index-property resolution**: `NoteRealmService.resolveScopedQuestionGenerationInstruction`
  (nearest folder → parents → notebook root, **single** winner) +
  `resolveQuestionGenerationInstructions` (scoped winner + note frontmatter). Used only by
  `QuestionGenerationRequestBuilder` + `NoteRealmServiceTest`.
- **Prompt assembly**: `QuestionGenerationRequestBuilder.openAiResponseRequestForQuestionGeneration`
  builds one user message = `CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER + "\n" + join(blocks, "\n\n")`.
- **Folder trail with names**: `FolderTrailSegments.fromRootToContainingFolder(note)` (outermost
  first, `Folder::getName`).
- **Full removal surface** (Assistant Management):
  - Backend: `entities/NotebookAiAssistant.java`, `repositories/NotebookAiAssistantRepository.java`,
    `controllers/dto/UpdateAiAssistantRequest.java`, `NotebookController` `updateAiAssistant` +
    `getAiAssistant`, `NotebookService.findByNotebookId/save`, `Notebook.notebookAiAssistant` field,
    `Note.getNotebookAssistantInstructions()`.
  - Frontend: `components/notebook/NotebookAssistantManagementForm.vue`, the "Assistant Management"
    `<section>` + `aiAssistant`/`additionalInstructions`/`fetchAiAssistant` wiring in
    `pages/NotebookPageView.vue`, generated client (`types.gen.ts`, `sdk.gen.ts`, `api-summary.md`,
    `open_api_docs.yaml` — via regeneration only), `components.d.ts` entry.
  - Tests: `frontend/tests/notebooks/NotebookAssistantManagementForm.spec.ts`, assertions in
    `frontend/tests/pages/NotebookPage.spec.ts` + `NotebookPageView.spec.ts`,
    `NotebookSharingGroupControllerTest` `UpdateNotebookAiAssistant`/`GetNotebookAiAssistant`,
    relevant cases in `NoteQuestionGenerationServiceTests`, dead e2e method
    `notebookPage.updateAiAssistantInstructions`.
  - Schema/docs: `notebook_ai_assistant` in `V100000000__baseline.sql` (immutable; left as-is) +
    `docs/database-erd.md` (regenerated).
- **API spec**: backend OpenAPI is `backend/src/main/resources/api.yml`; regenerate the frontend
  client with the `generate-api-client` skill (`pnpm generateTypeScript`).

---

## Phases

### Phase 1 — Nested, labeled, no-override instructions in question generation *(Behavior)*

**Why first:** Standalone user value (richer question context) and it makes the notebook-index
instruction **reliably included** regardless of folder-level instructions, so the later removal of
the old field is a true no-op for question generation.

- **Pre:** A note whose notebook index, one or more ancestor folders, and the note itself each set
  `question_generation_instruction`.
- **Trigger:** Build a question-generation request for that note.
- **Post:** One user message contains **all** present instructions, ordered notebook → outer folder
  → … → inner folder → focus note, each prefixed with its source label (notebook title / folder
  name / focus note). No level is dropped due to another level being present.

**Work**
- Replace the nearest-wins logic: add a `NoteRealmService` method returning **ordered labeled
  blocks** (notebook index, each folder on the trail by name, then note frontmatter), omitting
  blanks. Retire `resolveScopedQuestionGenerationInstruction`'s single-winner use for this path.
- Update `QuestionGenerationRequestBuilder` to render the labeled blocks into the single custom
  instruction user message (keep `CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER` or fold labels in;
  preserve token-budget estimate over the combined block).

**Tests (capability-named, observable boundary)**
- `NoteRealmServiceTest`: rewrite the override-based cases (`inner_folder_index_wins…`,
  `scoped_resolution_skips_inner…`) to assert **all** levels present, ordered, no override; keep
  key-alias and note-only cases.
- `NoteQuestionGenerationServiceTests`: assert the first user message contains each level's labeled
  block in order (extend `shouldPlaceContainerThenNoteQuestionInstructionsInFirstUserMessage`).

### Phase 2 — Remove notebook Additional Instruction from app code *(Behavior)*

- **Pre:** App has the "Assistant Management" section + `/{notebook}/ai-assistant` endpoints.
- **Trigger:** Open the notebook settings; generate questions/chat/evaluate.
- **Post:** No "Assistant Management" UI, no `ai-assistant` endpoints; question generation relies on
  the notebook-index property (Phase 1); chat/evaluation/automation/refinement no longer inject any
  notebook-level instruction.

**Work**
- Backend: delete `updateAiAssistant`/`getAiAssistant`, `UpdateAiAssistantRequest`,
  `NotebookAiAssistant`, `NotebookAiAssistantRepository`, `NotebookService.findByNotebookId/save`,
  `Notebook.notebookAiAssistant`, `Note.getNotebookAssistantInstructions()`. Remove its use from the
  five AI flows (Q-gen, evaluation, automation, conversation, refinement) — including the
  `notebookAssistantInstructions` parameter threaded through `AiQuestionGenerator` →
  `AiQuestionGeneratorForNote`.
- Frontend: delete `NotebookAssistantManagementForm.vue`, remove the "Assistant Management" section
  and all `aiAssistant`/`additionalInstructions`/`fetchAiAssistant` wiring from `NotebookPageView.vue`.
- Regenerate the API client (`generate-api-client` skill); fix any consumers.
- Remove dead e2e method `notebookPage.updateAiAssistantInstructions`.

**Tests**
- Delete `NotebookAssistantManagementForm.spec.ts`; remove `UpdateNotebookAiAssistant` /
  `GetNotebookAiAssistant` nested classes and the notebook-assistant cases in
  `NoteQuestionGenerationServiceTests`; drop assistant assertions from `NotebookPage.spec.ts` /
  `NotebookPageView.spec.ts`. Existing notebook controller/page tests (minus those) must stay green.

### Phase 3 — Drop the table and erase remaining traces *(Structure / cleanup)*

**Justified by Phase 2:** the entity/endpoints are gone, so the table and its doc/schema references
are now dead.

**Work**
- Add `backend/src/main/resources/db/migration/V3000001XX__drop_notebook_ai_assistant.sql`
  (`DROP TABLE IF EXISTS notebook_ai_assistant;`). Do **not** edit the baseline.
- Regenerate `docs/database-erd.md` (`database-erd` skill) so `notebook_ai_assistant` disappears.
- Final sweep: confirm no remaining references to `AiAssistant` / `additionalInstructions` /
  `notebook_ai_assistant` in app code, tests, or docs (generated baseline CREATE is the only
  permitted immutable artifact).

**Verify**
- Backend `pnpm backend:verify` (runs migration) green; ERD diff shows only the table removed.

---

## Test / verify commands

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (Phases 1–2),
  `pnpm backend:verify` (Phase 3, migration).
- Frontend: `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
- API client: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` (Phase 2).
- ERD: `CURSOR_DEV=true nix develop -c pnpm export:database-erd` (Phase 3).

## Status

- Phase 1 — done. `NoteRealmService.questionGenerationInstructionBlocks(Note)` now returns ordered,
  source-labeled instruction blocks (notebook index → folders outermost→innermost → focus note,
  blanks omitted, no override); `QuestionGenerationRequestBuilder` renders them into the single
  custom-instruction user message. Removed the old single-winner `resolveScopedQuestionGenerationInstruction`
  / `resolveQuestionGenerationInstructions`. Notebook title label uses `Notebook.getName()`.
  Discovery for later phases: the full backend suite is currently flaky due to a pre-existing
  `Duplicate entry 'userNN' for key 'user.user_external_identifier'` collision in test user
  creation (reproduces on clean `main`); unrelated to these phases.
- Phase 2 — done. Removed the notebook "Assistant Management" feature from app code: deleted the
  `NotebookAiAssistant` entity, `NotebookAiAssistantRepository`, `UpdateAiAssistantRequest`, the
  `/{notebook}/ai-assistant` GET/PATCH endpoints, `NotebookService.findByNotebookId/save` (and its
  now-dead repo field), `Notebook.notebookAiAssistant`, and `Note.getNotebookAssistantInstructions()`.
  Cut the notebook-level instruction injection from all five AI flows (question generation,
  evaluation, automation, conversation, refinement — including the `notebookAssistantInstructions`
  param on `AiQuestionGeneratorForNote`). Deleted `NotebookAssistantManagementForm.vue` and its spec,
  removed the "Assistant Management" wiring from `NotebookPageView.vue`, dropped the dead e2e method
  `notebookPage.updateAiAssistantInstructions`, and removed assistant test cases/mocks. Also deleted
  the stray scratch file `backend/src/main/resources/api.yml` (it only held the removed snippet).
  Regenerated the API client. Discovery for Phase 3: the `notebook_ai_assistant` table still exists
  in the baseline DDL and `docs/database-erd.md`; Phase 3 adds the DROP migration + ERD regen. The
  full backend suite remains flaky via the pre-existing `Duplicate entry 'userNN'` test-user
  collision (touched classes verified green in isolation).
- Phase 3 — planned
