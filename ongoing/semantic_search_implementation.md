# Semantic Search Implementation for Doughnut

## Overview

Replace the current literal note title search with intelligent semantic search that understands context and meaning, providing fuzzy matching and relevance-ranked results for both note titles and details.

## Current State

- **Current Search**: Literal LIKE pattern matching on `topic_constructor` only
- **Limitations**: 
  - No semantic understanding
  - No fuzzy/typo tolerance
  - No relevance ranking
  - Ignores note `details` content
  - No title weighting

## Proposed Solution Architecture

### Core Components

1. **OpenAI Embeddings API**: Convert text to high-quality vector representations
2. **Cloud SQL for MySQL (native vector support)**: Store embeddings in `VECTOR` columns; perform KNN/ANN similarity search using SQL only
3. **MySQL Database (existing tables)**: Primary storage for note data

### Data Flow

```
Note CRUD → MySQL (store text) → Generate Embedding → MySQL (store in VECTOR)
                                                       ↓
Search Query → Generate Embedding → SQL KNN/ANN on VECTOR → Ranked Results
```

## Technical Implementation

### 1. OpenAI Integration

- **Model**: `text-embedding-3-small` ($0.00002/1k tokens)
- **Input**: `note.topicConstructor + " " + note.details`
- **Output**: 1536-dimension vector
- **Per-item token cap (current)**: 4,000
- **Cost**: ~$0.02 per million tokens (very affordable)

### 2. Large Note Handling

- **Token Limit**: 8,000 tokens per request
- **Chunking Strategy**: Split large notes into overlapping chunks
- **Metadata**: Track chunk index and total chunks
- **Search**: Aggregate results from all chunks of a note

### 3. MySQL Vector Storage (Cloud SQL for MySQL)

Use a dedicated table to store embeddings. A note can have multiple rows: one for the title, and one for the details (if any). Include the note's context path to improve title and details embeddings.

```sql
-- Production (Cloud SQL for MySQL with vector support)
CREATE TABLE note_embeddings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  note_id BIGINT NOT NULL,
  kind ENUM('TITLE','DETAILS') NOT NULL,
  embedding VECTOR(1536) USING VARBINARY NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_note_embeddings_note_id (note_id)
);

-- Optional ANN index for performance
CREATE VECTOR INDEX note_embeddings_embedding_idx
ON note_embeddings(embedding)
USING SCANN
QUANTIZER = SQ8
DISTANCE_MEASURE = l2_squared;
```

Local development (no network) options:

- Implemented: Use a local schema variant without `VECTOR` and store raw bytes in `embedding_raw`. Semantic search runs locally by selecting latest embeddings and computing L2 distance in-app. Keep DDL compatible by using a separate migration profile:

```sql
-- Local (standard MySQL) schema variant without VECTOR
CREATE TABLE note_embeddings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  note_id BIGINT NOT NULL,
  kind ENUM('TITLE','DETAILS') NOT NULL,
  embedding_raw VARBINARY(6144) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_note_embeddings_note_id (note_id)
);
```

Notes:
- Standard MySQL does not accept unknown data types; it cannot "tolerate" `VECTOR(…)` syntax without support. We use Flyway placeholders to add `embedding` in prod and `embedding_raw` locally.
- Application logic on local profile performs non-vector semantic search by loading latest embeddings and computing L2 squared distance within the application.

### 4. CRUD Flow

**Create Note:**
1. Save note to MySQL
2. Generate embeddings via OpenAI
   - Title embedding input: `join(contextPath, "/") + " | " + title`
   - Details embedding input (if not empty): `join(contextPath, "/") + " | " + title + "\n\n" + details`
3. Insert rows into `note_embeddings` (`kind = 'TITLE'` and optionally `kind = 'DETAILS'`)

**Update Note:**
1. Update note in MySQL
2. Regenerate relevant embeddings (title and/or details) and upsert rows in `note_embeddings`

**Delete Note:**
1. Delete `note_embeddings` rows by `note_id`, then delete the note row

### 5. Search Implementation

**Query Process:**
1. Generate embedding for search query
2. Filter by allowed notebook IDs and user scope (notebooks, subscriptions, circles)
3. Compute similarity with candidate embeddings using SQL (`vector_distance`) or in-app (local)
4. Aggregate per `note_id` with weighting (title higher than details)
5. Apply similarity threshold (0.5)
6. Fetch full note data from MySQL
7. Return ranked results

**Title Weighting:**
- Apply 2x weight to title vs details. Combine distances per note:
  - `combined = (titleDistance * 2 + detailsDistance) / 3` (use `COALESCE` if one is missing)

Example KNN query (L2 squared):
```sql
WITH q AS (
  SELECT string_to_vector(:queryVectorJson) AS qv
)
SELECT
  ne.note_id,
  MIN(CASE WHEN ne.kind='TITLE' THEN vector_distance(ne.embedding, q.qv, 'distance_measure=l2_squared') END) AS title_dist,
  MIN(CASE WHEN ne.kind='DETAILS' THEN vector_distance(ne.embedding, q.qv, 'distance_measure=l2_squared') END) AS details_dist,
  ((COALESCE(MIN(CASE WHEN ne.kind='TITLE' THEN vector_distance(ne.embedding, q.qv, 'distance_measure=l2_squared') END), 1e9) * 2)
   + COALESCE(MIN(CASE WHEN ne.kind='DETAILS' THEN vector_distance(ne.embedding, q.qv, 'distance_measure=l2_squared') END), 1e9)) / 3 AS combined_dist
FROM note_embeddings ne
JOIN q
GROUP BY ne.note_id
ORDER BY combined_dist ASC
LIMIT 10;
```

### 6. Fallback Strategy

Not implemented yet. Current endpoints return only semantic results.

## Implementation Phases

### Phase 1: Basic Semantic Search (2-3 weeks)
- [x] OpenAI embedding service integration
- [x] New table `note_embeddings(note_id, kind, embedding)`
- [x] CRUD flow to insert/update/delete embeddings on note changes (currently via notebook reindex/incremental update)
- [x] Single embedding per note (TITLE only; no chunking yet)
- [x] Simple KNN similarity search with SQL (`vector_distance`) with local non-vector fallback
- [x] New semantic search endpoints
- [x] Flyway placeholders configured per profile for embedding column and optional vector index (implemented via `V200000196__create_note_embeddings.sql` and `application.yml`)

### Phase 2: Enhanced Features (1-2 weeks)  
- [ ] Large note chunking support
- [x] Notebook filtering implementation
- [x] Title vs details weighting
- [ ] Search result caching
- [ ] Performance monitoring

### Phase 3: Production Optimization (1 week)
- [ ] Async embedding generation
- [ ] Bulk embedding migration for existing notes
- [ ] Error handling and retry logic
- [ ] Create ANN index (`CREATE VECTOR INDEX ... USING SCANN`)
- [ ] Monitoring and alerting

## Configuration Requirements

### Cloud SQL for MySQL (Vector) Setup
- **Service**: Cloud SQL for MySQL with vector embeddings support
- **Schema**: `note_embeddings` table with `embedding VECTOR(1536) USING VARBINARY`, optional SCANN vector index
- **Functions**: `string_to_vector`, `vector_distance` (KNN), `approx_distance` (ANN)
- **Distance measures**: `l2_squared`, `cosine`, `dot_product`
- **Local**: Use local schema variant (no `VECTOR`) and run in-app distance search instead of SQL vector functions

### Environment-Specific Migrations (Flyway Free, minimal duplication)

- This project includes `FlyWayFreeVersionRealMigration` (manual trigger on startup for non-test) and a no-op `FlywayMigrationStrategy` bean. To avoid duplicating migrations across folders, use Flyway placeholders in a single shared migration.

Single shared migration example (lives in `db/migration`):
```sql
-- V200000200__create_note_embeddings.sql (shared)
CREATE TABLE note_embeddings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  note_id BIGINT NOT NULL,
  kind ENUM('TITLE','DETAILS') NOT NULL
);

-- Add environment-specific embedding column
ALTER TABLE note_embeddings ADD COLUMN ${embedding_column};
```

Configure placeholders per Spring profile in `application.yml`:
```yaml
# dev/test/e2e (local MySQL, no VECTOR support)
spring:
  flyway:
    placeholders:
      embedding_column: "embedding_raw VARBINARY(6144) NOT NULL"

# prod (Cloud SQL with vectors)
spring:
  config:
    activate:
      on-profile: prod
  flyway:
    placeholders:
      embedding_column: "embedding VECTOR(1536) USING VARBINARY NOT NULL"
```

Notes:
- Flyway Community supports placeholder replacement with the default `${...}` syntax.
- Keep all migrations in `db/migration` to maintain a single source of truth. Only the placeholder values change per environment.
- If a future DDL differs too much for placeholders, add a tiny env-specific follow-up migration that is guarded by another placeholder (emits no-op locally).

### OpenAI API
- **API Token**: `openai.token` in `application.yml`
- **Rate Limits**: 3,000 RPM for embeddings API
- **Retry Logic**: Exponential backoff for failures (TBD)
- **Monitoring**: Track usage and costs (TBD)

### Application Properties
```properties
# OpenAI Configuration
openai.token=${OPENAI_API_TOKEN}

# Database / Cloud SQL
spring.datasource.url=${JDBC_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Search Configuration
search.similarity.threshold=0.5
search.title.weight=2.0
search.chunk.max-chars=32000
```

## Data Migration Strategy

### Existing Notes Processing
1. **Batch Processing**: Process existing notes in chunks of 100
2. **Rate Limiting**: Respect OpenAI API limits
3. **Progress Tracking**: Log progress and handle interruptions
4. **Error Handling**: Retry failed embeddings
5. **Zero Downtime**: Run migration in background

### Estimated Migration Time
- **10,000 notes**: ~30 minutes (including API calls)
- **Cost**: ~$0.20 for 10k notes with 100 words average

## Performance Expectations

### Search Performance
- **Cloud SQL Vector Search**: Low 10s–100s ms depending on index (KNN vs ANN)
- **OpenAI Embedding Generation**: 100-300ms per query
- **Total Search Time**: <400ms end-to-end
- **Throughput**: 2-5 searches/second per instance

### Accuracy Improvements
- **Semantic Understanding**: Find related concepts (e.g., "car" finds "automobile")
- **Typo Tolerance**: Handle misspellings through semantic similarity
- **Context Matching**: Understand phrase meaning vs keyword matching
- **Relevance Ranking**: Results ordered by semantic similarity

## Risk Mitigation

### Vector Feature Availability / Compatibility
- **Cloud SQL dependency**: Vector features are provided by Cloud SQL for MySQL
- **Local dev (no network)**: Standard MySQL cannot accept unknown `VECTOR` types. Use environment-specific migrations that avoid `VECTOR` locally and feature-flag semantic search off, falling back to keyword/full-text.

### OpenAI API Issues  
- **Fallback**: Use MySQL full-text search
- **Retry Logic**: Exponential backoff with circuit breaker
- **Caching**: Cache query embeddings for repeated searches
- **Monitoring**: Alert on API failures

### Data Integrity
- **Transaction Management**: Use @Transactional for note + embedding updates
- **Monitoring**: Track embedding generation failures and retries

## Success Metrics

### User Experience
- **Search Relevance**: 80%+ of users find relevant results in top 5
- **Search Speed**: <500ms average response time
- **Typo Handling**: 90%+ of minor typos return relevant results

### Technical Metrics  
- **API Uptime**: 99.9% search availability
- **Data Consistency**: Notes and embeddings updated atomically
- **Cost Efficiency**: <$10/month for 10k active notes
- **Performance**: Handle 100+ concurrent searches

## Future Enhancements

### Advanced Features
- **Semantic Question Generation**: Use embeddings for better question suggestions
- **Related Notes**: Find semantically similar notes for recommendations
- **Auto-tagging**: Generate tags based on content similarity
- **Cross-notebook Search**: Semantic search across user's accessible notebooks

### Technical Improvements
- **Hybrid Search**: Combine keyword and semantic search
- **Query Expansion**: Enhance queries with related terms
- **Personalization**: Learn user preferences from search behavior
- **Real-time Updates**: Stream embedding updates for better performance 
