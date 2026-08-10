# Quick 010 — Focus Context XML + CLS cohesion

**Status:** in-progress (Phase 1 done)  
**Goal:** Make Focus Context presentation XML-tag + markdown (externally consistent across AI surfaces), then have commissioned Learning Session Requests reuse focus-note-only Focus Context (internally cohesive). ADR 0005 is open for change.

## Design decisions

1. **CLS scope = Option B (focus note only).** Each session item retrieves/renders Focus Context with `maxDepth = 0` (no related notes). Related-note expansion deferred.
2. **Full note content.** Drop CLS frontmatter stripping once Focus Context replaces the content bullet; Focus Context already presents full `note.content` with truncation and `doughnut-note-md` fences.
3. **Retrieval presentation = XML envelope + markdown body.** Replace `# Focus Context` / `## Focus Note` / `## Retrieved Note` with tags; keep metadata lines and fenced note bodies as markdown inside the tags.
4. **CLS envelope stays.** Keep `<instructions>`, `<session_item_titles>`, `<session_items>`, `<how_to_report>`, `### {title}`, and `- Learning status: …`. Replace `- Expected learning content: …` with an embedded `<focus_context>…</focus_context>` block per item (after a short interim where both exist).
5. **One renderer.** All Focus Context consumers go through [`FocusContextMarkdownRenderer`](../../../backend/src/main/java/com/odde/doughnut/services/focusContext/FocusContextMarkdownRenderer.java).
6. **Small commits.** Each phase below is one Behavior or Structure unit, stop-safe alone, sized for a single commit (~5 min including targeted tests).

### Target Focus Context shape (after Phases 1–3)

```markdown
<focus_context>
Purpose: Context around the focus note for AI use.
Max depth: 1

<focus_note>
Title: My Title
Notebook: My Notebook
Folder: path/here
Depth: 0

```doughnut-note-md
note body (may include frontmatter)
```
</focus_note>

<retrieved_note>
Title: Related
…
Reached by: OutgoingWikiLink

```doughnut-note-md
…
```
</retrieved_note>
</focus_context>
```

### Target CLS session item shape (after Phase 6; ADR 0005)

```markdown
### Hola
- Learning status: not yet tutored
<focus_context>
…
<focus_note>
Title: Hola
…
```doughnut-note-md
Hello
```
</focus_note>
</focus_context>
```

## Discoveries

- [`LearningSessionRequestMarkdownBuilder`](../../../backend/src/main/java/com/odde/doughnut/services/LearningSessionRequestMarkdownBuilder.java) is a parallel note presenter today.
- [`QuestionGenerationRequestBuilder.embedPropertyFocusInFocusContext`](../../../backend/src/main/java/com/odde/doughnut/services/QuestionGenerationRequestBuilder.java) anchors on `\n## Focus Note` — update in the `<focus_note>` phase.
- AI tool prompts / E2E OpenAI mocks assert `# Focus Context` — update in the matching header-tag phase.
- `maxDepth = 0` already skips related notes; name it `RetrievalConfig.focusNoteOnly()` in the Structure phase before CLS behavior.
- E2E pins `Expected learning content:` — remove only when the content bullet is deleted (Phase 6).

## Phases

### Phase 1 — Wrap Focus Context in `<focus_context>` (Behavior)

**Status:** done  
**Observable:** Rendered Focus Context opens/closes with `<focus_context>` / `</focus_context>` and no longer uses the `# Focus Context` markdown heading.

**Stop-safe:** Outer structure matches CLS-style XML envelopes; inner `## Focus Note` / `## Retrieved Note` unchanged until later phases.

**Work:**

1. TDD in [`FocusContextMarkdownRendererTest`](../../../backend/src/test/java/com/odde/doughnut/services/focusContext/FocusContextMarkdownRendererTest.java): assert `<focus_context>` wrapper; assert not `# Focus Context`.
2. Change only the outer header in [`FocusContextMarkdownRenderer`](../../../backend/src/main/java/com/odde/doughnut/services/focusContext/FocusContextMarkdownRenderer.java) (Purpose / Max depth stay inside the tag).
3. Retarget assertions/prompts that search for `# Focus Context`: question-gen / conversation unit tests, [`AiToolFactory`](../../../backend/src/main/java/com/odde/doughnut/services/ai/tools/AiToolFactory.java), [`NoteRefinementAiToolFactory`](../../../backend/src/main/java/com/odde/doughnut/services/ai/tools/NoteRefinementAiToolFactory.java) (outer name only), E2E [`openAiFocusContextRecallAssertions.ts`](../../../e2e_test/start/mock_services/openAiFocusContextRecallAssertions.ts) / [`conversation.ts`](../../../e2e_test/step_definitions/conversation.ts).

**Verify:** `pnpm backend:test_only`; targeted conversation / focus-context E2E spec(s) that assert the OpenAI payload marker.

---

### Phase 2 — Focus note section uses `<focus_note>` (Behavior)

**Status:** done  
**Observable:** Focus note block uses `<focus_note>…</focus_note>` instead of `## Focus Note`.

**Stop-safe:** Focus identity is tagged like the outer envelope; related notes still use `## Retrieved Note` until Phase 3.

**Work:**

1. TDD renderer: assert `<focus_note>`; assert not `## Focus Note`.
2. Renderer change for focus section only.
3. Update `embedPropertyFocusInFocusContext` to insert before `\n<focus_note>`.
4. Retarget unit/E2E/prompt strings that mention `## Focus Note` (conversation tests, NoteRefinement tool prompt).

**Verify:** `pnpm backend:test_only`; property-focus / conversation E2E if they assert the Focus Note marker.

---

### Phase 3 — Retrieved note section uses `<retrieved_note>` (Behavior)

**Status:** done  
**Observable:** Related notes use `<retrieved_note>…</retrieved_note>` instead of `## Retrieved Note`.

**Stop-safe:** Full Focus Context XML+markdown shape is complete for all consumers. CLS still uses the old content bullet (interim).

**Work:**

1. TDD renderer: assert `<retrieved_note>`; assert not `## Retrieved Note`.
2. Renderer change for retrieved sections only.
3. Retarget NoteRefinement (and any other) prompts that mention `## Retrieved Note`.
4. Update [`docs/focus-context/focus_context_retrieval_design.md`](../../../docs/focus-context/focus_context_retrieval_design.md) Markdown rendering section to the final XML+markdown shape.

**Verify:** `pnpm backend:test_only` (renderer + any retrieval markdown consumers).

---

### Phase 4 — `RetrievalConfig.focusNoteOnly()` (Structure)

**Status:** done  
**Structure for:** Phase 5 only.  
**Observable difference:** none (existing tests still pass). New factory returns `maxDepth = 0`.

**Work:**

1. Add `RetrievalConfig.focusNoteOnly()` (and overload with content budget if CLS will need it).
2. Small unit test on `RetrievalConfig` (maxDepth 0 / budget), not CLS yet.

**Verify:** `pnpm backend:test_only`.

---

### Phase 5 — CLS session items include focus-note-only Focus Context (Behavior)

**Status:** planned  
**Observable:** Each commissioned session item still has Learning status and `Expected learning content:`, **and** also embeds a focus-note-only `<focus_context>` block for that note.

**Stop-safe / interim:** Tutors already see Focus Context grammar; old bullet keeps existing E2E green. Phase 6 removes the duplicate.

**Work:**

1. Inject `FocusContextRetrievalService` + `FocusContextMarkdownRenderer` into [`LearningSessionRequestMarkdownBuilder`](../../../backend/src/main/java/com/odde/doughnut/services/LearningSessionRequestMarkdownBuilder.java).
2. Per session item: `retrieve(note, session.getUser(), RetrievalConfig.focusNoteOnly())` → `render`; append after Learning status (keep Expected learning content for now).
3. Extend [`LearningSessionCommissionTests`](../../../backend/src/test/java/com/odde/doughnut/controllers/LearningSessionCommissionTests.java): assert `<focus_context>` / `<focus_note>` / note body (or fence) present alongside existing content assertions.

**Verify:** `pnpm backend:test_only`. Do **not** change E2E yet (still passes via Expected learning content).

---

### Phase 6 — CLS drops Expected learning content; ADR 0005 matches (Behavior)

**Status:** planned  
**Observable:** Session items no longer include `Expected learning content:`; note body appears only via Focus Context. Proposed ADR 0005 Request example/prose match.

**Stop-safe:** Single note-presentation path inside CLS; protocol doc aligned. Full Quick 010 goal met.

**Work:**

1. Remove body-strip / Expected learning content from the builder.
2. Update commission unit tests: assert absence of `Expected learning content:`; keep Focus Context / Learning status assertions.
3. Update E2E feature + [`recallLearningSessionMethods.ts`](../../../e2e_test/start/pageObjects/recallLearningSessionMethods.ts) to assert Focus Context content (e.g. body text / `doughnut-note-md`), not the old bullet.
4. Revise Proposed [`docs/adrs/0005-commissioned-learning-session-protocol.md`](../../../docs/adrs/0005-commissioned-learning-session-protocol.md). Leave status Proposed.

**Verify:** `pnpm backend:test_only`; `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`.

## Out of scope

- Related-note expansion inside CLS session items (Option A)
- Machine transport / MCP for CLS
- Renaming `FocusContextMarkdownRenderer`
- Approving ADR 0005 (human process)

## Execution

Use **execute-plan** (or `/gsd-execute-phase` with local wrap-up). One commit + push per phase. Do not start until developer confirms.
