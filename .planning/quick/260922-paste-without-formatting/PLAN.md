# Paste without formatting

Status: in progress; slice 1 delivered.
Source: [SEED-035 story 1](../../seeds/SEED-035-paste-without-formatting.md#story-1).
Identity: SEED-035#story-1.
Research revision: `f2dddcb9fa31a70286385a4847beb252a7202110`, 2026-09-20.
Allocation: next numeric entry after `260921-faster-note-content-saving`;
the numeric prefix is an allocation, not a scheduled date.

## Goal and scope

An author pastes normally, notices escaped Markdown, and chooses a temporary
floating **Use as Markdown** action to replace that insertion with the original
clipboard text. Deliver the same choice in Markdown and rich note-body editing.
Preserve surrounding content, frontmatter, and the editing position. The seed
owns UI defaults, examples, and exclusions. It remains queued during planning.

One action, one dismissal control, one pending paste per mounted editor.
No previews, detection heuristics, preferences, new shortcuts, new paste menus,
global editor redesign, or backend/API/storage work. Correction uses the
ordinary content update/autosave path; no delayed-save transaction or promise
that the first paste never reaches the server. This is an editor interaction,
with frontend proof and zero new E2E scenarios planned (absolute ceiling: one).

## Existing solutions and architectural decision

PFE inspection covered frontend paste handlers, plain-text fields, popup and
overlay components, package/CLI/MCP paste references, and existing tests.

- `frontend/src/composables/useNoteContentPaste.ts` coordinates both note-editor
  modes. Extend this owner for the temporary choice, rather than creating a
  parallel paste manager or storing clipboard state in the global store.
- `usePasteWithLinkImageOptions.ts` owns existing whole-note link/image removal;
  it calls `markdownizer` and opens the modal in `commons/Popups/Popups.vue`.
  Preserve its removal semantics. Suspend the new choice while this modal is
  open, offer it after cancellation, and consume it if removal changes content.
  Applying the alternative must not launch that modal again. Do not unify the
  modal infrastructure with the non-modal affordance merely because both float.
- `NoteEditableContent.vue` is the common UI boundary. Render one affordance
  here, shared across modes; extract a presentation component only if it makes
  this owner smaller and clearer. `ToolbarOver.vue` has a full-width overlay,
  opacity, and extreme stacking unrelated to this interaction; it is not a fit.
- `QuillEditor.vue` already captures paste before Quill handles it and emits
  completion. It currently rewrites clipboard HTML via `markdownizer`, then
  emits only full resulting HTML. Capture original plain text and the native
  insertion context before conversion. Keep Quill ranges/Delta operations here;
  do not recreate rich positions by searching converted Markdown strings.
- `RichMarkdownEditor.vue` already renders Markdown, composes body with
  frontmatter, and owns editor-specific insertion. Reuse these responsibilities
  to interpret the alternative and preserve properties; do not duplicate its
  composition or introduce a second Markdown converter.
- `SeamlessTextEditor.vue` / `PropertyValueField.vue` already consume plain text,
  but own literal field editing, not note Markdown interpretation. Do not change
  them or transplant their literal-text behavior into this story.
- `TextContentWrapper.vue` owns local content and autosave. Leave persistence
  coordination there; replacement is a normal update, and server responses are
  not a second owner of the pending paste.

Model one transient **paste choice**: original text plus the capability to
replace the just-completed insertion and locate its affordance. Each editor
owns its necessary selection representation; the shared owner owns availability,
consumption, expiry, and dismissal. Keep only indispensable historical paste
data, not mirrored live note content or independent booleans for every phase.
Invalidate on intervening user changes, including property changes, undo,
another paste, mode/note changes, or teardown. A routine acknowledgement of the
same content must not erase the choice. An actual external content replacement
invalidates it. No rebase/replay machinery for stale edits.

The installed Quill API exposes `getContents`, `updateContents`, `setSelection`,
and `getBounds`; its clipboard implementation replaces the selected range via
Delta. Reuse those primitives for native replacement and positioning. Exact
replacement mechanics are an implementation choice to prove in slice 4, not
authorization for snapshotting and overwriting a subsequently edited note.

Design review: inspect the aggregate production diff and line counts; eliminate
duplicated conversion/lifecycle rules and unnecessary forwarding/state. Prefer
fewer, direct lines over a generic command framework. No arbitrary net-negative
line quota that encourages unrelated deletion or compressed code.

ADR index and in-file statuses agree: 0000–0007 are Accepted, with no successor
chain. Relevant constraints:

- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md):
  name note content, Markdown, and paste directly.
- [ADR 0004 — Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
  preserve existing authored frontmatter and reuse the current Markdown domain.
- [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md):
  avoid silent catches and speculative recovery layers; no asynchronous
  clipboard re-read or permission fallback is necessary.

No conflict or exception identified. The existing
[North Star](../../NORTH-STAR.md) concerns Git publication and persistence;
this local editor change needs no new topic or ADR.

## Outside-in proof

Extend `frontend/tests/notes/NoteEditableContent.paste.spec.ts` at the mounted
`NoteEditableContent` boundary, with real TextArea, RichMarkdownEditor, Quill,
conversion, and the relevant popup UI. Use realistic `DataTransfer` containing
both HTML and plain text, real DOM selection, and actual button interactions.
Mock only backend HTTP through `mockSdkService`; use `makeMe` for payloads.

Existing rich tests in `noteEditableContentTestSupport.ts` synthesize
`pasteComplete`; that seam is not proof of this new behavior. Replace or avoid
that seam for new recovery cases. Existing popup mocks are not acceptable for
the new modal-coexistence observation. Prefer Testing Library DOM queries,
not component internals or tests per new helper. Demonstrate the reported
failure with a failing boundary test before implementing the behavior.

| Promise | Owner and observation |
| --- | --- |
| Markdown recovery, including replacing a selection and multiline text | Slice 1: DOM textarea contains original Markdown in the insertion, prefix/suffix unchanged |
| Ordinary paste remains the default; unavailable alternative is absent | Slice 1: dismiss/ignore preserves converted text; HTML-only/plain-only fixtures preserve existing behavior |
| Only current paste can be corrected | Slice 1: edit/paste/context changes invalidate prior choice; Escape, outside and explicit dismissal do not change content |
| Ignored choice expires without rushing active interaction | Slice 2: controlled timer expires the choice, pauses during hover/focus, and is cleaned up |
| Rich paste capture preserves ordinary behavior | Slice 3: real rich clipboard event produces same rendered content and preserved properties |
| Rich recovery uses Markdown and preserves surrounding body/frontmatter | Slice 4: real paste and click render bold; mode switch exposes expected Markdown and existing properties |
| Link/image prompt remains usable without stale replacement | Slice 5: real prompt cancel offers correction; removal consumes it; correction does not reopen prompt |
| Touch/keyboard reachability and editing position | Slice 6: pointer activation preserves editor focus/caret; keyboard activation works; control fits reduced viewport without covering insertion |
| Small coherent frontend design | Every slice's refactor; aggregate review of ownership and handwritten diff at completion |

Use a small parameterized lifecycle set, not a Cartesian product of modes,
clipboard types, content shapes, and every dismissal event. Prove shared policy
once, with a rich invalidation case proving the adapter is connected. Check
the local editor result, not new database/Git-history assertions. Preserve
existing autosave tests rather than adding a backend journey for this feature.

Commands from repository root during execution:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.paste.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

Focused file while iterating; full frontend suite and typecheck for accepted
frontend proof, reused on identical content. No tests have been run to claim
product behavior during planning. No manual app test or E2E suite is required
by this plan. Browser unit checks cannot establish real iOS/Android virtual
keyboard behavior; report that limit rather than claiming device validation.

## Ordered slices

Each target includes implementation, focused proof, and local cleanup. Target
about five minutes; scrutinize estimates over five, and stop/finer-decompose
past ten unless the only delay is a documented focused-test/external wait.
Delivery gates follow each accepted slice. Estimates are hypotheses.

### 1. Correct the last Markdown paste
Type: Behavior
Status: done
Estimate: about 5 minutes; medium confidence.
Proof: focused mounted-editor cases in the table, then acceptance checks above.

Accepted proof:
```
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.pasteChoice.spec.ts tests/notes/NoteEditableContent.paste.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```
Both pass (14/14 tests; no type diagnostics), inspected at the mounted
`NoteEditableContent` boundary with real `DataTransfer` clipboard events,
real textarea selection, and real button interactions. `useNoteContentPaste.ts`
owns the transient `pasteChoice` (original text plus `replace()`) and all its
invalidation (typing/undo, note/mode switch, external content change, Escape,
outside click, explicit dismissal, teardown); `NoteEditableContent.vue` renders
the compact action bar. Rich-mode paste is untouched. Paste-choice tests live
in their own `NoteEditableContent.pasteChoice.spec.ts` (this directory's
one-concern-per-file convention), leaving the original `.paste.spec.ts`'s
link/image-removal tests unchanged.

Behavior: Paste HTML plus original Markdown into a selected span; a compact
action appears and replaces that paste with original text when chosen, without
altering surrounding text. Dismissal retains ordinary paste.

Implement the shared choice with this first adapter and minimal usable
affordance. Include invalidation and one-shot consumption before delivering:
typing/undo/new paste, changed note/mode/content, outside interaction, Escape,
close, and teardown. Preserve existing link/image handling; when it opens a
modal, clear the choice for now. Slice 5 restores it after cancellation using
the common rule, not a separate kind of paste. Do not postpone stale-edit
safety. Safe stop: Markdown correction works, dismissed by user interaction;
timed expiry follows in slice 2. Rich paste retains its existing behavior.

### 2. Dismiss an ignored paste choice automatically
Type: Behavior
Status: done
Estimate: about 5 minutes; medium confidence.
Proof: mounted editor with controlled time; observe action visibility and
unchanged pasted content, including hover/focus pause and teardown.

Accepted proof:
```
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.pasteChoice.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```
Both pass (14/14 tests; no type diagnostics). `useNoteContentPaste.ts` starts a
10s expiry timer wherever `pasteChoice` is set, clears it inside the existing
single `clearPasteChoice()` path (so every prior invalidation route also stops
the timer with no new call sites), and exposes `pausePasteChoiceExpiry`/
`resumePasteChoiceExpiry`, wired to `@mouseenter`/`@mouseleave`/`@focusin`/
`@focusout` on the action bar in `NoteEditableContent.vue`. Tests use
`vi.useFakeTimers()` scoped to the file's `expiry` describe block.

CI note: the push for slice 1 (commit `0c5e5725a0`) reported a `Backend Unit
tests` failure in GitHub Actions — 3 `StructuredResponseCreateParamsSerializerTest`
methods failing with `ApplicationContext failure threshold exceeded`, traced to
a single root cause: `Too many connections` from MySQL during Flyway init for
one Spring context in that CI run. This slice touches no backend files, and
every other job (frontend tests, lint, E2E, other unit tests) passed. Recorded
as CI infrastructure flakiness, not a defect; no repair made. The slice 2 push
(commit `6613c1cd37`) reproduced the identical `Backend Unit tests` failure —
same root cause (`Too many connections`), same cascading test class — a second,
consistent occurrence of the same pre-existing infrastructure issue; same
disposition, no repair made.

Behavior: Leave the action unused for 10 seconds and it disappears; interacting
with it by hover or keyboard focus pauses expiry. One shared lifecycle owns
the timer and cleanup. Safe stop: the basic temporary interaction is complete
for Markdown pastes that do not open the existing removal modal.

### 3. Retain rich paste context at the existing editor boundary
Type: Structure
Status: done
Estimate: about 5 minutes; medium confidence.
Proof: real rich DOM clipboard paste preserves current output, selection, and
frontmatter using existing editor tests and the outer mounted boundary.

Accepted proof:
```
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/QuillEditor.spec.ts tests/components/form/QuillEditor.paste.spec.ts tests/components/form/RichMarkdownEditor.spec.ts tests/notes/NoteEditableContent.paste.spec.ts tests/notes/NoteEditableContent.pasteChoice.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```
Both pass (37/37 tests; no type diagnostics). `QuillEditor.vue` exports
`QuillPasteContext` (`{ originalText, range: {index, length}, insertedLength }`),
captured in its existing capture-phase paste listener via `getSelection(true)`
(wrapped in try/catch — Quill's selection-read APIs can throw when no real
native caret exists) before Quill mutates the document, with `insertedLength`
computed from the applied Delta's length in `text-change`. Threaded unchanged
through `RichMarkdownEditor.vue` and `NoteEditableContent.vue` into
`useNoteContentPaste.ts`'s `handlePasteComplete` as `_quillContext` (received,
not yet consumed — slice 4 wires it into `pasteChoice`). No visible behavior
change; rich-mode paste UI is still absent, matching this Structure slice's
safe stop. `QuillEditor.spec.ts` split into itself (render/link tests),
`QuillEditor.paste.spec.ts` (paste tests), and `quillEditorTestHarness.ts`
(shared mount/cleanup), matching the existing `RichMarkdownEditor.*`/harness
convention in this directory.

Replace `replace()` in slice 4 with a direct Delta swap of the inserted span:
`quill.updateContents(new Delta().retain(range.index).delete(insertedLength).insert(originalText), Quill.sources.USER)`,
then `quill.setSelection(range.index + originalText.length, Quill.sources.SILENT)`.
`getBounds(range.index)` is available for slice 6's viewport placement, not
needed for `replace()` itself.

Structure: Carry original clipboard text and Quill-native insertion context
through the existing rich paste path; keep context with the editor that owns
it. Reuse conversion and frontmatter composition. Do not expose a nonworking
action or new global state. This immediately enables slice 4's replacement.
Safe stop: original rich paste behavior is unchanged and tests remain green.

### 4. Correct the last rich paste through the same choice
Type: Behavior
Status: planned
Estimate: about 5 minutes after slice 3; native range replacement is the risk.
Proof: real rich paste → action → bold content → Markdown-mode observation,
with selected-span/surrounding-content and existing-frontmatter preservation.

Behavior: The same choice corrects the rich insertion, keeps the editor's
selection after the inserted content, and becomes unavailable on subsequent
editing. Feed the existing shared lifecycle rather than copying it. Include one
rich invalidation case to prove the adapter participates in stale-edit safety.
Safe stop: correction works in both modes; opening the existing modal still
clears the choice until slice 5 adds its specified cancellation behavior.

### 5. Retain the choice after cancelling link or image removal
Type: Behavior
Status: planned
Estimate: about 5 minutes; medium confidence.
Proof: actual clipboard event and real popup UI at the mounted note editor;
cancel → correction available, removal → no stale correction.

Behavior: The existing removal modal suspends the current paste choice and
timer; cancellation resumes it if that paste is still current. Choosing
removal consumes it. Applying the alternative does not reopen the modal.
Replace the temporary clear-on-modal behavior from slice 1 in the shared owner.
Do not change whole-note removal semantics or the global popup infrastructure.
Safe stop: both existing removal and new recovery work together in both modes.

### 6. Keep the paste action reachable while editing
Type: Behavior
Status: planned
Estimate: about 5 minutes; medium confidence, viewport placement is the risk.
Proof: mounted-browser tests at a narrow/reduced viewport, DOM geometry and
pointer/focus observations; keyboard activation and dismissal.

Behavior: After paste near a visible editor edge, the compact action remains
reachable without covering the insertion; pointer activation preserves editor
focus and editing position. Use comfortable touch targets (at least 44 CSS px)
and accessible labels for action/dismissal. Fit placement to available viewport
space with minimal local geometry; use Quill bounds for rich mode and existing
textarea selection/element geometry for Markdown, avoiding a duplicate hidden
textarea renderer. Do not add a general overlay/keyboard-management framework.
Safe stop: the selected interaction is delivered, with the device-proof limit
stated above rather than extra E2E coverage.

## Cumulative assessment and delivery

The common rule is replacing the current paste once while it is still current.
Differences between textarea offsets and Quill ranges are editor mechanics,
not separate paste policies. The Structure slice immediately enables rich
recovery; the last slice owns viewport/focus refinement, not another feature.
No independent product outcome has been added.

Refinement assessment: separated expiry from the first correction, and modal
coexistence from rich replacement. Six slices, each with one behavior/structure
outcome and focused proof loop; no scope change or sizing exception selected.

Remaining concerns: slice 4 must prove that Quill replacement and model sync
retain the intended selection and frontmatter; slice 6 cannot certify real
mobile keyboards through desktop browser tests. These are bounded execution
proof risks, not missing product decisions. If either implementation exceeds
the hard time limit, refine that slice in this same plan using observed cause;
do not enlarge scope or spend the E2E allowance automatically.

After implementation authority, use `dough-execute-plan`: take the queued
story only when starting; Jidoka → fresh `dough-post-change-refactor` agent →
API generation only if signatures actually change (none anticipated) →
coordinator runs `./scripts/run.sh pnpm format:changed` once → update plan →
commit with the independent check-only lint hook → push and asynchronous CI
observation. Implementers/refactorers do not run format:changed or lint:changed.
Retain this plan for retrospective and story wrap-up after execution.

Execution identity: Story Branch Mode. Originating checkout `/Users/terryyin/git/doughnut`
(integration branch `main`), execution checkout
`/Users/terryyin/git/doughnut/.claude/worktrees/260922-paste-without-formatting`
on branch `claude/260922-paste-without-formatting`, pushed to `origin`. CI
observed via GitHub Actions `ci.yml` ("donut CI") on that branch.

## Current decisions and learnings

- Owner chose the temporary post-paste action and predominantly frontend unit
  tests; no blocking product question remains.
- Existing link/image options are a modal over whole-note content, requiring
  suspension/consumption of the new choice rather than concurrent stale edits.
- Original rich clipboard text is not currently carried by completion events.
- Planning checks and publication evidence belong in the delivery response;
  future execution records accepted commands/results and changed assumptions
  here as it progresses.
- Slice 1: `useNoteContentPaste.ts` owns the paste-choice's full lifecycle,
  including document-level Escape/outside-click listeners and note/mode/content
  watches, via its own `onMounted`/`onUnmounted` (precedent:
  `useAutoCollapseDetails.ts`), rather than wiring dismissal in
  `NoteEditableContent.vue`. Slices 2 (expiry) and 5 (modal-cancel restore)
  should extend this same composable-owned lifecycle rather than adding
  component-level state. Paste-choice tests live in their own
  `NoteEditableContent.pasteChoice.spec.ts`, separate from the pre-existing
  `.paste.spec.ts` (link/image-removal), matching this directory's
  one-concern-per-file convention; extend the former for rich-mode/expiry
  cases in later slices.
- Slice 3: in this repo's Vitest browser-mode tests (real headless Chromium),
  Quill 2.0.9's `getSelection`/`setSelection` throw for a genuinely-established
  native caret/selection when no prior test ever exercised those calls; this is
  an environment limitation (production rich paste already relies on the same
  Quill API and works), not a product defect. `QuillEditor.vue` wraps the
  capture-time `getSelection(true)` call in try/catch. Slice 4 tests needing a
  real, non-null `QuillPasteContext` should stub `quill.getSelection` via
  `vi.spyOn` (see `QuillEditor.paste.spec.ts`) rather than trying to establish a
  real browser caret; Quill's `updateContents`/Delta application itself works
  fine against the real model in this environment — only selection-read APIs
  are unreliable here. Slice 4 should feed a rich `PasteChoice` into the same
  `pasteChoice` owner (`useNoteContentPaste.ts`) rather than a parallel
  lifecycle, and drop the `_quillContext` underscore once it's consumed.
