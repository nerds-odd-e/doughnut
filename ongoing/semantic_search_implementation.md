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
2. **Redis Vector Store**: Fast vector similarity search with metadata filtering  
3. **MySQL Database**: Primary storage for note data (no embedding storage needed)
4. **Sync Service**: Keep Redis and MySQL in perfect sync for all CRUD operations

### Data Flow

```
Note CRUD → MySQL → Generate Embedding → Redis Vector Store
                                      ↓
Search Query → Generate Embedding → Redis Search → MySQL Note Fetch → Results
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

### 3. Redis Vector Storage

```java
// Storage Structure
"embedding:{noteId}" → {
    vector: float[1536],
    noteId: Long,
    notebookId: Long,
    title: String,
    createdAt: String,
    isChunk: Boolean,
    chunkIndex: Integer
}

// Notebook Index
"notebook:{notebookId}:notes" → Set<noteId>
```

### 4. CRUD Synchronization

**Create Note:**
1. Save note to MySQL
2. Generate embedding via OpenAI
3. Store embedding in Redis with metadata
4. Index by notebook for filtering

**Update Note:**
1. Update note in MySQL  
2. Delete old embeddings from Redis
3. Generate new embedding
4. Store new embedding in Redis

**Delete Note:**
1. Delete embeddings from Redis
2. Delete note from MySQL
3. Remove from notebook indexes

### 5. Search Implementation

**Query Process:**
1. Generate embedding for search query
2. Filter by allowed notebook IDs
3. Compute cosine similarity with candidate notes
4. Apply similarity threshold (0.7)
5. Sort by relevance score
6. Fetch full note data from MySQL
7. Return ranked results

**Title Weighting:**
- Apply 2x weight to title matches vs details matches
- Combine similarity scores: `(titleScore * 2 + detailsScore) / 3`

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
- [ ] Redis vector storage setup
- [ ] Basic CRUD synchronization
- [ ] Single embedding per note (no chunking yet)
- [ ] Simple similarity search
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
- [ ] Redis persistence configuration
- [ ] Monitoring and alerting

## Configuration Requirements

### Redis Setup
- **Redis version**: 6.2+ (for vector similarity features)
- **Memory**: ~60MB per 10k notes (1536 dims * 4 bytes * 10k)
- **Persistence**: Enable AOF for data durability
- **Hosting**: Google Cloud Memorystore (Redis) recommended

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

# Redis Configuration  
spring.redis.host=${REDIS_HOST}
spring.redis.port=6379
spring.redis.password=${REDIS_PASSWORD}

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
- **Redis Vector Search**: <50ms for 10k notes
- **OpenAI Embedding Generation**: 100-300ms per query
- **Total Search Time**: <400ms end-to-end
- **Throughput**: 2-5 searches/second per instance

### Accuracy Improvements
- **Semantic Understanding**: Find related concepts (e.g., "car" finds "automobile")
- **Typo Tolerance**: Handle misspellings through semantic similarity
- **Context Matching**: Understand phrase meaning vs keyword matching
- **Relevance Ranking**: Results ordered by semantic similarity

## Risk Mitigation

### Redis Data Loss
- **Detection**: Health check on startup
- **Recovery**: Automatic rebuild from MySQL notes
- **Cost**: Regenerate embeddings via OpenAI API
- **Time**: ~30 minutes for 10k notes

### OpenAI API Issues  
- **Fallback**: Use MySQL full-text search
- **Retry Logic**: Exponential backoff with circuit breaker
- **Caching**: Cache query embeddings for repeated searches
- **Monitoring**: Alert on API failures

### Sync Issues
- **Transaction Management**: Use @Transactional for consistency
- **Error Handling**: Rollback MySQL on Redis failures
- **Monitoring**: Track sync failures and inconsistencies
- **Manual Sync**: Admin endpoint to force re-sync

## Success Metrics

### User Experience
- **Search Relevance**: 80%+ of users find relevant results in top 5
- **Search Speed**: <500ms average response time
- **Typo Handling**: 90%+ of minor typos return relevant results

### Technical Metrics  
- **API Uptime**: 99.9% search availability
- **Sync Accuracy**: 99.99% MySQL-Redis consistency
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
