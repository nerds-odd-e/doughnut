---
id: SEED-064
status: dormant
planted: 2026-09-30
planted_during: owner selection of note properties fixes and improvements found by the one-hour manual UAT (SEED-061#story-1)
trigger_when: making note properties comfortable on an iPad, operable on a phone, and simpler to maintain
scope: medium
---

# SEED-064: Make note properties comfortable on an iPad, operable on a phone, and smaller in code

## Story Decomposition

<a id="story-7"></a>

### Expand a list property into a single list of clickable values

**Identity:** SEED-064#story-7
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-expanded-property-values/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"70a3bf6e741d2a35134da32aab897747fe8974a8eef7def8949e213c78bfef81","plan":"f4a135026f0fc4bfcd2597e14fccde01e8d55c142e56e5afcd343bde0b0cb235"}}
```

- **Goal:** A learner inspecting a list-valued note property can read and follow
  its linked notes directly from the expanded list, with each value shown once.
- **Scope:** Expanding the property replaces the compact value list beside the
  key with the detailed list below it. Keep the key and collapse control visible.
  Make the value/title on the left of each expanded item a clickable note link,
  retaining its authored label and destination. Keep each item's tracker link,
  learning status and existing actions alongside that item. Keep the existing
  list-wide Edit, Remove, Reify and Skip/Return controls available with their
  existing meanings. Collapsing restores the compact value links beside the key.
  Render authored occurrences in order, including plain text and repeated or
  long values; "shown once" removes repeated presentation, not authored entries.
  Repeated values still identify the same value-based learning unit. Preserve
  URL-property external links and wiki-link pending/dead feedback and recovery.
  Shared editors without a saved note keep their values visible when expanded;
  their presentation does not require a memory tracker. Delete the redundant
  expanded-state value rendering and any code used only by it; reconcile
  affected documentation and use a one-time acceptance reading to sweep its
  dependent references.
- **Deferred promises:** This delivery adds no learning-unit identity or
  eligibility policy, list editing workflow, or property operation. Read-only
  and draft rows retain their existing presentation and interactions.
- **Key examples:**
  1. A note has an `example of` property containing four note links, as in the
     owner's screenshot. Expand it: one detailed list presents the four authored
     labels in order alongside their tracker links, status and learning actions;
     the key, collapse control and Edit remain usable. Follow a label to its
     linked note. A labeled property target such as
     `[[Moon#prop:a%20part%20of|lunar topic]]` opens the target property using the
     label `lunar topic`.
  2. Collapse that property: the compact row again provides the note links
     beside `example of`. Reopen it and open Edit: the existing list dialog
     edits the same authored list.
  3. A mixed list contains a note link, repeated plain text and a value longer
     than the tracking limit. Expand it: all authored occurrences remain in
     order, while learning details continue to refer to their existing eligible
     value-based units. The same presentation rule keeps values readable in a
     shared editor without a saved note.
  4. Expand a list with pending and dead wiki links, or a `url` list with plain
     URL values: the active representation keeps the existing pending/dead
     feedback and recovery and the external URL actions.
- **Architecture:** The learner's visible list is authored content, while
  memory trackers are a value-based projection. Keep display ownership over the
  complete authored list independently of tracking eligibility and saved-note
  availability. Reuse existing property-value and wiki-link rendering with its
  resolution context and recovery events. Follow
  [ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) for property/tracker
  identity, [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#links-and-attachments)
  for authored link meaning, and
  [ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) for named note/property
  navigation. This is a frontend presentation change within those decisions.
- **Investigation:** Local rendered observations on 2026-10-20 at
  `51dec4f7b1678c85aa5cff2448c97e54b427aec5` reproduced compact links alongside
  raw expanded labels. A hide-only scratch change lost authored values excluded
  by the learning projection and all values in an editor without a saved note.
  A detail-scoped navigation proof failed on the baseline and passed when the
  existing wiki renderer and context were wired into the expanded label,
  including property destinations and dead-link recovery. All scratch changes
  were reverted. These observations clarify preservation needed by this story;
  they introduce no new goal or unresolved decision.
- **Value / learning:** Reduces repeated content and makes navigation available
  where the learner is already reading the per-value details, especially on iPad.
- **Effort hypothesis:** S (30–60 minutes), medium confidence; presentation and
  link reuse, subject to refinement of existing expanded-item rendering.
- **Depends on:** None.
- **Safe stopping point:** The expanded list supports note navigation and the
  existing per-value learning actions independently of other properties work.
- **Source:** Owner request and attached iPad screenshot, 2026-10-20. The
  expanded `example of` property shows four compact links beside the key and
  repeats those values below as `[[…]]` labels with `View tracker` links.

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
- Retired numbered-key conversion: [Numbered-property migration retirement](../../docs/numbered-property-migration.md).
