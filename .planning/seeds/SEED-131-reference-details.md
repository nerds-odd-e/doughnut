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

<a id="see-link-property-types-in-references"></a>

### See link property types in references

**Identity:** SEED-131#see-link-property-types-in-references
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/100-see-link-property-types-in-references/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"880b8fbf6d5e3b7d2f1bc1bdc687e380ad35b8046851f46223621921110be3f1","plan":"20191c9a80854f46efd860cfc3d5fd5e9f683577c79056de26c4b2fbb85c6b90"}}
```

**Goal.** A person reading a note on the web, or a connected assistant reading
that note's focus context, sees which property of each referring note holds the
link. They can tell the relationship the author named without opening the
referring note. This serves the backlog direction of retrieving the right
evidence for people and connected assistants.

**Scope.**

Required behavior:

- Each entry in a note's References section shows the property types of the
  referring note's properties whose value links to this note. The section is
  the same one on the note page and in note context.
- Each entry in the focus note's `inboundReferences` list of the structured
  focus context (MCP `get_note_graph` and `GET /notes/{id}/graph`) carries the
  same property types. The entry keeps the link string it has today, unchanged,
  so a consumer can still follow it.
- A property type is the key without its numeric suffix, as elsewhere in the
  product (`prerequisite 2` is `prerequisite`). Each distinct type appears once,
  in the order the referring note authors it.
- The label reads as the referring note's property. Property keys are free text
  with no inverse wording, so `prerequisite` beside Source must not read as
  "Source is a prerequisite of this note". A reversed reading would defeat the
  goal. In the structured result the field name and the tool description say
  the same.
- Labels come from the property index and the cached authored references. No
  referring note's content is loaded or parsed for a label. The index is rebuilt
  with every accepted content change, so the label follows the source property.
- A reference with no such property shows no label and is otherwise unchanged.

No new restriction: which references appear, their order, the caps and the
sampling stay as they are. A label appears only on a referring note the viewer
already sees, whose content the viewer can already read.

Scope limits (deferred, not prevented):

- A property value holding more than one link labels only its first link; the
  index associates one reference with each value.
- Keys the index leaves out give no label: `image`, `image_mask`,
  `wikidata_id`, `url`, `title_pattern`, `question_generation_instruction`,
  `note_level`, `type`, `relation`, `source`, `target`, `tags`, `aliases`,
  `overlaps`, `cssclasses`, and list items longer than 255 characters. A
  relationship note's title already names its relation.
- The Markdown focus context (export, question generation, conversation) is
  unchanged. It does not list inbound references, and a retrieved referring
  note carries its own frontmatter.
- Retrieval path steps and outgoing links get no label.
- References on an attachment page are unchanged.
- No grouping, filtering or sorting by property type.

**Key examples.**

1. Source has `prerequisite: "[[Target]]"`. A reader opens Target → the
   References section shows Source with `prerequisite`. An assistant calls
   `get_note_graph` for Target → the `inboundReferences` entry for Source holds
   its link string and the property type `prerequisite`.
2. Source links to Target only in its body text → both surfaces list Source
   with no label.
3. Source links to Target from `prerequisite`, `prerequisite 2` and `related` →
   Source appears once with `prerequisite` and `related`.
4. Source links to Target from `prerequisite` and again in its body text →
   Source appears once with `prerequisite`.
5. Source's key `prerequisite` is renamed to `follows` and the change is
   accepted → Target's References shows `follows`. The link is then moved from
   the property to the body text → the label is gone.
6. Boundary: Source has `see also: "[[Other]] and [[Target]]"` → Other shows
   Source with `see also`; Target shows Source with no label (first deferred
   limit above).

**UI.** On each reference card the property types appear as secondary text,
smaller than the note title, placed and worded so they belong to the referring
note. Several types sit together on the one card. A long key is shortened on
screen and does not break the card. The collapsed count is unchanged. The
later excerpt story adds its text to the same card.

**Architecture.** The inbound-reference result that already pairs a referring
note with its authored link texts is the one place that gains property types;
the References section and the focus context both read it. Two API contracts
change. A References entry needs its own shape, because `NoteTopology` is
shared by every card list. `inboundReferences` entries change from strings to
structured entries, which connected assistants will see; no frontend or CLI
code reads that list at revision 7445adb853. The generated client and the
`get_note_graph` description change with them. No schema change: the index
column and its key exist. No Accepted ADR conflicts: the vocabulary of ADR 0001
is used as it stands and ADR 0004's Markdown format is untouched.

**Considered.** Grouping references under property-type headings repeats a
source that uses two properties, needs a heading for unlabeled references, and
does not fit the per-reference excerpt that follows. Relying on the referring
note's own content keeps today's need to open it. Parsing each referring note
at read time breaks the cache constraint.

- **Effort hypothesis.** M, medium confidence.
- **Safe stopping point.** This story stands alone. Readers can identify named
  relationships on both surfaces, within the existing reference visibility rules.

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

Start with property types because the existing index already associates property
keys with references. It is the simpler way to see whether the detail is clear
in both places. Defer excerpts first if capacity is tight. Neither story has a
product prerequisite. If refinement finds that retrieval needs a separate rollout,
revisit the story boundary.

## When to Surface

Take up these stories when improving the evidence returned with note references.
See the [related learning and retrieval seed](SEED-073-learning-refinement-and-retrieval.md).
