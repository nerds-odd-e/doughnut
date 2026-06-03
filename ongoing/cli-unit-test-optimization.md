# CLI unit test optimization

Status: in-progress

## Profiling baseline (2026-06-03)

Command: `cd cli && CURSOR_DEV=true nix develop -c pnpm exec vitest run --reporter=json`

- **280 Vitest tests** (41 files), full suite wall ~10s.
- **No `sleep` / fixed `setTimeout` waits** in `cli/tests/` today.

### Top 10% slowest (28 tests, cutoff ≥524ms)

Grouped by **≤10 tests per phase** (3 groups — smaller than 10 per-file groups).

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits (`sleep`, arbitrary `setTimeout`).
3. Flaky = failure; tests must be deterministic.
4. Follow `cli.mdc` Ink testing guidance (`inkTestHelpers`, observable waits).

**Verify each phase:** `CURSOR_DEV=true nix develop -c pnpm cli:test` (or focused `cd cli && … pnpm exec vitest run <files>`).

---

### Phase 1: Slowest recall + notebook batch (10 tests)
Status: done

**Result (2026-06-03):** Shared `recallSingleAlphaToLoadMore` and `skipInitialWait` on remember-card invalid-input helper; session test runs invalid-input checks once; load-more tests deduplicated setup; faster observable waits (no `frames.join`, stripped-frame assertions, `waitForLastFrameToInclude` for errors/attach). Target tests ~244–419ms each (was ~781–1333ms in baseline profile).

**Scope files:**
- `cli/tests/recallJustReviewInteractive.session.test.tsx` (1 test in batch)
- `cli/tests/recallSpellingInteractive.test.tsx` (1)
- `cli/tests/InteractiveCliApp.addGmail.test.tsx` (1)
- `cli/tests/InteractiveCliApp.useNotebook.test.tsx` (2)
- `cli/tests/recallJustReviewInteractive.loadMore.test.tsx` (3)
- `cli/tests/recallJustReviewInteractive.loadMoreShuffle.test.tsx` (1)
- `cli/tests/recallMcqInteractive.test.tsx` (1)

**Slow tests (ms):**
- 1333 — session: empty Enter and non-y/n on remember card; two-item session completes
- 929 — spelling: after first answer, loading until second question
- 916 — addGmail: missing-credentials error after /add gmail
- 827 — useNotebook: nested plain line + /exit, up-arrow recalls /exit
- 819 — loadMore: y shows Loading more… in flight
- 818 — loadMore: Escape on load more → Recalled 1 note
- 810 — loadMore: empty Enter default yes
- 806 — loadMoreShuffle: shuffled first card
- 791 — mcq: loading next after first answer
- 781 — useNotebook: attach PDF structure excerpt

**Goals:** Deduplicate mock/setup across recall interactive tests; collapse overlapping load-more scenarios; avoid full-app paths where a narrower `runInteractive` suffices; remove redundant assertions.

---

### Phase 2: Recall escape + busy states (10 tests)
Status: done

**Scope files:**
- `cli/tests/recallJustReviewInteractive.session.test.tsx` (2)
- `cli/tests/recallJustReviewInteractive.loadMore.test.tsx` (1)
- `cli/tests/recallSpellingInteractive.test.tsx` (2)
- `cli/tests/recallMcqInteractive.test.tsx` (2)
- `cli/tests/recallJustReviewInteractive.escape.test.tsx` (3)

**Slow tests (ms):**
- 774 — session: busy label while markAsRecalled pending
- 760 — loadMore: empty extended window after two recalls
- 757 — session: loading next after first y
- 740 — spelling: busy while answerSpelling pending
- 711 — mcq: busy while answerQuiz pending
- 706 — escape: empty Enter on leave confirm
- 704 — spelling: wrong answer flow
- 687 — mcq: wrong choice + API index
- 672 — escape: Esc + y stops without markAsRecalled
- 649 — escape: Esc + n returns to remember card

**Goals:** Share helpers for “busy label” and “loading next” patterns; table-drive escape confirm variants if shorter; remove duplicate API mock trees.

**Result (2026-06-03):** Added `recallInteractiveShared.ts` (`deferred`, `waitBusyRecordReview`, `waitBusySubmitAnswer`, `waitLoadingNextQuestion`, `waitLoadingSpellingNext`); just-review mocks for deferred mark, delayed second card, and two-due-then-empty extended recalling; `reachLeaveRecallOnRemember` + `test.each` for three escape confirm cases; dropped redundant raw-frame red ANSI poll on wrong spelling and duplicate Incorrect assertion on wrong MCQ. Phase 2 target tests ~133–309ms each in local profile (was ~649–774ms baseline).

---

### Phase 3: Notebook shell + remaining slow tests (8 tests)
Status: planned

**Scope files:**
- `cli/tests/InteractiveCliApp.useNotebook.test.tsx` (2)
- `cli/tests/recallSpellingInteractive.test.tsx` (1)
- `cli/tests/InteractiveCliApp.test.tsx` (1)
- `cli/tests/useNotebookSlashCommand.test.tsx` (1)
- `cli/tests/recallMcqInteractive.test.tsx` (1)
- `cli/tests/recallJustReviewInteractive.escape.test.tsx` (1)
- `cli/tests/InteractiveCliApp.addGmail.test.tsx` (1)

**Slow tests (ms):**
- 608 — useNotebook: attach spinner ignores input
- 575 — useNotebook: /use opens stage; nested slash guidance
- 566 — spelling: NBSP trim + case in API body
- 557 — InteractiveCliApp: /exit one chunk
- 552 — useNotebookSlashCommand: stage /exit
- 537 — mcq: out-of-range then valid answer
- 533 — escape: Esc during initial load Cancelled
- 525 — addGmail: Esc during OAuth Cancelled

**Goals:** Merge overlapping /exit and notebook-stage tests; simplify attach-spinner test if over-driving UI; keep behavior coverage.

---

### Phase 4: Re-profile and close
Status: planned

Re-run JSON reporter; document before/after counts and top 10 slowest.
