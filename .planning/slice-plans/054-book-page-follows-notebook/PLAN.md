# Open another notebook's book without seeing the previous one

Work item: **SEED-059#story-13**.
Source: [refined story](../../seeds/SEED-059-book-reading-uat-fixes.md#story-13).

## Goal and scope

When the book page moves straight from one notebook's book to another's, the
reader sees the new book's layout, file, and saved position, and reading
records are saved against the book shown; the previous book's records are
unchanged. PDF and EPUB.

Excluded (story): any new way to go from one book to another; merging the
per-Rule Backgrounds in `book_browsing.feature`.

Must keep working: opening a book from the notebook list, notebook page, or
Settings; resume at the last place; story 1 and story 5 promises.

## Architecture

- **PFE:** the page's per-book state (book, file bytes, saved position) lives
  in `useBookReadingBootstrap(notebookId)`, which reads `notebookId` once in
  `onMounted`. Everything below it (`BookReadingContent`,
  `BookReadingEpubView`, `useBookReadingCurrentBlock`) takes its notebook and
  initial state once as well. Vue Router reuses the `bookReading` page instance
  between `/notebooks/A/book` and `/notebooks/B/book`, passing only a new
  `notebookId` prop.
- **Decision:** give each notebook's book its own page instance instead of
  teaching every part to reload. `BookReadingPage` renders its current content
  as one child keyed by `notebookId` (move the existing template and the
  `useBookReadingBootstrap` call into it); a new `notebookId` unmounts the old
  reader and mounts a fresh one. No reload logic, no stale-response guard, and
  the existing unmount behavior applies unchanged (EPUB flushes a pending
  position PATCH to its own notebook; PDF cancels). A `watch`-based reload in
  the bootstrap was considered and rejected: it adds reset and race handling
  for the same outcome.
- No ADR or North Star topic is affected.

## Decisive premises (observed 2026-09-29)

- **The page is reused across book-to-book navigation.** E2E observation during
  SEED-059#story-3 (URL `/notebooks/2/book` still showing notebook 1 and sending
  `PATCH /api/notebooks/1/book/reading-position`); `frontend/src/DonutApp.vue`
  and `layouts/NotebookSidebarLayout.vue` render `<component :is="Component">`
  with no key. Confirmed.
- **Bootstrap reads the notebook once.** `frontend/src/composables/useBookReadingBootstrap.ts`
  loads inside `onMounted` from the `notebookId` argument;
  `BookReadingPage.vue` passes `props.notebookId` by value. Confirmed.
- **Writes use the notebook the reader was created with.**
  `useBookReadingCurrentBlock.ts` patches `path: { notebook: toValue(options.notebookId) }`
  and flushes (EPUB) or cancels (PDF) on `onBeforeUnmount`. So remounting sends
  the old reader's last write to the old notebook. Confirmed by reading.
- **Page tests mount with a `notebookId` prop** (`mountBookReadingPage(id)` in
  `frontend/tests/pages/bookReadingPageTestSupport.ts`, via
  `helper.component(...).withRouter(...).withProps(...)`), so changing that prop
  on the mounted page reproduces what the router does. Confirmed by reading.
- **No existing test covers a notebook change on a mounted page.**
  `grep -rn "setProps" frontend/tests/pages/BookReadingPage*` → none. Confirmed.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| B's layout and file show after moving from A's book | 1 | Page test: mount notebook A's PDF book, change `notebookId` to B → B's layout block titles show, B's file is fetched |
| Position saved to B, A unchanged | 1 | Same test: after the change, reading-position PATCH goes to B's notebook; no PATCH to A after the switch (beyond A's own unmount behavior) |
| EPUB → PDF switch | 1 | Page test: mount A's EPUB, change to B's PDF → B's PDF layout shows |
| Opening, resume, stories 1/5 keep working | 1 | Existing `frontend/tests/pages/BookReadingPage*.spec.ts` and `useBookReadingBootstrap.spec.ts` stay green; `e2e_test/features/book_reading/book_browsing.feature` stays green |

The page test is the stable boundary: the router supplies exactly a new
`notebookId` prop to the reused page. No new E2E scenario: no in-app link
reaches book-to-book today, and the E2E observation that found it is the same
prop change.

## Slices

### 1. Book page shows and records the notebook it is opened for
Type: Behavior
Status: done
Proof: new cases in `frontend/tests/pages/BookReadingPage.spec.ts` (or the
reading-position spec if its helpers fit better) fail first, then pass;
`pnpm frontend:test` for the book-reading page and bootstrap specs; run
`book_browsing.feature` once.

Behavior: the book page is showing notebook A's book → the page's notebook
becomes B → B's layout, file, and saved position load, and reading position
updates go to B; A's reader is gone.

Accepted proof: `BookReadingPage` wraps a `BookReader` (the former page body,
now `frontend/src/components/book-reading/BookReader.vue`) keyed by
`notebookId`. `frontend/tests/pages/BookReadingPage.notebookSwitch.spec.ts`
("shows the other notebook's book and saves the reading position to it",
"shows the other notebook's PDF book after an EPUB book") failed first, then
passed; `pnpm frontend:test tests/pages/BookReadingPage` 45/45,
`useBookReadingBootstrap` 3/3, `vue-tsc --noEmit` clean,
`book_browsing.feature` 6/6.

Learning: Vue Router passes the new prop to the reused page, so a key inside
the page is enough; `DonutApp`, layouts and router are unchanged.
