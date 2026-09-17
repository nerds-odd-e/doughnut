# Hide trashed referrers from incoming references

Status: executed
Source: [SEED-009 story 41](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-41),
refined 2026-09-17. The owner authorized slice planning, but not execution.

## Goal and scope

When a notebook owner trashes a note that directly authors a reference to an
active note, a fresh view of the active target must not list the trashed note as
an incoming reference, regardless of the referrer's former folder depth. The
selected **Leave as dead links** choice continues to preserve the referrer's
authored content; this work changes visibility, not authored bytes.

This story includes body and frontmatter references as instances of the same
authored-reference rule and nested trash paths. It does not reinterpret the
lifecycle of a separate active relationship note whose endpoint is trashed,
change reference cleanup or rewriting, add trash navigation, change Git
synchronization, or alter folder-location trash semantics.

The design goal is to make the existing object-level trash rule cohesive,
proxy-safe, and simpler, preferably reducing production lines. Line count is a
supporting check rather than a completion criterion; do not compress proof or
introduce cleverness to satisfy it.

## Existing-solution assessment and architectural decisions

Source and tests were inspected at `5a221da58e`; no automated checks were run
during planning.

| Responsibility | Existing owner and decision |
| --- | --- |
| Persisted trash selection | The recursive `trashed_folder` view and the existing `@Formula` / `Note.JPA_AVAILABLE` / `Note.NATIVE_AVAILABLE` predicates already identify every descendant of a case-insensitive root `_trash`. Reuse unchanged. |
| Current object membership | `Folder.isTrashed()` is the shared domain owner used by `Note.isTrashed()` / `Note.isAvailable()`. Change this owner so ancestry traversal follows mapped accessors and therefore initializes lazy parent proxies. Preserve immediate in-transaction move behavior and the rule that only root `_trash` starts trash membership. Prefer a shorter recursive expression if it remains clear. |
| Incoming-reference visibility | `AuthoredNoteReferenceInboundFacade` already excludes a source when `sourceNote.isAvailable()` is false. Reuse this rule unchanged; do not add incoming-reference-only trash predicates, eager folder loading, a cache, or another membership state. |
| User-visible proof | `NoteController.showNote(...).getReferences()` is the stable API boundary rendered by the existing note page. Extend its controller coverage with the reproduced deep-folder case and a persistence-context clear so the proof exercises lazy reloading rather than the already-working in-memory graph. |

This follows [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md)
for Note, Folder, Wiki link, Property, and Relationship note meanings, and
[ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#trash),
which makes root `_trash` location—including all descendants—the authority for
trash membership and preserves authored reference spelling. It also follows
the existing [North Star](../../NORTH-STAR.md#one-complete-accepted-web-change)
direction that trash is a move into a location and recovery reuses existing
availability rules. No ADR conflict or new North Star topic was found.

## Outside-in proof

The owning example is:

- Given active target A and active referrer B under `Depth One/Depth Two`, with
  B directly authoring `[[A]]`, when the owner trashes B with **Leave as dead
  links**, clears the persistence context, reloads A, and shows it, A's
  `references` list is empty.

Existing coverage remains responsible for active referrers appearing, authored
content surviving Trash/Undo, root-only and case-insensitive persisted trash
membership, and immediate in-transaction object membership changes. The new
test owns only the depth-dependent fresh-load regression.

Proof command:

`CURSOR_DEV=true nix develop -c pnpm backend:test_only`

Per backend rules, run the full backend unit suite. No E2E addition is selected:
the backend `NoteRealm.references` response is the exact user-facing data, and
the frontend already renders that collection without a separate visibility
rule. No API generation or database migration is anticipated.

## Ordered slices

### 1. A deeply trashed referrer disappears after reload

Type: Behavior
Status: done
Proof: the controller example above fails before the change and passes afterward;
the full backend unit suite remains green.

Delivered: `Folder.isTrashed()` (`backend/src/main/java/com/odde/donut/entities/Folder.java`)
now traverses ancestry through the mapped `getParentFolder()`/`getName()`
accessors instead of the raw `parentFolder` field, so every lazy ancestor
proxy initializes during traversal after a persistence-context clear. Root-only,
case-insensitive `_trash` semantics and in-transaction membership are
unchanged; the rewrite is a net reduction from an imperative loop to a short
recursive expression. `AuthoredNoteReferenceInboundFacade` reused unchanged as
planned. Post-change refactor review found `Note.isTrashed()`,
`FolderConstructionService`, and `FolderRelocationService` already call
`Folder.isTrashed()` via method dispatch (not the raw field), so they inherit
the fix automatically; `FolderMoveDestinationRules.folderIsStrictDescendantOf`
and `FolderSubtreeLiveNotes.folderDepth` already traverse via
`getParentFolder()` and share no defect. No other production edit was needed.

Proof: new regression test
`NoteControllerShowTests.deeplyTrashedReferrerDisappearsFromTargetAfterFreshReload`
(`backend/src/test/java/com/odde/donut/controllers/NoteControllerShowTests.java`)
builds active target A and active referrer B two folders deep
(`Depth One/Depth Two`) with B directly authoring `[[A]]`, trashes B with
`leaveDeadLinks()`, flushes and clears the persistence context, reloads A via
`NoteRepository`, and asserts `NoteController.showNote(A).getReferences()` has
size 0. Confirmed failing (`hasSize<1>`) against the original implementation
and passing after the fix. Full suite:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` — green.

Behavior: Given an active note B two folders deep directly references active A,
when B is trashed with authored links preserved and A is freshly reloaded, A no
longer reports B as an incoming reference.

Drive the failing example through the existing note controllers and real
database, then make the smallest change in the shared `Folder.isTrashed()`
owner. Traverse mapped parent accessors so every lazy ancestor participates,
while retaining current in-transaction ancestry and root-only `_trash`
semantics. Do not special-case incoming references. Inspect the production diff
afterward: prefer a net reduction in the membership implementation, but keep
readability and the required proof ahead of line count.

Sizing hypothesis: about five minutes of active implementation and cleanup,
plus the mandatory full-suite wait. The slice has one behavior, one proof loop,
and no separable preparation.

## Current decisions

- Fix the shared object-level membership rule; retain the database view as the
  persisted-query owner.
- Prove a direct authored reference after a real Trash and fresh reload; do not
  add parallel body/property/URL cases that exercise the same referrer filter.
- Preserve the existing active-relationship-note behavior when only an endpoint
  is trashed; that is outside this story.
- Planning stops here. Execution, refactoring, formatting, commit, push, CI,
  retrospective, and story wrap-up require the applicable later workflow.

## Learnings

- The earlier non-reproduction covered only root and one-folder-deep referrers;
  two original folder levels are required to expose the lazy-proxy traversal
  defect after Trash mirrors the path under `_trash`.
- The database `trashed_folder` view already returns the reproduced deep folder,
  so replacing or duplicating persisted membership is not indicated.
- Existing E2E behavior intentionally leaves a separate relationship note
  active when an endpoint is trashed; treating that active note as though it
  were the trashed referrer would change relationship lifecycle semantics.
