---
id: SEED-101
status: dormant
planted: 2026-11-04
planted_during: discussion of Git-derived property timestamps for assimilation
trigger_when: improving the order in which learners assimilate note properties
scope: medium
---

# SEED-101: Use property change times in assimilation

## Story Decomposition

<a id="story-1"></a>

### Order property assimilation by when values changed

**Identity:** SEED-101#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

#### Goal

Learners encounter eligible property values in the order those values were last
added or changed within the same assimilation level. A property added today to
an old note queues after older material at that level.

#### Scope

- Use the committer time of the last accepted Git commit that changed the parsed
  property value. Compare scalar contents and date list items individually.
  Body edits and list reordering preserve existing property dates.
- Cache one optional timestamp per value in the property index. Scope ordinary
  save work to the affected notes. For a multi-commit push, use the newly
  accepted range to catch changes such as an edit followed by a revert.
  Assimilation reads the index directly.
- Rebuild dates from retained Git history in the background. While a date is
  missing, including when a history reset has removed the relevant history,
  use the note creation time. The cache stores ordering data only.

Accepted Git history supplies the evidence and the publication tip supplies the
indexed values. [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
