---
id: SEED-057
status: dormant
planted: 2026-09-29
planted_during: owner request to allow relationship reduction for a simple wiki-link sentence
trigger_when: improving relationship reduction for notes containing only a relationship sentence
scope: small
---

# SEED-057: Reduce relationship notes containing only a relationship sentence

## Why This Matters

A learner reducing a relationship note to a source property is currently
blocked when the body is not empty. The owner wants a body consisting only
of one wiki link, some words, another wiki link, and a final period to be
ignorable, so that this sentence does not prevent reduction.

## Alternatives and Decision

The owner chose an exception to the empty-body requirement for this specific
sentence form. Manually deleting the sentence first adds unnecessary work.
Other non-empty bodies retain the existing protection against content loss.

## Story Decomposition

One story extends the existing relationship reduction behavior.

<a id="story-1"></a>

### Reduce relationship notes whose body is a simple wiki-link sentence

**Identity:** SEED-057#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/048-reduce-legacy-relationship-sentence/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f9b14fdb83c9aa013d5afdabb0b9a73440a9c7c5f4e89b576b7dcfc44b0f7c93","plan":"d2a73cef8071b1b6d9d3ad70ac43a8178a271c60de54c8e98a2d20817df42288"}}
```

**Goal**

Learners can reduce a relationship note to a source property when its entire
body has the form `[[first link]] some words [[second link]].`.

**Scope**

- This is a temporary accommodation for existing legacy note data; keep it
  extremely narrow (owner, 2026-09-29).
- Match on the sentence shape only; the two links need not name the
  relationship's own source and target (owner, 2026-09-29).
- Treat that entire-body sentence form as ignorable for the existing
  relationship reduction content check, alongside the already-supported empty body.
- Ignore and discard the matching body when reduction proceeds; use the existing
  reduction behavior to create the source property and remove the relationship note.
- Preserve the remaining reduction checks and effects, including relationship
  metadata, source authorization, property naming, and memory tracker handling.
- Preserve the current refusal for other non-empty body content; a matching
  sentence embedded in additional content does not make that content ignorable.

**Key examples**

- A reducible relationship note has body `[[Moon]] is a part of [[Mars]].`
  → the learner chooses reduction → the sentence is ignored, the source property
  is created, and the relationship note is removed.
- The relationship body contains the same sentence followed by
  `Observations from orbit.` → the learner chooses reduction → the existing
  non-empty-content protection still applies.
- The relationship body is empty → the learner chooses reduction → the existing
  reduction behavior remains available.

- **For / why:** Learners avoid clearing a simple relationship sentence by hand.
- **Evaluation:** Reduce a note containing only the specified sentence and
  observe the same property creation and note removal as an empty-body reduction.
- **Effort hypothesis:** S (30–60 minutes), medium confidence; confirm the existing
  wiki-link parsing and reduction checks before planning.
- **Depends on:** No queued prerequisite is established.
- **Safe stopping point:** This exception is useful independently of other
  relationship improvements, with other body content still protected.

## Ordering and Scope Reduction

Append to the current backlog before the empty-content warning story, following
the owner's request order. Limit the exception to the specified sentence form.

## When to Surface

When selecting relationship editing and reduction improvements.

## Breadcrumbs

- Owner request on 2026-09-29: ignore relationship content in the form of one
  wiki link, some words, another wiki link, and a period when reducing the node.
- Current body check: `backend/src/main/java/com/odde/donut/services/NoteReferenceHandling.java`.
- Existing product examples: `e2e_test/features/relationships/relationship_edit_and_remove.feature`.
