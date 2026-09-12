# Document non-obvious rationale in the publication index-refresh simplification

Status: completed.
Source: bounded retrospective correction from
[dough-execution-retrospective](../../../.claude/skills/dough-execution-retrospective/SKILL.md)
on the completed execution of
[108-publish-notebook-edits-faster](../108-publish-notebook-edits-faster/PLAN.md)
(source story [SEED-018 story 3](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-3)).
Reviewed commit manifest: `a6fcddacad` (profiling infrastructure),
`a05a66686c` (production simplification), both on branch
`quick/108-publish-notebook-edits-faster`. This plan is planning-only; no
execution authority is granted here.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main`
- Execution checkout: `/Users/terryyin/git/doughnut-quick-109-document-publication-index-refresh-rationale`
  on `quick/109-document-publication-index-refresh-rationale`
- Integration target: `main`
- CI observation: unavailable for this execution branch because `ci.yml`
  (`donut CI`) is push-triggered only for `main`; branch CI remains unobserved.

## Beneficiary and bounded outcome

A future engineer reading or modifying `NoteAliasIndexService.refreshForNote`
or `NotePropertyIndexService.refreshForNote` can see, in the code itself, why
each method's index-maintenance approach looks different from neighboring
patterns elsewhere in the codebase — so a well-intentioned future edit does
not silently reintroduce the whole-session-flush cost that
[108-publish-notebook-edits-faster](../108-publish-notebook-edits-faster/PLAN.md)
just removed (median publication time for a 1,000-note edit commit dropped
from 29,746.720 ms to 10,308.043 ms, a 65.35% reduction). This is a
documentation-only correction: no behavior, public API, or test outcome
changes.

## Current findings and scope

Retrospective review of the delivered change (`a05a66686c`) found two related,
low-severity coherence gaps — both are exactly the kind of "hidden constraint /
subtle invariant / workaround" this project's own `general.mdc` says warrants
a comment, and neither currently has one:

1. **`NotePropertyIndexService.refreshForNote`'s `entityManager.persist(note)`
   call is load-bearing but unexplained.** It exists so that
   `Note.authoredNoteReferenceRows` (`cascade = CascadeType.ALL`) cascades a
   persist onto any still-transient `AuthoredNoteReferenceRow` children before
   `note.authoredReferenceRowsBySourceLocalKey()` is read a few lines later —
   this is exactly the transient-reference problem that broke the rejected
   first prototype (`TransientPropertyValueException` in two
   `TextContentControllerUpdateNoteTitleInboundWikiReferencesTests` cases,
   per `docs/notebook-publication-profiling.md#smaller-workload-refinement`).
   The neighboring detach-before-bulk-delete step already has a rationale
   comment; this call does not. A future reader could reasonably see it as
   redundant (the note is presumably already managed) and remove it, silently
   reintroducing that exact failure mode.

2. **Two new inline `entityManager.createQuery("DELETE ...").setFlushMode(FlushModeType.COMMIT).executeUpdate()`
   call sites (`NoteAliasIndexService.refreshForNote`,
   `NotePropertyIndexService.refreshForNote`) diverge from this codebase's
   established bulk-delete idiom** — a repository-level `@Modifying @Query`
   method, still used by `NoteEmbeddingRepository`, `NoteLevelIndexRepository`,
   and `QuestionGenerationBatchRequestRepository`. The divergence has a real
   reason (per-call `FlushModeType.COMMIT` control to avoid the extra flush a
   `@Modifying` repository method would trigger — the entire point of this
   performance change), but nothing in the code says so. A future
   "consistency cleanup" converting these back to `@Modifying` repository
   methods would look like a harmless refactor while quietly reintroducing
   the removed per-index flush cost.

Both findings are documentation-only; no defect, regression, or scope drift
was found in the delivered behavior, and the full backend suite (2,404 tests)
and three 1,000/1,000 candidate captures already prove the current behavior
and performance. No other implementation, process, or product finding from
this retrospective required correction.

## Preserved promises and constraints

Preserve all behavior, public API, transaction/rollback ownership, and test
outcomes established by
[108-publish-notebook-edits-faster](../108-publish-notebook-edits-faster/PLAN.md).
This correction adds comments only; it must not change any executable
statement, formatting-affecting code shape, or test assertion. No new ADR is
implied — this documents an existing implementation detail, not a new
architectural decision.

## Ordered slices

### 1. Add rationale comments to the two non-obvious index-refresh steps
Type: Structure
Status: done
Change: In
`backend/src/main/java/com/odde/donut/services/NotePropertyIndexService.java`,
add a short comment on the `entityManager.persist(note)` call explaining that
it cascades a persist onto `Note`'s still-transient `authoredNoteReferenceRows`
(via `cascade = CascadeType.ALL`) so they are readable through
`authoredReferenceRowsBySourceLocalKey()` before flush — and that removing it
reintroduces the `TransientPropertyValueException` the rejected first
prototype hit. In both
`backend/src/main/java/com/odde/donut/services/NoteAliasIndexService.java`
and `NotePropertyIndexService.java`, add a short comment on (or immediately
above) the inline `entityManager.createQuery("DELETE ...")` calls explaining
that they bypass this codebase's usual repository `@Modifying @Query`
bulk-delete idiom specifically to hold `FlushModeType.COMMIT` and avoid the
extra flush that idiom would otherwise force — the exact cost this change
removed — and that converting them back to a `@Modifying` repository method
would reintroduce it.

Keep each comment to the shortest sentence that conveys the invariant; do not
restate what the code already says, and do not reference this plan, story, or
ticket by name (comments describe current behavior, not development history).

Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passes
unchanged (comment-only change; no new test is needed since no behavior
changes). Visual diff review confirms only comment lines were added and no
executable statement, import, or formatting-relevant code moved.

Sizing: under 5 minutes — three short comments in two existing files.

Execution proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
passed unchanged. Visual diff review confirmed that the production change adds
only comment lines and moves no executable statement or import. The independent
post-change refactor found the comments already clean and made no further edits.

## Promise-to-proof ownership

| Promise | Owner | Observable proof |
| --- | --- | --- |
| No behavior change | 1 | Full backend suite unchanged; diff contains only comment additions |
| Non-obvious invariants are now documented in place | 1 | Comments present at both call sites, reviewed for accuracy against the cited investigation doc |

## Execution and delivery gates

Follow dough-execute-plan: Jidoka and proof inspection, a fresh
dough-post-change-refactor agent (expected to find nothing further — this is
a comment-only change), one coordinator
`./scripts/run.sh pnpm format:changed`, commit via the check-only lint hook,
push, and asynchronous CI handling. No generator regeneration applies (no
controller or DTO signature changes).
