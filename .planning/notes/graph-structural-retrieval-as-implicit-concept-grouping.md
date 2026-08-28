---
title: Graph-structural retrieval as implicit concept-grouping
date: 2026-08-21
context: /gsd-explore on retrieval algorithm and structure for OKF / LLM Wiki focus-note context
---

# Graph-structural retrieval as implicit concept-grouping

## The reframe

Research into how PKM tools structure retrieval for AI consumption (Smart Connections, NotebookLM, Heptabase, Tana, Savaal, Quizard) surfaced three theories of what gives an LLM "grasp" of a focus note:

1. **Graph-structural** — where the note sits (Donut's current approach: BFS over wiki links).
2. **Semantic** — what the note resembles (embedding-first, the dominant field norm).
3. **Concept-grouped** — what the note is about (Savaal/Quizard: extract concepts, retrieve per concept, generate per concept).

The key insight for Donut: **in a zettelkasten, wiki links ARE concept anchors.** Each linked note is an atomic concept node with a descriptive title — that's the zettelkasten convention. So Savaal's "extract concepts → retrieve per concept" *collapses* in Donut: the focus note's linked notes already *are* the concept groups.

**Donut's BFS-over-wiki-links retrieval is already doing implicit concept-grouped retrieval.** The question was never "should we switch to concept-grouped" — we're already there. The real design choices are two different ones (see the companion note on summarization, and SEED-007 on prompt splitting).

## What this means for the retrieval structure

- The graph-structural approach is **defensible**, not idiosyncratic-in-a-bad-way. The field is fragmented between embedding-RAG, user-curated scope, and concept-grouped retrieval; no de-facto standard exists for "focus note context windows" as of 2026.
- OKF v0.2 is a *format* spec (markdown + YAML frontmatter, `type` required, file path = identity, links = graph). It deliberately says nothing about context windows, retrieval depth, or envelopes — that's producer's choice.
- No surveyed tool exposes "how a note was reached" (edge type / reason) to the model. Retrieval reason is a UI affordance everywhere, never a model input. Donut's focus-context payload matches that: related notes carry depth and path, not an edge label.
- Donut's `<focus_context>` XML envelope with per-note Title/Notebook/Folder/Depth metadata is **more structured than any tool surveyed** (NotebookLM uses plain text with citation markers; Heptabase/Tana expose context via MCP tools returning JSON the client renders). This is a deliberate design choice, not a debt.

## Sources

- Smart Connections — embedding-first, no link traversal as primary retrieval ([github.com/brianpetro/obsidian-smart-connections](https://github.com/brianpetro/obsidian-smart-connections/))
- Logseq semantic search — bounded local context per block, vector similarity as auxiliary signal ([logseq PR #12710](https://github.com/logseq/logseq/commit/fd1906a0c84135f30c34ae84a20876cc46057dbe))
- Savaal (arXiv 2502.12477) — per-concept prompt splitting for QG ([arxiv.org/html/2502.12477v1](https://arxiv.org/html/2502.12477v1))
- ConQuer / Quizard — concept extraction at index time ([arxiv.org/abs/2503.14662](https://arxiv.org/abs/2503.14662), [github.com/timothyckl/quizard-generator](https://github.com/timothyckl/quizard-generator))
- OKF v0.2 spec — format only, no retrieval contract ([github.com/.../okf/SPEC.md](https://github.com/googlecloudplatform/knowledge-catalog/blob/main/okf/SPEC.md))
