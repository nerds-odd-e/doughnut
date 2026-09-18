# Compact spelling results

Status: planned
Source: [SEED-033 story 1](../../seeds/SEED-033-simplify-note-presentation.md#story-1).
Authority: owner request of 2026-09-18 to queue, refine, and slice-plan this
story. Planning only; execution has not started.
Baseline: `d551a74c906afe447409ec54c798d2ce2d8c8bc0`.

## Goal and scope

Show spelling results with their existing outcome and note link, without the
full embedded note. Remove the embed and obsolete tests/setup; preserve the
current result copy, note/property navigation, memory-tracker link, and
accidental-match resolution. Correct results continue to say “Correct!”; this
story does not add submitted-answer copy to successful results.

The same AnsweredSpellingQuestion serves current results and recall history.
No new result component, preview, API, or compatibility option is needed.
Conversation and Just review embeds, general NoteShow option removal, and
NoteShowPage restructuring belong to story 2 and are excluded here.

## Existing solutions and decisions

- `AnsweredSpellingQuestion.vue` owns the result and renders NoteShow solely
  to show the full focus note. Remove that import and template usage.
- `NoteUnderQuestion` already owns the breadcrumb and linked note title;
  `NoteTitleWithLink` uses the existing named note/property locations. Reuse
  these unchanged instead of adding another navigation representation.
- `NoteRealmLoader` currently loads the realm when the embed mounts. Removing
  the consumer removes that load path without changing shared storage. Do not
  promise zero note fetches for user-triggered accidental-match resolution.
- Other consumers still require NoteShow's embedded options. Keep its current
  shared responsibility until story 2 determines replacements.
- [Accepted ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) governs
  named navigation and rendered href helpers; preserve that existing contract.
  Grading and scheduling remain unchanged under
  [Accepted ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
- Current NORTH-STAR topics govern Git publication, not this local result
  presentation. No new architectural direction or ADR is required.

## Integration with work already taken

[SEED-032](../../seeds/SEED-032-retry-overlapped-spelling-match.md#story-1)
and [plan 143](../143-overlapped-spelling-inline-retry/PLAN.md) change the same
result component to remove overlap-only reveal/retry behavior. Before editing,
inspect the current component and working tree. Preserve whichever compatible
overlap implementation is present; never reintroduce the removed UI or replace
its deleted tests with absence assertions. This is shared-file coordination,
not a product dependency requiring backlog reordering.

## Ordered slices

### 1. Read spelling results and follow the note link for details

Type: Behavior
Status: done

Behavior: Given a completed spelling answer, when the learner views its result
directly or through recall history, the result retains its outcome and linked
focus-note title, with note details accessed through the link instead of a
full inline note. Accidental-match resolution remains available.

Implementation and proof ownership:

1. Update the existing page-level spelling result case in
   `frontend/tests/pages/RecallPage.spelling.spec.ts`: replace the internal
   NoteShow-prop assertion with the visible outcome and focus-note link using
   the production href helper. Keep the memory-tracker navigation proof at the
   rendered boundary. Use the real result and link components, mocking only
   the backend where new setup is needed.
2. Remove NoteShow from `AnsweredSpellingQuestion.vue`, without changing result
   conditions, copy, resolution actions, or shared note-page components.
3. Delete the NoteShow stub from `answeredSpellingQuestionTestSupport.ts` and
   assertions/setup solely supporting the embed in the accidental-match specs.
   Keep their positive explanation/link/Resolve workflow coverage. Do not
   replace deleted internal component assertions with new component-shape tests.
4. Retain existing success/history coverage; if it does not observe the
   result's note link, extend the nearest mounted result/page case to cover
   that positive signal with a successful answer. Do not duplicate the full
   canonical result shape for each grading outcome.

Proof mapping:

| Promise | Evidence owned by this slice |
| --- | --- |
| Incorrect result and reachable focus note | Updated mounted RecallPage spelling case checks the existing incorrect-answer text and rendered note destination. |
| Correct/history results retain useful feedback and navigation | Existing success/history boundary coverage, with only the missing positive link signal added if needed. |
| No embedded full note on spelling results | Production template/import deletion and removal of the now-unused embedding fixture; verify no alternate note renderer was introduced. |
| Accidental-match resolution remains available | Retained mounted accidental-match result and resolution tests. |
| Other note consumers and recall flows remain intact | Full frontend suite after the focused edit; preserve the current overlap implementation. |

Commands:

- During iteration:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/RecallPage.spelling.spec.ts`
- After edits and the required refactor pass:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test`

No backend changes, backend test run, manual-testing session, or new E2E
scenario is needed for this local rendering removal. Existing frontend tests
own the changed behavior; avoid introducing tests that only mirror deletion.

Sizing: one small Behavior leaf targeting about five minutes of editing and
focused proof. Full-suite runtime and required delivery gates may exceed that
target; this is one inseparable result-view change, not multiple outcomes.
If substantive implementation exceeds ten minutes, stop and finer-decompose
unless a concrete test-runtime or integration exception explains the overrun.
Safe stopping point: the result is compact and existing navigation/resolution
works, with both remaining embedded consumers unchanged.

## Later execution gates

Follow dough-execute-plan when implementation is authorized: Jidoka, a fresh
dough-post-change-refactor agent, then verification of resulting edits. API
generation is not expected. The coordinator runs
`./scripts/run.sh pnpm format:changed` once, updates this plan without a second
routine formatting pass, and follows the commit hook, push, and asynchronous
CI repair workflow. Keep this story queued until execution actually begins.

## Current decisions and assessment

- Scope is the small spelling-result removal, not the later shared-page cleanup.
- No open product decision for this story.
- One common result component handles the affected views; there is no new
  abstraction or cumulative special-case design.
- No unresolved slice-specific design or sizing concern was identified in
  this planning assessment. Reconcile the explicitly identified shared-file
  overlap work at execution time.
- Slice 1 executed: `NoteShow` removed from `AnsweredSpellingQuestion.vue`'s
  spelling-result view; results now rely on the existing
  `NoteUnderQuestion`/`NoteTitleWithLink` breadcrumb link. Updated
  `RecallPage.spelling.spec.ts` (both the incorrect-result and the
  resumed/history-view correct-result cases) to assert the rendered note link
  instead of internal `NoteShow` props, removed the `NoteShow` stub and its
  supporting assertions from `answeredSpellingQuestionTestSupport.ts` and
  `AnsweredSpellingQuestionAccidentalMatch.spec.ts`. Full frontend suite
  passed (344 files / 1892 tests). A fresh post-change-refactor pass found no
  candidates — already clean.
