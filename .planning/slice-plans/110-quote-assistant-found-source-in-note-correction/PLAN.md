# Quote a source the assistant found in a note correction

**Identity:** SEED-073#assistant-quoted-source-in-corrections
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#assistant-quoted-source-in-corrections)

## Goal and scope

A note author deciding on a content correction sees one passage quoted
from an outside source for the corrected claim, with a link that opens
the source at that passage. The assistant searches the web for it when it
proposes the correction. Without a source the correction is still offered
and the review says that no source was found.

Excluded, as the story says: title corrections; a server check that the
quote is on the page; more than one source; a note on how strong the
source is; handling the note's own `url` apart; keeping the assistant
from searching when it proposes nothing; storing the source in the note.

## Existing solutions and design

- The note conversation sends one streamed request built in
  `backend/src/main/java/com/odde/donut/services/ai/ConversationAiRequestBuilder.java`
  (163 lines). The library (openai-java 4.106.0) has
  `ResponseCreateParams.Builder.addTool(WebSearchTool)` and
  `maxToolCalls`. Add the web search tool there, limit a reply to 5 tool
  calls (the cost bound used in the try), and add one instruction to the
  existing developer messages. The reply stream mapper
  (`ResponseStreamToLegacyChatChunkMapper`) passes over search events; it
  needs no change.
- Use the instruction wording the
  [paid try](../../seeds/source-backed-note-edits-evidence.md#assistant-search-try-2026-11-18)
  observed, with "one passage" added because one try joined two:
  "Before you propose a correction to the note content, search the web
  for a source for the corrected claim. If the note has a url property,
  open that page first. In the tool call give sourceQuote, one passage
  copied word for word from the source page in the page's own language,
  and sourceUrl, the address of that page. When you find no source that
  supports the correction, leave both out."
- The content correction tool is the class
  `backend/src/main/java/com/odde/donut/services/ai/NoteContentCompletion.java`.
  It gains two optional parts, `sourceQuote` and `sourceUrl`. The class is
  in the API schema, so the generated client changes with it.
- A stored correction is `ConversationNoteCorrection` (table
  `conversation_note_correction`), written from the tool call in
  `NoteConversationAiReplyService` (84 lines) and returned as it is by
  `getConversationCorrections`. It gains two nullable columns for the
  quote and the address (`db-migration` skill). A source is a quote
  together with an address that starts with `http://` or `https://`;
  anything else is stored as no source. The reason: the address becomes a
  link the author clicks, and an address of another kind, such as
  `javascript:`, could run code in the author's session.
- Acceptance and the decision flow are untouched, so
  [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  holds as today. No Accepted decision is in conflict, and no North Star
  topic governs this.
- The review is `frontend/src/components/conversations/ConversationNoteCorrectionReview.vue`
  (201 lines; the file-size check is 250).
  [Showing the smallest change](../109-show-smallest-change-in-note-correction/PLAN.md)
  is being executed and edits the same file. So the source display is its
  own small component, placed right after the reason, and the review gains
  only the line that uses it.
- The link is the address followed by `#:~:text=` and the quote, encoded
  with `encodeURIComponent` and with `-` written as `%2D`. When the
  address already has a `#` part, `:~:text=` and the quote follow it. One
  rule for every page; a PDF ignores it and opens at its start. The link
  opens in a new tab with `rel="noopener noreferrer"`.
- No existing product code builds such a link or shows a quoted outside
  source; nothing to reuse beyond the above.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| `gpt-6.2-sol`, the production conversation model, accepts web search together with the correction function tools in one streamed request and returns a quote and an address in the tool call | slices 1 and 2 | 2026-11-18, 12 paid streamed calls to `https://api.openai.com/v1/responses` with `{"type":"web_search"}`, `max_tool_calls: 5` and the two added tool parts; model read from production `GET /api/settings/current-model-version` → all 12 completed; 10 of 10 corrections carried both | settled. Proxy: the calls were raw HTTP, not the Java library; the library builds the same request fields, and the row below covers the library build |
| The request builder accepts the web search tool and the tool-call limit beside the class-based tools, and the existing conversation tests and E2E still pass | slice 2 | 2026-11-18 at 41d1ee7e1f in this worktree, scratch edit adding `WebSearchTool` and `maxToolCalls(5)` to the builder, since reverted: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --build-cache --tests '*ConversationAiRequestBuilderTest' --tests '*ConversationAiReplyControllerTest' --tests '*OpenAiTransactionalRatchetTest'` → 15 tests, 0 failures; `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_content/note_content_completion.feature` → 2 passing | settled |
| A clicked link ending in `#:~:text=` and the encoded quote opens the page at the quote, also for Japanese | slice 3, example 2 | 2026-11-18, Chrome on the owner's Mac, links built with the rule above and clicked: Solid-OIDC specification scrolled to the sentence (scroll position 2532 of 18013); kotobank 「〘 他動詞 ザ上一 〙」 in view; 教育出版, a 100-character Japanese sentence, in view | settled for Chrome. A navigation started by a script does not scroll, so an automated browser test cannot observe the scroll; the tests observe the link's address |
| A correction is read by the web review only | slices 1 and 3 | 2026-11-18 at 41d1ee7e1f: search for `ConversationNoteCorrection` and `getConversationCorrections` over `cli/src`, `mcp-server/src`, `frontend`, `packages` → only the generated client, `packages/donut-test-fixtures` and the conversation components and their tests | settled |

## Outside-in proof ownership

Backend proof enters through the controller, as
`ConversationAiReplyControllerTest` does with `OpenAiResponseStreamMocker`,
and through `ConversationAiRequestBuilderTest` for the request. Component
proof enters through `ConversationInner` with
`frontend/tests/components/conversation/conversationCorrectionTestSupport.ts`.
Each new test must fail before its slice's change. Frontend proof also
needs both typechecks from the `frontend` skill.

| Story promise or example | Slice | Observation |
| --- | --- | --- |
| The correction carries one passage and the address of its page | 1 | controller test: a content tool call with `sourceQuote` and `sourceUrl` → `getConversationCorrections` returns both |
| No source → the correction is still offered | 1 | controller test: a content tool call without them → the correction is returned with neither; the existing correction tests stay green |
| Only a web address becomes a link | 1 | controller test: `sourceUrl` of `javascript:alert(1)` → the correction is returned with no source |
| The assistant searches before it proposes a content correction | 2 | request builder test: the request for a note conversation, and for a recall conversation, holds the web search tool, a limit of 5 tool calls, and the source instruction; the content tool's schema holds `sourceQuote` and `sourceUrl` |
| 1 and 3. The review shows the quote and a link, apart from the reason | 3 | component test: for a pending content correction with the Solid OIDC source the review shows the quote text and a link; the same with the Japanese quote 「〘 他動詞 ザ上一 〙」 |
| 2. The link opens the source at the quoted sentence | 3 | component test: the link's address is the source address followed by `#:~:text=` and the encoded quote, also when the address already has a `#` part; the browser's scroll is the premise observed above |
| 4. No source → the review says so | 3 | component test: a pending content correction without a source shows "No source was found." and no link |
| 5. A PDF source shows quote and link | 3 | component test: an address ending in `.pdf` gets the same quote and link |
| 6. An accepted correction still shows its source | 3 | component test: an accepted content correction with a source shows the quote and link; a declined one too |
| A title correction shows no source part | 3 | component test: a title correction shows neither a quote nor "No source was found." |
| The source reaches the review in the running product | 3 | E2E, the existing scenario in `e2e_test/features/ai_generated_content/note_content_completion.feature`: the mocked reply gains `sourceQuote` and `sourceUrl`; the pending correction shows the quote and a link to the address |

The reader's guide `docs/recall-help.md#reviewed-note-corrections` gains
the source in slice 3.

## Ordered slices

### 1. A content correction keeps the quote and address the assistant gives
Type: Behavior
Status: planned
Proof: the three controller tests above; the existing tests in
`ConversationAiReplyControllerTest` stay green; the migration runs on the
test database; the generated API client is regenerated
(`generate-api-client` skill) and both frontend typechecks pass.

Behavior: the assistant's reply calls the content correction tool with a
quote and a web address → the stored correction returns both from
`getConversationCorrections`. Without them, or with an address that is
not `http://` or `https://`, the correction is stored with no source. A
title correction stores none.

Interim behavior: the review does not show the source yet. Slice 3
replaces this.

### 2. The assistant searches the web before it proposes a content correction
Type: Behavior
Status: planned
Proof: the request builder test above; `ConversationAiRequestBuilderTest`,
`ConversationAiReplyControllerTest` and `OpenAiTransactionalRatchetTest`
stay green; the E2E feature
`e2e_test/features/ai_generated_content/note_content_completion.feature`
stays green.

Behavior: a conversation about a note, or about an answered recall
question → the request sent to OpenAI holds the web search tool, a limit
of 5 tool calls, and the instruction to search first and to give one
passage word for word with its address, or neither.

### 3. The review shows the quoted source, or says none was found
Type: Behavior
Status: planned
Proof: the component tests and the E2E step above; both typechecks; the
existing correction specs in `frontend/tests/components/conversation/`
stay green; `docs/recall-help.md` describes the source.

Behavior: a content correction with a source, pending or decided, for an
author or a reader → right after the reason the review shows the quote
and a link that opens the source in a new tab at the quoted passage. A
content correction without a source shows "No source was found." in the
same place. A title correction shows neither. `ConversationNoteCorrectionBuilder`
in `packages/donut-test-fixtures` gains the source for the tests.

## Current decisions

- Owner, 2026-11-18: the quote and link are their own parts of the
  correction; the link opens the page at the quote; a correction without
  a source is offered and says so; no server check of the quote.
- One passage and one address for a correction.
- A source needs an `http://` or `https://` address; otherwise the
  correction has no source.
- A content correction stored before this story has no source, so it also
  shows "No source was found.". No search was made for it. The owner's
  production notebooks hold few such corrections; they are left as they
  are.
- This plan and [plan 109](../109-show-smallest-change-in-note-correction/PLAN.md)
  both edit the review and its guide section. Order on the page: reason,
  source, change, editor.
- After release, the first real content correction in production is the
  check that the source appears; it needs no slice.

## Concerns

Slice-plan refinement was not needed: three slices, each one outcome with
its own proof, and each premise is observed. No slice-specific concern
remains.
