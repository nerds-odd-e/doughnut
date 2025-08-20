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
  context_path VARCHAR(1024) NULL,
  dimensions INT NOT NULL DEFAULT 1536,
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

- Preferred: Use a local schema variant without `VECTOR` and skip semantic search. Keep DDL compatible by using a separate migration profile:

```sql
-- Local (standard MySQL) schema variant without VECTOR
CREATE TABLE note_embeddings (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  note_id BIGINT NOT NULL,
  kind ENUM('TITLE','DETAILS') NOT NULL,
  context_path VARCHAR(1024) NULL,
  dimensions INT NOT NULL DEFAULT 1536,
  embedding_raw VARBINARY(6144) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_note_embeddings_note_id (note_id)
);
```

Notes:
- Standard MySQL does not accept unknown data types; it cannot "tolerate" `VECTOR(…)` syntax without support. Use environment-specific migrations or a feature flag to avoid executing vector-specific DDL/queries locally.
- Application logic should avoid issuing vector search SQL in local profile (fallback to keyword/full-text search).

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
2. Filter by allowed notebook IDs
3. Compute similarity with candidate embeddings using SQL (`vector_distance`/`approx_distance`)
4. Aggregate per `note_id` with weighting (title higher than details)
5. Apply similarity threshold (0.7)
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

```java
public SearchResults search(String query) {
    // Try semantic search first
    List<Note> semanticResults = semanticSearchService.search(query, 10);
    
    // Fallback to MySQL full-text if no semantic results
    if (semanticResults.isEmpty()) {
        semanticResults = mysqlFullTextService.search(query, 10);
    }
    
    return combineAndRank(semanticResults);
}
```

## Implementation Phases

### Phase 1: Basic Semantic Search (2-3 weeks)
- [ ] OpenAI embedding service integration
- [ ] New table `note_embeddings(note_id, kind, context_path, dimensions, embedding)`
- [ ] CRUD flow to insert/update/delete embeddings on note changes
- [ ] Single embedding per note (no chunking yet)
- [ ] Simple KNN similarity search with SQL (`vector_distance`)
- [ ] New search endpoint with fallback

### Phase 2: Enhanced Features (1-2 weeks)  
- [ ] Large note chunking support
- [ ] Notebook filtering implementation
- [ ] Title vs details weighting
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
- **Local**: No network; use local schema variant (no `VECTOR`) and disable semantic search via feature flag

### OpenAI API
- **API Key**: Secure storage in application properties
- **Rate Limits**: 3,000 RPM for embeddings API
- **Retry Logic**: Exponential backoff for failures
- **Monitoring**: Track usage and costs

### Application Properties
```properties
# OpenAI Configuration
openai.api.key=${OPENAI_API_KEY}
openai.embedding.model=text-embedding-3-small
openai.embedding.max-tokens=8000

# Database / Cloud SQL
spring.datasource.url=${JDBC_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Search Configuration
search.similarity.threshold=0.7
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
