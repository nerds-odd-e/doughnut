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

Property labels show on the web References section and the focus note's
`inboundReferences` only. Showing them in the Markdown focus context, retrieval
path steps, outgoing links or attachment page references, and grouping,
filtering or sorting references by property type, are not yet stories.

## When to Surface

Take up these stories when improving the evidence returned with note references.
See the [related learning and retrieval seed](SEED-073-learning-refinement-and-retrieval.md).
