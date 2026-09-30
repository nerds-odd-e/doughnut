# New block title prompt

Work item: **SEED-059#story-8**
([story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-8)).
Status: **story refined; three Behavior slices planned.**

## Goal and scope

A reader who creates a block from a paragraph is always asked for a title. The
default is the paragraph's first sentence, cut at a word boundary to at most 80
characters, already selected; Enter confirms and Escape cancels. The flow and
its tests get smaller: the length threshold, the "truncated" overlay marker and
the long-paragraph E2E scenario go away.

Excluded (see the story): renaming an existing block, making *New block* easier
to find, AI titles, EPUB block creation (story 10), any backend or generated
API change.

## Known architecture

- **Where the decision is made.** `BookReadingPdf.vue` `onCreateBlockFromContent`
  opens `NewBookBlockTitleDialog` only when `derivedTitle.length >= 512`
  (`STRUCTURAL_TITLE_MAX_CHARS`), otherwise it creates the block with no title
  and the server copies the paragraph text. Always opening the dialog removes
  that branch. `derivedTitle` is the server-provided paragraph text
  (`BookBlockPdfContentLocators.derivedTitleFromRaw`, up to 512); when a
  paragraph has no text it is `undefined` and the dialog gets an empty default.
- **The threshold has one other user.** `bookBlockSelectionBboxHighlight.ts`
  marks overlays `data-derived-title-truncated`; only the long-paragraph E2E
  page-object method reads it, plus one unit spec. Both go with the scenario.
- **The default rule is a small pure function** in `frontend/src/lib/book-reading/`
  (first sentence = up to the first `.`, `?` or `!` followed by whitespace or
  the end; longer than 80 → cut at the last space at or before 80, or at 80
  when there is no space). The frontend computes it; the server keeps its
  optional title with its 512 limit.
- **The dialog** (`NewBookBlockTitleDialog.vue`) has a plain `<input>` inside a
  `<dialog>`: no focus, no selection, no Enter handling. Escape already closes
  the native dialog and emits `cancel` through its `close` listener.

## Premises observed

- Only the long-paragraph flow reads `data-derived-title-truncated`:
  `grep -rn "derived-title-truncated\|derivedTitleTruncated" e2e_test frontend`
  finds the page object, the highlight lib and its spec, nothing else.
- Only one step calls `createBookBlockFromLongTextContentBlockOnPdf`
  (`I create a book block from a long-text content block on the PDF`), used by
  one scenario in `reorganize_layout.feature`.
- No frontend spec covers `NewBookBlockTitleDialog` or `onCreateBlockFromContent`
  today (`grep -rn "NewBookBlockTitleDialog\|new-block-title" frontend/tests`
  is empty); the E2E scenarios are the only proof of the flow.
- The server accepts `structuralTitle` up to `BookBlockTitleLimits.STRUCTURAL_MAX_CHARS`
  and trims it (`BookOutlineEditor.splitAtContent`), so an 80-character title
  needs no backend change.

## Outside-in proof

- Create a block from a paragraph (E2E, `reorganize_layout.feature`): the prompt
  always appears with a non-empty default; confirming it creates the block as
  a child of the selected block.
- The default for a 289-character paragraph is at most 80 characters and ends at
  a word; a short sentence stays whole (unit spec of the pure function).
- Opening the prompt selects the default; typing replaces it and Enter confirms;
  Escape cancels without creating (dialog component spec).

## Ordered slices

### 1. Creating a block from any paragraph asks for a title
Type: Behavior
Status: planned
Proof: E2E `Create a new book block from a content block bbox` gains "Then I
should be prompted to enter a title defaulting to the paragraph" (existing
step, renamed) and "When I confirm the title" before the layout assertion. The
long-paragraph scenario, its step, `createBookBlockFromLongTextContentBlockOnPdf`,
the `data-derived-title-truncated` marker with the overlay helper's
`derivedTitle` option and its two highlight specs are deleted; `STRUCTURAL_TITLE_MAX_CHARS` leaves the frontend.

Behavior: the reader clicks any paragraph and *New block* → the title prompt
opens with the paragraph text as default → Confirm creates the block. No length
threshold remains. About 8 min, mostly deletion. Stated reason it is one slice:
the branch removal and the removals it enables are proven by the one merged
scenario.

### 2. The prompt's default is a short first sentence
Type: Behavior
Status: planned
Proof: unit spec of the pure function with a table: short sentence unchanged;
two sentences → the first; a 200-character first sentence → at most 80
characters ending at a word; one 100-character word → 80 characters; no
sentence end and 300 characters → cut at a word; text with a trailing
sentence end and no space → whole sentence. E2E from slice 1 still passes (its
default is non-empty).

Behavior: the default the prompt shows is the first sentence, cut at a word
boundary to at most 80 characters. About 5 min.

### 3. The prompt is keyboard-ready
Type: Behavior
Status: planned
Proof: component spec of `NewBookBlockTitleDialog` (first spec for it): on open
the input has focus with its whole default selected; typing then Enter emits
`confirm` with the typed text; Escape emits `cancel` and no confirm. No new E2E.

Behavior: the reader opens the prompt, types over the selected default and
presses Enter to create the block; Escape abandons it. About 5 min.

## Current decisions

- 80 characters (owner, 2026-09-30).
- Always prompt, with no length or empty-text exceptions; an empty default is
  allowed for a paragraph with no text.
- Unit and component specs prove the rules; one E2E scenario proves the flow.

## Learnings

None yet.
