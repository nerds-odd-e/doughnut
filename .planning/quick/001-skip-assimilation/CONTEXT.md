# Assimilation-sequence skip — context

Ad-hoc slice (not on a GSD milestone). Phases in `PLAN.md` are small-commit-sized (one Behavior or one Structure each). Product language lives in Proposed [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md). This work both **locks the glossary** for these terms and **makes the runtime model match**.

## What is skipped (confirmed)

The mark does **not** skip assimilation. It only takes the unit out of the **assimilation sequence** (the next-to-assimilate walkthrough / `/api/assimilation/next`).

The learner **must** still be able to open the note and assimilate it as **any** tracker type:

- ordinary **Assimilate** → understanding memory tracker
- **Remember spelling** → spelling memory tracker
- **Assimilate as commissioned** → commissioned memory tracker

Spelling and commissioned trackers may coexist with a sequence-skip row. An understanding tracker must not: ordinary assimilate **clears** the sequence-skip row (the unit is no longer sequence-eligible anyway).

## Decoupling (confirmed)

After this plan, sequence skip is **not** coupled to an understanding memory tracker.

- A sequence-skip row means the unit is not offered as next.
- An understanding memory tracker means the unit was assimilated (and may later be removed from recall).
- Those two facts must not both exist for the same learner × unit.

New sequence skips insert an `assimilation_sequence_skip` row and do **not** create a tracker.

## Migration (faithful, `recall_count = 0` only)

From `UNDERSTANDING` trackers with `removed_from_tracking` and `recall_count = 0` (note-level and property-level, in their respective phases):

1. Insert matching `assimilation_sequence_skip` rows.
2. Soft-delete those trackers (`deleted_at`).

Trackers with `recall_count > 0` stay **removed from recall**. They are not sequence skips.

**Ambiguity accepted:** “Assimilated, then removed from recall, never recalled” looks the same as “skipped in the sequence” (`recall_count = 0`). Both become sequence skips. **Return to sequence** puts them back in the walkthrough; Revive does not apply.

## Grain

Same grain as an understanding tracker: learner × note (note-level) or learner × note × property (`property_key` empty string = note-level).

## Return to sequence vs Revive (must stay distinct)

| | Return to sequence | Revive |
|---|---|---|
| Undoes | Sequence skip | Remove from recall |
| UI | **Return to sequence** on assimilation settings | **Revive** on assimilation settings / tracker page |
| Storage | Delete `assimilation_sequence_skip` row | `removed_from_tracking = false` on the tracker |
| Result | Unit is **pending** again (back in the sequence) | Tracker is **active** for recall again |

Do not reuse Revive for sequence-skipped units.
