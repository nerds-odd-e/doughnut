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
Each value of a list property now has its own tracker. The conversion of existing
numbered keys is complete, as confirmed by the owner on 2026-10-02. The temporary
conversion code remains in application startup and can now be retired.

## Story

<a id="story-3"></a>

### Remove spent numbered-property migration after confirmed completion

**Identity:** SEED-063#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-retire-numbered-property-migration/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"9b353a3f46662125ec7c5cac83b626c1d03902c48ba1b6e00ea4b0abc9adc878","plan":"99b42283518af604b41300b727a0c35bcef073ff41cc297d2d66b625cabbc397"}}
```

- **Goal:** developers maintain ordinary application startup without spent
  one-time migration code after the numbered-property conversion is confirmed.
- **Scope:** remove the temporary startup caller and migration orchestration,
  migration-only helpers and proof harnesses, after confirmed completion in the
  deployed environments. Remove migration-only responsibilities from otherwise
  shared code without removing shared property naming, authored-frontmatter
  editing, property-list tracking, reference resolution or accepted-change
  responsibilities that continue to serve product behavior. Update affected
  documentation so it no longer describes the retired runner as active.
  Preserve stored consolidated content, accepted Git history, surviving tracker
  identities, schedules and learning data; retirement makes no further data
  conversion or history rewrite.
- **Key examples:**
  - Every deployed environment has confirmed that eligible legacy content,
    including Trash, is absent and no outstanding migration diagnostic remains
    → retire the temporary migration → subsequent ordinary startup no longer
    scans or converts numbered families, and existing consolidated content,
    accepted heads and learning state remain intact.
  - A migration reports an unrepresentable operation or operational completion
    has not been confirmed → consider retirement → removal remains blocked;
    passing unit fixtures or one successful startup alone cannot establish
    completion across the deployed environments.
  - After retirement, an author edits a multi-valued property and a learner uses
    its existing trackers → ordinary editing and learning → the shared product
    behavior still works with the existing tracker identities and history.
- **Prerequisite:** the numbered-property migration is delivered and its
  completion has been confirmed operationally. Unit fixtures alone do not
  satisfy this prerequisite. Application release and production observation
  remain separately authorized.
- **Completion confirmation:** on 2026-10-02 the owner stated, “I can confirm the
  migration is done,” and requested a slice plan. This satisfies the operational
  prerequisite for this cleanup. No independent production query or deployed
  revision was supplied or observed by this preparation. The removal gate comes
  from [the migration's operational contract](../../docs/numbered-property-migration.md).
- **Deferred promises:** releasing or observing production, repairing refused
  operations, changing conversion rules, adding an ongoing conversion service,
  or changing the schema/Flyway history. Shared recognition of authored legacy
  keys is not removed merely because the temporary converter is retired.
- **Origin:** the owner's 2026-10-01 instruction to select and queue cleanup
  when temporary Java migration code is introduced. Source:
  `b72298f0e01fddada636a1d45c20aed866c5fc59:.planning/seeds/SEED-063-track-property-values-separately.md#story-2`.
- **Effort hypothesis:** S; refine the actual removal boundary after confirmation.
- **Plan:** [Retire numbered-property migration](../slice-plans/001-retire-numbered-property-migration/PLAN.md).

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
