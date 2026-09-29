# Give AI Reorganize room to answer for a full-size book

Work item: **SEED-059#story-4**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-4)
(owner decisions 2026-09-29).

## Goal and scope

A reader whose PDF book has no bookmarks gets an *AI Reorganize* preview they
can confirm, however many blocks the book has: the reorganization request
allows an answer size that grows with the book's block count.

Excluded (owner decisions): a new failure message, keeping the current block
in view after a failure, splitting the book or asking only for changes, a size
cap or progress display, AI nesting quality, the preview's layout, EPUB, and
duplicate or label blocks. Waiting up to about a minute for a very large book
is accepted.

## Architecture

- **PFE:** `OpenAIResponseRequestBuilder.maxOutputTokens(long)` is the one way
  a request sets its answer size; other AI features already call it
  (`AiNoteAutomationService`, `NoteQuestionGenerationService`,
  `QuestionGenerationRequestBuilder`). `BookLayoutReorganizer.suggest` builds
  its request with that builder and never calls it. Set it there from the
  block count; add no new builder option, shared helper, or retry.
- No ADR or North Star topic is affected.

## Decisive premises (observed 2026-09-29)

- **The request inherits the 700-token default.** Read
  `OpenAIResponseRequestBuilder.java` (`maxOutputTokens = 700L`, passed to
  `.maxOutputTokens(...)` in `createParams`) and `BookLayoutReorganizer.suggest`
  (no `maxOutputTokens` call). Confirmed.
- **The UAT failure is a cut-off answer.** `~/.gradle/daemon/9.8.0/daemon-58848.out.log`
  line 32784 (2026-09-29 08:19): `OpenAIInvalidDataException: Error parsing
  JSON: {"blocks":[{"depth":0,"id":44}, … {"depth":1,"id":130},{"` — 87
  complete entries in 700 tokens, about 8 tokens per entry. Confirmed.
- **Existing tests already assert a request's answer size** through the
  mocked response service (`AiControllerExtractNotePreviewTest`:
  `params.rawParams().maxOutputTokens()`), and
  `NotebookBooksSuggestLayoutControllerTest` already captures the
  reorganization request's params. Confirmed; the proof extends that test.

## Outside-in proof

| Key example | Signal |
| --- | --- |
| A ~360-block book gets a preview it can confirm | Controller test: attaching a book with 361 blocks and requesting a suggestion sends a request whose `maxOutputTokens` covers at least 12 tokens per block (50% above the observed 8), so the answer is not cut off |
| The 27-block paper previews as today | Existing `NotebookBooksSuggestLayoutControllerTest` cases and `e2e_test/features/book_reading/ai_reorganize_layout.feature` stay green |

The mocked OpenAI cannot show a real model finishing a long answer; the
real-model run at 361 blocks is an optional owner demonstration, not a gate
(paid, and the stub E2E environment has no real model).

## Slices

### 1. AI Reorganize allows an answer as long as the book

Type: Behavior
Status: done
Proof: new case in `NotebookBooksSuggestLayoutControllerTest`
(`SuggestBookLayoutReorganization`), run with
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests 'com.odde.donut.controllers.NotebookBooksSuggestLayoutControllerTest'`;
then the existing cases in the same class stay green.

Behavior: a book with 361 blocks → request AI reorganization → the request
sent to OpenAI allows at least 12 × 361 output tokens. In
`BookLayoutReorganizer.suggest`, set `maxOutputTokens` from the ordered block
count with a per-block allowance and a small fixed base (for the JSON wrapper),
named as constants in that class. A small book keeps a budget no lower than
today's 700.

Accepted proof (2026-09-29): `allowsAnAnswerAsLongAsAFullSizeBook` attaches
361 top-level blocks and asserts the captured request's `maxOutputTokens`
≥ 12 × 361; it failed at 700 before the change. The class's 6 tests pass.
Implemented as `BASE_ANSWER_TOKENS` (700) + `ANSWER_TOKENS_PER_BLOCK` (12)
× block count in `BookLayoutReorganizer`.

## Current decisions

- Budget per block, not a fixed large number: the answer is one entry per
  block, so the need grows linearly and no book size is special.
- No change to error handling; remaining failures keep their current short
  messages (story scope).
- Local proof is the focused backend test only; no frontend, E2E, or API
  change is involved, so no API generation.
