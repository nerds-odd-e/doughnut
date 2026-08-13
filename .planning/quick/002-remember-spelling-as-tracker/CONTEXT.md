# Remember spelling as a memory tracker — context

Ad-hoc plan. Not on the GSD roadmap. Progress lives in [STATE.md](./STATE.md) in this folder — do not edit `.planning/STATE.md`.

## Goal

Learners create a **spelling memory tracker** themselves at assimilation, the same way they create a commissioned tracker. Remove the note-level **Remember Spelling** checkbox. Do not add a notebook-level default.

## Naming (UI)

**Remember spelling** — menu item on the existing assimilate-options caret (same caret as commissioned).

Not “Assimilate as spelling”: that reads as a *mode of assimilation* that replaces understanding. Spelling is extra practice on the title, not an assimilation mode. Keep **Assimilate as commissioned** as-is; commissioned *is* a mode.

## Current vs intended

| | Today | Intended |
|--|--|--|
| Source of truth | Note flag `remember_spelling` *and* `MemoryTrackerType.SPELLING` | Tracker type only |
| Who chooses | Note owner (shared with subscribers) | Learner at assimilation |
| Creation | Check box → verify title → Assimilate creates understanding **and** spelling (or add-spelling-only later) | **Remember spelling** → verify title → spelling tracker only |
| Assimilation due | Any note-level non-commissioned tracker (including spelling) takes the note off the queue | Due only if there is no **understanding** note-level tracker |
| Daily assimilation count | Today’s query skips commissioned only | Same as commissioned: spelling does not count |

## Constraints

- Add the **Remember spelling** action **before** removing the note field (interim: both paths exist).
- Tests that only cover the **note option** (checkbox shown/hidden/disabled/error copy, flag-driven assimilate creating two trackers): **delete**, do not retarget.
- Tests that cover **spelling behavior** (verification, spelling recall, accidental match, overlap, CLI spelling): **keep**. Change fixtures from the note flag to a spelling tracker / Remember spelling action. Do not drop coverage.
- No data migration when dropping `remember_spelling`. Existing `SPELLING` trackers stay; unread flags are discarded.
- No notebook-level default.
- Update Proposed ADRs `docs/adrs/0001-ubiquitous-language.md` and `docs/adrs/0003-spaced-repetition-scheduling-policy.md` to the **current** model only — no “we used to have a checkbox” narrative. Do not mark them Accepted.

## Out of scope

- Notebook-level “assimilate as spelling” default
- Changing spelling *recall* grading (accidental match, overlap, stems)
- Renaming “Assimilate as commissioned”
