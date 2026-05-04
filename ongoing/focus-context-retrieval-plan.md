# Focus Context Retrieval — Phased Plan

Drives the requirements in `ongoing/focus-context-retrieval-requirements.md`. Replaces the legacy GraphRAG mechanism (`docs/graph RAG/graph_RAG_design.md`, `backend/src/main/java/com/odde/doughnut/services/GraphRAGService.java`, `services/graphRAG/**`) with the new **Focus Context Retrieval** mechanism aligned with the wiki/Obsidian note model.

The plan is **stop-safe**: every phase is independently shippable, ordered by user value (question generation first, then chat / note automation, then API / MCP, then dead-code removal). Earlier phases keep the old code paths working until their consumer is migrated.

## Naming conventions used by all phases

When phases below mention names, use these (capability-based, not phase-based):

- `FocusContextRetrievalService` — replaces `GraphRAGService`.
- `FocusContextResult`, `FocusContextFocusNote`, `FocusContextNote` — replace `GraphRAGResult`, `FocusNote`, `BareNote`.
- `FocusContextEdgeType { OutgoingWikiLink, InboundWikiReference, FolderSibling }` — replaces `RelationshipToFocusNote`.
- `FocusContextMarkdownRenderer` — new; turns a `FocusContextResult` into the prompt-ready Markdown described in the requirements.
- Java package: `com.odde.doughnut.services.focusContext` (replaces `services.graphRAG`).
- Docs: `docs/focus-context/focus_context_retrieval_design.md` (replaces `docs/graph RAG/graph_RAG_design.md`).

The legacy class names stay until phase 5 so that intermediate phases compile.

## AI prompt modernization (cross-cutting)

`AiToolFactory.getBaseInstruction()` and `getDefaultMcqPrompt()` are rewritten in **Phase 1** alongside the question-generation switch and updated again in **Phase 2** when depth-2 evidence appears. New principles:

- Drop legacy phrasing about "atomic notes organized hierarchically with relations / lateral links" — Doughnut is now wiki/Markdown with `[[Note Title]]` links and folders inside notebooks.
- State the role declaratively (one short paragraph), not as numbered rules of the road.
- Treat the focus context as **hidden context** delivered in a fenced Markdown block (`# Doughnut Focus Context …`), and tell the model the user does **not** see it.
- Tell the model to use the focus note's title and details as the question subject, and to use other retrieved notes only as evidence / distractor seed material.
- Forbid pronouns referring to the hidden context ("this note", "the focus note", "above", "the following note").
- Move concrete MCQ formatting rules (3 choices, vary length, `strictChoiceOrder`, distractor plausibility) into a small, non-numbered section that sits next to the schema, since the structured-output schema already enforces shape.
- Defer reasoning to the model (it already silently chains thoughts in modern OpenAI models); do not add explicit "think step by step" instructions.

The same modernization applies to `questionEvaluationAiTool` and `questionRefineAiTool` in Phase 2 (they reference "hierarchical knowledge graph" / "contextual path" wording that no longer matches the wiki model).

---

## Phase 1 — Question generation uses Markdown focus context with depth-1 wiki links (Behavior)

### User value
The most-visible AI feature (predefined-question / recall question generation) starts using the new wiki-aware context. The model sees a Markdown-rendered focus note plus its directly linked notes and inbound referrers, instead of a JSON dump labeled "Focus Note and the notes related to it". Distractors and stems can already draw on actual outgoing wiki link targets, which the legacy GraphRAG only handled via `OutgoingWikiLinkRelationshipHandler` mixed into a JSON shape the model had to parse.

### Scope
- Add internal models `FocusContextResult`, `FocusContextFocusNote`, `FocusContextNote`, `FocusContextEdgeType` matching the requirements doc shape (with `depth`, `retrievalPath`, `edgeType`, `reason`, `details`, `detailsTruncated`).
- Add `FocusContextRetrievalService.retrieve(focusNote, viewer, RetrievalConfig)` returning a `FocusContextResult`. In this phase, traversal stops at depth 1 and only emits `OutgoingWikiLink` and `InboundWikiReference` edges. No folder siblings yet, no depth-2 expansion.
- Reuse `WikiTitleCacheService` for outgoing targets and inbound referrers; reuse `NoteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds` for batch hydration.
- Add `FocusContextMarkdownRenderer.render(FocusContextResult, RetrievalConfig)` producing the Markdown wrapper described in the requirements:
  - `# Doughnut Focus Context` header with `Max depth` line.
  - `## Focus Note` block with title, notebook, folder path, depth `0`, truncation flag, and content fenced as `doughnut-note-md`.
  - `## Retrieved Note` blocks per related note, with title, notebook, folder, depth, retrieval path (`[[A]] -> [[B]]`), edge type, truncation flag, and content fenced.
  - **Safe fence rule**: scan note content for the longest run of backticks and use `longestRun + 1` for the wrapper fence (focus and retrieved blocks each compute their own fence).
- Add token-budget split: `FOCUS_NOTE_DETAILS_MAX_TOKENS` and `RELATED_NOTE_DETAILS_MAX_TOKENS` constants in a `FocusContextConstants` class. Always keep the focus note (truncated if needed); related notes consume the remaining budget in BFS order.
- Wire `QuestionGenerationRequestBuilder.getChatRequestBuilder` and `PredefinedQuestionController` to call `FocusContextRetrievalService` + `FocusContextMarkdownRenderer` instead of `graphRAGService.getGraphRAGDescription(note)`.
- Modernize `AiToolFactory.getBaseInstruction()` and `getDefaultMcqPrompt()` per the cross-cutting section above. Drop the "atomic / hierarchical / lateral links" wording and the "Leverage the Extended Graph" numbered point.
- Leave `GraphRAGService` and the legacy `services/graphRAG` package in place; other consumers (`ChatCompletionConversationService`, `ChatCompletionNoteAutomationService`, `SuggestedQuestionForFineTuningService`, `NoteController.getGraph`, MCP `get_note_graph`) still use the old path until phases 4 / 5.

### Test plan
- **E2E (extends existing capability files; no new files):**
  - `e2e_test/features/recall/recall_quiz_ai_question.feature` — existing scenarios still pass when generation is run via the mocked OpenAI service. The mock's recorded prompt body now contains `# Doughnut Focus Context` and a `doughnut-note-md` fence instead of the legacy `Focus Note and the notes related to it: { ... }` JSON banner.
  - `e2e_test/features/ai_generated_recall_questions/user_feedback_for_question_generation.feature` — existing scenarios still pass.
  - One new `@wip` scenario in `recall_quiz_ai_question.feature` where the focus note has an outgoing wiki link `[[Bahamas]]`, the linked note's details are the only place a fact lives, and the generated MCQ uses that fact (Mountebank verifies the prompt contains the linked-note content under a `## Retrieved Note` block with `Reached by: OutgoingWikiLink`). Remove `@wip` once the prompt body matches.
- **Unit (Java, package `services.focusContext`):**
  - `FocusContextMarkdownRendererTest`: safe-fence selection (note with no backticks → 3 backticks; note with `` ``` `` → 4; note with `` ```` `` → 5); focus block always emitted even with empty related notes; truncation flag rendered when set; retrievalPath of `[[A]] -> [[B]]` for a depth-1 outgoing link.
  - `FocusContextRetrievalServiceTest` (Spring + `@Transactional`): focus only when no links; outgoing wiki targets emitted with `OutgoingWikiLink` and depth `1`; inbound referrers emitted with `InboundWikiReference`; deduplication when the same note is both an outgoing target and an inbound referrer (outgoing wins, single entry); token budget caps the related list; focus note details truncated to `FOCUS_NOTE_DETAILS_MAX_TOKENS` with `detailsTruncated=true`.

### Definition of done
- New scenario passes (`@wip` removed).
- All existing question-generation E2E and unit tests still pass.
- `QuestionGenerationRequestBuilder` no longer calls `graphRAGService.getGraphRAGDescription`.
- `AiToolFactory.getBaseInstruction()` / `getDefaultMcqPrompt()` updated; their Java-side tests (if any) updated to match.

---

## Phase 2 — Question generation context expands to depth 2 (Behavior)

### User value
Many Doughnut notes use a "relationship note" pattern (a note whose body links two other notes via wiki links and front matter). At depth 1 the model only sees the relationship note's body; at depth 2 it sees the actual subject and target notes. This is the depth justification spelled out in the requirements example.

### Scope
- Replace single-step expansion in `FocusContextRetrievalService` with a true breadth-first traversal up to a configurable max depth (default `2`, settable in `RetrievalConfig`).
- Edge prioritization within a depth: `OutgoingWikiLink` before `InboundWikiReference`. Folder siblings still excluded (Phase 3).
- Each `FocusContextNote` carries the full `retrievalPath` from focus (list of wiki URIs `[[A]] -> [[B]] -> [[C]]`) and the `edgeType` of the **last** hop that reached it.
- Deduplicate by internal note identity (note id), not by the wiki URI string. A note discovered via two paths keeps its **shortest** retrieval path.
- Apply token budget per added note as before; stop traversal when budget is exhausted **or** max depth is reached. Frontier batching uses `NoteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(ids)` once per depth ring.
- Renderer emits `Path: [[Focus]] -> [[Mid]] -> [[Far]]` for depth-2 notes.
- Update `AiToolFactory` MCQ prompt to acknowledge that the focus context may include indirect notes reached through other notes' wiki links / front matter — the model should still anchor the question on the focus note's own content, not on a depth-2 fact.

### Test plan
- **E2E (extends `recall_quiz_ai_question.feature`):**
  - One new `@wip` scenario where the focus note links `[[Mid]]`, `[[Mid]]` links `[[Far]]`, and only `[[Far]]` contains the distractor fact. With max depth 2 the prompt must include a `## Retrieved Note` block for `[[Far]]` whose `Path:` line shows `[[Focus]] -> [[Mid]] -> [[Far]]` and `Reached by: OutgoingWikiLink`. Mountebank fixture asserts the prompt body. Remove `@wip` once it passes.
- **Unit:**
  - `FocusContextRetrievalServiceTest` BFS cases: depth-2 outgoing chain reaches the leaf; depth-2 mixed (`Outgoing` then `Inbound`); cycle `A -> B -> A` does not loop; multi-path note keeps the shorter path; max depth 1 in config still produces the Phase 1 result; budget cuts off mid-frontier and remaining frontier is dropped.
  - `FocusContextMarkdownRendererTest`: depth-2 path string formatting; truncation propagated independently per note.

### Definition of done
- Depth-2 scenario passes; existing scenarios untouched.
- Configurable `maxDepth` defaults to `2`; verified in test.
- No N+1 queries: a single repository call per depth ring (a unit-level assertion using a `JdbcTemplate`-counting test or a Spring `Statistics` check is acceptable; otherwise document as a manual check and rely on existing repository batch helpers).

---

## Phase 3 — Folder sibling sampling for question generation, with caller-supplied seed (Behavior)

### User value
For question generation, peer notes from the same folder (or notebook root) are excellent distractor seeds. Sampling them randomly increases distractor variety across regenerations. Storing the seed on the generated `PredefinedQuestion` lets us reproduce the exact context that produced a given question (useful for the "contest / regenerate" flow and for fine-tuning data export).

### Scope
- Add `FolderSibling` edge type. Sampling rules from the requirements:
  - Per reached note, sample up to `MAX_FOLDER_SIBLINGS_PER_NOTE` (default e.g. 5) peers from the same folder (or notebook root if no folder).
  - Folder siblings are **not** expanded as frontiers.
  - Sampling uses a `java.util.Random` seeded by a caller-supplied `long` seed; same seed + same note set → identical sample.
  - When the seed is absent, sampling is deterministic by ascending note id (current `findStructuralPeerNotesInOrder` order, capped at `MAX_FOLDER_SIBLINGS_PER_NOTE`).
- Token budget: reserve a small bounded slice for siblings so they cannot crowd out wiki-linked notes (e.g. siblings collectively capped at 25% of the related-note budget). Implementation detail; tunable in `FocusContextConstants`.
- Renderer emits `Reached by: FolderSibling` and a `Path:` line that ends at the parent note that produced the sibling.
- DB migration `V300000173__predefined_question_context_seed.sql`: add nullable `context_seed BIGINT` column to `predefined_question`.
- `PredefinedQuestion` entity: add `Long contextSeed`. `PredefinedQuestionController` (or `QuestionGenerationRequestBuilder`) generates a `ThreadLocalRandom.nextLong()` seed per generation, passes it to `FocusContextRetrievalService`, and stores it on the saved `PredefinedQuestion`.
- Fine-tuning training-data export (`SuggestedQuestionForFineTuningService`) is **not** changed in this phase (still uses old GraphRAG path); Phase 4 will switch it and reuse the stored seed for replay.

### Test plan
- **E2E (extends `recall_quiz_ai_question.feature`):**
  - One new `@wip` scenario: focus note has 4 folder siblings, no wiki links. With a fixed seed (set via test config / `TestabilitySettings`), the prompt body lists exactly the expected sibling subset under `## Retrieved Note` blocks tagged `Reached by: FolderSibling`. Re-running with the same seed produces the same prompt; with a different seed produces a different subset. Remove `@wip` once it passes.
- **Unit:**
  - `FocusContextRetrievalServiceTest`: with seed `42` and a known folder, the sampled siblings match a fixed list; with no seed, siblings are the first `MAX_FOLDER_SIBLINGS_PER_NOTE` by ascending note id; siblings of a depth-1 wiki-link target are sampled from **its** folder, not the focus's folder; siblings are not used as expansion frontiers (a sibling that itself links to another note must not appear as a depth-2 wiki-link node).
  - `PredefinedQuestionControllerTests`: when a question is generated, the persisted `PredefinedQuestion.contextSeed` is the seed used for retrieval; regenerating with the same seed produces the same prompt body in the mocked OpenAI fixture.

### Definition of done
- Migration applied and verified by `pnpm backend:verify`.
- Seed round-trips through `PredefinedQuestion`.
- Folder-sibling scenarios pass with fixed seeds in mocked OpenAI tests.

---

## Phase 4 — Note assistant chat, note automation, and fine-tuning export use the new Markdown context (Behavior)

### User value
Every AI feature that previously consumed `GraphRAGService.getGraphRAGDescription` now sees the same modern wiki-aware Markdown context as question generation. Consistency across features (chat about a note, suggest title, understanding checklist, promote-point-to-sibling, fine-tuning data export) and improved relevance because outgoing wiki links and depth-2 reasoning are now available to those flows too.

### Scope
- Switch the following call sites from `graphRAGService.getGraphRAGDescription(note)` to `focusContextRetrievalService.retrieve(...)` + `focusContextMarkdownRenderer.render(...)`:
  - `ChatCompletionConversationService` (via `ConversationHistoryBuilder.addNoteContext`).
  - `ChatCompletionNoteAutomationService.createChatRequestBuilder` (uses `OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder`).
  - `SuggestedQuestionForFineTuningService` — when exporting a `PredefinedQuestion` as training data, replay the retrieval using the stored `contextSeed` from Phase 3 so the prompt matches what the model actually saw.
- `OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder`'s docstring is updated to point at `FocusContextMarkdownRenderer` rather than `GraphRAGService#getGraphRAGDescription`.
- Conversation/automation prompts are reviewed for stale GraphRAG-flavored wording (e.g. "this note", "the contextual path", references to "knowledge graph") and updated to the modernized style from the cross-cutting section.

### Test plan
- **E2E:**
  - `e2e_test/features/messages/chat_about_a_note.feature` — existing scenarios pass; the recorded mocked-OpenAI request body for the system message contains the Markdown wrapper (`# Doughnut Focus Context`) instead of the JSON banner.
  - `e2e_test/features/ai_generated_recall_questions/user_feedback_for_question_generation.feature` — admin training-data retrieval (the fine-tuning export) returns rows whose `prompt` contains the rendered Markdown, and the example for a previously generated `PredefinedQuestion` reproduces the original sibling sample by replaying its `contextSeed`.
- **Unit:**
  - `ConversationHistoryBuilderTest` — system message body switched from JSON to Markdown; assertions updated to match.
  - `ChatCompletionNoteAutomationServiceTest` and `NoteAutomationServiceTests` — same.
  - `SuggestedQuestionForFineTuningServiceTest` (extend or add): given a `PredefinedQuestion` with a stored `contextSeed`, the exported prompt equals the original generation prompt for the same focus note.

### Definition of done
- All four consumers no longer call `getGraphRAGDescription`.
- Existing chat / note-automation / fine-tuning E2E and unit tests pass with updated expected bodies.

---

## Phase 5 — `/notes/{note}/graph` API, MCP tool, and OpenAPI types switch to `FocusContextResult`; legacy GraphRAG package removed (Behavior + cleanup)

### User value
External integrators (the MCP server and any direct API consumers) see the new shape too. Removing the legacy package eliminates parallel concepts ("GraphRAG" vs "Focus Context") and prevents a maintainer from accidentally extending the dead path.

### Scope
- Rename the controller endpoint return type from `GraphRagResult` to `FocusContextResult` in `NoteController.getGraph`. The OpenAPI path may stay `/notes/{note}/graph` for now (URL stability), but the schema and DTO names match the new model. Regenerate the TS client (`pnpm generateTypeScript`).
- Rename JSON property `relationToFocusNote` to `edgeType` and add `depth`, `retrievalPath`, `folderPath`, `outgoingLinks`, `inboundReferences`, `sampleSiblings`, etc. per the TS shape in the requirements.
- Update `mcp-server/src/tools/get-note-graph.ts`:
  - Tool description rewritten to describe the wiki-link / inbound-reference / folder-sibling traversal (the current description still talks about "Parent notes / Child notes / Sibling notes" — outdated).
  - Tool keeps the name `get_note_graph` (stable surface) but its returned JSON now matches `FocusContextResult`.
  - Update `mcp-server/tests/tools/get-note-graph.test.ts` to assert the new shape.
- Delete the old internal package and class:
  - `backend/src/main/java/com/odde/doughnut/services/GraphRAGService.java`
  - `backend/src/main/java/com/odde/doughnut/services/graphRAG/**` (`GraphRAGResult`, `BareNote`, `FocusNote`, `RelationshipToFocusNote`, `RelationshipHandler` family, `PriorityLayer`, `UriAndTitle`, `OneTokenPerNoteStrategy`, `CharacterBasedTokenCountingStrategy`, `GraphRAGConstants`, `GraphNoteWikiUri` — keep `GraphNoteWikiUri` only if still used by the new renderer; otherwise inline its logic).
  - `backend/src/test/java/com/odde/doughnut/services/GraphRAGServiceTest.java` and the `services/graphRAG/**` test package — port the still-relevant assertions into `FocusContextRetrievalServiceTest` / `FocusContextMarkdownRendererTest` first, then delete (use `trash`).
- Replace `docs/graph RAG/graph_RAG_design.md` with `docs/focus-context/focus_context_retrieval_design.md` describing the new model. Remove the old folder (`trash`).
- Delete `ongoing/focus-context-retrieval-requirements.md` and `ongoing/focus-context-retrieval-plan.md` once the deploy gate of this phase is green.

### Test plan
- **E2E:**
  - Existing `e2e_test/features/recall/recall_quiz_ai_question.feature` and `e2e_test/features/messages/chat_about_a_note.feature` still pass (no direct dependency on the `/graph` endpoint shape, but they exercise the full retrieval path).
  - If any feature directly asserts `/graph` JSON, update the expected fields. Search at start of phase: `rg "graph" e2e_test`.
- **Unit:**
  - `NoteControllerTests.getGraph` updated to assert new field names and presence of `edgeType` / `depth` / `retrievalPath`.
  - `mcp-server/tests/tools/get-note-graph.test.ts` updated.
- **MCP smoke check:** run `pnpm -C mcp-server build` and exercise the rebuilt tool description against a known note to confirm the JSON parses with the new shape.

### Definition of done
- `rg -l "GraphRAG|graphRAG|GraphRag|graphRag"` returns nothing (except possibly a deprecation note in commit history).
- New OpenAPI types compile in the frontend (`pnpm generateTypeScript` clean).
- All targeted E2E specs above pass; backend `pnpm backend:verify` green; `pnpm -C mcp-server test` green.
- `ongoing/focus-context-retrieval-requirements.md` and `ongoing/focus-context-retrieval-plan.md` removed.

---

## Risks and notes

- **Mocked OpenAI fixtures:** Mountebank stubs that pin specific request bodies will break the moment the Phase 1 prompt body changes. Update them in the same commit as the prompt change; rely on existing helpers (`@usingMockedOpenAiService`).
- **Token-budget regressions:** the legacy code budgets in characters via `CharacterBasedTokenCountingStrategy`. The new constants are token-named but the implementation can keep the same character-based estimator under the hood; do **not** introduce a new tokenizer dependency in this work.
- **Reproducibility of sibling sampling for already-stored questions:** before Phase 3 ships, existing `predefined_question` rows have no `context_seed`. The fine-tuning export (Phase 4) treats a null seed as "use deterministic id-ascending order" so historical rows still produce a stable prompt.
- **Legacy `OutgoingWikiLinkRelationshipHandler` already exists** — its logic (resolve via `WikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer`) is reused inside the new BFS; the handler class itself is deleted in Phase 5.
- **`GraphNoteWikiUri.of`** is still useful for rendering `[[Notebook: Title]]` strings in the new path / link lists. Move it into the new package as `FocusContextWikiUri` (or keep the name) in Phase 1; delete the old one in Phase 5.
