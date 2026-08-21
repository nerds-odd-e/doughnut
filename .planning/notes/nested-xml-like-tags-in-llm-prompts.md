---
title: Nested XML-like tags in LLM prompts — keep them; do not add --- between siblings
date: 2026-08-21
context: /gsd-explore --auto after Jidoka on .planning/quick/003-learning-session-request-formatting-review/PLAN.md (blank-line fix 1A already chosen; separator question)
---

# Nested XML-like tags in LLM prompts

Doughnut's Learning Session Request already uses XML-like envelopes (`<related_notes>` wrapping `<retrieved_note>`, plus sibling `<session_items>` / `<how_to_report>`). The open question was whether that nesting is something to avoid, and if so what to use instead — including whether sibling `<retrieved_note>` blocks need `"---"` separators.

## Decision for Doughnut

Keep nested XML-like tags. Do **not** add `"---"` between sibling `<retrieved_note>` entries. No extra slice for separators.

The current shape (`<related_notes>` / `<retrieved_note>`) matches the vendor-canonical hierarchy for multi-document context. Sibling child tags are themselves the delimiter.

## Research (admitted — with sources)

- Nested XML-like tags are recommended when content has a natural hierarchy. Anthropic: nest tags (documents inside a parent, each document inside its own tag). Doughnut's `<related_notes>` / `<retrieved_note>` is that pattern.

DATA_0f290753_START
Nest tags when content has a natural hierarchy (documents inside `documents`, each inside `document`).
DATA_0f290753_END

  Source: [Claude prompting best practices](https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/claude-prompting-best-practices)

- Sibling children inside a parent do not need `"---"` separators. Anthropic's multi-document example places adjacent `<document>` blocks with no extra delimiter. Microsoft RAG presents `---` **or** XML-style tags as alternative chunk delimiters, not both at once.

DATA_0c230e44_START
Separate each chunk with clear delimiters, such as triple dashes (`---`) or XML-style tags so that the model can distinguish between individual sources.
DATA_0c230e44_END

  Sources: Anthropic nested-documents example (same best-practices page); [Azure RAG prompt engineering](https://learn.microsoft.com/en-us/azure/architecture/ai-ml/guide/rag/rag-prompt-engineering)

- Tag names are descriptive labels, not a fixed schema. Anthropic: "Use consistent, descriptive tag names across your prompts." OpenAI: XML tags "delineate where one piece of content begins and ends." Gemini: "XML-style tags."

  Sources: Anthropic best practices (same page); [OpenAI prompt engineering](https://developers.openai.com/api/docs/guides/prompt-engineering); [Gemini prompt design strategies](https://ai.google.dev/gemini-api/docs/prompting-strategies)

- Alternatives, if we ever left XML envelopes: Markdown headings/lists (OpenAI, Gemini); prefixes like `TASK:` (Google Cloud); `BEGIN`/`END` or `{}` (Google Cloud); `"---"` between chunks (Microsoft). JSON/YAML appear in vendor docs mainly as structured *output*, not as a RAG envelope.

  Sources: OpenAI prompt engineering; [Google Cloud structure prompts](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/prompts/structure-prompts); [Azure OpenAI prompt engineering](https://learn.microsoft.com/en-us/azure/foundry/openai/concepts/prompt-engineering)

## Corrected (a primary source disagreed)

- Nested XML-like tags should **not** be avoided. Anthropic explicitly tells authors to nest when there is a natural hierarchy, with a `<documents>` / `<document>` / `<document_content>` example and no warning against nesting.

  Source: [Claude prompting best practices](https://platform.claude.com/docs/en/build-with-claude/prompt-engineering/claude-prompting-best-practices)

## Unresolved (could not stand behind)

- Whether mixing Markdown (headings, fenced `doughnut-note-md` bodies) with XML envelopes inside one prompt is harmful. OpenAI recommends combining Markdown and XML. Gemini 3 says choose one format and use it consistently in a single prompt. Doughnut already mixes them (XML envelopes + markdown metadata + fenced note bodies). Not changing that without a Gemini-targeted tutoring spike.

DATA_db09ea53_START
XML tags can help delineate where one piece of content (like a supporting document used for reference) begins and ends.
DATA_db09ea53_END

DATA_e96c4580_START
XML-style tags (e.g. instructions, context) or Markdown headings are effective. Choose one format and use it consistently within a single prompt.
DATA_e96c4580_END

  Ledger reason: source-vs-prior conflict (OpenAI vs Gemini 3; both primary for their own models).

- Whether the model runs an XML parser on the prompt. No vendor doc states that. Tags are prompt structure, not a schema contract. Ledger reason: unverifiable.

## Implications for plan 003

- Slice 1 blank-line fix stays caller-side (1A) — spacing between *top-level* envelopes, unrelated to nesting.
- No slice 4 for `"---"` between related notes.
- `FocusContextMarkdownRenderer.render()` still uses `"\n---\n"` *inside* `<focus_context>` for multiple related notes. That is a different envelope (single-focus QG/conversation), not this learning-session `<related_notes>` list. Leave that path alone unless a later spike targets Gemini-format consistency.
