# Backend review (current codebase)

This note was refreshed to reflect **the tree as it exists in the workspace now**, not a historical commit range.

---

## 1. Dead code (no production caller; unit tests alone do not justify keeping it)

### `regenerateDetailsFromPoints` chain — **removed**

Deleted `NoteAutomationService.regenerateDetailsFromPoints`, `ChatCompletionNoteAutomationService.regenerateDetailsFromPoints`, and `AiToolFactory.regenerateDetailsFromPointsAiTool`. `RegeneratedNoteDetails` remains for `removePointsFromDetailsAiTool` / `removePointsAndRegenerateDetails`.

---

## 2. Things to simplify

### `AiController`: duplicated promote handlers

`promotePointToChild` and `promotePointToSibling` are the same shape: authorize, `getSinglePoint`, call automation, null-check AI result, delegate to `NoteConstructionService`. A private helper parameterized by **which** promote operation and **which** construction method reduces drift.

### `ChatCompletionNoteAutomationService`: duplicated note context for promote

`promotePointToChild` and `promotePointToSibling` both compute `noteTitle` / `noteDetails` the same way. A small private method (e.g. `currentNoteTitleAndDetails()`) removes duplication.

### AI failure handling

`throw new RuntimeException("AI failed to generate extraction result")` on null `PointExtractionResult` is generic and tends to surface as **500**. A dedicated exception or `ResponseStatusException` with a stable client-facing shape improves debuggability without changing success behavior.

### Client validation vs `IllegalArgumentException`

`removePointFromNote` throws `IllegalArgumentException` for empty details or empty points list. Unless a global handler maps that to **400**, clients may see **500**. Prefer `ResponseStatusException(HttpStatus.BAD_REQUEST, …)` or bean validation on `PointsRequestDTO` so HTTP semantics match validation failures.

### `getSinglePoint` vs list size

The error says points must contain **exactly** one point, but the implementation only checks for empty and then uses `getFirst()`. Extra points are silently ignored. If the contract is exactly one, assert `points.size() == 1`.

---

## 3. Duplicate code and weak cohesion

### Question-generation request construction

`NoteQuestionGenerationService.generateQuestionWithChatCompletion` rebuilds the chat flow starting from `QuestionGenerationRequestBuilder.getChatRequestBuilder(note)` and then re-applies notebook instructions, `additionalMessage`, and `responseJsonSchema` — parallel to `QuestionGenerationRequestBuilder.buildQuestionGenerationRequest`, but with a **different order** (`additionalMessage` is applied **before** the schema in the service, **after** in the builder). That split makes it easy for the two paths to drift (wording, ordering, future flags). **Higher cohesion:** drive real generation through one builder path, or extract a shared internal API that both call.

### `QuestionGenerationRequestBuilder.getChatRequestBuilder` copy vs MCQ flow

The leading user text still says “question **and choice** generator” while other layers sometimes say “question generator” only. Aligning wording with `AiToolFactory` / MCQ prompts reduces conceptual duplication.

### Remove-point: response-only vs promote: persist-on-server

`removePointFromNote` intentionally **does not** persist the note; `AiControllerTest` asserts `shouldNotModifyNoteInDatabase`, and the frontend applies `data.details` via `storedApi.updateTextField` (`NoteRefinement.vue`). Promote endpoints persist via `NoteConstructionService`. That is a valid split but **easy to misuse** for any other API client. Cohesion improvement without changing behavior: document the contract in OpenAPI description or a short class-level note on the controller method so “compute-only” is obvious.

---

## 4. Other improvements (no new product behavior)

### `MvcConfig` and static caching

Current `MvcConfig` correctly uses `@Configuration` and sets a short cache for `/index.html`. No issue noted; this supersedes an earlier review that referred to an `@EnableWebMvc`-only config that **does not** exist in the current tree.

### `application.yml` resource cache

All shown profiles use `spring.web.resources.cache` consistently (no `spring.resources` mismatch in the current file).

### Prompt robustness for remove / promote tools

User-controlled strings are embedded in prompts in `AiToolFactory` (e.g. `removePointsFromDetailsAiTool`, promote instructions). Quotes or newlines in input can weaken or break the instruction block. Escaping, structured message parts, or length limits are hardening options, not feature changes.

### `NoteRefinement.vue` “ignore” action

`ignoreSelectedPoints` only clears the selection after confirm and does not call the backend. If that is intentional interim UI, fine; if it is supposed to mirror prior “ignore points” behavior, it is a product gap (called out only as consistency with UI expectations, not as a backend-only fix).

---

## Quick reference

| Topic | Suggestion |
|--------|------------|
| `regenerateDetailsFromPoints` | Removed; `RegeneratedNoteDetails` kept for remove-points |
| Promote endpoints / service | Deduplicate controller and `ChatCompletionNoteAutomationService` |
| Question generation | Unify builder vs `generateQuestionWithChatCompletion` ordering |
| HTTP errors | Map validation failures to 400; avoid bare `RuntimeException` for AI null |
| `getSinglePoint` | Enforce exactly one point if that is the contract |
| Remove-point API | Document “response-only; client persists” for API consumers |
