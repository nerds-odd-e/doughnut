# Keep the assimilation panel in place after actions that stay on the note

**Identity:** SEED-113#keep-the-assimilation-panel-in-place-after-staying-actions
**Source:** [correction story](../../seeds/SEED-113-note-assimilation-experience.md#keep-the-assimilation-panel-in-place-after-staying-actions)

## Correction input

**Provenance.** Found by the execution retrospective of
[SEED-113#assimilate-notes-with-a-clear-default-and-result](../../seeds/SEED-113-note-assimilation-experience.md#assimilate-notes-with-a-clear-default-and-result),
reviewing its published commits `9868d40b3c`, `356995da2d`, `52d49395dc`,
`e61af632d9` and `0d97720312` (net diff `8f91daebcd..0d97720312`). The reload
mechanism is older: `737287a333` (2026-08-12) added the reload-and-rekey after
optional additions; `ca68e7c554` (2026-08-24) later moved tracker state into
the shared note-level actions that `NoteShow` provides. That execution moved
the mechanism into `NoteShow` (slice 1), extended it to note-level
Understanding success (slice 2) and bound it to both placements (slice 5).

**Current findings (at `0d97720312`).**

- `AssimilationPanel.vue` `afterAction` emits `reloadNeeded` after a completed
  action that does not navigate: Understanding success, an optional-mode
  addition, and a confirmed skip that finds nothing next. `NoteShow.vue`
  `onAssimilationReloadNeeded` then calls `noteStore.loadNoteRealm` and bumps
  `assimilationPanelKey`, rebuilding the panel in both placements.
- The reloaded `NoteRealm` / `Note` (`packages/generated/donut-backend-api/types.gen.ts`)
  carries topology, content, references and notebook data only — nothing the
  panel shows. Tracker, skip and result state already refresh through
  `reloadNoteInfo()` in `useMemoryTrackerActionHandlers.ts` and the
  note-scoped `understandingAdded`, both provided by `NoteShow`.
- Effects: one extra note request per such action; a failed reload rejects
  inside an event handler with no handling (ADR 0006); in Read first, choosing
  an optional mode closes the popover and `keepFocusAtTrigger` returns focus to
  the Optional modes trigger, which the rebuild then destroys, so focus falls to
  the page (traced from code, not yet observed).
- `frontend/tests/layouts/NotebookWorkspaceUnderstandingResult.spec.ts`
  "keeps this visit's Understanding result on the note through an optional
  addition" asserts the old panel `isConnected === false`, which pins the
  rebuild rather than a learner-visible result.

**Outcome.** After those actions the open panel updates in place: no note
reload and no rebuilt panel; focus the panel returned to the Optional modes
trigger stays there.

**Preserved promises and constraints.** Every promise of the reviewed story
holds: the Understanding result survives optional additions and width changes
and resets when another note opens; tracker links and next recall dates
update; Next item, same-note property destinations, daily-goal feedback and
**No more notes to assimilate** (presentation dismissed, note kept) behave as
now; skip confirmation, Return to sequence and direct Understanding behave as
now; property rows keep their own advance; spelling verification still covers
the note. ADRs 0001, 0005 and 0006 apply unchanged.

## Premises and observations

| Premise and consuming operation | Form/profile and result | Unverified remainder |
| --- | --- | --- |
| The panel's visible state updates from the shared note-level actions without the realm reload or rebuild | 2026-11-09, worktree at `0d97720312`, disposable edit making `NoteShow.vue` `onAssimilationReloadNeeded` a no-op, then `./scripts/run.sh env -u NODE_ENV pnpm frontend:test tests/layouts/NotebookWorkspaceUnderstandingResult.spec.ts tests/layouts/NotebookWorkspaceCompactOptionalModes.spec.ts tests/layouts/NotebookWorkspaceReadFirst.spec.ts tests/layouts/NotebookWorkspaceSequenceSkip.spec.ts tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/components/recall/AssimilationPanel.spec.ts tests/components/recall/AssimilationPanel.trackers.spec.ts` (headless Chromium, production CSS, HTTP mocked): 39 passed; only the `isConnected === false` assertion failed. File restored; tree clean. | Focus staying on the Optional modes trigger after choosing a mode, and the persisted E2E journeys, need this slice's proof. |

## Outside-in proof ownership

| Promise | Owner | Observable proof at a stable boundary |
| --- | --- | --- |
| A stay-on-note action keeps the panel in place without reloading the note | 1 | Mounted workspace: after Understanding and after Add to sessions, the same panel element remains connected and no extra note (`showNote`) request is made; result and tracker status update. |
| Focus returned to Optional modes stays there | 1 | Mounted workspace in Read first: choose Add to sessions from the popover → focus is on the Optional modes trigger after the addition completes. |
| Reviewed story behavior is preserved | 1 | Existing `NotebookWorkspace*` assimilation specs, `AssimilationPanel*` specs and the E2E assimilation family stay green with the `isConnected === false` assertion replaced by the in-place observation above. |

## Ordered slices

### 1. Update the panel in place after actions that stay on the note
Type: Behavior
Status: planned
Proof: All three proof rows.

Behavior: Understanding succeeds, an optional mode is added, or a confirmed skip
finds nothing next → the panel's result, tracker status or dismissal shows
without a note reload or rebuilt panel → in Read first, focus that returned to
Optional modes stays on that trigger.

Remove the `reloadNeeded` path (`AssimilationPanel` `afterAction` emit,
`NoteShow` `onAssimilationReloadNeeded` and `assimilationPanelKey` in both
placements) and anything only it used; keep tracker refresh in the shared
handlers. Replace the `isConnected === false` assertion with the positive
in-place and no-extra-request observation; add the focus observation to the
Read first optional-mode addition test. Removal reading at acceptance: search
the product for `reloadNeeded`, `onAssimilationReloadNeeded` and
`assimilationPanelKey`.
Sizing: target 5 minutes; one component path and two spec edits, plus runner
wait for the focused E2E family.

## Verification and delivery

Use `./scripts/run.sh` with `NIX_CONFIG="extra-experimental-features = nix-command flakes"`
exported in non-interactive shells; frontend tests need `env -u NODE_ENV`;
local E2E needs a placeholder `OPENAI_API_TOKEN` (private mock). Run the
focused specs above, `pnpm -C frontend exec vue-tsc --noEmit`,
`pnpm -C frontend exec tsc --noEmit`, the full frontend suite once, and:

```bash
./scripts/run.sh pnpm cy:run --spec e2e_test/features/assimilation/assimilation_walkthrough.feature,e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature,e2e_test/features/assimilation/assimilation_page_types.feature
```

Inspect host load before the E2E batch. Trace consumers of the removed event
and key with the installed consumer-trace script. Apply the repository
execution wrap-up: Jidoka → fresh independent refactor → coordinator's single
`./scripts/run.sh pnpm format:changed` → update this plan → agent commit →
push.
