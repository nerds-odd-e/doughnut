# Start every frontend spec's real router from one shared helper

## Source

- Story: [Start every frontend spec's real router from one shared helper](../../seeds/SEED-039-faster-ci-feedback.md#specs-share-production-router)
- Identity: SEED-039#specs-share-production-router
- Provenance: a retrospective finding on the story that added
  `productionRouterAt`
  (`8cb73fa479:.planning/seeds/SEED-039-faster-ci-feedback.md#internal-mocks-to-real-modules`).

## Goal and scope

Contributors can read and trust the routing in a frontend spec. A router over
the production `routes` is always started by the shared helper, and no test's
starting route depends on the test that ran before it.

- Every test starts with the browser URL at root, set in shared test support.
- The 11 files under `frontend/tests` that build their own router over the
  production `routes` get it from `productionRouterAt(location)`, or from plain
  `withRouter()` when the start does not matter to the spec.
- Test names stay, and no assertion is weakened.
- The frontend testing skill states the rule.

Excluded: the two `tests/routes` specs; routers over
`dummyRouteRecordsFromMetadata`; routers with a single stand-in route; a lint
rule; production code.

## Architecture

- Existing solution reused: `productionRouterAt` in
  `frontend/tests/helpers/RenderingHelper.ts`. No new helper is added.
- The reset joins the popup stack reset in the `beforeEach` of
  `frontend/tests/setupVitest.ts`, so each test's clean start has one owner.
- Routing rule followed: the frontend testing skill's "Routing assertions"
  section and ADR 0005. No North Star topic governs this work.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| A router from `withRouter()` starts at root whatever ran before | 1 | New spec `frontend/tests/helpers/RenderingHelper.spec.ts`: one test moves a production router to the recall location; the next mounts with `withRouter()` and reads the current route name `root` |
| The reset breaks no existing spec | 1 | Whole frontend suite, because every spec loads `setupVitest.ts` |
| The 6 spec-local browser-history routers come from the shared helper | 2 | Those specs pass with unchanged names and assertions |
| The 2 page support factories come from the shared helper | 3 | The 9 `FolderPage*` and `NoteShowPage*` specs that call them pass unchanged |
| The 3 memory-history production routers come from the shared helper | 4 | `MessageCenterPage.spec.ts`, `RecentSettingsTab.spec.ts`, and the `MainMenu*` specs pass unchanged |
| Only `RenderingHelper.ts` and the two `tests/routes` specs pass production `routes` to `createRouter(` | 4 | Listing below returns only those three files |
| The frontend testing skill states the rule | 4 | `.agents/skills/frontend-testing/SKILL.md`, "Routing assertions" |

Commands (run from the repository root):

- Focused: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --browser=chromium --browser.headless <spec files>`
- Whole suite (about 40 s): `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`
- Finished-state listing:
  `grep -rlE 'routes(: \[\.\.\.routes|,| \})' $(grep -rl 'createRouter(' frontend/tests) | xargs grep -l '@/routes/routes"'`

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| A plain `withRouter()` starts at the URL the previous test left | Slice 1 fixes this symptom | Temporary two-test spec at `b2f10e70ee`: after `productionRouterAt({ name: "recall" })`, the next test's `withRouter()` reported `recall`, not `root` | Symptom reproduced |
| Resetting the URL to root before each test keeps the suite green | Slice 1 | Trial line `window.history.replaceState(window.history.state, "", "/")` in the `beforeEach` of `setupVitest.ts`, whole suite run: 321 files, 2013 tests passed, including the probe above | Holds |
| The reset must keep the history state | Slice 1 | The same trial with `replaceState({}, "", "/")` failed 4 tests: `countHistoryEntriesAdded` read `NaN` | Holds; pass the current `history.state` |
| 11 files outside `tests/routes` pass production `routes` to `createRouter(` | Slices 2–4 | `grep -rn -A8 'createRouter(' frontend/tests`: 8 with browser history, 3 with memory history, as listed in the story | Holds |
| The two page factories are called without arguments from 9 spec files | Slice 3 sizing | `grep -rn 'createNoteShowPageRouter\|createFolderPageRouter' frontend/tests`: 5 and 4 spec files | Holds |

The trial edits were removed after the observations.

## Slices

### 1. Every test starts at the root location
Type: Behavior
Status: done
Proof: the new `RenderingHelper.spec.ts` cases, then the whole frontend suite.
Accepted proof: `RenderingHelper.spec.ts` failed first with `expected 'recall'
to be 'root'`, then passed with the reset; whole suite 321 files, 2014 tests
passed; `vue-tsc --noEmit` clean.

Behavior: an earlier test in the same file left a router at the recall
location → a test mounts a component with `withRouter()` → its current route
is `root`. The reset is one line beside `emptyPopupStack()` and keeps the
current history state.

### 2. Spec-local browser-history routers come from the shared helper
Type: Structure
Status: done
Proof: the six touched specs and the `QuillEditor*` specs that use the harness
pass with unchanged test names and assertions.
Accepted proof: the six files plus `QuillEditor.spec.ts` and
`QuillEditor.paste.spec.ts`, 7 files and 34 tests, passed; no added or removed
line holds an `it(` or `expect`; whole suite 321 files, 2014 tests passed
before the import-only refactor; `vue-tsc --noEmit` clean. Specs import
`productionRouterAt` from the `@tests/helpers` barrel.

Internal change: `quillEditorTestHarness.ts`, `RecentlyRecalledNotes.spec.ts`,
`FolderSelector.spec.ts`, `NoteMoreOptionsForm.spec.ts`,
`NoteUnresolvedWikiLinkModal.spec.ts`, and `NotebookCatalogReadBook.spec.ts`
drop their own `createRouter` and use `productionRouterAt` with a named
location, or plain `withRouter()` where the spec never reads the router.
Routers created once per file move into the test setup so each test gets its
own. Weakness removed: several ways to start a production router in specs.

### 3. Page support routers come from the shared helper
Type: Structure
Status: planned
Proof: the `FolderPage*.spec.ts` and `NoteShowPage*.spec.ts` files pass with
unchanged test names and assertions.

Internal change: `createFolderPageRouter` and `createNoteShowPageRouter` are
deleted; their callers call `productionRouterAt` with the page's own named
location. Same weakness as slice 2.

### 4. Memory-history production routers come from the shared helper
Type: Structure
Status: planned
Proof: `MessageCenterPage.spec.ts`, `RecentSettingsTab.spec.ts`, and the
`MainMenu*.spec.ts` files pass unchanged; the finished-state listing returns
only `RenderingHelper.ts`, `routes.spec.ts`, and `noteRouteFamily.spec.ts`;
then the whole frontend suite.

Internal change: `MessageCenterPage.spec.ts`, `RecentSettingsTab.spec.ts`, and
`mainMenuTestSupport.ts` use `productionRouterAt`. The frontend testing skill's
"Routing assertions" section says a production-routes router in a spec comes
from `productionRouterAt` or `withRouter()`, and that every test starts at
root. Same weakness as slice 2. Depends on slice 1: these routers move from
memory history to browser history, so they need the shared reset.

## Current decisions

- The known start is the root location, set by replacing the URL and keeping
  the history state; no router is created in shared setup.
- Slices 2–4 are Structure slices that own the retrospective correction
  directly; they add no product behavior.
