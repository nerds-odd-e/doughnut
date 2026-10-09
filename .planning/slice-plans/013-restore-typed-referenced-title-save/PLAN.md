# Restore typed referenced-title save in the note page object

## Source

- Story: [Restore typed referenced-title save in the note page object](../../seeds/SEED-066-voice-input.md#restore-typed-referenced-title-save)
- Identity: SEED-066#restore-typed-referenced-title-save
- Correction of: SEED-066#rename-with-spoken-title
- Provenance: attributable commits `548a284a4410bf3bf1eb2cb121b7751d88d582ab`,
  `ca98162b615cd38552770bd615720d3a05aead31` (page-object extract + chaining),
  `10834bc742d7475acfb550385c1b3ba79fe1f42d`; CI failure on
  `ca98162b615cd38552770bd615720d3a05aead31`
  (https://github.com/nerds-odd-e/doughnut/actions/runs/38001567782).
- Preparation workspace: `/Users/terryyin/git/doughnut/.worktrees/change-an-existing-note-s-title-by-speaking`
- Branch: `cursor/change-an-existing-note-s-title-by-speaking`
- Authority: retrospective correction planning only. This plan does not Take,
  execute, commit to trunk, publish, or release.

## Goal and scope

Repair the note page object's typed referenced-title save so existing E2E
steps that type a title and choose Keep or Update visible reference text
succeed again. Spoken rename on the note page is already correct and is not
rewritten here.

Excluded: product UI changes, SpeakTitleControl changes, new spoken-title
scenarios, and a wholesale rewrite of every arrow method on the page object.

## Current findings (evidence)

On HEAD `10834bc742d7475acfb550385c1b3ba79fe1f42d`,
`e2e_test/start/pageObjects/notePage.ts` defines `chooseReferencedTitleSave`
and `saveReferencedNoteTitle` as arrow properties. `saveReferencedNoteTitle`
calls `this.chooseReferencedTitleSave(...)`. Arrow properties do not receive
the page object as `this`, so callers of
`I set the note title to … keeping/updating visible reference text` throw
`TypeError: Cannot read properties of undefined (reading 'chooseReferencedTitleSave')`.

CI on slice-2 SHA `ca98162b` failed four scenarios in
`note_topology/wiki_link.feature` and `note_topology/property_wiki_link.feature`
with that exact error. Focused slice-2 proof used only
`chooseReferencedTitleSave` (spoken Keep step) and did not re-run the typed
path after the chaining edit.

## Existing solutions and architectural constraints

- Reuse `chooseReferencedTitleSave` for the panel click, busy wait, and
  injected-title update.
- Fix chaining without a second copy of the panel steps: either call
  `assumeNotePage().chooseReferencedTitleSave(choice, newTitle)` from
  `saveReferencedNoteTitle`, or convert these two helpers to method shorthand
  so `this` is the page object (same pattern as `undo` on the same object).
- No product Vue/backend change. No ADR or North Star change.

Skills during execution: `e2e-authoring`, `unit-testing` only if a mounted
fixture is added (not expected).

## Outside-in proof

| Promise (example) | Owner | Observable proof |
| --- | --- | --- |
| Typed Keep / Update referenced-title rename works again (1) | 1 | E2E: at least `wiki_link.feature` scenarios that call `I set the note title to … keeping/updating visible reference text` pass; prefer the Keep and Update scenarios under Renaming a referenced note. |
| Spoken Keep step still works (Keep) | 1 | Existing `record_live_audio.feature` scenario "Rename a linked note by speaking the title, keeping visible reference text" stays green (uses `chooseReferencedTitleSave` directly). |

Focused commands:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/wiki_link.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature
```

## Decisive premises and observations

Observed on 2026-10-10 at revision `10834bc742d7475acfb550385c1b3ba79fe1f42d`.

| Premise | Consumed by | Literal observation and result |
| --- | --- | --- |
| `saveReferencedNoteTitle` calls `this.chooseReferencedTitleSave` from an arrow property | 1 | Read `notePage.ts` lines 144–167: both helpers are `=>` properties; typed steps in `note_editing.ts` call `.saveReferencedNoteTitle(...)`. True. |
| Existing wiki-link E2E uses the typed steps | 1 | `rg` on features: `wiki_link.feature`, `property_wiki_link.feature`, and `cli_notebook_web_note_renames.feature` use keeping/updating visible reference text steps. True. |
| CI failure matches this TypeError | 1 | `gh run view` / logs for run 38001567782 on `ca98162b`: four failures, message `Cannot read properties of undefined (reading 'chooseReferencedTitleSave')`. True. |
| Spoken Keep path does not use `saveReferencedNoteTitle` | Keep | `note_editing.ts` "I keep visible reference text for the title" calls `.chooseReferencedTitleSave` only. True. |

No paid or state-changing observation required; no probe slice.

## Current decisions and cumulative design

- One fix: restore a working call from typed save to the shared panel helper
  without arrow-`this`. Prefer the smallest edit that makes
  `saveReferencedNoteTitle` succeed.
- Do not expand into renaming every arrow on the page object in this correction.

## Ordered slices

### 1. Fix typed referenced-title save chaining
Type: Behavior
Status: done
Proof: Applied as owned CI repair on SHA
`07f888578ee96b7248e08ba202a5a252fe26ae8b` during
SEED-066#rename-with-spoken-title execution: both helpers converted to method
shorthand; `wiki_link.feature` 11 passing;
`record_live_audio.feature` 5 passing.

Behavior: an author types a new title on a linked note and chooses Keep or
Update visible reference text → the panel choice runs and the rename journey
completes as before the page-object regression.

## Execution complete

Product advice: Applied as CI repair on the same story-branch execution that
introduced the regression; no separate Take required.
