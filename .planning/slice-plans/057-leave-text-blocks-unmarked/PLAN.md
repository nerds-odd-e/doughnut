# Leaving a block with text unread does not mark it

Work item: **SEED-059#story-15**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-15)
(owner decisions 2026-09-29). This plan is the first part of the original
story 15 plan, resplit on the owner's instruction; the other parts moved to
[plan 058](../058-current-block-same-in-pdf-and-epub/PLAN.md) (story 16) and
[plan 059](../059-reopen-epub-at-exact-paragraph/PLAN.md) (story 17).

## Goal and scope

A reader who moves past an EPUB block with text of its own, without reading
it, gets no record for it; blocks with no text of their own are still marked
read when the reader moves on.

Excluded: the reading panel's anchor for a one-paragraph block
(`lastDirectContentLocator`), the current-block rule and marking after an empty
block (story 16), records already made (story 6).

## Architecture

- **PFE:** `useAutoMarkNoDirectContentPredecessor.ts:21-35` is the only
  auto-mark rule, used by both readers through `useBookReadingSelection.ts:74`.
  It tests `contentLocators.length === 1`. Backend
  `BookBlockEpubContentLocators` / `BookBlockPdfContentLocators` always make the
  first content block locator 0 and count only later blocks as direct content,
  so a block's first payload is never counted. PDF is right by accident (its
  first content block is a heading or a `beginning_anchor`); EPUB's first
  payload is often a paragraph (`EpubSpineContent.java:86-126`: only `p`, `img`,
  `table` are payloads, and a start-only entry gets `beginning_anchor`).
- The frontend already receives `BookBlockFull.contentBlocks[].type`
  (`types.gen.ts:589-604`); nothing reads it today. Add one frontend predicate,
  "has text of its own", next to `bookBlockDirectContent.ts`: false when the
  block has one locator and its only content block is a `beginning_anchor`, or
  is a PDF block with one locator (its heading). Auto-mark uses it. No API or
  backend change.
- Keep `useAutoMarkNoDirectContentPredecessor`'s other conditions (predecessor
  row, no recorded disposition) unchanged.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| EPUB auto-marks a heading plus one paragraph | `epub_valid_minimal.epub` `OEBPS/chapter1.xhtml` is `<h1>Part One</h1><p>Opening paragraph for part one.</p>`; `epub_book.feature` "Entering the next EPUB block auto-marks a structural-only predecessor as read" asserts "Part One" is marked | Confirmed; that scenario pins the defect |
| A start-only EPUB block exists in a fixture | `regenerate_epub_long_chapter_before_target.sh:129-177`: "The Full Licence" is a heading directly followed by "Licence Section One"; `epub_book.feature` "Choosing a block with no content of its own…" chooses it | Confirmed; no fixture change |
| PDF heading-only auto-mark stays covered | `reading_record.feature:15-17` "Auto-read a heading-only book block when entering its successor" | Confirmed |
| Unit specs model blocks by locators only | `frontend/tests/composables/useBookReadingSelection.spec.ts:21,69,93,117,142`; `BookReadingPage.readingControlPanel.marking.spec.ts:144,178` | Confirmed; their fixtures need `contentBlocks` types |

## Key examples → proof

| Promise | Slice | Proof |
| --- | --- | --- |
| A start-only EPUB block is marked read when its successor is entered | 1 | E2E: choose "The Full Licence", then "Licence Section One" → "The Full Licence" marked read |
| Leaving "Part One" (heading and one paragraph) unread leaves it unmarked | 2 | E2E: the rewritten scenario; `useBookReadingSelection.spec.ts` |
| PDF heading-only blocks are still marked | 2 | `reading_record.feature` |

Proof commands:
`SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec 'e2e_test/features/book_reading/epub_book.feature,e2e_test/features/book_reading/reading_record.feature'`;
`CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/composables/useBookReadingSelection.spec.ts tests/pages/BookReadingPage.readingControlPanel.marking.spec.ts`.

## Slices

### 1. The auto-mark scenario uses a block with no text of its own
Type: Structure (test)
Status: done
Size: about 5 minutes; E2E runtime excepted.
Proof: `epub_book.feature` green with the moved scenario.
Accepted proof: `SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/book_reading/epub_book.feature`
→ 15/15 passing; Rule "Landing on the chosen place", scenario "Entering the
next EPUB block auto-marks a structural-only predecessor as read", last step
asserts "The Full Licence" is marked read.

Move "Entering the next EPUB block auto-marks a structural-only predecessor as
read" to the long-chapter fixture's Rule: choose "The Full Licence", then
"Licence Section One" → "The Full Licence" is marked read. Product unchanged.

### 2. Leaving a block with text unread does not mark it
Type: Behavior
Status: done
Size: about 5–8 minutes; E2E runtime excepted.
Proof: new E2E "Leaving an EPUB block with text unread does not mark it": choose
"Chapter Alpha" after "Part One" is shown → "Part One" has no mark (fails today);
unit specs updated to give blocks `contentBlocks` types, with one case each for
start-only, EPUB one-paragraph, and PDF heading-only; `reading_record.feature`
green.

Add the "has text of its own" predicate and use it in
`useAutoMarkNoDirectContentPredecessor`.

Accepted proof: the two frontend specs 10/10 (red on the old rule for the EPUB
paragraph case); `epub_book.feature` 16/16 and `reading_record.feature` 6/6
(new scenario red on the old rule); frontend pages/composables/lib/book-reading
500/500; `vue-tsc` clean.

## Execution complete

Product advice: no correction needed. Snap-back's `hasDirectContent`
(`useBookReadingSnapBack.ts`, more than one locator) keeps the old blind spot;
story 16 should reuse `hasNoTextOfItsOwn` and carry this story's definition of
"no text of its own" when this story is wrapped up. The reading panel's anchor
(`lastDirectContentLocator`, excluded here) has no home story; wrap-up should
place it in a story or confirm it stays out of scope.

## Current decisions

- One frontend predicate from `contentBlocks` types; no API change.
- The reading panel's anchor keeps using `lastDirectContentLocator` (excluded).
- The predicate is `hasNoTextOfItsOwn` (seed wording; a block with no locators
  is not auto-marked). The composable is renamed
  `useAutoMarkPredecessorWithNoTextOfItsOwn`.

## Learnings

- Resplit from the original story 15 plan (committed at `8e5d358124`): parts
  B and C went to plan 058, part D to plan 059.
