# Restore typed referenced-title save in the note page object

## Source

- Story: [Restore typed referenced-title save in the note page object](../../seeds/SEED-066-voice-input.md#restore-typed-referenced-title-save)
- Identity: SEED-066#restore-typed-referenced-title-save
- Correction of: SEED-066#rename-with-spoken-title (spent predecessor recoverable
  at `b4bb2f90a0d74a938dcadb98e8f279bbf09263e0:.planning/seeds/SEED-066-voice-input.md#rename-with-spoken-title`
  and `b4bb2f90a0d74a938dcadb98e8f279bbf09263e0:.planning/slice-plans/012-rename-with-spoken-title/PLAN.md`)
- Provenance: attributable commits `548a284a4410bf3bf1eb2cb121b7751d88d582ab`,
  `ca98162b615cd38552770bd615720d3a05aead31` (page-object extract + chaining),
  `10834bc742d7475acfb550385c1b3ba79fe1f42d`; CI failure on
  `ca98162b615cd38552770bd615720d3a05aead31`
  (https://github.com/nerds-odd-e/doughnut/actions/runs/38001567782).
  Repair commit `07f888578ee96b7248e08ba202a5a252fe26ae8b` already on the
  predecessor branch; verify after Take.
- Preparation workspace: `/Users/terryyin/git/doughnut/.worktrees/change-an-existing-note-s-title-by-speaking`
- Branch: `cursor/change-an-existing-note-s-title-by-speaking`
- Authority: queued follow-up for later Take. This plan does not execute here.

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
Proof: `wiki_link.feature` Keep and Update referenced-title scenarios green;
`record_live_audio.feature` linked spoken Keep scenario still green.
Predecessor CI repair `07f888578ee96b7248e08ba202a5a252fe26ae8b` may already
contain the method-shorthand fix — confirm at Take before rewriting.

Behavior: an author types a new title on a linked note and chooses Keep or
Update visible reference text → the panel choice runs and the rename journey
completes as before the page-object regression.

Accepted proof (2026-10-10, revision `5ee74ec76ed2b51ff24c8a1f9bf04a2054f67d15`):
explained empty change. Predecessor repair
`07f888578ee96b7248e08ba202a5a252fe26ae8b` is an ancestor of this branch;
`notePage.ts` already defines `chooseReferencedTitleSave` and
`saveReferencedNoteTitle` as method shorthand, so `this` is the page object.
No product or test code changed in this execution, so no refactor pass ran.
Every consumer of the typed and spoken steps passed:

```sh
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_topology/wiki_link.feature,e2e_test/features/note_topology/property_wiki_link.feature,e2e_test/features/note_creation_and_update/record_live_audio.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_note_renames.feature
```

- `wiki_link.feature` 11/11, including "Renaming a referenced note while
  keeping visible reference text" and "… while updating visible reference text".
- `property_wiki_link.feature` 11/11.
- `record_live_audio.feature` 5/5, including "Rename a linked note by speaking
  the title, keeping visible reference text".
- `cli_notebook_web_note_renames.feature` 1/1.

## Execution resume context

- Mode: story-branch; workspace
  `/Users/terryyin/git/doughnut/.worktrees/restore-typed-referenced-title-save-in-the-note`;
  branch `claude/restore-typed-referenced-title-save-in-the-note`; remote
  `origin`; target `main`.
- Published claim: `5ee74ec76ed2b51ff24c8a1f9bf04a2054f67d15` on `main`.
- Plan evidence: `f8f718da651d033034ed4b420e4077634949ef53` accepted on
  `refs/heads/claude/restore-typed-referenced-title-save-in-the-note`.

## Execution complete

Product advice: no change. The typed Keep and Update journeys work on this
branch through the predecessor's repair; no product behavior or backlog
priority is affected. The broader arrow-function cleanup of the note page
object stays deferred as the story states.
