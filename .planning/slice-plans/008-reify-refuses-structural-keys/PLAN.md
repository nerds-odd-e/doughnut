# Reify refuses structural keys

**Identity:** SEED-062#story-2
**Source:** [story](../../seeds/SEED-062-reify-property.md#story-2) — bounded retrospective correction of
SEED-062#story-1 ([plan](../006-reify-property/PLAN.md) while it exists; commits 508d4909..58bdfa2d on
`claude/reify-a-property`).

## Finding (current truth at 58bdfa2d)

- `NoteConstructionService.reifyPropertyIntoRelationshipNote` accepts any key whose value is one whole wiki link that
  resolves. On a relationship note (`type: Relationship`, `source: "[[Src]]"`, `target: "[[Other]]"`), reifying
  `source` creates "X source Src" and removes `source` from X, so X is no longer a well-formed relationship note
  (ADR 0004 shape).
- `RichFrontmatterPropertyRow.vue` computes `reifiable` from the value only, so the `source`/`target` rows of a
  relationship note show an enabled Reify.
- The backend already has one rule for keys that are not ordinary properties:
  `PropertyKeyNaming.isReservedStructuralKey` (type/relation/source/target, image, image mask, wikidata id, url, title
  pattern, question-generation instruction, note level); those keys are never indexed nor tracker-seeded.

## Goal and scope

Reify refuses a reserved structural key with a stated reason, and the row does not offer an enabled Reify for one.
Preserved: every SEED-062#story-1 promise (link-valued ordinary properties reify; plain text/list/missing/unresolved
refusals; trackers follow). Excluded: any change to which keys are structural.

## Decisive premises

| Premise | Operation that consumes it | Observation | Result |
| --- | --- | --- | --- |
| `isReservedStructuralKey` is the backend's single structural-key rule and covers relationship endpoints | slice 1 | read `PropertyKeyNaming.java` lines 97–118 | observed: includes `isRelationshipNoteStructuralPropertyKey` (type, relation, source, target) |
| The frontend has no one-to-one mirror of `isReservedStructuralKey`; `isScalarOnlyStructuralPropertyKey` (noteContentPropertyKeys.ts) covers type/relation/source/target, image, image_mask, wikidata id, title pattern, question-generation instruction, note level but not url | slice 2 | read `noteContentPropertyKeys.ts` lines 9–18; `rg isReservedStructural frontend/src` → no hit | observed; slice 2 decides whether to reuse it plus url or add the mirror beside it |

## Outside-in proof

| Promise | Owner | Observable proof |
| --- | --- | --- |
| Reifying `source` (or `target`) of a relationship note is refused with a message and changes nothing | slice 1 | `NoteReifyPropertyTests.ValueThatCannotBeReified` new case(s), red then green |
| The row of a structural key shows Reify disabled with a reason; an ordinary link property still reifies | slice 2 | `RichMarkdownEditor.propertyReify.spec.ts` new case, red then green; vue-tsc |

## Ordered slices

### 1. The server refuses reifying a structural key
Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '*NoteReifyPropertyTests*' -Dspring.profiles.active=test`.

Behavior: relationship note with `source: "[[Src]]"` → reify `source` → 400 "A structural property cannot be reified."
(wording may follow neighbouring messages); note content and note count unchanged. Check before the value checks, using
`PropertyKeyNaming.isReservedStructuralKey`.

### 2. The row says a structural key cannot be reified
Type: Behavior
Status: planned
Proof: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyReify.spec.ts`
and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`.

Behavior: panel open on `source: "[[Src]]"` of a relationship note → Reify disabled, reason visible, nothing sent; the
existing ordinary-link case still reifies. The reason text may differ from the non-link reason; keep one reason shown.

## Current decisions

- One rule, the backend's existing structural-key set; no new list of forbidden keys.
