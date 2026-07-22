---
sketch: 001
name: notebook-home-layout
question: "Does notebook home layout read as command center vs note/folder?"
winner: "C"
tags: [layout, notebook, home, index]
---

# Sketch 001: Notebook home layout

## Design Question

Does the notebook main column read as an **overview / command center** (Notion workspace home) — distinct from note (document) and folder (admin) — while keeping **indexContent** + properties as the primary job?

## How to View

```bash
open .planning/sketches/001-notebook-home-layout/index.html
```

## Variants

- **A: Hero + index canvas** — Notion-like hero (`notebook.name`, blurb), light property pills, index as primary canvas; settings behind a gear.
- **B: Canvas + property rail** — Index fills the main column; properties + description + management live in a persistent right rail.
- **C: Home / Settings tabs** — Default **Home** = index + properties; **Settings** holds today’s management / settings / search-indexing cards.

## What to Look For

1. At first glance, can you tell this is **not** a note page and **not** today’s settings stack?
2. Is **index** obviously the thing to read/edit?
3. Do demoted settings still feel reachable without stealing the home?
4. Which pattern fits Doughnut’s existing sidebar chrome best?

## Product fields shown

`notebook.name`, card `description`, `indexContent`, `title_pattern`, aliases, `skipMemoryTrackingEntirely`, Move / Share / Attach book / search index actions.
