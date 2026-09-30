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
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal**

As a note author, I can turn a property into a relationship node so that I can
work with it as a relationship while retaining the property's existing trackers.

**Scope**

- Let the user select an existing property and turn it into a relationship node
  representing the same information.
- Any trackers attached to the property follow the resulting relationship node.
  Preserve their learner association, recall history, and current scheduling
  state, so reifying a property does not restart learning.
- A property without trackers can also be reified.
- Deferred promises: bulk conversion and reversing the conversion.

**Key examples**

- A note has a property describing its relationship to another concept, with
  no trackers → the author reifies that property → a relationship node expresses
  the same information.
- A property has an existing tracker with recall history and a scheduled next
  recall → the author reifies the property → that tracker follows the relationship
  node, retaining its history and next recall.
- Several learners have trackers for the property → the author reifies it →
  all existing trackers follow, retaining each learner's learning state.

## When to Surface

When selecting note property or relationship improvements from the backlog.

## Breadcrumbs

- Owner request: "Reify a property" lets a user turn a property into a relationship
  node; if the property has trackers, they should follow.
- Related assessment: the note properties UX UAT report, recoverable at
  `1986473b79:.planning/seeds/SEED-061-note-properties-ux-uat.md`; its follow-up fixes are in
  [SEED-064](SEED-064-note-properties-fixes.md#story-1).
