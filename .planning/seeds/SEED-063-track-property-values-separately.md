---
id: SEED-063
status: dormant
planted: 2026-09-30
planted_during: owner request to replace numbered property keys with separately tracked values
trigger_when: improving how multi-valued note properties are expressed and memory tracked
scope: medium
---

# SEED-063: Track multiple values of one property without numbered keys

## Why This Matters

An example sentence can exemplify several vocabulary items or grammar concepts.
According to the owner's report, a tracked property could not be a list, so
authors used distinct keys such as `example of`, `example of two`, and
`example of three` to create a separate memory tracker for each association.
Each value of a list property now has its own tracker, but those numbered keys
remain in existing notes. The suffixes make one shared meaning look like different properties and are
confusing, even when the convention is explained in AI instructions.

## Story

<a id="story-2"></a>

### Existing numbered property keys become one list, and their trackers follow

**Identity:** SEED-063#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["Owner answers are pending for numeric-family selection/value ordering and notebook-versus-run collision handling.","The executable approach remains unselected: after those answers, write bounded slices with mapped preservation/publication proof and early census/startup probes."],"basis":{"document":"1f814a1627e488ad633e57c61b41188be3a0b5089d55bec97fec97a30465eced"}}
```

- **Goal:** existing notebook authors and learners see multiple associations
  under one meaningful property key, without losing the learning history or next
  recall of each previously tracked association.
- **Scope:** a one-time migration consolidates existing legacy numbered
  property families into a list under the base key and moves each learner's
  existing trackers to the corresponding key/value focus. Preserve tracker
  identities, histories, schedules, and note identities; do not recreate them.
  Apply the outcome across notebooks, including stored notes in Trash, so
  restoring a note does not restore the old convention. Refresh content-derived
  property indexes and references alongside content. The family selection and
  collision rules below remain open; the outcome is not a promise to delete
  every authored key that happens to end with a number.
  - Owner decision, 2026-10-01: no toggle, feature flag, or migration placeholder
    gate is needed. This overrides the installed migration skill's default
    gate guidance for one-time destructive DML.
  - For Git-bound notebooks, publish the transformed Portable content as an
    accepted descendant commit; content projection, tracker moves, and derived
    state must agree atomically. Preserve accepted history and private learning
    data. A SQL-only projection rewrite would not deliver this outcome.
  - Owner decision, 2026-10-01: if delivery introduces Java migration code,
    add a separate cleanup story for removing that spent code after confirmed
    migration completion. Select and queue that follow-up when Java migration
    code is introduced; no such code is introduced by this refinement.
  - Deferred promises: a recurring author action, a new migration UI, automatic
    merging of learning histories, and deletion of all numbered-key helpers.
    Existing helpers also recognize structural keys; their removal is not
    justified merely by migrating ordinary properties.
- **Key examples:**
  - `example of: "[[run]]"` and `example of 2: "[[past tense]]"`, each tracked
    by one or more learners → migration → `example of: ["[[run]]",
    "[[past tense]]"]`; each original tracker now focuses on its original
    value under `example of`, with the same history and next recall.
  - The same note is in a Git-bound notebook → migration → the next accepted
    tree contains that list and no migrated suffixed key; the application reads
    the same content, while learning history remains private.
  - A migrated note is in Trash → restore → its associations are already in
    the consolidated form and retained trackers still refer to their values.
  - A notebook already has the consolidated form → another migration attempt
    → content and tracker history remain unchanged; no duplicate content commit
    is required. This is a necessary property of a one-time migration that can
    be retried, not a user-controlled toggle.
- **Investigation evidence (2026-10-01):** static inspection, not a production
  census or an executed migration rehearsal.
  - `PropertyMemoryTrackerService.followPropertyValue` finds all learners'
    trackers of a scalar focus and changes only `propertyValue`. It preserves
    the tracker record, but does not rename the source key. Existing reuse
    therefore covers only part of this migration.
  - `NoteContentMarkdown.addPropertyValueToLeadingFrontmatter` already appends
    values, converts a scalar to a list, and avoids repeating an existing value;
    unsupported map/nested shapes are left as authored. It does not itself
    identify or consolidate numbered families.
  - Backend `PropertyKeyNaming.propertyKeyBaseAndSuffix` already recognizes
    numeric suffixes of at least 2, not word suffixes; reuse that domain owner
    rather than introducing another suffix parser. The frontend's
    `isListCapablePropertyKey` permits `url` but excludes scalar-only structural
    keys such as `image`. Backend `isReservedStructuralKey` also excludes `url`
    from learning indexes, so it is not the migration's list-capability rule.
  - Migration `V300000352__add_property_value_to_trackers_and_property_index.sql`
    enforces uniqueness by learner, note, tracker type, property key, and value.
    Two old trackers can therefore collide at one destination; preserving both
    records there is not supported by the current schema.
  - `AcceptedWebChangeService.apply` locks bindings and atomically appends
    changed Portable content with the projection transaction.
    `AuthoredNoteDocumentPersistence.persist` refreshes derived indexes.
    These are existing publication responsibilities to reuse, not evidence that
    they can already be invoked safely from a Flyway Java migration.
  - `FlyWayFreeVersionRealMigration.actualMigration` runs synchronously on
    `ApplicationReadyEvent` at highest precedence. A separate, later Spring
    startup listener can use injected application services after schema
    migration. This is the recommended entry point to rehearse, rather than
    assuming Flyway-created Java migrations can inject JPA services. Process
    notebooks in separately committed accepted-change transactions; derive
    retry eligibility from remaining legacy content, without a toggle or a
    second content authority. Recheck content and tracker destinations under
    the publication lock before changing them.
  - Existing `NotebookGitWebContentSaveAtomicControllerTest` injects a late
    binding-save failure and reads committed state and downloaded Git objects;
    this is the proof pattern to extend to tracker moves. Its current tests
    exercise a web content save, not this migration or startup ordering.
    `MemoryTrackerFollowPropertyValueControllerTest` and
    `MemoryTrackerUpdatePropertyKeyControllerTest` provide preservation
    examples, not evidence of a complete numbered-family migration.
  - `WikiLinkPropertyMatch` resolves a `#prop:` selector only while the exact
    authored key exists. Rebuilding derived indexes does not retarget another
    note's authored selector. Include inbound selectors in the early census;
    if any target a proposed removed key, stop that affected path and establish
    how to preserve those links before migration. Do not claim reference
    preservation from an index refresh alone.
- **Architecture:** [ADR 0002 — Git-native Portable notebook tree
  synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  requires Git publication and atomic projection/identity/derived-state
  acceptance; [ADR 0004 — OKF-compatible notebook Markdown
  profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs the authored frontmatter and shared Portable codec. The migration
  must use those contracts; it does not justify a second publication model.
- **Open decisions and observations:**
  - Family selection (owner answer pending): proposal is all list-capable
    numeric families, including untracked `url 2`, leaving scalar-only
    structural and word-suffix keys unchanged. Create the unsuffixed base if
    missing. Keep existing base values in authored order, then append suffix
    values in numeric order, preserving each source list's order and keeping
    the first occurrence of a duplicate value. Confirm whether this matches
    the owner's intended legacy convention. Do not fold case-distinct authored
    families together merely because a structural-key recognizer ignores case.
    The original report's `example of two` is not evidence of persisted word
    suffixes; a read-only content census would settle that premise.
  - Destination collisions (owner answer pending): proposal is to leave the
    entire affected notebook unchanged, report it for manual resolution, and
    continue independent notebooks. The alternative is to stop the complete
    migration run. Check all persisted trackers against the actual unique
    constraint, including inactive trackers; an active-only UI rename check is
    insufficient. History merging requires an owner-defined rule; do not
    silently discard either tracker.
  - Census: production counts and shapes remain unknown. A read-only scan of
    stored frontmatter and trackers should count affected notes, notebooks,
    bound notebooks, word suffixes, absent bases, existing lists, unsupported
    values, inbound removed-key selectors, and destination collisions using
    the agreed family rule. No
    production connection or data snapshot was established for this session;
    source inspection cannot supply these counts.
    Production access is operator-held, so this observation may be owned by an
    early read-only probe in the executable plan; missing production counts
    alone need not prevent planning. Unexpected shapes stop dependent work and
    revise the same plan before broad implementation. Do not use a development
    database as a disposable rehearsal environment.
  - [Rich property editing](../../docs/note-content-saving.md#rich-property-editing)
    uses canonical suggestions and exact-key append. This migration remains
    about existing content and preserves the separate learning histories.
  - After the two owner policy answers, execution planning should map each
    promise to bounded slices, with an early isolated startup/publication
    rehearsal and the operator-held census. Prove a downloaded descendant tree,
    unchanged tracker identities/history/schedules, Trash restoration,
    collision handling, late-failure rollback, and retry without a new commit.
    The recommended startup entry point is still static evidence, not a
    completed rehearsal. Refinement does not authorize implementation or a
    live migration.
- **Effort hypothesis:** M, low confidence until the census and collision policy
  are known; this refinement does not size executable slices.
- **Depends on:** per-value trackers are already in the product; no new
  per-value tracking capability is required.
- **Safe stopping point:** old notes keep working with numbered keys.

## When to Surface

When selecting note property or memory tracking improvements from the backlog.

## Breadcrumbs

- Owner's report and requirement, 2026-09-30: replace the confusing numbered-key
  convention with multiple values of one property, each tracked independently.
- Related: reifying a single-link property into a relationship note exists in the product and refuses
  structural keys; it is not a prerequisite of this seed.
- [Rich property editing](../../docs/note-content-saving.md#rich-property-editing)
  uses canonical key suggestions and appends values to an exact existing key.
  `propertyKeyBaseAndSuffix` in `frontend/src/utils/noteContentPropertyKeys.ts`
  still recognizes authored legacy content; numbered-key generation is removed.
