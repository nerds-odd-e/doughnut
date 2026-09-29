# Remove dead public endpoints and propose one file-serving consolidation

**Identity:** SEED-055#story-1
**Source:** [story](../../seeds/SEED-055-public-api-cleanup.md#story-1), refined with the owner on
2026-09-29 (E2E-used endpoints exempt; removal is transitive, followed by a whole-product cleanup;
no negative tests or history notes).

## Goal and scope

The public API advertises only endpoints that serve a frontend, CLI, MCP, or supported external
feature. Five endpoints with no such use are removed together with everything only they kept alive,
and the owner receives one proposal for the remaining overlapping file-serving endpoints.

Removed endpoints:

- `DELETE /api/user/token-info` (`UserController.revokeToken`)
- `GET /api/user/recall-ez-diffusion` and `GET /api/user/daily-probe-convergent-validity`
- `GET /api/memory-trackers/{memoryTracker}/recall-logs` (`MemoryTrackerController.getRecallLogs`)
- `GET /api/notebooks/{notebook}/book/file` (`NotebookBooksController.getBookFile`). The
  2026-09-29 dependency trace found no production caller: the frontend reads Book bytes through
  `GET /api/books/{book}/file`, which duplicates this handler line for line. The story's usage rule
  classifies it as dead, so it joins the four endpoints the refinement listed.

Excluded (from the story): redundancy review beyond the file-serving endpoints, implementing any
consolidation, the code-generation workaround endpoint
(`AiController.dummyEntryToGenerateDataTypesThatAreRequiredInEventStream`), unused DTO fields such as
`DailyProbe.accuracy` (still written and serialized), dead code unrelated to the removed endpoints,
and new API capabilities.

Every removal slice follows the same rule: delete the endpoint, then every production and test piece
whose only dependent was a removed piece, repeating until nothing orphaned remains; narrow visibility
that existed only for a removed caller; regenerate the API artifacts; sweep backend, frontend, CLI,
MCP, docs, and agent guidance for mentions. Nothing asserts the removed endpoint is gone.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| No non-test caller for the five endpoints | Dependency trace searching `frontend/src`, `frontend/tests`, `cli/src`, `mcp-server/src`, `packages` (non-generated), `scripts`, `docs`, `.agents`, `e2e_test` for operation names, URL fragments, and direct fetches; `git grep -n "book/file\|getBookFile(" -- frontend/src cli/src mcp-server/src e2e_test packages ':!packages/generated'` | No hits outside backend tests, generated files, untracked CLI bundles, and SEED-055 |
| E2E does not use any removed endpoint | Same trace over `e2e_test` | E2E uses `GET /token-info` (kept) and deletes tokens via `DELETE /api/user/token/{tokenId}` (kept) |
| Recall-log tests can observe writes through recall history | `git grep -c getRecallLogs -- backend/src/test`; `MemoryTrackerService.getRecallHistory` builds `RecallHistoryItem.from(getRecallLogs(...), ...)`; `MemoryTrackerRecallHistoryControllerTest` already reads `.getRecallLog()` from history items | 20 call sites in 7 controller test files; `MemoryTrackerService.getRecallLogs` stays (used by `getRecallHistory`) |
| Recall diagnostics own a closed code island | Trace of `UserController.java:215-244` | Dead with them: `RecallStatsService.computeConvergentValidity`/`computeEzDiffusion`, `RecallEzDiffusion`, `EzDiffusion`, `RecallProbeConvergentValidity`, `RecallEzDiffusionDTO`, `DailyProbeConvergentValidityDTO`, and 5 test classes. `RecallPaceAggregator.ABSOLUTE_FLOOR_MS`/`HARD_DROP_MS` and `DailyProbeDaySeries.latestByLocalDay` lose their only outside caller. No table orphaned |
| Revoke token owns only one helper | Trace of `UserController.java:127-151` | `persistedUserTokenFromBearerOrThrow` dies; `bearerTokenFromRequestOrThrow`, `findTokenByToken`, `UserService.deleteToken` stay |
| Notebook-addressed Book file owns one service method | `git grep -n "getNotebookBookFile\|notebookBookFileFromBook" -- backend/src` | `BookService.getNotebookBookFile` is used only by this endpoint; `notebookBookFileFromBook` stays (used by `BooksController`). Three test classes read bytes through it: `NotebookBooksBookFileControllerTest` (exercises the endpoint), `NotebookBooksAttachControllerTest:77`, `NotebookBooksAttachNotebookFileControllerTest:53,76` (observe attached bytes) |
| All file-serving endpoints share one byte store | Trace: all read through `NotebookAttachmentFile.bytes`; Book endpoints share `BookSourceFile.read`; image endpoints share `InlineImage.of` | Yes; only the Book endpoints set ETag/304/Cache-Control |

## Commands

- Focused backend tests in this worktree:
  `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '<pattern>'`
  (`docs/worktree-backend-tests.md`); one `--tests` pattern per command.
- Regenerate the API client after each controller removal, as the `generate-api-client` skill
  requires: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, then
  `CURSOR_DEV=true nix develop -c pnpm frontend:test` because generated types feed the frontend.
  CI's generated-code check fails if this is skipped.
- Each removal slice runs the full backend suite once at its end, because the `backend-testing`
  skill requires the full suite for backend changes. In the execution worktree that is
  `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree` (its own database); unset
  `SPRING_DATASOURCE_URL DB_URL SPRING_FLYWAY_URL` first.

## Proof

| Promise | Owning slice | Observation |
| --- | --- | --- |
| Recall-log writes stay proved without the recall-logs endpoint | 1 | The 7 rewritten test classes pass, reading logs from recall history |
| Each dead endpoint is gone from the public API with its orphaned support | 2–5 | Regenerated `open_api_docs.yaml` and `api-summary.md` no longer list it; `git grep` for its operation name, path, and orphaned classes finds nothing outside Git history |
| Supported journeys keep working | 2–5 | Full backend suite and `frontend:test` green; kept siblings' tests (`getTokenInfo`, `deleteToken`, recall history, recall stats, `BooksControllerTest`) unchanged and green |
| The owner has one file-serving proposal | 6 | Seed section naming endpoints, consumers, differences, recommendation, benefit, consumer impact, migration work |

## Ordered slices

### 1. Recall-log tests observe logs through recall history
Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test:worktree --tests '*Recall*'` (355 green,
all six rewritten `Recall*` classes selected) and `--tests '*MemoryTracker*'`
(`MemoryTrackerTrackingControllerTest` 10/10) green; `git grep getRecallLogs -- backend/src/test`
empty. Tests read logs through `ControllerTestBase.recallLogsOf`.
Learning: `backend:test:worktree` accepts exactly one `--tests` pattern; run one command per pattern.

Rewrite the 20 `getRecallLogs` call sites in `MemoryTrackerTrackingControllerTest`,
`MemoryTrackerRecallHistoryRetrievabilityTest`, `LearningSessionRecordTutorFeedbackRecallLogTests`,
`RecallPromptAnswerControllerTest`, `RecallPromptAccidentalMatchGradingTests`,
`RecallPromptAccidentalMatchConfusionAdjustmentTests` (including its `assertConfusionLogged` /
`assertNoConfusionLog` helpers), and `RecallPromptOverlapTryAgainTests` to read
`getRecallLog()` from `memoryTrackerController.getRecallHistory(...)`, following
`MemoryTrackerRecallHistoryControllerTest`. Same assertions; no product change.

Enables slice 2.

### 2. The public API no longer offers recall logs by memory tracker
Type: Behavior
Status: done
Proof: regenerated API artifacts omit `getRecallLogs` and `/recall-logs`; full backend suite (2703,
0 failures) and `frontend:test` (1943) green. `MemoryTrackerService.getRecallLogs` lost its only
outside caller and was inlined into `getRecallHistory`.

Behavior: a memory tracker with recall logs → the owner inspects the public API → no recall-logs
endpoint is listed, while the memory tracker page still shows recall history through
`getRecallHistory`. Remove `MemoryTrackerController.getRecallLogs` and its unused `RecallLog` import.

### 3. The public API no longer offers bearer-token self-revocation
Type: Behavior
Status: done
Proof: regenerated artifacts omit the `DELETE /api/user/token-info` operation (`GET` stays);
`UserTokenControllerTest` 9/9 green without the two revoke tests; full backend suite (2701, 0
failures) and `frontend:test` (1943) green.

Behavior: a user with an access token → the owner inspects the public API → token info can still be
read and tokens deleted by id from settings, but no bearer self-revocation exists. Remove
`revokeToken`, `persistedUserTokenFromBearerOrThrow`, and
`revokeTokenDeletesTokenByBearerAuth` / `revokeTokenReturns401ForInvalidToken`.

### 4. The public API no longer offers recall probe diagnostics
Type: Behavior
Status: done
Accepted: grep empty; full backend suite (2679, 0 failures) and `frontend:test` (1943) green.
`RecallPaceAggregator.compute`'s `todayRowsToScore` overload was already dead before this story
(its caller left in 8ca3115dd5), so it stays out of scope.
Proof: regenerated artifacts omit both diagnostic operations and their DTO schemas; `git grep` finds
no `EzDiffusion`, `ConvergentValidity`, or `RecallProbeConvergentValidity`; recall-stats tests
(`UserRecallStatsControllerTest`, `RecallStatsServiceAccuracyGuessingFloorTest`) green; full backend
suite and `frontend:test` green.

Behavior: a user with recall and daily-probe history → the owner inspects the public API → recall
stats remain, and neither diagnostic endpoint exists. Remove both endpoints with their Javadoc, the
two `RecallStatsService` compute methods and imports, `RecallEzDiffusion`, `EzDiffusion`,
`RecallProbeConvergentValidity`, both DTOs, and their five test classes. Make
`RecallPaceAggregator.ABSOLUTE_FLOOR_MS` / `HARD_DROP_MS` and `DailyProbeDaySeries.latestByLocalDay`
private and drop the Javadoc that described the removed callers. Keep shared fixtures that other tests use.

### 5. The public API serves a Book's file only by Book
Type: Behavior
Status: done
Accepted: full backend suite (2674, 0 failures) and `frontend:test` (1943) green; focused
`--tests 'com.odde.donut.controllers.*Book*'` (100) green after refactor. The deleted test class's
`deleteBook` guards moved to `NotebookBooksDeleteBookControllerTest`; its unknown-source-path 404
moved to `BooksControllerTest`. `BookService.notebookBookFileFromBook` is now `bookFile`.
Proof: regenerated artifacts omit `getBookFile` and `/api/notebooks/{notebook}/book/file`;
`BooksControllerTest`, `NotebookBooksAttachControllerTest`, and
`NotebookBooksAttachNotebookFileControllerTest` green; full backend suite and `frontend:test` green.

Behavior: a notebook with an attached Book → the reader opens it → the Book's bytes still arrive
through `GET /api/books/{book}/file`, and no notebook-addressed Book file endpoint exists. Remove
`NotebookBooksController.getBookFile`, `BookService.getNotebookBookFile` and anything only it used,
and `NotebookBooksBookFileControllerTest` (its cases are covered by `BooksControllerTest`: missing
book, read authorization, bytes, ETag/304). Change the two attach tests to observe attached bytes
through `BooksController` for the attached Book.

### 6. The owner can decide on file-serving consolidation
Type: Behavior
Status: done
Accepted: `## File-serving proposal` in the seed recommends retiring `GET /api/books/{book}/file`
in favor of `/attachments/{attachment}/content` with its caching, and keeping both image endpoints;
cited handlers and the reader's hand-built URL re-read against the code.
Proof: a `## File-serving proposal` section in the seed, reviewed against the code it cites.

Behavior: the four remaining file-serving endpoints (`/api/books/{book}/file`,
`/attachments/{attachment}/content`, `/attachments/{attachment}/image`, `/api/notes/{note}/image`)
→ compare consumers, authorization, lookup, response headers, and caching (only the Book endpoint
sets ETag/304/Cache-Control; all read `NotebookAttachmentFile.bytes`) → one recommendation naming
affected endpoints, intended benefit, consumer impact, and migration work. No product code changes.
