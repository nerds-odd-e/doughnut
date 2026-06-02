# Frontend slow unit test optimization

Profiled with `CI=true npx vitest run --browser=chromium --reporter=json` (2026-06-03).

## Rules

- Remove or simplify redundant tests first.
- Strictly no fixed-time waits (`sleep`).
- Flaky tests are failures — fix or remove.

## Top 10 slowest (ms)

| # | ms | file | test |
|---|-----|------|------|
| 1 | 2956 | FullScreen.spec.ts | enters fullscreen mode when button is clicked |
| 2 | 2219 | ManageAccessTokensPage.spec.ts | displays "No Label" when token label is empty |
| 3 | 2206 | NoteToolbar.moreOptions.spec.ts | copies export markdown while keeping export dialog open |
| 4 | 1789 | FullScreen.spec.ts | exits fullscreen mode when exit button is clicked |
| 5 | 1738 | NoteExportForm.spec.ts | fetches AI markdown on open and downloads from primary button |
| 6 | 979 | NoteExportForm.spec.ts | fetches graph JSON when expanded and downloads on button click |
| 7 | 958 | PopButton.spec.ts | blurs button when dialog closes via close_request |
| 8 | 928 | NoteExportForm.spec.ts | does not refetch graph JSON if already loaded when toggling open/close |
| 9 | 752 | BookReadingPage.spec.ts | shows error when PDF bytes are not valid |

Grouping: per test file where multiple top-10 hits; singleton files batched (4 cases &lt; 10).

### Group 1: FullScreen.spec.ts
Status: done

Optimized `frontend/tests/common/FullScreen.spec.ts`: dropped vitest/browser `page` polling, use wrapper/DOM clicks; merged slot-content test into enter-fullscreen test (3 tests total).

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/common/FullScreen.spec.ts`

### Group 2: NoteExportForm.spec.ts
Status: done

Optimized `frontend/tests/notes/NoteExportForm.spec.ts`: dropped `vitest/browser` `page` queries for wrapper/DOM; merged graph download + cache-toggle into one test (4 tests total).

Verify: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteExportForm.spec.ts`

### Group 3: singleton slow tests batch
Status: done

Optimized singleton slow tests: dropped `getByRole` / `vitest/browser` `page` where possible; DOM `querySelector` / wrapper clicks; PopButton close via `getByText` + close button; BookReadingPage invalid PDF via `loadError` emit after viewer mount (pdf.js parse moved out of page test).

Verify each file:
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/ManageAccessTokensPage.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteToolbar.moreOptions.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/commons/Popups/PopButton.spec.ts`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/BookReadingPage.spec.ts`

**Plan complete** — all three groups done.
