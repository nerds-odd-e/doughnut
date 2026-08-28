---
title: QG retrieval — summarization is the highest-leverage step
date: 2026-08-21
context: /gsd-explore on retrieval algorithm and structure for OKF / LLM Wiki focus-note context
---

# QG retrieval — summarization is the highest-leverage step

## The finding

ConQuer (which Quizard adapts) ran an ablation on its question-generation pipeline. The single most critical factor was **the summarization step** — feeding raw retrieved content caused the biggest score drop because the model was "overwhelmed by excessive information." Summarization beat structural reorganization of the context.

Savaal's results corroborate this from a different angle: concept-grouped retrieval beats whole-document retrieval on **depth of understanding** (6.5× better on dissertations, 1.5× on papers), but both score ~90% on **clarity**. The win is in conceptual depth and coverage, not coherence. The depth win scales with document length.

## What this means for Doughnut

Doughnut's current `<focus_context>` envelope feeds **raw note bodies** in fenced `donut-note-md` blocks, bounded by a token budget (~2500 tokens for related notes). The research suggests the highest-leverage change to improve QG quality is **not** restructuring the envelope or reorganizing which notes are selected — it is adding a **summarization pass** before generation, so the model receives compressed, signal-dense context rather than raw note bodies it has to compress itself.

This is a stronger lever than:
- Switching from graph-structural to embedding-based retrieval (the field norm, but not obviously better for a zettelkasten where links are deliberate).
- Splitting the prompt per linked-note-group (Savaal-style — see SEED-007; pays off mainly at document scale, untested at single-note scale).
- Adding more metadata to the envelope (Doughnut is already the most structured in the field).

## Caveats

- ConQuer's evaluation was small-scale (3 quizzes). Savaal's was also small-scale.
- Savaal found its GPT-4o AI-judge was **unaligned with human experts** despite heavy prompt engineering — "calls into question the wisdom of using only AI judges." Relevant if we ever auto-evaluate QG quality; human evaluation is needed for any real comparison.
- Concept extraction misses **implicit** concepts (ConQuer: a plant/sunlight question missed "photosynthesis"). A summarization pass could either help (by surfacing implicit concepts) or hurt (by dropping them) — design choice.

## Open question

Whether a summarization pass helps at **single-note** scale (Doughnut's case) vs **large-document** scale (Savaal/ConQuer's case) is untested. At single-note scale the focus note itself is already atomic; the question is whether summarizing the *related notes* before generation improves QG. This is the most direct spike available.

## Sources

- ConQuer ablation — summarization step most critical ([aclanthology.org/2025.naacl-srw.9.pdf](https://aclanthology.org/2025.naacl-srw.9.pdf))
- Savaal — depth vs clarity split, scales with document length ([arxiv.org/html/2502.12477v1](https://arxiv.org/html/2502.12477v1))
- Savaal AI-judge misalignment with human experts ([arxiv.org/html/2502.12477v1](https://arxiv.org/html/2502.12477v1))
