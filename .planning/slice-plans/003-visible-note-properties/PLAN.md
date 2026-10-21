# Restore visible properties and footer timestamps

**Identity:** SEED-082#story-1
**Source:** [Restore visible note properties and bottom timestamps](../../seeds/SEED-082-visible-note-properties.md#story-1)

## Goal and scope

Readers and authors read properties directly above a note's body and find Created
and Last updated below its body and images, before references. Keep rich property
editing, raw Markdown, permissions, row-level learning actions, existing property
locations, activity presentation, responsive rows and the bounded reading column.
Delete the whole-section disclosure and all exclusively dependent support.

The owner's constraint is no new navigation. New routes, visibility preferences,
property capabilities and sidebar redesign are outside this delivery. Preserve
authored titles/content and the historical footer surface: full `NoteShow`,
including read-only and Markdown modes. Context readers retain their own composition.
This preparation authorizes no implementation or Take.

## Current decisions and reuse

- PFE: `RichFrontmatterProperties` and `useRichFrontmatterPropertyEditing` already
  own rows, parsing, valid-empty insertion, drafts and validation. Standalone rich
  editors already present these directly. Reuse them; remove the enclosing
  visibility concern rather than adding a preference or an always-true state.
- `NoteShow` owns the ordering of text, image and references. Its version at
  `8d30de7ea3^` places the footer between image and references. Recover that
  responsibility using current timestamps and localized formatting. Keep current
  recency emphasis with the footer; `noteRecentUpdate.ts` consumes that observable.
  The old indicator's color implementation is historical evidence, not a reason
  to revert unrelated styling. Keep timestamp formatting/emphasis in one owner.
- `NoteTextContent` also serves `NoteContextReader` and learning contexts.
  Properties remain available through that shared surface; the footer belongs
  to the full-note composition. No API, schema, generated-client or backend
  responsibility changes are anticipated.
- `useFocusedNoteProperty` owns property focus/scroll and existing named locations;
  `RichFrontmatterPropertyPanel` and `useAssimilationView` own eligible learning
  presentation. Delete only their whole-section visibility dependencies. Preserve
  note-identity transitions, missing-property feedback, panel opening/closing,
  and the active activity control's priority and lifetime.
- `useAutoCollapseDetails` is shared dropdown behavior, not the note-section
  disclosure; retain it and its callers. Likewise retain row-panel toggles.
- Accepted [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  requires preserving authored YAML. [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md)
  retains authored-key property identity, named locations, replace/push semantics
  and explicit missing-property feedback. [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
  governs the isolated checkout's tests. Index and relevant in-file statuses agree;
  no conflict or exception. ADR 0001's accepted status agrees with the index despite
  its filename omitting the accepted suffix; no status change is implied.
- `.planning/NORTH-STAR.md` supplies no additional presentation decision. Existing
  storage, learning and route owners suffice; no new architectural direction.

## Preparation observations

Observed 2026-10-21 in
`/Users/terryyin/git/doughnut/.worktrees/restore-visible-note-properties-and-bottom-times`
at `8b1b5d91f0b6d2b0fac687555f7b39fe0210f9a7`. All temporary source and test edits
were reverted before seed/plan recording; `git status --short` then returned empty.
These are headless browser observations, not delivered implementation or acceptance
of future edits. No application E2E stack, backend tests or production system ran.

1. Baseline: the current disclosure, dates, drafts, row layout and activity work
   through their mounted boundaries. This command passed 32 tests in six files:

   ```sh
   CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteTextContent.detailsEditing.spec.ts tests/notes/NoteTextContent.dates.spec.ts tests/notes/NoteTextContent.readingLayout.spec.ts tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/components/form/RichMarkdownEditor.listPropertyLayout.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts
   ```

2. Discriminating reproduction: temporary `NotePresentation.preparation.spec.ts`
   mounted `NoteTextContent` with authored `topic: training` in editable/read-only
   modes, checked visible section geometry above the body, and mounted `NoteShow`
   with `image: images/example.png` to check labeled timestamp metadata after the
   image. This command failed all three cases on unchanged product code: both
   sections were hidden and the footer was missing.

   ```sh
   CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NotePresentation.preparation.spec.ts
   ```

3. Bounded trial: temporarily deleted the disclosure provider and its consumers
   in the property section, focus watcher, insertion path and learning panel;
   removed the header control/date slot and placed a minimal labeled date block
   after the image in `NoteShow`. Adapted the editing and workspace-activity
   fixtures by deleting their reveal operations and disclosure-only assertions.
   This command passed all 25 cases, including the three previously failing
   observations, validation recovery/save, rich/raw property insertion, direct
   property learning, panel close/reopen, note changes and activity priority:

   ```sh
   CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NotePresentation.preparation.spec.ts tests/notes/NoteTextContent.detailsEditing.spec.ts tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/components/form/RichMarkdownEditor.listPropertyLayout.spec.ts tests/components/form/RichMarkdownEditor.readOnlyProperties.spec.ts
   ```

4. The same trial, with the context-reader fixture's reveal operation deleted,
   passed these 34 cases through real components: context reading, property
   locations, coarse-pointer row controls, editing, list learning and wiki links.

   ```sh
   CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteContextReader.spec.ts tests/components/form/RichMarkdownEditor.propertyLocation.spec.ts tests/components/form/RichMarkdownEditor.propertyTouchTargets.touch.spec.ts tests/components/form/RichMarkdownEditor.propertyRowEditing.spec.ts tests/components/form/RichMarkdownEditor.listPropertyMemoryTracking.spec.ts tests/components/form/RichMarkdownEditor.propertyWikiLinks.spec.ts
   ```

The trial settles reuse and the principal runtime wiring/sizing premise. It did
not implement final footer styling or the complete cleanup, and does not replace
the final proof below. New footer geometry/localization tests must establish their
own failure before the final implementation. Existing baseline date tests prove
current formatting/emphasis, not the planned footer's position.

## Outside-in proof ownership

All behavioral proof below establishes surviving/replacement behavior or existing
permission refusal. Deletion sweep is a separate reading, never an absence test.

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Created / Last updated after body and image, before references | 1 | Mounted `NoteShow` with distinct timestamps, an image and inbound references: visible labels, localized values and `time` values; geometry/order across rich/raw and read-only presentation. |
| Readable responsive dates and recency emphasis | 1 | Footer browser tests at 375px and desktop in light/dark; date text stays within its container, and recent/older timestamps produce different emphasis. Preserve newer/unchanged-content scenarios in `note_recent_update.feature`. |
| Direct scalar/list rows; valid-empty insertion; read-only permissions | 2 | Mounted `NoteTextContent`/page on initial arrival shows readable rows above body; empty editable note offers Add property, read-only respects editing refusal. Retain `RichMarkdownEditor.readOnlyProperties`, list and row editing proofs. |
| Draft/error recovery, add/edit/remove, body frontmatter preservation, rich/raw insertion | 2 | Reconcile mixed note editing tests around draft value, validation, corrected save and serialized content; retain row editing, validation, raw Markdown and property-memory-guard suites. |
| Existing property focus, rename/delete, missing-property feedback and note transitions | 2 | Mounted note/page and `RichMarkdownEditor.propertyLocation` scenarios assert focused row, explicit missing-key feedback, named-location continuity and next-note behavior. |
| Row learning actions and eligible activity presentation | 2 | `NotebookWorkspaceAssimilation` retains direct property arrival, panel close/open, identity/lifetime, Resume priority and next-unit journey; retain list-property memory tracking. |
| Context reading, links and images | 2 | `NoteContextReader`, property wiki-link, note wiki-link and `NoteShowPage.imageUpload` scenarios exercise actual consumers without a reveal fixture. |
| Quiet wrapping/stacking rows, accessible controls/touch targets and bounded body typography | 2 | Retain browser geometry in `RichMarkdownEditor.listPropertyLayout`, `propertyTouchTargets.touch`, row/link layout tests and `NoteTextContent.readingLayout`; remove only assertions belonging to the deleted header. |

## Ordered slices

### 1. Read timestamps at the bottom of the full note
Type: Behavior
Status: planned
Proof: Footer observations in the first two rows above, plus existing note/context
reading and recent-update scenarios with their date consumer aligned.

Behavior: Given a full note with body, optional image and references, opening it
or switching rich/raw representation keeps Created and Last updated below the
content and image, before references, with localized values and readable recency
emphasis.

- Move timestamp ownership to the full-note composition. Remove date-only slot
  forwarding through `NoteEditableContent` and `RichMarkdownEditor` and reconcile
  date tests around `NoteShow`. Move header freshness/date-only support with it.
- Keep the property disclosure functional until slice 2, with its property
  summary only. Reconcile mixed header/layout tests in this same slice so this
  is a green, useful stopping point.
- Add discriminating mounted footer coverage; preserve the existing content-save
  recency E2E behavior and adapt its page object only if its observable moves.
- Estimate 5–8 minutes including focused browser proof and local cleanup.
  The independent property visibility outcome belongs to slice 2.

### 2. Read and use properties directly throughout note workflows
Type: Behavior
Status: planned
Proof: Remaining proof-table rows, exercised through mounted note/page/workspace
boundaries and retained property-editing E2E consumers.

Behavior: Given an editable or read-only note, initial arrival directly presents
the appropriate property rows and controls above its body. Existing property
locations and learning actions focus the current property, and author edits keep
their drafts, validation feedback and saved values available.

- Delete `noteDetailsDisclosure.ts`, the header control/summary/unfinished-edit
  presentation, visibility state and exclusive styles. Remove dependent wiring
  from properties, insertion, focus and panel presentation. Keep the actual
  editor drafts/validation and row-learning state with their existing owners.
- Delete exclusively disclosure-focused tests. Re-home mixed tests from
  `NoteTextContent.detailsDisclosure`, `detailsEditing`, `dates` and
  `readingLayout` under surviving capability names; retain their useful body
  save, focus, missing-key, editing and layout observations. Delete the
  disclosure-touch test; retain actual property touch-target proof.
- Reconcile context-reader, page image-upload and workspace-assimilation tests.
  Delete `revealNoteDetails` and its imports/calls transitively. Existing E2E
  consumers use the existing `findNoteContentRegion` directly, including body
  focus, property layout/editing, wiki links, property locations, assimilation
  and relationship operations. Do not leave a compatibility wrapper.
- Preserve reading CSS and editor/row semantics. Regenerate component declarations
  with Vite if component membership changes; never manually repair generated files.
- Estimate 8–10 minutes including focused checks and cleanup, medium confidence.
  Scrutinized because the shared visibility owner has several consumers: they
  form one atomic removal and externally observable outcome. Splitting them
  would leave competing visibility semantics or broken consumers. Full required
  frontend/typecheck and runner startup waits may exceed this estimate; they are
  the focused-test/external-wait exception, not permission for extra implementation.
  At >5 minutes reassess; at >10 minutes of substantive work stop and refine the
  remaining boundary with the actual evidence under AGENTS.md's sizing rule.

## Verification and delivery

Use mounted browser proof as the local behavior boundary. During execution run
the complete frontend suite because the shared note surface and editor have
distributed consumers; then both required frontend typechecks on the same tree:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

The requirement comes from `.agents/skills/frontend/SKILL.md` (Frontend proof),
`frontend-testing` and `unit-testing`, not from treating every CI job as a local
gate. Use focused files while iterating, reusing unchanged accepted evidence.
When E2E support changes, run affected scenarios in the isolated runner, starting
with these existing features; add other affected helper consumers only when their
distinct observation is not already covered:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/note_recent_update.feature,e2e_test/features/note_topology/note_property.feature,e2e_test/features/note_topology/note_property_layout.feature
```

This is future execution proof, not a claimed preparation E2E result. The runner
owns services; preserve ADR 0007 isolation. No paid service or manual testing is
required. API generation is conditional on an actual contract change.

Each slice follows repository delivery: Jidoka, fresh independent
`dough-post-change-refactor` agent, applicable generation, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update, commit with check-only
hook and authorized push. Execution then owns retrospective/wrap-up and CI duties.

## One-time acceptance reading

At slice 2 acceptance, search the whole product (backend, frontend, CLI, MCP,
tests, docs, agent guidance and generated artifacts) for the retired symbols,
module path, header selectors/classes, summary/unfinished-edit strings and
date-only slot. Starting terms: `noteDetailsDisclosure`,
`useNoteDetailsDisclosure`, `note-details-toggle`, `note-property-summary`,
`note-details-unfinished-edit`, `revealNoteDetails`, `after-properties`,
`note-detail-dates`. Follow exclusively used support until no product references
remain. Retain `note-updated-at` if it remains the footer's live recency observable.
Record command, classified result and revision here as a reading, not a test.
Planning/history names are provenance, not product consumers; preserve dated
evidence honestly until ordinary story wrap-up. Add no permanent absence checker.

## Plan review

Slice-plan refinement was not needed: two independent user outcomes, one existing
property model, mapped proof and a measured bounded trial support the boundaries.
Slice 1's property disclosure is accepted interim behavior replaced by slice 2.
No remaining slice-specific concern was identified in this review. Final styling,
full consumer cleanup and proof are execution obligations, not completed work.
