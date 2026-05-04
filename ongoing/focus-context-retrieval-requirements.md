# Focus Context Retrieval Requirements

## Purpose

Replace the existing GraphRAG mechanism with a new **Focus Context Retrieval** mechanism aligned with Donut's wiki/Obsidian-compatible note model.

The mechanism retrieves a bounded, AI-oriented context around a single focus note. It is used by AI features such as question generation, assistants, and note understanding workflows.

The retrieval is **focus-centered**, not query-centered. There is no search phrase. The focus note itself is the retrieval seed.

## Background

Donut has migrated from a tree-structured note model to a wiki-compatible model:

- Notes live inside notebooks.
- A notebook roughly maps to an Obsidian vault.
- Notes are Markdown files.
- Notes may be organized in folders.
- Note titles cannot be duplicated within the same folder.
- Notes may contain YAML front matter.
- Links use double-square-bracket wiki syntax, such as `[[Note Title]]`.
- Cross-notebook links use a notebook prefix, such as `[[Notebook: Note Title]]`.
- The old parent/child structure no longer exists as a structural graph.
- Old parent data is migrated into front matter as a `parent` property using a wiki link.
- Old relation notes are no longer separate graph entities; their source, target, and relation type are represented as Markdown/front matter properties.

Because of this migration, the old GraphRAG design based on parent/child/relationship-note expansion is no longer appropriate.

## Name

The new mechanism should be called:

**Focus Context Retrieval**

Suggested code names:

- `FocusContextRetrievalService`
- `FocusContextRetriever`
- `FocusContextResult`
- `FocusContextMarkdownRenderer`

Avoid continuing to use the name `GraphRAG` unless referring to the legacy mechanism.

## Core Concept

Focus Context Retrieval builds a compact note neighborhood around a focus note by traversing Donut's wiki-style graph.

The graph consists of:

- Outgoing wiki links from the focus note.
- Inbound wiki references to the focus note.
- Wiki links found in front matter properties.
- Relation-style notes with properties such as `source`, `target`, and `relationType`.
- Folder-scoped structural peers, especially older and younger sibling notes in the same folder.
- Optional future signals such as tags, aliases, shared properties, or semantic similarity.

The result must fit within a token budget.

## Retrieval Model

### Retrieval Seed

The retrieval seed is always the focus note.

There is no user search phrase in the default retrieval process.

### Traversal Style

The retrieval algorithm should use **focus-centered weighted breadth-first traversal**.

Requirements:

- Start from the focus note at depth `0`.
- Expand outward through wiki and structural relationships.
- Traverse breadth-first by depth.
- Within each depth, prioritize stronger relationship types before weaker ones.
- Stop when the token budget is exhausted.
- Stop when the configured maximum depth is reached.
- Deduplicate notes by internal note identity, not by serialized wiki URI.

### Depth

The default maximum depth must be at least `3`.

Rationale:

Many Donut notes now represent relationships between two other notes. A shallow traversal may retrieve only the relationship note itself, without retrieving the source and target notes that contain the actual details.

Example shape:

```text
Focus Note
  -> Relationship Note
      -> Source Note
      -> Target Note
          -> Further Explanation Note
```

A maximum depth of only `1` or `2` is often insufficient.

Suggested defaults:

- Default max depth: `3`
- Configurable max depth: `3` to `5`
- Token budget remains the hard stopping constraint.

## Edge Types

The system should internally recognize typed relationships between notes.

Initial edge types:

- `OutgoingWikiLink`
- `InboundWikiReference`
- `FrontmatterWikiLink`
- `RelationSource`
- `RelationTarget`
- `FolderOlderSibling`
- `FolderYoungerSibling`

Possible future edge types:

- `SharedTag`
- `SharedProperty`
- `AliasMatch`
- `TwoHopBridge`
- `SemanticSimilarity`
- `LexicalMatch`

## Relationship-Style Notes

A note should be treated as relation-style when its front matter contains relationship-defining properties such as:

```yaml
source: [[Some Note]]
target: [[Another Note]]
relationType: confused with
```

Special requirement:

When the traversal reaches a relation-style note, its `source` and `target` notes should be expanded with high priority.

This is necessary because relation-style notes often act as connectors rather than complete knowledge notes.

## Prioritization

The traversal must not be blind BFS. It should be weighted BFS.

Suggested initial priority order:

1. Relation-style note source/target links.
2. Outgoing wiki links from the focus note.
3. Frontmatter wiki links.
4. Inbound wiki references.
5. Outgoing links from high-priority retrieved notes.
6. Nearby folder siblings.
7. Links from siblings.

Folder siblings are useful, especially for question generation, but they should generally be weaker than explicit wiki links and relationship-note source/target links.

## Token Budget

The system must respect a token budget.

Requirements:

- Always include the focus note, possibly truncated if necessary.
- Add retrieved notes while budget allows.
- Stop expansion when no more notes fit.
- Track whether each note's content was truncated.
- Track token usage for debugging.
- Prefer higher-priority notes when budget is limited.

Suggested budget behavior:

- Reserve most of the budget for the focus note and explicitly linked notes.
- Avoid allowing folder siblings to consume too much of the budget.
- If a note is too large, include a truncated version or summary according to the later AI use case.

## Internal Result Model

The retrieval service should return a structured internal result.

Do not make the retrieval service return only Markdown.

Suggested model:

```java
class FocusContextResult {
    RetrievedFocusNote focus;
    List<RetrievedNote> retrievedNotes;
    TokenBudgetReport budgetReport;
}

class RetrievedNote {
    Note note;
    int depth;
    double score;
    List<RetrievalPath> paths;
    boolean truncated;
    int tokenCost;
}

class RetrievalPath {
    List<PathStep> steps;
}

class PathStep {
    String fromWikiUri;
    String toWikiUri;
    EdgeType edgeType;
}
```

The structured result is useful for:

- Testing.
- Debugging.
- Deduplication.
- Scoring.
- Token budgeting.
- Rendering into different prompt formats.

## Removal of `RelationshipToFocusNote`

The old `RelationshipToFocusNote` enum should be removed or deprecated.

Rationale:

- The old enum reflects the previous tree-based model.
- A note can have multiple relationships to the focus note.
- A single enum cannot represent depth-2 or depth-3 traversal paths well.
- The enum would keep growing as new wiki-style relationship types are added.
- The retrieval path is more useful than a single relationship label.

However, relationship information should not be removed entirely.

Instead of this:

```json
{
  "relationshipToFocusNote": "OlderSibling"
}
```

Use retrieval evidence:

```json
{
  "depth": 2,
  "paths": [
    {
      "steps": [
        {"from": "[[Focus]]", "to": "[[Relation Note]]", "edgeType": "OutgoingWikiLink"},
        {"from": "[[Relation Note]]", "to": "[[Target Note]]", "edgeType": "RelationTarget"}
      ]
    }
  ]
}
```

The AI should not be forced to infer all retrieval relationships from raw note content. The renderer should provide lightweight path and reason information.

## AI Prompt Output Format

The final context sent to AI should be rendered as Markdown-like text, not as a large JSON object.

Rationale:

- Donut notes are already Markdown.
- Markdown is more natural for LLM consumption than deeply nested JSON.
- Markdown preserves the source medium better.
- It reduces JSON syntax noise.
- It is easier to inspect during debugging.

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
# Donut Focus Context

Purpose: Context around the focus note for AI use.
Max depth: 3

## Focus Note

URI: [[Focus Note]]
Notebook: Software Development
Folder: TDD
Depth: 0

Content:

```donut-note-md
---
tags: [tdd]
---

# Actual note heading

Actual note content.
```

---

## Retrieved Note

URI: [[Linked Note]]
Depth: 1
Path: [[Focus Note]] -> [[Linked Note]]
Reached by: outgoing wiki link
Truncated: false

Content:

```donut-note-md
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
````donut-note-md
This note contains a normal fenced block:

```java
class Example {}
```

The outer fence is longer, so the content is safe.
````
`````

## Prompt Rendering Requirements

The Markdown renderer should include:

- Focus note title and wiki URI.
- Notebook name.
- Folder path.
- Depth.
- Retrieval path for each retrieved note.
- Edge type or short reason for each retrieved note.
- Truncation status.
- Raw note Markdown content inside safe fenced blocks.

The renderer should avoid:

- Treating raw note headings as wrapper headings.
- Concatenating raw Markdown directly into the wrapper without isolation.
- Sending only JSON unless a specific downstream consumer requires JSON.

## Use Cases

Initial consumers:

- Question generation.
- AI note assistant.
- Note explanation or summarization.
- Recall and learning support.

Question generation should prioritize:

1. Focus note content.
2. Outgoing links from the focus note.
3. Relation-style source/target notes.
4. Frontmatter-linked notes.
5. Inbound references.
6. Nearby folder siblings.

## Non-Goals

The new mechanism does not need to:

- Reconstruct the old tree structure.
- Support parent/child/ancestor/cousin graph handlers.
- Preserve relationship notes as separate graph entities.
- Use a search phrase for the default focus-centered mode.
- Implement Microsoft-style GraphRAG with entity extraction and community summaries.
- Return the old `GraphRAGResult` JSON shape.

## Compatibility Notes

Canonical product URLs remain `/n{id}` or routed equivalents.

Wiki-style URIs such as `[[Title]]` or `[[Notebook: Title]]` are used for AI context rendering and graph traversal explanation.

The system should avoid confusing canonical product identity with prompt-facing wiki identity.

## Summary

Focus Context Retrieval should replace the old GraphRAG mechanism with a focus-centered, wiki-aware, token-budgeted traversal system.

The core design is:

```text
Focus note
  -> weighted BFS over wiki links, backlinks, frontmatter links, relation-style notes, and folder peers
  -> max depth at least 3
  -> token-budgeted structured result
  -> Markdown prompt rendering with safely fenced raw note bodies
```

The old `RelationshipToFocusNote` enum should be removed or deprecated in favor of retrieval paths, edge types, depth, and selection reasons.
