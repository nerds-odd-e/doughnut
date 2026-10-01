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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-numbered-property-consolidation/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"42ec34c66cf63185acde46dc7e6574b7249b728a9afcf9234fdd7cbc10559d58","plan":"e019e0b4420e8e322848ab94a7fdb677125aeaae3d0f58d9f0d681f2969a78e2"}}
```

- **Goal:** existing notebook authors and learners see multiple associations
  under one meaningful property key, without losing the learning history or next
  recall of each previously tracked association, except redundant trackers
  explicitly allowed to be dropped below.
- **Scope:** a one-time migration consolidates existing legacy numbered
  property families into a list under the base key and moves each learner's
  existing trackers to the corresponding key/value focus. Preserve tracker
  identities, histories, schedules, and note identities; do not recreate them.
  The duplicate-destination exception below permits dropping only redundant
  trackers and their dependent history, without merging histories.
  Apply the outcome across notebooks, including stored notes in Trash, so
  restoring a note does not restore the old convention. Refresh content-derived
  property indexes and references alongside content. Consolidate list-capable
  numeric families (suffix at least 2), including `url`, while leaving
  scalar-only structural and word-suffix keys unchanged. Create a missing
  unsuffixed base. Keep its values first in authored order, then values from
  numeric suffixes in ascending order, preserving source-list order and the
  first occurrence of each duplicate value. Keep case-distinct authored
  families distinct. These family and ordering rules were accepted by the
  owner in this session on 2026-10-01.
  - Owner decision, 2026-10-01: duplicate tracker destinations are not expected
    in production, but dropping redundant trackers is allowed to keep the
    logic simple. This supersedes the earlier skip-notebook proposal. Keep a
    tracker already at the final destination when present; otherwise keep the
    lowest-ID tracker in that destination group. Drop the other trackers using
    existing deletion semantics; retain the survivor's own history and schedule
    and do not merge histories. Match the actual database uniqueness rule,
    including inactive trackers and collation, not only Java string equality.
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
  - Only `topic 10: C` and `topic 2: B` exist → migration →
    `topic: [B, C]`; the missing base is created and each tracker follows its
    value. With an existing `topic: [A, B]`, the result is `[A, B, C]`.
  - Two same-learner, same-type trackers target `example of: "[[run]]"` and
    `example of 2: "[[run]]"` → migration → one value and one retained tracker
    at that destination. The redundant tracker is dropped, not history-merged.
  - An authored wiki selector resolves to a migrated suffixed key → migration
    → retarget that resolved selector to the retained base key and refresh its
    source-owned reference index in the same accepted change. Preserve the
    visible link text. Do not guess a destination for ambiguous/unresolved
    selectors. If a rewritten selector is itself a tracked list value, its
    tracker follows the rewritten value; the duplicate rule applies there too.
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
    note's authored selector. Reference preservation therefore includes
    retargeting resolved selectors, through the existing authored-reference
    rewrite and multi-notebook accepted-change owners; an index refresh alone
    does not deliver it.
- **Architecture:** [ADR 0002 — Git-native Portable notebook tree
  synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  requires Git publication and atomic projection/identity/derived-state
  acceptance; [ADR 0004 — OKF-compatible notebook Markdown
  profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs the authored frontmatter and shared Portable codec. The migration
  must use those contracts; it does not justify a second publication model.
  [ADR 0007 — Environments and isolation](../../docs/adrs/0007-environments-and-isolation-accepted.md)
  confines migration rehearsals to disposable test data.
- **Remaining observations:** no product-policy question is open.
  - Production counts and shapes are unknown; no production connection or
    snapshot was established. The owner's expectation that duplicates do not
    exist is an assumption, not observed evidence, and the duplicate rule makes
    correctness independent of it. A production census is not a preparation
    gate. Observe supported shapes, collation, startup ordering, and atomicity
    against isolated fixtures before enabling the startup caller. Unexpected
    unsupported or unmappable data stays unchanged and is reported rather than
    guessing a destructive transformation.
  - [Rich property editing](../../docs/note-content-saving.md#rich-property-editing)
    uses canonical suggestions and exact-key append. This migration remains
    about existing content and preserves the separate learning histories.
  - The executable plan maps each promise to bounded slices, with an early
    isolated startup/publication rehearsal. Prove a downloaded descendant tree,
    unchanged tracker identities/history/schedules, Trash restoration,
    redundant-tracker deletion, reference continuity, late-failure rollback,
    and retry without a new commit.
    The recommended startup entry point is still static evidence, not a
    completed rehearsal. Refinement does not authorize implementation or a
    live migration.
- **Execution plan:** [Numbered property consolidation](../slice-plans/001-numbered-property-consolidation/PLAN.md).
- **Effort hypothesis:** M, medium confidence after policy selection; startup
  integration is bounded by the plan's early probe.
- **Depends on:** per-value trackers are already in the product; no new
  per-value tracking capability is required.
- **Safe stopping point:** old notes keep working with numbered keys.

<a id="story-3"></a>

### Remove spent numbered-property migration after confirmed completion

**Identity:** SEED-063#story-3

- **Goal:** developers maintain ordinary application startup without spent
  one-time migration code after the numbered-property conversion is confirmed.
- **Scope:** remove the temporary startup caller and migration orchestration,
  plus migration-only proof harnesses, after confirmed completion in the
  deployed environments. Retain shared property naming, authored-frontmatter
  editing and tracker responsibilities that continue to serve product behavior.
  Preserve accepted history, tracker identities and learning data.
- **Key example:** all eligible operations have completed and no reported
  unmapped operation remains → retire the one-time runner → startup no longer
  scans legacy families, while the consolidated content and learning remain.
- **Prerequisite:** story 2 is delivered and its migration completion has been
  confirmed operationally. Unit fixtures alone do not satisfy this prerequisite.
  Application release and production observation remain separately authorized.
- **Origin:** the owner's 2026-10-01 instruction to select and queue cleanup
  when temporary Java migration code is introduced by story 2.
- **Effort hypothesis:** S; refine the actual removal boundary after confirmation.

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
