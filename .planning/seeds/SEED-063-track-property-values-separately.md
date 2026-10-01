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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-property-key-suggestions/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e3a7398ffaf820e8285802989fe16dee9b0383451e2fb21f92c96167cc3ed51e","plan":"2d149d4e3cf27d4f545b50a842fb7168e377a7550dfd676cba103fcd1e751763"}}
```

- **Goal:** notebook authors choose a meaningful property key and add another
  value under that key, without Donut suggesting `url 2` or `example of 2`.
  Close the unfulfilled no-new-numbered-keys promise of completed story 1.
- **Scope:**
  - Rich-mode preset suggestions use canonical base keys and never generate
    numbered alternatives, in both Add property and stored-row key fields.
    Keep the existing note versus folder/notebook-readme preset sets and filtering.
  - In Add property, an occupied list-capable preset such as `url` or
    `example of` remains available under its base key. Confirming another value
    follows the existing exact-key append rule: a scalar becomes a list, and an
    existing list gains the value in order, preserving earlier values.
  - Owner decision, 2026-10-01: omit occupied scalar-only and singleton
    preset slots rather than offering numbered alternatives. This includes
    `image`, `wikidata_id`, `question_generation_instruction`, `title_pattern`
    in readme context, and the existing note-only singletons `aliases`,
    `overlaps`, and `note_level`. Recognized legacy aliases and numbered keys
    continue to count as occupying these slots, as the current family rules do.
    Authors edit the existing row to change its value. Preserve scalar-only
    structural rules; removing suffix generation does not permit structural lists.
  - Owner decision, 2026-10-01: when editing a stored row's key, omit a
    preset whose exact base key is already used by another row, including
    list-capable keys. Ignore the current row when checking occupancy, so its
    own canonical preset remains available unless another row occupies it.
    Renaming is not adding a value. Preserve the existing duplicate-key
    validation if an author manually enters another row's exact key; do not
    merge rows or move trackers as a new rename behavior. This rejection follows
    `validatePropertyRowsForRichEdit` and `commitRow`'s current duplicate rule.
  - Remove numbered-key generation functions and exports made unused by this
    change, including the `nextAvailablePropertyKeyFor*` responsibility and its
    obsolete tests. Keep `propertyKeyBaseAndSuffix` and other recognition needed
    by structural controls, validation, and authored legacy content.
  - Deferred promises: migrating existing keys or trackers (story 2), merging
    rows or learning histories, changing exact-key matching or alias/case
    normalization, prohibiting manually authored numbered keys, changing the
    draft row's Add/Cancel layout, and cleaning up spent Java migration code.
    These are delivery exclusions, not additional product rejection rules.
- **Key examples:**
  - Note has `example of: "[[run]]"` → open Add property's key suggestions →
    choose `example of`, enter `[[past tense]]`, and confirm → saved Markdown
    has one `example of` list with both values in that order; neither the
    suggestions nor saved content gains a generated `example of 2`.
  - Note has `url: ["https://one.example", "https://two.example"]` → choose
    `url` in Add property and confirm `https://three.example` → the same list
    holds all three URLs, without a generated `url 2` key.
  - Note has `image: "/one.png"` and `wikidataId: "Q1"` → open Add property's
    suggestions → occupied `image` and `wikidata_id` slots are omitted; no
    `image 2` or `wikidata_id 2` is offered. Existing values remain editable.
  - Folder/notebook readme has `titlePattern: "Topic *"` → open Add property's
    suggestions → the occupied `title_pattern` slot is omitted without a
    numbered substitute; note-only presets stay absent as today.
  - Note has separate `topic` and `url` rows → open `topic`'s key suggestions →
    occupied `url` is omitted; manually renaming `topic` to `url` keeps the
    current duplicate-key error and leaves saved content unchanged. Opening
    the `url` row's own suggestions still offers `url` when no other row uses it.
  - Existing note has a legacy `example of 2` → view/edit → it remains readable
    and retains its tracker. If no exact `example of` key exists, choosing that
    base preset adds it without silently consolidating the legacy row.
- **Proof boundary:** the mounted editor must exercise occupied preset selection
  and observe saved Markdown and literal option names, rather than deriving
  expected options from the generator under test. Extend the existing authoring
  journey for persistence through save/reload. These are refinement examples,
  not a slice plan or newly executed verification.
- **Current evidence (2026-10-01):** static inspection confirms Add property
  appends to an exact occupied list-capable key, while stored-row commit rejects
  duplicate exact keys. The shared preset component already receives the
  current row index for occupancy exclusion. The suffix parser remains used by
  structural-key recognition and note-level validation; it is not obsolete.
- **Open decisions:** none for the selected outcome. The owner accepted the
  occupied-preset rules on 2026-10-01 and requested slice planning. No new
  structural or rename behavior is required to achieve the authoring outcome.
- **Effort hypothesis:** S–M, medium confidence; the generator is shared, while
  the list append capability already exists.
- **Plan:** [Property key suggestions use meaningful base keys](../slice-plans/008-property-key-suggestions/PLAN.md).
- **Depends on:** per-value tracking and list append already delivered. Coordinate
  with the [shared property draft](../../docs/note-content-saving.md#rich-property-editing)
  key field at execution. Apply the same suggestion rules to the current Add
  property surface; this refinement changes no sibling story.
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
- Rich property drafts use the per-value rule for an existing key; see
  [rich property editing](../../docs/note-content-saving.md#rich-property-editing). The remaining numbered-key code is `propertyKeyBaseAndSuffix` and
  `nextAvailablePropertyKeyFor*` in `frontend/src/utils/noteContentPropertyKeys.ts`; the backend no longer
  creates numbered keys.
