---
id: SEED-049
status: dormant
planted: 2026-09-27
planted_during: owner-requested backlog capture after SEED-047#story-1
trigger_when: selected from the product backlog
scope: medium
---

# SEED-049: Give the frontend note store a cohesive architecture

## Why This Matters

`frontend/src/store/StoredApiCollection.ts` holds most note-changing behavior
in one class: loading and caching note realms, note creation, text and content
edits, image upload, wiki-link cache refresh, undo, trash, permanent deletion,
removal landing, relationship reduction, and moves. It was about 380 lines
before SEED-047#story-1 and about 403 after it, well over the project's
250-line file guide. Each new note behavior lands there by default, so
unrelated concerns keep accumulating in one place.

## Story Decomposition

<a id="story-1"></a>

### Give the frontend note store a cohesive architecture

**Identity:** SEED-049#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** Developers and AI agents changing note behavior in the web frontend
find each note concern (for example editing, undo, removal and landing,
placement and moves) in one cohesive, domain-named place, so a change to one
concern does not grow or touch an unrelated one. `StoredApiCollection.ts`
and its successors stay within the 250-line file guide, with product behavior
unchanged.

**Refinement and planning need careful architectural design (owner request,
2026-09-27):** do not refine this story into a mechanical file split. Before
slice planning, design the target structure: the domain concepts and their
boundaries, what stays shared (note storage, router, undo history), how
components and composables reach each concern, and how tests keep observing
behavior through high-level entry points. Check accepted ADRs and the North
Star, run a Proudly Found Elsewhere search for existing seams, and record
the chosen design and rejected alternatives in the seed before planning.

## When to Surface

Owner priority: first in the product backlog as of 2026-09-27.

## Breadcrumbs

- Owner request, 2026-09-27, after executing SEED-047#story-1: add the
  `StoredApiCollection.ts` improvement to the product backlog as its own
  story with the highest priority; it needs a more careful architectural
  design to refine and plan it.
- The refactor passes for SEED-047#story-1 flagged the file size and
  recorded the split as out of that story's scope (plan 014 learnings).
