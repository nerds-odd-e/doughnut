# CLI slow unit test optimization

Profiled with `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run --reporter=json'` (2026-06-03). 291 vitest cases.

## Rules

- Remove or simplify redundant tests first.
- Strictly no fixed-time waits (`sleep`).
- Flaky tests are failures — fix or remove.

## Top 10 slowest (ms)

| # | ms | file | test |
|---|-----|------|------|
| 1 | 908 | InteractiveCliApp.addGmail.test.tsx | shows missing-credentials error in transcript after /add gmail |
| 2 | 827 | recallJustReviewInteractive.test.tsx | shows busy label in bordered input while markAsRecalled is pending |
| 3 | 789 | recallSpellingInteractive.test.tsx | wrong spelling shows Incorrect., records answer with correct false |
| 4 | 771 | InteractiveCliApp.useNotebook.test.tsx | after nested plain line and /exit, root up-arrow recalls /exit not stale root prefix |
| 5 | 677 | recallSpellingInteractive.test.tsx | shows busy label in bordered input while answerSpelling is pending |
| 6 | 656 | recallJustReviewInteractive.test.tsx | after y on first just-review, shows loading next until second tracker loads |
| 7 | 652 | recallMcqInteractive.test.tsx | wrong MCQ choice shows Incorrect and sends 0-based choiceIndex to API |
| 8 | 626 | InteractiveCliApp.useNotebook.test.tsx | full app: /use opens notebook stage; / shows nested slash guidance |
| 9 | 595 | recallJustReviewInteractive.session.test.tsx | empty Enter and non-y/n on remember card do not recall; two-item session completes |
| 10 | 591 | useNotebookSlashCommand.test.tsx | active stage /exit clears notebook shell and records assistant line |

Grouping: per test file where multiple top-10 hits; singleton files batched (3 cases &lt; 10).

### Group 1: recall just-review interactive tests
Status: done

Top-10 hits: #2 (827ms), #6 (656ms), #9 (595ms). Optimized `cli/tests/recallJustReviewInteractive.*` — lastFrame-based waits, removed redundant cases, 17 tests in session/loadMore/escape files + shared suite.

Verify: `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/recallJustReviewInteractive'`

### Group 2: recallSpellingInteractive.test.tsx
Status: done

Top-10 hits: #3 (789ms), #5 (677ms). `lastFrame` waits via `recallSpellingInteractive.waits.ts`; dropped redundant Esc-only and duplicate mixed-case tests (9→7).

Verify: `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/recallSpellingInteractive.test.tsx'`

### Group 3: InteractiveCliApp.useNotebook.test.tsx
Status: done

Top-10 hits: #4 (771ms), #8 (626ms). `lastFrame` waits via `useNotebookInteractive.waits.ts`; merged redundant attach/spinner/EPUB tests (17→14).

Verify: `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/InteractiveCliApp.useNotebook.test.tsx'`

### Group 4: singleton slow tests batch
Status: done

Top-10 hits: #1 addGmail (908ms), #7 recallMcq (652ms), #10 useNotebookSlashCommand (591ms). `lastFrame` waits; `recallMcqInteractive.waits.ts` and `useNotebookSlashCommand.waits.ts`; dropped redundant MCQ Esc-only and duplicate Esc+n buffer tests (14→11 MCQ); addGmail error paths use single raw last-frame wait instead of full `frames` scrollback.

Verify each file with `pnpm exec vitest run tests/<file>` from `cli/`.

**Plan complete**
