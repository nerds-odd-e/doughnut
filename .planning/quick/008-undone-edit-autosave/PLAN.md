# Keep remote note content aligned when an in-flight edit is undone

Work item: **SEED-036#story-1**.
Source: [refined bug story](../../seeds/SEED-036-keep-undone-edits-consistent.md#story-1).
Status: in progress; slice 1 delivered, slice 2 not started.

## Execution

- Mode: Story Branch.
- Originating checkout: `/Users/terryyin/git/doughnut` (`main`).
- Integration checkout: `/Users/terryyin/git/doughnut` (`main`).
- Execution checkout: `/Users/terryyin/git/doughnut/.worktrees/008-undone-edit-autosave`, branch `008-undone-edit-autosave`, created this session from `19244e701cab42ed45060fcbf57559d0d09acc4d`.
- Claim published: `dc29e4525a0fdb9b68a8afd94a820553850fc088` on `refs/heads/main`. Claim CI: unobserved (story-branch observer does not cover trunk).
- Increment target: `refs/heads/008-undone-edit-autosave`.
- Default-checkout refresh after the trunk claim: deferred (unclear ownership of the open integration checkout). Its HEAD remains `19244e701cab42ed45060fcbf57559d0d09acc4d`.
- Replanning: existing planning authority retained (refine this plan if active slice work exceeds 10 minutes, except the named verification-wait exception).
- CI: GitHub Actions workflow `ci.yml`, display name `donut CI`. Observer directory `/tmp/dough-ci-501/watch-zDxIaG` for `nerds-odd-e/doughnut` branch `008-undone-edit-autosave`.

## Goal and scope

After an author edits and restores note content, successful autosave settles on
the latest draft, even when it equals the value preceding an outstanding save.
Keep local editing responsive and preserve established normalization, server
canonicalization, save guards, and completion/error behavior.

The pending-debounce case and the in-flight case are two boundaries of that
same rule. Empty content is valid. No multi-writer conflict protocol, offline
queue, automatic retry policy, toolbar undo redesign, navigation durability
expansion, backend optimization, or new UI is included. The user requested
preparation only; the backlog item stays queued.

## Evidence and existing solution

Inspected revision: `4f0b551959c8a5b9bbc4625fe0f13f3502a00a07`.
This is code evidence, not a claim that a runtime regression has been reproduced.

- `frontend/src/composables/useDebouncedTextAutosave.ts` already owns local text,
  acknowledged text, debounce, serialized persistence, proposal versions,
  response synchronization, and flush/cancel. `propose` returns when the draft
  equals `lastSavedValue` before advancing the proposal or replacing pending
  work. Thus a restore can leave an obsolete timer/request current. Completion
  can then advance saved-version bookkeeping without recording the real
  acknowledged text. The empty-value exception in `syncFromExternal` also needs
  assessment against the same response-ownership rule.
- `TextContentWrapper.vue` connects that owner to `StoredApiCollection` and the
  content mutation barrier. `updateTextField` skips unchanged stored values,
  records undo history, sends the API mutation, then refreshes the store. Keep
  that domain responsibility; a correcting save must run after the earlier
  response has updated the stored value. Tests must exercise this real path.
- `NoteEditableContent.vue` supplies Markdown and rich editor input plus the
  property-memory guard. Neither editor should own a second save queue.
- Other actual consumers are `ScopedReadmeEditor.vue` (notebook/folder Readme),
  `AutosavingPageNameEditor.vue` (trimmed names, cancellation for invalid blank
  input), and title editing through `TextContentWrapper` (including explicit
  referenced-title save/discard and `markSaved`). Preserve these contracts.
- `noteContentMutationBarrier.ts` owns closing mutation admission and awaiting
  saves before trashing. It delegates persistence; it is not another autosave
  engine. `NoteEditingHistory` owns user history, not network completion.
- Product-wide searches across frontend, CLI, MCP, and backend found this
  frontend composable as the existing owner of the relevant draft/debounce
  lifecycle. Backend Git publication and CLI synchronization have different
  responsibilities and do not observe the unsent local draft.

PFE decision: **change the existing autosave owner**. Reuse its serial execution
and callers. Add no note-only undo handler, parallel queue, or generic sync
framework. Remove redundant state only where current invariants justify it.

## Design and current decisions

Use one coherent model: **current draft**, **acknowledged persisted value**, and
**outstanding save work**. Proposal identity identifies which draft a response
belongs to; it does not itself prove remote equality. Derive dirty/completion
meaning from this model instead of maintaining competing saved flags.

1. Every meaningful draft change supersedes older unsent proposals, including a
   return to an earlier acknowledged value. Evaluate whether a write is needed
   against the acknowledged value when it can actually execute.
2. Serialize writes through the existing owner. A successful older write is
   still evidence of what the server accepted, even when it no longer matches
   the draft. Preserve the newer draft and arrange its required follow-up.
3. A save response must not overwrite a newer draft. Continue to accept server
   canonical content when there is no newer draft; request text and canonical
   response text need not be byte-identical. Keep this distinction in the
   existing response/persistence contract if a small contract adjustment is
   required, rather than adding content-shape recognizers in individual editors.
4. `flushAndWait` must await the restore required by the flushed draft before
   reporting success. Cancellation still suppresses unsent work; it cannot undo
   a request already accepted by the server. Preserve existing guard rejection
   and error outcomes without adding automatic retries.

Follow [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) for domain
meaning, [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
for the existing server publication authority (no new revision protocol), and
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) for deliberate
error handling. No ADR exception is needed. The existing North Star's accepted
change boundary remains intact; this local repair warrants no new direction topic.

## Outside-in proof ownership

Use mounted components/pages with real composable, store, and editor code;
mock only SDK HTTP responses with `mockSdkService` or
`mockSdkServiceWithImplementation`, `makeMe` payloads, deferred response gates,
and fake debounce timers. Do not mock persistence decisions or manually supply
the final corrected state. Simulate server state by applying each received
request when its gate resolves, then return its response through the real store.

| Promise | Owner | Observation |
| --- | --- | --- |
| Restore before dispatch sends no obsolete edit | Slice 1 | Input `A → AB → A`; drain debounce/blur; no content request and DOM remains `A` |
| Restore during a write reaches the server | Slice 2 | Hold `AB`, edit to `A`, release; observe correcting request, simulated server state and DOM both `A`, clean only after completion |
| Old response cannot overwrite latest intent | Slice 2 | Real reactive store feedback while the first response resolves; latest input remains visible; empty restoration and continued edit use the same rule |
| Canonical response remains accepted | Slice 2 | Existing wrapped ordinary-note response test plus a canonical old response while a newer draft exists |
| Awaited save includes correction; errors do not permit trash | Slice 2 | Page-level mutation order: first save, correcting save, then trash; existing failed-save case still prevents trash |
| Shared consumers retain their contracts | Both | Existing title, Readme, page-name, normalization, navigation, guard, and explicit-title tests below remain green |

Current evidence to extend/preserve:

- `frontend/tests/notes/NoteEditableContent.debouncedSave.spec.ts`: debounce,
  immediate wiki-link flush, and wrapped ordinary-note canonical response.
- `frontend/tests/notes/NoteEditableContent.spec.ts`: navigation and preserving
  a newer draft when a first response arrives. Its current case observes the
  draft only; it does not prove the final correcting request or remote value.
- `frontend/tests/pages/NoteShowPage.autosaveTrash.spec.ts`: real page/store
  feedback, waiting before trash, reopening after failure. Extend here for the
  correcting-save completion promise instead of testing a private queue.
- `frontend/tests/notes/NoteTextContent.titleEdit.saveRace.spec.ts`,
  `TextContentWrapper.spec.ts`, `NoteEditableContent.memoryTracker.spec.ts`, and
  `NoteEditableContent.htmlNormalization.spec.ts`: shared lifecycle contracts.
- `frontend/tests/pages/NotebookPage.spec.ts`,
  `NotebookPageView.settings.spec.ts`, and `FolderPage.renameDissolve.spec.ts`:
  Readme autosave, trimmed names, blank-name rejection and pending cancellation.

## Ordered slices

### 1. Restoring a draft discards its unsent edit

Type: Behavior
Status: done
Sizing hypothesis: about 5 minutes including focused proof and cleanup.

Accepted proof: mounted `NoteEditableContent` restore `A → AB → A` before debounce does not call `updateNoteContent` and the textarea stays `A` (`does not send obsolete edit when draft is restored before debounce fires` in `frontend/tests/notes/NoteEditableContent.debouncedSave.spec.ts`; command `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.debouncedSave.spec.ts`, 6 passed). Shared caller files named below also passed (30 tests) on the same implementation. Learning: equality with the acknowledged value must cancel the pending debounce and realign proposal version with `savedVersion`; returning early left the unsent edit armed. In-flight correction remains slice 2.

Saved `A` → draft `AB` → restore `A` before dispatch → obsolete `AB` never
reaches the API and the editor stays at `A`.

Write the failing regression at the mounted note editor boundary first. Change
proposal invalidation/debounce handling in the shared owner so returning to the
acknowledged value supersedes unsent work. Preserve the existing debounce and
immediate-flush behavior. Reuse the note test support; do not introduce a
composable-only harness or a separate undo-specific path.

Proof: new pending-restore case and existing debounce/canonical tests in
`NoteEditableContent.debouncedSave.spec.ts`, plus the shared caller checks at
delivery. Safe stopping point: undone unsent edits no longer leak to the server;
the in-flight case remains explicitly unfinished for slice 2.

### 2. An acknowledged older edit is followed by the latest draft

Type: Behavior
Status: planned
Sizing hypothesis: 5–10 minutes for the focused implementation/proof loop;
scrutinized above the 5-minute target because response ownership and completion
must change coherently. Slow required full-suite/typecheck time is an explicit
verification wait exception, not permission for an unbounded implementation.

Saved `A` → send and hold `AB` → restore `A` → acknowledge `AB` → save `A`
serially and settle with the latest draft in the DOM and on the server.

Add deterministic red proof before repair. Complete the common model for
acknowledgements, newer draft preservation, and correcting persistence. Include
the empty draft, a subsequent ordinary edit, and canonical-response variants as
data variations, with focused assertions for their unique deltas. Exercise
reactive response feedback; final API arguments alone are insufficient.

Extend the existing page-level trash scenario to prove that its awaited save
includes the correction. Keep queue ownership inside the autosave composable;
the barrier continues to delegate. Reuse the existing failed-save scenario to
verify that failure prevents trash. Keep content guard/validation and explicit
title cancellation contracts intact.

Proof: new editor/page scenarios and existing shared-consumer tests mapped
above, followed by the required frontend suite and typecheck. Safe stopping
point: the reported successful-save interaction converges without another user
action and all named shared consumers retain their existing behavior.

## Commands and delivery gates

Run from the execution checkout after its normal dependency setup:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteEditableContent.debouncedSave.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NoteShowPage.autosaveTrash.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

Use focused commands for red/green iteration and the full suite for shared-owner
acceptance. Reuse green evidence only for identical content. Runtime tests have
not been run during this planning session; neither slice is done.

Authorized execution uses `dough-execute-plan`: Jidoka, fresh independent
`dough-post-change-refactor` agent, API generation only if signatures actually
change, coordinator `./scripts/run.sh pnpm format:changed` once, plan update,
commit with check-only lint hook, then push and asynchronous CI repair. No
backend contract change is expected. Do not add a standalone lint/format pass
for implementers or a manual-testing requirement.

At more than 10 minutes of active slice work, stop and refine the same plan
unless the named verification wait exception applies. Preserve attempt-owned
evidence safely. If response ownership proves to require a broader product
decision, stop that path and refine the story rather than growing this repair.

## Cumulative design review

Both slices implement one rule: a newer draft supersedes older intent; an
accepted write updates the acknowledged baseline, and outstanding work brings
that baseline to the draft. The first boundary provides independently useful
correction before dispatch; the second handles an irreversible dispatched write.
Neither needs a separate architecture layer or per-editor state machine.

No remaining slice-specific blocking concern was identified in this review.
The bounded timing, canonical-response, and shared-caller risks have explicit
proof owners. An additional slice-plan-refinement pass is unnecessary unless
execution supplies contrary evidence. Readiness is recorded in the canonical
story by the preparation recorder; it does not authorize execution.
