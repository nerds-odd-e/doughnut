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
- **Prerequisite:** the numbered-property migration is delivered and its
  completion has been confirmed operationally. Unit fixtures alone do not
  satisfy this prerequisite. Application release and production observation
  remain separately authorized.
- **Origin:** the owner's 2026-10-01 instruction to select and queue cleanup
  when temporary Java migration code is introduced. Source:
  `b72298f0e01fddada636a1d45c20aed866c5fc59:.planning/seeds/SEED-063-track-property-values-separately.md#story-2`.
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
