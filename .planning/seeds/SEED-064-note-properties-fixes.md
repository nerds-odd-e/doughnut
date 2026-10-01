---
id: SEED-064
status: dormant
planted: 2026-09-30
planted_during: owner selection of note properties fixes and improvements found by the one-hour manual UAT (SEED-061#story-1)
trigger_when: making note properties comfortable on an iPad, operable on a phone, and simpler to maintain
scope: medium
---

# SEED-064: Make note properties comfortable on an iPad, operable on a phone, and smaller in code

## Unselected owner choices

- **Findings left out by the owner** (low value for the effort, or not reproduced):
  - a collapsed "show all" mode for long property lists (iPad I2); the row wrapping already keeps every value visible;
  - an empty-state message for a note without properties, and hiding the `type: Note` row (iPad I5); its answer
    depends on whether the row is stored (ADR 0004);
  - a message when a value is appended to a list key such as `url` (iPad I7);
  - a confirmation or undo for remove (iPad I8); 44 px targets on touch devices reduce mistaken taps;
  - the upload error disappearing after 2.5 seconds (iPad I10);
  - a Replace tap that timed out once and a tap blocked once after an image upload: not reproduced;
  - the panel's Assimilate moving to another note and the Skip wording (phone finding, iPad I8 wording): these are
    assimilation behavior, not properties layout; the owner has not decided whether they are wanted;
  - merging the `noteContent*` utilities, one YAML parser for both languages, replacing the value field with a plain
    input, changing the value dialog, and a new confirmation dialog for remove (report, "Not recommended").

## Open Decisions

- Not queued, owner has not decided: whether the row panel's Assimilate should move to another note after saving
  a property understanding item, and the wording of its Skip confirmation.


## Source evidence

- UAT report: `1986473b79:.planning/seeds/SEED-061-note-properties-ux-uat.md`, section `## UAT Findings` (screenshots were not committed).
- Current property editing behavior: [Note-content saving](../../docs/note-content-saving.md#rich-property-editing).
- Active numbered-key conversion: [SEED-063](SEED-063-track-property-values-separately.md#story-2).
