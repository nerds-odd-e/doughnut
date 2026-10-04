# Build the test router over production routes in one place

## Source

- Story: [Build the test router over production routes in one place](../../seeds/SEED-039-faster-ci-feedback.md#one-production-router-builder)
- Identity: SEED-039#one-production-router-builder
- Provenance: retrospective of
  [SEED-039#specs-share-production-router](../../seeds/SEED-039-faster-ci-feedback.md#specs-share-production-router),
  plan `slice-plans/002-specs-share-production-router`, reviewed commits
  98728709f1, fb541c485a, 3a412044e8, 863553ca54.

## Findings

- `frontend/tests/helpers/RenderingHelper.ts` builds
  `createRouter({ history: createWebHistory(), routes })` twice: in
  `productionRouterAt` and as the default of `withRouter()`. The reviewed
  story made both the only supported ways to get that router, so the
  construction is one concept in two places.
- `frontend/tests/pages/noteShowPageTestSupport.ts`: `renderNoteShowPage` and
  `noteShowPageWithSidebarLayoutMount` have no callers, and
  `frontend/tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue` is used
  only by them. They became unused in f9acb3cb44, before the reviewed story;
  the reviewed story left `renderNoteShowWith` as an indirection with one real
  caller.

## Preserved

- `productionRouterAt(location)` and `withRouter()` keep their signatures and
  behavior; every test still starts at root (reset in `setupVitest.ts`).
- Test names and assertions are unchanged. No production code changes.

## Proof

| Promise | Slice | Proof |
| --- | --- | --- |
| One construction backs both helpers | 1 | `grep -n 'createRouter(' frontend/tests/helpers/RenderingHelper.ts` shows one call; `RenderingHelper.spec.ts` passes |
| Unused NoteShowPage sidebar support is gone | 1 | `NoteShowPage*.spec.ts` pass; `vue-tsc --noEmit` clean |
| Nothing else breaks | 1 | Whole frontend suite |

Commands (from the repository root):

- Focused: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --browser=chromium --browser.headless tests/helpers/RenderingHelper.spec.ts tests/pages/NoteShowPage`
- Typecheck: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit >/dev/null 2>&1; echo $?`
- Whole suite: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The sidebar-layout path has no callers | `grep -rn 'renderNoteShowPage\b\|noteShowPageWithSidebarLayoutMount\|NoteShowPageWithNotebookSidebarLayout' frontend/` at 863553ca54: only definitions in `noteShowPageTestSupport.ts` | Holds |
| `withRouter()` default and `productionRouterAt` build the same router | `RenderingHelper.ts` lines 13 and 78–81 at 863553ca54 | Holds |

## Slices

### 1. One production router builder, no unused NoteShowPage support
Type: Structure
Status: planned
Proof: the focused command, the typecheck, then the whole suite.

Internal change: one private function in `RenderingHelper.ts` builds the
production-routes router; `productionRouterAt` and `withRouter()` use it.
Delete `renderNoteShowPage`, `noteShowPageWithSidebarLayoutMount`, the
fixture `NoteShowPageWithNotebookSidebarLayout.vue`, and the union type in
`noteShowPageHelper`; fold `renderNoteShowWith` into
`renderNoteShowPageWithoutSidebar`. Weakness removed: a duplicated router
construction and dead test support.

## Current decisions

- Keep `renderNoteShowPageWithoutSidebar`'s name; renaming its callers is
  outside this correction.
