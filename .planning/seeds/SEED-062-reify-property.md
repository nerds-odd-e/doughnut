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

<a id="story-2"></a>

### Reify refuses structural keys

**Identity:** SEED-062#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/008-reify-refuses-structural-keys/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"25c537bc060b7feada1b43f6feca0159e38d3b83ab16277189ce8223db6a11c4","plan":"70517cd228d8c303458d4fe4d460a21a8b0d7204d6c583f44023dc6b258b9daa"}}
```

**Goal**

As a note author, I cannot break a relationship note, or turn a structural key into a relationship, by reifying one of
its structural keys; the property row says why instead.

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
