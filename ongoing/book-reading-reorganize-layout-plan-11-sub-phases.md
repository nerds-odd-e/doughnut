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
|------|------|------|
| `POST` | `…/book/reorganize-layout/suggest` | Sends blocks to AI, returns `BookLayoutReorganizationSuggestion` |
| `POST` | `…/book/reorganize-layout/apply` | Accepts the confirmed suggestion, applies depth changes, returns `BookMutationResponse` |

The suggest endpoint is a read-from-AI operation (no DB mutation). The apply endpoint bulk-updates all block depths in one transaction and validates the tree invariant.

### Suggest flow: loading, errors, dialog title

- **While suggest is in flight:** The **entire book layout** UI is **masked**; a **spinner** is **centered in the visible layout region** (viewport of the layout panel, not the whole app chrome — align with whatever root element wraps the book layout sidebar/tree).
- **When suggest fails** (network, 4xx/5xx, validation error from backend): **do not** swallow errors — let the existing API/error path **surface a toast** (same pattern as other book actions). No bespoke inline error panel for 11.2.x unless a later phase requires it.
- **Preview dialog:** Use a **fixed, user-visible dialog title** string so E2E and a11y are stable. Canonical copy: **`Reorganize layout (preview)`** — use this exact string in the modal heading and in step definitions that assert the dialog is open.

### Preview UX

A modal dialog (title: **`Reorganize layout (preview)`**) showing the block list with before/after depth columns. Blocks whose depth changed are highlighted. Two buttons: **Confirm** (calls apply) and **Cancel** (closes dialog, discards suggestion).

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

### Sub-phase 11.2.1 — Frontend: trigger, layout mask, spinner, error toast

**Status:** planned

**Behavior:** User clicks **AI Reorganize** (or the chosen sidebar label) in the book layout. The app calls the suggest endpoint. While the request is pending, the **book layout is fully masked** with a **spinner centered in the layout’s visible area**. If the request **fails**, the error propagates through the normal API path and the user sees a **toast** (no silent failure).

**Tests:** Prefer **Vitest** (mounted book layout / composable) for mask + spinner + error path; optional narrow E2E later in 11.2.3 if a single failing mock is easy to maintain.

**What changes:**

- **Regenerate TS client:** `pnpm generateTypeScript` after sub-phase 11.1 (if not already current).
- **Sidebar button** in the book layout triggers the suggest API call (via existing `apiCallWithLoading()` or equivalent so errors surface to toast).
- **Loading UI:** Overlay on the layout root; centered spinner; dismiss overlay when the request settles (success or failure).

**Out of scope for 11.2.1:** Opening the preview dialog on success (11.2.2).

**Interim:** If 11.2.1 lands alone, a successful suggest only **clears the mask** and should **retain the suggestion in client state** for 11.2.2 — the user sees no dialog yet. Prefer **shipping 11.2.2 in the same release window** so the success path is not empty in production for long. If that is not possible, treat the empty success as **interim behavior** and remove it as soon as 11.2.2 merges (see planning rules on interim behavior).

---

### Sub-phase 11.2.2 — Frontend: preview dialog shell (exact title)

**Status:** planned

**Behavior:** When suggest **succeeds**, a **modal** opens. The dialog **heading** is exactly **`Reorganize layout (preview)`**. Body can be a **placeholder** (empty state or single line) — block list and depth diff are sub-phase **11.4**.

**Tests:** Vitest that a successful suggest opens the modal and exposes the title text; or minimal component test.

**What changes:**

- **Modal/dialog** component or inline DaisyUI modal wired to success path from 11.2.1.
- **Cancel / dismiss** closes the dialog and discards the in-memory suggestion (standard behavior).

---

### Sub-phase 11.2.3 — E2E: mock OpenAI + steps through visible dialog

**Status:** planned

**Behavior:** End-to-end proof: with **`@usingMockedOpenAiService`**, stub `POST /chat/completions`, click the trigger, assert the preview dialog with title **`Reorganize layout (preview)`** is visible.

**E2E steps enabled:**

```gherkin
When I request AI reorganization of the book layout
Then I should see a reorganization preview dialog
# And the preview should show block "2. The Usual Definition Is Not Enough" with suggested depth 1
# When I confirm the AI suggestion
# Then the book block "2. The Usual Definition Is Not Enough" should be at depth 1 in the book layout
```

**What changes:**

- **E2E mock:** Step (or background) stubs `POST /chat/completions` via `mock_services.openAi().chatCompletion().requestMessageMatches(…).stubJsonSchemaResponse(…)` with canned `BookLayoutReorganizationSuggestion` JSON using fixture block IDs.
- **Step definitions:** “When I request AI reorganization…” clicks the sidebar button; “Then I should see a reorganization preview dialog” asserts an element with the exact title **`Reorganize layout (preview)`** (e.g. dialog heading / `role="dialog"` region).

**Why separate from 11.2.1–11.2.2:** Mountebank predicates, fixture IDs, and Gherkin glue are a distinct chunk of work; frontend can be tested in isolation first.

---

### Sub-phase 11.4 — Preview shows block-level depth suggestions

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

**Why a separate sub-phase from 11.2.x:** 11.2.3 proves the vertical slice through a **visible dialog shell**. This sub-phase focuses on rendering **suggestion data** correctly — a distinct postcondition.

---

### Sub-phase 11.5 — Confirm AI suggestion and apply depth changes

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
2. ~~**Error UX (suggest fails):**~~ **Resolved:** Errors propagate; **toast** is sufficient (no extra bespoke UI for 11.2.x).
3. **Large books:** If a book has hundreds of blocks, the prompt may be long. For now, send all blocks. If token limits become an issue, consider pagination or summarization in a later phase.
4. **Cancel:** Trivial — close the dialog (11.2.2+). No separate sub-phase unless the team wants an explicit E2E assertion for cancel.
