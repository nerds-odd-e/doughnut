---
id: SEED-051
status: dormant
planted: 2026-09-28
planted_during: owner request to retire semantic search after a brief alternatives review
trigger_when: prioritizing non-urgent feature cleanup and reducing ongoing operating cost
scope: medium
---

# SEED-051: Retire costly, underused note semantic search

## Why This Matters

Note embeddings were introduced some time ago and are still refreshed. The
owner reports that semantic search performs poorly, partly because of the
Google Cloud SQL for MySQL implementation, and provides little practical value
for its ongoing cost and maintenance. Users can now check out notebooks locally
and work with them in modern IDEs, reducing the urgency of hosted semantic
search. The requested direction is complete decommissioning, preceded by a
brief investigation of whether a cheaper, faster, useful alternative exists.

## Alternatives and Decision

Default direction: remove the hosted note embedding and semantic search feature
completely. First review the options below against ordinary Donut search and
the existing local notebook/IDE workflow. A promising alternative is a proposal
for the owner to consider, not an implicit commitment to build a replacement.

### Preliminary research — 2026-09-28

- **Existing Cloud SQL implementation:** the production query in
  `NoteEmbeddingJdbcRepository.semanticKnnSearch` uses `vector_distance` with
  scope joins, a distance threshold, ordering, and a limit. Google documents
  `vector_distance` for exact KNN and `approx_distance` for indexed ANN.
  An index/query mismatch is therefore a plausible performance lead, not a
  confirmed root cause. Check the deployed version, index, query plan, and
  query-embedding API latency before concluding that MySQL itself is unusable.
  ANN filtering can return too few matches, so notebook and authorization scope
  must remain correct. See [Google's search and filtering documentation](https://docs.cloud.google.com/sql/docs/mysql/search-filter-vector-embeddings).
- **Simplest alternative:** retain ordinary Donut search and use the existing
  local notebook/IDE workflow. If local keyword search needs more, SQLite FTS5
  supplies full-text search with relevance ranking; it does not provide
  meaning-based matching. This is a candidate to assess, not a new feature
  promise. See [SQLite FTS5](https://www.sqlite.org/fts5.html).
- **Local semantic alternative:** local embeddings through Ollama plus a local
  vector index such as Faiss could move embedding generation and search off the
  hosted database. The inference is that this could avoid hosted embedding API
  calls and database search load; hardware cost, setup, index freshness,
  relevance, and maintenance still need assessment. See [Ollama embeddings](https://docs.ollama.com/capabilities/embeddings)
  and [Faiss](https://github.com/facebookresearch/faiss).

No production benchmark, bill analysis, or alternative prototype was performed
for this capture. The reported poor performance remains owner evidence; these
options are research leads rather than proven savings or speed improvements.

## Story Decomposition

<a id="story-1"></a>

### Decommission note embeddings and semantic search

**Identity:** SEED-051#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** Donut users and the operator need reliable note discovery
  without paying for and maintaining an underused, slow embedding feature.
- **Evaluation:** after a brief documented alternatives review, Donut no longer
  exposes semantic search or embedding index controls, generates or refreshes
  note/query embeddings, or retains live embedding storage and feature-specific
  infrastructure. Ordinary search, notes, relationships, and local notebook
  checkout continue to work.
- **Value / learning:** remove recurring API, database, and maintenance work;
  establish whether any inexpensive alternative offers enough additional value
  over the current local IDE workflow to warrant a separate proposal.
- **Effort hypothesis:** L (roughly 2–4 hours), low confidence until the removal
  inventory is refined. The alternatives review is intended to be brief
  (initial hypothesis: 30–60 minutes), not a replacement implementation project.
- **Depends on:** no other queued story. Conduct the alternatives review before
  removing the feature.
- **Safe stopping point:** the review alone leaves a useful recommendation and
  an unchanged working product; after retirement, ordinary search and local
  notebook work remain available without an embedding dependency.

**Scope**

- Briefly compare fixing the existing vector query/index usage, using the local
  notebook/IDE workflow or keyword search, and a lightweight local semantic
  option. Assess usefulness on representative note-finding tasks, end-to-end
  latency, refresh work, recurring cost, and implementation/operational effort.
  Distinguish measured evidence from estimates and identify any promising
  alternative that would justify reconsidering the removal direction.
- Remove semantic search from all product surfaces, including note creation
  and relationship-target search, and remove notebook index update/reset UI
  and APIs. Remove backend services, repositories, entities, query generation,
  scheduled refresh, and any other note-embedding producers or consumers.
- Remove persisted embedding data, tables/vector indexes, and feature-specific
  configuration and infrastructure through the normal migration and deployment
  path. Cover existing installations as well as fresh/local/test databases.
- Remove or update obsolete tests, fixtures, generated API contracts,
  dependencies, and documentation. Preserve shared utilities, AI capabilities,
  and search behavior that have uses independent of note embeddings; complete
  removal means removing this feature's responsibilities, not unrelated uses.
- A replacement search service, database migration to another engine, new local
  search tool, or broader search redesign is not promised by this story.

## Ordering and Scope Reduction

Queue as non-urgent maintenance. Preserve the current Taken work and other
priorities. Keep the brief alternatives review ahead of decommissioning within
this story; do not turn the review into an open-ended search platform project.
Refine the removal boundaries and evaluation examples before slice planning.

## Breadcrumbs

- Owner request, 2026-09-28: completely remove note semantic search/embeddings,
  first spending a little effort on cheaper or more usable alternatives; local
  notebook checkout and modern IDEs reduce the need and urgency.
- `backend/src/main/java/com/odde/donut/services/EmbeddingMaintenanceJob.java`
  currently schedules production notebook embedding updates every five minutes.
- `backend/src/main/java/com/odde/donut/entities/repositories/NoteEmbeddingJdbcRepository.java`
  contains the production exact-distance query.
- `backend/src/main/java/com/odde/donut/controllers/SearchController.java` and
  `NotebookController.java` expose semantic search and embedding index controls.
- `docs/gcp/prod_env.md` documents Cloud SQL MySQL 8.4 and its vector flag.
