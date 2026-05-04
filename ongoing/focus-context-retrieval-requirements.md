# Focus Context Retrieval Requirements

## Purpose

Replace the existing GraphRAG mechanism with a new **Focus Context Retrieval** mechanism aligned with Doughnut's wiki/Obsidian-compatible note model.

The mechanism retrieves a bounded, AI-oriented context around a single focus note. It is used by AI features such as question generation, assistants, and note understanding workflows.

The retrieval is **focus-centered**, not query-centered. There is no search phrase. The focus note itself is the retrieval seed.

## Background

Doughnut has migrated from a tree-structured note model to a wiki-compatible model:

- Notes live inside notebooks.
- Notes are Markdown files.
- Notes may be organized in folders.
- Note titles cannot be duplicated within the same folder.
- Notes may contain YAML front matter.
- Links use double-square-bracket wiki syntax, such as `[[Note Title]]`.
- Cross-notebook links use a notebook prefix, such as `[[Notebook: Note Title]]`.

Because of this migration, the old GraphRAG design based on parent/child/relationship-note expansion is no longer appropriate.

## Name

The new mechanism should be called:

**Focus Context Retrieval**

Suggested code names:

- `FocusContextRetrievalService`
- `FocusContextRetriever`
- `FocusContextResult`
- `FocusContextMarkdownRenderer`

Eventually, remove the name `GraphRAG` completely.

## Core Concept

Focus Context Retrieval builds a compact note neighborhood around a focus note by traversing Doughnut's wiki-style graph.

The graph consists of:

- Outgoing wiki links discovered in Markdown note details and YAML front matter.
- Inbound wiki references from the wiki title cache.
- Bounded folder-scoped peer samples.

Traversal is recursive up to the configured maximum depth. Notes reached at one depth may become expansion frontiers for the next depth, subject to edge priority, deduplication, and token budget.

Folder sibling notes may be added as context, but they must not expand further. However, if another reached note becomes an expansion frontier through a wiki link or inbound reference, that note may also get its own folder sibling sample added when the traversal reaches it and budget allows.

The result must fit within a token budget.

## Retrieval Model

### Retrieval Seed

The retrieval seed is always the focus note.

There is no user search phrase in the default retrieval process.

### Traversal Style

The retrieval algorithm should use **focus-centered weighted breadth-first traversal**.

Requirements:

- Start from the focus note at depth `0`.
- Expand outward through outgoing wiki links and inbound wiki references.
- Traverse breadth-first by depth.
- Within each depth, prioritize stronger edge types before weaker ones.
- Stop when the token budget is exhausted.
- Stop when the configured maximum depth is reached.
- Deduplicate notes by internal note identity.
- Avoid revisiting the same note through repeated wiki URIs or aliases.

### Depth

The default maximum depth must be at least `3`.

Rationale:

Many Doughnut notes now represent relationships between two other notes using ordinary Markdown and front matter links. A shallow traversal may retrieve only the relationship-description note itself, without retrieving the notes that contain the actual details.

Example shape:

```text
Focus Note
  -> Relationship Note
      -> Source Note
      -> Target Note
          -> Further Explanation Note
```

defaults:

- Default max depth: `2`
- But developer can change it in code
- Token budget remains the hard stopping constraint.

## Edge Types

The system should internally recognize typed relationships between notes.

Initial edge types:

- `OutgoingWikiLink`
- `InboundWikiReference`
- `FolderSibling`

`OutgoingWikiLink` includes wiki links found in both the Markdown body and YAML front matter. Front matter links are not a separate edge type unless a later use case needs that distinction.

## Prioritization

Traversal proceeds breadth-first from the focus note:

0. Focus note itself.
1. Outgoing wiki links.
2. Inbound wiki references.
3. Folder sibling sample.

Folder siblings are useful, especially for question generation, but they should generally be weaker than explicit wiki links.

Folder siblings must not be used as expansion frontiers by default.

## Folder Sibling Sampling

Folder sibling retrieval should be bounded and configurable.

Requirements:

- Select peers from the same folder as the reached note.
- If the note has no folder, select peers from the notebook root.
- Apply a maximum number of sampled siblings per note.
- Sibling selection may be deterministic or randomized based on caller-supplied parameters.
- For question generation, randomized sibling sampling is allowed and expected.
- The random seed must be provided by the caller and stored by the caller when reproducibility is needed.
- For `PredefinedQuestion`, the caller should store the seed used to generate the question context.

The retrieval service should not invent an untracked random seed internally when deterministic reproduction is required.

## Token Budget

The system must respect a token budget.

Requirements:

- Always include the focus note.
- If the focus note is too large, truncate its note details to `FOCUS_NOTE_DETAILS_MAX_TOKENS`.
- Retrieved notes use a separate maximum detail allowance, `RELATED_NOTE_DETAILS_MAX_TOKENS`.
- Add retrieved notes while budget allows.
- Stop expansion when no more notes fit.
- Prefer higher-priority notes when budget is limited.
- Avoid allowing folder siblings to consume too much of the budget.
- If a note is too large, include a truncated version or a summary according to the later AI use case.

Suggested budget behavior:

- Reserve most of the budget for the focus note and explicitly linked notes.
- Use a smaller bounded allocation for inbound references and folder siblings.
- Make truncation explicit in the result.

## Internal Result Model

The retrieval service should return a structured internal result, replacing the old `GraphRagResult` model.

The old fields related to `relationToFocusNote`, `olderSiblings`, and `youngerSiblings` must be removed.

The old single `relationToFocusNote` enum must not be kept. It is too narrow for the wiki graph model and cannot represent multi-hop or multi-path retrieval well.

Instead, each retrieved note should expose lightweight retrieval evidence.

Suggested new shape:

```typescript
export type FocusContextNote = {
    notebook?: string;
    title?: string;
    folderPath?: string;
    depth?: number;
    retrievalPath?: Array<string>;
    edgeType?: 'OutgoingWikiLink' | 'InboundWikiReference' | 'FolderSibling';
    reason?: string;
    details?: string;
    detailsTruncated?: boolean;
    createdAt?: string;
};

export type FocusContextFocusNote = {
    notebook?: string;
    title?: string;
    folderPath?: string;
    depth?: 0;
    outgoingLinks?: Array<string>;
    inboundReferences?: Array<string>;
    sampleSiblings?: Array<string>;
    details?: string;
    detailsTruncated?: boolean;
    createdAt?: string;
};

export type FocusContextResult = {
    focusNote?: FocusContextFocusNote;
    relatedNotes?: Array<FocusContextNote>;
};
```

### Retrieval Evidence

The AI should be able to infer semantic relationships from the note content, but the retrieval mechanism should still provide basic retrieval evidence.

Each retrieved note should include:

- Depth from the focus note.
- Retrieval path.
- Edge type or short reason for why it was included.
- Truncation status.

The retrieval path is important because a note's reason for inclusion may not be obvious from its own Markdown content. For example, a note may be included because it was reached through another note's front matter wiki link.

## AI Prompt Output Format

The final context sent to AI should be rendered as Markdown-like text, not as a large JSON object.

However, the note details are themselves arbitrary Markdown and may contain:

- YAML front matter.
- Headings.
- Horizontal rules.
- Fenced code blocks.
- Any other Markdown syntax.

Therefore, the output must clearly isolate wrapper structure from raw note content.

## Markdown Rendering Format

Use a Markdown wrapper with each raw note body inside a fenced block.

Example:

````markdown
# Doughnut Focus Context

Purpose: Context around the focus note for AI use.
Max depth: 2

## Focus Note

Title: TDD
Notebook: Software Development
Folder: TDD
Depth: 0
Truncated: false

Content:

```doughnut-note-md
---
blah: [[practices]]
---

# Actual note heading

Actual note content with [[wiki link]].
```

---

## Retrieved Note

Title: Note Title
Notebook: Software Development
Folder: TDD
Depth: 1
Path: [[TDD]] -> [[Note Title]]
Reached by: OutgoingWikiLink
Truncated: false

Content:

```doughnut-note-md
---
parent: [[Some Parent]]
---

# Linked note heading

Actual linked note content.
```
````

## Safe Fencing Requirement

Because note bodies may contain fenced code blocks, the renderer must dynamically choose a safe fence length.

Requirement:

1. Scan the note content.
2. Find the longest consecutive run of backticks.
3. Use a fence with length `longestRun + 1`.

Example:

`````markdown
````doughnut-note-md
This note contains a normal fenced block:

```java
class Example {}
```

The outer fence is longer, so the content is safe.
````
`````

## Prompt Rendering Requirements

The Markdown renderer should include:

- Focus note title.
- Notebook name.
- Folder path.
- Depth.
- Retrieval path for each retrieved note.
- Edge type or short reason for each retrieved note.
- Truncation status.
- Raw note Markdown content inside safe fenced blocks.

## Performance Requirements

The solution must consider performance and avoid excessive database queries.

Requirements:

- Use the wiki title cache for outgoing link resolution and inbound reference lookup where possible.
- Avoid per-note database queries during traversal when batch loading is possible.
- Batch-fetch notes discovered at the same depth.
- Batch-fetch folder sibling candidates where possible.
- Deduplicate candidate note IDs before hydration.
- Avoid repeatedly loading the same note, folder, notebook, or wiki-link metadata.
- Cache traversal-local lookups inside a single retrieval request.
- Apply token-budget and max-depth constraints early enough to avoid expanding unnecessary branches.
- Avoid expanding folder siblings as frontiers, reducing graph explosion.
- Make query count observable in tests or logs for large retrieval cases.

Performance-sensitive implementation should prefer this general flow:

```text
1. Resolve focus note.
2. Use cached wiki-link/index data to discover candidate IDs by depth.
3. Deduplicate IDs before hydration.
4. Batch-load note details for the selected frontier.
5. Apply budget and truncation.
6. Repeat until budget or max depth is reached.
```

