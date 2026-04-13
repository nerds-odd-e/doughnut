# Phase 11 — AI-assisted depth reorganization: Sub-phases

**Parent plan:** [`ongoing/book-reading-reorganize-layout-plan.md`](book-reading-reorganize-layout-plan.md) — Phase 11.

---

## Design decisions

### Data sent to AI

Minimize payload: send only what the AI needs to infer hierarchy from titles.

```json
[
  { "id": 42, "title": "1. Introduction", "depth": 0 },
  { "id": 43, "title": "1.1 Background", "depth": 0 },
  { "id": 44, "title": "2. Main Topic", "depth": 0 }
]
```

No content blocks, no bboxes, no layout sequence. The AI sees titles (which often contain numbering hints like "1.1", "Chapter 2", etc.) and current depths so it can propose corrections.

### AI response format (structured output)

Use the existing `InstructionAndSchema` + `requestAndGetJsonSchemaResult` pattern. Define a new DTO:

```java
@Data
public class BookLayoutReorganizationSuggestion {
  @JsonProperty(required = true)
  public List<BlockDepthSuggestion> blocks;

  @Data
  public static class BlockDepthSuggestion {
    @JsonProperty(required = true)
    public Integer id;
    @JsonProperty(required = true)
    public Integer depth;
  }
}
```

The structured output JSON schema constrains the AI to return exactly this shape. The backend validates:
1. Every input block ID appears exactly once in the response.
2. The suggested depths form a valid tree (same invariant as manual indent/outdent).
3. No ID is added or removed.

If validation fails, the suggestion is rejected (error to the frontend — not silently applied).

### Model choice and "thinking"

The task is: given a flat list of titles with current depths, propose correct nesting depths. This is mostly pattern matching on numbering ("1.", "1.1", "Chapter", section headings) — not deep reasoning. `gpt-4.1-mini` (the codebase default for `evaluation_model`) should be sufficient.

No special "thinking" / reasoning tokens are needed. The structured output schema already constrains the response. If results are poor with mini, upgrading to `gpt-4.1` is a config change, not a code change.

### ID matching strategy

By including block IDs in the prompt input and requiring the AI to echo them back in the structured output, we get a direct match. The backend validates that the returned ID set equals the input ID set before presenting the preview. This avoids positional matching (which would break if the AI reorders rows).

### Mocking in E2E

Use the existing `@usingMockedOpenAiService` infrastructure:
- Mountebank imposter on port 5001 (already set up by the tag hook).
- Step definition stubs `POST /chat/completions` with a `requestMessageMatches` predicate matching the system prompt (e.g. `.*reorganize.*layout.*`) and returns a `stubJsonSchemaResponse` containing the expected `BookLayoutReorganizationSuggestion` JSON.
- The mock response uses the same block IDs from the test fixture with modified depths.

### API shape

Two endpoints (suggest and apply are separate so the user sees a preview before committing):

| Verb | Path | Purpose |
|------|------|---------|
| `POST` | `…/book/reorganize-layout/suggest` | Sends blocks to AI, returns `BookLayoutReorganizationSuggestion` |
| `POST` | `…/book/reorganize-layout/apply` | Accepts the confirmed suggestion, applies depth changes, returns `BookMutationResponse` |

The suggest endpoint is a read-from-AI operation (no DB mutation). The apply endpoint bulk-updates all block depths in one transaction and validates the tree invariant.

### Preview UX

A modal dialog showing the block list with before/after depth columns. Blocks whose depth changed are highlighted. Two buttons: **Confirm** (calls apply) and **Cancel** (closes dialog, discards suggestion).

---

## E2E scenario (full, for E2E-led decomposition)

```gherkin
@usingMockedOpenAiService
Scenario: AI reorganizes the book layout depth
  When I request AI reorganization of the book layout
  Then I should see a reorganization preview dialog
  And the preview should show block "2. The Usual Definition Is Not Enough" with suggested depth 1
  When I confirm the AI suggestion
  Then the book block "2. The Usual Definition Is Not Enough" should be at depth 1 in the book layout
```

---

## Sub-phases

### Sub-phase 11.1 — Backend: suggest endpoint returns AI suggestion

**Status:** planned

**Behavior (tested through controller):** The suggest endpoint loads the book's blocks, sends titles+depths+IDs to the AI, and returns a `BookLayoutReorganizationSuggestion`.

**Why a separate phase:** This is a large structural change (new DTOs, AI prompt, `InstructionAndSchema`, service method, controller endpoint, response validation). Getting this working and tested through the controller before wiring frontend and E2E mocking reduces risk — the phased-planning heuristic "if making the test pass needs a large structural change, plan that structure as its own phase" applies.

**What changes:**

- **DTO:** `BookLayoutReorganizationSuggestion` with `List<BlockDepthSuggestion>` (structured output schema class). Each entry: `id` (Integer) + `depth` (Integer).
- **Prompt:** New `InstructionAndSchema` factory method that builds the system prompt ("Given these book blocks with titles and current depths, suggest the correct nesting depths…") paired with `BookLayoutReorganizationSuggestion.class`.
- **Service method:** In `BookService` (or a new collaborator): build the AI request from the book's blocks (titles + depths + IDs as user-message JSON), call `requestAndGetJsonSchemaResult`, validate the response (ID set match, valid tree invariant).
- **Controller endpoint:** `POST /api/notebooks/{notebook}/book/reorganize-layout/suggest` — delegates to service, returns suggestion DTO.
- **Test:** Controller test using the existing `OpenAIChatCompletionMock` to stub the AI response. Asserts the endpoint returns the expected suggestion shape and validates a round-trip (block IDs in → same IDs out with new depths).

---

### Sub-phase 11.2 — Frontend: trigger button and preview dialog (E2E)

**Status:** planned

**Behavior:** User clicks a button in the book layout sidebar. The app calls the suggest endpoint (AI is mocked via Mountebank), and a preview dialog appears showing the block list.

**E2E steps enabled:**

```gherkin
When I request AI reorganization of the book layout
Then I should see a reorganization preview dialog
# And the preview should show block "2. The Usual Definition Is Not Enough" with suggested depth 1
# When I confirm the AI suggestion
# Then the book block "2. The Usual Definition Is Not Enough" should be at depth 1 in the book layout
```

**What changes:**

- **Regenerate TS client:** `pnpm generateTypeScript` to pick up the new suggest endpoint from sub-phase 11.1.
- **E2E mock:** New step definition stubs `POST /chat/completions` via `mock_services.openAi().chatCompletion().requestMessageMatches(…).stubJsonSchemaResponse(…)` with a canned `BookLayoutReorganizationSuggestion` JSON using the test fixture's block IDs.
- **Frontend button:** In the book layout sidebar, a button (e.g. "AI Reorganize") triggers the suggest API call.
- **Frontend dialog:** On successful response, a modal/dialog opens showing the suggestion. For this sub-phase, the dialog just needs to be visible (the next sub-phase asserts its content).
- **E2E step definitions:** "When I request AI reorganization" clicks the button; "Then I should see a reorganization preview dialog" asserts the dialog element is visible.

---

### Sub-phase 11.3 — Preview shows block-level depth suggestions

**Status:** planned

**Behavior:** The preview dialog displays each block with its suggested depth, so the user can evaluate the AI's proposal before committing.

**E2E steps enabled (uncomment):**

```gherkin
When I request AI reorganization of the book layout
Then I should see a reorganization preview dialog
And the preview should show block "2. The Usual Definition Is Not Enough" with suggested depth 1
# When I confirm the AI suggestion
# Then the book block "2. The Usual Definition Is Not Enough" should be at depth 1 in the book layout
```

**What changes:**

- **Frontend dialog content:** Render the block list inside the preview dialog — each row shows the block title with visual indentation matching the suggested depth. Blocks whose depth changed from the current value are highlighted (e.g. different background or a change indicator).
- **E2E step definition:** "And the preview should show block {string} with suggested depth {int}" asserts the dialog contains a row for that block at the expected depth level.

**Why a separate sub-phase from 11.2:** Sub-phase 11.2 proves the vertical slice works end-to-end (button → API → mock → dialog visible). This sub-phase focuses on the dialog rendering the actual suggestion data correctly — a distinct aspect of the postcondition. Splitting keeps each red/green cycle small.

---

### Sub-phase 11.4 — Confirm AI suggestion and apply depth changes

**Status:** planned

**Behavior:** User confirms the previewed suggestion. The app applies all depth changes in one operation. The dialog closes and the layout reflects the new depths.

**E2E steps enabled (uncomment):**

```gherkin
When I request AI reorganization of the book layout
Then I should see a reorganization preview dialog
And the preview should show block "2. The Usual Definition Is Not Enough" with suggested depth 1
When I confirm the AI suggestion
Then the book block "2. The Usual Definition Is Not Enough" should be at depth 1 in the book layout
```

**What changes:**

- **Backend endpoint:** `POST /api/notebooks/{notebook}/book/reorganize-layout/apply` — accepts the `BookLayoutReorganizationSuggestion` (the confirmed depth array), validates tree invariant, bulk-updates all block depths in one transaction, returns `BookMutationResponse`.
- **Regenerate TS client:** Pick up the new apply endpoint.
- **Frontend confirm button:** In the preview dialog, "Confirm" calls the apply endpoint with the suggestion data. On success, merges the response into the current book (same `mergeBookMutationIntoFull` pattern) and closes the dialog.
- **E2E step definitions:** "When I confirm the AI suggestion" clicks Confirm; "Then the book block {string} should be at depth {int} in the book layout" reuses the existing depth assertion step.

---

## Open questions (to resolve during implementation)

1. **Prompt tuning:** The exact system prompt wording will need iteration. Start with a straightforward instruction and refine based on real AI output quality (outside E2E — E2E uses mocks).
2. **Error UX:** What happens when the AI returns an invalid suggestion (bad IDs, invalid tree)? Simplest: show an error toast and let the user retry. Could be a follow-up sub-phase if it's never hit in practice, or handle inline with validation in the suggest endpoint (sub-phase 11.1 already validates).
3. **Large books:** If a book has hundreds of blocks, the prompt may be long. For now, send all blocks. If token limits become an issue, consider pagination or summarization in a later phase.
4. **Cancel:** Trivial — just close the dialog. Included implicitly in sub-phase 11.2's dialog (standard dismiss behavior). No separate sub-phase needed unless the team wants an explicit E2E assertion for cancel.
