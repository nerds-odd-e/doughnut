---
id: SEED-062
status: dormant
planted: 2026-09-30
planted_during: owner request to capture Reify a property as a product backlog story
trigger_when: enabling users to turn note properties into relationship nodes
scope: small
---

# SEED-062: Reify a property

## Why This Matters

A note author may want to express an existing property as a relationship node.
Changing that representation should preserve its meaning and any learning
already recorded against the property.

## Story

<a id="story-1"></a>

### Reify a property

**Identity:** SEED-062#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/006-reify-property/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4826b09ae98e8edf84c063f1df910be591fb72d70704d065afe0ac7dbb7d868e","plan":"aa85e518f2a96ac87a2f046128754e61238cf89510208e34dd477cc1b6b3505a"}}
```

**Goal**

As a note author, I can turn a property into a relationship node so that I can
work with it as a relationship while retaining the property's existing trackers.

**Scope**

- Let the user select an existing property and turn it into a relationship node
  representing the same information: the source is the note holding the property,
  the relation is the property key, and the target is the note its wiki-link value
  names. The new relationship note is created in the same notebook and the same
  folder as the source note.
- Only a property whose value is a wiki link to a note can be reified. A plain-text
  value cannot; that limit is stated to the user rather than failing silently.
- Reifying replaces the property: the property is removed from the source note and
  the relationship note holds the information.
- Any trackers attached to the property follow the resulting relationship node,
  which becomes the tracked note. Preserve their learner association, recall history, and current scheduling
  state, so reifying a property does not restart learning.
- A property without trackers can also be reified.
- Deferred promises: bulk conversion and reversing the conversion.

**Key examples**

- A note has a property describing its relationship to another concept, with
  no trackers → the author reifies that property → a relationship note in the same
  notebook and folder expresses the same information, and the property is gone
  from the source note.
- A property whose value is plain text, not a wiki link → the author tries to
  reify it → nothing changes and the author is told why it cannot be reified.
- A property has an existing tracker with recall history and a scheduled next
  recall → the author reifies the property → that tracker follows the relationship
  node, retaining its history and next recall.
- Several learners have trackers for the property → the author reifies it →
  all existing trackers follow, retaining each learner's learning state.

<a id="story-2"></a>

### Reify refuses structural keys

**Identity:** SEED-062#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-reify-refuses-structural-keys/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"48c27d24a27a1d43b01ae9c775ed25d8a2e847c34297d8a9b872f5c8fc1072ba","plan":"86f9f362284ef46887543741148902a738553a53e0e19e289d873d1b9cb1fc14"}}
```

**Goal**

As a note author, I cannot break a relationship note, or turn a structural key into a relationship, by reifying one of
its structural keys; the property row says why instead (correction of SEED-062#story-1).

**Scope**

- Reify is refused for a reserved structural frontmatter key (relationship `type`, `relation`, `source`, `target`, and
  the other keys the backend already treats as structural rather than properties), even when its value is one link.
- The property row does not offer an enabled Reify for such a key and states why.
- No other reify behavior changes. Plan: [008-reify-refuses-structural-keys](../slice-plans/008-reify-refuses-structural-keys/PLAN.md).

## When to Surface

When selecting note property or relationship improvements from the backlog.

## Breadcrumbs

- Owner request: "Reify a property" lets a user turn a property into a relationship
  node; if the property has trackers, they should follow.
- Related assessment: the note properties UX UAT report, recoverable at
  `1986473b79:.planning/seeds/SEED-061-note-properties-ux-uat.md`; its follow-up fixes are in
  [SEED-064](SEED-064-note-properties-fixes.md#story-1).
