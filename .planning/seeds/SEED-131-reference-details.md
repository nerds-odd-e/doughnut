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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/103-label-every-property-link-in-references/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0e279e855e3c01761afb5eba4cd14e239ce8ee4c5f5104be3d9d11f26271fb0e","plan":"3ed6e67d56cfac43cc6bd8bb6b1e12540879c6e65cb23834580fea7af335b3f3"}}
```

**Goal.** A note reader or connected assistant sees, for each reference, every
property of the referring note that holds the link, so they can tell the
authored relationship without opening the referring note. The label is read
from the referring note's own text, so the property a link sits in is named
wherever in the frontmatter it was written. This advances the seed's aim of
showing enough of the authored reference to judge whether to follow it.

**Scope.**

- Required behavior. A reference names the property type of every property of
  the referring note whose value holds a link to the target. This holds for any
  key, for a scalar or a list item of any length, and for any link in a value
  that holds several. The web References section (note page and note context)
  and the retrieval focus note's `inboundReferences` (`get_note_graph` and
  `GET /notes/{id}/graph`) carry the same types.
- Unchanged. Property types stay distinct, in the referring note's authored
  key order, with the numeric suffix dropped and the example-of family
  canonical. The card line stays "via its …". Which references appear, their
  order, the caps and the sampling stay as they are. A referring note that
  links to the target only from its body shows no label.
- Required restriction (owner decision, 2026-11-17). A relationship note gives
  no label for its `source` and `target`, on the web and in retrieval. Its
  title already names the relation and both ends, and on the owner's
  most-referenced note 97 of 125 reference cards are relationship notes, so
  the line would add height to most cards without information. The same keys
  on an ordinary note are labelled.
- Scope limits, each deliverable on its own later: the Markdown focus context
  (it renders no inbound references today), retrieval path steps, outgoing
  links, attachment page references, and grouping, filtering or sorting
  references by property type. Body-text evidence belongs to
  [See surrounding text in references](#see-surrounding-text-in-references).

**Key examples.**

1. Several links in one value. Source has
   `see also: "[[Other]] and [[Target]]"` → Other's and Target's References
   both show Source with "via its see also", and Target's `inboundReferences`
   entry for Source carries `referrerPropertyTypes: ["see also"]`.
2. A key the property index leaves out. Further has
   `overlaps: ["[[Farther]]"]` → Farther's References show Further with
   "via its overlaps".
3. A long list item. Source has a `notes` list item longer than 255 characters
   that contains `[[Target]]` → Target's References show Source with
   "via its notes".
4. A relationship note. "Lithos tagged by Greek" has `type: Relationship`,
   `source: "[[Lithos]]"`, `target: "[[Greek]]"` → Greek's and Lithos's
   References show it with no label, and its `inboundReferences` entries carry
   no `referrerPropertyTypes`.
5. The same key on an ordinary note. Article has `source: "[[Book]]"` and no
   `type: Relationship` → Book's References show Article with "via its source".
6. Only the property that holds this link. Source has
   `prerequisite: "[[Other]]"` and links `[[Target]]` from its body → Target's
   References show Source with no label; Other's show "via its prerequisite".
7. Several properties. Source has `prerequisite: "[[Target]]"`,
   `prerequisite 2: "[[Target]]"` and `related: "[[Target]]"` → "via its
   prerequisite, related".

**Evidence (2026-11-17).** Counted in the owner's 日本語学習 and English notebooks
at their accepted history that day (11,369 notes; a script over the cloned
Markdown, matching link text to titles approximately):

- 0 of 6,865 link-holding indexed property values hold more than one link, and
  no list item with a link is longer than 255 characters. Examples 1 and 3
  follow from the rule; they were not seen in this content.
- Left-out keys that hold links: `source` and `target` of 5,143 relationship
  notes (about 10,500 reference entries, roughly 60% of all), and `overlaps`
  (39 values). No other left-out key holds a link.
- 276 targets have ten or more references; 205 of them mix label kinds, while
  the list is ordered by note id. This is an observation for the deferred
  grouping and sorting, not a commitment.
- Other owners' notes were not counted; that needs a production database
  query.

**Architecture.** Stored note Markdown is authoritative
([ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)),
and the reference index holds one row per distinct authored link of a note,
which says nothing about where the link sits. The label is therefore derived
when references are read: for each referring note already found by live
resolution, its frontmatter is parsed with the existing authored-reference
parser, and a property is named when its value holds a link with the same
identity as a reference that resolved to the target. The referring note's
content is already loaded at that point. The property index keeps its one job,
value identities for recall trackers, with its schema unchanged; labels do not
read it. The alternative of making the property index associate every link
with its property was rejected: it changes a table whose rows are tracker
value identities, and still leaves the left-out keys unlabelled. One quality
to confirm in planning: reading labels for a note with the most references
seen (125) stays as responsive as today.

- **Effort hypothesis.** S, medium confidence. One backend rule replaces the
  index lookup; both surfaces already carry the types.
- **Safe stopping point.** Each scope limit can be delivered on its own.

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
