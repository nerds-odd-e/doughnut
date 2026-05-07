# Focus context retrieval

Doughnut gathers **bounded context around one focus note** for AI features (question generation, chat about a note, note automation, exports) and for the JSON `GET /api/notes/{note}/graph` surface.

## Model

- **Focus note** — Depth `0`. Always included; details may be truncated to a fixed token budget.
- **Related notes** — Reached by breadth-first expansion up to a configured **max depth** (default `2`), within a **related-note token budget** (default `2500`, overridable per graph request).

## Edges

| Type | Meaning |
|------|---------|
| `OutgoingWikiLink` | Target of a `[[wiki link]]` from note content or YAML front matter. |
| `InboundWikiReference` | Another note in scope that links to this title (wiki title cache). |
| `FolderSibling` | Sampled peer in the same folder or notebook root; not used as an expansion frontier. |

Within a depth, **OutgoingWikiLink** is preferred over **InboundWikiReference** when the same note is reached both ways. Notes dedupe by internal id; the **shortest** path wins.

**Per-depth sampling cap** (inbound wiki references per parent, and folder siblings per anchor) at graph depth:

| Depth | Max sampled per parent / anchor |
|-------|--------------------------------|
| 1     | 6                              |
| 2     | 2 (`floor(6 / 3)`)             |
| 3+    | 0 (stops)                      |

(`sampleCapAtGraphDepth(depth)` in code.)

When a `sampleSeed` is provided in `RetrievalConfig`, candidates above the cap are randomly shuffled with that seed before truncation (deterministic, reproducible). Without a seed, the first N in existing order are taken.

The focus note's flat `inboundReferences` URI list (no token cost) is also capped at **20** and seeded-shuffled when a seed is set.

For **folder siblings**, an anchor at wiki depth `d` uses `sampleCapAtGraphDepth(d + 1)` (same depth as related notes emitted from that anchor). The focus note's `sampleSiblings` URI list uses `sampleCapAtGraphDepth(1)`.

## API shape (`FocusContextResult`)

- `focusNote` — `notebook`, `title`, `folderPath`, `depth`, `outgoingLinks`, `inboundReferences`, `sampleSiblings` (wiki-style URI strings), `details`, `detailsTruncated`, optional `createdAt`.
- `relatedNotes` — Same core fields plus `depth`, `retrievalPath` (wiki URI chain), `edgeType`, optional `reason`, `createdAt`, `details`, `detailsTruncated`.

## Markdown rendering

For LLM prompts, the same retrieval result is rendered as Markdown with a `# Focus Context` header and fenced `doughnut-note-md` blocks (fence width chosen so note content cannot break out).

## Code

- `com.odde.doughnut.services.focusContext.FocusContextRetrievalService`
- `com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer`
- `ApproximateUtf8TokenBudget` — shared UTF-8 byte / token estimate for truncation (e.g. embeddings).
