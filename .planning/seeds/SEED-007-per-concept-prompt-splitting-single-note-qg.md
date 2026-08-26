---
id: SEED-007
status: dormant
planted: 2026-08-21
planted_during: /gsd-explore on focus-note retrieval structure for OKF / LLM Wiki
trigger_when: when QG quality plateaus, or when redesigning the retrieval→prompt pipeline (FocusContextMarkdownRenderer / question-generation prompt)
scope: medium
---

# SEED-007: Per-concept prompt splitting for single-note QG

## Why This Matters

Research on concept-grouped retrieval for question generation surfaced Savaal (arXiv 2502.12477), which splits the LLM call **per concept**: each call receives one main idea plus its top-k retrieved passages, and generates N/M questions per idea in separate calls. Savaal reports 6.5× better depth-of-understanding on dissertations and 1.5× on papers vs direct-prompting.

In Doughnut, the wiki links of the focus note ARE concept anchors (each linked note is an atomic concept node — see `.planning/notes/graph-structural-retrieval-as-implicit-concept-grouping.md`). So the per-concept split translates to: **one question-generation call per linked-note-group**, where each call receives the focus note plus the related notes reached through that one link, rather than one call with all related notes batched together.

This is a different prompt structure from the current single `<focus_context>` envelope that batches all related notes. It is the most concrete structural change the research points to for deepening QG.

## When to Surface

**Trigger:** QG quality plateaus (questions feel shallow, repetitive, or fail to test discrimination between the focus note and its neighbors); or when redesigning the retrieval→prompt pipeline (`FocusContextMarkdownRenderer`, the question-generation prompt assembly, or the recall-prompt generation service).

Also surface if the companion summarization finding (`.planning/notes/qg-retrieval-summarization-highest-leverage.md`) is acted on and the question becomes "summarize-then-batch vs summarize-then-split-per-concept."

## Scope Estimate

**Medium** — the retrieval service already produces per-link-grouped candidates (BFS frontier tracks the parent that proposed each note). The work is in the prompt-assembly layer: group related notes by their proposing link, render one envelope per group, and make one generation call per group. Requires evaluating whether the depth gain (Savaal's evidence) shows up at single-note scale, where Savaal is untested.

Key risk: Savaal's per-concept win **scales with document length**. At single-note scale the gain may be small or absent. This is exactly the open empirical question — spike before committing.

## Breadcrumbs

- `backend/src/main/java/com/odde/donut/services/focusContext/FocusContextRetrievalService.java` — `Proposal` / `retrievalPath` already track the proposing parent
- `backend/src/main/java/com/odde/donut/services/focusContext/FocusContextMarkdownRenderer.java` — single-envelope renderer; would need a per-group variant
- `docs/focus-context/focus_context_retrieval_design.md` — current retrieval design
- `.planning/notes/graph-structural-retrieval-as-implicit-concept-grouping.md` — why wiki links are concept anchors
- `.planning/notes/qg-retrieval-summarization-highest-leverage.md` — companion finding; summarization may be a stronger lever than splitting
- Savaal: [arxiv.org/html/2502.12477v1](https://arxiv.org/html/2502.12477v1), [github.com/mit-nms/savaal](https://github.com/mit-nms/savaal)

## Notes

Captured during `/gsd-explore` on focus-note retrieval structure. The seed is dormant until QG quality becomes a measured concern or the prompt pipeline is redesigned. Savaal's AI-judge was unaligned with human experts — any spike needs human evaluation, not auto-evaluation.
