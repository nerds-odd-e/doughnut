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
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["Migration family selection and destination-tracker collision policy remain unresolved.","Production census and a bounded executable approach with Git publication proof are not established."],"basis":{"document":"e7163f5841ad2d6c2cf3030960bc147b73b4be62f514388b5c75272ef828278c"}}
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
  - `frontend/src/utils/noteContentPropertyKeys.ts` recognizes a numeric suffix
    of at least 2, not word suffixes. It classifies `url` as list-capable but
    structural keys such as `image` as scalar-only.
  - The rich-mode preset dropdown still offers numbered keys when a preset is
    occupied (`noteContentPropertyKeyPresets.ts`, consumed by
    `RichFrontmatterPropertyKeyPresets.vue`). Existing tests explicitly expect
    `url 2`, `image 2`, and `example of 3`. The earlier assertion that the
    product no longer creates numbered keys was too broad; backend reduce no
    longer needs them, but the dropdown still does.
  - Migration `V300000352__add_property_value_to_trackers_and_property_index.sql`
    enforces uniqueness by learner, note, tracker type, property key, and value.
    Two old trackers can therefore collide at one destination; preserving both
    records there is not supported by the current schema.
  - `AcceptedWebChangeService.apply` locks bindings and atomically appends
    changed Portable content with the projection transaction.
    `AuthoredNoteDocumentPersistence.persist` refreshes derived indexes.
    These are existing publication responsibilities to reuse, not evidence that
    they can already be invoked safely from a Flyway Java migration.
- **Architecture:** [ADR 0002 — Git-native Portable notebook tree
  synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  requires Git publication and atomic projection/identity/derived-state
  acceptance; [ADR 0004 — OKF-compatible notebook Markdown
  profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  governs the authored frontmatter and shared Portable codec. The migration
  must use those contracts; it does not justify a second publication model.
- **Open decisions and observations:**
  - Family selection: proposal is all list-capable numeric families, including
    untracked `url 2`, leaving structural and word-suffix keys unchanged.
    Confirm whether this matches the owner's intended legacy convention.
    The original report's `example of two` is not evidence of persisted word
    suffixes; a read-only content census would settle that premise.
  - Destination collisions: proposal is to stop the affected notebook before
    changing content or learning data and resolve it manually. History merging
    requires an owner-defined rule; do not silently discard either tracker.
  - Census: production counts and shapes remain unknown. A read-only scan of
    stored frontmatter and trackers should count affected notes, notebooks,
    bound notebooks, word suffixes, absent bases, existing lists, unsupported
    values, and destination collisions using the agreed family rule. No
    production connection or data snapshot was established for this session;
    source inspection cannot supply these counts.
  - For absent base keys, existing base lists, and out-of-order numeric suffixes,
    settle grouping and value ordering with concrete observed examples before
    implementation; do not infer a destructive rule solely from suffix syntax.
  - The remaining preset path can reintroduce numbered keys after migration.
    [Story 3](#story-3) now owns stopping that creation path and cleaning up its
    generation functions; this migration remains about existing content.
  - Execution planning must choose a one-time entry point that can honor Git
    publication and restart without partial notebook updates. Static inspection
    has not established Java Flyway/service wiring or completed end-to-end proof.
- **Effort hypothesis:** M, low confidence until the census and collision policy
  are known; this refinement does not size executable slices.
- **Depends on:** per-value trackers are already in the product; no new
  per-value tracking capability is required.
- **Safe stopping point:** old notes keep working with numbered keys.

<a id="story-3"></a>

### Property key suggestions stop creating numbered keys, and obsolete generation functions are removed

**Identity:** SEED-063#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **Goal:** notebook authors choose a meaningful property key and add another
  value under that key, without Donut suggesting `url 2` or `example of 2`.
  Close the unfulfilled no-new-numbered-keys promise of completed story 1.
- **Scope:**
  - Remove automatic numbered-key generation from rich-mode key suggestions,
    including the add-property and existing-row key fields that share them.
    An occupied list-capable preset still names its base key; adding another
    value uses the existing list-append behavior.
  - Remove unused numbered-key generation functions and exports once their
    callers are replaced. Do not preserve tests whose expected output enshrines
    the retired numbered suggestions.
  - Preserve scalar-only structural property rules and singleton presets; do
    not convert `image`, `wikidata_id`, or other structural values into lists.
    The occupied structural-preset interaction needs refinement before execution.
  - Keep recognition of authored legacy numbered keys needed by existing notes
    and story 2. Recognition and generation are different responsibilities;
    remove only code made obsolete by this outcome.
  - This is a correction to new authoring behavior, not the existing-content
    migration, the draft row's Add/Cancel layout, or cleanup of spent Java
    migration code. The latter remains story 2's conditional follow-up.
- **Key examples:**
  - Note has `example of: "[[run]]"` → open Add property's key suggestions →
    choose `example of`, enter `[[past tense]]`, and add → one `example of` list
    holds both values; Donut suggests and creates no `example of 2`.
  - Note has `url: "https://one.example"` → choose the `url` suggestion and add
    `https://two.example` → both URLs share the `url` list, without a `url 2` key.
  - Existing note has a legacy `example of 2` → view/edit → it remains readable;
    this authoring correction does not migrate its content or move its tracker.
- **Proof gap to close:** drive the mounted editor through an occupied preset
  selection and observe saved Markdown and suggestions. Expected options must
  express the product promise directly, not be computed by the same generator
  under test. Extend the existing authoring journey where it owns this boundary.
- **Open refinement:** how occupied scalar-only presets are offered or omitted;
  how choosing an occupied key while renaming an existing row should behave;
  verify integration with SEED-064#story-6's draft row when delivered.
- **Effort hypothesis:** S–M, medium confidence; the generator is shared, while
  the list append capability already exists. No executable plan is created here.
- **Depends on:** per-value tracking and list append already delivered. Coordinate
  with SEED-064#story-6's shared key field; it does not own this correction.
- **Safe stopping point:** new authoring stops growing the legacy convention;
  existing numbered content continues working until story 2 migrates it.

**Investigation evidence (2026-10-01)**

- At `e2833b5ac8`, story 1's Goal explicitly says "Donut itself also stops
  creating numbered keys". The same commit's executable plan narrows its
  goal/proof to per-value learning and the reduce flow, and its Current
  decisions says frontend numbered-key suggestions stay for story 2 and
  SEED-064#story-6 to decide. That is a story/plan mismatch already present
  before execution, not a later dropdown regression. No owner approval for
  this particular deferral was found in the recovered repository records;
  the original conversation was not inspected.
- `bc07de120d` had assigned removal of occupied numbered preset entries to
  SEED-063. SEED-064#story-6's refinement at `d71fcee679` excludes changing
  the existing-key rule. It promises no new numbered suggestion, but does
  not promise removal of the old suggestions. The two artifacts therefore
  leave the same user-visible behavior without a concrete delivery owner.
- `eead7350eb` removes the backend numbered-key helper and makes reduce
  append a value; it does not change the frontend preset helper. Story 1's
  proof table has no occupied-preset selection journey.
- `ec61ef942a` records execution complete while retaining that explicit
  frontend deferral. `63fa9f178b` removes story 1 and its plan, and changes
  story 2's context to "The product no longer creates new ones" despite
  the remaining generator. This closure statement is not supported by
  the delivered dropdown behavior.
- Current tests preserve the old behavior: the preset utility test expects
  `url 2` and `example of 3`; the mounted editor's preset test derives its
  expected options from that same helper. Typing the exact existing base
  key exercises list append, but choosing the suggested numbered key
  bypasses that branch and creates another key.
- Observed verification: `CURSOR_DEV=true nix develop -c pnpm frontend:test
  tests/utils/noteContentPropertyKeyPresets.spec.ts
  tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts` passed
  22/22 tests. This confirms the old numbered-preset expectations are still
  green; it is not proof of the promised no-numbered-key journey.
- Conclusion: stopping new numbered-key suggestions is an undelivered part
  of story 1's recorded promise, distinct from migrating existing content.
  The completion label and absence of story 1 from the queue cannot establish
  that this promise was fulfilled. Owner decision, 2026-10-01: this new story
  owns closing the gap and cleaning up the generation functions. No product
  code has changed yet.

## When to Surface

When selecting note property or memory tracking improvements from the backlog.

## Breadcrumbs

- Owner's report and requirement, 2026-09-30: replace the confusing numbered-key
  convention with multiple values of one property, each tracked independently.
- Related: reifying a single-link property into a relationship note exists in the product and refuses
  structural keys; it is not a prerequisite of this seed.
- Related UAT fixes: [SEED-064#story-6](SEED-064-note-properties-fixes.md#story-6) (add-property row) uses the
  per-value rule for an existing key. The remaining numbered-key code is `propertyKeyBaseAndSuffix` and
  `nextAvailablePropertyKeyFor*` in `frontend/src/utils/noteContentPropertyKeys.ts`; the backend no longer
  creates numbered keys.
