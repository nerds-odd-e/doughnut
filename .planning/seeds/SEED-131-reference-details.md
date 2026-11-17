---
id: SEED-131
status: dormant
planted: 2026-11-17
planted_during: product backlog shaping for note references and retrieval
trigger_when: improving the evidence shown with references in notes and retrieval
scope: medium
---

# SEED-131: See why another note references this one

## Why This Matters

People reading a Donut note or using retrieved notes as evidence can see which
notes reference it. To tell what relationship the author meant or how the
author used the link in prose, they need to open the source note and find the
link. Show enough of the authored reference in both places to judge whether to
follow it. This advances the backlog direction of retrieving the right evidence
for people and connected assistants.

## Alternatives and Decision

Two stories each carry one kind of evidence through the note view and retrieval.
The property type names an authored relationship. The prose excerpt shows how
the author used the reference in context. Each gives readers something they can
use as soon as it ships.

The smaller change would add both details to the note view and put retrieval in
a third story. It would help people browsing notes sooner. Until the third story
shipped, retrieval consumers would still have to open the source notes to find
either detail. Deferring all changes leaves that manual workflow in both places.

## Story Decomposition

<a id="label-every-property-link-in-references"></a>

### Label every property link in references

**Identity:** SEED-131#label-every-property-link-in-references
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A note reader or connected assistant can see the authored
  property behind every reference, including the cases the first
  property-type labels leave out.
- **Evaluation.** Source has `see also: "[[Other]] and [[Target]]"` → both
  Other's and Target's References show Source with `see also`, and so does
  Target's `inboundReferences` entry.
- **Boundary.** Deferred from the delivered property-type labels: a property
  value holding more than one link labels only its first link, because the
  property index associates one reference with each value; keys the index
  leaves out (`image`, `image_mask`, `wikidata_id`, `url`, `title_pattern`,
  `question_generation_instruction`, `note_level`, `type`, `relation`,
  `source`, `target`, `tags`, `aliases`, `overlaps`, `cssclasses`, and list
  items longer than 255 characters) give no label; the Markdown focus context,
  retrieval path steps, outgoing links and attachment page references carry no
  label; references are not grouped, filtered or sorted by property type.
  Refinement selects which of these give readers value; a relationship note's
  title already names its relation.
- **Effort hypothesis.** S to M, low confidence. Labelling every link in a
  value likely needs the index to associate each link with its property.
- **Safe stopping point.** Each deferred case can be delivered on its own.

<a id="see-surrounding-text-in-references"></a>

### See surrounding text in references

**Identity:** SEED-131#see-surrounding-text-in-references
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why.** A note reader or retrieval consumer can see how a source note
  used a link in its body text and judge whether to follow it.
- **Evaluation.** A source note links to the target from the body text. The
  target's References section shows a few words before and after the link in
  smaller gray text. When retrieval includes that reference, the result includes
  the same authored excerpt.
- **Boundary.** The excerpt comes from a body-text occurrence. If the source
  note references the target both from a property and from body text, show the
  property type and the excerpt once both stories are delivered. Keep excerpts
  within existing visibility rules and retrieval limits.
- **Effort hypothesis.** M, low confidence. The existing lookup identifies the
  referrer. Confirm how to recover a short excerpt where the same link occurs
  more than once.
- **Safe stopping point.** This story can deliver body-text evidence on both
  surfaces independently of property types.

## Ordering and Scope Reduction

Neither story has a product prerequisite. If refinement finds that retrieval
needs a separate rollout, revisit the story boundary.

## When to Surface

Take up these stories when improving the evidence returned with note references.
See the [related learning and retrieval seed](SEED-073-learning-refinement-and-retrieval.md).
